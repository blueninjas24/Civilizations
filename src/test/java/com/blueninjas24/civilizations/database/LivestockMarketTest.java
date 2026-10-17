package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.industry.LivestockMarketPolicy.Position;
import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LivestockMarketTest {
    @TempDir Path directory;LivestockRepositoryTest f;
    final UUID player=new UUID(8,1);
    @BeforeEach void setup()throws Exception {f=new LivestockRepositoryTest();f.directory=directory;f.setup();f.openHerd();f.feed(100);f.advance(new UUID(9,1),1);}
    @AfterEach void close(){f.close();}
    LivestockMarket market(LivestockMarketPolicy policy){return new LivestockMarket(f.db.getConnection(),policy,BusinessOperationPolicy.prototype());}
    Position here(){return new Position(f.world,0,64,0);}
    @Test void readOnlyQuoteUsesRealEligibleNearbyStockAndEditableExactTerms()throws Exception {
        var snapshot=new LivestockLossesTest();snapshot.f=f;var before=snapshot.snapshot();
        var offers=market(LivestockMarketPolicy.prototype()).offers(player,here(),1000,Set.of(f.world));assertEquals(1,offers.size());var o=offers.getFirst();
        assertEquals(player,o.terms().player());assertEquals(f.business.id(),o.terms().seller());assertEquals(Product.MILK,o.terms().product());assertEquals(1,o.terms().emptyBuckets());assertEquals(2,o.terms().emeralds());assertEquals(2,o.availableUnits());assertEquals(31000,o.expiresMillis());assertEquals(before,snapshot.snapshot());
        var p=new LivestockMarketPolicy(true,900,5,12,3,64,24,1,10);var changed=market(p).offers(player,here(),1000,Set.of(f.world)).getFirst();assertEquals(5,changed.terms().emeralds());assertEquals(4500,changed.terms().cost());assertEquals(11000,changed.expiresMillis());
    }
    @Test void absentDistantWrongWorldAndDisabledMarketsNeverOfferProducts()throws Exception {
        var m=market(LivestockMarketPolicy.prototype());assertTrue(m.offers(player,here(),1000,Set.of()).isEmpty());
        assertTrue(m.offers(player,new Position(f.world,65,64,0),1000,Set.of(f.world)).isEmpty());assertTrue(m.offers(player,new Position(f.world,64,64,64),1000,Set.of(f.world)).isEmpty());
        assertTrue(m.offers(player,new Position(f.world,0,89,0),1000,Set.of(f.world)).isEmpty());var other=UUID.randomUUID();assertTrue(m.offers(player,new Position(other,0,64,0),1000,Set.of(other)).isEmpty());
        assertTrue(market(LivestockMarketPolicy.disabled()).offers(player,here(),1000,Set.of(f.world)).isEmpty());
    }
    @Test void finalReservationRechecksPricePositionStaffAndStockThenRecoveryHonorsOldTerms()throws Exception {
        var p=LivestockMarketPolicy.prototype();var m=market(p);var t=m.offers(player,here(),1000,Set.of(f.world)).getFirst().terms();
        assertThrows(LivestockProductSales.Declined.class,()->m.reserve(t,new Position(f.world,65,64,0),Set.of(f.world)));
        assertThrows(LivestockProductSales.Declined.class,()->market(LivestockMarketPolicy.disabled()).reserve(t,here(),Set.of(f.world)));
        var wrong=new Terms(t.operation(),t.player(),t.settlement(),t.world(),t.seller(),t.product(),1,3,800);assertThrows(LivestockProductSales.Declined.class,()->m.reserve(wrong,here(),Set.of(f.world)));
        var held=m.reserve(t,here(),Set.of(f.world));f.db.getBusinessRepository().endEmployment(new UUID(0,7),1,"Fixture departure");
        assertEquals(held,market(LivestockMarketPolicy.disabled()).reserve(t,new Position(f.world,1000,64,0),Set.of()));assertEquals(1,f.units(f.milk));
        assertTrue(m.offers(player,here(),1000,Set.of(f.world)).isEmpty());assertTrue(m.cancel(t).isPresent());assertEquals(2,f.units(f.milk));
    }
    @Test void failedReservationCanCancelWithoutCreatingGoodsMoneyOrAReceipt()throws Exception {
        var t=LivestockMarketPolicy.prototype().terms(UUID.randomUUID(),player,f.town,f.world,f.business.id(),Product.MILK);
        assertTrue(market(LivestockMarketPolicy.prototype()).cancel(t).isEmpty());assertEquals(2,f.units(f.milk));assertEquals(0,f.count("livestock_product_sales"));
        assertThrows(SQLException.class,()->new LivestockProductSales(f.db.getConnection(),BusinessOperationPolicy.prototype()).complete(t));
    }
}
