package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RecipeProductionTest {
    private static final Commodity TOOLS=new Commodity("tools");
    private static final Map<Commodity,Long> SPACE=Map.of(Commodity.IRON,1L,Commodity.COAL,1L,TOOLS,1L);
    private static final Map<Commodity,Long> STOCK=Map.of(Commodity.IRON,100L,Commodity.COAL,100L,TOOLS,0L);
    @Test void workSiteInputsAndRequestedBatchesAllLimitOutput() {
        var recipe=ProductionRecipe.smithTools();
        assertEquals(2,RecipeProduction.calculate(recipe,100,1,100,STOCK,SPACE).batches());
        assertEquals(8,RecipeProduction.calculate(recipe,100,10,100,STOCK,SPACE).batches());
        assertEquals(1,RecipeProduction.calculate(recipe,1,10,100,STOCK,SPACE).batches());
        assertEquals(0,RecipeProduction.calculate(recipe,100,0,100,STOCK,SPACE).batches());
        var scarce=Map.of(Commodity.IRON,3L,Commodity.COAL,100L,TOOLS,0L);
        assertEquals(1,RecipeProduction.calculate(recipe,100,10,100,scarce,SPACE).batches());
    }
    @Test void consumingInputsFreesSpaceInAFullStoreWithoutDiscardingAnything() {
        var plan=RecipeProduction.calculate(ProductionRecipe.smithTools(),8,4,0,STOCK,SPACE);
        assertEquals(8,plan.batches());assertEquals(Map.of(Commodity.IRON,16L,Commodity.COAL,8L),plan.consumed());
        assertEquals(Map.of(TOOLS,8L),plan.produced());assertTrue(plan.explanation().contains("no net growth"));
    }
    @Test void NetGrowthAndOverlappingInputOutputGoodsRespectCapacity() {
        var recipe=new ProductionRecipe("grow","blacksmith",Map.of(TOOLS,1L),Map.of(TOOLS,3L),10,100);
        var stock=Map.of(TOOLS,10L);var sizes=Map.of(TOOLS,2L);
        assertEquals(1,RecipeProduction.calculate(recipe,100,1,7,stock,sizes).batches());
        assertEquals(0,RecipeProduction.calculate(recipe,100,1,3,stock,sizes).batches());
    }
    @Test void unsupportedMissingNegativeAndUnboundedSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new ProductionRecipe("bad","blacksmith",Map.of(),Map.of(TOOLS,1L),1,1));
        assertThrows(IllegalArgumentException.class,()->new ProductionRecipe("bad","blacksmith",Map.of(Commodity.COAL,0L),Map.of(TOOLS,1L),1,1));
        assertThrows(IllegalArgumentException.class,()->new ProductionRecipe("bad","blacksmith",Map.of(Commodity.COAL,1L),Map.of(TOOLS,1L),0,1));
        assertThrows(IllegalArgumentException.class,()->RecipeProduction.calculate(ProductionRecipe.smithTools(),1,1,1,Map.of(),SPACE));
        assertThrows(IllegalArgumentException.class,()->RecipeProduction.calculate(ProductionRecipe.smithTools(),-1,1,1,STOCK,SPACE));
    }
    @Test void tenThousandSeededPlansConserveInputsAndSpaceAndAreRepeatable() {
        var random=new Random(31);
        for(int i=0;i<10000;i++) {
            var recipe=new ProductionRecipe("smith_tools","blacksmith",Map.of(Commodity.IRON,1L+random.nextInt(5),Commodity.COAL,1L),
                    Map.of(TOOLS,1L+random.nextInt(8)),1+random.nextInt(8),1+random.nextInt(32));
            var stock=Map.of(Commodity.IRON,(long)random.nextInt(100),Commodity.COAL,(long)random.nextInt(100),TOOLS,0L);
            long free=random.nextInt(100),workers=random.nextInt(8),requested=random.nextInt(100);
            var plan=RecipeProduction.calculate(recipe,requested,workers,free,stock,SPACE);
            assertEquals(plan,RecipeProduction.calculate(recipe,requested,workers,free,stock,SPACE));
            assertTrue(plan.batches()<=requested && plan.batches()<=workers*recipe.batchesPerWorker() && plan.batches()<=recipe.siteCapacity());
            plan.consumed().forEach((g,u)->assertTrue(u<=stock.get(g)));
            long released=plan.consumed().values().stream().mapToLong(Long::longValue).sum();
            long added=plan.produced().values().stream().mapToLong(Long::longValue).sum();assertTrue(added-released<=free);
        }
    }
}
