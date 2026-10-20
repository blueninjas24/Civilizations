package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.population.Household;
import java.util.*;

/** Whole-household proposals only. Reuse permitted vacancies first; never move people, claim land or grant beds. */
public record HousingProjectPlanner(int maximumNewProposals) {
    public record Need(Household household,int livingResidents,boolean suitablyHoused) {
        public Need{Objects.requireNonNull(household);if(livingResidents<0||livingResidents>1024)throw new IllegalArgumentException("Invalid household size");}
    }
    /** Only confirmed village-owned, usable and unreserved homes can be offered for reuse. */
    public record Vacancy(UUID home,UUID village,UUID world,int beds,boolean villageOwned,boolean usable,boolean reserved) {
        public Vacancy{Objects.requireNonNull(home);Objects.requireNonNull(village);Objects.requireNonNull(world);if(beds<1||beds>1024)throw new IllegalArgumentException("Invalid vacant home capacity");}
    }
    public enum Action { ALREADY_HOUSED,NO_LIVING_MEMBERS,WAIT_WORLD,WAIT_PROJECT,WAIT_SURVEY,WAIT_PLAN,WAIT_CAP,REUSE,PROPOSE }
    public record Recommendation(UUID household,Action action,UUID vacantHome,String templateId,String fingerprint,String explanation){}
    public record Plan(List<Recommendation> recommendations) {public Plan{recommendations=List.copyOf(recommendations);}}
    public HousingProjectPlanner{if(maximumNewProposals<1||maximumNewProposals>64)throw new IllegalArgumentException("Housing proposal cap must be1..64");}
    public static HousingProjectPlanner prototype(){return new HousingProjectPlanner(1);}
    public Plan review(UUID village,UUID world,List<Need> needs,List<Vacancy> vacancies,List<ConstructionTemplate> templates,
                       Set<UUID> pendingHouseholds,boolean vacancySurveyComplete,boolean worldAvailable,long seed,long period) {
        Objects.requireNonNull(village);Objects.requireNonNull(world);needs=List.copyOf(needs);vacancies=List.copyOf(vacancies);templates=List.copyOf(templates);pendingHouseholds=Set.copyOf(pendingHouseholds);
        if(period<0||needs.size()>1024||vacancies.size()>1024||templates.size()>64)throw new IllegalArgumentException("Housing review exceeds bounded inputs");
        var identities=new HashSet<UUID>();for(var need:needs)if(!need.household().settlementId().equals(village)||!identities.add(need.household().id()))throw new IllegalArgumentException("Duplicate or wrong-village household");
        identities.clear();for(var home:vacancies)if(!identities.add(home.home()))throw new IllegalArgumentException("Duplicate home evidence");
        var capacities=new HashMap<String,Integer>();for(var template:templates)if(template.purpose()==ConstructionTemplate.Purpose.HOME) {
            if(capacities.putIfAbsent(template.id(),HomePlanCapacity.beds(template))!=null)throw new IllegalArgumentException("Ambiguous home template ID");
        }
        var plans=templates.stream().filter(t->t.purpose()==ConstructionTemplate.Purpose.HOME).sorted(Comparator.comparingInt((ConstructionTemplate t)->capacities.get(t.id())).thenComparing(ConstructionTemplate::id)).toList();
        var ordered=needs.stream().sorted(Comparator.comparingInt(Need::livingResidents).reversed().thenComparingLong(n->rank(n.household().id(),seed,period)).thenComparing(n->n.household().id())).toList();
        var usedHomes=new HashSet<UUID>();var result=new ArrayList<Recommendation>();int proposed=0;
        for(var need:ordered) {
            UUID household=need.household().id();Action action;UUID home=null;String template=null,fingerprint=null,reason;
            if(need.livingResidents()==0){action=Action.NO_LIVING_MEMBERS;reason="Empty household creates no new housing demand";}
            else if(need.suitablyHoused()){action=Action.ALREADY_HOUSED;reason="Current home meets this household's needs";}
            else if(!worldAvailable){action=Action.WAIT_WORLD;reason="Absent world pauses housing proposals";}
            else if(pendingHouseholds.contains(household)){action=Action.WAIT_PROJECT;reason="Existing household project retained; no duplicate proposal";}
            else {
                var available=vacancies.stream().filter(v->v.village().equals(village)&&v.world().equals(world)&&v.villageOwned()&&v.usable()&&!v.reserved()&&!usedHomes.contains(v.home())&&v.beds()>=need.livingResidents())
                        .min(Comparator.comparingInt(Vacancy::beds).thenComparing(Vacancy::home));
                if(available.isPresent()){action=Action.REUSE;home=available.orElseThrow().home();usedHomes.add(home);reason="Suitable permitted vacant home before new construction; assignment still requires owner validation";}
                else if(!vacancySurveyComplete){action=Action.WAIT_SURVEY;reason="Cannot bypass uncertain vacancy evidence by building another home";}
                else {
                    var fitting=plans.stream().filter(t->capacities.get(t.id())>=need.livingResidents()).findFirst();
                    if(fitting.isEmpty()){action=Action.WAIT_PLAN;reason="No plan houses this whole household; no forced family split or ill-fitting home";}
                    else if(proposed>=maximumNewProposals){action=Action.WAIT_CAP;reason="Bounded new-proposal allowance reached; no spending or site rights granted";}
                    else {action=Action.PROPOSE;proposed++;template=fitting.orElseThrow().id();fingerprint=fitting.orElseThrow().fingerprint();reason="Whole-household plan proposal; funding, site, labor and construction remain required";}
                }
            }
            result.add(new Recommendation(household,action,home,template,fingerprint,reason));
        }return new Plan(result);
    }
    private static long rank(UUID id,long seed,long period){long n=id.getMostSignificantBits()^id.getLeastSignificantBits()^seed^(period*0x9e3779b97f4a7c15L);n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
}
