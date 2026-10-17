package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockSupplyChainTest {
    @TempDir Path directory;LivestockInvestmentTest f;UUID herd,warehouse=new UUID(0,80),farm=new UUID(0,90);
    final Commodity grain=new Commodity("grain"),milk=new Commodity("milk");AtomicBoolean healthy=new AtomicBoolean(true);IndustryRuntimePolicy policy;
    @BeforeEach void setup()throws Exception{
        f=new LivestockInvestmentTest();f.directory=directory;f.setup();var opened=f.accept();assertTrue(opened.opened(),opened.explanation());herd=opened.business();
        for(var id:List.of(warehouse,farm)){
            UUID person=new UUID(1,id.getLeastSignificantBits());f.f.db.getPopulationRepository().insertResident(new Resident(person,"Fixture worker",f.f.town,null,300,Resident.Status.ALIVE));
            var site=new BusinessSite(new UUID(2,id.getLeastSignificantBits()),f.f.town,f.f.world,8,64,(int)id.getLeastSignificantBits(),1,"Explicit fixture source/customer");
            var business=new Business(id,f.f.town,"Fixture "+(id.equals(farm)?"crop farm":"warehouse"),person,person,id.equals(farm)?"farming":"warehouse",site.id(),1,0);
            var req=new JobRequirements(216,Map.of());f.f.db.getBusinessRepository().found(new BusinessFounding(business,site,req,req,0,"Explicit fixture business"));f.f.db.getBusinessRepository().hire(UUID.randomUUID(),id,person,0);
        }
        f.f.db.getIndustryRepository().openStore(warehouse,128,Map.of(grain,1L,milk,1L),1000);
        var c=f.f.db.getConnection();c.setAutoCommit(false);FarmMarketStore.ensureRows(c,farm,FarmMarketPolicy.prototype(),1);c.commit();c.setAutoCommit(true);
        f.f.sql("UPDATE industry_stock SET units=40 WHERE business_id='"+farm+"' AND commodity='grain'");
        var mining=new IndustryPolicy(10,1,1,128,1,128,Map.of(Commodity.COAL,1L),Map.of(Commodity.COAL,1L,grain,1L,milk,1L));
        policy=new IndustryRuntimePolicy(true,1000,8,mining,Map.of(),new WorkshopSupplyPolicy(true,2,8,Map.of(grain,25L,milk,200L)),new WarehouseSupplyPolicy(true,16,16));
    }
    @AfterEach void close(){f.close();}
    long scalar(String sql)throws Exception{try(var s=f.f.db.getConnection().createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    @Test void newlyFoundedHerdCreatesRealPaidDemandThroughWarehouseToCropFarmer()throws Exception{
        var runtime=new IndustryRuntime(f.f.db.getConnection(),policy,BusinessOperationPolicy.prototype());runtime.configureLivestock(LivestockPolicy.prototype(),LivestockRuntimePolicy.prototype(),LivestockWorkingArea.prototype(),healthy::get);runtime.start(1000);
        for(int n=2;n<=10;n++){
            runtime.pulse(n*1000L,Set.of(f.f.world));
            assertEquals(40,scalar("SELECT COALESCE(SUM(units),0) FROM industry_stock WHERE commodity='grain'")+scalar("SELECT COALESCE(SUM(units),0) FROM local_shipments WHERE commodity='grain' AND delivered_period IS NULL")+scalar("SELECT COALESCE(SUM(feed_used),0) FROM livestock_periods"));
            assertEquals(9500,f.f.cash()+scalar("SELECT SUM(hundredths) FROM industry_stores"));
        }
        assertTrue(f.f.db.getIndustryRepository().store(herd).orElseThrow().units().get(milk)>0);
        assertTrue(f.f.db.getIndustryRepository().store(farm).orElseThrow().hundredths()>0,"Crop farmer gets paid for actual owned grain");
        assertTrue(scalar("SELECT COUNT(*) FROM local_shipments WHERE seller_id='"+farm+"' AND buyer_id='"+warehouse+"'")>0);
        assertTrue(scalar("SELECT COUNT(*) FROM local_shipments WHERE seller_id='"+warehouse+"' AND buyer_id='"+herd+"'")>0);
    }
    @Test void feedPlanningCountsIncomingAndDefersOnUntrustedOrUnfundedWork()throws Exception{
        var demand=new LocalIndustryDemand(f.f.db.getConnection(),policy,BusinessOperationPolicy.prototype()).withLivestock(LivestockPolicy.prototype(),LivestockRuntimePolicy.prototype(),LivestockWorkingArea.prototype(),healthy::get);
        var first=demand.review(f.f.town,Set.of(f.f.world),16);assertTrue(first.complete(),first.explanation());assertTrue(first.uncovered().getOrDefault(grain,0L)>0);
        healthy.set(false);assertEquals(0,demand.review(f.f.town,Set.of(f.f.world),16).uncovered().getOrDefault(grain,0L));healthy.set(true);
        f.f.sql("UPDATE industry_stores SET hundredths=0 WHERE business_id='"+herd+"'");assertEquals(0,demand.review(f.f.town,Set.of(f.f.world),16).uncovered().getOrDefault(grain,0L));
    }
}
