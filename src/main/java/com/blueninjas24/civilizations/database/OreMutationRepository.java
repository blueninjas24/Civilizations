package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.sql.*;
import java.util.*;

/** Placement exclusions and actual source losses from one native event commit together. */
public final class OreMutationRepository {
    public record Result(int placements,long retiredBatches) {}
    private final Connection connection;
    public OreMutationRepository(Connection connection){this.connection=Objects.requireNonNull(connection);}
    public Result record(UUID world,Collection<Position> placements,Collection<Position> removals,long millis,String reason)throws SQLException {
        Objects.requireNonNull(world);var placed=List.copyOf(placements);var removed=List.copyOf(removals);
        if(millis<0||placed.size()+removed.size()==0||placed.size()>512||removed.size()>512
                ||new HashSet<>(placed).size()!=placed.size()||new HashSet<>(removed).size()!=removed.size()
                ||reason==null||reason.isBlank()||reason.length()>256)throw new IllegalArgumentException("Invalid bounded ore mutation");
        if(!connection.getAutoCommit())throw new SQLException("Mutation requires idle SQL owner");
        connection.setAutoCommit(false);
        try {
            int added=placed.isEmpty()?0:new OrePlacementRepository(connection).recordRows(world,placed,millis);
            long retired=removed.isEmpty()?0:new MineralSources(connection).removeRows(world,removed,millis,reason);
            connection.commit();connection.setAutoCommit(true);return new Result(added,retired);
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
}
