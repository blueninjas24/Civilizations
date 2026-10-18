package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.IndustryRuntimePolicy;
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
            IndustryStockCatalogue.addEmptyRows(c,business,id,weight,"bakery-catalogue-v1");
        }
    }
}
