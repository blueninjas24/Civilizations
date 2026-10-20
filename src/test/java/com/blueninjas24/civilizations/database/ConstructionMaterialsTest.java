package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.economy.Commodity;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionMaterialsTest {
    @TempDir Path directory;private IndustryRepositoryTest fixture;private ConstructionDraft draft;
    @BeforeEach void setup()throws Exception {
        fixture=new IndustryRepositoryTest();fixture.directory=directory;fixture.setup();fixture.stores(64);
        fixture.sql("UPDATE industry_stock SET units=5 WHERE business_id='"+fixture.warehouse.id()+"' AND commodity='stone'");
        var template=new ConstructionTemplate("small_store",1,"Small store","Original test",Purpose.STORAGE,new Size(1,1,1),
                List.of(new Stage("base",1,Map.of(Commodity.STONE,2L),Map.of(new Position(0,0,0),"minecraft:cobblestone"))),
                List.of(new Anchor(Role.ENTRANCE,new Position(0,0,0),Direction.NORTH),new Anchor(Role.ROAD,new Position(0,-1,-1),Direction.NORTH)));
        draft=new ConstructionDraft(UUID.randomUUID(),fixture.town,fixture.world,new Position(10,64,10),Transform.identity(),template,1000);
        new ConstructionDraftRepository(fixture.database.getConnection()).save(draft);
        new ConstructionSiteReservations(fixture.database.getConnection()).reserve(draft.id(),clear(),1001);
    }
    @AfterEach void close(){fixture.close();}
    private ConstructionMaterials repository(){return new ConstructionMaterials(fixture.database.getConnection());}
    private ConstructionMaterials.Authority clear(){return new ConstructionMaterials.Authority(draft.id(),fixture.world,1000,true,true);}
    @Test void actualOwnedSuppliesMoveExactlyOnceAndSurviveRestartWithoutChargingMoneyAgain()throws Exception {
        UUID operation=UUID.randomUUID();long money=fixture.allMoney();
        var receipt=repository().reserveBusiness(operation,draft.id(),fixture.warehouse.id(),clear(),1001,ConstructionBudgetPolicy.prototype());
        assertEquals(3,fixture.quantity(fixture.warehouse,Commodity.STONE));assertEquals(2,receipt.materials().getFirst().units());
        assertEquals(5,fixture.quantity(fixture.warehouse,Commodity.STONE)+receipt.materials().getFirst().units());assertEquals(money,fixture.allMoney());
        fixture.database.close();fixture.open();
        assertEquals(receipt,repository().find(operation).orElseThrow());
        assertEquals(receipt,repository().reserveBusiness(operation,draft.id(),fixture.warehouse.id(),new ConstructionMaterials.Authority(draft.id(),fixture.world,0,false,false),9000,ConstructionBudgetPolicy.prototype()));
        assertThrows(SQLException.class,()->repository().reserveBusiness(UUID.randomUUID(),draft.id(),fixture.warehouse.id(),clear(),1001,ConstructionBudgetPolicy.prototype()));
        assertEquals(3,fixture.quantity(fixture.warehouse,Commodity.STONE));assertEquals(money,fixture.allMoney());
        assertThrows(UnsupportedOperationException.class,()->receipt.materials().clear());
    }
    @Test void incompleteSitesMissingMaterialsAndLateSqlFailureCannotPartiallyTakeSupplies()throws Exception {
        UUID operation=UUID.randomUUID();
        assertThrows(SQLException.class,()->repository().reserveBusiness(operation,draft.id(),fixture.warehouse.id(),new ConstructionMaterials.Authority(draft.id(),fixture.world,1000,true,false),1001,ConstructionBudgetPolicy.prototype()));
        assertThrows(SQLException.class,()->repository().reserveBusiness(operation,draft.id(),fixture.warehouse.id(),clear(),6001,ConstructionBudgetPolicy.prototype()));
        fixture.sql("UPDATE industry_stock SET units=1 WHERE business_id='"+fixture.warehouse.id()+"' AND commodity='stone'");
        assertThrows(SQLException.class,()->repository().reserveBusiness(operation,draft.id(),fixture.warehouse.id(),clear(),1001,ConstructionBudgetPolicy.prototype()));
        assertEquals(1,fixture.quantity(fixture.warehouse,Commodity.STONE));
        fixture.sql("UPDATE industry_stock SET units=5 WHERE business_id='"+fixture.warehouse.id()+"' AND commodity='stone'");
        fixture.sql("CREATE TRIGGER refuse_material_hold AFTER INSERT ON construction_material_items BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        assertThrows(SQLException.class,()->repository().reserveBusiness(operation,draft.id(),fixture.warehouse.id(),clear(),1001,ConstructionBudgetPolicy.prototype()));
        assertTrue(repository().find(operation).isEmpty());assertEquals(5,fixture.quantity(fixture.warehouse,Commodity.STONE));assertTrue(fixture.database.getConnection().getAutoCommit());
    }
    @Test void releasingUnusedSiteBlocksNewMaterialDebitsAndFundedSitesCannotBeReleased()throws Exception {
        var sites=new ConstructionSiteReservations(fixture.database.getConnection());sites.releaseUnused(draft.id(),1002);
        assertThrows(SQLException.class,()->repository().reserveBusiness(UUID.randomUUID(),draft.id(),fixture.warehouse.id(),clear(),1003,ConstructionBudgetPolicy.prototype()));
        assertEquals(5,fixture.quantity(fixture.warehouse,Commodity.STONE));
        var other=new ConstructionDraft(UUID.randomUUID(),draft.settlement(),draft.world(),draft.origin(),draft.transform(),draft.template(),1000);
        new ConstructionDraftRepository(fixture.database.getConnection()).save(other);sites.reserve(other.id(),new ConstructionMaterials.Authority(other.id(),fixture.world,1000,true,true),1003);
        repository().reserveBusiness(UUID.randomUUID(),other.id(),fixture.warehouse.id(),new ConstructionMaterials.Authority(other.id(),fixture.world,1000,true,true),1003,ConstructionBudgetPolicy.prototype());
        assertThrows(SQLException.class,()->sites.releaseUnused(other.id(),1004));assertTrue(sites.find(other.id()).orElseThrow().active());
    }
    @Test void unusedGoodsReturnExactlyOnceAndOnlyThenCanTheirSiteRelease()throws Exception {
        var reservation=UUID.randomUUID();long money=fixture.allMoney();
        var original=repository().reserveBusiness(reservation,draft.id(),fixture.warehouse.id(),clear(),1001,ConstructionBudgetPolicy.prototype());
        assertEquals(Map.of(Commodity.STONE,2L),repository().available(reservation));
        var operation=UUID.randomUUID();var returns=new ConstructionMaterialReturns(fixture.database.getConnection());
        var receipt=returns.returnUnused(operation,reservation,1002).receipt().orElseThrow();
        assertEquals(3,receipt.items().getFirst().beforeUnits());assertEquals(5,receipt.items().getFirst().afterUnits());
        assertEquals(5,fixture.quantity(fixture.warehouse,Commodity.STONE));assertTrue(repository().available(reservation).isEmpty());assertEquals(money,fixture.allMoney());
        assertEquals(original,repository().find(reservation).orElseThrow(),"Original transfer remains immutable history");
        assertFalse(new ConstructionSiteReservations(fixture.database.getConnection()).releaseUnused(draft.id(),1003).active());
        fixture.database.close();fixture.open();returns=new ConstructionMaterialReturns(fixture.database.getConnection());
        assertEquals(receipt,returns.returnUnused(operation,reservation,9000).receipt().orElseThrow());assertEquals(5,fixture.quantity(fixture.warehouse,Commodity.STONE));
        var reopened=returns;assertThrows(SQLException.class,()->reopened.returnUnused(UUID.randomUUID(),reservation,9001));
    }
    @Test void fullStorageRetainsAllGoodsAndLateFailureRollsBackReturnForRetry()throws Exception {
        var reservation=UUID.randomUUID();repository().reserveBusiness(reservation,draft.id(),fixture.warehouse.id(),clear(),1001,ConstructionBudgetPolicy.prototype());
        var returns=new ConstructionMaterialReturns(fixture.database.getConnection());var operation=UUID.randomUUID();
        fixture.sql("UPDATE industry_stock SET units=64 WHERE business_id='"+fixture.warehouse.id()+"' AND commodity='stone'");
        assertTrue(returns.returnUnused(operation,reservation,1002).receipt().isEmpty());assertTrue(returns.find(operation).isEmpty());
        assertEquals(Map.of(Commodity.STONE,2L),repository().available(reservation));
        fixture.sql("UPDATE industry_stock SET units=62 WHERE business_id='"+fixture.warehouse.id()+"' AND commodity='stone'");
        fixture.sql("CREATE TRIGGER refuse_return AFTER INSERT ON construction_material_returns BEGIN SELECT RAISE(ABORT,'fixture'); END");
        assertThrows(SQLException.class,()->returns.returnUnused(operation,reservation,1003));assertEquals(62,fixture.quantity(fixture.warehouse,Commodity.STONE));
        assertEquals(Map.of(Commodity.STONE,2L),repository().available(reservation));assertTrue(fixture.database.getConnection().getAutoCommit());
        fixture.sql("DROP TRIGGER refuse_return");assertTrue(returns.returnUnused(operation,reservation,1004).receipt().isPresent());assertEquals(64,fixture.quantity(fixture.warehouse,Commodity.STONE));
    }
    @Test void migration104PreservesHeldAssetsAndPreparedWorkRefusesReturns()throws Exception {
        var reservation=UUID.randomUUID();var held=repository().reserveBusiness(reservation,draft.id(),fixture.warehouse.id(),clear(),1001,ConstructionBudgetPolicy.prototype());
        var c=fixture.database.getConnection();SchemaFixtures.rewind(c,104);fixture.sql("CREATE TABLE construction_material_returns(blocker INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(c));assertEquals(held,repository().find(reservation).orElseThrow());
        fixture.sql("DROP TABLE construction_material_returns");new SchemaMigrator().migrate(c);
        assertEquals(Map.of(Commodity.STONE,2L),repository().available(reservation));
        new ConstructionBlockJournal(c).prepare(UUID.randomUUID(),draft.id(),0,"minecraft:cobblestone");
        assertThrows(SQLException.class,()->new ConstructionMaterialReturns(c).returnUnused(UUID.randomUUID(),reservation,1002));
        assertEquals(3,fixture.quantity(fixture.warehouse,Commodity.STONE));assertEquals(Map.of(Commodity.STONE,2L),repository().available(reservation));
    }
    @Test void migration101PreservesFrozenPlansAndWriteIntentsAcrossFailureAndRetry()throws Exception {
        var c=fixture.database.getConnection();var operation=UUID.randomUUID();
        var intent=new ConstructionBlockJournal(c).prepare(operation,draft.id(),0,"minecraft:cobblestone");
        SchemaFixtures.rewind(c,101);fixture.sql("CREATE TABLE construction_material_items(blocker INTEGER)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(c));
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA user_version")){assertTrue(r.next());assertEquals(101,r.getInt(1));}
        assertEquals(draft,new ConstructionDraftRepository(c).find(draft.id()).orElseThrow());
        fixture.sql("DROP TABLE construction_material_items");new SchemaMigrator().migrate(c);
        assertEquals(intent,new ConstructionBlockJournal(c).find(operation).orElseThrow());
        assertTrue(repository().find(UUID.randomUUID()).isEmpty());
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
}
