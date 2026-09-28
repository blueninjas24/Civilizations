package com.blueninjas24.civilizations.listener;

import io.papermc.paper.entity.poi.PoiType;
import io.papermc.paper.registry.keys.tags.PoiTypeTagKeys;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import com.blueninjas24.civilizations.Civilizations;
import com.blueninjas24.civilizations.settlement.Settlement;
import com.blueninjas24.civilizations.settlement.SettlementNameGenerator;

import java.sql.SQLException;
import java.util.UUID;

import java.util.HashSet;
import java.util.Set;

public class VillageDiscoveryListener implements Listener {

    private final Civilizations plugin;

    public VillageDiscoveryListener(Civilizations plugin) {
        this.plugin = plugin;
    }

    private final Set<String> discoveredVillages = new HashSet<>();

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
                poiType -> poiType.equals(io.papermc.paper.entity.poi.PoiTypes.HOME),
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
                    player.sendMessage(
                            "§6[Civilizations] §fYou have entered §e"
                                    + existingSettlement.getName()
                    );

                    return;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(
                        "Failed to check for existing settlement: " + e.getMessage()
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
                    System.currentTimeMillis()
            );

            try {
                plugin.getDatabaseManager().saveSettlement(settlement);
            } catch (SQLException e) {
                plugin.getLogger().severe(
                        "Failed to save settlement " + name + ": " + e.getMessage()
                );
                return;
            }

            player.sendMessage(
                    "§6[Civilizations] §fSettlement discovered: §e" + name
            );

            plugin.getLogger().info(
                    player.getName() + " discovered " + name
                            + " near "
                            + location.getBlockX() + ", "
                            + location.getBlockZ()
            );
        }
    }
}