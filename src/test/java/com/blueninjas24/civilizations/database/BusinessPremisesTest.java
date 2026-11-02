package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class BusinessPremisesTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),donor=UUID.randomUUID(),person=UUID.randomUUID(),business=UUID.randomUUID();
    final UUID consent=UUID.randomUUID(),operation=UUID.randomUUID();BuildingLayout layout;
    Connection c(){return db.getConnection();}BusinessPremises premises(){return new BusinessPremises(c());}BuildingRegistry registry(){return new BuildingRegistry(c());}
    BuildingRegistry.Authorization authority(){return BuildingRegistry.Authorization.playerDeclaration(donor,true);}
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("BusinessPremisesTest"));db.initialize();}
    @BeforeEach void setup()throws Exception{
        open();db.saveSettlement(new Settlement(town,"Premises fixture","fixture",0,64,0,1,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(person,"Existing owner",town,null,300,Resident.Status.ALIVE));
        var bounds=new BuildingLayout.Bounds(new Position(0,64,0),new Position(4,68,4));
        layout=new BuildingLayout(UUID.randomUUID(),town,world,BuildingLayout.Kind.WORKPLACE,bounds,bounds,new Position(0,65,1),List.of(),List.of());
        registry().donate(consent,layout,donor,authority(),null,1000);found(business,1);
    }
    @AfterEach void close(){if(db!=null)db.close();}
    void found(UUID id,int x)throws Exception{
        var site=new BusinessSite(UUID.randomUUID(),town,world,x,65,1,1000,"Explicit existing fixture marker");
        var firm=new Business(id,town,"Saved smith",person,person,"blacksmith",site.id(),2,0);var requirements=new JobRequirements(0,Map.of());
        new BusinessRepository(c()).found(new BusinessFounding(firm,site,requirements,requirements,0,"Existing fixture firm"));
        c().setAutoCommit(false);try{new WorkplaceOperations(c()).attachRows(new WorkplaceOperations.Anchor(id,world,new VillageObservation.Position(x,65,1),WorkplaceObservation.Kind.SMITHING_TABLE),1010);c().commit();}finally{c().setAutoCommit(true);}
    }
    BusinessPremises.Request request(){return new BusinessPremises.Request(operation,business,layout.id(),consent,layout.fingerprint(),1020);}
    BusinessPremises.Hold reserve()throws Exception{return premises().reserve(request(),Set.of(world),1020);}
    void sql(String sql)throws SQLException{try(var s=c().createStatement()){s.execute(sql);}}
    long count(String table)throws SQLException{try(var s=c().createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){return r.getLong(1);}}
    void closeFirm(UUID id)throws SQLException{
        sql("INSERT INTO business_sale_listings VALUES('"+id+"','"+person+"',1,1000,0,1000,1000,'EXPIRED',NULL)");
        sql("INSERT INTO business_closures VALUES('"+id+"','"+person+"',1,'Explicit fixture closure')");
    }
    @Test void holdsPersistAndExactRetriesNeverCreateAssetsOrRights()throws Exception{
        var saved=reserve();assertTrue(saved.retained());assertEquals(saved,reserve());assertFalse(new BuildingRenovations(c()).hasPermission(layout.id(),world));
        for(var table:List.of("treasury_entries","construction_drafts","business_employment","dwelling_assignments","industry_stores"))assertEquals(0,count(table),table);
        assertEquals(1,count("businesses"));assertEquals(1,count("residents"));
        db.close();open();assertEquals(saved,premises().find(operation).orElseThrow());
        assertEquals(saved,premises().reserve(request(),Set.of(),9000)); // Historical receipt, not renewed permission.
        assertThrows(SQLException.class,()->premises().release(UUID.randomUUID(),operation,1030));
        assertThrows(SQLException.class,()->sql("DELETE FROM business_premises"));
        assertThrows(SQLException.class,()->sql("UPDATE business_premises SET layout_hash='"+"0".repeat(64)+"'"));
    }
    @Test void newHoldsRequirePresentWorldCurrentConsentFreshCompatibleFixedMarker()throws Exception{
        assertThrows(SQLException.class,()->premises().reserve(request(),Set.of(),1020));
        assertThrows(SQLException.class,()->premises().reserve(request(),Set.of(world),7000));
        sql("UPDATE business_workplace_anchors SET present=0");assertThrows(SQLException.class,this::reserve);
        sql("UPDATE business_workplace_anchors SET present=1,captured_millis=1021");assertThrows(SQLException.class,this::reserve);
        sql("UPDATE business_workplace_anchors SET captured_millis=1010");
        assertThrows(SQLException.class,()->premises().reserve(new BusinessPremises.Request(operation,business,layout.id(),UUID.randomUUID(),layout.fingerprint(),1020),Set.of(world),1020));
        assertEquals(0,count("business_premises"));assertTrue(c().getAutoCommit());reserve();
        assertThrows(SQLException.class,()->premises().reserve(new BusinessPremises.Request(operation,business,layout.id(),consent,"0".repeat(64),1020),Set.of(world),1020));
    }
    @Test void inspectedReservationsCannotSubstituteAnotherMarkerOrUseAnUnsafeInterior()throws Exception{
        var cells=new HashMap<Position,DwellingInspection.Cell>();
        for(var p:DwellingInspection.requiredCells(layout))cells.put(p,DwellingInspection.Cell.of(p.y()==64||p.y()==68?DwellingInspection.Type.SUPPORT:DwellingInspection.Type.PASSABLE));
        var marker=new Position(1,65,1);cells.put(marker,DwellingInspection.Cell.of(DwellingInspection.Type.SUPPORT));
        var observation=new DwellingInspection.Observation(world,layout.fingerprint(),1010,true,cells);
        var otherCells=new HashMap<>(cells);otherCells.put(new Position(2,65,1),DwellingInspection.Cell.of(DwellingInspection.Type.SUPPORT));
        var wrong=new WorkplaceInspection.Evidence(new DwellingInspection.Observation(world,layout.fingerprint(),1010,true,otherCells),new Position(2,65,1),Optional.of(WorkplaceObservation.Kind.SMITHING_TABLE),true);
        var wrongAssessment=WorkplaceInspection.inspect(layout,wrong,1020,5000,WorkplaceInspection.Policy.prototype());
        assertTrue(wrongAssessment.suitable());assertThrows(SQLException.class,()->premises().reserveInspected(request(),wrongAssessment,Set.of(world),1020));
        cells.put(layout.entrance(),DwellingInspection.Cell.of(DwellingInspection.Type.BLOCKED));
        var unsafe=new WorkplaceInspection.Evidence(new DwellingInspection.Observation(world,layout.fingerprint(),1010,true,cells),marker,wrong.kind(),true);
        assertThrows(SQLException.class,()->premises().reserveInspected(request(),WorkplaceInspection.inspect(layout,unsafe,1020,5000,WorkplaceInspection.Policy.prototype()),Set.of(world),1020));
        assertEquals(0,count("business_premises"));
        var exact=new WorkplaceInspection.Evidence(observation,marker,wrong.kind(),true);
        assertTrue(premises().reserveInspected(request(),WorkplaceInspection.inspect(layout,exact,1020,5000,WorkplaceInspection.Policy.prototype()),Set.of(world),1020).retained());
    }
    @Test void reclamationReleasesOnlyTheOldUseEpochAndReplayCannotReactivateIt()throws Exception{
        reserve();UUID reclaim=UUID.randomUUID();registry().reclaim(reclaim,layout.id(),authority(),consent,1030);
        UUID release=UUID.randomUUID();var retired=premises().release(release,operation,1040);assertFalse(retired.retained());assertEquals(retired,premises().release(release,operation,1040));
        UUID nextConsent=UUID.randomUUID();registry().donate(nextConsent,layout,donor,authority(),reclaim,1050);
        assertEquals(retired,reserve());
        assertThrows(SQLException.class,()->premises().reserve(new BusinessPremises.Request(UUID.randomUUID(),business,layout.id(),consent,layout.fingerprint(),1060),Set.of(world),1060));
        var next=new BusinessPremises.Request(UUID.randomUUID(),business,layout.id(),nextConsent,layout.fingerprint(),1060);
        assertTrue(premises().reserve(next,Set.of(world),1060).retained());assertEquals(2,count("business_premises"));
        assertThrows(SQLException.class,()->sql("UPDATE business_premises SET released_ms=NULL,release_operation=NULL WHERE operation_id='"+operation+"'"));
    }
    @Test void legacyOccupantsAndRetainedReservationsBlockDoubleUseUntilClosedAndReleased()throws Exception{
        var original=reserve();UUID second=UUID.randomUUID();found(second,2);
        var next=new BusinessPremises.Request(UUID.randomUUID(),second,layout.id(),consent,layout.fingerprint(),1020);
        assertThrows(SQLException.class,()->premises().reserve(next,Set.of(world),1020));
        closeFirm(business);assertThrows(SQLException.class,()->premises().reserve(next,Set.of(world),1020));
        premises().release(UUID.randomUUID(),operation,1030);assertTrue(premises().reserve(next,Set.of(world),1030).retained());
        assertEquals(2,count("businesses"));assertEquals(2,count("business_sites"));assertEquals(2,count("business_premises"));assertEquals(original.request(),premises().find(operation).orElseThrow().request());
        // A business founded before this feature has no reservation, but still occupies its building.
        UUID third=UUID.randomUUID();found(third,3);assertThrows(SQLException.class,()->premises().reserve(new BusinessPremises.Request(UUID.randomUUID(),third,layout.id(),consent,layout.fingerprint(),1040),Set.of(world),1040));
    }
    @Test void anOlderFirmWithoutAnyPremisesHoldStillPreventsInferredVacancy()throws Exception{
        UUID second=UUID.randomUUID();found(second,2);assertEquals(0,count("business_premises"));
        assertThrows(SQLException.class,this::reserve);
        assertThrows(SQLException.class,()->premises().reserve(new BusinessPremises.Request(UUID.randomUUID(),second,layout.id(),consent,layout.fingerprint(),1020),Set.of(world),1020));
        assertEquals(0,count("business_premises"));
    }
    @Test void lateInsertAndPopulated119UpgradeFailuresRollBackWithoutLosingHistory()throws Exception{
        sql("CREATE TRIGGER fixture_fail AFTER INSERT ON business_premises BEGIN SELECT RAISE(ABORT,'Late fixture failure'); END");
        assertThrows(SQLException.class,this::reserve);assertEquals(0,count("business_premises"));assertTrue(c().getAutoCommit());sql("DROP TRIGGER fixture_fail");
        SchemaFixtures.rewind(c(),119);var migrations=new ArrayList<SchemaMigrator.Migration>();for(int i=0;i<119;i++)migrations.add(connection->{});
        migrations.add(connection->{BusinessPremises.createSchema(connection);throw new SQLException("Late120 fixture failure");});
        assertThrows(SQLException.class,()->new SchemaMigrator(migrations).migrate(c()));
        try(var s=c().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(119,r.getInt(1));}
        assertEquals(1,count("businesses"));assertEquals(layout,registry().find(layout.id()).orElseThrow().layout());
        new SchemaMigrator().migrate(c());reserve();try(var s=c().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
