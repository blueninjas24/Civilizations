package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SheepPersistenceTest {
    @TempDir Path directory;
    LivestockRepositoryTest f;
    final Commodity wool=new Commodity("wool");
    @BeforeEach void setup()throws Exception{f=new LivestockRepositoryTest();f.directory=directory;f.setup();}
    @AfterEach void close(){f.close();}
    void openSheep()throws Exception {
        f.sql("INSERT INTO industry_stock VALUES('"+f.business.id()+"','wool',1,0)");
        f.repo().open(f.business.id(),LivestockHerd.Species.SHEEP,f.grain,8,f.roster);
    }
    @Test void sheepConsumeFeedAndWorkForWoolAndBirthsWithoutEggsAndReplayAfterRestart()throws Exception {
        openSheep();f.feed(100);var operation=new UUID(0,100);
        var first=f.advance(operation,1);assertEquals(2,first.renewableUnits());assertEquals(1,first.births());
        assertEquals(94,f.units(f.grain));assertEquals(2,f.units(wool));assertEquals(0,f.units(f.eggs));assertEquals(0,f.units(f.milk));
        f.db.close();f.reopen();assertEquals(first,f.advance(operation,1));assertEquals(2,f.units(wool));
        assertEquals(LivestockHerd.Species.SHEEP,f.repo().load(f.business.id()).orElseThrow().species());
        assertEquals(3,f.advance(new UUID(0,101),2).renewableUnits());assertEquals(5,f.units(wool));
    }
    @Test void missingFeedAbsenceAndLateFailureNeverMintWool()throws Exception {
        openSheep();assertEquals(0,f.advance(new UUID(0,100),1).renewableUnits());f.feed(100);
        var before=f.repo().load(f.business.id()).orElseThrow().state();
        assertEquals(before,f.repo().advance(new UUID(0,101),f.business.id(),2,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),false).after());
        f.sql("CREATE TRIGGER fail_sheep BEFORE INSERT ON livestock_periods WHEN NEW.period>1 BEGIN SELECT RAISE(ABORT,'fixture'); END");
        assertThrows(SQLException.class,()->f.advance(new UUID(0,102),2));assertEquals(0,f.units(wool));assertEquals(100,f.units(f.grain));
        f.sql("DROP TRIGGER fail_sheep");assertEquals(3,f.advance(new UUID(0,102),2).renewableUnits());
    }
    @Test void sheepBirthAndPositiveAwaySightKeepIndividualSavedIdentities()throws Exception {
        openSheep();f.feed(100);f.advance(new UUID(0,100),1);
        var c=f.db.getConnection();var birth=new LivestockBirths(c).page(f.business.id(),0,-1,16).getFirst();
        var claim=new LivestockBirthProjections.Claim(birth.id(),new UUID(0,150),f.world,LivestockHerd.Species.SHEEP,0,64,0,1000);
        var projections=new LivestockBirthProjections(c);assertEquals(LivestockBirthProjections.State.CLAIMED,projections.claim(claim,Set.of(f.world)).state());
        assertTrue(projections.activate(birth.id(),claim.entity()));
        var away=new LivestockPresence.Sight(new UUID(0,10),LivestockHerd.Species.SHEEP,f.world,new com.blueninjas24.civilizations.settlement.VillageObservation.Position(100,64,0),1000);
        var presence=new LivestockPresence(c);assertEquals(1,presence.record(List.of(away)));assertEquals(away,presence.find(away.animal()).orElseThrow());
        assertEquals(1,presence.unavailableAdultsRows(f.business.id(),2,LivestockWorkingArea.prototype()));
        f.db.close();f.reopen();assertEquals(claim,new LivestockBirthProjections(f.db.getConnection()).find(birth.id()).orElseThrow().claim());
    }
    /** Reconstruct the real pre103 CHECK constraints, not just an earlier version marker. */
    void legacy()throws Exception {
        var c=f.db.getConnection();f.sql("PRAGMA foreign_keys=OFF");
        for(var table:SheepMigration.TABLES) {
            String original;var extras=new ArrayList<String>();
            try(var s=c.createStatement();var r=s.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='"+table+"'")){assertTrue(r.next());original=r.getString(1);}
            try(var s=c.createStatement();var r=s.executeQuery("SELECT sql FROM sqlite_master WHERE tbl_name='"+table+"' AND type IN('index','trigger') AND sql IS NOT NULL")){while(r.next())extras.add(r.getString(1));}
            String old=original.replaceFirst("CREATE TABLE \\\"?"+table+"\\\"?","CREATE TABLE "+table+"_legacy").replace("CHECK(species IN('COW','CHICKEN','SHEEP'))","CHECK(species IN('COW','CHICKEN'))");
            f.sql(old);f.sql("INSERT INTO "+table+"_legacy SELECT * FROM "+table);f.sql("DROP TABLE "+table);f.sql("ALTER TABLE "+table+"_legacy RENAME TO "+table);
            for(var sql:extras)f.sql(sql);
        }
        f.sql("DROP TABLE construction_site_reservations");f.sql("PRAGMA user_version=102");f.sql("PRAGMA foreign_keys=ON");
    }
    @Test void actualVersion102RowsAndReceiptsSurviveConstraintUpgradeAndLateRollback()throws Exception {
        f.openHerd();f.feed(100);var op=new UUID(0,100);var before=f.advance(op,1);legacy();
        // A collision on the final table forces rollback after three parent rebuilds.
        f.sql("CREATE TABLE livestock_slaughter_sheep_upgrade(value INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(f.db.getConnection()));
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){assertEquals(102,r.getInt(1));}
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_keys")){assertEquals(1,r.getInt(1));}
        f.sql("DROP TABLE livestock_slaughter_sheep_upgrade");new SchemaMigrator().migrate(f.db.getConnection());
        assertEquals(before,f.advance(op,1));assertEquals(2,f.units(f.milk));assertEquals(94,f.units(f.grain));
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
        for(var table:SheepMigration.TABLES)try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("SELECT sql FROM sqlite_master WHERE name='"+table+"'")){assertTrue(r.next());assertTrue(r.getString(1).contains("'SHEEP'"));}
    }
}
