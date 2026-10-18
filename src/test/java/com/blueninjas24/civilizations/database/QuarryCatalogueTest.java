package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.JobRequirements;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class QuarryCatalogueTest {
    @TempDir Path directory;IndustryRuntimeTest f;UUID warehouse=UUID.randomUUID();
    @BeforeEach void setup()throws Exception {
        f=new IndustryRuntimeTest();f.directory=directory;f.setup();
        var site=new BusinessSite(UUID.randomUUID(),f.town,f.world,8,64,0,1,"Explicit warehouse catalogue fixture");
        var req=new JobRequirements(216,Map.of());
        f.db.getBusinessRepository().found(new BusinessFounding(new Business(warehouse,f.town,"Warehouse",f.smith,f.smith,"warehouse",site.id(),1,0),site,req,req,0,"Explicit fixture"));
        f.db.getIndustryRepository().openStore(warehouse,80,f.space,123);
    }
    @AfterEach void close(){f.close();}
    IndustryRuntime runtime(boolean recipe,long weight) {
        var sizes=new TreeMap<>(f.space);sizes.put(Commodity.SAND,weight);var glass=new Commodity("glass");sizes.put(glass,1L);
        var extraction=new IndustryPolicy(10,1,2,100,1,128,Map.of(Commodity.COAL,1L),sizes);
        var r=new ProductionRecipe("glass", "craftsman",Map.of(Commodity.SAND,1L),Map.of(glass,1L),1,1);
        var p=new IndustryRuntimePolicy(true,1000,8,extraction,recipe?Map.of(r.id(),r):Map.of(),new WorkshopSupplyPolicy(true,1,16,Map.of(Commodity.SAND,100L)),new WarehouseSupplyPolicy(true,16,16));
        var runtime=new IndustryRuntime(f.db.getConnection(),p);runtime.configureQuarries(QuarryPolicy.prototype());return runtime;
    }
    long audits()throws Exception {try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM industry_operations WHERE terms LIKE 'quarry-catalogue-v1|%'")){return r.getLong(1);}}
    String terms()throws Exception {try(var s=f.db.getConnection().prepareStatement("SELECT terms FROM industry_stores WHERE business_id=?")){s.setString(1,warehouse.toString());try(var r=s.executeQuery()){assertTrue(r.next());return r.getString(1);}}}
    @Test void explicitlyConfiguredSandCustomerAddsOnlyAuditedEmptyWarehouseSlotOnce()throws Exception {
        var before=f.db.getIndustryRepository().store(warehouse).orElseThrow();var terms=terms();runtime(true,1).start(1000);
        var after=f.db.getIndustryRepository().store(warehouse).orElseThrow();assertEquals(0,after.units().get(Commodity.SAND));
        assertEquals(before.capacity(),after.capacity());assertEquals(before.hundredths(),after.hundredths());assertEquals(terms,terms());
        before.units().forEach((good,units)->assertEquals(units,after.units().get(good)));assertEquals(1,audits());
        runtime(true,4).start(2000);assertEquals(after,f.db.getIndustryRepository().store(warehouse).orElseThrow());assertEquals(1,audits());
        f.sql("DELETE FROM industry_stock WHERE business_id='"+warehouse+"' AND commodity='sand'");
        assertThrows(SQLException.class,()->runtime(true,1).start(3000));
    }
    @Test void noCustomerLeavesSavedCatalogueAloneAndFailedAuditRollsBackAddition()throws Exception {
        var before=f.db.getIndustryRepository().store(warehouse).orElseThrow();runtime(false,1).start(1000);
        assertEquals(before,f.db.getIndustryRepository().store(warehouse).orElseThrow());assertEquals(0,audits());
        f.sql("CREATE TRIGGER reject_quarry_catalogue AFTER INSERT ON industry_operations BEGIN SELECT RAISE(ABORT,'fixture catalogue failure'); END");
        assertThrows(SQLException.class,()->runtime(true,1).start(2000));assertEquals(before,f.db.getIndustryRepository().store(warehouse).orElseThrow());assertEquals(0,audits());
    }
}
