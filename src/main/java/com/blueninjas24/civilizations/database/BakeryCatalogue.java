package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.IndustryRuntimePolicy;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Explicit additive transition for configured bakery ingredients; never replaces saved opening terms or assets. */
final class BakeryCatalogue {
    private BakeryCatalogue() {}
    static void ensureRows(Connection c, UUID business, String kind, IndustryRuntimePolicy policy) throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Bakery catalogue transition requires owner transaction");
        if(!Set.of("bakery","warehouse").contains(kind))return;
        for(String id:List.of("milk","eggs")) {
            var good=new Commodity(id);var weight=policy.mining().storageSpacePerUnit().get(good);
            if(weight==null||policy.recipes().values().stream().noneMatch(r->r.industry().equals("bakery")&&r.inputs().containsKey(good)))continue;
            try(var s=c.prepareStatement("SELECT 1 FROM industry_stock WHERE business_id=? AND commodity=?")) {
                s.setString(1,business.toString());s.setString(2,id);
                try(var r=s.executeQuery()){if(r.next())continue;} // Never rescale an existing slot.
            }
            var operation=UUID.nameUUIDFromBytes(("civilizations:bakery-catalogue-v1:"+business+":"+id).getBytes(StandardCharsets.UTF_8));
            boolean reviewed;
            try(var s=c.prepareStatement("SELECT 1 FROM industry_operations WHERE id=?")) {
                s.setString(1,operation.toString());try(var r=s.executeQuery()){reviewed=r.next();}
            }
            if(reviewed)throw new SQLException("Audited bakery ingredient slot disappeared; reviewed recovery required");
            String opening;
            try(var s=c.prepareStatement("SELECT terms FROM industry_stores WHERE business_id=?")) {
                s.setString(1,business.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Store disappeared");opening=r.getString(1);}
            }
            try(var s=c.prepareStatement("INSERT INTO industry_stock(business_id,commodity,space_per_unit,units) VALUES(?,?,?,0)")) {
                s.setString(1,business.toString());s.setString(2,id);s.setLong(3,weight);s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO industry_operations(id,terms,quantity,explanation) VALUES(?,?,0,?)")) {
                s.setString(1,operation.toString());s.setString(2,"bakery-catalogue-v1|"+business+"|"+id+"|"+weight+"|"+opening);
                s.setString(3,"Added empty "+id+" ingredient slot (space/unit="+weight+"); stock, cash, capacity and opening terms preserved");s.executeUpdate();
            }
        }
    }
}
