package com.blueninjas24.civilizations.database;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Audited empty-slot additions; never rescales saved goods, storage, capital or original terms. */
final class IndustryStockCatalogue {
    private IndustryStockCatalogue() {}
    static void addEmptyRows(Connection c,UUID business,String id,long weight,String namespace)throws SQLException {
        if(c.getAutoCommit())throw new SQLException("Catalogue transition requires owner transaction");
        try(var s=c.prepareStatement("SELECT 1 FROM industry_stock WHERE business_id=? AND commodity=?")) {
            s.setString(1,business.toString());s.setString(2,id);
            try(var r=s.executeQuery()){if(r.next())return;} // Never rescale an existing slot.
        }
        var operation=UUID.nameUUIDFromBytes(("civilizations:"+namespace+":"+business+":"+id).getBytes(StandardCharsets.UTF_8));
        boolean reviewed;
        try(var s=c.prepareStatement("SELECT 1 FROM industry_operations WHERE id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()){reviewed=r.next();}
        }
        if(reviewed)throw new SQLException("Audited industry stock slot disappeared; reviewed recovery required");
        String opening;
        try(var s=c.prepareStatement("SELECT terms FROM industry_stores WHERE business_id=?")) {
            s.setString(1,business.toString());try(var r=s.executeQuery()){if(!r.next())throw new SQLException("Store disappeared");opening=r.getString(1);}
        }
        try(var s=c.prepareStatement("INSERT INTO industry_stock(business_id,commodity,space_per_unit,units) VALUES(?,?,?,0)")) {
            s.setString(1,business.toString());s.setString(2,id);s.setLong(3,weight);s.executeUpdate();
        }
        try(var s=c.prepareStatement("INSERT INTO industry_operations(id,terms,quantity,explanation) VALUES(?,?,0,?)")) {
            s.setString(1,operation.toString());s.setString(2,namespace+"|"+business+"|"+id+"|"+weight+"|"+opening);
            s.setString(3,"Added empty "+id+" ingredient slot (space/unit="+weight+"); stock, cash, capacity and opening terms preserved");s.executeUpdate();
        }
    }
}
