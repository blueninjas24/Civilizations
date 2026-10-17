package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockRuntimeTest {
    @TempDir Path directory;LivestockRepositoryTest f;UUID seller=new UUID(0,80);AtomicBoolean healthy=new AtomicBoolean(true);
    @BeforeEach void setup()throws Exception{
        f=new LivestockRepositoryTest();f.directory=directory;f.setup();f.openHerd();
        var site=new BusinessSite(new UUID(0,81),f.town,f.world,8,64,0,1,"Explicit supply fixture");
        var business=new Business(seller,f.town,"Feed warehouse",f.owner,f.owner,"warehouse",site.id(),1,0);
        f.db.getBusinessRepository().found(new BusinessFounding(business,site,new com.blueninjas24.civilizations.population.JobRequirements(216,Map.of()),new com.blueninjas24.civilizations.population.JobRequirements(216,Map.of()),0,"Fixture feed seller"));
        f.db.getBusinessRepository().hire(new UUID(0,82),seller,f.owner,0);
        f.db.getIndustryRepository().openStore(seller,128,Map.of(f.grain,1L),0);
        f.sql("UPDATE industry_stock SET units=40 WHERE business_id='"+seller+"' AND commodity='grain'");
        f.sql("UPDATE industry_stores SET hundredths=1000 WHERE business_id='"+f.business.id()+"'");
    }
    @AfterEach void close(){f.close();}
    IndustryRuntime runtime(){
        var policy=new IndustryRuntimePolicy(true,1000,8,IndustryPolicy.prototype(),Map.of(),new WorkshopSupplyPolicy(true,2,8,Map.of(f.grain,25L)));
        var result=new IndustryRuntime(f.db.getConnection(),policy,BusinessOperationPolicy.prototype());
        result.configureLivestock(LivestockPolicy.prototype(),LivestockRuntimePolicy.prototype(),LivestockWorkingArea.prototype(),healthy::get);return result;
    }
    long scalar(String query)throws Exception{try(var s=f.db.getConnection().createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getLong(1);}}
    long period()throws Exception{return f.repo().load(f.business.id()).orElseThrow().state().period();}
    long inTransit()throws Exception{return scalar("SELECT COALESCE(SUM(units),0) FROM local_shipments WHERE delivered_period IS NULL");}
    void conserved()throws Exception{
        long warehouse=f.db.getIndustryRepository().store(seller).orElseThrow().units().get(f.grain);
        long feedUsed=scalar("SELECT COALESCE(SUM(feed_used),0) FROM livestock_periods");
        assertEquals(40,warehouse+f.units(f.grain)+inTransit()+feedUsed);
        assertEquals(1000,f.db.getIndustryRepository().store(seller).orElseThrow().hundredths()+f.db.getIndustryRepository().store(f.business.id()).orElseThrow().hundredths());
    }
    @Test void paidFeedWaitsForDeliveryThenRealWorkProducesGoodsWithoutReplayOrDowntime()throws Exception{
        var r=runtime();r.start(1000);assertEquals(1,r.pulse(2000,Set.of(f.world)));assertEquals(1,period());
        assertTrue(inTransit()>0);assertEquals(0,f.units(f.grain));assertEquals(0,f.units(f.milk));conserved();
        assertEquals(0,r.pulse(2000,Set.of(f.world)));
        for(int step=3;step<=8;step++){r.pulse(step*1000L,Set.of(f.world));conserved();}
        assertTrue(f.units(f.milk)>0);assertTrue(scalar("SELECT COUNT(*) FROM livestock_births")>0);
        long before=period();f.db.close();f.reopen();r=runtime();r.start(100_000);assertEquals(0,r.pulse(100_000,Set.of(f.world)));assertEquals(before,period());conserved();
    }
    @Test void missingFundsOrQualificationCannotBuyFeedAndAbsentOrUntrustedWorldEarnsNoTime()throws Exception{
        f.sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+f.business.id()+"'");var r=runtime();r.start(1000);r.pulse(2000,Set.of(f.world));assertEquals(0,inTransit());assertEquals(0,f.units(f.milk));
        f.sql("UPDATE industry_stores SET hundredths=1000 WHERE business_id='"+f.business.id()+"'");f.sql("UPDATE residents SET age_steps=1 WHERE id='"+f.worker+"'");r.pulse(3000,Set.of(f.world));assertEquals(0,inTransit());
        long before=period();r.pulse(100_000,Set.of());assertEquals(before,period());healthy.set(false);r.pulse(200_000,Set.of(f.world));assertEquals(before,period());
        healthy.set(true);r.pulse(201_000,Set.of(f.world));assertEquals(before+1,period());assertEquals(0,inTransit());
    }
    @Test void savedManualHerdPeriodsEnrollAboveExistingHistoryInsteadOfReplayingIt()throws Exception{
        f.feed(30);f.advance(new UUID(0,91),1);f.advance(new UUID(0,92),2);long products=f.units(f.milk);
        var r=runtime();r.start(1000);assertEquals(0,r.pulse(1000,Set.of(f.world)));assertEquals(2,period());assertEquals(products,f.units(f.milk));
        r.pulse(2000,Set.of(f.world));assertEquals(3,period());
    }
    @Test void failedFinalClockWriteRollsBackPurchasesAndHerdWorkTogetherAndRetryBuysOnce()throws Exception{
        var r=runtime();r.start(1000);
        f.sql("CREATE TRIGGER reject_herd_clock AFTER INSERT ON industry_runtime_history BEGIN SELECT RAISE(ABORT,'fixture final clock failure'); END");
        assertThrows(SQLException.class,()->r.pulse(2000,Set.of(f.world)));assertEquals(0,period());assertEquals(0,inTransit());assertEquals(0,scalar("SELECT COUNT(*) FROM local_shipments"));conserved();
        assertTrue(f.db.getConnection().getAutoCommit());f.sql("DROP TRIGGER reject_herd_clock");assertEquals(1,r.pulse(2000,Set.of(f.world)));assertEquals(1,period());assertTrue(inTransit()>0);conserved();
        assertEquals(0,r.pulse(2000,Set.of(f.world)));conserved();
    }
}
