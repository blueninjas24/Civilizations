package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import java.util.*;

/** Pure storage-slot exchange plan. Caller preserves full metadata of unchanged slots and revalidates
 * actual Paper contents immediately before applying. No armor/offhand/container/drop fallback. */
public final class LivestockInventory {
    private LivestockInventory() {}
    public record Slot(String item, int count, int maximum, boolean plain) {
        public static final Slot EMPTY = new Slot("", 0, 0, true);
        public Slot {
            Objects.requireNonNull(item);
            if (count == 0) {
                if (!item.isEmpty() || maximum != 0 || !plain) throw new IllegalArgumentException("Invalid empty inventory slot");
            } else if (count < 0 || maximum < 1 || !item.matches("[A-Z0-9_]{1,80}") || plain && count > maximum)
                throw new IllegalArgumentException("Invalid inventory slot");
        }
        boolean usable(String material) { return plain && item.equals(material); }
    }
    public record Plan(List<Slot> before, List<Slot> after) {
        public Plan { before = List.copyOf(before); after = List.copyOf(after); }
    }
    public static final class Unavailable extends IllegalArgumentException {
        Unavailable(String message) { super(message); }
    }
    public static Plan plan(List<Slot> inventory, Terms terms, int outputMaximum) {
        return plan(inventory,terms,outputMaximum,true);
    }
    /** Pending goods occupy separate stacks so death retention cannot also protect older player property. */
    public static Plan separated(List<Slot> inventory,Terms terms,int outputMaximum) {
        return plan(inventory,terms,outputMaximum,false);
    }
    private static Plan plan(List<Slot> inventory,Terms terms,int outputMaximum,boolean mergeOutput) {
        Objects.requireNonNull(terms);
        var before = List.copyOf(inventory);
        if (before.isEmpty() || before.size() > 36 || outputMaximum < 1)
            throw new IllegalArgumentException("Invalid storage inventory or output stack limit");
        var after = new ArrayList<>(before);
        take(after, "EMERALD", terms.emeralds(), "Not enough plain emeralds in your inventory.");
        if (terms.emptyBuckets() > 0) take(after, "BUCKET", terms.emptyBuckets(), "Bring a plain empty bucket for the milk.");
        int remaining = terms.count(); String output = terms.product().item();
        for (int i = 0; mergeOutput && i < after.size() && remaining > 0; i++) {
            var slot = after.get(i);
            if (!slot.usable(output) || slot.maximum() != outputMaximum) continue;
            int moved = Math.min(remaining, slot.maximum() - slot.count());
            if (moved > 0) { after.set(i, new Slot(output, slot.count() + moved, outputMaximum, true)); remaining -= moved; }
        }
        for (int i = 0; i < after.size() && remaining > 0; i++) if (after.get(i).count() == 0) {
            int moved = Math.min(remaining, outputMaximum);
            after.set(i, new Slot(output, moved, outputMaximum, true)); remaining -= moved;
        }
        if (remaining != 0) throw new Unavailable("Make room in your inventory before buying these goods.");
        return new Plan(before, after);
    }
    private static void take(List<Slot> slots, String material, int count, String failure) {
        int remaining = count;
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            var slot = slots.get(i); if (!slot.usable(material)) continue;
            int removed = Math.min(remaining, slot.count()), left = slot.count() - removed;
            slots.set(i, left == 0 ? Slot.EMPTY : new Slot(material, left, slot.maximum(), true)); remaining -= removed;
        }
        if (remaining != 0) throw new Unavailable(failure);
    }
}
