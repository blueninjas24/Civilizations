package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class LivestockProductSalesTest {
    @TempDir Path directory;
    LivestockRepositoryTest f;
    @BeforeEach void setup() throws Exception { f = new LivestockRepositoryTest(); f.directory = directory; f.setup(); }
    @AfterEach void close() { f.close(); }
    LivestockProductSales sales() { return new LivestockProductSales(f.db.getConnection(), BusinessOperationPolicy.prototype()); }
    IndustryRepository.Store store() throws Exception { return f.db.getIndustryRepository().store(f.business.id()).orElseThrow(); }
    void produce(Product product) throws Exception {
        f.repo().open(f.business.id(), product == Product.MILK ? LivestockHerd.Species.COW : LivestockHerd.Species.CHICKEN, f.grain, 8, f.roster);
        f.feed(100); f.advance(new UUID(9, 1), 1);
    }
    Terms terms(Product product) { return new Terms(UUID.randomUUID(), new UUID(8, 1), f.town, f.world, f.business.id(), product, 1, 3, 800); }
    LivestockProductSales.Receipt reserve(Terms terms) throws Exception { return sales().reserve(terms, Set.of(f.world)); }
    @ParameterizedTest @EnumSource(Product.class)
    void realProductsReserveWithoutRevenueAndCompleteOnlyOnceAcrossRestart(Product product) throws Exception {
        produce(product); var terms = terms(product); long units = f.units(product.commodity()), free = store().freeSpace();
        var reserved = reserve(terms);
        assertEquals(LivestockProductSales.Status.RESERVED, reserved.status()); assertEquals(1, reserved.acceptedPeriod());
        assertEquals(units - 1, f.units(product.commodity())); assertEquals(0, store().hundredths());
        assertEquals(free, store().freeSpace(), "Cancellation still needs the removed product's room");
        assertEquals(reserved, reserve(terms));
        f.db.close(); f.reopen();
        var completed = sales().complete(terms); assertEquals(LivestockProductSales.Status.COMPLETED, completed.status());
        assertEquals(2400, store().hundredths()); assertEquals(free + 1, store().freeSpace());
        assertEquals(completed, sales().complete(terms)); assertEquals(completed, sales().reserve(terms, Set.of()));
        assertThrows(SQLException.class, () -> sales().cancel(terms)); assertEquals(2400, store().hundredths());
        assertEquals(units - 1, f.units(product.commodity()));
    }
    @Test void cancellationRestoresGoodsOnceEvenWhenOtherWorkFillsAllUnreservedRoom() throws Exception {
        produce(Product.MILK); var terms = terms(Product.MILK); long milk = f.units(f.milk);
        reserve(terms); long free = store().freeSpace(); f.feed(f.units(f.grain) + free);
        assertEquals(0, store().freeSpace()); f.db.close(); f.reopen();
        var cancelled = sales().cancel(terms); assertEquals(milk, f.units(f.milk)); assertEquals(0, store().freeSpace());
        assertEquals(0, store().hundredths()); assertEquals(cancelled, sales().cancel(terms));
        assertEquals(cancelled, reserve(terms)); assertThrows(SQLException.class, () -> sales().complete(terms));
        assertEquals(milk, f.units(f.milk));
    }
    @Test void independentBuyersCannotReserveOrSellTheSameProduct() throws Exception {
        produce(Product.MILK); var first = terms(Product.MILK); var second = terms(Product.MILK);
        reserve(first); reserve(second); assertEquals(0, f.units(f.milk));
        assertThrows(LivestockProductSales.Declined.class, () -> reserve(terms(Product.MILK)));
        sales().cancel(first); var third = terms(Product.MILK); reserve(third);
        sales().complete(second); sales().complete(third); assertEquals(4800, store().hundredths()); assertEquals(0, f.units(f.milk));
    }
    @Test void eggQuantityIsBoundToTheSavedOperation() throws Exception {
        produce(Product.EGGS); var t = terms(Product.EGGS); reserve(t);
        var changed = new Terms(t.operation(), t.player(), t.settlement(), t.world(), t.seller(), t.product(), 2, t.emeralds(), t.hundredthsPerEmerald());
        assertThrows(SQLException.class, () -> reserve(changed)); assertThrows(SQLException.class, () -> sales().complete(changed));
        assertThrows(SQLException.class, () -> sales().cancel(changed)); assertEquals(1, f.units(f.eggs));
        sales().cancel(t); assertEquals(2, f.units(f.eggs));
    }
    @Test void herdProductionCannotUseSpaceHeldForCancelledSales() throws Exception {
        produce(Product.MILK); var t = terms(Product.MILK); reserve(t);
        f.feed(f.units(f.grain) + store().freeSpace()); assertEquals(0, store().freeSpace());
        long milk = f.units(f.milk); var next = f.advance(new UUID(9, 2), 2);
        assertEquals(0, next.renewableUnits()); assertEquals(milk, f.units(f.milk));
        sales().cancel(t); assertEquals(milk + 1, f.units(f.milk)); assertTrue(store().freeSpace() >= 0);
        assertEquals(0, store().hundredths());
    }
    @Test void operationIdentityBindsEveryAcceptedTermAndUnknownFinishesDoNothing() throws Exception {
        produce(Product.MILK); var t = terms(Product.MILK); reserve(t);
        var changed = List.of(
            new Terms(t.operation(), UUID.randomUUID(), t.settlement(), t.world(), t.seller(), t.product(), 1, 3, 800),
            new Terms(t.operation(), t.player(), UUID.randomUUID(), t.world(), t.seller(), t.product(), 1, 3, 800),
            new Terms(t.operation(), t.player(), t.settlement(), UUID.randomUUID(), t.seller(), t.product(), 1, 3, 800),
            new Terms(t.operation(), t.player(), t.settlement(), t.world(), UUID.randomUUID(), t.product(), 1, 3, 800),
            new Terms(t.operation(), t.player(), t.settlement(), t.world(), t.seller(), Product.EGGS, 1, 3, 800),
            new Terms(t.operation(), t.player(), t.settlement(), t.world(), t.seller(), t.product(), 1, 4, 800),
            new Terms(t.operation(), t.player(), t.settlement(), t.world(), t.seller(), t.product(), 1, 3, 801));
        for (var other : changed) {
            assertThrows(SQLException.class, () -> reserve(other)); assertThrows(SQLException.class, () -> sales().complete(other));
            assertThrows(SQLException.class, () -> sales().cancel(other));
        }
        assertThrows(SQLException.class, () -> sales().complete(terms(Product.MILK)));
        assertThrows(SQLException.class, () -> sales().cancel(terms(Product.MILK)));
        assertEquals(1, f.units(f.milk)); assertEquals(0, store().hundredths()); assertEquals(1, f.count("livestock_product_sales"));
    }
    @Test void absentWrongWorldWrongTownMissingHerdAndUnproducedGoodsAreRefused() throws Exception {
        var t = terms(Product.MILK); assertThrows(SQLException.class, () -> reserve(t));
        f.openHerd(); assertThrows(LivestockProductSales.Declined.class, () -> reserve(t));
        f.feed(100); f.advance(new UUID(9, 1), 1);
        assertThrows(LivestockProductSales.Declined.class, () -> sales().reserve(t, Set.of()));
        var world = UUID.randomUUID(); assertThrows(LivestockProductSales.Declined.class, () -> sales().reserve(new Terms(t.operation(), t.player(), t.settlement(), world, t.seller(), t.product(), 1, 3, 800), Set.of(world)));
        assertThrows(LivestockProductSales.Declined.class, () -> reserve(new Terms(t.operation(), t.player(), UUID.randomUUID(), t.world(), t.seller(), t.product(), 1, 3, 800)));
        assertThrows(LivestockProductSales.Declined.class, () -> reserve(terms(Product.EGGS)));
        assertEquals(2, f.units(f.milk)); assertEquals(0, f.count("livestock_product_sales"));
    }
    @Test void unstaffedSuppliersCannotStartSalesButAcceptedOrdersCanRecover() throws Exception {
        produce(Product.MILK); var t = terms(Product.MILK); reserve(t);
        f.db.getBusinessRepository().endEmployment(new UUID(0, 7), 1, "Fixture worker departure");
        assertThrows(LivestockProductSales.Declined.class, () -> reserve(terms(Product.MILK)));
        sales().complete(t); assertEquals(2400, store().hundredths());
    }
    @Test void insertAndFinalizationFailuresRollbackAllGoodsMoneyAndStatusThenRetry() throws Exception {
        produce(Product.MILK); var t = terms(Product.MILK);
        f.sql("CREATE TRIGGER refuse_sale BEFORE INSERT ON livestock_product_sales BEGIN SELECT RAISE(ABORT,'fixture reserve'); END");
        assertThrows(SQLException.class, () -> reserve(t)); assertEquals(2, f.units(f.milk)); assertEquals(0, f.count("livestock_product_sales"));
        assertTrue(f.db.getConnection().getAutoCommit()); f.sql("DROP TRIGGER refuse_sale"); reserve(t);
        f.sql("CREATE TRIGGER refuse_finish BEFORE UPDATE ON livestock_product_sales BEGIN SELECT RAISE(ABORT,'fixture finish'); END");
        assertThrows(SQLException.class, () -> sales().complete(t)); assertEquals(0, store().hundredths());
        assertThrows(SQLException.class, () -> sales().cancel(t)); assertEquals(1, f.units(f.milk));
        assertEquals(LivestockProductSales.Status.RESERVED, sales().find(t.operation()).orElseThrow().status());
        f.sql("DROP TRIGGER refuse_finish"); sales().complete(t); assertEquals(2400, store().hundredths());
    }
    @Test void capitalOverflowKeepsTheReservationUnchangedForReview() throws Exception {
        produce(Product.MILK); var t = terms(Product.MILK); reserve(t);
        f.sql("UPDATE industry_stores SET hundredths=" + Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> sales().complete(t)); assertEquals(Long.MAX_VALUE, store().hundredths());
        assertEquals(LivestockProductSales.Status.RESERVED, sales().find(t.operation()).orElseThrow().status());
        sales().cancel(t); assertEquals(2, f.units(f.milk));
    }
    @Test void nestedOwnerTransactionsAreNotCommittedBySaleMethods() throws Exception {
        produce(Product.MILK); var t = terms(Product.MILK); f.db.getConnection().setAutoCommit(false);
        try { assertThrows(SQLException.class, () -> reserve(t)); assertThrows(SQLException.class, () -> sales().cancel(t)); }
        finally { f.db.getConnection().rollback(); f.db.getConnection().setAutoCommit(true); }
        assertEquals(2, f.units(f.milk)); assertEquals(0, f.count("livestock_product_sales"));
    }
    @Test void schema83UpgradeRetainsEveryPredecessorRowAndRollsBackFailures() throws Exception {
        produce(Product.MILK); var snapshot = new LivestockLossesTest(); snapshot.f = f;
        SchemaFixtures.rewind(f.db.getConnection(), 83); var before = snapshot.snapshot();
        f.sql("CREATE VIEW livestock_product_sales AS SELECT 1");
        assertThrows(SQLException.class, () -> new SchemaMigrator().migrate(f.db.getConnection()));
        try (var s = f.db.getConnection().createStatement(); var r = s.executeQuery("PRAGMA user_version")) { assertEquals(83, r.getInt(1)); }
        f.sql("DROP VIEW livestock_product_sales"); assertEquals(before, snapshot.snapshot());
        new SchemaMigrator().migrate(f.db.getConnection()); var after = snapshot.snapshot();
        assertEquals(List.of(),after.remove("livestock_presence"));assertEquals(List.of(),after.remove("livestock_birth_losses"));assertEquals(List.of(),after.remove("livestock_birth_projections"));assertEquals(List.of(),after.remove("livestock_birth_entities"));assertEquals(List.of(), after.remove("livestock_births")); assertEquals(List.of(), after.remove("livestock_product_sales")); assertEquals(before, after);
        new SchemaMigrator().migrate(f.db.getConnection());
        reserve(terms(Product.MILK));
        try (var s = f.db.getConnection().createStatement(); var r = s.executeQuery("PRAGMA foreign_key_check")) { assertFalse(r.next()); }
    }
}
