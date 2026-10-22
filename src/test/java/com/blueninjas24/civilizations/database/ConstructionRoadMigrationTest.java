package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionRoadMigrationTest {
    @TempDir Path directory;
    private Connection open()throws SQLException{var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("upgrade.sqlite"));sql(c,"PRAGMA foreign_keys=ON");return c;}
    private static void sql(Connection c,String sql)throws SQLException{try(var s=c.createStatement()){s.execute(sql);}}
    private static long number(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    private static String schema(Connection c)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery("SELECT sql FROM sqlite_master WHERE name='construction_block_journal'")){return r.getString(1);}}
    private List<ConstructionBlockJournal.Intent> oldSave(Connection c)throws Exception{
        SchemaMigrator.throughVersion(116).migrate(c);UUID town=UUID.randomUUID(),world=UUID.randomUUID();
        sql(c,"INSERT INTO settlements VALUES('"+town+"','Retained town','fixture',0,64,0,1,0,0)");sql(c,"INSERT INTO observed_worlds VALUES('"+world+"','fixture')");sql(c,"INSERT INTO settlement_worlds VALUES('"+town+"','"+world+"')");
        var plan=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_path.yml"));
        var draft=new ConstructionDraft(UUID.randomUUID(),town,world,new Position(10,64,10),Transform.identity(),plan,1000);new ConstructionDraftRepository(c).save(draft);
        var journal=new ConstructionBlockJournal(c);UUID first=UUID.randomUUID();journal.prepare(first,draft.id(),0,"minecraft:cobblestone");journal.acknowledge(first,"minecraft:cobblestone");
        return List.of(journal.find(first).orElseThrow(),journal.prepare(UUID.randomUUID(),draft.id(),1,"minecraft:cobblestone"));
    }
    private static void preserved(Connection c,List<ConstructionBlockJournal.Intent> old)throws SQLException{
        for(var intent:old)assertEquals(intent,new ConstructionBlockJournal(c).find(intent.operation()).orElseThrow());
        assertEquals(1,number(c,"SELECT count(*) FROM construction_drafts"));assertEquals(2,number(c,"SELECT count(*) FROM construction_block_journal"));
        assertEquals(0,number(c,"SELECT count(*) FROM pragma_foreign_key_check"));assertEquals(1,number(c,"PRAGMA foreign_keys"));
    }
    @Test void upgradesPopulated116WithoutChangingAnySavedIntentAndReopens()throws Exception{
        List<ConstructionBlockJournal.Intent> old;
        try(var c=open()){
            old=oldSave(c);new SchemaMigrator().migrate(c);preserved(c,old);assertEquals(117,number(c,"PRAGMA user_version"));
            assertEquals(0,number(c,"SELECT count(*) FROM construction_road_ground"));assertEquals(1,number(c,"SELECT count(*) FROM sqlite_master WHERE type='index' AND name='construction_authored_positions'"));
            assertThrows(SQLException.class,()->sql(c,"INSERT INTO construction_block_journal SELECT '"+UUID.randomUUID()+"',draft_id,2,12,64,10,'minecraft:dirt',after_state,'PREPARED' FROM construction_block_journal LIMIT 1"));
        }
        try(var c=open()){new SchemaMigrator().migrate(c);preserved(c,old);assertEquals(0,number(c,"SELECT count(*) FROM construction_road_ground"));}
    }
    @Test void lateMigrationFailureRollsBackTableRebuildIndexVersionAndHistory()throws Exception{
        try(var c=open()){
            var old=oldSave(c);var before=schema(c);var migrations=new ArrayList<SchemaMigrator.Migration>();for(int i=0;i<116;i++)migrations.add(connection->{});
            migrations.add(connection->{ConstructionRoadIntents.migrate(connection);throw new SQLException("fixture late117 failure");});
            assertThrows(SQLException.class,()->new SchemaMigrator(migrations).migrate(c));assertTrue(c.getAutoCommit());
            assertEquals(116,number(c,"PRAGMA user_version"));assertEquals(before,schema(c));preserved(c,old);
            assertEquals(0,number(c,"SELECT count(*) FROM sqlite_master WHERE name='construction_road_ground' OR name='construction_block_journal_v116'"));
            new SchemaMigrator().migrate(c);preserved(c,old);assertEquals(117,number(c,"PRAGMA user_version"));
        }
    }
}
