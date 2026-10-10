package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import com.blueninjas24.civilizations.application.PopulationSession;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class BusinessSalesTest {
    @TempDir Path directory;
    DatabaseManager db;
    BusinessRepository firms;
    BusinessSales sales;
    UUID town,world;
    Resident seller,buyer,payer;
    BusinessFounding target,source;
    int site;
    static final JobRequirements QUALIFIED=new JobRequirements(216,Map.of("farming",10L));
    @BeforeEach void setup()throws Exception {
        open();town=UUID.randomUUID();world=UUID.randomUUID();
        db.saveSettlement(new Settlement(town,"Fixture Town","Fixture World",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Fixture World");db.getVillageIdentityRepository().bindSettlement(town,world);
        new TreasuryRepository(db.getConnection()).initialize(UUID.randomUUID(),town,100000,"Test-owned starting funds");
        seller=person(20);buyer=person(20);payer=person(20);
        target=firm(seller);source=firm(buyer);
        new IndustryRepository(db.getConnection()).openStore(source.business().id(),100,Map.of(new Commodity("stone"),1L),6000);
        new IndustryRepository(db.getConnection()).openStore(target.business().id(),100,Map.of(new Commodity("stone"),1L),300);
        die(seller);
    }
    void open()throws SQLException {
        db=new DatabaseManager(directory.toFile(),Logger.getLogger("BusinessSalesTest"));db.initialize();firms=db.getBusinessRepository();
        sales=new BusinessSales(db.getConnection(),BusinessPurchasePolicy.prototype());
    }
    @AfterEach void close(){db.close();}
    Resident person(long experience)throws SQLException {
        var p=new Resident(UUID.randomUUID(),"Fixture Person",town,null,300,Resident.Status.ALIVE);
        db.getPopulationRepository().insertResident(p);db.getPopulationRepository().recordSkillExperience(p.id(),"farming",experience);return p;
    }
    void die(Resident p)throws SQLException {db.getPopulationRepository().updateResident(new Resident(p.id(),p.name(),p.settlementId(),null,p.ageSteps(),Resident.Status.DECEASED));}
    void child(Resident p,Resident c)throws SQLException {db.getPopulationRepository().insertFamilyTie(new FamilyTie(p.id(),c.id(),FamilyTie.Type.PARENT_CHILD));}
    BusinessFounding firm(Resident p)throws SQLException {
        var s=new BusinessSite(UUID.randomUUID(),town,world,site++,64,0,0,"Fixture-owned working site");
        var b=new Business(UUID.randomUUID(),town,"Fixture Firm",p.id(),p.id(),"farming",s.id(),2,0);
        return firms.found(new BusinessFounding(b,s,QUALIFIED,QUALIFIED,0,"Fixture qualified opportunity"));
    }
    BusinessSales.Request request(long cash,UUID estate,long estateCash) {
        return new BusinessSales.Request(UUID.randomUUID(),target.business().id(),seller.id(),buyer.id(),1,
                cash==0?null:source.business().id(),cash,estate,estateCash);
    }
    long cash(UUID b)throws SQLException{return new IndustryRepository(db.getConnection()).store(b).orElseThrow().hundredths();}
    void sql(String query)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(query);}}
    long count(String table)throws SQLException {try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}}
    long allMoney()throws SQLException {
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT (SELECT COALESCE(SUM(hundredths),0) FROM treasuries)+(SELECT COALESCE(SUM(hundredths),0) FROM industry_stores)+(SELECT COALESCE(SUM(hundredths),0) FROM family_estates)")){r.next();return r.getLong(1);}
    }
    UUID earnedEstate(Resident beneficiary)throws SQLException {
        var root=person(20);var old=db.getPopulationRepository().findResident(beneficiary.id()).orElseThrow();
        // A child not yet trained cannot inherit this business. Sale creates the money, never a test grant to the estate.
        db.getPopulationRepository().updateResident(new Resident(old.id(),old.name(),town,null,100,Resident.Status.ALIVE));
        child(root,beneficiary);var sold=firm(root);var customer=person(20);var funding=firm(customer);
        new IndustryRepository(db.getConnection()).openStore(funding.business().id(),100,Map.of(new Commodity("stone"),1L),10000);
        die(root);
        sales.sell(new BusinessSales.Request(UUID.randomUUID(),sold.business().id(),root.id(),customer.id(),1,funding.business().id(),2000,null,0));
        db.getPopulationRepository().updateResident(old);return root.id();
    }
    @Test void cashPurchaseConservesAllMoneyAssetsHistoryAndReplaysAfterRestartAndDisable()throws Exception {
        long money=allMoney();var q=request(1000,null,0);var before=new IndustryRepository(db.getConnection()).store(target.business().id()).orElseThrow();
        assertEquals(q,sales.sell(q));assertEquals(buyer.id(),firms.findBusiness(q.business()).orElseThrow().ownerId());
        assertEquals(target,firms.findFounding(q.business()).orElseThrow());assertEquals(before,new IndustryRepository(db.getConnection()).store(q.business()).orElseThrow());
        assertEquals(5000,cash(source.business().id()));assertEquals(1000,sales.estate(seller.id()).orElseThrow());assertEquals(money,allMoney());
        assertEquals(2,sales.entries(q.operation()).size());assertEquals(1,count("business_sales"));
        db.close();open();var disabled=new BusinessSales(db.getConnection(),BusinessPurchasePolicy.disabled());
        assertEquals(q,disabled.sell(q));assertEquals(money,allMoney());assertEquals(5000,cash(source.business().id()));
        assertThrows(SQLException.class,()->disabled.sell(new BusinessSales.Request(q.operation(),q.business(),q.seller(),q.buyer(),2,q.cashSource(),q.cashAmount(),null,0)));
        assertThrows(SQLException.class,()->sales.sell(request(1000,null,0)));assertEquals(money,allMoney());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void bothApprovedFundingSourcesPayOneFamilyEstateWithoutCreatingMoney()throws Exception {
        UUID estate=earnedEstate(buyer);long money=allMoney();var q=request(1000,estate,500);
        sales.sell(q);assertEquals(5000,cash(source.business().id()));assertEquals(1500,sales.estate(estate).orElseThrow());
        assertEquals(1500,sales.estate(seller.id()).orElseThrow());assertEquals(money,allMoney());assertEquals(3,sales.entries(q.operation()).size());
        long debit=sales.entries(q.operation()).stream().filter(e->e.direction().equals("PAYMENT")).mapToLong(e->e.before()-e.after()).sum();
        long credit=sales.entries(q.operation()).stream().filter(e->e.direction().equals("PROCEEDS")).mapToLong(e->e.after()-e.before()).sum();
        assertEquals(q.price(),debit);assertEquals(debit,credit);
    }
    @Test void estateOnlyPurchaseUsesRealPriorSaleProceeds()throws Exception {
        var estate=earnedEstate(buyer);long money=allMoney();sales.sell(request(0,estate,2000));
        assertEquals(0,sales.estate(estate).orElseThrow());assertEquals(2000,sales.estate(seller.id()).orElseThrow());
        assertEquals(6000,cash(source.business().id()));assertEquals(money,allMoney());
    }
    @Test void insufficientOrDisabledFundingRejectsWithoutOpeningEstate()throws Exception {
        long money=allMoney();assertThrows(SQLException.class,()->sales.sell(request(1251,null,0)));
        assertThrows(SQLException.class,()->new BusinessSales(db.getConnection(),BusinessPurchasePolicy.disabled()).sell(request(1,null,0)));
        assertEquals(0,count("family_estates"));assertEquals(0,count("business_sales"));assertEquals(money,allMoney());
        sales.sell(request(1250,null,0));assertEquals(4750,cash(source.business().id()));
    }
    @Test void unrelatedOrMissingEstatesCannotFinanceBuyer()throws Exception {
        UUID estate=earnedEstate(payer);long money=allMoney();
        assertThrows(SQLException.class,()->sales.sell(request(0,estate,100)));
        child(seller,buyer);assertThrows(SQLException.class,()->sales.sell(request(0,seller.id(),100)));
        assertEquals(money,allMoney());assertTrue(sales.estate(seller.id()).isEmpty());
    }
    @Test void fundingBusinessMustBelongToBuyerAndCannotBeTarget()throws Exception {
        var other=firm(payer);new IndustryRepository(db.getConnection()).openStore(other.business().id(),100,Map.of(new Commodity("stone"),1L),6000);
        var q=new BusinessSales.Request(UUID.randomUUID(),target.business().id(),seller.id(),buyer.id(),1,other.business().id(),100,null,0);
        assertThrows(SQLException.class,()->sales.sell(q));
        assertThrows(IllegalArgumentException.class,()->new BusinessSales.Request(UUID.randomUUID(),target.business().id(),seller.id(),buyer.id(),1,target.business().id(),100,null,0));
    }
    @Test void qualifiedFamilyAndEmployeeCannotBeBypassedByPaidSale()throws Exception {
        var heir=person(20);child(seller,heir);assertThrows(SQLException.class,()->sales.sell(request(1000,null,0)));
        die(heir);var employee=person(20);firms.hire(UUID.randomUUID(),target.business().id(),employee.id(),0);
        assertThrows(SQLException.class,()->sales.sell(request(1000,null,0)));assertEquals(0,count("business_sales"));
    }
    @Test void underageUntrainedDeadOrMovedBuyersAreRejected()throws Exception {
        for(var state:List.of(new Resident(buyer.id(),buyer.name(),town,null,100,Resident.Status.ALIVE),
                new Resident(buyer.id(),buyer.name(),town,null,300,Resident.Status.DECEASED))) {
            db.getPopulationRepository().updateResident(state);assertThrows(SQLException.class,()->sales.sell(request(1000,null,0)));
        }
        db.getPopulationRepository().updateResident(buyer);
        UUID elsewhere=UUID.randomUUID();db.saveSettlement(new Settlement(elsewhere,"Elsewhere","Fixture World",20,64,0,1,0,0));
        sql("UPDATE residents SET settlement_id='"+elsewhere+"' WHERE id='"+buyer.id()+"'");
        assertThrows(SQLException.class,()->sales.sell(request(1000,null,0)));
        sql("UPDATE residents SET settlement_id='"+town+"' WHERE id='"+buyer.id()+"'");
        var untrained=person(0);var q=new BusinessSales.Request(UUID.randomUUID(),target.business().id(),seller.id(),untrained.id(),1,null,0,UUID.randomUUID(),1);
        assertThrows(SQLException.class,()->sales.sell(q));assertEquals(0,count("business_sales"));
    }
    @Test void primaryOccupationCannotBeChangedByPurchasingAnotherIndustry()throws Exception {
        var s=new BusinessSite(UUID.randomUUID(),town,world,site++,64,0,0,"Fixture other industry");
        var b=new Business(UUID.randomUUID(),town,"Fixture Mine",payer.id(),payer.id(),"mining",s.id(),2,0);
        firms.found(new BusinessFounding(b,s,QUALIFIED,QUALIFIED,0,"Fixture occupation constraint"));
        firms.hire(UUID.randomUUID(),b.id(),buyer.id(),0);
        assertThrows(SQLException.class,()->sales.sell(request(1000,null,0)));assertEquals(0,count("business_sales"));
    }
    @Test void ownerApiRefusesAbsentWorldAndFutureDatesButReplaysAcceptedSaleAfterRestart()throws Exception {
        var q=new BusinessSales.Request(UUID.randomUUID(),target.business().id(),seller.id(),buyer.id(),0,source.business().id(),1000,null,0);
        db.close();
        var settings=new PopulationSession.Settings(42,1000,0,2,new PopulationPolicy(12,216,240,480,720,0,0,0),MigrationPolicy.disabled(),
                com.blueninjas24.civilizations.settlement.VillageRecognition.Policy.prototype(),VillageEnrollmentRepository.Policy.prototype());
        try {
            try(var session=new PopulationSession(directory.toFile(),Logger.getLogger("BusinessSaleOwnerTest"),settings,()->1000)) {
                session.configureBusinessPurchases(BusinessPurchasePolicy.prototype());session.start(false,Set.of()).get(5,TimeUnit.SECONDS);
                assertThrows(ExecutionException.class,()->session.sellBusiness(q).get(5,TimeUnit.SECONDS));
                assertThrows(ExecutionException.class,()->session.openBusinessSale(q.business(),1000,1000).get(5,TimeUnit.SECONDS));
                assertNull(session.status().failure());
                session.updateLoadedWorlds(Set.of(world)).get(5,TimeUnit.SECONDS);
                session.openBusinessSale(q.business(),1000,1000).get(5,TimeUnit.SECONDS);
                assertThrows(ExecutionException.class,()->session.sellBusiness(request(1000,null,0)).get(5,TimeUnit.SECONDS));
                assertNull(session.status().failure());
                assertEquals(q,session.sellBusiness(q).get(5,TimeUnit.SECONDS));
                assertThrows(IllegalStateException.class,()->session.configureBusinessPurchases(BusinessPurchasePolicy.disabled()));
            }
            try(var restarted=new PopulationSession(directory.toFile(),Logger.getLogger("BusinessSaleOwnerTest"),settings,()->100000)) {
                restarted.start(false,Set.of()).get(5,TimeUnit.SECONDS);
                assertEquals(q,restarted.sellBusiness(q).get(5,TimeUnit.SECONDS));
            }
        }finally{open();}
        assertEquals(1000,sales.estate(seller.id()).orElseThrow());assertEquals(5000,cash(source.business().id()));assertEquals(1,count("business_sales"));
        assertEquals(BusinessSaleListings.Status.SOLD,new BusinessSaleListings(db.getConnection()).find(q.business(),seller.id()).orElseThrow().status());
    }
    @Test void savedListingTermsReplayWithoutResettingDeadlineOrChangingPrice()throws Exception {
        var listings=new BusinessSaleListings(db.getConnection());var listing=listings.open(target.business().id(),1000,1000,1000,1);
        assertEquals(listing,listings.open(target.business().id(),1000,1000,2000,2));
        assertThrows(SQLException.class,()->listings.open(target.business().id(),1001,1000,2000,2));
        assertThrows(SQLException.class,()->listings.open(source.business().id(),1000,1000,1000,1));
        var heir=person(20);child(seller,heir);
        // Already accepted terms remain saved; no payment can bypass newly available inheritance.
        assertEquals(listing,listings.open(target.business().id(),1000,1000,3000,3));
        assertThrows(SQLException.class,()->sales.sell(request(1000,null,0)));
    }
    @Test void absenceAndDowntimePreserveEarnedTimeAndExpiryRefusesNewPaymentWithoutDeletingAssets()throws Exception {
        var listings=new BusinessSaleListings(db.getConnection());var id=target.business().id();long money=allMoney();
        listings.open(id,1000,1000,1000,1);listings.observe(1500,Set.of(world));listings.observe(100000,Set.of());
        assertEquals(500,listings.find(id,seller.id()).orElseThrow().elapsedMillis());db.close();open();listings=new BusinessSaleListings(db.getConnection());
        listings.resume(1000000);listings.observe(1000499,Set.of(world));assertEquals(BusinessSaleListings.Status.OPEN,listings.find(id,seller.id()).orElseThrow().status());
        listings.observe(1000500,Set.of(world));assertEquals(BusinessSaleListings.Status.EXPIRED,listings.find(id,seller.id()).orElseThrow().status());
        assertThrows(SQLException.class,()->sales.sell(request(1000,null,0)));assertEquals(money,allMoney());assertEquals(target,firms.findFounding(id).orElseThrow());
        assertEquals(seller.id(),firms.findBusiness(id).orElseThrow().ownerId());assertEquals(300,cash(id));assertEquals(0,count("business_sales"));
    }
    @Test void listingPriceAndSaleCompletionAreAtomicWithMoneyAndAcceptedReplay()throws Exception {
        var listings=new BusinessSaleListings(db.getConnection());var id=target.business().id();listings.open(id,1000,1000,1000,1);
        assertThrows(SQLException.class,()->sales.sell(request(999,null,0)));assertEquals(6000,cash(source.business().id()));
        var q=request(1000,null,0);sales.sell(q);var sold=listings.find(id,seller.id()).orElseThrow();
        assertEquals(BusinessSaleListings.Status.SOLD,sold.status());assertEquals(q.operation(),sold.saleOperation());
        listings.observe(Long.MAX_VALUE,Set.of(world));assertEquals(sold,listings.find(id,seller.id()).orElseThrow());assertEquals(q,sales.sell(q));
    }
    @Test void listingCompletionFailureRollsBackOwnerPaymentAndEstateThenRetriesOnce()throws Exception {
        var listings=new BusinessSaleListings(db.getConnection());var id=target.business().id();var before=listings.open(id,1000,1000,1000,1);long money=allMoney();
        sql("CREATE TRIGGER fail_listing_completion BEFORE UPDATE OF status ON business_sale_listings WHEN NEW.status='SOLD' BEGIN SELECT RAISE(ABORT,'fixture listing failure'); END");
        var q=request(1000,null,0);assertThrows(SQLException.class,()->sales.sell(q));assertEquals(before,listings.find(id,seller.id()).orElseThrow());
        assertEquals(seller.id(),firms.findBusiness(id).orElseThrow().ownerId());assertEquals(money,allMoney());assertEquals(0,count("family_estates"));assertTrue(sales.find(q.operation()).isEmpty());
        sql("DROP TRIGGER fail_listing_completion");sales.sell(q);sales.sell(q);assertEquals(money,allMoney());assertEquals(1,count("business_sales"));
    }
    @Test void newlyQualifiedInheritanceResolvesAnOpenOfferWithoutChargingBuyer()throws Exception {
        var listings=new BusinessSaleListings(db.getConnection());var id=target.business().id();listings.open(id,1000,1000,1000,1);
        child(seller,buyer);new BusinessSuccession(db.getConnection()).succeed(UUID.randomUUID(),id,1).orElseThrow();listings.observe(1500,Set.of(world));
        assertEquals(BusinessSaleListings.Status.RESOLVED,listings.find(id,seller.id()).orElseThrow().status());assertEquals(6000,cash(source.business().id()));assertEquals(0,count("family_estates"));
    }
    @Test void unexpectedlyMissingAcceptedListingCannotCommitPaymentWithoutItsCompletion()throws Exception {
        var listings=new BusinessSaleListings(db.getConnection());var id=target.business().id();var old=listings.open(id,1000,1000,1000,1);
        sql("CREATE TRIGGER remove_sale_listing AFTER UPDATE OF owner_id ON businesses BEGIN DELETE FROM business_sale_listings WHERE business_id=NEW.id; END");
        var q=request(1000,null,0);long money=allMoney();assertThrows(SQLException.class,()->sales.sell(q));
        assertEquals(old,listings.find(id,seller.id()).orElseThrow());assertEquals(seller.id(),firms.findBusiness(id).orElseThrow().ownerId());
        assertEquals(money,allMoney());assertTrue(sales.find(q.operation()).isEmpty());assertEquals(0,count("family_estates"));
    }
    @Test void clockFailureRollsBackAndRetryExpiresWithBoundedArithmetic()throws Exception {
        var listings=new BusinessSaleListings(db.getConnection());var id=target.business().id();var before=listings.open(id,1000,1000,1000,1);
        sql("CREATE TRIGGER fail_listing_clock BEFORE UPDATE OF last_ms ON business_sale_listings BEGIN SELECT RAISE(ABORT,'fixture clock failure'); END");
        assertThrows(SQLException.class,()->listings.observe(1500,Set.of(world)));assertEquals(before,listings.find(id,seller.id()).orElseThrow());assertTrue(db.getConnection().getAutoCommit());
        sql("DROP TRIGGER fail_listing_clock");listings.observe(900,Set.of(world));assertEquals(before,listings.find(id,seller.id()).orElseThrow());
        listings.observe(Long.MAX_VALUE,Set.of(world));assertEquals(1000,listings.find(id,seller.id()).orElseThrow().elapsedMillis());
    }
    @Test void schema39ListingUpgradeFailureRetainsFundsAndRetries()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),39);sql("CREATE INDEX business_open_sale_listings ON businesses(id)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));assertThrows(SQLException.class,()->count("business_sale_listings"));
        assertEquals(6000,cash(source.business().id()));assertEquals(target,firms.findFounding(target.business().id()).orElseThrow());
        sql("DROP INDEX business_open_sale_listings");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,count("business_sale_listings"));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void failedWorldBoundarySuspendsNewSalesUntilCauseIsFixedAndSessionRestarts()throws Exception {
        var q=new BusinessSales.Request(UUID.randomUUID(),target.business().id(),seller.id(),buyer.id(),0,source.business().id(),1000,null,0);
        new BusinessSaleListings(db.getConnection()).open(q.business(),1000,1000,1000,0);
        sql("CREATE TRIGGER fail_listing_boundary BEFORE UPDATE OF last_ms ON business_sale_listings WHEN NEW.last_ms>OLD.last_ms BEGIN SELECT RAISE(ABORT,'fixture boundary failure'); END");
        db.close();var now=new java.util.concurrent.atomic.AtomicLong(1000);
        var settings=new PopulationSession.Settings(42,1000,0,2,new PopulationPolicy(12,216,240,480,720,0,0,0),MigrationPolicy.disabled(),
                com.blueninjas24.civilizations.settlement.VillageRecognition.Policy.prototype(),VillageEnrollmentRepository.Policy.prototype());
        try {
            try(var session=new PopulationSession(directory.toFile(),Logger.getLogger("ListingBoundaryTest"),settings,now::get)) {
                session.configureBusinessPurchases(BusinessPurchasePolicy.prototype());session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
                now.set(1500);session.updateLoadedWorlds(Set.of()).get(5,TimeUnit.SECONDS);assertTrue(session.businessSaleDiagnostic().contains("fixture boundary failure"));
                session.updateLoadedWorlds(Set.of(world)).get(5,TimeUnit.SECONDS);
                assertThrows(ExecutionException.class,()->session.sellBusiness(q).get(5,TimeUnit.SECONDS));
            }
            open();assertEquals(6000,cash(source.business().id()));assertTrue(sales.find(q.operation()).isEmpty());
            sql("DROP TRIGGER fail_listing_boundary");db.close();now.set(100000);
            try(var resumed=new PopulationSession(directory.toFile(),Logger.getLogger("ListingBoundaryTest"),settings,now::get)) {
                resumed.configureBusinessPurchases(BusinessPurchasePolicy.prototype());resumed.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
                assertEquals(q,resumed.sellBusiness(q).get(5,TimeUnit.SECONDS));
            }
        }finally{open();}
        assertEquals(1,count("business_sales"));assertEquals(5000,cash(source.business().id()));
    }
    @Test void allPaymentAndOwnershipChangesRollbackWhenAuditFailsAndRetryOnce()throws Exception {
        var estate=earnedEstate(buyer);long money=allMoney();var q=request(1000,estate,500);
        sql("CREATE TRIGGER fail_sale BEFORE UPDATE OF owner_id ON businesses BEGIN SELECT RAISE(ABORT,'fixture sale failure'); END");
        assertThrows(SQLException.class,()->sales.sell(q));assertTrue(db.getConnection().getAutoCommit());
        assertEquals(seller.id(),firms.findBusiness(q.business()).orElseThrow().ownerId());assertEquals(6000,cash(source.business().id()));
        assertEquals(2000,sales.estate(estate).orElseThrow());assertTrue(sales.estate(seller.id()).isEmpty());assertTrue(sales.find(q.operation()).isEmpty());assertEquals(money,allMoney());
        sql("DROP TRIGGER fail_sale");sales.sell(q);sales.sell(q);assertEquals(money,allMoney());assertEquals(3,sales.entries(q.operation()).size());
    }
    @Test void schema38FailedUpgradeRollsBackRetainsBusinessFundsAndCanRetry()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),38);sql("CREATE INDEX business_sale_history ON businesses(id)");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        assertThrows(SQLException.class,()->count("family_estates"));assertEquals(target,firms.findFounding(target.business().id()).orElseThrow());assertEquals(6000,cash(source.business().id()));
        sql("DROP INDEX business_sale_history");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,count("business_sales"));
    }
}
