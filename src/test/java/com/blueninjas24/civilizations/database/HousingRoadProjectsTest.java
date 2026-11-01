package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HousingRoadProjectsTest {
    @TempDir Path directory;DatabaseManager db;ConstructionDraft road,home;
    final UUID town=new UUID(0,1),world=new UUID(0,2),family=new UUID(0,3);
    Connection c(){return db.getConnection();}
    HousingRoadProjects projects(){return new HousingRoadProjects(c());}
    DwellingOccupancy.Context context(){return new DwellingOccupancy.Context(world,true,DwellingInspection.Policy.prototype(),1100);}
    HousingProjects.Proposal proposal(){return new HousingProjects.Proposal(home.id(),family,1000);}
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("HousingRoadProjectsTest"));db.initialize();}
    @BeforeEach void setup()throws Exception{
        open();db.saveSettlement(new Settlement(town,"Access fixture","fixture",0,64,0,1,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture world");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertHousehold(new Household(family,town,"Saved family"));
        db.getPopulationRepository().insertResident(new Resident(new UUID(0,4),"Existing resident",town,family,300,Resident.Status.ALIVE));
        sql("INSERT INTO household_capacity VALUES('"+family+"',0)");db.getDemographicRepository().initializeClock(42,PopulationPolicy.prototype());
        var template=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
        home=new ConstructionDraft(UUID.randomUUID(),town,world,new Position(0,64,1),Transform.identity(),template,1000);
        var recipe=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_path.yml"));
        var design=LocalRoadDesign.freeze(new LocalRoadAccessPlanner.Result(LocalRoadAccessPlanner.Status.READY,List.of(new Position(2,63,0),new Position(2,63,-1)),1,2),recipe).orElseThrow();
        road=new ConstructionDraft(UUID.randomUUID(),town,world,design.origin(),Transform.identity(),design.template(),1000);
        new ConstructionDraftRepository(c()).save(home);new ConstructionDraftRepository(c()).save(road);
    }
    @AfterEach void close(){db.close();}
    void sql(String sql)throws SQLException{try(var s=c().createStatement()){s.execute(sql);}}
    long count(String table)throws SQLException{try(var s=c().createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){return r.getLong(1);}}
    HousingRoadProjects.Link bind()throws SQLException{return projects().bind(road.id(),proposal(),context(),HousingProjectPlanner.prototype(),42,0);}
    @Test void purposeAndExistingFamilyReservationPersistWithoutLandMoneyOrCapacity()throws Exception{
        var saved=bind();assertEquals(home.id(),saved.homeDraft());assertEquals(saved,bind());
        assertEquals(Set.of(family),new HousingProjects(c()).activeHouseholds(town,world));
        for(var table:List.of("construction_site_reservations","construction_supply_orders","construction_block_journal","household_capacity_sources","civ_roads"))assertEquals(0,count(table),table);
        assertEquals(1,count("residents"));assertEquals(1,count("housing_project_bindings"));
        db.close();open();assertEquals(saved,projects().findRoad(road.id()).orElseThrow());assertEquals(saved,projects().findHome(home.id()).orElseThrow());
        assertThrows(SQLException.class,()->sql("DELETE FROM housing_road_projects"));
    }
    @Test void lateFailureRollsBackHouseholdReservationAndExactRetryWorks()throws Exception{
        sql("CREATE TRIGGER fail_road_link AFTER INSERT ON housing_road_projects BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,this::bind);assertEquals(0,count("housing_project_bindings"));assertEquals(0,count("housing_road_projects"));assertTrue(c().getAutoCommit());
        sql("DROP TRIGGER fail_road_link");bind();assertEquals(1,count("housing_project_bindings"));
    }
    @Test void actualNeedAndFrozenConnectorCannotBeInvented()throws Exception{
        sql("UPDATE household_capacity SET capacity=1");assertThrows(SQLException.class,this::bind);assertEquals(0,count("housing_project_bindings"));
        sql("UPDATE household_capacity SET capacity=0");
        var unrelated=new ConstructionDraft(UUID.randomUUID(),town,world,new Position(20,64,20),home.transform(),home.template(),1000);new ConstructionDraftRepository(c()).save(unrelated);
        assertThrows(SQLException.class,()->projects().bind(road.id(),new HousingProjects.Proposal(unrelated.id(),family,1000),context(),HousingProjectPlanner.prototype(),42,0));
        assertEquals(0,count("housing_project_bindings"));bind();
    }
    @Test void changedBeneficiaryAndFutureProtectionPauseRoadWithoutLosingPurpose()throws Exception{
        bind();var sites=new ConstructionSiteReservations(c());var authority=new ConstructionMaterials.Authority(road.id(),world,1100,true,true);sites.reserve(road.id(),authority,1100);
        sql("UPDATE household_capacity SET capacity=1");
        c().setAutoCommit(false);try{assertThrows(SQLException.class,()->sites.requireActiveRows(road));}finally{c().rollback();c().setAutoCommit(true);}
        sql("UPDATE household_capacity SET capacity=0");UUID actor=UUID.randomUUID();
        new BuildingRegistry(c()).exclude(new BuildingRegistry.Exclusion(UUID.randomUUID(),world,actor,new BuildingLayout.Bounds(new Position(0,64,1),new Position(0,65,1)),1100),BuildingRegistry.Authorization.playerDeclaration(actor,true));
        c().setAutoCommit(false);try{assertThrows(SQLException.class,()->sites.requireActiveRows(road));}finally{c().rollback();c().setAutoCommit(true);}
        assertTrue(sites.find(road.id()).orElseThrow().active());assertEquals(1,count("housing_road_projects"));
        assertThrows(SQLException.class,()->new HousingProjects(c()).cancel(UUID.randomUUID(),home.id(),1100,context()));
        sites.releaseUnused(road.id(),1100);new HousingProjects(c()).cancel(UUID.randomUUID(),home.id(),1100,context());
        assertFalse(new HousingProjects(c()).find(home.id()).orElseThrow().active());assertTrue(projects().findRoad(road.id()).isPresent());
        assertThrows(SQLException.class,()->sites.reserve(road.id(),authority,1100));
    }
    @Test void populated118MigrationPreservesFamilyDraftsAndLateFailureIsAtomic()throws Exception{
        SchemaFixtures.rewind(c(),118);var migrations=new ArrayList<SchemaMigrator.Migration>();for(int i=0;i<118;i++)migrations.add(connection->{});
        migrations.add(connection->{HousingRoadProjects.createSchema(connection);throw new SQLException("fixture late119 failure");});
        assertThrows(SQLException.class,()->new SchemaMigrator(migrations).migrate(c()));assertEquals(1,count("residents"));
        try(var s=c().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(118,r.getInt(1));}
        new SchemaMigrator().migrate(c());assertEquals(0,count("housing_road_projects"));assertEquals(home,new ConstructionDraftRepository(c()).find(home.id()).orElseThrow());bind();
        try(var s=c().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
