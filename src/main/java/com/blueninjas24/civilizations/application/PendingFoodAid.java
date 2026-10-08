package com.blueninjas24.civilizations.application;

import java.util.*;

/** Immutable recovery receipt saved alongside the player's debited Minecraft inventory. */
public record PendingFoodAid(UUID operation, UUID player, UUID settlement, long units, String item, int count) {
    public PendingFoodAid {
        Objects.requireNonNull(operation); Objects.requireNonNull(player); Objects.requireNonNull(settlement);
        if (units < 1 || units > 64_000_000 || count < 1 || count > 64 || item == null || !item.matches("[A-Z_]{1,32}"))
            throw new IllegalArgumentException("Invalid pending food aid receipt");
    }
    public String encode() { return "aid1|"+operation+"|"+player+"|"+settlement+"|"+units+"|"+item+"|"+count; }
    public static PendingFoodAid decode(String encoded) {
        if (encoded == null || encoded.length() > 200) throw new IllegalArgumentException("Invalid pending aid receipt");
        var fields=encoded.split("\\|",-1);
        if (fields.length != 7 || !fields[0].equals("aid1")) throw new IllegalArgumentException("Unknown pending aid receipt format");
        var receipt=new PendingFoodAid(UUID.fromString(fields[1]),UUID.fromString(fields[2]),UUID.fromString(fields[3]),Long.parseLong(fields[4]),fields[5],Integer.parseInt(fields[6]));
        if(!receipt.encode().equals(encoded))throw new IllegalArgumentException("Noncanonical pending aid receipt");
        return receipt;
    }
    public String source() { return "player-food-aid:"+player+":"+count+":"+item; }
}
