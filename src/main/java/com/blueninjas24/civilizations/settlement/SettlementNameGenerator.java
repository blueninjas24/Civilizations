package com.blueninjas24.civilizations.settlement;

import java.util.List;
import java.util.Random;

public class SettlementNameGenerator {

    private static final Random RANDOM = new Random();

    private static final List<String> PREFIXES = List.of(
            "Oak", "Willow", "Stone", "Iron", "Ash",
            "River", "Green", "High", "West", "Red",
            "Silver", "Thorn", "Pine", "Mist", "Gold"
    );

    private static final List<String> SUFFIXES = List.of(
            "mere", "ford", "haven", "wick", "stead",
            "brook", "field", "vale", "wood", "ridge",
            "crest", "fall", "bridge", "watch", "holm"
    );

    public static String generate() {
        String prefix = PREFIXES.get(RANDOM.nextInt(PREFIXES.size()));
        String suffix = SUFFIXES.get(RANDOM.nextInt(SUFFIXES.size()));

        return prefix + suffix;
    }
}