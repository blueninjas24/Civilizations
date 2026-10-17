package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticBusinessResaleTest {
    @TempDir Path directory;DatabaseManager db;
    final UUID town=UUID.randomUUID(),world=UUID.randomUUID();
    Resident seller,buyer,other;BusinessFounding target,source;int site;
    final JobRequirements qualified=new JobRequirements(216,Map.of("farming",2L));
    @BeforeEach void setup()throws Exception {
        open();db.saveSettlement(new Settlement(town,"Sale fixture","World",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"World");db.getVillageIdentityRepository().bindSettlement(town,world);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,100000,"Actual fixture capital");
        seller=person(10);buyer=person(10);other=person(0);target=firm(seller);source=firm(buyer);
        db.getIndustryRepository().openStore(target.business().id(),100,Map.of(Commodity.STONE,1L),300);
        db.getIndustryRepository().openStore(source.business().id(),100,Map.of(Commodity.STONE,1L),10000);die(seller);
    }
    void open()throws SQLException{db=new DatabaseManager(directory.toFile(),Logger.getLogger("AutoResaleTest"));db.initialize();}
    @AfterEach void close(){db.close();}
    Resident person(long xp)throws SQLException {
        var p=new Resident(UUID.randomUUID(),"Fixture",town,null,300,Resident.Status.ALIVE);db.getPopulationRepository().insertResident(p);db.getPopulationRepository().recordSkillExperience(p.id(),"farming",xp);return p;
    }
    void die(Resident p)throws SQLException{db.getPopulationRepository().updateResident(new Resident(p.id(),p.name(),town,null,300,Resident.Status.DECEASED));}
    BusinessFounding firm(Resident p)throws SQLException {
        var s=new BusinessSite(UUID.randomUUID(),town,world,site++,64,0,0,"Fixture site");var b=new Business(UUID.randomUUID(),town,"Firm",p.id(),p.id(),"farming",s.id(),2,0);
        return db.getBusinessRepository().found(new BusinessFounding(b,s,qualified,new JobRequirements(216,Map.of()),0,"Actual fixture opportunity"));
    }
    AutomaticBusinessResale auto(){return new AutomaticBusinessResale(db.getConnection(),BusinessResalePolicy.prototype(),BusinessPurchasePolicy.prototype(),42);}
    @Test void actualMilkEggAndGrainStockCanBeSoldWithItsBusinessAndMoneyStaysReal()throws Exception{
        var owner=person(10);var location=new BusinessSite(UUID.randomUUID(),town,world,site++,64,0,1,"Explicit dairy resale fixture");
        var firm=new Business(UUID.randomUUID(),town,"Stocked dairy",owner.id(),owner.id(),"livestock",location.id(),1,0);
        db.getBusinessRepository().found(new BusinessFounding(firm,location,qualified,new JobRequirements(216,Map.of()),0,"Explicit qualified dairy"));
        var grain=new Commodity("grain");var milk=new Commodity("milk");var eggs=new Commodity("eggs");
        db.getIndustryRepository().openStore(firm.id(),100,Map.of(grain,1L,milk,1L,eggs,1L),300);
        for(var entry:Map.of(grain,5L,milk,2L,eggs,4L).entrySet())sql("UPDATE industry_stock SET units="+entry.getValue()+" WHERE business_id='"+firm.id()+"' AND commodity='"+entry.getKey().id()+"'");
        var investment=firm(buyer);db.getIndustryRepository().openStore(investment.business().id(),100,Map.of(Commodity.STONE,1L),20000);
        var stock=db.getIndustryRepository().store(firm.id()).orElseThrow().units();long before=money();die(owner);
        var review=auto().review(firm.id(),1,1000);assertTrue(review.contains("Paid automatic sale"),review);
        assertEquals(buyer.id(),db.getBusinessRepository().findBusiness(firm.id()).orElseThrow().ownerId());
        assertEquals(stock,db.getIndustryRepository().store(firm.id()).orElseThrow().units());assertEquals(before,money());
    }
    void sql(String query)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    long count(String table)throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){return r.getLong(1);}}
    long cash(UUID id)throws SQLException{return db.getIndustryRepository().store(id).orElseThrow().hundredths();}
    long money()throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT (SELECT SUM(hundredths) FROM treasuries)+(SELECT SUM(hundredths) FROM industry_stores)+(SELECT COALESCE(SUM(hundredths),0) FROM family_estates)")){return r.getLong(1);}}
    @Test void automaticOfferAndRealPaymentCommitTogetherAndRepeatRestartDoesNotCharge()throws Exception {
        long before=money();assertTrue(auto().review(target.business().id(),1,1000).contains("Paid automatic sale"));
        assertEquals(buyer.id(),db.getBusinessRepository().findBusiness(target.business().id()).orElseThrow().ownerId());
        assertEquals(8700,cash(source.business().id()));assertEquals(300,cash(target.business().id()));assertEquals(1300,new BusinessSales(db.getConnection(),BusinessPurchasePolicy.disabled()).estate(seller.id()).orElseThrow());assertEquals(before,money());
        assertEquals(BusinessSaleListings.Status.SOLD,new BusinessSaleListings(db.getConnection()).find(target.business().id(),seller.id()).orElseThrow().status());
        db.close();open();auto().review(target.business().id(),2,100000);assertEquals(before,money());assertEquals(1,count("business_sales"));assertEquals(8700,cash(source.business().id()));
    }
    @Test void failedFinalReviewRollsBackListingPaymentOwnershipAndEstateThenRetries()throws Exception {
        sql("CREATE TRIGGER reject_review BEFORE INSERT ON business_lifecycle_reviews BEGIN SELECT RAISE(ABORT,'injected'); END");
        long before=money();assertThrows(SQLException.class,()->auto().review(target.business().id(),1,1000));
        assertEquals(seller.id(),db.getBusinessRepository().findBusiness(target.business().id()).orElseThrow().ownerId());assertEquals(before,money());assertEquals(0,count("business_sales"));assertEquals(0,count("business_sale_listings"));assertEquals(0,count("family_estates"));
        sql("DROP TRIGGER reject_review");auto().review(target.business().id(),1,1000);assertEquals(1,count("business_sales"));assertEquals(before,money());
    }
    @Test void qualifiedFamilyAndExistingEmployeeInheritBeforeAnyAutomaticSale()throws Exception {
        var relative=person(10);db.getPopulationRepository().insertFamilyTie(new FamilyTie(seller.id(),relative.id(),FamilyTie.Type.PARENT_CHILD));
        assertTrue(auto().review(target.business().id(),1,1000).contains("inheritance"));assertEquals(0,count("business_sale_listings"));
        var runtime=new BusinessRuntime(db.getConnection(),8,1024);runtime.configureResale(BusinessResalePolicy.prototype(),BusinessPurchasePolicy.prototype(),42);
        runtime.refresh(1,Set.of(world),1000,true);assertEquals(relative.id(),db.getBusinessRepository().findBusiness(target.business().id()).orElseThrow().ownerId());assertEquals(0,count("business_sales"));
        var dead=person(10);var firm=firm(dead);var employee=person(10);db.getBusinessRepository().hire(UUID.randomUUID(),firm.business().id(),employee.id(),0);die(dead);
        runtime.refresh(2,Set.of(world),2000,true);assertEquals(employee.id(),db.getBusinessRepository().findBusiness(firm.business().id()).orElseThrow().ownerId());assertEquals(0,count("business_sales"));
    }
    @Test void savedPricesSurviveRetuningAndDisabledFundingCannotInventBuyerCash()throws Exception {
        var noMoney=new AutomaticBusinessResale(db.getConnection(),BusinessResalePolicy.prototype(),BusinessPurchasePolicy.disabled(),42);assertTrue(noMoney.review(target.business().id(),1,1000).contains("none funded"));
        var old=new BusinessSaleListings(db.getConnection()).find(target.business().id(),seller.id()).orElseThrow();assertEquals(1300,old.price());
        var tuned=new BusinessResalePolicy(true,9000,100,1000,32,Map.of());var resumed=new AutomaticBusinessResale(db.getConnection(),tuned,BusinessPurchasePolicy.prototype(),42);
        resumed.review(target.business().id(),2,2000);resumed.review(target.business().id(),2,2000);
        var saved=new BusinessSaleListings(db.getConnection()).find(target.business().id(),seller.id()).orElseThrow();assertEquals(old.price(),saved.price());assertEquals(old.deadlineMillis(),saved.deadlineMillis());assertEquals(8700,cash(source.business().id()));
    }
    @Test void searchPagesReachLaterResidentsAcrossRestartWithoutPopulationOrOwnershipCap()throws Exception {
        var policy=new BusinessResalePolicy(true,1000,50,600000,1,Map.of(Commodity.STONE,100L));
        for(int i=0;i<12;i++)person(0);
        for(int i=0;i<20&&count("business_sales")==0;i++){new AutomaticBusinessResale(db.getConnection(),policy,BusinessPurchasePolicy.prototype(),42).review(target.business().id(),i+1,1000+i);db.close();open();}
        assertEquals(1,count("business_sales"));
        for(int i=0;i<3;i++){var dead=person(10);var more=firm(dead);die(dead);auto().review(more.business().id(),30+i,2000+i);}
        try(var s=db.getConnection().prepareStatement("SELECT COUNT(*) FROM businesses WHERE owner_id=?")){s.setString(1,buyer.id().toString());try(var r=s.executeQuery()){assertEquals(5,r.getInt(1));}}
        assertEquals(0,count("business_employment"));
    }
    @Test void absentWorldAndUnsafeClockReviewCannotCreateOffersAndExpiryRetainsClosedAssets()throws Exception {
        var runtime=new BusinessRuntime(db.getConnection(),8,1024);runtime.configureResale(BusinessResalePolicy.prototype(),BusinessPurchasePolicy.disabled(),42);
        runtime.refresh(1,Set.of(),1000,true);assertEquals(0,count("business_sale_listings"));runtime.refresh(1,Set.of(world),1000,false);assertEquals(0,count("business_sale_listings"));
        runtime.refresh(1,Set.of(world),1000,true);assertEquals(1,count("business_sale_listings"));
        var offers=new BusinessSaleListings(db.getConnection());offers.observe(2000,Set.of());assertEquals(0,offers.find(target.business().id(),seller.id()).orElseThrow().elapsedMillis());
        offers.resume(1000000);offers.observe(1600000,Set.of(world));runtime.refresh(2,Set.of(world),1600000,true);
        assertEquals(1,count("business_closures"));assertEquals(300,cash(target.business().id()));assertEquals(0,count("business_sales"));
        runtime.refresh(3,Set.of(world),1700000,true);assertEquals(1,count("business_closures"));
    }
    @Test void invalidStockValuationDefersWithoutListingOrCashLoss()throws Exception {
        sql("UPDATE industry_stock SET units=1 WHERE business_id='"+target.business().id()+"' AND commodity='stone'");
        var unknown=new AutomaticBusinessResale(db.getConnection(),new BusinessResalePolicy(true,1000,50,600000,32,Map.of()),BusinessPurchasePolicy.prototype(),42);
        assertTrue(unknown.review(target.business().id(),1,1000).contains("No resale value"));assertEquals(0,count("business_sale_listings"));assertEquals(10000,cash(source.business().id()));
    }
    @Test void schema43FailureRollsBackReviewMigrationAndRetryPreservesCashSkills()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),43);sql("CREATE INDEX business_local_portfolio ON residents(name)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));assertEquals(10000,cash(source.business().id()));assertEquals(10,db.getPopulationRepository().findWorkProfile(buyer.id()).orElseThrow().experience().get("farming"));
        sql("DROP INDEX business_local_portfolio");new SchemaMigrator().migrate(db.getConnection());auto().review(target.business().id(),1,1000);assertEquals(8700,cash(source.business().id()));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void automaticBuyerCanCombineEligibleEstateAndRealBusinessCashWithoutSpendingUnrelatedFunds()throws Exception {
        var root=person(10);var sold=firm(root);var customer=person(10);var payer=firm(customer);
        db.getIndustryRepository().openStore(payer.business().id(),100,Map.of(Commodity.STONE,1L),5000);
        db.getPopulationRepository().updateResident(new Resident(buyer.id(),buyer.name(),town,null,100,Resident.Status.ALIVE));
        db.getPopulationRepository().insertFamilyTie(new FamilyTie(root.id(),buyer.id(),FamilyTie.Type.PARENT_CHILD));die(root);
        new BusinessSales(db.getConnection(),BusinessPurchasePolicy.prototype()).sell(new BusinessSales.Request(UUID.randomUUID(),sold.business().id(),root.id(),customer.id(),1,payer.business().id(),1000,null,0));
        db.getPopulationRepository().updateResident(buyer);
        var limited=new BusinessPurchasePolicy(1000,25,50);long before=money();
        new AutomaticBusinessResale(db.getConnection(),BusinessResalePolicy.prototype(),limited,42).review(target.business().id(),2,1000);
        assertEquals(buyer.id(),db.getBusinessRepository().findBusiness(target.business().id()).orElseThrow().ownerId());
        assertEquals(9200,cash(source.business().id()));assertEquals(500,new BusinessSales(db.getConnection(),limited).estate(root.id()).orElseThrow());assertEquals(before,money());
        var listing=new BusinessSaleListings(db.getConnection()).find(target.business().id(),seller.id()).orElseThrow();var entries=new BusinessSales(db.getConnection(),limited).entries(listing.saleOperation());assertEquals(3,entries.size());
    }
    @Test void ownedPendingCargoIsValuedAndRetainedWhenBusinessChangesOwner()throws Exception {
        var vendor=firm(buyer);db.getIndustryRepository().openStore(vendor.business().id(),100,Map.of(Commodity.STONE,1L),0);
        sql("UPDATE industry_stock SET units=1 WHERE business_id='"+vendor.business().id()+"' AND commodity='stone'");
        // Trusted delivered starting stock in a temporary fixture; payment and cargo use the real API.
        var shipment=db.getIndustryRepository().purchase(UUID.randomUUID(),vendor.business().id(),target.business().id(),Commodity.STONE,1,100,1,2,128);
        auto().review(target.business().id(),1,1000);var listing=new BusinessSaleListings(db.getConnection()).find(target.business().id(),seller.id()).orElseThrow();assertEquals(1300,listing.price());
        assertEquals(200,cash(target.business().id()));assertEquals(shipment,db.getIndustryRepository().shipment(shipment.id()).orElseThrow());
    }
}
