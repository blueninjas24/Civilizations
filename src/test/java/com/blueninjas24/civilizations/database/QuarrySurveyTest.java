package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.blueninjas24.civilizations.database.MineralOpportunities.Status.*;

class QuarrySurveyTest {
    @TempDir Path directory;
    DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),person=UUID.randomUUID();
    final VillageObservation.Position stoneAt=new VillageObservation.Position(0,40,0);
    final VillageObservation.Position sandAt=new VillageObservation.Position(8,40,0);

    List<MineralObservation> quarries(long time, List<VillageObservation.Position> stone,
                                      List<VillageObservation.Position> sand) {
        return List.of(new MineralObservation(world,time,true,Commodity.STONE,stone),
                new MineralObservation(world,time,true,Commodity.SAND,sand));
    }
    MineralOpportunities quarries(){return MineralOpportunities.quarries(db.getConnection());}
    MineralOpportunities minerals(){return new MineralOpportunities(db.getConnection());}
    MineralOpportunities.Review quarryReview(long now,long lifetime,Set<UUID> worlds)throws SQLException {
        return quarries().review(town,now,lifetime,worlds);
    }
    void sql(String sql)throws SQLException {try(var s=db.getConnection().createStatement()){s.execute(sql);}}

    @BeforeEach void open()throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("QuarrySurveyTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");
        db.getVillageIdentityRepository().bindSettlement(town,world);
        new OreTrackingRepository(db.getConnection()).begin(0);
    }
    @AfterEach void close(){if(db!=null)db.close();}

    @Test void quarryAndCoalIronSurveysKeepIndependentHistoryAndCurrentMembership()throws Exception {
        var coalIron=List.of(new MineralObservation(world,10,true,Commodity.COAL,List.of(
                        new VillageObservation.Position(30,40,0))),
                new MineralObservation(world,10,true,Commodity.IRON,List.of()));
        minerals().observe(town,coalIron);
        quarries().observe(town,quarries(10,List.of(stoneAt),List.of(sandAt)));

        assertEquals(CURRENT,quarryReview(10,10,Set.of(world)).status());
        assertEquals(2,quarryReview(10,10,Set.of(world)).candidates().size());
        assertEquals(1,minerals().review(town,10,10,Set.of(world)).candidates().size());
        assertEquals(List.of(Commodity.STONE,Commodity.SAND),quarries().read(town,512).stream()
                .map(MineralOpportunities.Evidence::mineral).toList());

        quarries().observe(town,quarries(20,List.of(),List.of()));
        assertTrue(quarryReview(20,10,Set.of(world)).candidates().isEmpty());
        assertEquals(20,quarryReview(20,10,Set.of(world)).capturedMillis());
        assertEquals(10,minerals().review(town,10,10,Set.of(world)).capturedMillis());
        quarries().observe(town,quarries(15,List.of(stoneAt),List.of()));
        assertEquals(20,quarryReview(20,10,Set.of(world)).capturedMillis());
        assertTrue(quarryReview(20,10,Set.of(world)).candidates().isEmpty());
        assertEquals(2,quarries().read(town,512).size());
    }

    @Test void quarryReaderRequiresStoneSandPairAndRejectsCoordinateReassignment()throws Exception {
        var reader=MineralSurveyReader.quarries(db.getConnection());
        assertEquals(2,reader.read(town,quarries(10,List.of(stoneAt),List.of(sandAt))).size());
        var coalIron=List.of(new MineralObservation(world,10,true,Commodity.COAL,List.of()),
                new MineralObservation(world,10,true,Commodity.IRON,List.of()));
        assertThrows(IllegalArgumentException.class,()->reader.read(town,coalIron));
        assertThrows(SQLException.class,()->reader.read(town,quarries(10,List.of(stoneAt),List.of(stoneAt))));
        var partial=new MineralObservation(world,10,false,Commodity.STONE,List.of());
        assertThrows(SQLException.class,()->reader.read(town,List.of(partial,
                new MineralObservation(world,10,true,Commodity.SAND,List.of()))));
    }

    @Test void exclusionsClaimsAndUnavailableOrExpiredEvidenceStayConservative()throws Exception {
        new OrePlacementRepository(db.getConnection()).record(world,
                List.of(new VillageObservation.Position(1,40,0)),5);
        quarries().observe(town,quarries(10,List.of(stoneAt,new VillageObservation.Position(1,40,0)),List.of(sandAt)));
        assertEquals(2,quarries().read(town,512).size());
        assertEquals(CURRENT,quarryReview(10,10,Set.of(world)).status());

        db.getPopulationRepository().insertResident(new Resident(person,"Founder",town,null,300,Resident.Status.ALIVE));
        var site=new BusinessSite(UUID.randomUUID(),town,world,0,40,0,10,"Saved quarry deposit fixture");
        var business=new Business(UUID.randomUUID(),town,"Fixture quarry",person,person,"quarry",site.id(),4,0);
        var requirements=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(business,site,requirements,requirements,0,"Test fixture"));
        var discovery=new MineralDiscoveryRepository(db.getConnection()).discover(site.id(),
                new MineralObservation(world,10,true,Commodity.STONE,List.of(stoneAt)),2,Map.of(Commodity.STONE,1L));
        assertEquals(1,quarryReview(10,10,Set.of(world)).candidates().size());
        assertEquals(discovery.deposit().id(),quarries().read(town,512).stream()
                .filter(e->e.position().equals(stoneAt)).findFirst().orElseThrow().savedDeposit());

        assertEquals(INACTIVE_WORLD,quarryReview(10,10,Set.of()).status());
        assertEquals(STALE,quarryReview(21,10,Set.of(world)).status());
        new OreTrackingRepository(db.getConnection()).markGap(11);
        assertEquals(TRACKING_GAP,quarryReview(11,10,Set.of(world)).status());
        assertTrue(quarryReview(11,10,Set.of(world)).candidates().isEmpty());
        assertEquals(2,quarries().read(town,512).size());
    }

    @Test void exactCaptureReplaySucceedsAndConflictingReplayRollsBack()throws Exception {
        var pair=quarries(10,List.of(stoneAt),List.of());
        quarries().observe(town,pair);var before=quarryReview(10,10,Set.of(world));
        quarries().observe(town,pair);
        assertEquals(before,quarryReview(10,10,Set.of(world)));
        assertThrows(SQLException.class,()->quarries().observe(town,
                quarries(10,List.of(sandAt),List.of())));
        assertEquals(before,quarryReview(10,10,Set.of(world)));
        assertEquals(1,quarries().read(town,512).size());
        assertTrue(db.getConnection().getAutoCommit());
    }

    @Test void schema90UpgradePreservesEveryOldTableAndRowAcrossRollbackAndRetry()throws Exception {
        minerals().observe(town,List.of(new MineralObservation(world,10,true,Commodity.COAL,List.of()),
                new MineralObservation(world,10,true,Commodity.IRON,List.of())));
        SchemaFixtures.rewind(db.getConnection(),90);
        var before=oldTableRows();
        sql("CREATE TABLE quarry_survey_members (blocker INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        assertEquals(90,scalar("PRAGMA user_version"));
        assertEquals(before,oldTableRows());
        assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='quarry_opportunities'"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='quarry_surveys'"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='quarry_founder_reviews'"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='quarry_founder_courses'"));
        sql("DROP TABLE quarry_survey_members");

        new SchemaMigrator().migrate(db.getConnection());
        assertEquals(SchemaMigrator.CURRENT_VERSION,scalar("PRAGMA user_version"));
        assertEquals(before,oldTableRows());
        for(var table:List.of("quarry_opportunities","forestry_work","forestry_supports","forestry_habitat_changes","forestry_resources","forestry_tracking","forestry_exclusions","forestry_source_losses","quarry_surveys","quarry_survey_members",
                "quarry_founder_reviews","quarry_founder_courses"))assertEquals(0,scalar("SELECT COUNT(*) FROM "+table),table);
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }

    @Test void emptyNeighborChunkCannotEraseAProspectButEmptyOriginalChunkCan()throws Exception {
        var stone=new Position(1,40,1);
        quarries().observeChunk(town,List.of(new MineralObservation(world,10,true,Commodity.STONE,List.of(stone)),new MineralObservation(world,10,true,Commodity.SAND,List.of())),0,0);
        quarries().observeChunk(town,List.of(new MineralObservation(world,20,true,Commodity.STONE,List.of()),new MineralObservation(world,20,true,Commodity.SAND,List.of())),1,0);
        var active=quarries().review(town,20,1000,Set.of(world));assertEquals(List.of(stone),active.candidates().stream().map(MineralOpportunities.Evidence::position).toList());
        assertEquals(10,active.capturedMillis(),"Neighbor capture never makes an old prospect fresher");
        quarries().observeChunk(town,List.of(new MineralObservation(world,30,true,Commodity.STONE,List.of()),new MineralObservation(world,30,true,Commodity.SAND,List.of())),0,0);
        assertTrue(quarries().review(town,30,1000,Set.of(world)).candidates().isEmpty());
        assertThrows(IllegalArgumentException.class,()->quarries().observeChunk(town,List.of(new MineralObservation(world,40,true,Commodity.STONE,List.of(stone)),new MineralObservation(world,40,true,Commodity.SAND,List.of())),1,0));
    }

    @Test void expiredHistoricalChunkScopesCannotExhaustCurrentReviewBudget()throws Exception {
        try(var s=db.getConnection().prepareStatement("WITH RECURSIVE n(x) AS (VALUES(1) UNION ALL SELECT x+1 FROM n WHERE x<65) INSERT INTO quarry_surveys(settlement_id,world_id,scope_x,scope_z,captured_millis,fingerprint) SELECT ?,?,100+x,100,1,? FROM n")) {
            s.setString(1,town.toString());s.setString(2,world.toString());s.setString(3,"0".repeat(64));assertEquals(65,s.executeUpdate());
        }
        var p=new Position(1,40,1);
        quarries().observeChunk(town,List.of(new MineralObservation(world,100,true,Commodity.STONE,List.of(p)),new MineralObservation(world,100,true,Commodity.SAND,List.of())),0,0);
        assertEquals(1,quarries().review(town,100,10,Set.of(world)).candidates().size());
        assertEquals(66,scalar("SELECT COUNT(*) FROM quarry_surveys"),"Historical frames are preserved");
    }

    private long scalar(String sql)throws SQLException {
        try(var s=db.getConnection().createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);}
    }
    private Map<String,List<List<String>>> oldTableRows()throws SQLException {
        var state=new TreeMap<String,List<List<String>>>();
        try(var s=db.getConnection().createStatement();var tables=s.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'quarry_%' AND name NOT LIKE 'forestry_%' ORDER BY name")) {
            var names=new ArrayList<String>();while(tables.next())names.add(tables.getString(1));
            for(var name:names) {
                var rows=new ArrayList<List<String>>();
                try(var query=db.getConnection().createStatement();var data=query.executeQuery("SELECT * FROM \""+name+"\"")) {
                    int columns=data.getMetaData().getColumnCount();
                    while(data.next()) {
                        var row=new ArrayList<String>();for(int i=1;i<=columns;i++)row.add(Objects.toString(data.getObject(i),null));
                        rows.add(Collections.unmodifiableList(row));
                    }
                }
                rows.sort(Comparator.comparing(Object::toString));state.put(name,List.copyOf(rows));
            }
        }
        return state;
    }
}
