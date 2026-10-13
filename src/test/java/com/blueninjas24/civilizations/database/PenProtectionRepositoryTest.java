package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PenProtectionRepositoryTest {
    @TempDir Path directory; DatabaseManager db;
    final UUID world=new UUID(0,1),owner=new UUID(0,2),id=new UUID(0,3),animal=new UUID(0,4);
    @BeforeEach void open()throws Exception {db=new DatabaseManager(directory.toFile(),Logger.getLogger("PenProtectionRepositoryTest"));db.initialize();db.getVillageIdentityRepository().observeWorld(world,"Pen fixture");}
    @AfterEach void close(){db.close();}
    PenProtectionRepository repo(){return new PenProtectionRepository(db.getConnection());}
    PenScan.Evidence geometry(){var scan=new PenScan(p->Math.abs(p.x())==2||Math.abs(p.z())==2?PenScan.Cell.BARRIER:PenScan.Cell.INTERIOR,new PenScan.Column(0,0),new PenScan.Limits(4,16));while(!scan.complete())scan.advance(4);return scan.evidence();}
    PenProtection pen(UUID identity,UUID player,Set<UUID> animals){return new PenProtection(identity,world,player,64,3,64,0,new PenScan.Column(0,0),geometry(),animals,1000);}
    long scalar(String sql)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);}}
    void sql(String sql)throws Exception{try(var s=db.getConnection().createStatement()){s.execute(sql);}}
    @Test void completePenAndAnimalsPersistAndExactRetriesDoNotRepeat()throws Exception {
        var pen=pen(id,owner,Set.of(animal));repo().save(pen,true);assertEquals(pen,repo().save(pen,true));
        assertEquals(9,scalar("SELECT COUNT(*) FROM protected_pen_columns WHERE kind='INTERIOR'"));assertEquals(12,scalar("SELECT COUNT(*) FROM protected_pen_columns WHERE kind='BOUNDARY'"));
        db.close();open();assertEquals(pen,repo().load(id).orElseThrow());assertEquals(owner,new AnimalOwnershipRepository(db.getConnection()).load(animal).orElseThrow().player());
        assertEquals(Optional.of(id),repo().at(world,64,0,0));assertTrue(repo().at(world,64,20,20).isEmpty());assertTrue(repo().at(world,65,0,0).isEmpty());
    }
    @Test void lackOfCertifiedAuthorityCannotWriteAnyClaim()throws Exception {
        assertThrows(SQLException.class,()->repo().save(pen(id,owner,Set.of(animal)),false));assertEquals(0,scalar("SELECT COUNT(*) FROM protected_pens"));assertEquals(0,scalar("SELECT COUNT(*) FROM animal_ownership"));
    }
    @Test void changedOwnerOrMembershipCannotOverwriteAnExistingPen()throws Exception {
        var p=pen(id,owner,Set.of(animal));repo().save(p,true);
        assertThrows(SQLException.class,()->repo().save(pen(id,new UUID(0,99),Set.of(animal)),true));
        assertThrows(SQLException.class,()->repo().save(pen(id,owner,Set.of()),true));assertEquals(p,repo().load(id).orElseThrow());
    }
    @Test void overlappingClaimsRollBackWithoutTakingAnimals()throws Exception {
        repo().save(pen(id,owner,Set.of(animal)),true);UUID otherAnimal=new UUID(0,5);
        assertThrows(SQLException.class,()->repo().save(pen(new UUID(0,6),owner,Set.of(otherAnimal)),true));
        assertEquals(1,scalar("SELECT COUNT(*) FROM protected_pens"));assertTrue(new AnimalOwnershipRepository(db.getConnection()).load(otherAnimal).isEmpty());assertTrue(db.getConnection().getAutoCommit());
    }
    @Test void anotherOwnersAnimalCancelsWholePenAndEarlierAnimalWrites()throws Exception {
        var store=new AnimalOwnershipRepository(db.getConnection());var protectedAnimal=new AnimalOwnershipRepository.Claim(animal,world,LivestockAdoption.Ownership.PLAYER,new UUID(0,99),null,900,"Other owner fixture");store.record(protectedAnimal);
        UUID unclaimed=new UUID(0,0);assertThrows(SQLException.class,()->repo().save(pen(id,owner,Set.of(unclaimed,animal)),true));
        assertTrue(repo().load(id).isEmpty());assertTrue(store.load(unclaimed).isEmpty());assertEquals(protectedAnimal,store.load(animal).orElseThrow());
    }
    @Test void alreadyOwnedAnimalRetainsOriginalProvenanceAndLateFailureRollsBack()throws Exception {
        var store=new AnimalOwnershipRepository(db.getConnection());var old=new AnimalOwnershipRepository.Claim(animal,world,LivestockAdoption.Ownership.PLAYER,owner,null,900,"Original fixture ownership");store.record(old);
        sql("CREATE TRIGGER reject_pen_roster AFTER INSERT ON protected_pen_animals BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->repo().save(pen(id,owner,Set.of(animal)),true));assertTrue(repo().load(id).isEmpty());assertEquals(old,store.load(animal).orElseThrow());
        sql("DROP TRIGGER reject_pen_roster");repo().save(pen(id,owner,Set.of(animal)),true);assertEquals(old,store.load(animal).orElseThrow());
    }
    @Test void migration74FailureIsAtomicAndRetriesWithoutInventingPens()throws Exception {
        var old=new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(animal,world,LivestockAdoption.Ownership.PLAYER,owner,null,900,"Migration fixture"));
        SchemaFixtures.rewind(db.getConnection(),74);sql("CREATE VIEW protected_pen_columns AS SELECT 1");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));assertEquals(74,scalar("PRAGMA user_version"));assertEquals(0,scalar("SELECT COUNT(*) FROM sqlite_master WHERE name='protected_pens'"));assertEquals(old,new AnimalOwnershipRepository(db.getConnection()).load(animal).orElseThrow());
        sql("DROP VIEW protected_pen_columns");new SchemaMigrator().migrate(db.getConnection());assertEquals(SchemaMigrator.CURRENT_VERSION,scalar("PRAGMA user_version"));assertEquals(0,scalar("SELECT COUNT(*) FROM protected_pens"));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void disconnectedForgedGeometryOrDistantSignCannotBeProtected() {
        var broken=new PenScan.Evidence(PenScan.State.ENCLOSED,Set.of(new PenScan.Column(0,0)),Set.of(new PenScan.Column(1,0)));
        assertThrows(IllegalArgumentException.class,()->new PenProtection(id,world,owner,64,2,64,0,new PenScan.Column(0,0),broken,Set.of(),1000));
        assertThrows(IllegalArgumentException.class,()->new PenProtection(id,world,owner,64,100,64,0,new PenScan.Column(0,0),geometry(),Set.of(),1000));
    }
}
