package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HouseholdFormationTest {
    @TempDir Path directory;
    private final UUID town=id(1),world=id(2);
    private static UUID id(int n){return new UUID(0,n);}
    private void sql(Connection c,String text)throws SQLException{try(var s=c.createStatement()){s.execute(text);}}
    private long count(Connection c,String table)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){return r.getLong(1);}}
    private Connection open()throws Exception{var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("formation.sqlite"));sql(c,"PRAGMA foreign_keys=ON");new SchemaMigrator().migrate(c);return c;}
    private void seed(Connection c)throws Exception{
        sql(c,"INSERT INTO settlements VALUES('"+town+"','Fixture','fixture',0,64,0,1,0,0)");
        sql(c,"INSERT INTO observed_worlds VALUES('"+world+"','fixture')");sql(c,"INSERT INTO settlement_worlds VALUES('"+town+"','"+world+"')");
        new DemographicRepository(c).initializeClock(42,PopulationPolicy.prototype());
    }
    private void person(Connection c,int n,long age,UUID household)throws Exception{new PopulationRepository(c).insertResident(new Resident(id(n),"Fixture "+n,town,household,age,Resident.Status.ALIVE));}
    @Test void singleAndExistingPartnersFormOnceUnderSharedMonthCapWithNoInventedRelations()throws Exception{
        UUID pairHome;
        try(var c=open()){
            seed(c);person(c,10,240,null);person(c,11,240,null);person(c,12,240,null);person(c,13,240,null);person(c,14,20,null);
            var people=new PopulationRepository(c);people.insertFamilyTie(new FamilyTie(id(11),id(12),FamilyTie.Type.PARTNER));
            var policy=HouseholdFormationPolicy.prototype();var formation=new HouseholdFormation(c);
            assertEquals(2,formation.form(town,world,216,0,1000,policy));assertEquals(0,formation.form(town,world,216,0,1001,policy));
            var solo=people.findResident(id(10)).orElseThrow().householdId();pairHome=people.findResident(id(11)).orElseThrow().householdId();
            assertNotNull(solo);assertNotEquals(solo,pairHome);assertEquals(pairHome,people.findResident(id(12)).orElseThrow().householdId());
            assertNull(people.findResident(id(13)).orElseThrow().householdId());assertNull(people.findResident(id(14)).orElseThrow().householdId());
            assertEquals(1,count(c,"family_ties"));assertEquals(2,count(c,"household_formations"));assertEquals(3,count(c,"household_formation_members"));
            assertEquals(0,HousingCapacity.read(c,town).get(solo));assertEquals(5,count(c,"residents"));
            sql(c,"DELETE FROM household_capacity"); // A later village observation refreshes raw bed-group evidence.
            assertEquals(0,HousingCapacity.read(c,town).get(solo));assertEquals(0,HousingCapacity.read(c,town).get(pairHome));
            sql(c,"UPDATE demographic_clock SET completed_steps=1");assertEquals(1,formation.form(town,world,216,1,1100,policy));
            assertThrows(BuildingRegistry.Declined.class,()->formation.form(town,world,216,0,1101,policy));
        }
        try(var c=open()){assertEquals(pairHome,new PopulationRepository(c).findResident(id(11)).orElseThrow().householdId());assertEquals(3,count(c,"household_formations"));
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}}
    }
    @Test void existingHomesChildrenAndAmbiguousPartnersRemainUntouchedAndFailureRollsBack()throws Exception{
        try(var c=open()){
            seed(c);var people=new PopulationRepository(c);var existing=new Household(id(50),town,"Existing");people.insertHousehold(existing);
            person(c,10,240,existing.id());person(c,11,240,null);person(c,12,240,null);person(c,13,20,null);person(c,20,240,null);
            people.insertFamilyTie(new FamilyTie(id(10),id(11),FamilyTie.Type.PARTNER));people.insertFamilyTie(new FamilyTie(id(12),id(13),FamilyTie.Type.PARENT_CHILD));
            var formation=new HouseholdFormation(c);var policy=HouseholdFormationPolicy.prototype();
            sql(c,"CREATE TRIGGER fail_formation BEFORE INSERT ON household_formation_members BEGIN SELECT RAISE(ABORT,'injected'); END");
            assertThrows(SQLException.class,()->formation.form(town,world,216,0,1000,policy));
            assertEquals(1,count(c,"households"));assertEquals(0,count(c,"household_formations"));assertNull(people.findResident(id(20)).orElseThrow().householdId());assertTrue(c.getAutoCommit());
            sql(c,"DROP TRIGGER fail_formation");assertEquals(1,formation.form(town,world,216,0,1000,policy));
            assertEquals(existing.id(),people.findResident(id(10)).orElseThrow().householdId());
            for(int n:List.of(11,12,13))assertNull(people.findResident(id(n)).orElseThrow().householdId());
            assertEquals(2,count(c,"family_ties"));assertEquals(0,formation.form(town,world,216,0,1001,HouseholdFormationPolicy.disabled()));
        }
    }
    @Test void migrationFrom110RetainsSavedPeopleAndAddsOnlyEmptyFormationHistory()throws Exception{
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("upgrade.sqlite"))){
            sql(c,"PRAGMA foreign_keys=ON");SchemaMigrator.throughVersion(110).migrate(c);seed(c);person(c,10,240,null);
            new SchemaMigrator().migrate(c);assertEquals(1,count(c,"residents"));assertEquals(0,count(c,"household_formations"));assertEquals(0,count(c,"household_formation_members"));
            assertNull(new PopulationRepository(c).findResident(id(10)).orElseThrow().householdId());
        }
    }
}
