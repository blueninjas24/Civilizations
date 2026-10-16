package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockRepositoryTest {
    @TempDir Path directory;
    DatabaseManager db;
    final UUID town=new UUID(0,1),world=new UUID(0,2),owner=new UUID(0,3),worker=new UUID(0,4);
    final Commodity grain=new Commodity("grain"),milk=new Commodity("milk"),eggs=new Commodity("eggs");
    Business business;
    Map<UUID,Integer> roster;
    @BeforeEach void setup()throws Exception {
        reopen();db.saveSettlement(new Settlement(town,"Herd fixture","Herd world",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(world,"Herd world");db.getVillageIdentityRepository().bindSettlement(town,world);
        for(UUID person:List.of(owner,worker))db.getPopulationRepository().insertResident(new Resident(person,"Fixture adult",town,null,300,Resident.Status.ALIVE));
        var site=new BusinessSite(new UUID(0,5),town,world,0,64,0,1,"Trusted fixture herd site");
        business=new Business(new UUID(0,6),town,"Fixture herd",owner,owner,"livestock",site.id(),1,0);
        db.getBusinessRepository().found(new BusinessFounding(business,site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of()),0,"Fixture opportunity"));
        db.getBusinessRepository().hire(new UUID(0,7),business.id(),worker,0);
        db.getIndustryRepository().openStore(business.id(),256,Map.of(grain,1L,milk,1L,eggs,1L),0);
        roster=Map.of(new UUID(0,10),0,new UUID(0,11),0,new UUID(0,12),2);
        for(UUID animal:roster.keySet())new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(animal,world,LivestockAdoption.Ownership.VILLAGE,null,town,1,"Trusted test animal"));
    }
    void reopen()throws Exception{db=new DatabaseManager(directory.toFile(),Logger.getLogger("LivestockRepositoryTest"));db.initialize();}
    @AfterEach void close(){db.close();}
    LivestockRepository repo(){return new LivestockRepository(db.getConnection());}
    void sql(String sql)throws SQLException{try(var s=db.getConnection().createStatement()){s.execute(sql);}}
    long count(String table)throws SQLException{try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}}
    long units(Commodity commodity)throws SQLException{return db.getIndustryRepository().store(business.id()).orElseThrow().units().get(commodity);}
    void feed(long units)throws SQLException{sql("UPDATE industry_stock SET units="+units+" WHERE business_id='"+business.id()+"' AND commodity='grain'");}
    LivestockHerd.Plan advance(UUID operation,long period)throws SQLException{return repo().advance(operation,business.id(),period,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);}
    void openHerd()throws SQLException{repo().open(business.id(),LivestockHerd.Species.COW,grain,8,roster);}

    @Test void openingPreservesOwnershipAndRestartsWithoutRefillOrDuplicateHeads()throws Exception {
        openHerd();var before=repo().load(business.id()).orElseThrow();
        assertEquals(2,before.state().adults());assertEquals(List.of(new LivestockHerd.Young(2,1)),before.state().young());assertEquals(3,before.state().heads());
        assertEquals(before,repo().open(business.id(),LivestockHerd.Species.COW,grain,8,roster));
        assertThrows(SQLException.class,()->repo().open(business.id(),LivestockHerd.Species.COW,grain,80,roster));
        db.close();reopen();assertEquals(before,repo().load(business.id()).orElseThrow());assertEquals(3,count("animal_ownership"));assertEquals(0,units(milk));
    }
    @Test void actualFeedProductsAndBirthsCommitOnceAndHistoricalReplaySurvivesLaterPeriods()throws Exception {
        openHerd();feed(100);UUID operation=new UUID(0,100);
        var first=advance(operation,1);assertEquals(1,first.births());assertEquals(6,first.feedUsed());assertEquals(2,units(milk));assertEquals(94,units(grain));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT * FROM livestock_periods WHERE period=1")) {
            assertTrue(r.next());assertEquals(100,r.getLong("before_feed"));assertEquals(94,r.getLong("after_feed"));
            assertEquals(0,r.getLong("before_product"));assertEquals(2,r.getLong("after_product"));assertEquals(10,r.getLong("available_work"));
            assertTrue(r.getString("explanation").contains("qualified workers=1"));
        }
        assertEquals(first,advance(operation,1));assertEquals(94,units(grain));
        var second=advance(new UUID(0,101),2);assertEquals(1,second.matured());assertEquals(3,second.after().adults());
        db.close();reopen();assertEquals(first,advance(operation,1));assertEquals(second.after(),repo().load(business.id()).orElseThrow().state());
        assertThrows(SQLException.class,()->advance(new UUID(0,102),2));assertThrows(SQLException.class,()->advance(new UUID(0,103),4));
        assertThrows(SQLException.class,()->repo().advance(operation,business.id(),1,9,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true));
        assertEquals(91,units(grain));assertEquals(5,units(milk));assertEquals(0,db.getIndustryRepository().store(business.id()).orElseThrow().hundredths());
    }
    @Test void absentWorldDisabledPolicyAndNoEligibleLaborNeverManufactureProducts()throws Exception {
        openHerd();feed(100);var before=repo().load(business.id()).orElseThrow().state();
        assertEquals(before,repo().advance(new UUID(0,100),business.id(),1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),false).after());
        assertEquals(before,repo().advance(new UUID(0,101),business.id(),1,10,10,LivestockPolicy.disabled(),BusinessOperationPolicy.prototype(),true).after());
        assertEquals(1,count("livestock_periods"));assertEquals(100,units(grain));
        sql("UPDATE residents SET age_steps=1 WHERE id='"+worker+"'");
        var idle=advance(new UUID(0,102),1);assertEquals(0,idle.feedUsed());assertEquals(0,idle.births());assertEquals(0,units(milk));assertEquals(100,units(grain));
    }
    @Test void shortageAndFullStoragePreserveHeadsAndNeverBorrowPendingSpace()throws Exception {
        openHerd();var starved=advance(new UUID(0,100),1);assertEquals(3,starved.after().heads());assertEquals(0,starved.feedUsed());
        feed(256);var full=advance(new UUID(0,101),2);assertEquals(0,full.renewableUnits());assertEquals(0,full.births());assertEquals(0,full.feedUsed());assertEquals(3,full.after().heads());
        // Reserve the last six slots with actual incoming cargo from another real store.
        feed(250);var site=new BusinessSite(new UUID(0,20),town,world,10,64,0,1,"Fixture supplier");
        var supplier=new Business(new UUID(0,21),town,"Supplier",owner,owner,"warehouse",site.id(),1,0);
        db.getBusinessRepository().found(new BusinessFounding(supplier,site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of()),0,"Supplier fixture"));
        db.getIndustryRepository().openStore(supplier.id(),20,Map.of(grain,1L),0);
        sql("UPDATE industry_stock SET units=6 WHERE business_id='"+supplier.id()+"'");
        // Purchase requires a positive real price; seed test-only purchaser cash.
        sql("UPDATE industry_stores SET hundredths=1 WHERE business_id='"+business.id()+"'");
        db.getIndustryRepository().purchase(new UUID(0,22),supplier.id(),business.id(),grain,6,1,2,1,16);
        var reserved=advance(new UUID(0,102),3);assertEquals(0,reserved.renewableUnits());assertEquals(250,units(grain));assertEquals(6,db.getIndustryRepository().store(business.id()).orElseThrow().reservedIncomingSpace());
    }
    @Test void finalCheckpointFailureRollsBackFeedProductsAndCohortsForSafeRetry()throws Exception {
        openHerd();feed(100);var before=repo().load(business.id()).orElseThrow();
        sql("CREATE TRIGGER fail_herd_cohort BEFORE INSERT ON livestock_young WHEN NEW.period>0 BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
        UUID operation=new UUID(0,100);assertThrows(SQLException.class,()->advance(operation,1));
        assertEquals(before,repo().load(business.id()).orElseThrow());assertEquals(100,units(grain));assertEquals(0,units(milk));assertEquals(1,count("livestock_periods"));assertTrue(db.getConnection().getAutoCommit());
        sql("DROP TRIGGER fail_herd_cohort");assertEquals(1,advance(operation,1).births());assertEquals(94,units(grain));
    }
    @Test void unknownPlayerOrWrongWorldAnimalsAndReuseCannotSeedHerds()throws Exception {
        assertThrows(SQLException.class,()->repo().open(business.id(),LivestockHerd.Species.COW,grain,8,Map.of(new UUID(0,99),0)));
        UUID playerAnimal=new UUID(0,90);new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(playerAnimal,world,LivestockAdoption.Ownership.PLAYER,owner,null,1,"Player fixture"));
        assertThrows(SQLException.class,()->repo().open(business.id(),LivestockHerd.Species.COW,grain,8,Map.of(playerAnimal,0)));
        UUID otherWorld=new UUID(0,91),otherTown=new UUID(0,92),otherAnimal=new UUID(0,93);
        db.saveSettlement(new Settlement(otherTown,"Other fixture","Other world",0,64,0,1,0,0));
        db.getVillageIdentityRepository().observeWorld(otherWorld,"Other world");db.getVillageIdentityRepository().bindSettlement(otherTown,otherWorld);
        new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(otherAnimal,otherWorld,LivestockAdoption.Ownership.VILLAGE,null,otherTown,1,"Other world fixture"));
        assertThrows(SQLException.class,()->repo().open(business.id(),LivestockHerd.Species.COW,grain,8,Map.of(otherAnimal,0)));
        assertEquals(0,count("livestock_herds"));openHerd();
        var site=new BusinessSite(new UUID(0,30),town,world,20,64,0,1,"Other herd fixture");var other=new Business(new UUID(0,31),town,"Other herd",owner,owner,"livestock",site.id(),1,0);
        db.getBusinessRepository().found(new BusinessFounding(other,site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of()),0,"Other fixture"));
        db.getIndustryRepository().openStore(other.id(),20,Map.of(grain,1L,milk,1L),0);
        assertThrows(SQLException.class,()->repo().open(other.id(),LivestockHerd.Species.COW,grain,8,roster));assertTrue(repo().load(other.id()).isEmpty());assertEquals(1,count("livestock_herds"));
    }
    @Test void chickenOutputUsesDistinctEggsAndNoMeatOrMilk()throws Exception {
        repo().open(business.id(),LivestockHerd.Species.CHICKEN,grain,3,roster);feed(20);
        var first=advance(new UUID(0,100),1);assertEquals(0,first.births());assertEquals(2,units(eggs));assertEquals(0,units(milk));assertEquals(0,first.meatUnits());assertEquals(0,first.slaughtered());
    }
    @Test void longRunRestartReplayConservesActualFeedOutputAndHerdBounds()throws Exception {
        openHerd();long feedUsed=0,output=0;UUID lastOperation=null;LivestockHerd.Plan last=null;
        for(int period=1;period<=240;period++) {
            // Explicit test-only restocking/consumption, no production API manufactures these supplies.
            feed(100);sql("UPDATE industry_stock SET units=0 WHERE commodity='milk'");
            lastOperation=new UUID(1,period);last=advance(lastOperation,period);feedUsed+=last.feedUsed();output+=last.renewableUnits();
            assertEquals(100-last.feedUsed(),units(grain));assertEquals(last.renewableUnits(),units(milk));assertTrue(last.after().heads()<=8);
            assertEquals(last,advance(lastOperation,period));assertEquals(100-last.feedUsed(),units(grain));
            if(period%24==0){db.close();reopen();assertEquals(last.after(),repo().load(business.id()).orElseThrow().state());}
        }
        assertTrue(feedUsed>output);assertTrue(output>1000);assertEquals(last,advance(lastOperation,240));assertEquals(241,count("livestock_periods"));
    }
    @Test void schema75UpgradeAndInjectedFailurePreserveExistingOwnershipAndRecover()throws Exception {
        SchemaFixtures.rewind(db.getConnection(),75);
        sql("PRAGMA user_version=75");sql("CREATE VIEW livestock_young AS SELECT 1 AS fixture");
        assertThrows(SQLException.class,()->new SchemaMigrator().migrate(db.getConnection()));
        assertEquals(3,count("animal_ownership"));assertTrue(db.getConnection().getAutoCommit());
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA user_version")){r.next();assertEquals(75,r.getInt(1));}
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='livestock_herds'")){r.next();assertEquals(0,r.getInt(1));}
        sql("DROP VIEW livestock_young");new SchemaMigrator().migrate(db.getConnection());assertEquals(0,count("livestock_herds"));assertEquals(3,count("animal_ownership"));
        try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){assertFalse(r.next());}
    }
    @Test void missingProductsAndOtherIndustriesAreRefusedWithoutChangingExistingStores()throws Exception {
        assertThrows(SQLException.class,()->sql("INSERT INTO livestock_herds VALUES(NULL,'COW',8,'grain','invalid fixture')"));
        assertThrows(SQLException.class,()->sql("INSERT INTO livestock_initial_animals VALUES(NULL,'"+business.id()+"',0)"));
        sql("DELETE FROM industry_stock WHERE commodity='milk'");
        assertThrows(SQLException.class,()->repo().open(business.id(),LivestockHerd.Species.COW,grain,8,roster));assertEquals(0,count("livestock_herds"));
        assertThrows(IllegalArgumentException.class,()->repo().open(business.id(),LivestockHerd.Species.CHICKEN,eggs,8,roster));
        sql("UPDATE businesses SET industry='farming'");
        assertThrows(SQLException.class,()->repo().open(business.id(),LivestockHerd.Species.CHICKEN,grain,8,roster));assertEquals(0,count("livestock_herds"));assertEquals(3,count("animal_ownership"));
    }
    @Test void unavailableOwnerAndStaffedForSalePolicyUseActualEligibleEmployees()throws Exception {
        openHerd();feed(100);sql("UPDATE residents SET status='DECEASED' WHERE id='"+owner+"'");
        var paused=repo().advance(new UUID(0,100),business.id(),1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.disabled(),true);
        assertEquals(0,paused.feedUsed());assertEquals(0,units(milk));
        assertEquals(3,advance(new UUID(0,101),2).renewableUnits());assertEquals(93,units(grain));
        db.getBusinessRepository().endEmployment(new UUID(0,7),3,"Fixture staff departure");
        assertEquals(0,advance(new UUID(0,102),3).renewableUnits());assertEquals(93,units(grain));
    }
    @Test void realPurchasedFeedAndPaidMilkDeliveryPreserveGoodsAndMoney()throws Exception {
        openHerd();var site=new BusinessSite(new UUID(0,20),town,world,10,64,0,1,"Fixture market");
        var market=new Business(new UUID(0,21),town,"Market",owner,owner,"warehouse",site.id(),1,0);
        db.getBusinessRepository().found(new BusinessFounding(market,site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of()),0,"Market fixture"));
        db.getIndustryRepository().openStore(market.id(),256,Map.of(grain,1L,milk,1L),0);
        // Only opening fixture supplies/cash; all subsequent transfers use real repository payments/cargo.
        sql("UPDATE industry_stock SET units=12 WHERE business_id='"+market.id()+"' AND commodity='grain'");
        sql("UPDATE industry_stores SET hundredths=100 WHERE business_id='"+business.id()+"'");
        var order=new UUID(0,22);db.getIndustryRepository().purchase(order,market.id(),business.id(),grain,12,10,0,1,16);
        assertEquals(0,advance(new UUID(0,100),1).renewableUnits());assertEquals(0,units(grain));
        db.getIndustryRepository().deliver(order,1);assertEquals(12,units(grain));
        var produced=advance(new UUID(0,101),2);assertEquals(3,produced.renewableUnits());assertEquals(5,units(grain));
        var sale=new UUID(0,23);db.getIndustryRepository().purchase(sale,business.id(),market.id(),milk,3,6,2,1,16);
        assertEquals(0,units(milk));assertEquals(96,db.getIndustryRepository().store(business.id()).orElseThrow().hundredths());
        assertEquals(4,db.getIndustryRepository().store(market.id()).orElseThrow().hundredths());
        assertEquals(0,db.getIndustryRepository().store(market.id()).orElseThrow().units().get(milk));
        db.close();reopen();db.getIndustryRepository().deliver(sale,3);db.getIndustryRepository().deliver(sale,3);
        assertEquals(3,db.getIndustryRepository().store(market.id()).orElseThrow().units().get(milk));
        assertEquals(100,db.getIndustryRepository().store(market.id()).orElseThrow().hundredths()+db.getIndustryRepository().store(business.id()).orElseThrow().hundredths());
    }
}
