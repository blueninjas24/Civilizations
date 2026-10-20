package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.BuildingRegistry.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BuildingRegistryTest {
    @TempDir Path directory;
    private final UUID town=UUID.randomUUID(), world=UUID.randomUUID(), donor=UUID.randomUUID();
    private final Authorization owner=new Authorization(donor,false,true,true);
    private Connection open() throws Exception {
        var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("registry.sqlite"));
        try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}
        new SchemaMigrator().migrate(c);return c;
    }
    private void village(Connection c) throws Exception {
        try(var s=c.prepareStatement("INSERT INTO settlements VALUES(?,'Unchanged','fixture',0,64,0,1,7,8)")){s.setString(1,town.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO observed_worlds VALUES(?,'fixture')")){s.setString(1,world.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,town.toString());s.setString(2,world.toString());s.executeUpdate();}
    }
    private static Position p(int x,int y,int z){return new Position(x,y,z);}
    private static Bounds box(int x,int y,int z,int xx,int yy,int zz){return new Bounds(p(x,y,z),p(xx,yy,zz));}
    private BuildingLayout home(int x) {
        var room=box(x,64,0,x+4,67,4);
        var unit=new Unit(UUID.randomUUID(),p(x,65,0),List.of(room),List.of(new Bed(p(x+1,65,1),Direction.EAST)));
        return new BuildingLayout(UUID.randomUUID(),town,world,Kind.HOUSING,box(x-1,63,-1,x+5,68,5),room,p(x,65,0),List.of(),List.of(unit));
    }
    private static long count(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){return r.getLong(1);}}
    private static void healthy(Connection c)throws SQLException {
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){assertEquals("ok",r.getString(1));}
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }

    @Test void donationReclaimAndRedonationSurviveReopenWithoutChangingExistingHousingOrEconomy() throws Exception {
        var layout=home(10);UUID give=UUID.randomUUID(),take=UUID.randomUUID(),again=UUID.randomUUID();
        try(var c=open()) {
            village(c);var registry=new BuildingRegistry(c);
            var initial=registry.donate(give,layout,donor,owner,null,1000);
            assertEquals(initial,registry.donate(give,layout,donor,owner,null,1000));
            assertTrue(registry.hasDonationPermission(layout.id()));
            var reclaim=registry.reclaim(take,layout.id(),owner,give,2000);
            assertEquals(Action.RECLAIM,reclaim.action());assertFalse(registry.hasDonationPermission(layout.id()));
            assertEquals(initial,registry.donate(give,layout,donor,owner,null,1000),"Old exact replay cannot restore revoked consent");
            assertFalse(registry.hasDonationPermission(layout.id()));
            registry.donate(again,layout,donor,owner,take,3000);
            assertEquals(3,count(c,"SELECT count(*) FROM building_consent_events"));
            assertEquals(7,count(c,"SELECT population FROM settlements"));assertEquals(8,count(c,"SELECT beds FROM settlements"));
            assertEquals(0,count(c,"SELECT count(*) FROM economy_accounts"));
        }
        try(var c=open()) {
            var registry=new BuildingRegistry(c);var saved=registry.find(layout.id()).orElseThrow();
            assertEquals(layout,saved.layout());assertEquals(donor,saved.donor());assertEquals(1000,saved.createdMillis());
            assertEquals(again,saved.latest().operation());assertTrue(registry.hasDonationPermission(layout.id()));healthy(c);
        }
    }

    @Test void unauthorizedUnconfirmedStaleAndChangedRequestsFailClosed() throws Exception {
        try(var c=open()) {
            village(c);var registry=new BuildingRegistry(c);var layout=home(10);var give=UUID.randomUUID();
            for(var bad:List.of(new Authorization(donor,false,false,true),new Authorization(donor,false,true,false),
                    new Authorization(UUID.randomUUID(),false,true,true)))
                assertThrows(SQLException.class,()->registry.donate(give,layout,donor,bad,null,1000));
            registry.donate(give,layout,donor,owner,null,1000);
            assertThrows(SQLException.class,()->registry.reclaim(UUID.randomUUID(),layout.id(),owner,null,2000));
            assertThrows(SQLException.class,()->registry.reclaim(UUID.randomUUID(),layout.id(),owner,give,999));
            assertThrows(SQLException.class,()->registry.reclaim(UUID.randomUUID(),layout.id(),new Authorization(UUID.randomUUID(),false,true,true),give,2000));
            assertThrows(SQLException.class,()->registry.donate(give,layout,donor,owner,null,1001));
            assertThrows(SQLException.class,()->registry.donate(UUID.randomUUID(),layout,donor,owner,give,2000));
            assertEquals(1,count(c,"SELECT count(*) FROM building_consent_events"));assertTrue(c.getAutoCommit());
            var admin=new Authorization(UUID.randomUUID(),true,false,true);
            registry.reclaim(UUID.randomUUID(),layout.id(),admin,give,2000);
            assertFalse(registry.hasDonationPermission(layout.id()));
        }
    }

    @Test void exclusionsPauseConsentAndBlockConstructionUntilAuthorizedReleaseWithoutErasingHistory() throws Exception {
        try(var c=open()) {
            village(c);var registry=new BuildingRegistry(c);var layout=home(10);var give=UUID.randomUUID();
            registry.donate(give,layout,donor,owner,null,1000);
            var exclusion=new Exclusion(UUID.randomUUID(),world,donor,layout.plot(),2000);
            assertEquals(exclusion,registry.exclude(exclusion,owner));assertEquals(exclusion,registry.exclude(exclusion,owner));
            assertFalse(registry.hasDonationPermission(layout.id()));
            assertThrows(SQLException.class,()->registry.donate(UUID.randomUUID(),home(100),donor,owner,give,2000));
            var scope=new ConstructionPreview.Bounds(p(9,0,-1),p(15,100,5));
            assertEquals(49,new ConstructionLandRepository(c).protectedColumns(world,scope).size());
            assertTrue(new ConstructionLandRepository(c).protectedColumns(UUID.randomUUID(),scope).isEmpty());
            assertThrows(SQLException.class,()->registry.releaseExclusion(UUID.randomUUID(),exclusion.id(),new Authorization(UUID.randomUUID(),false,false,true),3000));
            var release=UUID.randomUUID();registry.releaseExclusion(release,exclusion.id(),owner,3000);registry.releaseExclusion(release,exclusion.id(),owner,3000);
            assertTrue(registry.hasDonationPermission(layout.id()));
            registry.reclaim(UUID.randomUUID(),layout.id(),owner,give,4000);
            assertEquals(49,new ConstructionLandRepository(c).protectedColumns(world,scope).size(),"Reclaimed player building still excludes construction");
            assertEquals(1,count(c,"SELECT count(*) FROM building_exclusions"));assertEquals(1,count(c,"SELECT count(*) FROM building_exclusion_releases"));healthy(c);
        }
    }

    @Test void unregisteredExclusionBlocksDonationAndReleasesOnlyItsOwnColumns() throws Exception {
        try(var c=open()) {
            village(c);var registry=new BuildingRegistry(c);var layout=home(10);
            var exclusion=new Exclusion(UUID.randomUUID(),world,donor,layout.plot(),1000);registry.exclude(exclusion,owner);
            assertThrows(SQLException.class,()->registry.donate(UUID.randomUUID(),layout,donor,owner,null,2000));
            assertTrue(registry.find(layout.id()).isEmpty());
            var scope=new ConstructionPreview.Bounds(p(9,0,-1),p(15,100,5));
            assertEquals(49,new ConstructionLandRepository(c).protectedColumns(world,scope).size());
            registry.releaseExclusion(UUID.randomUUID(),exclusion.id(),owner,2000);
            assertTrue(new ConstructionLandRepository(c).protectedColumns(world,scope).isEmpty());
            registry.exclude(exclusion,owner);
            assertTrue(new ConstructionLandRepository(c).protectedColumns(world,scope).isEmpty(),"Old protection replay cannot undo its later release");
            registry.donate(UUID.randomUUID(),layout,donor,owner,null,3000);
        }
    }

    @Test void donationCannotBypassSavedPensOrReservedConstructionEvenWithConfirmedOwnership() throws Exception {
        try(var c=open()) {
            village(c);var registry=new BuildingRegistry(c);var pen=UUID.randomUUID();
            try(var s=c.prepareStatement("INSERT INTO protected_pens VALUES(?,?,?,64,10,64,1,10,1,1000)")) {
                s.setString(1,pen.toString());s.setString(2,world.toString());s.setString(3,donor.toString());s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO protected_pen_columns VALUES(?,?,64,10,1,'INTERIOR')")) {
                s.setString(1,pen.toString());s.setString(2,world.toString());s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO active_pen_claims VALUES(?)")){s.setString(1,pen.toString());s.executeUpdate();}
            assertThrows(SQLException.class,()->registry.donate(UUID.randomUUID(),home(10),donor,owner,null,1000));
            var stranger=UUID.randomUUID();
            assertThrows(Declined.class,()->registry.exclude(new Exclusion(UUID.randomUUID(),world,stranger,home(10).plot(),1000),Authorization.playerDeclaration(stranger,true)));
            var template=com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_path.yml"));
            var draft=new ConstructionDraft(UUID.randomUUID(),town,world,p(100,64,0),Transform.identity(),template,1000);
            new ConstructionDraftRepository(c).save(draft);
            new ConstructionSiteReservations(c).reserve(draft.id(),new ConstructionMaterials.Authority(draft.id(),world,1000,true,true),1000);
            assertThrows(SQLException.class,()->registry.donate(UUID.randomUUID(),home(100),donor,owner,null,1000));
            assertThrows(Declined.class,()->registry.exclude(new Exclusion(UUID.randomUUID(),world,donor,home(100).plot(),1000),owner));
            assertEquals(0,count(c,"SELECT count(*) FROM building_records"));
            assertEquals(1,count(c,"SELECT count(*) FROM active_pen_claims"));
            assertEquals(1,count(c,"SELECT count(*) FROM construction_site_reservations"));healthy(c);
        }
    }

    @Test void playerDeclaredExclusionCannotOverrideAnotherSavedBuildingOrExclusionButOwnResizeIsAllowed()throws Exception {
        try(var c=open()) {
            village(c);var registry=new BuildingRegistry(c);var layout=home(10);
            registry.donate(UUID.randomUUID(),layout,donor,owner,null,1000);
            var stranger=UUID.randomUUID();var claim=new Exclusion(UUID.randomUUID(),world,stranger,layout.plot(),2000);
            assertThrows(Declined.class,()->registry.exclude(claim,Authorization.playerDeclaration(stranger,true)));
            var first=new Exclusion(UUID.randomUUID(),world,donor,home(100).plot(),2000);registry.exclude(first,owner);
            assertThrows(Declined.class,()->registry.exclude(new Exclusion(UUID.randomUUID(),world,stranger,first.bounds(),2000),Authorization.playerDeclaration(stranger,true)));
            var replacement=new Exclusion(UUID.randomUUID(),world,donor,first.bounds(),2100);registry.exclude(replacement,owner);
            registry.releaseExclusion(UUID.randomUUID(),first.id(),owner,2200);
            assertEquals(List.of(replacement.id()),registry.exclusionsAt(world,102,2));
            assertTrue(registry.hasDonationPermission(layout.id()));healthy(c);
        }
    }

    @Test void overlapWrongWorldAndLateEventFailureRollBackLayoutUnitsAndConsentTogether() throws Exception {
        try(var c=open()) {
            village(c);var registry=new BuildingRegistry(c);var first=home(10);registry.donate(UUID.randomUUID(),first,donor,owner,null,1000);
            assertThrows(SQLException.class,()->registry.donate(UUID.randomUUID(),home(12),donor,owner,null,1000));
            var different=home(100);var wrong=new BuildingLayout(different.id(),town,UUID.randomUUID(),different.kind(),different.plot(),different.structure(),different.entrance(),different.shared(),different.units());
            assertThrows(SQLException.class,()->registry.donate(UUID.randomUUID(),wrong,donor,owner,null,1000));
            try(var s=c.createStatement()){s.execute("CREATE TRIGGER reject_building_receipt BEFORE INSERT ON building_consent_events BEGIN SELECT RAISE(ABORT,'injected late failure'); END");}
            var operation=UUID.randomUUID();assertThrows(SQLException.class,()->registry.donate(operation,different,donor,owner,null,2000));
            assertTrue(registry.find(different.id()).isEmpty());assertEquals(1,count(c,"SELECT count(*) FROM building_units"));assertTrue(c.getAutoCommit());
            try(var s=c.createStatement()){s.execute("DROP TRIGGER reject_building_receipt");}
            registry.donate(operation,different,donor,owner,null,2000);assertEquals(2,count(c,"SELECT count(*) FROM building_units"));healthy(c);
        }
    }

    @Test void corruptLayoutOrUnitIndexCannotProvidePermission() throws Exception {
        try(var c=open()) {
            village(c);var registry=new BuildingRegistry(c);var layout=home(10);registry.donate(UUID.randomUUID(),layout,donor,owner,null,1000);
            try(var s=c.createStatement()){s.executeUpdate("UPDATE building_units SET declared_beds=2");}
            assertThrows(SQLException.class,()->registry.hasDonationPermission(layout.id()));
            try(var s=c.createStatement()){s.executeUpdate("UPDATE building_units SET declared_beds=1");s.executeUpdate("UPDATE building_records SET layout_hash='"+"0".repeat(64)+"'");}
            assertThrows(SQLException.class,()->registry.find(layout.id()));
        }
    }

    @Test void migration108Preserves107DataAndRollsBackBeforeRetry() throws Exception {
        try(var c=open()) {
            village(c);SchemaFixtures.rewind(c,107);
            try(var s=c.createStatement()){s.execute("CREATE TABLE building_units(conflict TEXT)");}
            assertThrows(SQLException.class,()->new SchemaMigrator().migrate(c));
            assertEquals(107,count(c,"PRAGMA user_version"));assertEquals(0,count(c,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='building_records'"));
            assertEquals(7,count(c,"SELECT population FROM settlements"));assertEquals(8,count(c,"SELECT beds FROM settlements"));
            try(var s=c.createStatement()){s.execute("DROP TABLE building_units");}
            new SchemaMigrator().migrate(c);assertEquals(108,count(c,"PRAGMA user_version"));
            assertEquals(0,count(c,"SELECT count(*) FROM building_records"));healthy(c);
            var registry=new BuildingRegistry(c);registry.donate(UUID.randomUUID(),home(10),donor,owner,null,1000);
        }
    }
}
