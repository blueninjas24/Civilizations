package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorkplaceMarkerExtensionTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=new UUID(0,1),world=new UUID(0,2),person=new UUID(0,3);
    final List<String> tables=List.of("workplace_markers","workplace_sightings","workplace_operation_history","business_workplace_anchors");
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Marker fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        db.getPopulationRepository().insertResident(new Resident(person,"Fixture owner",town,null,300,Resident.Status.ALIVE));
    }
    void open()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("WorkplaceMarkerExtensionTest"));db.initialize();}
    @AfterEach void close(){db.close();}
    void sql(String sql)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(sql);}}
    List<List<Object>> rows(String sql)throws SQLException{var list=new ArrayList<List<Object>>();try(var s=db.getConnection().createStatement();var r=s.executeQuery(sql)){while(r.next()){var row=new ArrayList<Object>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.add(r.getObject(i));list.add(row);}}return list;}
    Map<String,List<List<Object>>> snapshot()throws SQLException {
        var all=new TreeMap<String,List<List<Object>>>();for(var name:rows("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"))all.put((String)name.getFirst(),rows("SELECT * FROM \""+name.getFirst()+"\" ORDER BY rowid"));return all;
    }
    WorkplaceObservation.Marker marker(int x,WorkplaceObservation.Kind kind){return new WorkplaceObservation.Marker(new VillageObservation.Position(x,64,0),kind);}
    void observe(long time,WorkplaceObservation.Marker... markers)throws SQLException{new WorkplaceOpportunities(db.getConnection()).observe(town,new WorkplaceObservation(world,0,0,time,true,List.of(markers)));}
    WorkplaceOperations.Anchor firm(int x,WorkplaceObservation.Kind kind)throws SQLException {
        var site=new BusinessSite(new UUID(1,x),town,world,x,64,0,1000,"Trusted fixture site");
        var business=new Business(new UUID(2,x),town,"Fixture firm",person,person,kind.industry(),site.id(),1,0);
        var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(business,site,requirements,requirements,0,"Fixture founding"));
        var anchor=new WorkplaceOperations.Anchor(business.id(),world,new VillageObservation.Position(x,64,0),kind);
        db.getConnection().setAutoCommit(false);try{new WorkplaceOperations(db.getConnection()).attachRows(anchor,1000);db.getConnection().commit();}finally{db.getConnection().setAutoCommit(true);}return anchor;
    }
    // Reconstruct exact schema76 marker constraints only in this test's temporary DB.
    void legacy76()throws SQLException {
        var ddl=new HashMap<String,String>();var accessories=rows("SELECT sql FROM sqlite_master WHERE type IN('index','trigger') AND sql IS NOT NULL AND tbl_name IN('workplace_markers','workplace_sightings','workplace_operation_history','business_workplace_anchors') ORDER BY type,name");
        for(String table:tables){ddl.put(table,(String)rows("SELECT sql FROM sqlite_master WHERE name='"+table+"'").getFirst().getFirst());sql("CREATE TEMP TABLE fixture_old_"+table+" AS SELECT * FROM "+table);}
        for(String table:tables)sql("DROP TABLE "+table);
        for(String table:List.of("workplace_sightings","workplace_markers","business_workplace_anchors","workplace_operation_history")){sql(ddl.get(table).replace(",'SMOKER','CRAFTING_TABLE'",""));sql("INSERT INTO "+table+" SELECT * FROM fixture_old_"+table);sql("DROP TABLE fixture_old_"+table);}
        for(var accessory:accessories)sql((String)accessory.getFirst());sql("PRAGMA user_version=76");
    }
    @Test void bakeryAndCraftsmanEvidencePersistsWithoutInventingBusinessOrRights()throws Exception {
        observe(1000,marker(1,WorkplaceObservation.Kind.SMOKER),marker(2,WorkplaceObservation.Kind.CRAFTING_TABLE));
        var before=snapshot();var review=new WorkplaceOpportunities(db.getConnection()).review(town,1000,100,Set.of(world),512);
        assertEquals(List.of("bakery","craftsman"),review.candidates().stream().map(WorkplaceOpportunities.Candidate::industry).toList());
        for(String asset:List.of("businesses","business_sites","treasury_entries","industry_stores","business_employment"))assertTrue(before.get(asset).isEmpty());
        db.close();open();assertEquals(before,snapshot());
    }
    @Test void newAnchorKindsPauseOnConfirmedDamageAndResumeOnlyOnValidLoadedEvidence()throws Exception {
        var bakery=firm(1,WorkplaceObservation.Kind.SMOKER);var craft=firm(2,WorkplaceObservation.Kind.CRAFTING_TABLE);
        var ops=new WorkplaceOperations(db.getConnection());assertEquals(2,ops.anchors(null,64).size());
        ops.confirm(List.of(new WorkplaceOperations.Confirmation(bakery,false,1001),new WorkplaceOperations.Confirmation(craft,false,1001)),1001,Set.of(world));
        assertTrue(WorkplaceOperations.paused(db.getConnection(),bakery.business()));assertTrue(WorkplaceOperations.paused(db.getConnection(),craft.business()));
        var before=snapshot();ops.confirm(List.of(new WorkplaceOperations.Confirmation(bakery,true,1002)),1002,Set.of());assertEquals(before,snapshot());
        ops.confirm(List.of(new WorkplaceOperations.Confirmation(bakery,true,1002)),1002,Set.of(world));assertFalse(WorkplaceOperations.paused(db.getConnection(),bakery.business()));assertTrue(WorkplaceOperations.paused(db.getConnection(),craft.business()));
        db.close();open();assertTrue(WorkplaceOperations.paused(db.getConnection(),craft.business()));
    }
    @Test void markerIndustryMismatchCannotAttachToAnotherBusiness()throws Exception {
        var anchor=firm(1,WorkplaceObservation.Kind.SMOKER);var before=snapshot();
        db.getConnection().setAutoCommit(false);
        try{assertThrows(SQLException.class,()->new WorkplaceOperations(db.getConnection()).attachRows(new WorkplaceOperations.Anchor(anchor.business(),world,anchor.position(),WorkplaceObservation.Kind.CRAFTING_TABLE),1001));db.getConnection().rollback();}finally{db.getConnection().setAutoCommit(true);}
        assertEquals(before,snapshot());
    }
    @Test void exactUpgradePreservesAllRowsAndPermitsNewKindsWithoutLosingOldHistory()throws Exception {
        observe(1000,marker(1,WorkplaceObservation.Kind.BARREL),marker(2,WorkplaceObservation.Kind.SMITHING_TABLE));firm(3,WorkplaceObservation.Kind.SMITHING_TABLE);legacy76();var before=snapshot();
        assertThrows(SQLException.class,()->observe(1001,marker(1,WorkplaceObservation.Kind.SMOKER)));
        new SchemaMigrator().migrate(db.getConnection());assertEquals(before,snapshot());
        assertEquals(2,rows("SELECT * FROM workplace_sightings").size());observe(1001,marker(1,WorkplaceObservation.Kind.SMOKER),marker(2,WorkplaceObservation.Kind.CRAFTING_TABLE));assertEquals(4,rows("SELECT * FROM workplace_sightings").size());
        assertTrue(rows("PRAGMA foreign_key_check").isEmpty());assertTrue(rows("SELECT name FROM sqlite_temp_master WHERE name LIKE 'civ_schema77_%'").isEmpty());
    }
    @Test void failureAfterRebuildRollsBackEveryRowConstraintAndVersionForRetry()throws Exception {
        observe(1000,marker(1,WorkplaceObservation.Kind.BARREL));firm(3,WorkplaceObservation.Kind.SMITHING_TABLE);legacy76();var before=snapshot();
        List<SchemaMigrator.Migration> steps=new ArrayList<>(Collections.nCopies(76,c->{}));steps.add(c->{WorkplaceMarkerMigration.apply(c);try(var s=c.createStatement()){s.execute("THIS IS INVALID SQL");}});
        assertThrows(SQLException.class,()->new SchemaMigrator(steps).migrate(db.getConnection()));assertEquals(before,snapshot());assertTrue(db.getConnection().getAutoCommit());
        assertEquals(76,((Number)rows("PRAGMA user_version").getFirst().getFirst()).intValue());assertThrows(SQLException.class,()->observe(1001,marker(1,WorkplaceObservation.Kind.SMOKER)));
        assertTrue(rows("SELECT name FROM sqlite_temp_master WHERE name LIKE 'civ_schema77_%'").isEmpty());new SchemaMigrator().migrate(db.getConnection());assertEquals(before,snapshot());
    }
    @Test void customIndexesAndTriggersSurviveWithoutFiringDuringCopiedHistory()throws Exception {
        observe(1000,marker(1,WorkplaceObservation.Kind.BARREL));legacy76();sql("CREATE TABLE fixture_audit(calls INTEGER NOT NULL)");sql("INSERT INTO fixture_audit VALUES(0)");
        sql("CREATE INDEX fixture_sighting_kind ON workplace_sightings(kind)");sql("CREATE TRIGGER fixture_sighting_audit AFTER INSERT ON workplace_sightings BEGIN UPDATE fixture_audit SET calls=calls+1; END");
        var before=snapshot();new SchemaMigrator().migrate(db.getConnection());assertEquals(before,snapshot());assertEquals(2,rows("SELECT name FROM sqlite_master WHERE name IN('fixture_sighting_kind','fixture_sighting_audit')").size());
        observe(1001,marker(2,WorkplaceObservation.Kind.SMOKER));assertEquals(1,((Number)rows("SELECT calls FROM fixture_audit").getFirst().getFirst()).intValue());
        assertThrows(SQLException.class,()->sql("UPDATE workplace_sightings SET kind='FURNACE'"));
    }
    @Test void unknownCascadingReferencesRefuseUpgradeBeforeAnyCustomDataCanDisappear()throws Exception {
        var anchor=firm(3,WorkplaceObservation.Kind.SMITHING_TABLE);legacy76();
        sql("CREATE TABLE fixture_external(business TEXT REFERENCES business_workplace_anchors(business_id) ON DELETE CASCADE,notes TEXT NOT NULL)");
        sql("INSERT INTO fixture_external VALUES('"+anchor.business()+"','Must retain extension history')");var before=snapshot();
        var failure=assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));assertTrue(failure.getMessage().contains("external foreign key"));
        assertEquals(before,snapshot());assertEquals(76,((Number)rows("PRAGMA user_version").getFirst().getFirst()).intValue());assertTrue(db.getConnection().getAutoCommit());
        assertTrue(rows("SELECT name FROM sqlite_temp_master WHERE name LIKE 'civ_schema77_%'").isEmpty());assertTrue(rows("PRAGMA foreign_key_check").isEmpty());
    }
    @Test void unrelatedMarkersCannotExhaustAnotherIndustrysCandidateBudget()throws Exception {
        var markers=new ArrayList<WorkplaceObservation.Marker>();markers.add(marker(1,WorkplaceObservation.Kind.BARREL));markers.add(marker(2,WorkplaceObservation.Kind.SMITHING_TABLE));
        for(int x=3;x<13;x++)markers.add(marker(x,WorkplaceObservation.Kind.CRAFTING_TABLE));observe(1000,markers.toArray(WorkplaceObservation.Marker[]::new));
        var opportunities=new WorkplaceOpportunities(db.getConnection());assertTrue(opportunities.review(town,1000,100,Set.of(world),1).candidates().isEmpty());
        assertEquals("blacksmith",opportunities.review(town,1000,100,Set.of(world),1,Set.of(WorkplaceObservation.Kind.SMITHING_TABLE)).candidates().getFirst().industry());
        assertEquals("warehouse",opportunities.review(town,1000,100,Set.of(world),1,Set.of(WorkplaceObservation.Kind.BARREL)).candidates().getFirst().industry());
        assertTrue(opportunities.review(town,1000,100,Set.of(world),1,Set.of(WorkplaceObservation.Kind.CRAFTING_TABLE)).candidates().isEmpty());
        assertThrows(IllegalArgumentException.class,()->opportunities.review(town,1000,100,Set.of(world),1,Set.of()));
    }
}
