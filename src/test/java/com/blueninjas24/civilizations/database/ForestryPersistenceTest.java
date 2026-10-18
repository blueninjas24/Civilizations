package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ForestryPersistenceTest {
    @TempDir Path directory;BusinessStartupGrantsTest data;
    BusinessStartupGrants.WorkshopRequest request;
    List<Position> trees;
    @BeforeEach void setup()throws Exception {
        data=new BusinessStartupGrantsTest();data.directory=directory;data.setup();
        request=data.producer("logging",data.person,20,1000);
        trees=java.util.stream.IntStream.range(0,6).mapToObj(y->new Position(22,40+y,2)).toList();
    }
    @AfterEach void close(){if(data!=null&&data.db!=null)data.db.close();}
    ForestObservation evidence(){return new ForestObservation(data.world,10,true,trees);}
    ForestryRepository forests(){return new ForestryRepository(data.db.getConnection());}
    BusinessStartupGrants.Forestry start()throws Exception{return data.grants().startForestry(request,evidence(),ForestryPolicy.prototype(),StartupGrantPolicy.prototype());}
    UUID site(){return request.founding().site().id();}
    UUID firm(){return request.founding().business().id();}

    @Test void paidOpeningAndWorkSurviveRestartReplayAndRetuningWithoutRefill()throws Exception {
        var opened=start();assertEquals(8500,data.cash());assertEquals(96,opened.resource().stock().available());
        assertEquals(0,opened.startup().store().units().values().stream().mapToLong(Long::longValue).sum());
        UUID operation=UUID.randomUUID();var work=data.db.getIndustryRepository().log(operation,firm(),4,1,ForestryPolicy.prototype());
        assertEquals(4,work.quantity());var saved=forests().find(site()).orElseThrow();assertEquals(92,saved.stock().available());
        data.db.close();data.open();
        var replay=data.grants().startForestry(request,evidence(),ForestryPolicy.disabled(),StartupGrantPolicy.disabled());
        assertEquals(saved,replay.resource());assertEquals(8500,data.cash());assertEquals(1,data.count("startup_grants"));
        assertEquals(work,data.db.getIndustryRepository().log(operation,firm(),4,1,ForestryPolicy.prototype()));
        var retuned=new ForestryPolicy(true,1,999,100000,1,1,1,1);
        assertEquals(saved,forests().open(site(),evidence(),retuned));
        assertEquals(4,data.db.getIndustryRepository().store(firm()).orElseThrow().units().values().stream().mapToLong(Long::longValue).sum());
        assertThrows(SQLException.class,()->data.db.getIndustryRepository().log(UUID.randomUUID(),firm(),4,1,ForestryPolicy.prototype()));
        assertEquals(1,data.count("forestry_work"));
    }

    @Test void overlappingSupportsWrongWorldOrLateAuditFailureCannotCreatePartialAssets()throws Exception {
        data.sql("CREATE TRIGGER reject_forest_support BEFORE INSERT ON forestry_supports BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,this::start);data.unchanged();assertEquals(0,data.count("forestry_resources"));
        data.sql("DROP TRIGGER reject_forest_support");start();
        var second=data.producer("logging",data.person,25,1000);
        data.db.getBusinessRepository().found(second.founding());
        var otherSite=second.founding().site().id();
        assertThrows(SQLException.class,()->forests().open(otherSite,evidence(),ForestryPolicy.prototype()));
        assertTrue(forests().find(otherSite).isEmpty());assertEquals(6,data.count("forestry_supports"));
        assertThrows(SQLException.class,()->forests().open(site(),new ForestObservation(UUID.randomUUID(),20,true,trees),ForestryPolicy.prototype()));
        assertEquals(1,data.count("forestry_resources"));
    }

    @Test void authoritativeCompleteHabitatLossAndReturnPreserveHistoryAndNeverRefill()throws Exception {
        start();var verdicts=new TreeMap<Position,Boolean>(Position.ORDER);trees.forEach(p->verdicts.put(p,p.y()<43));
        var partial=new HashMap<>(verdicts);partial.remove(trees.getFirst());
        assertThrows(SQLException.class,()->forests().habitat(UUID.randomUUID(),site(),data.world,20,partial));
        var id=UUID.randomUUID();var loss=forests().habitat(id,site(),data.world,20,verdicts);
        assertEquals(48,loss.lost());assertEquals(loss,forests().habitat(id,site(),data.world,20,verdicts));
        verdicts.replaceAll((p,value)->true);
        assertThrows(SQLException.class,()->forests().habitat(id,site(),data.world,20,verdicts));
        assertThrows(SQLException.class,()->forests().habitat(UUID.randomUUID(),site(),data.world,20,verdicts));
        var restored=forests().habitat(UUID.randomUUID(),site(),data.world,30,verdicts);
        assertEquals(96,restored.after().capacity());assertEquals(48,restored.after().available());
        assertEquals(trees,forests().supports(site()));assertEquals(2,data.count("forestry_habitat_changes"));
        data.sql("CREATE TRIGGER reject_habitat_audit BEFORE INSERT ON forestry_habitat_changes BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        verdicts.replaceAll((p,value)->false);var before=forests().find(site()).orElseThrow();
        assertThrows(SQLException.class,()->forests().habitat(UUID.randomUUID(),site(),data.world,40,verdicts));
        assertEquals(before,forests().find(site()).orElseThrow());
    }

    @Test void schema91UpgradePreservesEveryPredecessorTableAcrossFailureAndRetry()throws Exception {
        // Retain real paid firms/stores/employment while proving that migration never retrofits a forest.
        data.grants().startWorkshop(request,StartupGrantPolicy.prototype());
        SchemaFixtures.rewind(data.db.getConnection(),91);var before=snapshot();
        data.sql("CREATE TABLE forestry_supports(blocker INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(data.db.getConnection()));
        assertEquals(91,scalar("PRAGMA user_version"));assertEquals(before,snapshot());
        assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE name='forestry_resources'"));
        data.sql("DROP TABLE forestry_supports");new SchemaMigrator().migrate(data.db.getConnection());
        assertEquals(SchemaMigrator.CURRENT_VERSION,scalar("PRAGMA user_version"));assertEquals(before,snapshot());
        for(var name:List.of("forestry_resources","forestry_tracking","forestry_exclusions","forestry_source_losses","forestry_founder_reviews","forestry_founder_courses","forestry_supports","forestry_work","forestry_habitat_changes"))assertEquals(0,data.count(name),name);
        try(var s=data.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    private long scalar(String query)throws SQLException {
        try(var s=data.db.getConnection().createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getLong(1);}
    }
    private Map<String,List<List<String>>> snapshot()throws SQLException {
        var result=new TreeMap<String,List<List<String>>>();var names=new ArrayList<String>();
        try(var s=data.db.getConnection().createStatement();var r=s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'forestry_%' AND name NOT IN ('resource_recovery_epochs','mineral_recoveries','forest_recoveries') ORDER BY name")){while(r.next())names.add(r.getString(1));}
        for(var name:names) {
            var rows=new ArrayList<List<String>>();
            try(var s=data.db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM \""+name+"\"")) {
                while(r.next()){var row=new ArrayList<String>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.add(Objects.toString(r.getObject(i),null));rows.add(Collections.unmodifiableList(row));}
            }rows.sort(Comparator.comparing(Object::toString));result.put(name,List.copyOf(rows));
        }return result;
    }
}
