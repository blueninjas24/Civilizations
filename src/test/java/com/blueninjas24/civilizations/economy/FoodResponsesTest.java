package com.blueninjas24.civilizations.economy;

import org.junit.jupiter.api.Test;
import java.util.*;
import com.blueninjas24.civilizations.economy.FoodResponses.*;
import static org.junit.jupiter.api.Assertions.*;

class FoodResponsesTest {
    private final UUID town=UUID.fromString("00000000-0000-0000-0000-000000000052");
    @Test void blockedOptionsNeverWinAndFeasibleAlternativesVaryAcrossSeeds() {
        var raw=List.of(new Candidate(Action.WAIT,true,false,1,"Wait"),new Candidate(Action.REQUEST_DONATION,true,false,6,"Voluntary"),
                new Candidate(Action.OFFER_EMERALDS,false,false,1000,"No budget"),new Candidate(Action.OFFER_TIMBER,true,false,4,"Saved goods"),new Candidate(Action.OFFER_STONE,false,false,1000,"No stone"));
        var selected=EnumSet.noneOf(Action.class);
        for(int seed=0;seed<1000;seed++){var p=FoodResponses.choose(town,1,seed,25,raw);assertEquals(p,FoodResponses.choose(town,1,seed,25,raw));selected.add(p.selected());assertFalse(p.candidates().get(2).eligible());}
        assertEquals(EnumSet.of(Action.REQUEST_DONATION,Action.OFFER_TIMBER),selected);
    }
    @Test void unavailableResponsesFallBackToWaitingAndIncompleteComparisonsFail() {
        var raw=new ArrayList<Candidate>();for(var action:Action.values())raw.add(new Candidate(action,action==Action.WAIT,false,1,"Reason"));
        assertEquals(Action.WAIT,FoodResponses.choose(town,1,42,25,raw).selected());
        assertThrows(IllegalArgumentException.class,()->FoodResponses.choose(town,1,42,25,raw.subList(0,4)));
        assertThrows(IllegalArgumentException.class,()->new Policy(true,0,1,1,1,25));
    }
}
