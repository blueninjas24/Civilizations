package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionSiteReservationsTest {
    @TempDir Path directory;PenProtectionRepositoryTest f;
    final UUID town=new UUID(0,20);
    @BeforeEach void setup()throws Exception {
        f=new PenProtectionRepositoryTest();f.directory=directory;f.open();
        f.db.saveSettlement(new Settlement(town,"Construction fixture","Pen fixture",0,64,0,1,0,0));
        f.db.getVillageIdentityRepository().bindSettlement(town,f.world);
    }
    @AfterEach void close(){f.close();}
    ConstructionSiteReservations sites(){return new ConstructionSiteReservations(f.db.getConnection());}
    ConstructionMaterials.Authority clear(ConstructionDraft draft){return new ConstructionMaterials.Authority(draft.id(),f.world,1000,true,true);}
    ConstructionDraft draft(int x,int z)throws Exception {
        var plan=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_storehouse.yml"));
        return new ConstructionDraftRepository(f.db.getConnection()).save(new ConstructionDraft(UUID.randomUUID(),town,f.world,new Position(x,64,z),Transform.identity(),plan,1000));
    }
    @Test void overlappingProjectsRefuseAndReservationSurvivesRestartUntilExplicitUnusedRelease()throws Exception {
        var a=draft(10,10);var b=draft(14,14);var receipt=sites().reserve(a.id(),clear(a),1001);
        assertThrows(SQLException.class,()->sites().reserve(b.id(),clear(b),1001));
        assertEquals(25,new ConstructionLandRepository(f.db.getConnection()).protectedColumns(f.world,receipt.footprint()).size());
        f.close();f.open();assertEquals(receipt,sites().find(a.id()).orElseThrow());
        assertEquals(receipt,sites().reserve(a.id(),new ConstructionMaterials.Authority(a.id(),UUID.randomUUID(),0,false,false),9000));
        var released=sites().releaseUnused(a.id(),2000);assertFalse(released.active());assertEquals(2000L,released.releasedMillis());
        assertEquals(released,sites().releaseUnused(a.id(),3000));assertThrows(SQLException.class,()->sites().reserve(a.id(),clear(a),1001));
        assertTrue(sites().reserve(b.id(),clear(b),1001).active());assertEquals(2,f.scalar("SELECT COUNT(*) FROM construction_site_reservations"));
    }
    @Test void staleIncompleteWrongWorldAndSavedEmptyPlayerPlotsCannotAcquireASite()throws Exception {
        var d=draft(0,0);
        assertThrows(SQLException.class,()->sites().reserve(d.id(),clear(d),6001));
        assertThrows(SQLException.class,()->sites().reserve(d.id(),new ConstructionMaterials.Authority(UUID.randomUUID(),f.world,1000,true,true),1001));
        assertThrows(SQLException.class,()->sites().reserve(d.id(),new ConstructionMaterials.Authority(d.id(),f.world,1000,false,true),1001));
        assertThrows(SQLException.class,()->sites().reserve(d.id(),new ConstructionMaterials.Authority(d.id(),UUID.randomUUID(),1000,true,true),1001));
        f.repo().save(f.pen(f.id,f.owner,Set.of()),true);
        assertThrows(SQLException.class,()->sites().reserve(d.id(),clear(d),1001));assertTrue(sites().find(d.id()).isEmpty());
    }
    @Test void newlyProtectedPlotPausesFundingAndStartedWorkCannotReleaseItsHistory()throws Exception {
        var d=draft(0,0);sites().reserve(d.id(),clear(d),1001);
        var c=f.db.getConnection();c.setAutoCommit(false);
        try{sites().requireActiveRows(d);}finally{c.rollback();c.setAutoCommit(true);}
        f.repo().save(f.pen(f.id,f.owner,Set.of()),true);
        c.setAutoCommit(false);try{assertThrows(SQLException.class,()->sites().requireActiveRows(d));}finally{c.rollback();c.setAutoCommit(true);}
        new ConstructionBlockJournal(c).prepare(UUID.randomUUID(),d.id(),0,"minecraft:cobblestone");
        assertThrows(SQLException.class,()->sites().releaseUnused(d.id(),2000));assertTrue(sites().find(d.id()).orElseThrow().active());
    }
    @Test void failedInsertionAndVersion103UpgradeLeaveNoPartialReservationOrLostDraft()throws Exception {
        var d=draft(10,10);f.sql("CREATE TRIGGER refuse_site AFTER INSERT ON construction_site_reservations BEGIN SELECT RAISE(ABORT,'fixture'); END");
        assertThrows(SQLException.class,()->sites().reserve(d.id(),clear(d),1001));assertTrue(sites().find(d.id()).isEmpty());assertTrue(f.db.getConnection().getAutoCommit());
        SchemaFixtures.rewind(f.db.getConnection(),103);f.sql("CREATE TABLE construction_site_reservations(blocker INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.db.getConnection()));
        assertEquals(103,f.scalar("PRAGMA user_version"));assertEquals(d,new ConstructionDraftRepository(f.db.getConnection()).find(d.id()).orElseThrow());
        f.sql("DROP TABLE construction_site_reservations");new SchemaMigrator().migrate(f.db.getConnection());
        assertTrue(sites().reserve(d.id(),clear(d),1001).active());
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void savedProtectionAtExternalAccessRefusesNewAndRenewedAuthorityWithoutClaimingTheRoad()throws Exception {
        // Body z=3..7 is clear; its required road anchor (0,63,2) touches the pen boundary.
        var existing=draft(-2,3);var receipt=sites().reserve(existing.id(),clear(existing),1001);
        assertEquals(3,receipt.footprint().minimum().z());
        assertFalse(new ConstructionLandRepository(f.db.getConnection()).protectedColumns(f.world,
                new ConstructionPreview.Bounds(new Position(0,63,2),new Position(0,63,2))).contains(new ConstructionLandRepository.Column(0,2)));
        f.repo().save(f.pen(f.id,f.owner,Set.of()),true);
        var c=f.db.getConnection();c.setAutoCommit(false);
        try{
            assertEquals(ConstructionWorkQueue.State.PAUSED,new ConstructionWorkQueue(c).reviewRows(existing.id()).state());
            assertThrows(SQLException.class,()->sites().requireActiveRows(existing));
        }
        finally{c.rollback();c.setAutoCommit(true);}
        // Receipt replay is historical only and must not refresh authority.
        assertEquals(receipt,sites().reserve(existing.id(),clear(existing),1002));
        sites().releaseUnused(existing.id(),2000);
        var next=draft(-2,3);
        assertThrows(SQLException.class,()->sites().reserve(next.id(),clear(next),2001));
        assertTrue(sites().find(next.id()).isEmpty());
        assertEquals(0,f.scalar("SELECT COUNT(*) FROM construction_block_journal"));
        assertEquals(0,f.scalar("SELECT COUNT(*) FROM construction_material_reservations"));
    }
    @Test void unrelatedProtectedColumnBesideAccessDoesNotExcludeTheWholeBoundingRectangle()throws Exception {
        f.repo().save(f.pen(f.id,f.owner,Set.of()),true);
        // Body starts at z=3; anchor (3,63,2) is outside the pen. Bounding corners (1,2)/(2,2) are not used.
        var d=draft(1,3);assertTrue(sites().reserve(d.id(),clear(d),1001).active());
        var c=f.db.getConnection();c.setAutoCommit(false);
        try{sites().requireActiveRows(d);}finally{c.rollback();c.setAutoCommit(true);}
    }
}
