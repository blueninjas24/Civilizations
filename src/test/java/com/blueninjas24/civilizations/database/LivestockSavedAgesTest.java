package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockSavedAgesTest {
    @TempDir Path directory;LivestockRepositoryTest f;
    @BeforeEach void setup()throws Exception{f=new LivestockRepositoryTest();f.directory=directory;f.setup();f.openHerd();f.feed(100);}
    @AfterEach void close(){f.close();}
    LivestockPresence repo(){return new LivestockPresence(f.db.getConnection());}
    @Test void initialRosterCarriesActualSavedHerdPeriodAndMaturityAcrossWorkAndRestart()throws Exception {
        var initial=repo().page(null,16);assertEquals(3,initial.size());assertTrue(initial.stream().allMatch(a->a.herdPeriod()==0));assertEquals(2,initial.stream().filter(a->a.maturePeriod()==0).count());
        f.advance(new UUID(120,1),1);var young=repo().page(null,16).stream().filter(a->a.maturePeriod()==2).findFirst().orElseThrow();assertEquals(1,young.herdPeriod());
        f.db.close();f.reopen();assertEquals(young,repo().page(null,16).stream().filter(a->a.animal().equals(young.animal())).findFirst().orElseThrow());
        f.advance(new UUID(120,2),2);var before=f.db.getIndustryRepository().store(f.business.id()).orElseThrow();var matured=repo().page(null,16);assertTrue(matured.stream().allMatch(a->a.herdPeriod()==2&&a.maturePeriod()<=2));assertEquals(before,f.db.getIndustryRepository().store(f.business.id()).orElseThrow());
    }
    @Test void absenceOrDisabledWorkCannotAdvanceAnAnimalAgeView()throws Exception {
        var before=repo().page(null,16);f.repo().advance(UUID.randomUUID(),f.business.id(),1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),false);
        f.repo().advance(UUID.randomUUID(),f.business.id(),1,10,10,LivestockPolicy.disabled(),BusinessOperationPolicy.prototype(),true);assertEquals(before,repo().page(null,16));assertEquals(100,f.units(f.grain));
    }
    @Test void missingCheckpointIsNotGuessedAsAZeroAgeOrRepairedByReading()throws Exception {
        f.sql("DELETE FROM livestock_young");f.sql("DELETE FROM livestock_periods");assertThrows(SQLException.class,()->repo().page(null,16));assertEquals(0,f.count("livestock_periods"));assertEquals(3,f.count("animal_ownership"));assertEquals(100,f.units(f.grain));
    }
    @Test void recordedPlayerAnimalsNeverJoinAgePagesOrChangeTheSavedHerd()throws Exception {
        UUID playerAnimal=new UUID(121,1);new AnimalOwnershipRepository(f.db.getConnection()).record(new AnimalOwnershipRepository.Claim(playerAnimal,f.world,LivestockAdoption.Ownership.PLAYER,f.owner,null,1000,"Explicit player fixture"));
        var members=repo().page(null,16);assertEquals(3,members.size());assertTrue(members.stream().noneMatch(a->a.animal().equals(playerAnimal)));assertEquals(3,f.repo().load(f.business.id()).orElseThrow().state().heads());assertEquals(4,f.count("animal_ownership"));
    }
}
