package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CrossVillageBusinessTest {
    @TempDir Path directory;
    private AutomaticBusinessResaleTest f;
    private UUID remoteTown,remoteOwner,remoteSource;
    @BeforeEach void setup()throws Exception {
        f=new AutomaticBusinessResaleTest();f.directory=directory;f.setup();
        remoteTown=UUID.randomUUID();remoteOwner=UUID.randomUUID();remoteSource=UUID.randomUUID();
        f.db.saveSettlement(new Settlement(remoteTown,"Second village","World",200,64,0,1,0,0));
        f.db.getVillageIdentityRepository().bindSettlement(remoteTown,f.world);
        new TreasuryRepository(f.db.getConnection()).initialize(UUID.randomUUID(),remoteTown,30000,"Explicit second-village fixture capital");
        f.db.getPopulationRepository().insertResident(new Resident(remoteOwner,"Remote owner",remoteTown,null,300,Resident.Status.ALIVE));
        f.db.getPopulationRepository().recordSkillExperience(remoteOwner,"farming",10);
        var site=new BusinessSite(UUID.randomUUID(),remoteTown,f.world,200,64,0,0,"Explicit second-village site");
        var business=new Business(remoteSource,remoteTown,"Existing second-village business",remoteOwner,remoteOwner,"farming",site.id(),1,0);
        f.db.getBusinessRepository().found(new BusinessFounding(business,site,f.qualified,new JobRequirements(216,Map.of()),0,"Real fixture capital"));
        f.db.getIndustryRepository().openStore(remoteSource,100,Map.of(Commodity.STONE,1L),20000);
        f.db.getBusinessRepository().hire(UUID.randomUUID(),remoteSource,remoteOwner,0);
        // Local candidates are not trained owners; the remote buyer's qualification is the only changed opportunity.
        f.sql("UPDATE resident_skills SET experience=0 WHERE resident_id='"+f.buyer.id()+"'");
        f.db.getBusinessRepository().hire(UUID.randomUUID(),f.target.business().id(),f.other.id(),0);
    }
    @AfterEach void close(){if(f!=null)f.close();}
    private BusinessSales.Request request() {
        return new BusinessSales.Request(UUID.randomUUID(),f.target.business().id(),f.seller.id(),remoteOwner,1,remoteSource,1300,null,0);
    }
    @Test void automaticAcquisitionKeepsLocalAssetsStaffAndOnePrimaryJobAcrossRestart()throws Exception {
        long money=f.money();var id=f.target.business().id();var stock=f.db.getIndustryRepository().store(id).orElseThrow();
        var workers=f.db.getBusinessRepository().currentWorkers(id);
        String result=f.auto().review(id,1,1000);assertTrue(result.contains("Paid automatic sale"),result);
        assertEquals(remoteOwner,f.db.getBusinessRepository().findBusiness(id).orElseThrow().ownerId());
        assertEquals(f.target,f.db.getBusinessRepository().findFounding(id).orElseThrow());
        assertEquals(workers,f.db.getBusinessRepository().currentWorkers(id));assertEquals(stock,f.db.getIndustryRepository().store(id).orElseThrow());
        assertEquals(18700,f.cash(remoteSource));assertEquals(money,f.money());assertEquals(0,f.count("local_shipments"));
        assertTrue(new BusinessOperations(f.db.getConnection(),BusinessOperationPolicy.prototype()).evaluate(id).permitted());
        var runtime=new BusinessRuntime(f.db.getConnection(),8,1024);
        assertTrue(runtime.refresh(1,Set.of(f.world)).businesses().get(id).ownerAvailable());
        assertTrue(f.db.getBusinessRepository().currentWorkers(id).stream().allMatch(job->{try {
            return f.db.getPopulationRepository().findResident(job.residentId()).orElseThrow().settlementId().equals(f.town);
        }catch(SQLException problem){throw new RuntimeException(problem);}}));
        assertEquals(1,f.db.getBusinessRepository().currentWorkers(remoteSource).size());
        f.db.close();f.open();f.auto().review(id,2,100000);
        assertEquals(1,f.count("business_sales"));assertEquals(money,f.money());assertEquals(18700,f.cash(remoteSource));
        try(var s=f.db.getConnection().createStatement();var rows=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(rows.next());}
    }
    @Test void disabledScopeAndDifferentWorldCannotMoveAnyFundsOrOwnership()throws Exception {
        long before=f.money();var localOnly=new BusinessSales(f.db.getConnection(),new BusinessPurchasePolicy(1000,25,100));
        assertThrows(BusinessSales.Declined.class,()->localOnly.sell(request()));
        var different=UUID.randomUUID();f.db.getVillageIdentityRepository().observeWorld(different,"Different world");
        f.sql("UPDATE settlement_worlds SET world_id='"+different+"' WHERE settlement_id='"+remoteTown+"'");
        assertThrows(BusinessSales.Declined.class,()->new BusinessSales(f.db.getConnection(),BusinessPurchasePolicy.prototype()).sell(request()));
        f.auto().review(f.target.business().id(),1,1000);
        assertEquals(f.seller.id(),f.db.getBusinessRepository().findBusiness(f.target.business().id()).orElseThrow().ownerId());
        assertEquals(0,f.count("business_sales"));assertEquals(before,f.money());
    }
    @Test void failedRemotePaymentRollsBackAndQualifiedFamilyCanLaterInheritTheBranch()throws Exception {
        long money=f.money();f.sql("CREATE TRIGGER reject_remote_entry BEFORE INSERT ON business_sale_entries BEGIN SELECT RAISE(ABORT,'fixture payment failure'); END");
        assertThrows(SQLException.class,()->f.auto().review(f.target.business().id(),1,1000));
        assertEquals(0,f.count("business_sales"));assertEquals(0,f.count("business_sale_listings"));assertEquals(money,f.money());
        f.sql("DROP TRIGGER reject_remote_entry");f.auto().review(f.target.business().id(),1,1000);
        UUID heir=UUID.randomUUID();f.db.getPopulationRepository().insertResident(new Resident(heir,"Qualified family",remoteTown,null,300,Resident.Status.ALIVE));
        f.db.getPopulationRepository().recordSkillExperience(heir,"farming",10);
        f.db.getPopulationRepository().insertFamilyTie(new FamilyTie(remoteOwner,heir,FamilyTie.Type.PARENT_CHILD));
        f.db.getPopulationRepository().updateResident(new Resident(remoteOwner,"Remote owner",remoteTown,null,300,Resident.Status.DECEASED));
        var transfer=new BusinessSuccession(f.db.getConnection()).succeed(UUID.randomUUID(),f.target.business().id(),2).orElseThrow();
        assertEquals(heir,transfer.nextOwner());assertEquals(BusinessSuccession.Kind.FAMILY,transfer.kind());
        assertEquals(f.seller.id(),f.db.getBusinessRepository().findFounding(f.target.business().id()).orElseThrow().business().founderId());
        assertEquals(money,f.money());assertEquals(1,f.count("business_ownership"));
    }
    @Test void estateCannotSpendAcrossWorldsWhenNewRemotePurchasesAreDisabled()throws Exception {
        f.db.getPopulationRepository().recordSkillExperience(f.buyer.id(),"farming",10);
        f.db.getPopulationRepository().insertFamilyTie(new FamilyTie(remoteOwner,f.buyer.id(),FamilyTie.Type.PARENT_CHILD));
        f.db.getPopulationRepository().updateResident(new Resident(remoteOwner,"Estate family",remoteTown,null,300,Resident.Status.DECEASED));
        // Explicit fixture estate transferred from declared business capital, never minted by a production rule.
        f.sql("UPDATE industry_stores SET hundredths=hundredths-10000 WHERE business_id='"+remoteSource+"'");
        f.sql("INSERT INTO family_estates VALUES('"+remoteOwner+"',10000)");
        UUID otherWorld=UUID.randomUUID();f.db.getVillageIdentityRepository().observeWorld(otherWorld,"Other fixture world");
        f.sql("UPDATE settlement_worlds SET world_id='"+otherWorld+"' WHERE settlement_id='"+remoteTown+"'");
        var sale=new BusinessSales.Request(UUID.randomUUID(),f.target.business().id(),f.seller.id(),f.buyer.id(),1,null,0,remoteOwner,1300);
        long before=f.money();var localOnly=new BusinessPurchasePolicy(1000,0,100);
        assertThrows(BusinessSales.Declined.class,()->new BusinessSales(f.db.getConnection(),localOnly).sell(sale));
        var automatic=new AutomaticBusinessResale(f.db.getConnection(),BusinessResalePolicy.prototype(),localOnly,42);
        automatic.review(f.target.business().id(),1,1000);
        assertEquals(before,f.money());assertEquals(0,f.count("business_sales"));
        f.sql("UPDATE settlement_worlds SET world_id='"+f.world+"' WHERE settlement_id='"+remoteTown+"'");
        automatic.review(f.target.business().id(),2,2000);
        automatic.review(f.target.business().id(),3,3000); // A bounded buyer page may wrap before reconsidering changed funds.
        assertEquals(1,f.count("business_sales"));
        assertEquals(before,f.money());assertEquals(8700,new BusinessSales(f.db.getConnection(),localOnly).estate(remoteOwner).orElseThrow());
    }
}
