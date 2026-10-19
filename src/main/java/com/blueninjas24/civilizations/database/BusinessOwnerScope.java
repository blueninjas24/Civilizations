package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.Business;
import com.blueninjas24.civilizations.population.Resident;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/** Ownership may span villages; workplaces and worker assignments remain local. */
final class BusinessOwnerScope {
    private BusinessOwnerScope() {}
    static boolean sameWorld(Connection connection,UUID first,UUID second)throws SQLException {
        if(first.equals(second))return true; // Preserve legacy local identity without inventing a world binding.
        try(var s=connection.prepareStatement("""
                SELECT 1 FROM settlement_worlds a JOIN settlement_worlds b ON b.world_id=a.world_id
                WHERE a.settlement_id=? AND b.settlement_id=?
                """)) {
            s.setString(1,first.toString());s.setString(2,second.toString());
            try(var r=s.executeQuery()){return r.next();}
        }
    }
    static boolean available(Connection connection,Resident owner,Business business)throws SQLException {
        return owner.status()==Resident.Status.ALIVE&&sameWorld(connection,owner.settlementId(),business.settlementId());
    }
}
