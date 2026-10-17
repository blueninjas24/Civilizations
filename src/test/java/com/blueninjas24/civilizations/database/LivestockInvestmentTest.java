package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockInvestmentTest {
    @TempDir Path directory;BusinessStartupGrantsTest f;
    LivestockInvestment investment(){return new LivestockInvestment(f.db.getConnection(),LivestockAdoptionPolicy.prototype(),LivestockPolicy.prototype(),StartupGrantPolicy.prototype(),216);}
    LivestockObservation.Animal animal(int n,int age,String origin){return new LivestockObservation.Animal(new UUID(0,n),LivestockHerd.Species.COW,new Position(n-10,64,0),age,false,false,false,origin);}
    LivestockOpportunity proof(){return new LivestockOpportunity(f.town,f.world,new Position(0,64,0),List.of(animal(10,0,"NATURAL"),animal(11,0,"CHUNK_GEN"),animal(12,-12000,"NATURAL")),40000,30000);}
    @BeforeEach void setup()throws Exception{f=new BusinessStartupGrantsTest();f.directory=directory;f.setup();f.db.getPopulationRepository().recordSkillExperience(f.person,"farming",1);}
    @AfterEach void close(){f.db.close();}
    LivestockInvestment.Result accept()throws Exception{return investment().accept(proof(),0,40001,Set.of(f.world),()->true);}
    long count(String table)throws Exception{try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}}
    @Test void realGrantClaimsJobStoreAndInitialAgesCommitOnceAndSurviveRestart()throws Exception{
        var result=accept();assertTrue(result.opened(),result.explanation());assertEquals(7500,f.cash());
        var store=f.db.getIndustryRepository().store(result.business()).orElseThrow();assertEquals(2000,store.hundredths());assertTrue(store.units().values().stream().allMatch(n->n==0));
        var herd=new LivestockRepository(f.db.getConnection()).load(result.business()).orElseThrow();assertEquals(2,herd.state().adults());assertEquals(1,herd.state().young().size());assertEquals(6,herd.state().young().getFirst().maturePeriod());
        assertEquals(3,count("animal_ownership"));assertEquals(1,count("business_employment"));assertEquals(1,count("startup_grants"));assertFalse(accept().opened());assertEquals(7500,f.cash());
        f.db.close();f.open();assertEquals(herd,new LivestockRepository(f.db.getConnection()).load(result.business()).orElseThrow());assertFalse(accept().opened());assertEquals(3,count("animal_ownership"));
    }
    @Test void refusedMoneyOrFinalWriteNeverLeavesOrphanOwnership()throws Exception{
        f.sql("UPDATE needs_periods SET fulfilled=0");assertFalse(accept().opened());assertEquals(0,count("animal_ownership"));assertEquals(10000,f.cash());
        f.sql("UPDATE needs_periods SET fulfilled=demand");f.sql("CREATE TRIGGER reject_live_herd BEFORE INSERT ON livestock_initial_animals BEGIN SELECT RAISE(ABORT,'fixture final opening failure'); END");
        assertThrows(SQLException.class,this::accept);assertEquals(0,count("animal_ownership"));assertEquals(0,count("businesses"));assertEquals(10000,f.cash());assertTrue(f.db.getConnection().getAutoCommit());
        f.sql("DROP TRIGGER reject_live_herd");assertTrue(accept().opened());
    }
    @Test void unknownHumanOriginNamedOwnedStaleOrAbsentAnimalsCannotBeTaken()throws Exception{
        var base=proof();
        for(String origin:List.of("UNKNOWN","DEFAULT","BREEDING","COMMAND","SPAWNER_EGG")){
            var animals=List.of(animal(10,0,origin),animal(11,0,"NATURAL"));
            assertFalse(investment().accept(new LivestockOpportunity(f.town,f.world,base.site(),animals,40000,30000),0,40001,Set.of(f.world),()->true).opened());
        }
        assertFalse(investment().accept(base,0,60000,Set.of(f.world),()->true).opened());assertFalse(investment().accept(base,0,40001,Set.of(),()->true).opened());
        new AnimalOwnershipRepository(f.db.getConnection()).record(new AnimalOwnershipRepository.Claim(new UUID(0,10),f.world,LivestockAdoption.Ownership.PLAYER,f.person,null,1,"Explicit player fixture"));
        assertFalse(accept().opened());assertEquals(1,count("animal_ownership"));assertEquals(0,count("businesses"));
    }
    @Test void changingProofRollsBackLateAndQualificationNeverAppearsForFree()throws Exception{
        f.db.getPopulationRepository().recordSkillExperience(f.person,"farming",0);assertFalse(accept().opened());assertEquals(0,count("animal_ownership"));
        f.db.getPopulationRepository().recordSkillExperience(f.person,"farming",1);
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        assertFalse(investment().accept(proof(),0,40001,Set.of(f.world),()->calls.incrementAndGet()==1).opened());
        assertEquals(0,count("animal_ownership"));assertEquals(10000,f.cash());assertTrue(accept().opened());
    }
    @Test void savedProtectedAreaRefusesEvenAnimalsThatWereNotInItsInitialRoster()throws Exception{
        var scan=new PenScan(p->Math.abs(p.x())==3||Math.abs(p.z())==3?PenScan.Cell.BARRIER:PenScan.Cell.INTERIOR,new PenScan.Column(0,0),new PenScan.Limits(4,32));
        while(!scan.complete())scan.advance(16);
        var pen=new PenProtection(UUID.randomUUID(),f.world,f.person,64,4,64,0,new PenScan.Column(0,0),scan.evidence(),Set.of(),1);
        new PenProtectionRepository(f.db.getConnection()).save(pen,true);
        assertFalse(accept().opened());assertEquals(0,count("animal_ownership"));assertEquals(10000,f.cash());
    }
}
