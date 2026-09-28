package com.blueninjas24.civilizations.listener;

import com.blueninjas24.civilizations.Civilizations;
import com.blueninjas24.civilizations.needs.NeedsEngine;
import com.blueninjas24.civilizations.needs.NeedsEngine.HousingStatus;
import com.blueninjas24.civilizations.settlement.Settlement;
import com.blueninjas24.civilizations.settlement.SettlementNameGenerator;
import io.papermc.paper.entity.poi.PoiTypes;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VillageDiscoveryListener implements Listener {

    private final Civilizations plugin;
    private final NeedsEngine needsEngine = new NeedsEngine();
    private final Set<String> discoveredVillages = new HashSet<>();

    public VillageDiscoveryListener(Civilizations plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        // Only check when the player enters a different chunk.
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }

        Player player = event.getPlayer();
        Location location = player.getLocation();

        var villagePois = location.getWorld().locateAllPoiInRange(
                location,
                poiType -> poiType.equals(PoiTypes.HOME),
                48
        );

        if (villagePois.isEmpty()) {
            return;
        }

        String villageKey =
                location.getWorld().getName()
                        + ":"
                        + (location.getBlockX() >> 7)
                        + ":"
                        + (location.getBlockZ() >> 7);

        if (discoveredVillages.add(villageKey)) {

            try {
                Settlement existingSettlement =
                        plugin.getDatabaseManager().findNearbySettlement(
                                location.getWorld().getName(),
                                location.getBlockX(),
                                location.getBlockZ(),
                                128
                        );

                if (existingSettlement != null) {

                    int population = location.getWorld()
                            .getNearbyEntities(location, 128, 64, 128)
                            .stream()
                            .filter(entity -> entity instanceof Villager)
                            .toList()
                            .size();

                    int beds = location.getWorld().locateAllPoiInRange(
                            location,
                            poiType -> poiType.equals(PoiTypes.HOME),
                            128
                    ).size();

                    existingSettlement.setPopulation(population);
                    plugin.getDatabaseManager()
                            .updatePopulation(existingSettlement);

                    existingSettlement.setBeds(beds);
                    plugin.getDatabaseManager()
                            .updateBeds(existingSettlement);

                    // Evaluate what the settlement currently needs.
                    HousingStatus housingStatus =
                            needsEngine.evaluateHousing(existingSettlement);

                    player.sendMessage(
                            "§6[Civilizations] §fYou have entered §e"
                                    + existingSettlement.getName()
                                    + " §7— Population: §f"
                                    + population
                                    + " §7— Beds: §f"
                                    + beds
                                    + " §7— Housing: §f"
                                    + housingStatus
                    );

                    if (housingStatus == HousingStatus.HOUSING_SHORTAGE) {
                        plugin.getLogger().info(
                                existingSettlement.getName()
                                        + " has identified a housing shortage."
                        );
                    }

                    return;
                }

            } catch (SQLException e) {
                plugin.getLogger().severe(
                        "Failed to check for existing settlement: "
                                + e.getMessage()
                );
                return;
            }

            String name = SettlementNameGenerator.generate();

            Settlement settlement = new Settlement(
                    UUID.randomUUID(),
                    name,
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    System.currentTimeMillis(),
                    0,
                    0
            );

            try {
                plugin.getDatabaseManager().saveSettlement(settlement);
            } catch (SQLException e) {
                plugin.getLogger().severe(
                        "Failed to save settlement "
                                + name
                                + ": "
                                + e.getMessage()
                );
                return;
            }

            player.sendMessage(
                    "§6[Civilizations] §fSettlement discovered: §e" + name
            );

            plugin.getLogger().info(
                    player.getName()
                            + " discovered "
                            + name
                            + " near "
                            + location.getBlockX()
                            + ", "
                            + location.getBlockZ()
            );
        }
    }
}