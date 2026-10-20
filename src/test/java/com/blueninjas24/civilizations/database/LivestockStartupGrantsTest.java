package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/** Actual startup composition only. Animal identities and stock belong to each temporary fixture. */
class LivestockStartupGrantsTest {
    @TempDir Path directory;CraftsmanInvestmentTest f;
    BusinessStartupGrants.WorkshopRequest request;Map<UUID,Integer> animals;UUID source;final Commodity grain=new Commodity("grain"),milk=new Commodity("milk"),eggs=new Commodity("eggs");
    @BeforeEach void setup()throws Exception {
        f=new CraftsmanInvestmentTest();f.directory=directory;f.setup();f.f.database.getPopulationRepository().recordSkillExperience(f.candidate,"farming",10);
        var site=new BusinessSite(UUID.randomUUID(),f.f.town,f.f.world,1,64,1,1000,"Trusted existing herd site fixture");
        var firm=new Business(UUID.randomUUID(),f.f.town,"Explicit herd fixture",f.candidate,f.candidate,"livestock",site.id(),2,0);
        var founding=new BusinessFounding(firm,site,new JobRequirements(216,Map.of("farming",10L)),new JobRequirements(216,Map.of("farming",1L)),500,"Trusted herd opportunity");
        request=new BusinessStartupGrants.WorkshopRequest(founding,128,Map.of(grain,1L,milk,1L,eggs,1L,new Commodity("wool"),1L),4000,f.candidate);
        animals=Map.of(UUID.randomUUID(),0,UUID.randomUUID(),0,UUID.randomUUID(),2);
        for(var id:animals.keySet())new AnimalOwnershipRepository(f.f.database.getConnection()).record(new AnimalOwnershipRepository.Claim(id,f.f.world,LivestockAdoption.Ownership.VILLAGE,null,f.f.town,1000,"Explicit confirmed fixture animal"));
        source=f.f.business("warehouse",40).id();f.f.industry.openStore(source,100,Map.of(grain,1L),0);
        var worker=UUID.randomUUID();f.f.database.getPopulationRepository().insertResident(new Resident(worker,"Fixture feed merchant",f.f.town,null,300,Resident.Status.ALIVE));f.f.database.getBusinessRepository().hire(UUID.randomUUID(),source,worker,0);
        f.f.sql("UPDATE industry_stock SET units=24 WHERE business_id='"+source+"' AND commodity='grain'");
        f.economy.pulse(3000,false); // Fresh assessment includes this actual supplier resident.
    }
    @AfterEach void close(){f.close();}
    BusinessStartupGrants engine(){return new BusinessStartupGrants(f.f.database.getConnection());}
    BusinessStartupGrants.Livestock start(LivestockHerd.Species species)throws SQLException{return engine().startLivestock(request,species,grain,8,animals,StartupGrantPolicy.prototype());}
    IndustryRepository.Store store()throws SQLException{return f.f.industry.store(request.founding().business().id()).orElseThrow();}
    @ParameterizedTest @EnumSource(LivestockHerd.Species.class)
    void realProtectedGrantAndHerdOpenTogetherThenActualPaidFeedProducesGoods(LivestockHerd.Species species)throws Exception {
        long cash=new TreasuryRepository(f.f.database.getConnection()).load(f.f.town).orElseThrow().hundredths();var ownership=animals.keySet().stream().map(id->{try{return new AnimalOwnershipRepository(f.f.database.getConnection()).load(id).orElseThrow();}catch(SQLException e){throw new RuntimeException(e);}}).toList();
        var opened=start(species);assertEquals(3,opened.herd().state().heads());assertEquals(2,opened.herd().state().adults());assertEquals(4000,opened.startup().store().hundredths());assertEquals(4500,opened.startup().grant().beforeCash()-opened.startup().grant().afterCash());
        assertEquals(cash-4500,new TreasuryRepository(f.f.database.getConnection()).load(f.f.town).orElseThrow().hundredths());assertTrue(store().units().values().stream().allMatch(x->x==0));
        assertEquals(24,f.f.industry.store(source).orElseThrow().units().get(grain));
        for(var claim:ownership)assertEquals(claim,new AnimalOwnershipRepository(f.f.database.getConnection()).load(claim.animal()).orElseThrow());
        var shipment=UUID.randomUUID();f.f.industry.purchase(shipment,source,request.founding().business().id(),grain,10,1000,0,1,128);assertEquals(0,store().units().get(grain));assertEquals(10,store().reservedIncomingSpace());
        assertThrows(SQLException.class,()->f.f.industry.deliver(shipment,0));f.f.industry.deliver(shipment,1);
        var used=new LivestockRepository(f.f.database.getConnection()).advance(UUID.randomUUID(),request.founding().business().id(),1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);
        assertEquals(6,used.feedUsed());assertEquals(2,store().units().get(species.product()));assertEquals(4,store().units().get(grain));
        long total=new TreasuryRepository(f.f.database.getConnection()).load(f.f.town).orElseThrow().hundredths()+store().hundredths()+f.f.stock(f.f.warehouse).hundredths()+f.f.stock(f.f.shop).hundredths()+f.f.industry.store(source).orElseThrow().hundredths();assertEquals(29500,total,"Only recorded500 startup expense leaves cash circulation");
        assertFalse(engine().preview(f.f.town,"craftsman",500,4000,StartupGrantPolicy.prototype()).eligible(),"Livestock uses the same shared period budget");
    }
    @Test void restartReplayReturnsCurrentAssetsWithoutRefillingCashFeedAnimalsOrJobs()throws Exception {
        start(LivestockHerd.Species.COW);var id=request.founding().business().id();var ship=UUID.randomUUID();f.f.industry.purchase(ship,source,id,grain,10,1000,0,1,128);f.f.industry.deliver(ship,1);
        new LivestockRepository(f.f.database.getConnection()).advance(UUID.randomUUID(),id,1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);
        var episode=f.f.database.getBusinessRepository().currentWorkers(id).getFirst();f.f.database.getBusinessRepository().endEmployment(episode.id(),1,"Explicit original worker departure");
        f.f.database.close();f.f.open();var before=f.snapshot();var replay=engine().startLivestock(request,LivestockHerd.Species.COW,grain,8,animals,StartupGrantPolicy.disabled());assertEquals(4,replay.herd().state().heads());assertEquals(3000,replay.startup().store().hundredths());assertEquals(before,f.snapshot());
        assertThrows(SQLException.class,()->engine().startLivestock(request,LivestockHerd.Species.CHICKEN,grain,8,animals,StartupGrantPolicy.prototype()));assertEquals(before,f.snapshot());
    }
    @Test void finalHerdWriteFailureRollsBackGrantFirmSiteStoreWorkerAndAllAssets()throws Exception {
        f.f.sql("CREATE TRIGGER refuse_herd BEFORE INSERT ON livestock_periods BEGIN SELECT RAISE(ABORT,'fixture last herd checkpoint'); END");var before=f.snapshot();
        assertThrows(SQLException.class,()->start(LivestockHerd.Species.COW));assertEquals(before,f.snapshot());f.f.sql("DROP TRIGGER refuse_herd");start(LivestockHerd.Species.COW);
    }
    @Test void unknownOrPlayerAnimalsCannotBeSeizedAndWrongWorldRefusesAtomically()throws Exception {
        var unknown=new HashMap<>(animals);unknown.put(UUID.randomUUID(),0);var before=f.snapshot();assertThrows(SQLException.class,()->engine().startLivestock(request,LivestockHerd.Species.COW,grain,8,unknown,StartupGrantPolicy.prototype()));assertEquals(before,f.snapshot());
        var playerAnimal=UUID.randomUUID();new AnimalOwnershipRepository(f.f.database.getConnection()).record(new AnimalOwnershipRepository.Claim(playerAnimal,f.f.world,LivestockAdoption.Ownership.PLAYER,UUID.randomUUID(),null,1000,"Explicit player fixture"));
        var mixed=new HashMap<>(animals);mixed.put(playerAnimal,0);before=f.snapshot();assertThrows(SQLException.class,()->engine().startLivestock(request,LivestockHerd.Species.COW,grain,8,mixed,StartupGrantPolicy.prototype()));assertEquals(before,f.snapshot());
        var otherWorld=UUID.randomUUID();f.f.database.getVillageIdentityRepository().observeWorld(otherWorld,"Explicit other fixture world");
        var original=request.founding();var p=original.site();var wrongSite=new BusinessSite(p.id(),p.settlementId(),otherWorld,p.x(),p.y(),p.z(),p.observedMillis(),p.evidence());
        var changed=new BusinessStartupGrants.WorkshopRequest(new BusinessFounding(original.business(),wrongSite,original.founderRequirements(),original.workerRequirements(),original.startupHundredths(),original.reason()),request.capacity(),request.space(),request.capital(),request.worker());
        before=f.snapshot();assertThrows(SQLException.class,()->engine().startLivestock(changed,LivestockHerd.Species.COW,grain,8,animals,StartupGrantPolicy.prototype()));assertEquals(before,f.snapshot());
    }
    @Test void foodShortageAndOptOutRefuseWithoutChangingAnyRows()throws Exception {
        var p=StartupGrantPolicy.prototype();var old=new HashSet<>(p.industries());old.remove("livestock");var disabled=new StartupGrantPolicy(true,p.maximumGrant(),p.perPeriodLimit(),p.lifetimeLimit(),p.minimumTreasury(),p.foodReservePeriods(),p.hundredthsPerFoodUnit(),old);
        var before=f.snapshot();assertThrows(BusinessStartupGrants.Declined.class,()->engine().startLivestock(request,LivestockHerd.Species.COW,grain,8,animals,disabled));assertEquals(before,f.snapshot());
        long cash=new TreasuryRepository(f.f.database.getConnection()).load(f.f.town).orElseThrow().hundredths();
        var protection=new StartupGrantPolicy(true,p.maximumGrant(),p.perPeriodLimit(),p.lifetimeLimit(),cash-4500+1,p.foodReservePeriods(),p.hundredthsPerFoodUnit(),p.industries());
        before=f.snapshot();assertThrows(BusinessStartupGrants.Declined.class,()->engine().startLivestock(request,LivestockHerd.Species.COW,grain,8,animals,protection));assertEquals(before,f.snapshot());
        f.f.sql("UPDATE economy_stocks SET units=0 WHERE commodity='food'");f.economy.pulse(4000,false);before=f.snapshot();assertThrows(BusinessStartupGrants.Declined.class,()->start(LivestockHerd.Species.COW));assertEquals(before,f.snapshot());
    }
    @Test void qualificationAndUnsupportedCatalogueRollbackTheEntireOpening()throws Exception {
        f.f.sql("UPDATE resident_skills SET experience=0 WHERE resident_id='"+f.candidate+"' AND skill='farming'");var before=f.snapshot();assertThrows(SQLException.class,()->start(LivestockHerd.Species.COW));assertEquals(before,f.snapshot());
        f.f.sql("UPDATE resident_skills SET experience=10 WHERE resident_id='"+f.candidate+"' AND skill='farming'");before=f.snapshot();var wrong=new BusinessStartupGrants.WorkshopRequest(request.founding(),128,Map.of(grain,1L),4000,f.candidate);
        assertThrows(SQLException.class,()->engine().startLivestock(wrong,LivestockHerd.Species.COW,grain,8,animals,StartupGrantPolicy.prototype()));assertEquals(before,f.snapshot());
    }
    @Test void separateLegacyGrantCannotBeRetrofittedIntoAnAtomicHerdOpening()throws Exception {
        engine().startWorkshop(request,StartupGrantPolicy.prototype());var before=f.snapshot();assertThrows(BusinessStartupGrants.Declined.class,()->start(LivestockHerd.Species.COW));assertEquals(before,f.snapshot());
        new LivestockRepository(f.f.database.getConnection()).open(request.founding().business().id(),LivestockHerd.Species.COW,grain,8,animals);
        var ended=f.f.database.getBusinessRepository().currentWorkers(request.founding().business().id()).getFirst();f.f.database.getBusinessRepository().endEmployment(ended.id(),1,"Explicit legacy worker departure");
        before=f.snapshot();assertThrows(BusinessStartupGrants.Declined.class,()->start(LivestockHerd.Species.COW));assertEquals(before,f.snapshot(),"Separate legacy setup must not acquire a new job through startup replay");
    }
}
