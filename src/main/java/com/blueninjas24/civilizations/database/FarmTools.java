package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Actual food-clock tool work; productive state/use/food receipt commit together. */
final class FarmTools {
    private final Connection connection;
    private final FarmToolUpkeepPolicy policy;
    private final FarmToolBudgetPolicy budget;
    private final BusinessOperationPolicy operations;
    private final IndustryPolicy transport;
    private final LinkedHashMap<UUID,String> suppliers=new LinkedHashMap<>();
    FarmTools(Connection connection,FarmToolUpkeepPolicy policy,FarmToolBudgetPolicy budget,
              BusinessOperationPolicy operations,IndustryPolicy transport) {
        this.connection=connection;this.policy=policy;this.budget=budget;this.operations=operations;this.transport=transport;
    }
    ToolUpkeep.State state(UUID farm)throws SQLException {
        try(var s=connection.prepareStatement("SELECT productive_periods,service_periods,remainder_percent FROM business_tools WHERE business_id=?")) {
            s.setString(1,farm.toString());try(var r=s.executeQuery()){return r.next()?new ToolUpkeep.State(r.getLong(1),r.getLong(2),r.getInt(3)):ToolUpkeep.State.initial();}
        }
    }
    private boolean supported(UUID farm,Optional<IndustryRepository.Store> saved)throws SQLException {
        if(saved.isEmpty())return true;
        var store=saved.orElseThrow();
        if(store.capacity()!=1||store.hundredths()!=0||!store.spacePerUnit().equals(Map.of(ToolUpkeepPolicy.TOOLS,1L)))return false;
        try(var s=connection.prepareStatement("SELECT 1 FROM farm_tool_purchases WHERE farm_id=? LIMIT 1")) {
            s.setString(1,farm.toString());try(var r=s.executeQuery()){return r.next();}
        }
    }
    void deliverRows(UUID town,long period)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Farm delivery requires food transaction");
        var arrivals=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT t.id FROM local_shipments t JOIN farm_tool_purchases p ON p.operation_id=t.id
            WHERE p.farm_id=? AND p.settlement_id=? AND t.delivered_period IS NULL AND t.due_period<=?
            ORDER BY t.due_period,t.id LIMIT 129
            """)) {
            s.setString(1,FarmEnterprises.businessId(town).toString());s.setString(2,town.toString());s.setLong(3,period);
            try(var r=s.executeQuery()){while(r.next())arrivals.add(UUID.fromString(r.getString(1)));}
        }
        if(arrivals.size()>128)throw new SQLException("Farm paid cargo exceeds bounded delivery review");
        var industry=new IndustryRepository(connection);for(var id:arrivals)industry.deliverRows(id,period);
    }
    ToolUpkeep.Plan prepareRows(UUID town,long maximum)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Farm upkeep requires food transaction");
        if(!policy.enabled())return null;
        UUID farm=FarmEnterprises.businessId(town);
        if(new BusinessRepository(connection).findBusiness(farm).isEmpty())return null;
        var saved=new IndustryRepository(connection).store(farm);if(!supported(farm,saved))return null;
        long held=saved.map(s->s.units().getOrDefault(ToolUpkeepPolicy.TOOLS,0L)).orElse(0L);
        return ToolUpkeep.prepare(state(farm),maximum,held,policy.workPolicy(budget.toolHundredths()));
    }
    void finishRows(UUID town,long period,ToolUpkeep.Plan plan,long output)throws SQLException {
        if(connection.getAutoCommit())throw new SQLException("Farm upkeep requires food transaction");
        if(plan==null)return;
        UUID farm=FarmEnterprises.businessId(town);var after=ToolUpkeep.finish(plan,output,policy.workPolicy(budget.toolHundredths()));
        if(after.toolsUsed()>0)new IndustryRepository(connection).consumeRows(id("civilizations:farm-tool-use-v1:"+town+":"+period),farm,
                ToolUpkeepPolicy.TOOLS,after.toolsUsed(),IndustryRepository.Use.MAINTENANCE,"Toolkit service begun by actual farm food period "+period);
        try(var s=connection.prepareStatement("INSERT INTO business_tools VALUES(?,?,?,?) ON CONFLICT(business_id) DO UPDATE SET productive_periods=excluded.productive_periods,service_periods=excluded.service_periods,remainder_percent=excluded.remainder_percent")) {
            s.setString(1,farm.toString());s.setLong(2,after.after().productivePeriods());s.setLong(3,after.after().servicePeriods());s.setInt(4,after.after().remainderPercent());s.executeUpdate();
        }
        String reason="Farm tools: actual food "+output+"; kits used "+after.toolsUsed()+"; productive periods "+after.after().productivePeriods()
                +"; service remaining "+after.after().servicePeriods()+"; slowed fraction "+after.after().remainderPercent()+"/100";
        try(var s=connection.prepareStatement("INSERT INTO farm_tool_work VALUES(?,?,'food',?,?,?,?,?,?)")) {
            s.setString(1,town.toString());s.setLong(2,period);s.setString(3,farm.toString());s.setLong(4,output);s.setLong(5,plan.requested());
            s.setInt(6,after.toolsUsed());s.setString(7,policy.toString());s.setString(8,reason);s.executeUpdate();
        }
    }
    String reviewPurchase(UUID town,long period,Set<UUID> worlds)throws SQLException {
        if(!connection.getAutoCommit())throw new SQLException("Farm order review requires idle SQL owner");
        if(!policy.enabled()||!budget.enabled())return "New public farm tool orders disabled";
        if(worlds==null)return "Farm tool ordering paused: world availability unknown";
        UUID farm=FarmEnterprises.businessId(town);
        var businesses=new BusinessRepository(connection);var business=businesses.findBusiness(farm);
        if(business.isEmpty())return "No saved farm for equipment ordering";
        if(!new BusinessOperations(connection,operations).evaluate(farm).permitted())return "Farm equipment order waits for operating staff/owner";
        var industry=new IndustryRepository(connection);var store=industry.store(farm);
        if(!supported(farm,store))return "Retained farm cash/catalog requires explicit equipment transition";
        var state=state(farm);
        if(state.productivePeriods()<policy.startupGracePeriods()||state.servicePeriods()>1)return "No productive farm replacement due";
        if(store.isPresent()&&store.orElseThrow().freeSpace()<1)return "Farm already owns equipment or paid incoming kit";
        String cursor=suppliers.getOrDefault(town,"");var candidates=page(town,cursor);
        if(candidates.isEmpty()&&!cursor.isEmpty())candidates=page(town,"");
        String last="",reason="No eligible toolkit supplier in bounded local page";
        for(var seller:candidates) {
            last=seller.toString();
            if(!new BusinessOperations(connection,operations).evaluate(seller).permitted())continue;
            try {
                var request=new FarmToolPurchases.Request(id("civilizations:farm-tool-order-v1:"+town+":"+period+":"+seller),farm,seller,period,
                        transport.localDeliveryPeriods(),transport.localHaulUnitsPerPeriod());
                new FarmToolPurchases(connection,operations).purchase(request,budget,worlds);
                reason="Actual public toolkit paid; delivery after food period "+Math.addExact(period,transport.localDeliveryPeriods());break;
            }catch(FarmToolPurchases.Declined declined){reason="Farm equipment order waits: "+declined.getMessage();break;}
        }
        suppliers.remove(town);suppliers.put(town,last);while(suppliers.size()>4096)suppliers.remove(suppliers.keySet().iterator().next());
        return reason+"; bounded supplier candidates "+candidates.size();
    }
    private List<UUID> page(UUID town,String cursor)throws SQLException {
        var candidates=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
            SELECT b.id FROM businesses b JOIN industry_stock k ON k.business_id=b.id
            WHERE b.settlement_id=? AND b.id>? AND b.industry IN('warehouse','blacksmith') AND k.commodity='tools' AND k.units>=1
            ORDER BY b.id LIMIT ?
            """)) {
            s.setString(1,town.toString());s.setString(2,cursor);s.setInt(3,policy.suppliersPerReview());
            try(var r=s.executeQuery()){while(r.next())candidates.add(UUID.fromString(r.getString(1)));}
        }
        return candidates;
    }
    Optional<String> explanation(UUID town,long period)throws SQLException {
        try(var s=connection.prepareStatement("SELECT explanation FROM farm_tool_work WHERE settlement_id=? AND period=?")) {
            s.setString(1,town.toString());s.setLong(2,period);try(var r=s.executeQuery()){return r.next()?Optional.of(r.getString(1)):Optional.empty();}
        }
    }
    private static UUID id(String value){return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));}
}
