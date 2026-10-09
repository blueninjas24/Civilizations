package com.blueninjas24.civilizations.database;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

/** Explicit settlement spending, in hundredths of Trade Value. No inferred revenue or personal wallets. */
public final class TreasuryRepository {
    public record Account(UUID settlement, long hundredths) {
        public Account { Objects.requireNonNull(settlement); if (hundredths < 0) throw new IllegalArgumentException("Negative treasury"); }
        public BigDecimal tradeValue() { return BigDecimal.valueOf(hundredths, 2); }
    }
    public record Entry(UUID operation, UUID settlement, String kind, long amount, long balanceAfter, String reason) {}
    private final Connection connection;
    public TreasuryRepository(Connection connection) { this.connection = connection; }
    public Optional<Account> load(UUID town) throws SQLException {
        try (var s = connection.prepareStatement("SELECT hundredths FROM treasuries WHERE settlement_id=?")) {
            s.setString(1, town.toString()); try (var r = s.executeQuery()) { return r.next() ? Optional.of(new Account(town, r.getLong(1))) : Optional.empty(); }
        }
    }
    /** Caller must provide an approved initial amount and source. Absence is unknown, not zero. */
    public Entry initialize(UUID operation, UUID town, long hundredths, String reason) throws SQLException {
        if (hundredths < 0) throw new IllegalArgumentException("Negative opening treasury");
        return apply(operation, town, "OPENING", hundredths, reason);
    }
    /** Duplicate operation IDs replay the original result; mismatched reuse fails. Never allows debt. */
    public Entry post(UUID operation, UUID town, long delta, String reason) throws SQLException {
        if (delta == 0 || delta == Long.MIN_VALUE) throw new IllegalArgumentException("Invalid treasury movement");
        return apply(operation, town, delta > 0 ? "CREDIT" : "DEBIT", Math.abs(delta), reason);
    }
    /** Internal callers may compose funding/spending with a receipt in their own owner transaction. */
    Entry initializeRows(UUID operation, UUID town, long hundredths, String reason) throws SQLException {
        if(hundredths<0)throw new IllegalArgumentException("Negative opening treasury");
        return applyRows(operation,town,"OPENING",hundredths,reason);
    }
    Entry postRows(UUID operation, UUID town, long delta, String reason) throws SQLException {
        if(delta==0 || delta==Long.MIN_VALUE)throw new IllegalArgumentException("Invalid treasury movement");
        return applyRows(operation,town,delta>0?"CREDIT":"DEBIT",Math.abs(delta),reason);
    }
    private static void validate(UUID operation, UUID town, String reason) {
        Objects.requireNonNull(operation); Objects.requireNonNull(town); Objects.requireNonNull(reason);
        if(reason.isBlank())throw new IllegalArgumentException("Treasury source/reason required");
    }
    private Entry apply(UUID operation, UUID town, String kind, long amount, String reason) throws SQLException {
        validate(operation,town,reason);
        if(!connection.getAutoCommit())throw new SQLException("Treasury requires no active transaction");
        connection.setAutoCommit(false);
        try {
            var result=applyRows(operation,town,kind,amount,reason);
            connection.commit();connection.setAutoCommit(true);return result;
        } catch(SQLException | RuntimeException failure) {
            try { connection.rollback();connection.setAutoCommit(true); } catch(SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
    /** Never commits or rolls back: the caller must finish or roll back the entire surrounding operation. */
    private Entry applyRows(UUID operation, UUID town, String kind, long amount, String reason) throws SQLException {
        validate(operation,town,reason);
        if(connection.getAutoCommit())throw new SQLException("Treasury rows require an owner transaction");
        var old=entry(operation);
        if(old.isPresent()) {
            var saved=old.orElseThrow();
            if(!saved.settlement().equals(town) || !saved.kind().equals(kind) || saved.amount()!=amount || !saved.reason().equals(reason))
                throw new SQLException("Treasury operation ID reused with different inputs");
            return saved;
        }
        var before=load(town);long after;
        if(kind.equals("OPENING")) {
            if(before.isPresent())throw new SQLException("Treasury already initialized");
            after=amount;
            try(var s=connection.prepareStatement("INSERT INTO treasuries(settlement_id,hundredths) VALUES (?,?)")) { s.setString(1,town.toString());s.setLong(2,after);s.executeUpdate(); }
        } else {
            long balance=before.orElseThrow(()->new SQLException("Treasury not initialized")).hundredths();
            if(kind.equals("DEBIT") && amount>balance)throw new SQLException("Insufficient treasury");
            after=kind.equals("CREDIT")?Math.addExact(balance,amount):balance-amount;
            try(var s=connection.prepareStatement("UPDATE treasuries SET hundredths=? WHERE settlement_id=?")) {
                s.setLong(1,after);s.setString(2,town.toString());if(s.executeUpdate()!=1)throw new SQLException("Treasury disappeared");
            }
        }
        var result=new Entry(operation,town,kind,amount,after,reason);
        try(var s=connection.prepareStatement("INSERT INTO treasury_entries(operation_id,settlement_id,kind,amount,balance_after,reason) VALUES (?,?,?,?,?,?)")) {
            s.setString(1,operation.toString());s.setString(2,town.toString());s.setString(3,kind);s.setLong(4,amount);s.setLong(5,after);s.setString(6,reason);s.executeUpdate();
        }
        return result;
    }
    public Optional<Entry> entry(UUID operation) throws SQLException {
        try (var s = connection.prepareStatement("SELECT * FROM treasury_entries WHERE operation_id=?")) {
            s.setString(1, operation.toString()); try (var r = s.executeQuery()) {
                return r.next() ? Optional.of(new Entry(operation, UUID.fromString(r.getString("settlement_id")), r.getString("kind"),
                        r.getLong("amount"), r.getLong("balance_after"), r.getString("reason"))) : Optional.empty();
            }
        }
    }
}
