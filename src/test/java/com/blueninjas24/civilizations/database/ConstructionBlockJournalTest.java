package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionBlockJournalTest {
    @TempDir Path directory;
    private final UUID world=UUID.randomUUID(),town=UUID.randomUUID();
    private Connection open()throws Exception {
        var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("journal.sqlite"));
        try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}new SchemaMigrator().migrate(c);return c;
    }
    private ConstructionDraft fixture(Connection c)throws Exception {
        try(var s=c.prepareStatement("INSERT INTO settlements(id,name,world,center_x,center_y,center_z,discovered_at,population,beds) VALUES(?,'Town','test',0,64,0,1,0,0)")){s.setString(1,town.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO observed_worlds VALUES(?,'test')")){s.setString(1,world.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,town.toString());s.setString(2,world.toString());s.executeUpdate();}
        return new ConstructionDraftRepository(c).save(new ConstructionDraft(UUID.randomUUID(),town,world,new Position(-17,64,31),new Transform(Rotation.CLOCKWISE_90,true),
                ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_path.yml")),1000));
    }
    @Test void interruptedBeforeWriteAndAfterWriteRecoverWithoutChangingIntentOrSkippingWork()throws Exception {
        UUID operation=UUID.randomUUID();ConstructionDraft draft;
        try(var c=open()) {
            draft=fixture(c);var journal=new ConstructionBlockJournal(c);var intent=journal.prepare(operation,draft.id(),0,"minecraft:cobblestone");
            assertEquals(new Position(-17,64,35),intent.at());assertEquals(ConstructionBlockJournal.State.PREPARED,intent.state());
        }
        try(var c=open()) {
            var journal=new ConstructionBlockJournal(c);var intent=journal.find(operation).orElseThrow();
            assertEquals(ConstructionRecovery.Action.WRITE,ConstructionRecovery.inspect(intent.before(),intent.after(),"minecraft:air",true,true,true,true));
            // Emulate a server crash after its physical write but before the separate SQL acknowledgement.
            assertEquals(ConstructionRecovery.Action.ACKNOWLEDGE,ConstructionRecovery.inspect(intent.before(),intent.after(),"minecraft:cobblestone",true,true,true,true));
            assertEquals(ConstructionBlockJournal.State.APPLIED,journal.acknowledge(operation,"minecraft:cobblestone").state());
            assertEquals(ConstructionBlockJournal.State.APPLIED,journal.prepare(operation,draft.id(),0,"minecraft:cobblestone").state());
            assertEquals(ConstructionBlockJournal.State.PREPARED,journal.prepare(UUID.randomUUID(),draft.id(),1,"minecraft:cobblestone").state());
        }
    }
    @Test void changedBlocksPausePermanentlyWithoutOverwriteAndIdentityReuseRollsBack()throws Exception {
        try(var c=open()) {
            var draft=fixture(c);var journal=new ConstructionBlockJournal(c);UUID op=UUID.randomUUID();
            var intent=journal.prepare(op,draft.id(),0,"minecraft:cobblestone");
            assertThrows(SQLException.class,()->journal.prepare(UUID.randomUUID(),draft.id(),1,"minecraft:cobblestone"));
            assertThrows(SQLException.class,()->journal.prepare(op,draft.id(),1,"minecraft:cobblestone"));
            assertThrows(IllegalArgumentException.class,()->journal.prepare(UUID.randomUUID(),draft.id(),0,"minecraft:tnt"));
            assertTrue(c.getAutoCommit());assertEquals(intent,journal.find(op).orElseThrow());
            assertEquals(ConstructionRecovery.Action.CONFLICT,ConstructionRecovery.inspect(intent.before(),intent.after(),"minecraft:diamond_block",true,true,true,true));
            assertEquals(ConstructionBlockJournal.State.CONFLICT,journal.acknowledge(op,"minecraft:diamond_block").state());
            assertThrows(SQLException.class,()->journal.acknowledge(op,"minecraft:cobblestone"));
            assertThrows(SQLException.class,()->journal.prepare(UUID.randomUUID(),draft.id(),1,"minecraft:cobblestone"));
        }
    }
    @Test void recoveryWaitsForWorldChunkAuthorityAndEntitySafetyWithoutRequestingBlocks() {
        for(int missing=0;missing<4;missing++) {
            boolean[] flags={true,true,true,true};flags[missing]=false;
            assertEquals(ConstructionRecovery.Action.WAIT,ConstructionRecovery.inspect("minecraft:air","minecraft:cobblestone",null,flags[0],flags[1],flags[2],flags[3]));
        }
    }
    @Test void migration100PreservesFrozenDraftAndCorruptIntentCannotReachAWriter()throws Exception {
        try(var c=open()) {
            var draft=fixture(c);try(var s=c.createStatement()){s.execute("DROP TABLE construction_block_journal");s.execute("PRAGMA user_version=100");}
            new SchemaMigrator().migrate(c);assertEquals(draft,new ConstructionDraftRepository(c).find(draft.id()).orElseThrow());
            var journal=new ConstructionBlockJournal(c);var op=UUID.randomUUID();journal.prepare(op,draft.id(),0,"minecraft:cobblestone");
            try(var s=c.createStatement()){s.execute("UPDATE construction_block_journal SET world_x=1234");}
            assertThrows(SQLException.class,()->journal.find(op));
        }
    }
}
