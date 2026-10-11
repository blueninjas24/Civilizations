package com.blueninjas24.civilizations.industry;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SourceDepletionTest {
    @Test void balancedRoundsHandleZeroUnequalAndHugeBalancesWithoutPerUnitWork(){
        assertEquals(List.of(23L,23L,23L,24L),SourceDepletion.drain(List.of(25L,25L,25L,25L),7));
        assertEquals(List.of(0L,19L,20L,21L),SourceDepletion.drain(List.of(0L,23L,23L,24L),10));
        assertEquals(List.of(0L,96L,96L),SourceDepletion.drain(List.of(1L,100L,100L),9));
        assertEquals(List.of(0L,0L),SourceDepletion.drain(List.of(Long.MAX_VALUE,0L),Long.MAX_VALUE));
        assertEquals(List.of(0L,2L),SourceDepletion.drain(List.of(0L,2L),0));
        assertThrows(IllegalArgumentException.class,()->SourceDepletion.drain(List.of(1L),2));assertThrows(IllegalArgumentException.class,()->SourceDepletion.drain(List.of(-1L),0));
    }
    @Test void seededSmallCasesMatchLiteralRoundRobinAndConserveExactly(){
        var random=new Random(42);
        for(int run=0;run<500;run++) {
            int size=1+random.nextInt(12);
            var before=new ArrayList<Long>();for(int i=0;i<size;i++)before.add((long)random.nextInt(20));
            long total=before.stream().mapToLong(Long::longValue).sum(),amount=total==0?0:random.nextLong(total+1);var literal=new ArrayList<>(before);long left=amount;
            while(left>0)for(int i=0;i<literal.size()&&left>0;i++)if(literal.get(i)>0){literal.set(i,literal.get(i)-1);left--;}
            assertEquals(literal,SourceDepletion.drain(before,amount));
        }
    }
}
