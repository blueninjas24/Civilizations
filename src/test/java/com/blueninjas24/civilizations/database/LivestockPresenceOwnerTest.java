package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.database.LivestockPresence.Sight;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockPresenceOwnerTest {
    @TempDir Path directory;LivestockRepositoryTest f;PopulationSession session;
    @BeforeEach void setup()throws Exception {
        f=new LivestockRepositoryTest();f.directory=directory;f.setup();var capture=new LivestockCapture(f.db.getConnection());assertTrue(capture.begin(1000).trusted());f.openHerd();f.feed(100);capture.finish(2000,true);
    }
    @AfterEach void close(){if(session!=null)session.close();f.close();}
    void start(boolean present)throws Exception {
        f.db.close();session=new PopulationSession(directory.toFile(),Logger.getLogger("Presence owner fixture"),PopulationSession.Settings.prototype(),()->3000);
        session.configureLivestockWorkingArea(new LivestockWorkingArea(8));session.configureBusinessOperations(BusinessOperationPolicy.prototype());
        if(present)session.start(false,Set.of(f.world)).join();else session.start(false).join();
    }
    @Test void configuredWorkingRadiusAndPositiveOwnerHandoffDriveActualPaidProduction()throws Exception {
        start(true);assertEquals(3,session.livestockMembers(null,16).join().size());
        var sight=new Sight(new UUID(0,10),LivestockHerd.Species.COW,f.world,new Position(9,64,0),3000);
        assertEquals(1,session.recordLivestockPresence(List.of(sight)).join());
        var plan=session.advanceLivestock(new UUID(90,1),f.business.id(),1,10,10,LivestockPolicy.prototype()).join();
        assertEquals(1,plan.renewableUnits());assertEquals(0,plan.births());assertEquals(3,plan.after().heads());assertEquals(1,plan.feedUsed());
        assertEquals(plan,session.advanceLivestock(new UUID(90,1),f.business.id(),1,10,10,LivestockPolicy.prototype()).join());
        assertThrows(IllegalStateException.class,()->session.configureLivestockWorkingArea(new LivestockWorkingArea(32)));
        session.close();f.reopen();assertEquals(99,f.units(f.grain));assertEquals(1,f.units(f.milk));assertEquals(3,f.count("animal_ownership"));
    }
    @Test void unknownWorldAndUntrustedCaptureCannotAccrueLiveProductionOrClearTheGap()throws Exception {
        start(false);assertEquals(0,session.advanceLivestock(new UUID(91,1),f.business.id(),1,10,10,LivestockPolicy.prototype()).join().after().period());
        session.close();session=null;f.reopen();new LivestockCapture(f.db.getConnection()).markGap(4000);start(true);assertFalse(session.livestockCaptureReady());
        session.recordLivestockPresence(List.of(new Sight(new UUID(0,10),LivestockHerd.Species.COW,f.world,new Position(0,64,0),5000))).join();
        assertFalse(session.livestockCaptureReady());assertThrows(CompletionException.class,()->session.advanceLivestock(new UUID(91,2),f.business.id(),1,10,10,LivestockPolicy.prototype()).join());
        session.close();f.reopen();assertEquals(100,f.units(f.grain));assertEquals(0,f.units(f.milk));assertEquals(1,f.count("livestock_periods"));
    }
}
