package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivestockInventoryTest {
    LivestockInventory.Slot slot(String item, int count, int limit) { return new LivestockInventory.Slot(item,count,limit,true); }
    List<LivestockInventory.Slot> inventory() { return new ArrayList<>(Collections.nCopies(36,LivestockInventory.Slot.EMPTY)); }
    Terms terms(Product product,int count,int price) { return new Terms(new UUID(1,1),new UUID(2,2),new UUID(3,3),new UUID(4,4),new UUID(5,5),product,count,price,800); }
    long count(List<LivestockInventory.Slot> slots,String item) { return slots.stream().filter(s->s.plain()&&s.item().equals(item)).mapToLong(LivestockInventory.Slot::count).sum(); }
    @Test void milkConsumesOnlyOneRealBucketAndExactPaymentPreservingUnrelatedItems() {
        var before=inventory();before.set(0,slot("EMERALD",3,64));before.set(1,slot("BUCKET",16,16));
        var custom=new LivestockInventory.Slot("DIAMOND_SWORD",1,1,false);before.set(2,custom);
        var plan=LivestockInventory.plan(before,terms(Product.MILK,1,2),1);
        assertEquals(1,count(plan.after(),"EMERALD"));assertEquals(15,count(plan.after(),"BUCKET"));assertEquals(1,count(plan.after(),"MILK_BUCKET"));
        assertEquals(custom,plan.after().get(2));assertEquals(3,count(before,"EMERALD"));assertEquals(16,count(before,"BUCKET"));
        before.set(0,LivestockInventory.Slot.EMPTY);assertEquals(3,count(plan.before(),"EMERALD"));
        assertThrows(UnsupportedOperationException.class,()->plan.after().clear());
    }
    @Test void missingBucketsOrPaymentNeverChangeTheOriginalInventory() {
        var before=inventory();before.set(0,slot("EMERALD",2,64));var original=List.copyOf(before);
        assertThrows(LivestockInventory.Unavailable.class,()->LivestockInventory.plan(before,terms(Product.MILK,1,2),1));assertEquals(original,before);
        before.set(1,slot("BUCKET",1,16));before.set(0,slot("EMERALD",1,64));original=List.copyOf(before);
        assertThrows(LivestockInventory.Unavailable.class,()->LivestockInventory.plan(before,terms(Product.MILK,1,2),1));assertEquals(original,before);
    }
    @Test void fullInventoryCanUseAnActuallyEmptiedSlotButCannotDropOverflowOrInventRoom() {
        var before=new ArrayList<>(Collections.nCopies(36,slot("STONE",64,64)));before.set(0,slot("EMERALD",64,64));before.set(1,slot("BUCKET",16,16));
        var original=List.copyOf(before);assertThrows(LivestockInventory.Unavailable.class,()->LivestockInventory.plan(before,terms(Product.MILK,1,2),1));assertEquals(original,before);
        before.set(0,slot("EMERALD",2,64));var after=LivestockInventory.plan(before,terms(Product.MILK,1,2),1).after();
        assertEquals("MILK_BUCKET",after.get(0).item());assertEquals(15,count(after,"BUCKET"));assertEquals(34*64,count(after,"STONE"));
    }
    @Test void eggOrderSplitsByActualStackLimitAndMergesOnlyOrdinaryMatchingItems() {
        var before=inventory();before.set(0,slot("EMERALD",2,64));before.set(1,slot("EGG",15,16));
        var custom=new LivestockInventory.Slot("EGG",8,16,false);before.set(2,custom);
        var after=LivestockInventory.plan(before,terms(Product.EGGS,64,1),16).after();
        assertEquals(79,count(after,"EGG"));assertEquals(custom,after.get(2));assertEquals(1,count(after,"EMERALD"));
        for(var item:after)if(item.plain()&&item.item().equals("EGG"))assertTrue(item.count()<=16);
        assertEquals(0,count(after,"BUCKET"));
    }
    @Test void customCurrencyAndBucketsAreNeverConsumedOrRewritten() {
        var before=inventory();before.set(0,new LivestockInventory.Slot("EMERALD",64,64,false));before.set(1,new LivestockInventory.Slot("BUCKET",1,16,false));
        assertThrows(LivestockInventory.Unavailable.class,()->LivestockInventory.plan(before,terms(Product.MILK,1,2),1));
        before.set(2,slot("EMERALD",2,64));assertThrows(LivestockInventory.Unavailable.class,()->LivestockInventory.plan(before,terms(Product.MILK,1,2),1));
        before.set(3,slot("BUCKET",1,16));var after=LivestockInventory.plan(before,terms(Product.MILK,1,2),1).after();
        assertEquals(before.get(0),after.get(0));assertEquals(before.get(1),after.get(1));
    }
    @Test void multiStackOrdersRefuseInsufficientDestinationSpaceWithoutPartialDebit() {
        var before=new ArrayList<>(Collections.nCopies(36,slot("STONE",64,64)));before.set(0,slot("EMERALD",1,64));before.set(1,LivestockInventory.Slot.EMPTY);
        var original=List.copyOf(before);assertThrows(LivestockInventory.Unavailable.class,()->LivestockInventory.plan(before,terms(Product.EGGS,64,1),16));assertEquals(original,before);
    }
    @Test void seededInventorySweepConservesMoneyContainersGoodsAndReadOnlySlots() {
        var random=new Random(872613);
        for(int trial=0;trial<3000;trial++) {
            var before=inventory();for(int i=0;i<36;i++)if(random.nextBoolean())before.set(i,slot("STONE",64,64));
            before.set(0,slot("EMERALD",1+random.nextInt(64),64));before.set(1,slot("BUCKET",1+random.nextInt(16),16));
            before.set(2,new LivestockInventory.Slot("EMERALD",64,64,false));var original=List.copyOf(before);
            var product=random.nextBoolean()?Product.MILK:Product.EGGS;var t=terms(product,product==Product.MILK?1:1+random.nextInt(64),1+random.nextInt(64));
            try {
                var after=LivestockInventory.plan(before,t,product==Product.MILK?1:16).after();
                assertEquals(count(before,"EMERALD")-t.emeralds(),count(after,"EMERALD"));assertEquals(count(before,"BUCKET")-t.emptyBuckets(),count(after,"BUCKET"));
                assertEquals(count(before,t.product().item())+t.count(),count(after,t.product().item()));assertEquals(before.get(2),after.get(2));assertEquals(count(before,"STONE"),count(after,"STONE"));
                for(var slot:after)if(slot.plain())assertTrue(slot.count()<=slot.maximum());
            }catch(LivestockInventory.Unavailable unavailable){assertEquals(original,before);}
        }
    }
}
