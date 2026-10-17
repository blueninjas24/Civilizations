package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.industry.LivestockMarketPolicy.Position;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockMarketOwnerTest {
    @TempDir Path directory; LivestockRepositoryTest f;PopulationSession session;
    @BeforeEach void setup()throws Exception {
        f=new LivestockRepositoryTest();f.directory=directory;f.setup();f.openHerd();f.feed(100);f.advance(new UUID(9,1),1);f.db.close();
    }
    PopulationSession start(boolean online)throws Exception {
        session=new PopulationSession(directory.toFile(),Logger.getLogger("Livestock market owner"),PopulationSession.Settings.prototype(),()->1000);
        session.configureLivestockMarket(LivestockMarketPolicy.prototype());session.configureBusinessOperations(BusinessOperationPolicy.prototype());
        if(online)session.start(false,Set.of(f.world)).join();else session.start(false).join();return session;
    }
    @AfterEach void close(){if(session!=null)session.close();f.close();}
    @Test void ownerOrdersReserveCompleteAndRecoveryWithoutPaperOrInventoryAccess()throws Exception {
        start(true);var position=new Position(f.world,0,64,0);var t=session.livestockOffers(new UUID(8,1),position).join().getFirst().terms();
        var first=session.reserveLivestockSale(t,position).join();assertEquals(LivestockProductSales.Status.RESERVED,first.status());
        assertEquals(LivestockProductSales.Status.COMPLETED,session.completeLivestockSale(t).join().status());session.close();f.reopen();
        assertEquals(1,f.units(f.milk));assertEquals(1600,f.db.getIndustryRepository().store(f.business.id()).orElseThrow().hundredths());
        f.db.close();session=null;start(true);assertEquals(LivestockProductSales.Status.COMPLETED,session.reserveLivestockSale(t,position).join().status());
        assertEquals(LivestockProductSales.Status.COMPLETED,session.completeLivestockSale(t).join().status());
    }
    @Test void ownerCancellationReturnsReservedGoodsAndUnknownOrderCreatesNothing()throws Exception {
        start(true);var position=new Position(f.world,0,64,0);var t=session.livestockOffers(new UUID(8,1),position).join().getFirst().terms();
        assertTrue(session.cancelLivestockSale(t).join().isEmpty());session.reserveLivestockSale(t,position).join();assertTrue(session.cancelLivestockSale(t).join().isPresent());
        session.close();f.reopen();assertEquals(2,f.units(f.milk));assertEquals(0,f.db.getIndustryRepository().store(f.business.id()).orElseThrow().hundredths());
    }
    @Test void headlessUnknownWorldsCannotOfferOrReserveLivePlayerTrade()throws Exception {
        start(false);var position=new Position(f.world,0,64,0);assertTrue(session.livestockOffers(new UUID(8,1),position).join().isEmpty());
        var t=LivestockMarketPolicy.prototype().terms(UUID.randomUUID(),new UUID(8,1),f.town,f.world,f.business.id(),LivestockProductSale.Product.MILK);
        assertThrows(java.util.concurrent.CompletionException.class,()->session.reserveLivestockSale(t,position).join());
        assertThrows(IllegalStateException.class,()->session.configureLivestockMarket(LivestockMarketPolicy.disabled()));
    }
}
