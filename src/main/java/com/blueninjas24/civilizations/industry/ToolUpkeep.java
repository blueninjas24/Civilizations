package com.blueninjas24.civilizations.industry;

/** Deterministic wear and fractional slowed work. State changes share the productive receipt transaction. */
public final class ToolUpkeep {
    public record State(long productivePeriods,long servicePeriods,int remainderPercent) {
        public State {if(productivePeriods<0||servicePeriods<0||remainderPercent<0||remainderPercent>99)throw new IllegalArgumentException("Invalid tool service state");}
        public static State initial(){return new State(0,0,0);}
    }
    public record Plan(State before,long requested,int remainderPercent,boolean useTool,boolean grace) {}
    public record Outcome(State after,int toolsUsed) {}
    private ToolUpkeep() {}
    public static Plan prepare(State state,long requested,long heldTools,ToolUpkeepPolicy policy) {
        if(requested<0||heldTools<0)throw new IllegalArgumentException("Invalid tool work resources");
        boolean grace=state.productivePeriods()<policy.startupGracePeriods();
        boolean use=!grace&&state.servicePeriods()==0&&heldTools>0;
        if(grace||state.servicePeriods()>0||heldTools>0)return new Plan(state,requested,state.remainderPercent(),use,grace);
        long credit=Math.addExact(Math.multiplyExact(requested,policy.missingToolsOutputPercent()),state.remainderPercent());
        return new Plan(state,credit/100,(int)(credit%100),false,false);
    }
    public static Outcome finish(Plan plan,long actualBatches,ToolUpkeepPolicy policy) {
        if(actualBatches<0||actualBatches>plan.requested())throw new IllegalArgumentException("Production exceeds tool-adjusted request");
        var before=plan.before();long productive=before.productivePeriods(),service=before.servicePeriods();int used=0;
        if(actualBatches>0) {
            productive=Math.incrementExact(productive);
            if(plan.useTool()){used=1;service=policy.workPeriodsPerTool();}
            if(!plan.grace()&&service>0)service--;
        }
        return new Outcome(new State(productive,service,plan.remainderPercent()),used);
    }
}
