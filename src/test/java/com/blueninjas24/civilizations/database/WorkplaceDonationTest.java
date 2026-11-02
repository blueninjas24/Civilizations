package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class WorkplaceDonationTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID world=UUID.randomUUID(),town=UUID.randomUUID(),person=UUID.randomUUID(),donor=UUID.randomUUID(),business=UUID.randomUUID(),operation=UUID.randomUUID();
    BuildingLayout layout;WorkplaceInspection.Assessment assessment;
    Connection c(){return db.getConnection();}WorkplaceDonation donations(){return new WorkplaceDonation(c());}
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("WorkplaceDonationTest"));db.initialize();}
    BuildingRegistry.Authorization authority(){return BuildingRegistry.Authorization.playerDeclaration(donor,true);}
    WorkplaceDonation.Request request(){return new WorkplaceDonation.Request(operation,business,layout,donor,null,1020);}
    WorkplaceDonation.Receipt confirm()throws Exception{return donations().confirm(request(),authority(),assessment,Set.of(world),1020);}
    @BeforeEach void setup()throws Exception{
        open();db.saveSettlement(new Settlement(town,"Donation fixture","fixture",0,64,0,1,0,0));db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(person,"Existing owner",town,null,300,Resident.Status.ALIVE));
        var site=new BusinessSite(UUID.randomUUID(),town,world,3,65,2,900,"Explicit old workplace");var firm=new Business(business,town,"Saved smith",person,person,"blacksmith",site.id(),2,0);var requirement=new JobRequirements(0,Map.of());
        new BusinessRepository(c()).found(new BusinessFounding(firm,site,requirement,requirement,0,"Existing fixture firm"));
        c().setAutoCommit(false);try{new WorkplaceOperations(c()).attachRows(new WorkplaceOperations.Anchor(business,world,new VillageObservation.Position(3,65,2),WorkplaceObservation.Kind.SMITHING_TABLE),900);c().commit();}finally{c().setAutoCommit(true);}
        new WorkplaceOperations(c()).confirm(List.of(new WorkplaceOperations.Confirmation(new WorkplaceOperations.Anchor(business,world,new VillageObservation.Position(3,65,2),WorkplaceObservation.Kind.SMITHING_TABLE),false,950)),950,Set.of(world));
        var bounds=new BuildingLayout.Bounds(new Position(0,64,0),new Position(4,67,4));layout=new BuildingLayout(UUID.randomUUID(),town,world,BuildingLayout.Kind.WORKPLACE,bounds,bounds,new Position(0,65,2),List.of(),List.of());
        var cells=new HashMap<Position,Cell>();for(var at:DwellingInspection.requiredCells(layout))cells.put(at,Cell.of(at.y()==64||at.y()==67?Type.SUPPORT:Type.PASSABLE));cells.put(new Position(3,65,2),Cell.of(Type.SUPPORT));
        var evidence=new WorkplaceInspection.Evidence(new Observation(world,layout.fingerprint(),1010,true,cells),new Position(3,65,2),Optional.of(WorkplaceObservation.Kind.SMITHING_TABLE),true);
        assessment=WorkplaceInspection.inspect(layout,evidence,1020,5000,WorkplaceInspection.Policy.prototype());assertTrue(assessment.suitable());
    }
    @AfterEach void close(){if(db!=null)db.close();}
    void sql(String query)throws SQLException{try(var s=c().createStatement()){s.execute(query);}}
    long count(String table)throws SQLException{try(var s=c().createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){return r.getLong(1);}}
    @Test void donationMarkerAndHoldPersistAsOneReceiptWithoutGrantingAssets()throws Exception{
        var saved=confirm();assertEquals(saved,confirm());assertTrue(new BuildingRegistry(c()).hasDonationPermission(layout.id()));assertFalse(WorkplaceOperations.paused(c(),business));
        assertEquals(operation,saved.premises().request().useConsent());assertEquals(1,count("building_records"));assertEquals(1,count("business_premises"));
        for(var table:List.of("treasury_entries","industry_stores","business_employment","construction_drafts","dwelling_assignments"))assertEquals(0,count(table),table);
        db.close();open();assertEquals(saved,donations().confirm(request(),authority(),null,Set.of(),9000));
    }
    @Test void latePremisesFailureRollsBackDonationAndMarkerTransitionBeforeExactRetry()throws Exception{
        sql("CREATE TRIGGER fixture_late_failure AFTER INSERT ON business_premises BEGIN SELECT RAISE(ABORT,'Late publication failure'); END");
        assertThrows(SQLException.class,this::confirm);assertTrue(c().getAutoCommit());assertTrue(WorkplaceOperations.paused(c(),business));
        assertEquals(2,count("workplace_operation_history"));for(var table:List.of("building_records","building_consent_events","business_premises"))assertEquals(0,count(table),table);
        sql("DROP TRIGGER fixture_late_failure");confirm();assertEquals(3,count("workplace_operation_history"));assertEquals(1,count("building_records"));
    }
    @Test void replayAfterReclamationAndReleaseCannotRestoreUseOrRebindAnotherBusiness()throws Exception{
        var saved=confirm();new BuildingRegistry(c()).reclaim(UUID.randomUUID(),layout.id(),authority(),operation,1030);assertTrue(BusinessPremises.denied(c(),business));
        assertEquals(saved,donations().confirm(request(),authority(),null,Set.of(),1040));assertFalse(new BuildingRegistry(c()).hasDonationPermission(layout.id()));
        new BusinessPremises(c()).release(UUID.randomUUID(),saved.premises().request().operation(),1050);
        assertFalse(donations().confirm(request(),authority(),null,Set.of(),1060).premises().retained());assertTrue(BusinessPremises.denied(c(),business));
        var changed=new WorkplaceDonation.Request(operation,UUID.randomUUID(),layout,donor,null,1020);
        assertThrows(SQLException.class,()->donations().confirm(changed,authority(),null,Set.of(),1060));assertEquals(2,count("building_consent_events"));
    }
    @Test void absentWorldMissingOwnershipAndStaleInspectionCannotPublishPartialDonation()throws Exception{
        assertThrows(SQLException.class,()->donations().confirm(request(),authority(),assessment,Set.of(),1020));
        assertThrows(SQLException.class,()->donations().confirm(request(),BuildingRegistry.Authorization.playerDeclaration(donor,false),assessment,Set.of(world),1020));
        var freshRequest=new WorkplaceDonation.Request(operation,business,layout,donor,null,7000);
        assertThrows(SQLException.class,()->donations().confirm(freshRequest,authority(),assessment,Set.of(world),7000));
        assertEquals(0,count("building_records"));assertTrue(WorkplaceOperations.paused(c(),business));assertTrue(c().getAutoCommit());
    }
    @Test void publicWrappersRefuseNestedTransactionsWithoutCommittingTheCallersRows()throws Exception{
        c().setAutoCommit(false);try{
            assertThrows(SQLException.class,this::confirm);
            assertThrows(SQLException.class,()->new BuildingRegistry(c()).donate(operation,layout,donor,authority(),null,1020));
            assertThrows(SQLException.class,()->new WorkplaceOperations(c()).confirm(List.of(),1020,Set.of(world)));
            assertFalse(c().getAutoCommit());assertEquals(0,count("building_records"));
        }finally{c().rollback();c().setAutoCommit(true);}
    }
}
