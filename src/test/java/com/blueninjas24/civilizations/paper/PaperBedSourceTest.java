package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.settlement.BedScan;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.block.data.type.Bed;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PaperBedSourceTest {
    @Test void negativeHeightSectionsMapToPaperArrayIndexesAndBedHalvesCountOnce() {
        var sectionCalls = new AtomicInteger();
        var snapshot = (ChunkSnapshot) Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(), new Class<?>[]{ChunkSnapshot.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isSectionEmpty" -> {
                        int index = (int) args[0]; assertTrue(index >= 0 && index < 24);
                        sectionCalls.incrementAndGet(); yield index != 8; // world Y 64..79
                    }
                    case "getBlockType" -> (int) args[0] == 0 && (int) args[1] == 64 && (int) args[2] < 2 ? Material.RED_BED : Material.AIR;
                    case "getBlockData" -> Proxy.newProxyInstance(Bed.class.getClassLoader(), new Class<?>[]{Bed.class},
                            (bed, bedMethod, bedArgs) -> {
                                assertEquals("getPart", bedMethod.getName());
                                return (int) args[2] == 1 ? Bed.Part.HEAD : Bed.Part.FOOT;
                            });
                    default -> throw new AssertionError("Unexpected snapshot call: " + method.getName());
                });
        var scanner = new BedScan(new PaperBedSource(snapshot, -64), -1, -1, -64, 320, 10);
        while (!scanner.complete()) scanner.advance(200);
        assertEquals(24, sectionCalls.get());
        assertEquals(List.of(new VillageObservation.Position(-16, 64, -15)), scanner.beds());
    }
}
