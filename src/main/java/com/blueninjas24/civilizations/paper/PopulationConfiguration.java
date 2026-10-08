package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.database.VillageEnrollmentRepository;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.VillageRecognition;
import org.bukkit.configuration.ConfigurationSection;

/** Strict adapter: reject value types and truncated numbers before opening a database. */
public final class PopulationConfiguration {
    private PopulationConfiguration() {}

    public static PopulationSession.Settings read(ConfigurationSection config) {
        return new PopulationSession.Settings(integer(config, "population.seed"),
                integer(config, "population.millis-per-month"),
                smallInteger(config, "population.maximum-offline-months"),
                smallInteger(config, "population.months-per-batch"),
                new PopulationPolicy(smallInteger(config, "population.months-per-year"),
                        integer(config, "population.adulthood-months"),
                        integer(config, "population.parent-minimum-months"),
                        integer(config, "population.parent-maximum-months"),
                        integer(config, "population.senior-months"),
                        decimal(config, "population.birth-chance"),
                        decimal(config, "population.background-death-chance"),
                        decimal(config, "population.senior-death-increase-per-year")),
                new MigrationPolicy(decimal(config, "population.household-move-chance"),
                        decimal(config, "population.external-arrival-chance")),
                new VillageRecognition.Policy(smallInteger(config, "recognition.connection-radius"),
                        smallInteger(config, "recognition.resident-radius"),
                        smallInteger(config, "recognition.minimum-beds"),
                        smallInteger(config, "recognition.minimum-villagers"),
                        smallInteger(config, "recognition.maximum-beds"),
                        smallInteger(config, "recognition.maximum-villagers")),
                new VillageEnrollmentRepository.Policy(smallInteger(config, "enrollment.home-bed-radius"),
                        smallInteger(config, "enrollment.residents-per-bed"),
                        integer(config, "enrollment.initial-adult-months"),
                        integer(config, "enrollment.initial-child-months"),
                        new PartnershipFormation.Policy(bool(config, "enrollment.form-partnerships"),
                                integer(config, "enrollment.partnership-minimum-months"))));
    }

    private static long integer(ConfigurationSection config, String path) {
        Object value = config.get(path);
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
            return ((Number) value).longValue();
        throw new IllegalArgumentException(path + " must be an integer");
    }
    private static int smallInteger(ConfigurationSection config, String path) {
        try { return Math.toIntExact(integer(config, path)); }
        catch (ArithmeticException invalid) { throw new IllegalArgumentException(path + " is too large", invalid); }
    }
    private static double decimal(ConfigurationSection config, String path) {
        if (config.get(path) instanceof Number value) return value.doubleValue();
        throw new IllegalArgumentException(path + " must be a number");
    }
    private static boolean bool(ConfigurationSection config, String path) {
        if (config.get(path) instanceof Boolean value) return value;
        throw new IllegalArgumentException(path + " must be true or false");
    }
}
