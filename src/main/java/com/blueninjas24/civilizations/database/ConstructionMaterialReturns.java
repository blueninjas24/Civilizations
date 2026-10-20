package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.economy.Commodity;
import java.sql.*;
import java.util.*;

/** Return all unused onsite goods to their source, or retain the whole stockpile until space exists. */
public final class ConstructionMaterialReturns {
    public record Returned(Commodity commodity,long units,long beforeUnits,long afterUnits){}
    public record Receipt(UUID operation,UUID reservation,UUID business,long returnedMillis,List<Returned> items) {
        public Receipt{items=List.copyOf(items);}
    }
    public record Attempt(Optional<Receipt> receipt,String explanation){}
    private final Connection c;
    public ConstructionMaterialReturns(Connection connection){c=Objects.requireNonNull(connection);}
    static void createSchema(Connection c)throws SQLException {
        try(var s=c.createStatement()){s.execute("""
            CREATE TABLE construction_material_returns (
                return_operation TEXT NOT NULL,
                reservation_operation TEXT NOT NULL,
                commodity TEXT NOT NULL,
                units INTEGER NOT NULL CHECK(typeof(units)='integer' AND units>0),
                before_units INTEGER NOT NULL CHECK(typeof(before_units)='integer' AND before_units>=0),
                after_units INTEGER NOT NULL CHECK(typeof(after_units)='integer' AND after_units>=before_units AND after_units-before_units=units),
                returned_ms INTEGER NOT NULL CHECK(typeof(returned_ms)='integer' AND returned_ms>=0),
                PRIMARY KEY(reservation_operation,commodity),UNIQUE(return_operation,commodity),
                FOREIGN KEY(reservation_operation,commodity) REFERENCES construction_material_items(operation_id,commodity)
            )
            """);}
    }
    public Attempt returnUnused(UUID operation,UUID reservation,long now)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(reservation);if(now<0)throw new IllegalArgumentException("Negative material return time");
        if(!c.getAutoCommit())throw new SQLException("Material return requires idle SQL owner");c.setAutoCommit(false);
        try {
            var old=find(operation);if(old.isPresent()) {
                if(!old.orElseThrow().reservation().equals(reservation))throw new SQLException("Material return identity reused for another reservation");
                c.commit();c.setAutoCommit(true);return new Attempt(old,"Saved exact return; nothing transferred again");
            }
            var held=new ConstructionMaterials(c).find(reservation).orElseThrow(()->new SQLException("Unknown construction material reservation"));
            if(now<held.reservedMillis())throw new SQLException("Return precedes material reservation");
            if(returned(reservation))throw new SQLException("Construction materials were already returned under another operation");
            try(var s=c.prepareStatement("SELECT 1 FROM construction_block_journal WHERE draft_id=? LIMIT 1")) {
                s.setString(1,held.draft().toString());try(var r=s.executeQuery()){if(r.next())throw new SQLException("Prepared or started construction cannot return goods without reviewed physical recovery");}
            }
            var store=new IndustryRepository(c).store(held.business()).orElseThrow(()->new SQLException("Original construction source store missing"));
            long space=0;var items=new ArrayList<Returned>();
            for(var item:held.materials()) {
                Long size=store.spacePerUnit().get(item.commodity());if(size==null)throw new SQLException("Original source no longer accepts a held material");
                space=Math.addExact(space,Math.multiplyExact(item.units(),size));long before=store.units().get(item.commodity());
                items.add(new Returned(item.commodity(),item.units(),before,Math.addExact(before,item.units())));
            }
            if(store.freeSpace()<space) {
                c.commit();c.setAutoCommit(true);return new Attempt(Optional.empty(),"Return waits for source storage space; all onsite goods remain saved");
            }
            for(var item:items) {
                try(var s=c.prepareStatement("UPDATE industry_stock SET units=? WHERE business_id=? AND commodity=? AND units=?")) {
                    s.setLong(1,item.afterUnits());s.setString(2,held.business().toString());s.setString(3,item.commodity().id());s.setLong(4,item.beforeUnits());if(s.executeUpdate()!=1)throw new SQLException("Return source stock changed");
                }
                try(var s=c.prepareStatement("INSERT INTO construction_material_returns VALUES(?,?,?,?,?,?,?)")) {
                    s.setString(1,operation.toString());s.setString(2,reservation.toString());s.setString(3,item.commodity().id());s.setLong(4,item.units());s.setLong(5,item.beforeUnits());s.setLong(6,item.afterUnits());s.setLong(7,now);s.executeUpdate();
                }
            }
            var receipt=new Receipt(operation,reservation,held.business(),now,items);c.commit();c.setAutoCommit(true);return new Attempt(Optional.of(receipt),"Unused goods returned once; original reservation and before/after history retained");
        }catch(SQLException|RuntimeException failure){try{c.rollback();c.setAutoCommit(true);}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    public boolean returned(UUID reservation)throws SQLException {
        try(var s=c.prepareStatement("SELECT return_operation FROM construction_material_returns WHERE reservation_operation=? LIMIT 1")) {
            s.setString(1,reservation.toString());try(var r=s.executeQuery()){return r.next()&&find(UUID.fromString(r.getString(1))).orElseThrow().reservation().equals(reservation);}
        }
    }
    public Optional<Receipt> find(UUID operation)throws SQLException {
        try(var s=c.prepareStatement("SELECT * FROM construction_material_returns WHERE return_operation=? ORDER BY commodity")) {
            s.setString(1,operation.toString());try(var r=s.executeQuery()) {
                UUID reservation=null;long returned=-1;var items=new ArrayList<Returned>();
                while(r.next()) {
                    if(items.size()==512)throw new SQLException("Material return exceeds frozen commodity budget");
                    UUID source=UUID.fromString(r.getString("reservation_operation"));long time=r.getLong("returned_ms");
                    if(reservation==null){reservation=source;returned=time;}
                    else if(!reservation.equals(source)||returned!=time)throw new SQLException("Material return receipt mixes reservation identities/times");
                    items.add(new Returned(new Commodity(r.getString("commodity")),r.getLong("units"),r.getLong("before_units"),r.getLong("after_units")));
                }
                if(reservation==null)return Optional.empty();
                var held=new ConstructionMaterials(c).find(reservation).orElseThrow(()->new SQLException("Return lost its original reservation"));
                var expected=new TreeMap<Commodity,Long>();held.materials().forEach(i->expected.put(i.commodity(),i.units()));
                if(returned<held.reservedMillis()||items.size()!=expected.size()||items.stream().anyMatch(i->!Objects.equals(expected.get(i.commodity()),i.units())))throw new SQLException("Return differs from the full original material reservation");
                return Optional.of(new Receipt(operation,reservation,held.business(),returned,items));
            }
        }catch(IllegalArgumentException invalid){throw new SQLException("Invalid saved construction material return",invalid);}
    }
}
