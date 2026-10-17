package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PenProtectionLifecycleTest {
    @TempDir Path directory;
    DatabaseManager db;
    final UUID world=new UUID(0,1), owner=new UUID(0,2), penId=new UUID(0,3), animal=new UUID(0,4);
    @BeforeEach void open()throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("PenProtectionLifecycleTest"));db.initialize();
        db.getVillageIdentityRepository().observeWorld(world,"Lifecycle fixture");
    }
    @AfterEach void close(){db.close();}
    PenProtectionRepository pens(){return new PenProtectionRepository(db.getConnection());}
    PenProtectionLifecycle lifecycle(){return new PenProtectionLifecycle(db.getConnection());}
    PenProtection pen(UUID id,int offset,Set<UUID> animals){
        var scan=new PenScan(p->Math.abs(p.x()-offset)==2||Math.abs(p.z())==2?PenScan.Cell.BARRIER:PenScan.Cell.INTERIOR,
                new PenScan.Column(offset,0),new PenScan.Limits(4,16));while(!scan.complete())scan.advance(8);
        return new PenProtection(id,world,owner,64,offset+3,64,0,new PenScan.Column(offset,0),scan.evidence(),animals,1000);
    }
    PenProtectionLifecycle.Release release(UUID pen){return new PenProtectionLifecycle.Release(new UUID(0,10),pen,owner,2000,"Owner released enclosure");}
    void sql(String query)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    long scalar(String query)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getLong(1);}}
    void integrity()throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}assertTrue(db.getConnection().getAutoCommit());}

    @Test void releaseSurvivesRestartRetainsHistoryAndDoesNotReleaseAnimals()throws Exception{
        var pen=pen(penId,0,Set.of(animal));pens().save(pen,true);
        var ownership=new AnimalOwnershipRepository(db.getConnection()).load(animal).orElseThrow();
        var request=release(penId);assertEquals(request,lifecycle().release(request,false));
        assertTrue(pens().at(world,64,0,0).isEmpty());assertEquals(pen,pens().load(penId).orElseThrow());
        assertEquals(21,scalar("SELECT COUNT(*) FROM protected_pen_columns"));
        db.close();open();assertEquals(request,lifecycle().released(penId).orElseThrow());
        assertEquals(request,lifecycle().release(request,false));assertEquals(1,scalar("SELECT COUNT(*) FROM pen_releases"));
        assertEquals(ownership,new AnimalOwnershipRepository(db.getConnection()).load(animal).orElseThrow());integrity();
    }
    @Test void oldProtectionRetryCannotReclaimReleasedArea()throws Exception{
        var old=pen(penId,0,Set.of(animal));pens().save(old,true);lifecycle().release(release(penId),false);
        var replacement=pen(new UUID(0,20),0,Set.of());pens().save(replacement,true);
        assertEquals(old,pens().save(old,true));assertEquals(List.of(replacement.id()),pens().atAll(world,64,0,0));
        assertEquals(2,scalar("SELECT COUNT(*) FROM protected_pens"));assertEquals(42,scalar("SELECT COUNT(*) FROM protected_pen_columns"));
        assertThrows(SQLException.class,()->sql("INSERT INTO active_pen_claims VALUES('"+penId+"')"));integrity();
    }
    @Test void neighboursCanShareFencesAndReleaseOnlyTheirOwnArea()throws Exception{
        var left=pen(penId,0,Set.of());var right=pen(new UUID(0,20),4,Set.of());
        pens().save(left,true);pens().save(right,true);
        assertEquals(List.of(left.id(),right.id()),pens().atAll(world,64,2,0));
        assertEquals(Optional.of(left.id()),pens().at(world,64,2,0));
        assertThrows(UnsupportedOperationException.class,()->pens().atAll(world,64,2,0).clear());
        lifecycle().release(release(penId),false);assertEquals(List.of(right.id()),pens().atAll(world,64,2,0));
        assertEquals(Optional.of(right.id()),pens().at(world,64,4,0));integrity();
    }
    @Test void interiorAgainstExistingBoundaryAndBoundaryAgainstExistingInteriorReject()throws Exception{
        pens().save(pen(penId,0,Set.of()),true);
        for(int offset:List.of(1,2,3))assertThrows(PenProtectionLifecycle.Declined.class,()->pens().save(pen(new UUID(0,20+offset),offset,Set.of(animal)),true));
        assertEquals(1,scalar("SELECT COUNT(*) FROM protected_pens"));assertEquals(0,scalar("SELECT COUNT(*) FROM animal_ownership"));integrity();
    }
    @Test void onlySavedOwnerOrFreshAdminAuthorityCanRelease()throws Exception{
        pens().save(pen(penId,0,Set.of()),true);
        var stranger=new PenProtectionLifecycle.Release(new UUID(0,10),penId,new UUID(0,99),2000,"Admin resolution");
        assertThrows(PenProtectionLifecycle.Declined.class,()->lifecycle().release(stranger,false));
        assertTrue(lifecycle().released(penId).isEmpty());assertEquals(Optional.of(penId),pens().at(world,64,0,0));
        assertEquals(stranger,lifecycle().release(stranger,true));
        assertThrows(PenProtectionLifecycle.Declined.class,()->lifecycle().release(stranger,false));integrity();
    }
    @Test void changedReleaseTermsAndCrossPenOperationReuseReject()throws Exception{
        pens().save(pen(penId,0,Set.of()),true);var second=pen(new UUID(0,20),8,Set.of());pens().save(second,true);
        var request=release(penId);lifecycle().release(request,false);
        assertThrows(PenProtectionLifecycle.Declined.class,()->lifecycle().release(new PenProtectionLifecycle.Release(new UUID(0,11),penId,owner,2000,"Changed operation"),false));
        assertThrows(PenProtectionLifecycle.Declined.class,()->lifecycle().release(release(second.id()),false));
        assertEquals(Optional.of(second.id()),pens().at(world,64,8,0));assertEquals(1,scalar("SELECT COUNT(*) FROM pen_releases"));integrity();
    }
    @Test void unknownPenOldTimestampInvalidTermsAndNestedTransactionCannotRelease()throws Exception{
        assertThrows(PenProtectionLifecycle.Declined.class,()->lifecycle().release(release(penId),false));
        pens().save(pen(penId,0,Set.of()),true);
        assertThrows(PenProtectionLifecycle.Declined.class,()->lifecycle().release(new PenProtectionLifecycle.Release(new UUID(0,10),penId,owner,999,"Too early"),false));
        assertThrows(IllegalArgumentException.class,()->new PenProtectionLifecycle.Release(new UUID(0,10),penId,owner,-1,"bad"));
        assertThrows(IllegalArgumentException.class,()->new PenProtectionLifecycle.Release(new UUID(0,10),penId,owner,2000," "));
        db.getConnection().setAutoCommit(false);assertThrows(SQLException.class,()->lifecycle().release(release(penId),false));
        assertFalse(db.getConnection().getAutoCommit());db.getConnection().rollback();db.getConnection().setAutoCommit(true);
        assertEquals(Optional.of(penId),pens().at(world,64,0,0));integrity();
    }
    @Test void lateReleaseFailureRollsBackReceiptAndActiveArea()throws Exception{
        pens().save(pen(penId,0,Set.of(animal)),true);
        sql("CREATE TRIGGER reject_release AFTER DELETE ON active_pen_claims BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->lifecycle().release(release(penId),false));
        assertTrue(lifecycle().released(penId).isEmpty());assertEquals(Optional.of(penId),pens().at(world,64,0,0));integrity();
        sql("DROP TRIGGER reject_release");lifecycle().release(release(penId),false);assertTrue(pens().at(world,64,0,0).isEmpty());
    }
    @Test void lateNewPenFailureDoesNotReserveArea()throws Exception{
        sql("CREATE TRIGGER reject_roster AFTER INSERT ON protected_pen_animals BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->pens().save(pen(penId,0,Set.of(animal)),true));
        assertEquals(0,scalar("SELECT COUNT(*) FROM active_pen_claims"));assertEquals(0,scalar("SELECT COUNT(*) FROM protected_pen_columns"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM animal_ownership"));integrity();
    }
    @Test void migration87PreservesEveryPenColumnAndAnimalAndBackfillsActiveClaims()throws Exception{
        var pen=pen(penId,0,Set.of(animal));pens().save(pen,true);
        SchemaFixtures.rewind(db.getConnection(),87);
        assertEquals(87,scalar("PRAGMA user_version"));new SchemaMigrator().migrate(db.getConnection());
        assertEquals(SchemaMigrator.CURRENT_VERSION,scalar("PRAGMA user_version"));assertEquals(pen,pens().load(penId).orElseThrow());
        assertEquals(Optional.of(penId),pens().at(world,64,0,0));assertEquals(1,scalar("SELECT COUNT(*) FROM active_pen_claims"));
        assertEquals(0,scalar("SELECT COUNT(*) FROM pen_releases"));assertEquals(21,scalar("SELECT COUNT(*) FROM protected_pen_columns"));
        assertEquals(owner,new AnimalOwnershipRepository(db.getConnection()).load(animal).orElseThrow().player());integrity();
    }
    @Test void migration87FailureAfterRebuildRollsBackOriginalConstraintAndAllRows()throws Exception{
        var pen=pen(penId,0,Set.of(animal));pens().save(pen,true);
        var other=pen(new UUID(0,20),8,Set.of());pens().save(other,true);SchemaFixtures.rewind(db.getConnection(),87);
        sql("CREATE VIEW pen_releases AS SELECT 1");assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        assertEquals(87,scalar("PRAGMA user_version"));assertEquals(pen,pens().load(penId).orElseThrow());
        assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE name IN ('active_pen_claims','pen_columns_v88')"));
        assertThrows(SQLException.class,()->sql("INSERT INTO protected_pen_columns VALUES('"+other.id()+"','"+world+"',64,0,0,'INTERIOR')"));
        sql("DROP VIEW pen_releases");new SchemaMigrator().migrate(db.getConnection());assertEquals(Optional.of(penId),pens().at(world,64,0,0));integrity();
    }
}
