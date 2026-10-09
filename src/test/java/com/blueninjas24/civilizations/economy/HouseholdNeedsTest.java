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
        assertEquals(588L,first.get(599).homes().getFirst().unmetPeriods());
        assertEquals(0L,first.getLast().homes().getFirst().unmetPeriods());
        assertEquals(0,first.getLast().shortagePeriods()); assertEquals(1200,first.getLast().period());
        assertEquals(HouseholdNeeds.Housing.SUFFICIENT,first.getLast().homes().getFirst().housing());
    }
    private List<HouseholdNeeds.Assessment> scenario(PopulationState population) {
        var state = new State(0,Map.of(Commodity.FOOD,new Stock(36,0)));var history = new ArrayList<HouseholdNeeds.Assessment>();
        for (int period=0;period<1200;period++) {
            var next = CommodityAccounting.step(state,Map.of(Commodity.FOOD,new Flow(period>=600?4:0,0,3,0,0,0,0)),settings().shortages());
            history.add(HouseholdNeeds.assess(town,population,next,216,settings(),history.isEmpty()?null:history.getLast()));state=next.state();
        }
        return history;
    }
    @Test void extinctHouseholdsAndOtherSettlementsDoNotContributeDemand() {
        var other = new Resident(UUID.randomUUID(),"Elsewhere",UUID.randomUUID(),null,300,Resident.Status.ALIVE);
        var population = new PopulationState(List.of(other,person(home,300,Resident.Status.DECEASED)),List.of(new Household(home,town,"Empty")),List.of(),Map.of(home,0));
        var needs = HouseholdNeeds.assess(town,population,food(0,0),216,settings());
        assertTrue(needs.homes().isEmpty()); assertEquals(0,needs.unassignedResidents()); assertEquals(Condition.BALANCED,needs.sharedFood());
    }
    @Test void proportionalWholeUnitsConserveFoodIncludingUnassignedPeople() {
        var population = new PopulationState(List.of(person(home,300,Resident.Status.ALIVE),person(home,10,Resident.Status.ALIVE),person(null,300,Resident.Status.ALIVE)),
                List.of(new Household(home,town,"Home")),List.of(),Map.of(home,2));
        var needs = HouseholdNeeds.assess(town,population,food(2,5),216,settings());
        assertEquals(1L,needs.homes().getFirst().supplied()); assertEquals(1L,needs.unassignedSupplied());
        assertEquals(2L,needs.unassignedDemand()); assertEquals(1L,needs.homes().getFirst().unmetPeriods());
        assertEquals("UNDERSUPPLIED",needs.homes().getFirst().foodCoverage());
    }
    @Test void wholeUnitRemaindersHaveStableTieOrderAndHandleLongProducts() {
        assertArrayEquals(new long[]{1,0,0},HouseholdNeeds.allocate(List.of(1L,1L,1L),1));
        assertArrayEquals(new long[]{0,0},HouseholdNeeds.allocate(List.of(0L,0L),0));
        long half=Long.MAX_VALUE/2;
        var shares=HouseholdNeeds.allocate(List.of(half,half),Long.MAX_VALUE-3);
        assertEquals(Long.MAX_VALUE-3,Math.addExact(shares[0],shares[1]));
        assertTrue(shares[0]<=half && shares[1]<=half);
        assertThrows(IllegalArgumentException.class,()->HouseholdNeeds.allocate(List.of(1L),2));
        assertThrows(ArithmeticException.class,()->HouseholdNeeds.allocate(List.of(Long.MAX_VALUE,1L),1));
    }
    @Test void consecutiveUnmetPeriodsSurviveAssessmentAndClearOnRelief() {
        var population = new PopulationState(List.of(person(home,300,Resident.Status.ALIVE)),List.of(new Household(home,town,"Home")),List.of(),Map.of(home,1));
        var first=HouseholdNeeds.assess(town,population,food(0,2),216,settings());
        var secondPeriod=CommodityAccounting.step(new State(1,Map.of(Commodity.FOOD,new Stock(0,1))),Map.of(Commodity.FOOD,new Flow(0,0,2,0,0,0,0)),settings().shortages());
        var second=HouseholdNeeds.assess(town,population,secondPeriod,216,settings(),first);
        assertEquals(2L,second.homes().getFirst().unmetPeriods());
        var relief=CommodityAccounting.step(secondPeriod.state(),Map.of(Commodity.FOOD,new Flow(0,2,2,0,0,0,0)),settings().shortages());
        var recovered=HouseholdNeeds.assess(town,population,relief,216,settings(),second);
        assertEquals(0L,recovered.homes().getFirst().unmetPeriods()); assertEquals("SUPPLIED",recovered.homes().getFirst().foodCoverage());
        assertThrows(IllegalArgumentException.class,()->HouseholdNeeds.assess(town,population,relief,216,settings(),first));
    }
    @Test void familyOrderingDoesNotChangeAllocationAndHistoryRemainsExplicitlyUnknown() {
        var other=new UUID(0,0); var homes=List.of(new Household(home,town,"Home"),new Household(other,town,"Other"));
        var people=List.of(person(home,300,Resident.Status.ALIVE),person(other,300,Resident.Status.ALIVE));
        var a=HouseholdNeeds.assess(town,new PopulationState(people,homes,List.of(),Map.of()),food(1,4),216,settings());
        var reversed=new ArrayList<>(homes);Collections.reverse(reversed);
        var b=HouseholdNeeds.assess(town,new PopulationState(people,reversed,List.of(),Map.of()),food(1,4),216,settings());
        assertEquals(a,b);assertEquals(1L,a.homes().getFirst().supplied());
        assertEquals("UNKNOWN",new HouseholdNeeds.Home(home,1,0,2,1).foodCoverage());
    }

    @Test void manySeededShortagesNeverLoseFoodOrOverfillAnyFamily() {
        var random=new Random(917);
        for(int scenario=0;scenario<10000;scenario++) {
            var demands=new ArrayList<Long>();long total=0;
            for(int home=0;home<1+random.nextInt(30);home++){long demand=random.nextInt(1000);demands.add(demand);total+=demand;}
            long supplied=random.nextLong(total+1);var shares=HouseholdNeeds.allocate(demands,supplied);long allocated=0;
            for(int home=0;home<shares.length;home++){assertTrue(shares[home]>=0&&shares[home]<=demands.get(home));allocated+=shares[home];}
            assertEquals(supplied,allocated);
        }
    }

}
