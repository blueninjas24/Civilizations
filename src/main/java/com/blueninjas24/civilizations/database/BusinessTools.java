package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Saved tool service and actual use tied to real productive receipts, not a second simulation thread. */
final class BusinessTools {
    private final Connection connection;
    private final IndustryRuntimePolicy policy;
    BusinessTools(Connection connection,IndustryRuntimePolicy policy){this.connection=connection;this.policy=policy;}
    ToolUpkeep.State state(UUID business)throws SQLException {
        try(var s=connection.prepareStatement("SELECT productive_periods,service_periods,remainder_percent FROM business_tools WHERE business_id=?")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){return r.next()?new ToolUpkeep.State(r.getLong(1),r.getLong(2),r.getInt(3)):ToolUpkeep.State.initial();}
        }
    }
    boolean applies(String kind,IndustryRepository.Store store){return policy.tools().applies(kind)&&store.spacePerUnit().containsKey(ToolUpkeepPolicy.TOOLS);}
    boolean replacementDue(UUID business,String kind,IndustryRepository.Store store)throws SQLException {
        if(!applies(kind,store))return false;var saved=state(business);
        if(kind.equals("logging")) {
            var firm=new BusinessRepository(connection).findBusiness(business).orElseThrow();
            if(!new IndustryRepository(connection).forestryHistoryTrusted(firm.siteId()))return false;
            var forest=new ForestryRepository(connection).find(firm.siteId());
            if(forest.isEmpty()||forest.orElseThrow().stock().capacity()==0)return false;
        }
        if(java.util.Set.of("mining","quarry","sandpit").contains(kind)) {
            UUID deposit=null;long remaining=0;int count=0;
            try(var s=connection.prepareStatement("SELECT d.id,d.remaining_batches FROM mineral_deposits d JOIN businesses b ON b.site_id=d.site_id WHERE b.id=? ORDER BY d.id LIMIT 2")) {
                s.setString(1,business.toString());try(var r=s.executeQuery()){while(r.next()){count++;deposit=UUID.fromString(r.getString(1));remaining=r.getLong(2);}}
            }
            if(count!=1||remaining==0||!new IndustryRepository(connection).sourceHistoryTrusted(deposit))return false;
        }
        return saved.productivePeriods()>=policy.tools().startupGracePeriods()&&saved.servicePeriods()<=1;
    }
    long missingFunded(UUID business,String kind,long workers,IndustryRepository.Store store,Map<Commodity,Long> pending)throws SQLException {
        if(workers==0||!replacementDue(business,kind,store)||!policy.supply().enabled())return 0;
        if(store.units().getOrDefault(ToolUpkeepPolicy.TOOLS,0L)>0||pending.getOrDefault(ToolUpkeepPolicy.TOOLS,0L)>0)return 0;
        return store.hundredths()>=policy.tools().toolValueHundredths()&&store.freeSpace()>=store.spacePerUnit().get(ToolUpkeepPolicy.TOOLS)?1:0;
    }
    String acquireRows(UUID business,String kind,long period,long haulWindow,BusinessOperationPolicy operations)throws SQLException {
        var store=new IndustryRepository(connection).store(business).orElseThrow();
        if(!replacementDue(business,kind,store))return "No tool replacement due or retained store lacks tool support";
        var supply=policy.purchasingSupply();var one=new WorkshopSupplyPolicy(supply.enabled(),1,supply.warehousesPerReview(),supply.unitHundredths());
        var recipe=new ProductionRecipe("maintenance_tools",kind,Map.of(ToolUpkeepPolicy.TOOLS,1L),Map.of(ToolUpkeepPolicy.TOOLS,1L),1,1);
        return "Tool replacement: "+new WorkshopSupplies(connection,operations).acquireRows(business,recipe,period,haulWindow,policy.millisPerPeriod(),one,policy.mining());
    }
    ToolUpkeep.Plan prepare(UUID business,String kind,long requested)throws SQLException {
        var store=new IndustryRepository(connection).store(business).orElseThrow();
        return applies(kind,store)?ToolUpkeep.prepare(state(business),requested,store.units().get(ToolUpkeepPolicy.TOOLS),policy.tools()):null;
    }
    String finishRows(UUID operation,UUID business,long period,ToolUpkeep.Plan plan,long quantity)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Tool upkeep requires active productive transaction");
        if(plan==null)return "Tool upkeep disabled/not configured, or retained store requires explicit tool support";
        var after=ToolUpkeep.finish(plan,quantity,policy.tools());
        if(after.toolsUsed()>0) {
            var use=UUID.nameUUIDFromBytes(("civilizations:tool-use-v1:"+operation).getBytes(StandardCharsets.UTF_8));
            new IndustryRepository(connection).consumeRows(use,business,ToolUpkeepPolicy.TOOLS,after.toolsUsed(),IndustryRepository.Use.MAINTENANCE,
                    "Tool service begun by actual productive industry period "+period);
        }
        try(var s=connection.prepareStatement("INSERT INTO business_tools VALUES(?,?,?,?) ON CONFLICT(business_id) DO UPDATE SET productive_periods=excluded.productive_periods,service_periods=excluded.service_periods,remainder_percent=excluded.remainder_percent")) {
            s.setString(1,business.toString());s.setLong(2,after.after().productivePeriods());s.setLong(3,after.after().servicePeriods());s.setInt(4,after.after().remainderPercent());s.executeUpdate();
        }
        String reason="Tool upkeep: used "+after.toolsUsed()+"; productive periods "+after.after().productivePeriods()+"; service remaining "+after.after().servicePeriods()+"; requested "+plan.requested()+"; fractional slowed work "+after.after().remainderPercent()+"/100";
        try(var s=connection.prepareStatement("INSERT INTO business_tool_reviews VALUES(?,?,?,?,?,?)")) {
            s.setString(1,operation.toString());s.setString(2,business.toString());s.setLong(3,period);s.setInt(4,after.toolsUsed());s.setString(5,policy.tools().toString());s.setString(6,reason);s.executeUpdate();
        }
        return reason;
    }
}
