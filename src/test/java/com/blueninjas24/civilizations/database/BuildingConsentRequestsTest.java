package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.BuildingConsentRequests;
import com.blueninjas24.civilizations.application.BuildingConsentRequests.*;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.BuildingRegistry.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BuildingConsentRequestsTest {
    @TempDir Path directory;
    private Connection c;private BuildingRegistry registry;private BuildingConsentRequests requests;
    private final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),player=UUID.randomUUID();
    private Context context(){return new Context(player,world,true,true,false,Set.of(world));}
    @BeforeEach void setup()throws Exception {
        c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("consent.sqlite"));
        try(var s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");}new SchemaMigrator().migrate(c);
        try(var s=c.prepareStatement("INSERT INTO settlements VALUES(?,'Fixture','world',0,64,0,1,3,4)")){s.setString(1,town.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO observed_worlds VALUES(?,'Fixture')")){s.setString(1,world.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,town.toString());s.setString(2,world.toString());s.executeUpdate();}
        registry=new BuildingRegistry(c);requests=new BuildingConsentRequests(registry,new Policy(2,1000));
    }
    @AfterEach void close()throws Exception{if(c!=null)c.close();}
    private BuildingLayout home(int x) {
        var bounds=new Bounds(new Position(x,64,0),new Position(x+4,67,4));
        var entry=new Position(x,65,0);var unit=new Unit(UUID.randomUUID(),entry,List.of(bounds),List.of(new Bed(new Position(x+1,65,2),Direction.EAST)));
        return new BuildingLayout(UUID.randomUUID(),town,world,Kind.HOUSING,bounds,bounds,entry,List.of(),List.of(unit));
    }
    private long count(String table)throws Exception{try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){return r.getLong(1);}}

    @Test void ordinaryPlayerCanDeclareOwnershipOnlyAtExplicitConfirmationAndRetriesSaveOnce()throws Exception {
        var layout=home(10);var preview=requests.donation(context(),layout,1000);
        assertEquals(0,count("building_records"));assertTrue(preview.explanation().contains("belongs to you"));
        assertThrows(Declined.class,()->requests.confirm(context(),preview.token(),false,1100));
        assertEquals(0,count("building_records"));
        var receipt=requests.confirm(context(),preview.token(),true,1200);
        assertFalse(receipt.administrator());assertEquals(player,registry.find(layout.id()).orElseThrow().donor());
        assertEquals(receipt,requests.confirm(context(),preview.token(),true,1300));
        assertEquals(1,count("building_consent_events"));assertTrue(registry.hasDonationPermission(layout.id()));
        try(var s=c.createStatement();var r=s.executeQuery("SELECT population,beds FROM settlements")){assertEquals(3,r.getInt(1));assertEquals(4,r.getInt(2));}
    }

    @Test void anotherPlayerWrongWorldLostPermissionExpiryAndCancellationCannotConfirm()throws Exception {
        var layout=home(10);var preview=requests.donation(context(),layout,1000);
        for(var other:List.of(new Context(UUID.randomUUID(),world,true,true,false,Set.of(world)),
                new Context(player,UUID.randomUUID(),true,true,false,Set.of(world)),
                new Context(player,world,false,true,false,Set.of(world)),new Context(player,world,true,true,false,Set.of())))
            assertThrows(Declined.class,()->requests.confirm(other,preview.token(),true,1100));
        assertThrows(Declined.class,()->requests.confirm(context(),UUID.randomUUID(),true,1100));
        assertThrows(Declined.class,()->requests.confirm(context(),preview.token(),true,2000));
        var next=requests.donation(context(),layout,2100);requests.cancel(player);
        assertThrows(Declined.class,()->requests.confirm(context(),next.token(),true,2200));assertEquals(0,count("building_records"));
    }

    @Test void newProtectionBetweenPreviewAndConfirmationIsRecheckedInSaveTransaction()throws Exception {
        var layout=home(10);var preview=requests.donation(context(),layout,1000);
        var exclusion=new Exclusion(UUID.randomUUID(),world,player,layout.plot(),1050);
        registry.exclude(exclusion,Authorization.playerDeclaration(player,true));
        assertThrows(Declined.class,()->requests.confirm(context(),preview.token(),true,1100));assertEquals(0,count("building_records"));
        registry.releaseExclusion(UUID.randomUUID(),exclusion.id(),Authorization.playerDeclaration(player,true),1150);
        assertEquals(1200,requests.confirm(context(),preview.token(),true,1200).atMillis());
    }

    @Test void lateDatabaseFailureRetainsFrozenRetryIdentityAndCreatesNoDuplicateRights()throws Exception {
        var layout=home(10);var preview=requests.donation(context(),layout,1000);
        try(var s=c.createStatement()){s.execute("CREATE TRIGGER fail_consent BEFORE INSERT ON building_consent_events BEGIN SELECT RAISE(ABORT,'fixture'); END");}
        var failure=assertThrows(SQLException.class,()->requests.confirm(context(),preview.token(),true,1100));assertFalse(failure instanceof Declined);
        assertEquals(0,count("building_records"));assertEquals(0,count("building_units"));assertTrue(c.getAutoCommit());
        try(var s=c.createStatement()){s.execute("DROP TRIGGER fail_consent");}
        var receipt=requests.confirm(context(),preview.token(),true,1200);assertEquals(1100,receipt.atMillis());
        assertEquals(preview.token(),receipt.operation());assertEquals(1,count("building_consent_events"));
    }

    @Test void reclaimUsesRecordedDonorOrCurrentAdminAndCannotRevokeLaterRedonation()throws Exception {
        var layout=home(10);var grant=requests.donation(context(),layout,1000);requests.confirm(context(),grant.token(),true,1050);
        var admin=UUID.randomUUID();var adminContext=new Context(admin,world,false,false,true,Set.of(world));
        var reclaim=requests.reclamation(adminContext,layout.id(),1100);
        assertThrows(Declined.class,()->requests.confirm(new Context(admin,world,false,true,false,Set.of(world)),reclaim.token(),false,1200));
        var ownerReclaim=requests.reclamation(context(),layout.id(),1150);requests.confirm(context(),ownerReclaim.token(),false,1200);
        var again=requests.donation(context(),layout,1250);requests.confirm(context(),again.token(),true,1300);
        assertThrows(Declined.class,()->requests.confirm(adminContext,reclaim.token(),false,1400));
        assertTrue(registry.hasDonationPermission(layout.id()));
        var foreign=new Context(UUID.randomUUID(),world,true,true,false,Set.of(world));
        assertThrows(Declined.class,()->requests.reclamation(foreign,layout.id(),1450));
        assertThrows(Declined.class,()->requests.donation(foreign,layout,1450));
    }

    @Test void requestBudgetExpiresAndReplacementRequiresTheNewExactToken()throws Exception {
        requests=new BuildingConsentRequests(registry,new Policy(1,1000));var layout=home(10);
        var first=requests.donation(context(),layout,1000);var replacement=requests.donation(context(),layout,1100);
        assertThrows(Declined.class,()->requests.confirm(context(),first.token(),true,1200));
        var other=new Context(UUID.randomUUID(),world,true,true,false,Set.of(world));
        assertThrows(Declined.class,()->requests.donation(other,home(100),1200));
        var afterExpiry=requests.donation(other,home(100),2100);assertNotNull(afterExpiry);
        assertThrows(Declined.class,()->requests.confirm(context(),replacement.token(),true,2150));assertEquals(0,count("building_records"));
    }
}
