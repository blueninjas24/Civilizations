package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.economy.*;
import java.util.*;

/** Exact barter consent/recovery serialized in the same Minecraft snapshot as inventory. */
public record PendingFoodBarter(FoodBarters.Terms terms,Stage stage) {
    public static final String KEY="civilizations:pending_food_barter";
    public enum Stage { PREPARED, EXCHANGED, CANCELLED, REVIEW }
    public PendingFoodBarter {Objects.requireNonNull(terms);Objects.requireNonNull(stage);}
    public PendingFoodBarter withStage(Stage next){return new PendingFoodBarter(terms,next);}
    public String encode(){return "bar1|"+stage+"|"+terms.operation()+"|"+terms.player()+"|"+terms.settlement()+"|"+terms.foodUnits()
            +"|"+terms.foodItem()+"|"+terms.foodCount()+"|"+terms.commodity().id()+"|"+terms.paymentItem()+"|"+terms.paymentCount()+"|"+terms.unitsPerItem();}
    public static PendingFoodBarter decode(String encoded) {
        if(encoded==null||encoded.length()>340)throw new IllegalArgumentException("Invalid barter receipt length");
        var f=encoded.split("\\|",-1);if(f.length!=12||!f[0].equals("bar1"))throw new IllegalArgumentException("Unsupported barter receipt");
        var receipt=new PendingFoodBarter(new FoodBarters.Terms(UUID.fromString(f[2]),UUID.fromString(f[3]),UUID.fromString(f[4]),
                Long.parseLong(f[5]),f[6],Integer.parseInt(f[7]),new Commodity(f[8]),f[9],Integer.parseInt(f[10]),Long.parseLong(f[11])),Stage.valueOf(f[1]));
        if(!receipt.encode().equals(encoded))throw new IllegalArgumentException("Noncanonical barter receipt");return receipt;
    }
}
