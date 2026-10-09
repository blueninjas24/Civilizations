package com.blueninjas24.civilizations.economy;

import java.util.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.Report;

/** A real shortage becomes a voluntary appeal; observations do not prove an action caused recovery. */
public final class FoodAppeals {
    private FoodAppeals() {}
    public enum Status { NONE, OPEN, RESOLVED, WITHDRAWN }
    public enum Change { NONE, OPENED, IMPROVED, UNCHANGED, WORSENED, RESOLVED, WITHDRAWN }
    public record Snapshot(UUID settlement, long period, long priorityPeriod, Status status,
                           long unmet, Long previousUnmet, long imports, long production, Change change) {
        public Snapshot {
            Objects.requireNonNull(settlement); Objects.requireNonNull(status); Objects.requireNonNull(change);
            if (period < 1 || priorityPeriod < 1 || priorityPeriod > period || unmet < 0 || imports < 0 || production < 0
                    || (previousUnmet != null && previousUnmet < 0)) throw new IllegalArgumentException("Invalid appeal evidence");
            if (status == Status.OPEN && unmet == 0 || status == Status.RESOLVED && unmet != 0)
                throw new IllegalArgumentException("Appeal contradicts shortage evidence");
            boolean valid = switch(status) {
                case NONE -> change==Change.NONE && previousUnmet==null;
                case RESOLVED -> change==Change.RESOLVED && previousUnmet!=null && previousUnmet>0;
                case WITHDRAWN -> change==Change.WITHDRAWN && unmet>0 && previousUnmet!=null && previousUnmet>0;
                case OPEN -> switch(change) {
                    case OPENED -> previousUnmet==null;
                    case IMPROVED -> previousUnmet!=null && unmet<previousUnmet;
                    case WORSENED -> previousUnmet!=null && unmet>previousUnmet && previousUnmet>0;
                    case UNCHANGED -> previousUnmet!=null && unmet==previousUnmet;
                    default -> false;
                };
            };
            if(!valid)throw new IllegalArgumentException("Appeal change contradicts evidence");
        }
        public String explanation() {
            return "Food appeal at period " + period + ": " + status + "; latest unmet need " + unmet
                    + (previousUnmet == null ? "; prior appeal unknown or inactive" : "; previous appeal unmet " + previousUnmet)
                    + "; observed change " + change + "; imports " + imports + ", local production " + production
                    + ". Observed recovery does not prove the appeal caused it; no payment promised.";
        }
    }
    public static Snapshot observe(HouseholdNeeds.Assessment needs, VillagePriorities.Decision priority,
                                   Report food, Snapshot previous) {
        if (!needs.settlement().equals(priority.settlement()) || priority.period() > needs.period()
                || !food.commodity().equals(Commodity.FOOD) || food.demand()!=needs.demand() || food.fulfilled()!=needs.fulfilled() || food.condition()!=needs.sharedFood() || food.flow().households()!=needs.demand())
            throw new IllegalArgumentException("Appeal evidence disagrees");
        if (previous != null && (!previous.settlement().equals(needs.settlement()) || previous.period()!=needs.period()-1))
            throw new IllegalArgumentException("Appeal history out of order");
        long unmet=needs.demand()-needs.fulfilled();
        boolean wasOpen=previous!=null && previous.status()==Status.OPEN;
        boolean open=unmet>0 && priority.selected()==VillagePriorities.Action.SEEK_FOOD;
        Status status=open?Status.OPEN:wasOpen?(unmet==0?Status.RESOLVED:Status.WITHDRAWN):Status.NONE;
        Change change=Change.NONE;
        if(open) change=!wasOpen?Change.OPENED:unmet<previous.unmet()?Change.IMPROVED:unmet>previous.unmet()?Change.WORSENED:Change.UNCHANGED;
        else if(wasOpen) change=unmet==0?Change.RESOLVED:Change.WITHDRAWN;
        return new Snapshot(needs.settlement(),needs.period(),priority.period(),status,unmet,wasOpen?previous.unmet():null,
                food.flow().imports(),food.flow().production(),change);
    }
}
