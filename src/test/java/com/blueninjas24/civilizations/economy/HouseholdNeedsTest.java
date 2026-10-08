package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class HouseholdNeedsTest {
    private final UUID town = UUID.randomUUID(), home = UUID.randomUUID();
    private EconomySettings settings() {
        var prices = new EnumMap<Condition,Integer>(Condition.class); for(var c:Condition.values()) prices.put(c,100);
        return new EconomySettings(60,120,2,2,1,4,100,new Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,12L),Map.of(Commodity.FOOD,BigDecimal.TWO),prices);
    }
    private Resident person(UUID household, long age, Resident.Status status) { return new Resident(UUID.randomUUID(),"Person",town,household,age,status); }
    private Result food(long opening, long demand) { return CommodityAccounting.step(new State(0,Map.of(Commodity.FOOD,new Stock(opening,2))),Map.of(Commodity.FOOD,new Flow(0,0,demand,0,0,0,0)),settings().shortages()); }
    @Test void livingFamiliesHaveAgeBasedDemandAndObservedHousingWithoutClaimingIndividualRations() {
        var population = new PopulationState(List.of(person(home,216,Resident.Status.ALIVE),person(home,10,Resident.Status.ALIVE),person(home,300,Resident.Status.DECEASED),person(null,300,Resident.Status.ALIVE)),
                List.of(new Household(home,town,"Home")),List.of(),Map.of(home,1));
        var needs = HouseholdNeeds.assess(town,population,food(1,5),216,settings());
        assertEquals(Condition.SEVERE_SHORTAGE,needs.sharedFood()); assertEquals(3,needs.shortagePeriods()); assertEquals(1,needs.unassignedResidents());
        var family = needs.homes().getFirst(); assertEquals(1,family.adults()); assertEquals(1,family.children()); assertEquals(3,family.foodDemand()); assertEquals(HouseholdNeeds.Housing.CROWDED,family.housing());
        assertTrue(needs.explanation().contains("does not prove")); assertThrows(UnsupportedOperationException.class,()->needs.homes().clear());
    }
    @Test void unknownHousingIsDistinctFromMeasuredZeroAndRecoveryClearsFoodPressure() {
        var people = List.of(person(home,300,Resident.Status.ALIVE)); var homes = List.of(new Household(home,town,"Home"));
        var unknown = HouseholdNeeds.assess(town,new PopulationState(people,homes,List.of(),Map.of()),food(2,2),216,settings());
        assertEquals(HouseholdNeeds.Housing.UNKNOWN,unknown.homes().getFirst().housing()); assertEquals(0,unknown.shortagePeriods());
        var zero = HouseholdNeeds.assess(town,new PopulationState(people,homes,List.of(),Map.of(home,0)),food(2,2),216,settings());
        assertEquals(HouseholdNeeds.Housing.CROWDED,zero.homes().getFirst().housing());
        assertEquals(HouseholdNeeds.Housing.SUFFICIENT,new HouseholdNeeds.Home(home,1,0,2,1).housing());
    }
    @Test void mixedFoodUsesAndMismatchedDemographicsCannotInventAllocation() {
        var empty = new PopulationState(List.of(),List.of(),List.of(),Map.of());
        assertThrows(IllegalArgumentException.class,()->HouseholdNeeds.assess(town,empty,food(2,2),216,settings()));
        var mixed = CommodityAccounting.step(new State(0,Map.of(Commodity.FOOD,new Stock(2,0))),Map.of(Commodity.FOOD,new Flow(0,0,0,1,0,0,0)),settings().shortages());
        assertThrows(IllegalArgumentException.class,()->HouseholdNeeds.assess(town,empty,mixed,216,settings()));
    }
    @Test void longFoodCrisisAndRecoveryProduceRepeatableNeedsWithoutInventingPersonalWealth() {
        var people = List.of(person(home,300,Resident.Status.ALIVE),person(home,10,Resident.Status.ALIVE));
        var population = new PopulationState(people,List.of(new Household(home,town,"Home")),List.of(),Map.of(home,2));
        var first = scenario(population); var second = scenario(population);
        assertEquals(first,second); assertEquals(Condition.SEVERE_SHORTAGE,first.get(599).sharedFood());
        assertEquals(0,first.getLast().shortagePeriods()); assertEquals(1200,first.getLast().period());
        assertEquals(HouseholdNeeds.Housing.SUFFICIENT,first.getLast().homes().getFirst().housing());
    }
    private List<HouseholdNeeds.Assessment> scenario(PopulationState population) {
        var state = new State(0,Map.of(Commodity.FOOD,new Stock(36,0)));var history = new ArrayList<HouseholdNeeds.Assessment>();
        for (int period=0;period<1200;period++) {
            var next = CommodityAccounting.step(state,Map.of(Commodity.FOOD,new Flow(period>=600?4:0,0,3,0,0,0,0)),settings().shortages());
            history.add(HouseholdNeeds.assess(town,population,next,216,settings()));state=next.state();
        }
        return history;
    }
    @Test void extinctHouseholdsAndOtherSettlementsDoNotContributeDemand() {
        var other = new Resident(UUID.randomUUID(),"Elsewhere",UUID.randomUUID(),null,300,Resident.Status.ALIVE);
        var population = new PopulationState(List.of(other,person(home,300,Resident.Status.DECEASED)),List.of(new Household(home,town,"Empty")),List.of(),Map.of(home,0));
        var needs = HouseholdNeeds.assess(town,population,food(0,0),216,settings());
        assertTrue(needs.homes().isEmpty()); assertEquals(0,needs.unassignedResidents()); assertEquals(Condition.BALANCED,needs.sharedFood());
    }
}
