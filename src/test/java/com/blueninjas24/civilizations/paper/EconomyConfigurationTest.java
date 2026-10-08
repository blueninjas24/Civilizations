package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EconomyConfigurationTest {
    private YamlConfiguration defaults() throws Exception {
        var config = new YamlConfiguration();
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/economy.yml"));
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) { config.load(reader); }
        return config;
    }
    @Test void shippedCommentedFileDefinesApprovedPrototypeAndPlainPercentages() throws Exception {
        var settings = EconomyConfiguration.read(defaults());
        assertEquals(60, settings.secondsPerPeriod()); assertEquals(120, settings.maximumOfflinePeriods()); assertEquals(2, settings.periodsPerBatch());
        assertEquals(new Policy(5000, 12500, 20000, 3), settings.shortages());
        assertEquals(5, settings.foodDemand(2, 1)); assertEquals(24, settings.startingReserves(2).get(Commodity.FOOD));
        assertEquals(new BigDecimal("5.00"), settings.price(Commodity.FOOD, Condition.SEVERE_SHORTAGE));
    }
    @ParameterizedTest @ValueSource(strings = {"pace.seconds-per-period", "food.units-per-adult", "shortages.severe-after-periods", "prices.condition-percent.balanced"})
    void fractionalAndQuotedWholeNumbersFailRatherThanTruncateOrFallBack(String path) throws Exception {
        var config = defaults(); config.set(path, 1.5);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        config.set(path, "2"); assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
    }
    @Test void missingKeysTyposUnknownVersionsAndPartialSectionsFailClearly() throws Exception {
        var config = defaults(); config.set("food.output-percent", null); var missing = config;
        assertTrue(assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(missing)).getMessage().contains("food"));
        config = defaults(); config.set("food.output-percnt", 50); var typo = config;
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(typo));
        config = defaults(); config.set("format-version", 2); var version = config;
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(version));
    }
    @ParameterizedTest @ValueSource(strings = {"pace.seconds-per-period", "pace.maximum-offline-periods", "food.units-per-adult", "food.output-percent", "starting-units-per-resident.food", "prices.condition-percent.balanced"})
    void negativeUnsafeSettingsFail(String path) throws Exception {
        var config = defaults(); config.set(path, -1);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
    }
    @Test void decimalPricesAreExactAndUnsupportedPrecisionOrNonfiniteValuesFail() throws Exception {
        var config = defaults(); config.set("prices.base-trade-value.food", 2.25);
        assertEquals(new BigDecimal("3.38"), EconomyConfiguration.read(config).price(Commodity.FOOD, Condition.SHORTAGE));
        config.set("prices.base-trade-value.food", 2.001); assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        config.set("prices.base-trade-value.food", Double.NaN); assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        config.set("prices.base-trade-value.food", "2.25"); assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
    }
    @Test void extendingCommodityKeysRequiresMatchingInventoryAndPrice() throws Exception {
        var config = defaults(); config.set("starting-units-per-resident.wool", 2);
        assertThrows(IllegalArgumentException.class, () -> EconomyConfiguration.read(config));
        config.set("prices.base-trade-value.wool", 3);
        assertEquals(6, EconomyConfiguration.read(config).startingReserves(3).get(new Commodity("wool")));
    }
    @Test void normalFamineAndFeastEditsProduceDistinctExplainableScenariosWithoutChangingCalendar() throws Exception {
        var normal = EconomyConfiguration.read(defaults());
        var famineFile = defaults(); famineFile.set("food.output-percent", 25); var famine = EconomyConfiguration.read(famineFile);
        var feastFile = defaults(); feastFile.set("food.output-percent", 200); var feast = EconomyConfiguration.read(feastFile);
        assertEquals(normal.secondsPerPeriod(), famine.secondsPerPeriod()); assertEquals(normal.secondsPerPeriod(), feast.secondsPerPeriod());
        var normalResult = scenario(normal); var famineResult = scenario(famine); var feastResult = scenario(feast);
        assertEquals(48, normalResult.state().stocks().get(Commodity.FOOD).units());
        assertEquals(0, famineResult.state().stocks().get(Commodity.FOOD).units());
        assertEquals(208, feastResult.state().stocks().get(Commodity.FOOD).units());
        assertEquals(Condition.SEVERE_SHORTAGE, famineResult.reports().stream().filter(r -> r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().condition());
        assertEquals(Condition.ABUNDANT, feastResult.reports().stream().filter(r -> r.commodity().equals(Commodity.FOOD)).findFirst().orElseThrow().condition());
    }
    private Result scenario(EconomySettings settings) {
        var stocks = new TreeMap<Commodity, Stock>(); settings.startingReserves(4).forEach((commodity, units) -> stocks.put(commodity, new Stock(units, 0)));
        var state = new State(0, stocks); Result result = null;
        for (int period = 0; period < 20; period++) {
            var flows = new TreeMap<Commodity, Flow>(); state.stocks().keySet().forEach(commodity -> flows.put(commodity, Flow.idle()));
            flows.put(Commodity.FOOD, new Flow(settings.foodProduction(2, OptionalLong.of(2)), 0, settings.foodDemand(4, 0), 0, 0, 0, 0));
            result = CommodityAccounting.step(state, flows, settings.shortages()); state = result.state();
        }
        return result;
    }
}
