package com.blueninjas24.civilizations.economy;

import java.util.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;

/** A published, nonbinding procurement preference; actual exchanges still require consent and fresh checks. */
public final class FoodResponses {
    private FoodResponses() {}
    public enum Action { WAIT, REQUEST_DONATION, OFFER_EMERALDS, OFFER_TIMBER, OFFER_STONE }
    public record Policy(boolean enabled,int donationWeight,int emeraldWeight,int barterWeight,int waitWeight,int relativePercent) {
        public Policy {
            if(donationWeight<1||donationWeight>1000||emeraldWeight<1||emeraldWeight>1000||barterWeight<1||barterWeight>1000
                    ||waitWeight<1||waitWeight>1000||relativePercent<0||relativePercent>100)throw new IllegalArgumentException("Invalid response policy");
        }
        public static Policy prototype(){return new Policy(true,6,4,4,1,25);}
        public static Policy disabled(){return new Policy(false,6,4,4,1,25);}
    }
    public record Settings(Policy policy,boolean donations,FoodPurchases.Policy purchases,FoodBarters.Policy barter,FoodAidPreview.Policy food) {
        public Settings {Objects.requireNonNull(policy);Objects.requireNonNull(purchases);Objects.requireNonNull(barter);Objects.requireNonNull(food);}
        public static Settings disabled(){return new Settings(Policy.disabled(),false,FoodPurchases.Policy.disabled(),FoodBarters.Policy.disabled(),FoodAidPreview.Policy.prototype());}
        public static Settings prototype(){return new Settings(Policy.prototype(),true,FoodPurchases.Policy.prototype(),FoodBarters.Policy.prototype(),FoodAidPreview.Policy.prototype());}
    }
    public record Candidate(Action action,boolean feasible,boolean eligible,long weight,String reason) {
        public Candidate {Objects.requireNonNull(action);Objects.requireNonNull(reason);if(weight<1||weight>1000||reason.length()>2048||eligible&&!feasible)throw new IllegalArgumentException("Invalid response candidate");}
        public String explanation(){return action+": feasible "+feasible+", eligible "+eligible+", weight "+weight+"; "+reason;}
    }
    public record Plan(UUID settlement,long period,long seed,int relativePercent,Action selected,List<Candidate> candidates) {
        public Plan {Objects.requireNonNull(settlement);Objects.requireNonNull(selected);candidates=List.copyOf(candidates);if(period<1)throw new IllegalArgumentException("Invalid response period");}
        public String explanation(){return "Published food response at period "+period+": "+selected+". Preference only; available alternatives remain optional, and every transfer is checked again.";}
    }
    public static Plan choose(UUID town,long period,long seed,int relative,List<Candidate> raw) {
        if(relative<0||relative>100||raw.size()!=Action.values().length||raw.stream().map(Candidate::action).distinct().count()!=raw.size())throw new IllegalArgumentException("Incomplete response comparison");
        long best=raw.stream().filter(Candidate::feasible).mapToLong(Candidate::weight).max().orElseThrow(()->new IllegalArgumentException("No feasible response"));
        var candidates=raw.stream().sorted(Comparator.comparing(Candidate::action)).map(c->new Candidate(c.action(),c.feasible(),c.feasible()&&c.weight()*100>=best*relative,c.weight(),c.reason())).toList();
        long total=candidates.stream().filter(Candidate::eligible).mapToLong(Candidate::weight).sum();
        var random=new SplittableRandom(seed^town.getMostSignificantBits()^Long.rotateLeft(town.getLeastSignificantBits(),23)^period*0xD1B54A32D192ED03L);
        long pick=random.nextLong(total);Action selected=null;
        for(var c:candidates)if(c.eligible()){if(pick<c.weight()){selected=c.action();break;}pick-=c.weight();}
        return new Plan(town,period,seed,relative,Objects.requireNonNull(selected),candidates);
    }
    /** Exact one-stack capability, not a promise that a player will supply it. */
    private static long minimumStack(long value,long price,long need,FoodAidPreview.Policy food) {
        if(value<1||price<1||need<1)return 0;
        long required=value/price+(value%price==0?0:1),best=Long.MAX_VALUE;
        for(long units:food.unitsPerItem().values()) {
            long count=required/units+(required%units==0?0:1);
            if(count>=1&&count<=64){long stack=units*count;if(stack<=need)best=Math.min(best,stack);}
        }
        return best==Long.MAX_VALUE?0:best;
    }
    public static Plan assess(UUID town,long period,long seed,VillagePriorities.Decision priority,HouseholdNeeds.Assessment needs,
                              boolean caughtUp,long pendingSupply,Long treasury,Result stocks,EconomySettings economy,Settings settings) {
        if(!town.equals(priority.settlement())||!town.equals(needs.settlement())||period!=needs.period()||priority.period()>period||pendingSupply<0||treasury!=null&&treasury<0)
            throw new IllegalArgumentException("Response evidence disagrees");
        var p=settings.policy();boolean active=p.enabled()&&priority.selected()==VillagePriorities.Action.SEEK_FOOD&&needs.demand()>needs.fulfilled();
        var food=stocks.reports().stream().filter(r->r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow();
        if(stocks.state().completedPeriods()!=period||food.demand()!=needs.demand()||food.fulfilled()!=needs.fulfilled())throw new IllegalArgumentException("Response food period disagrees");
        long supply=Math.addExact(food.after().units(),pendingSupply);
        long price=economy.price(Commodity.FOOD,needs.sharedFood()).movePointRight(2).longValueExact();
        boolean supported=settings.food().enabled()&&!settings.food().unitsPerItem().isEmpty();
        long cashNeed=Math.max(0,Math.multiplyExact(needs.demand(),settings.purchases().reservePeriods())-supply);
        long cashStack=minimumStack(settings.purchases().hundredthsPerEmerald(),price,cashNeed,settings.food());
        var raw=new ArrayList<Candidate>();
        raw.add(new Candidate(Action.WAIT,true,false,p.waitWeight(),"Spend nothing; no supplier is assumed. Governing priority "+priority.selected()+"; housing projects require later authorized sites/construction."));
        boolean additionalNeeded=supply<Math.multiplyExact(needs.demand(),Math.max(settings.purchases().reservePeriods(),settings.barter().foodReservePeriods()));
        raw.add(new Candidate(Action.REQUEST_DONATION,active&&supported&&settings.donations()&&additionalNeeded,false,p.donationWeight(),
                "Voluntary appeal; no payment, guaranteed donor or reputation reward. Enabled "+settings.donations()+"; active food priority "+active+"; supported food "+supported+"; pending/reserve supplies still below target "+additionalNeeded+"."));
        raw.add(new Candidate(Action.OFFER_EMERALDS,active&&caughtUp&&supported&&settings.purchases().enabled()&&treasury!=null&&treasury>=settings.purchases().hundredthsPerEmerald()&&cashStack>0,false,p.emeraldWeight(),
                "Spendable budget "+(treasury==null?"unknown":treasury)+" hundredths TV; one emerald costs "+settings.purchases().hundredthsPerEmerald()+"; remaining food target "+cashNeed+"; minimum supported stack "+cashStack+" food units; enabled "+settings.purchases().enabled()+"; caught up "+caughtUp+"; active priority "+active+". Fresh quote and player consent required."));
        for(var commodity:List.of(Commodity.TIMBER,Commodity.STONE)) {
            var goods=settings.barter().goods().get(commodity);
            var material=stocks.reports().stream().filter(r->r.commodity().equals(commodity)).findFirst().orElse(null);
            long need=Math.max(0,Math.multiplyExact(needs.demand(),settings.barter().foodReservePeriods())-supply);
            boolean idle=material!=null&&material.flow().demand()==0&&material.after().shortagePeriods()==0;
            long available=goods==null||material==null?0:Math.max(0,material.after().units()-goods.keepUnits());
            long stack=goods==null||material==null?0:minimumStack(Math.multiplyExact(economy.price(commodity,Condition.BALANCED).movePointRight(2).longValueExact(),goods.unitsPerItem()),price,need,settings.food());
            boolean feasible=active&&caughtUp&&supported&&settings.barter().enabled()&&goods!=null&&idle&&available>=goods.unitsPerItem()&&stack>0;
            raw.add(new Candidate(commodity.equals(Commodity.TIMBER)?Action.OFFER_TIMBER:Action.OFFER_STONE,feasible,false,p.barterWeight(),
                    "Actual exportable "+commodity.id()+" units above floor "+available+"; idle/non-shortage evidence "+idle+"; minimum supported food stack "+stack+"; remaining target "+need+"; enabled "+settings.barter().enabled()+"; caught up "+caughtUp+"; active priority "+active+". No recurring industry output; fresh stock quote and consent required."));
        }
        return choose(town,period,seed,p.relativePercent(),raw);
    }
}
