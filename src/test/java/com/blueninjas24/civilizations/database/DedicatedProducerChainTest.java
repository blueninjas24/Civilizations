package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DedicatedProducerChainTest {
    @TempDir Path directory;
    DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID(),owner=UUID.randomUUID();
    final UUID warehouse=new UUID(0,1),customer=new UUID(0,2);
    final Commodity food=new Commodity("food"),stone=new Commodity("stone"),sand=new Commodity("sand"),output=new Commodity("finished");
    final Map<Commodity,Long> sizes=Map.of(food,1L,stone,1L,sand,1L,output,1L,ToolUpkeepPolicy.TOOLS,1L,Commodity.COAL,1L);

    @BeforeEach void setup()throws Exception {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("DedicatedProducerChainTest"));db.initialize();
        db.saveSettlement(new Settlement(town,"Fixture","Fixture",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
        resident(owner);new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,5000,"Fixture capital");
        business(warehouse,"warehouse",1000);worker(warehouse);
        business(customer,"consumer",1000);worker(customer);
    }
    @AfterEach void close(){if(db!=null)db.close();}
    void resident(UUID id)throws Exception{db.getPopulationRepository().insertResident(new Resident(id,"Resident",town,null,300,Resident.Status.ALIVE));}
    void worker(UUID business)throws Exception{UUID id=UUID.randomUUID();resident(id);db.getBusinessRepository().hire(UUID.randomUUID(),business,id,0);}
    UUID business(UUID id,String kind,long cash)throws Exception {
        var site=new BusinessSite(UUID.randomUUID(),town,world,(int)id.getLeastSignificantBits(),40,0,1,"Fixture site");var req=new JobRequirements(216,Map.of());
        db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,kind,owner,owner,kind,site.id(),2,0),site,req,req,0,"Fixture opportunity"));
        db.getIndustryRepository().openStore(id,100,sizes,cash);return site.id();
    }
    IndustryRuntimePolicy policy(String kind,Commodity input) {
        var recipe=new ProductionRecipe("dedicated_input",kind,Map.of(input,1L),Map.of(output,1L),1,1);
        return new IndustryRuntimePolicy(true,1000,8,new IndustryPolicy(10,1,1,100,1,128,Map.of(Commodity.COAL,1L),sizes),Map.of(recipe.id(),recipe),
                new WorkshopSupplyPolicy(true,1,16,Map.of(input,100L)),new WarehouseSupplyPolicy(true,16,16),
                new ToolUpkeepPolicy(true,0,1,50,100,Set.of("quarry")));
    }
    @ParameterizedTest @ValueSource(strings={"fishing","quarry","sandpit"})
    void registeredDedicatedFirmsSupplyOnlyTheirSavedOutput(String kind)throws Exception {
        Commodity good=kind.equals("fishing")?food:kind.equals("quarry")?stone:sand;
        UUID site=db.getBusinessRepository().findBusiness(customer).orElseThrow().siteId();
        try(var s=db.getConnection().prepareStatement("UPDATE businesses SET industry=? WHERE id=?")){s.setString(1,kind);s.setString(2,customer.toString());s.executeUpdate();}
        if(kind.equals("fishing")) {
            try(var s=db.getConnection().prepareStatement("INSERT INTO fishery_resources(site_id,world_id,capacity,available,remainder,observed_ms,opening_terms) VALUES(?,?,128,96,0,1,'fixture')")) {
                s.setString(1,site.toString());s.setString(2,world.toString());s.executeUpdate();
            }
            db.getIndustryRepository().fish(UUID.randomUUID(),customer,1,0,FishingPolicy.prototype());
        } else {
            UUID deposit=UUID.randomUUID();db.getIndustryRepository().discover(deposit,site,1,10,Map.of(good,2L));
            try(var s=db.getConnection().prepareStatement("INSERT INTO mineral_discovery(deposit_id,world_id,mineral,observed_millis) VALUES(?,?,?,1)")) {
                s.setString(1,deposit.toString());s.setString(2,world.toString());s.setString(3,good.id());s.executeUpdate();
            }
            db.getIndustryRepository().mine(UUID.randomUUID(),customer,deposit,1,0,1,1);
        }
        // A separate supported recipe business creates genuine demand for the producer's commodity.
        UUID buyer=UUID.randomUUID();business(buyer,"consumer",1000);worker(buyer);
        db.getConnection().setAutoCommit(false);
        try(var s=db.getConnection().prepareStatement("INSERT INTO industry_runtime(business_id,cadence,last_ms,remainder,target,completed) VALUES(?,1000,0,0,0,0)")) {
            s.setString(1,warehouse.toString());s.executeUpdate();
        }
        var review=new LocalIndustryDemand(db.getConnection(),policy("consumer",good),BusinessOperationPolicy.disabled()).review(town,Set.of(world),8);
        assertTrue(review.complete(),review.explanation());
        new WarehouseSupplies(db.getConnection(),policy("consumer",good),BusinessOperationPolicy.disabled()).acquireRows(warehouse,world,1,2);
        db.getConnection().commit();db.getConnection().setAutoCommit(true);
        assertEquals(1,count("local_shipments"));
        assertEquals(900,db.getIndustryRepository().store(warehouse).orElseThrow().hundredths());
        assertEquals(1100,db.getIndustryRepository().store(customer).orElseThrow().hundredths());
        assertEquals(0,db.getIndustryRepository().store(warehouse).orElseThrow().units().get(good));
        assertEquals(1,db.getIndustryRepository().store(warehouse).orElseThrow().reservedIncomingSpace());
        try(var s=db.getConnection().prepareStatement("SELECT commodity,delivered_period FROM local_shipments")){try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(good.id(),r.getString(1));assertTrue(r.getObject(2)==null);}}
        db.getIndustryRepository().deliver(UUID.nameUUIDFromBytes(("civilizations:warehouse-input-v1:"+warehouse+":1:"+customer+":"+good.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),2);
        assertEquals(1,db.getIndustryRepository().store(warehouse).orElseThrow().units().get(good));
        assertEquals(0,db.getIndustryRepository().store(warehouse).orElseThrow().reservedIncomingSpace());
        long sellerCash=db.getIndustryRepository().store(customer).orElseThrow().hundredths();
        db.getIndustryRepository().deliver(UUID.nameUUIDFromBytes(("civilizations:warehouse-input-v1:"+warehouse+":1:"+customer+":"+good.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),2);
        assertEquals(1,db.getIndustryRepository().store(warehouse).orElseThrow().units().get(good));
        assertEquals(sellerCash,db.getIndustryRepository().store(customer).orElseThrow().hundredths());
    }
    long count(String table)throws Exception{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    @Test void unregisteredUnknownIndustryStillDefersLocalDemand()throws Exception {
        business(UUID.randomUUID(),"retained-unknown",0);
        var review=new LocalIndustryDemand(db.getConnection(),policy("consumer",food),BusinessOperationPolicy.disabled()).review(town,Set.of(world),8);
        assertFalse(review.complete());assertTrue(review.explanation().contains("Unsupported stored industry"));
    }
    @ParameterizedTest @ValueSource(strings={"fishing","quarry","sandpit"})
    void dedicatedIndustryWithoutMatchingSavedResourceStillDefers(String kind)throws Exception {
        business(UUID.randomUUID(),kind,0);
        var review=new LocalIndustryDemand(db.getConnection(),policy("consumer",food),BusinessOperationPolicy.disabled()).review(town,Set.of(world),8);
        assertFalse(review.complete());assertTrue(review.explanation().contains("Unsupported stored industry"));
    }
    @Test void exhaustedQuarryCannotCreateReplacementToolDemand()throws Exception {
        UUID quarry=UUID.randomUUID(),site=business(quarry,"quarry",1000),deposit=UUID.randomUUID();worker(quarry);
        db.getIndustryRepository().discover(deposit,site,1,2,Map.of(stone,2L));
        try(var s=db.getConnection().prepareStatement("INSERT INTO mineral_discovery(deposit_id,world_id,mineral,observed_millis) VALUES(?,?,?,1)")) {
            s.setString(1,deposit.toString());s.setString(2,world.toString());s.setString(3,"stone");s.executeUpdate();
        }
        db.getIndustryRepository().mine(UUID.randomUUID(),quarry,deposit,1,0,1,1);
        var review=new LocalIndustryDemand(db.getConnection(),policy("consumer",food),BusinessOperationPolicy.disabled()).review(town,Set.of(world),8);
        assertTrue(review.complete(),review.explanation());assertEquals(1,review.uncovered().get(ToolUpkeepPolicy.TOOLS));
        db.getIndustryRepository().mine(UUID.randomUUID(),quarry,deposit,1,1,1,1);
        review=new LocalIndustryDemand(db.getConnection(),policy("consumer",food),BusinessOperationPolicy.disabled()).review(town,Set.of(world),8);
        assertTrue(review.complete(),review.explanation());assertFalse(review.uncovered().containsKey(ToolUpkeepPolicy.TOOLS));
    }
}
