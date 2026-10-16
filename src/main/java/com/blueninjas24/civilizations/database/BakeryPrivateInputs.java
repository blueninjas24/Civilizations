package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.WorkEligibility;
import java.sql.*;
import java.util.*;

/** Narrow first bread batch from actual privately owned grain; no public-stock conversion. */
final class BakeryPrivateInputs {
    record Review(BakeryInvestment.PrivateInputs quote,WorkshopStarterPlan.Review bundle,String explanation) {}
    private final Connection connection;
    private final WorkshopStarterPolicy policy;
    private final BusinessOperationPolicy operations;
    BakeryPrivateInputs(Connection c,WorkshopStarterPolicy p,BusinessOperationPolicy o){connection=c;policy=p;operations=o;}
    Review review(UUID town,UUID world,ProductionRecipe recipe,long cash,long capacity,long haul,Map<Commodity,Long> sizes)throws SQLException {
        if(!recipe.inputs().keySet().equals(Set.of(FarmMarketStore.GRAIN)))return new Review(null,null,"Private first-batch route currently requires grain-only bread inputs");
        var ids=new ArrayList<UUID>();
        try(var s=connection.prepareStatement("""
                SELECT b.id FROM businesses b JOIN industry_stores k ON k.business_id=b.id JOIN business_sites p ON p.id=b.site_id
                  JOIN industry_stock g ON g.business_id=b.id AND g.commodity='grain'
                WHERE b.settlement_id=? AND p.world_id=? AND g.units>=?
                  AND (b.industry='warehouse' OR (b.industry='farming' AND EXISTS(SELECT 1 FROM farm_market_stores f WHERE f.business_id=b.id)))
                ORDER BY b.id LIMIT 65
                """)) {
            s.setString(1,town.toString());s.setString(2,world.toString());s.setLong(3,Math.addExact(recipe.inputs().get(FarmMarketStore.GRAIN),policy.reserveUnits()));
            try(var r=s.executeQuery()){while(r.next())ids.add(UUID.fromString(r.getString(1)));}
        }
        if(ids.size()>64)return new Review(null,null,"Private grain supplier budget exceeded; no partial selection");
        var businesses=new BusinessRepository(connection);var people=new PopulationRepository(connection);var availability=new BusinessOperations(connection,operations);
        for(var id:ids) {
            var founding=businesses.findFounding(id).orElseThrow();
            if(!founding.site().worldId().equals(world)||!availability.evaluate(id).permitted())continue;
            var store=new IndustryRepository(connection).store(id).orElseThrow();
            if(founding.business().industry().equals("farming")&&!FarmMarketStore.supported(connection,id,store))continue;
            boolean staffed=false;
            for(var job:businesses.currentWorkers(id)) {
                var person=people.findResident(job.residentId()).orElseThrow();
                if(person.settlementId().equals(town)&&WorkEligibility.evaluate(person,people.findWorkProfile(person.id()).orElseThrow(),founding.workerRequirements()).eligible()){staffed=true;break;}
            }
            if(!staffed)continue;
            var plan=WorkshopStarterPlan.review(recipe,1,policy,cash,capacity,haul,store.units(),sizes);
            if(plan.eligible())return new Review(new BakeryInvestment.PrivateInputs(id,plan.price()),plan,"Real paid private grain starter with retained supplier reserve");
        }
        return new Review(null,null,"No qualified operating private grain supplier with a complete affordable reserved batch");
    }
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){s.execute("""
            CREATE TABLE bakery_private_starters (
                business_id TEXT PRIMARY KEY REFERENCES businesses(id),
                shipment_id TEXT NOT NULL UNIQUE,
                seller_id TEXT NOT NULL REFERENCES businesses(id),
                terms TEXT NOT NULL CHECK(length(terms) BETWEEN 1 AND 8192),
                FOREIGN KEY(shipment_id,business_id) REFERENCES local_shipments(id,buyer_id)
            )
            """);}
    }
    private static String terms(BakeryInvestment.Draft draft){return "bakery-private-starter-v1:"+draft.privateInputs().seller()+":"+draft.privateInputs().price()+":"+draft.supplies().terms();}
    static boolean recorded(Connection c,UUID business)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM bakery_private_starters WHERE business_id=?")){s.setString(1,business.toString());try(var r=s.executeQuery()){return r.next();}}
    }
    static void requireReplay(Connection c,BakeryInvestment.Draft draft)throws SQLException {
        try(var s=c.prepareStatement("SELECT p.* FROM bakery_private_starters p JOIN local_shipments s ON s.id=p.shipment_id AND s.buyer_id=p.business_id AND s.seller_id=p.seller_id WHERE p.business_id=?")) {
            s.setString(1,draft.anchor().business().toString());try(var r=s.executeQuery()) {
                if(!r.next()||!r.getString("shipment_id").equals(draft.supplies().operation().toString())||!r.getString("seller_id").equals(draft.privateInputs().seller().toString())||!r.getString("terms").equals(terms(draft)))
                    throw new SQLException("Private bakery starter identity or accepted terms changed");
            }
        }
    }
    static void purchaseRows(Connection c,BakeryInvestment.Draft draft,long now,IndustryRuntimePolicy industry)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Private starter requires complete founding transaction");
        var request=draft.supplies();var quote=Objects.requireNonNull(draft.privateInputs());
        // The new business's delivery clock starts now, with no invented past work.
        try(var s=c.prepareStatement("INSERT INTO industry_runtime VALUES(?,?,?,0,0,0)")) {
            s.setString(1,request.business().toString());s.setLong(2,industry.millisPerPeriod());s.setLong(3,now);s.executeUpdate();
        }
        new IndustryRepository(c).purchaseRows(request.operation(),quote.seller(),request.business(),FarmMarketStore.GRAIN,request.recipe().inputs().get(FarmMarketStore.GRAIN),quote.price(),
                request.period(),request.delay(),request.haulCapacity(),now/industry.millisPerPeriod(),industry.millisPerPeriod());
        try(var s=c.prepareStatement("INSERT INTO bakery_private_starters VALUES(?,?,?,?)")) {
            s.setString(1,request.business().toString());s.setString(2,request.operation().toString());s.setString(3,quote.seller().toString());s.setString(4,terms(draft));s.executeUpdate();
        }
    }
}
