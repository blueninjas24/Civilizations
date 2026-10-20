package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.economy.Commodity;
import java.sql.*;
import java.util.*;

/** Initial transfer receipts are immutable history; available() reports goods still held. No cash fee or native writes. */
public final class ConstructionMaterials {
    public record Authority(UUID draft,UUID world,long checkedMillis,boolean worldLoaded,boolean siteCleared) {
        public Authority{Objects.requireNonNull(draft);Objects.requireNonNull(world);if(checkedMillis<0)throw new IllegalArgumentException("Negative construction authority time");}
    }
    public record Held(Commodity commodity,long units,long beforeUnits,long afterUnits){}
    public record Receipt(UUID operation,UUID draft,UUID business,long reservedMillis,List<Held> materials) {
        public Receipt{materials=List.copyOf(materials);}
    }
    private final Connection connection;
    public ConstructionMaterials(Connection connection){this.connection=Objects.requireNonNull(connection);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()) {
            s.execute("""
                CREATE TABLE construction_material_reservations (
                    operation_id TEXT PRIMARY KEY NOT NULL,
                    draft_id TEXT NOT NULL UNIQUE REFERENCES construction_drafts(id),
                    business_id TEXT NOT NULL REFERENCES businesses(id),
                    reserved_ms INTEGER NOT NULL CHECK(typeof(reserved_ms)='integer' AND reserved_ms>=0)
                )
                """);
            s.execute("""
                CREATE TABLE construction_material_items (
                    operation_id TEXT NOT NULL REFERENCES construction_material_reservations(operation_id),
                    commodity TEXT NOT NULL CHECK(length(commodity) BETWEEN 1 AND 64),
                    units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units>0),
                    before_units INTEGER NOT NULL CHECK(typeof(before_units)='integer' AND before_units>=units),
                    after_units INTEGER NOT NULL CHECK(typeof(after_units)='integer' AND after_units>=0 AND after_units=before_units-units),
                    PRIMARY KEY(operation_id,commodity)
                )
                """);
        }
    }
    /** Trusted caller supplies fresh, complete land/entity/protection authority. Quotes or previews alone cannot call this. */
    public Receipt reserveBusiness(UUID operation,UUID draftId,UUID business,Authority authority,long now,
                                   ConstructionBudgetPolicy policy)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(draftId);Objects.requireNonNull(business);Objects.requireNonNull(authority);Objects.requireNonNull(policy);
        if(now<0)throw new IllegalArgumentException("Negative reservation time");
        if(!connection.getAutoCommit())throw new SQLException("Construction material reservation requires no active owner transaction");connection.setAutoCommit(false);
        try {
            var old=find(operation);
            if(old.isPresent()) {
                var saved=old.orElseThrow();if(!saved.draft().equals(draftId)||!saved.business().equals(business))throw new SQLException("Construction reservation identity reused with changed source or draft");
                connection.commit();connection.setAutoCommit(true);return saved;
            }
            var draft=new ConstructionDraftRepository(connection).find(draftId).orElseThrow(()->new SQLException("Unknown construction draft"));
            if(!authority.draft().equals(draft.id())||!authority.world().equals(draft.world())||!authority.worldLoaded()||!authority.siteCleared()||authority.checkedMillis()>now||now-authority.checkedMillis()>5000)
                throw new SQLException("Fresh complete land authority and loaded world required before reserving supplies");
            new ConstructionSiteReservations(connection).requireActiveRows(draft);
            try(var s=connection.prepareStatement("SELECT 1 FROM construction_supply_orders WHERE draft_id=? LIMIT 1")) {
                s.setString(1,draftId.toString());try(var r=s.executeQuery()){if(r.next())throw new SQLException("Paid supply and owned-stock routes cannot double-fund the same project");}
            }
            var founding=new BusinessRepository(connection).findFounding(business).orElseThrow(()->new SQLException("Unknown construction owner"));
            if(!founding.business().settlementId().equals(draft.settlement())||!founding.site().worldId().equals(draft.world()))throw new SQLException("Construction supplies must belong to this village and world");
            BusinessClosure.requireOpen(connection,business);
            var store=new IndustryRepository(connection).store(business).orElseThrow(()->new SQLException("Business has no material store"));
            var quote=policy.review(ConstructionBudgetPolicy.Source.BUSINESS,draft.template().purpose(),0,
                    new ConstructionBudgetPolicy.Evidence(store.hundredths(),0,0,0,0));
            if(!quote.allowed())throw new SQLException(quote.reason());
            var materials=new ArrayList<Held>();
            for(var item:draft.template().materials().entrySet()) {
                long before=store.units().getOrDefault(item.getKey(),0L);
                if(before<item.getValue())throw new SQLException("Construction waits for owned material: "+item.getKey().id());
                materials.add(new Held(item.getKey(),item.getValue(),before,before-item.getValue()));
            }
            try(var s=connection.prepareStatement("INSERT INTO construction_material_reservations VALUES(?,?,?,?)")) {
                s.setString(1,operation.toString());s.setString(2,draftId.toString());s.setString(3,business.toString());s.setLong(4,now);s.executeUpdate();
            }
            for(var item:materials) {
                try(var s=connection.prepareStatement("UPDATE industry_stock SET units=? WHERE business_id=? AND commodity=? AND units=?")) {
                    s.setLong(1,item.afterUnits());s.setString(2,business.toString());s.setString(3,item.commodity().id());s.setLong(4,item.beforeUnits());
                    if(s.executeUpdate()!=1)throw new SQLException("Construction source stock changed");
                }
                try(var s=connection.prepareStatement("INSERT INTO construction_material_items VALUES(?,?,?,?,?)")) {
                    s.setString(1,operation.toString());s.setString(2,item.commodity().id());s.setLong(3,item.units());s.setLong(4,item.beforeUnits());s.setLong(5,item.afterUnits());s.executeUpdate();
                }
            }
            var receipt=new Receipt(operation,draftId,business,now,materials);connection.commit();connection.setAutoCommit(true);return receipt;
        }catch(SQLException|RuntimeException failure){try{connection.rollback();connection.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    public Optional<Receipt> find(UUID operation)throws SQLException {
        try(var s=connection.prepareStatement("SELECT * FROM construction_material_reservations WHERE operation_id=?")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                if(!r.next())return Optional.empty();var held=new ArrayList<Held>();
                try(var items=connection.prepareStatement("SELECT * FROM construction_material_items WHERE operation_id=? ORDER BY commodity")) {
                    items.setString(1,operation.toString());try(var rows=items.executeQuery()){while(rows.next()) {
                        if(held.size()==ConstructionTemplate.MAX_STAGES*32)throw new SQLException("Construction material receipt exceeds frozen commodity budget");
                        held.add(new Held(new Commodity(rows.getString("commodity")),rows.getLong("units"),rows.getLong("before_units"),rows.getLong("after_units")));
                    }}
                }
                var draft=new ConstructionDraftRepository(connection).find(UUID.fromString(r.getString("draft_id"))).orElseThrow(()->new SQLException("Reservation lost its frozen draft"));
                var business=UUID.fromString(r.getString("business_id"));var source=new BusinessRepository(connection).findFounding(business).orElseThrow(()->new SQLException("Reservation lost its source business"));
                if(!source.business().settlementId().equals(draft.settlement())||!source.site().worldId().equals(draft.world()))throw new SQLException("Reservation source no longer matches frozen world/village identity");
                var expected=draft.template().materials();if(held.size()!=expected.size()||held.stream().anyMatch(h->!Objects.equals(expected.get(h.commodity()),h.units())))throw new SQLException("Construction receipt differs from frozen material costs");
                return Optional.of(new Receipt(operation,draft.id(),business,r.getLong("reserved_ms"),held));
            }
        }
    }
    public Map<Commodity,Long> available(UUID operation)throws SQLException {
        var receipt=find(operation).orElseThrow(()->new SQLException("Unknown construction reservation"));
        if(new ConstructionMaterialReturns(connection).returned(operation))return Map.of();
        var stock=new TreeMap<Commodity,Long>();receipt.materials().forEach(i->stock.put(i.commodity(),i.units()));return Collections.unmodifiableMap(stock);
    }
}
