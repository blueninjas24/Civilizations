package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.economy.FoodAidPreview;
import com.blueninjas24.civilizations.population.Resident;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

/** Main-thread, cached read-only aid preview. No inventory setter, saveData, JDBC or delivery submission. */
public final class FoodAidPreviewCommand implements CommandExecutor {
    private final PopulationSession session;
    private final FoodAidPreview.Policy policy;
    public FoodAidPreviewCommand(PopulationSession session, FoodAidPreview.Policy policy) {
        this.session = Objects.requireNonNull(session); this.policy = Objects.requireNonNull(policy);
    }
    public static long foodUnits(ItemStack stack, FoodAidPreview.Policy policy) {
        // Custom names, enchantments and plugin components may carry value; never treat them as ordinary food.
        if (stack.hasItemMeta()) throw new IllegalArgumentException("Use plain food without custom item data");
        return policy.units(stack.getType().name(), stack.getAmount());
    }
    static FoodAidPreview.Recipient recipient(PopulationSession session, org.bukkit.Location location, FoodAidPreview.Policy policy, UUID requested) {
        var projection = session.projectionView().orElse(null);
        if (projection == null) return new FoodAidPreview.Recipient(FoodAidPreview.Match.NONE,null);
        var economies = session.economyView();
        var homes = new ArrayList<FoodAidPreview.Home>();
        for (var person : projection.population().residents()) {
            if (person.status() != Resident.Status.ALIVE || person.householdId() == null) continue;
            var home = projection.identities().homes().get(person.householdId());
            var economy = economies.get(person.settlementId());
            if (home == null || economy == null || economy.farm() == null || !economy.farm().complete()) continue;
            var position = home.position();
            homes.add(new FoodAidPreview.Home(person.settlementId(), home.worldId(), position.x(), position.y(), position.z()));
        }
        return FoodAidPreview.recipient(location.getWorld().getUID(), location.getX(), location.getY(), location.getZ(), homes, policy.radius(), requested);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("civilizations.aid.preview")) { sender.sendMessage("You do not have permission to preview food aid."); return true; }
        if (!(sender instanceof Player player)) { sender.sendMessage("Food aid preview requires a player holding food near a village."); return true; }
        if (!policy.enabled()) { player.sendMessage("Food aid previews are disabled in economy.yml."); return true; }
        if (args.length < 1 || args.length > 2 || !args[0].equalsIgnoreCase("aid")) { player.sendMessage("Usage: /civ aid [settlement UUID]"); return true; }
        UUID requested = null;
        try { if (args.length == 2) requested = UUID.fromString(args[1]); }
        catch (IllegalArgumentException invalid) { player.sendMessage("Use a valid settlement UUID."); return true; }
        var projection = session.projectionView().orElse(null);
        if (!session.status().ready() || projection == null) { player.sendMessage("Civilizations is still starting; try again shortly."); return true; }
        var economies = session.economyView();
        var recipient = recipient(session, player.getLocation(), policy, requested);
        if (recipient.match() == FoodAidPreview.Match.NONE) { player.sendMessage("No ready village home within " + policy.radius() + " blocks. Food aid needs reliable village evidence."); return true; }
        if (recipient.match() == FoodAidPreview.Match.AMBIGUOUS) { player.sendMessage("Multiple villages are equally close. Use /civ aid <settlement UUID> to choose."); return true; }
        var stack = player.getInventory().getItemInMainHand();
        long units;
        try { units = foodUnits(stack, policy); }
        catch (IllegalArgumentException invalid) { player.sendMessage(invalid.getMessage() + ". Hold a plain supported food stack; mappings are in economy.yml."); return true; }
        var economy = economies.get(recipient.town());
        player.sendMessage("Food aid preview for " + session.settlementNames().getOrDefault(recipient.town(), "nearby village") + ": " + stack.getAmount() + " " + stack.getType().name().toLowerCase(Locale.ROOT) + " = " + units + " food units.");
        if (economy.needs() == null) player.sendMessage("Food situation: awaiting the first completed food period.");
        else player.sendMessage("Latest food period " + economy.needs().period() + ": " + economy.needs().sharedFood()
                + "; demand supplied " + economy.needs().fulfilled() + "/" + economy.needs().demand() + ".");
        player.sendMessage("Preview only: no items taken, no delivery created and no payment or reputation awarded.");
        return true;
    }
}
