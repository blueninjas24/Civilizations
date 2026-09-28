package com.blueninjas24.civilizations.listener;

import io.papermc.paper.entity.poi.PoiType;
import io.papermc.paper.registry.keys.tags.PoiTypeTagKeys;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;

public class VillageDiscoveryListener implements Listener {

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
            player.sendMessage("§6[Civilizations] §fYou have discovered a settlement!");

            player.getServer().getLogger().info(
                    "[Civilizations] "
                            + player.getName()
                            + " discovered a settlement near "
                            + location.getBlockX()
                            + ", "
                            + location.getBlockZ()
            );
        }
    }
}