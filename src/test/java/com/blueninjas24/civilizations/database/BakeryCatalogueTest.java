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

class BakeryCatalogueTest {
    @TempDir Path directory;IndustryRuntimeTest f;UUID bakery=UUID.randomUUID();
    Commodity grain=new Commodity("grain"),milk=new Commodity("milk"),eggs=new Commodity("eggs");
    @BeforeEach void setup()throws Exception {
        f=new IndustryRuntimeTest();f.directory=directory;f.setup();
        var site=new BusinessSite(UUID.randomUUID(),f.town,f.world,8,64,0,1,"Explicit bakery transition fixture");
        var b=new Business(bakery,f.town,"Fixture bakery",f.smith,f.smith,"bakery",site.id(),1,0);
        var req=new JobRequirements(216,Map.of());f.db.getBusinessRepository().found(new BusinessFounding(b,site,req,req,0,"Explicit fixture"));
        f.db.getIndustryRepository().openStore(bakery,100,Map.of(grain,1L,Commodity.FOOD,1L),500);
        f.sql("UPDATE industry_stock SET units=3 WHERE business_id='"+bakery+"'");
    }
    @AfterEach void close(){f.close();}
    IndustryRuntimePolicy policy(boolean cake,long weight){
        var space=Map.of(Commodity.COAL,1L,grain,1L,Commodity.FOOD,1L,milk,weight,eggs,weight);
        var mine=new IndustryPolicy(1,1,1,100,1,128,Map.of(Commodity.COAL,1L),space);
        return new IndustryRuntimePolicy(true,1000,8,mine,cake?Map.of("bakery_bread",ProductionRecipe.bakeryBread(),"bakery_cake",ProductionRecipe.bakeryCake()):Map.of("bakery_bread",ProductionRecipe.bakeryBread()));
    }
    long reviews()throws Exception {try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM industry_operations WHERE terms LIKE 'bakery-catalogue-v1|%'")){return r.getLong(1);}}
    String terms()throws Exception {try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("SELECT terms FROM industry_stores WHERE business_id='"+bakery+"'")){return r.getString(1);}}
    @Test void runtimeAddsOnlyEmptyConfiguredSlotsOnceAndRetainsAssetsOpeningAndSavedWeights()throws Exception {
        var before=f.db.getIndustryRepository().store(bakery).orElseThrow();var opening=terms();
        new IndustryRuntime(f.db.getConnection(),policy(true,1)).start(1000);
        var after=f.db.getIndustryRepository().store(bakery).orElseThrow();
        assertEquals(before.capacity(),after.capacity());assertEquals(before.hundredths(),after.hundredths());assertEquals(opening,terms());
        before.units().forEach((g,u)->assertEquals(u,after.units().get(g)));assertEquals(0,after.units().get(milk));assertEquals(0,after.units().get(eggs));assertEquals(2,reviews());
        new IndustryRuntime(f.db.getConnection(),policy(true,4)).start(2000);
        assertEquals(after,f.db.getIndustryRepository().store(bakery).orElseThrow());assertEquals(2,reviews());
    }
    @Test void noRecipeOptInLeavesLegacyCatalogueAloneAndLateFailureRollsBack()throws Exception {
        var before=f.db.getIndustryRepository().store(bakery).orElseThrow();
        new IndustryRuntime(f.db.getConnection(),policy(false,1)).start(1000);assertEquals(before,f.db.getIndustryRepository().store(bakery).orElseThrow());assertEquals(0,reviews());
        f.sql("CREATE TRIGGER reject_catalogue AFTER INSERT ON industry_operations BEGIN SELECT RAISE(ABORT,'fixture catalogue audit failure'); END");
        assertThrows(SQLException.class,()->new IndustryRuntime(f.db.getConnection(),policy(true,1)).start(2000));
        assertEquals(before,f.db.getIndustryRepository().store(bakery).orElseThrow());assertEquals(0,reviews());assertTrue(f.db.getConnection().getAutoCommit());
        f.sql("DROP TRIGGER reject_catalogue");new IndustryRuntime(f.db.getConnection(),policy(true,1)).start(2000);assertEquals(2,reviews());
        f.sql("DELETE FROM industry_stock WHERE business_id='"+bakery+"' AND commodity='milk'");
        assertThrows(SQLException.class,()->new IndustryRuntime(f.db.getConnection(),policy(true,1)).start(3000));
    }
}
