package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.Source;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.BusinessOperationPolicy;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionVillageProcurementTest {
    @TempDir Path directory;
    ConstructionSupplyOrdersTest f;
    @BeforeEach void setup()throws Exception{
        f=new ConstructionSupplyOrdersTest();f.directory=directory;f.setup();
        f.f.sql("UPDATE treasuries SET hundredths=100000"); // Explicit fixture capital, never application seeding.
    }
    @AfterEach void close(){f.close();}
    ConstructionVillageProcurement procurement(){return new ConstructionVillageProcurement(f.c(),ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype(),f.runtime,BusinessOperationPolicy.prototype());}
    ConstructionVillageProcurement.Result buy()throws Exception{return procurement().purchaseRemaining(f.publicDraft.id(),f.proof(f.publicDraft),3001,Set.of(f.f.world));}
    void supply()throws Exception{f.f.sql("UPDATE industry_stock SET units=50 WHERE business_id='"+f.seller+"'");}
    int orders()throws Exception{try(var s=f.c().createStatement();var r=s.executeQuery("SELECT count(*) FROM construction_supply_orders")){return r.getInt(1);}}

    @Test void buysExactInputsConservesMoneyAndRestartNeverOrdersThemAgain()throws Exception{
        supply();long total=f.money(),cash=f.f.cash();
        var result=buy();assertEquals(3,result.purchases().size());assertEquals(2940,cash-f.f.cash());assertEquals(total,f.money());
        assertTrue(f.orders().stock(f.publicDraft.id()).isEmpty(),"Paid cargo is not yet delivered stock");
        var saved=new TreeMap<Commodity,Long>();for(var receipt:result.purchases())saved.merge(receipt.request().commodity(),receipt.request().units(),Math::addExact);
        assertEquals(f.publicDraft.template().materials(),saved);assertEquals(3,orders());
        assertTrue(buy().purchases().isEmpty());f.f.db.close();f.f.open();assertTrue(buy().purchases().isEmpty());assertEquals(3,orders());assertEquals(total,f.money());
    }
    @Test void missingOneInputSpendsNothingThenSplitsActualStockBetweenQualifiedSuppliers()throws Exception{
        long cash=f.money(),treasury=f.f.cash();assertThrows(SQLException.class,this::buy);assertEquals(0,orders());assertEquals(treasury,f.f.cash());
        f.f.sql("UPDATE industry_stock SET units=16 WHERE business_id='"+f.buyer+"' AND commodity='timber'");
        var result=buy();assertEquals(4,result.purchases().size());assertEquals(cash,f.money());
        assertEquals(36,result.purchases().stream().filter(r->r.request().commodity().equals(Commodity.TIMBER)).mapToLong(r->r.request().units()).sum());
    }
    @Test void lateReceiptFailureRollsBackAllEarlierMaterialsAndPayments()throws Exception{
        supply();long total=f.money(),cash=f.f.cash();
        f.f.sql("CREATE TRIGGER reject_last_material AFTER INSERT ON construction_supply_orders WHEN NEW.commodity='wool' BEGIN SELECT RAISE(ABORT,'fixture last material failure'); END");
        assertThrows(SQLException.class,this::buy);assertEquals(0,orders());assertEquals(cash,f.f.cash());assertEquals(total,f.money());
        assertTrue(f.c().getAutoCommit());assertEquals(50,f.f.db.getIndustryRepository().store(f.seller).orElseThrow().units().get(Commodity.STONE));
        f.f.sql("DROP TRIGGER reject_last_material");assertEquals(3,buy().purchases().size());
    }
    @Test void priorPaidCargoCountsAndCannotBeBoughtTwice()throws Exception{
        supply();f.orders().purchase(f.request(Source.VILLAGE,2),f.proof(f.publicDraft),3001,Set.of(f.f.world));
        var result=buy();assertEquals(10,result.purchases().stream().filter(r->r.request().commodity().equals(Commodity.STONE)).mapToLong(r->r.request().units()).sum());
        assertEquals(4,orders());assertTrue(buy().purchases().isEmpty());
    }
    @Test void absentStaleUnfedAndUnstaffedEvidenceCannotPurchase()throws Exception{
        supply();assertThrows(SQLException.class,()->procurement().purchaseRemaining(f.publicDraft.id(),f.proof(f.publicDraft),3001,Set.of()));
        assertThrows(SQLException.class,()->procurement().purchaseRemaining(f.publicDraft.id(),f.proof(f.publicDraft),9000,Set.of(f.f.world)));
        f.f.sql("UPDATE needs_periods SET fulfilled=0 WHERE period=2");assertThrows(SQLException.class,this::buy);
        f.f.sql("UPDATE needs_periods SET fulfilled=demand WHERE period=2");
        f.f.sql("UPDATE business_employment SET ended_step=1,end_reason='Fixture departure' WHERE ended_step IS NULL");assertThrows(SQLException.class,this::buy);assertEquals(0,orders());
    }
    @Test void sharedHaulAndProtectedBudgetPreventPartialShopping()throws Exception{
        supply();f.runtime=f.policy(4);assertThrows(SQLException.class,this::buy);assertEquals(0,orders());
        f.runtime=f.policy(128);f.f.sql("UPDATE treasuries SET hundredths=3500");assertThrows(SQLException.class,this::buy);assertEquals(0,orders());
    }
}
