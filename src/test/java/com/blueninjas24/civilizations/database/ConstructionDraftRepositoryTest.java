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

class ConstructionDraftRepositoryTest {
    @TempDir Path directory;
    private final UUID town=UUID.randomUUID(),world=UUID.randomUUID();
    private Connection open()throws Exception {
        var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("construction.sqlite"));
        try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}new SchemaMigrator().migrate(c);return c;
    }
    private void village(Connection c)throws Exception {
        try(var s=c.prepareStatement("INSERT INTO settlements(id,name,world,center_x,center_y,center_z,discovered_at,population,beds) VALUES(?,'Preserved town','old-name',0,64,0,1,7,8)")){s.setString(1,town.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO observed_worlds VALUES(?,'current-name')")){s.setString(1,world.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,town.toString());s.setString(2,world.toString());s.executeUpdate();}
    }
    private ConstructionDraft draft()throws Exception {
        var template=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_storehouse.yml"));
        return new ConstructionDraft(UUID.randomUUID(),town,world,new Position(-17,64,31),new Transform(Rotation.COUNTERCLOCKWISE_90,true),template,1000);
    }
    @Test void draftSurvivesReopenAndTemplateChangesWithoutAcquiringEconomicOrBlockAuthority()throws Exception {
        var draft=draft();
        try(var c=open()){village(c);var repository=new ConstructionDraftRepository(c);assertEquals(draft,repository.save(draft));assertEquals(draft,repository.save(draft));}
        try(var c=open()){
            var repository=new ConstructionDraftRepository(c);assertEquals(draft,repository.find(draft.id()).orElseThrow());
            var edited=new ConstructionTemplate(draft.template().id(),2,"Edited",draft.template().attribution(),draft.template().purpose(),draft.template().size(),draft.template().stages(),draft.template().anchors());
            assertThrows(SQLException.class,()->repository.save(new ConstructionDraft(draft.id(),town,world,draft.origin(),draft.transform(),edited,1000)));
            assertEquals(draft,repository.find(draft.id()).orElseThrow());assertTrue(c.getAutoCommit());
            try(var s=c.createStatement();var rows=s.executeQuery("SELECT population,beds FROM settlements")){assertTrue(rows.next());assertEquals(7,rows.getInt(1));assertEquals(8,rows.getInt(2));}
            try(var s=c.createStatement();var rows=s.executeQuery("SELECT count(*) FROM construction_drafts")){assertEquals(1,rows.getInt(1));}
        }
    }
    @Test void wrongWorldRollbackAndCorruptSnapshotFailClosed()throws Exception {
        try(var c=open()) {
            village(c);var repository=new ConstructionDraftRepository(c);var draft=draft();
            assertThrows(SQLException.class,()->repository.save(new ConstructionDraft(draft.id(),town,UUID.randomUUID(),draft.origin(),draft.transform(),draft.template(),1000)));
            assertTrue(c.getAutoCommit());assertTrue(repository.find(draft.id()).isEmpty());repository.save(draft);
            try(var s=c.createStatement()){s.executeUpdate("UPDATE construction_drafts SET template_hash='"+"0".repeat(64)+"'");}
            assertThrows(SQLException.class,()->repository.find(draft.id()));
        }
    }
    @Test void migrationFrom99PreservesExistingVillageAndCanReopenWithoutReset()throws Exception {
        var draft=draft();
        try(var c=open()) {
            village(c);try(var s=c.createStatement()){s.execute("DROP TABLE construction_block_journal");s.execute("DROP TABLE construction_drafts");s.execute("PRAGMA user_version=99");}
            new SchemaMigrator().migrate(c);
            try(var s=c.createStatement();var rows=s.executeQuery("PRAGMA user_version")){assertEquals(SchemaMigrator.CURRENT_VERSION,rows.getInt(1));}
            assertEquals(draft,new ConstructionDraftRepository(c).save(draft));
        }
        try(var c=open()) {
            assertEquals(draft,new ConstructionDraftRepository(c).find(draft.id()).orElseThrow());
            try(var s=c.createStatement();var rows=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(rows.next());}
            try(var s=c.createStatement();var rows=s.executeQuery("PRAGMA integrity_check")){assertTrue(rows.next());assertEquals("ok",rows.getString(1));}
        }
    }
}
