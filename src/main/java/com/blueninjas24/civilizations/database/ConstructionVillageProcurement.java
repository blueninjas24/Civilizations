package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.Source;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Complete remaining public-project inputs or spend nothing. No native writes or new clock. */
public final class ConstructionVillageProcurement {
    public record Result(List<ConstructionSupplyOrders.Receipt> purchases,String explanation){
        public Result{purchases=List.copyOf(purchases);}
    }
    private final Connection c;
    private final ConstructionSupplyOrders orders;
    public ConstructionVillageProcurement(Connection c,ConstructionBudgetPolicy budget,ConstructionSupplyPolicy supply,
                                           IndustryRuntimePolicy industry,BusinessOperationPolicy operations){
        this.c=Objects.requireNonNull(c);orders=new ConstructionSupplyOrders(c,budget,supply,industry,operations);
    }

    /** Trusted adapter must supply fresh complete authority; a dry preview never grants permission. */
    public Result purchaseRemaining(UUID draftId,ConstructionMaterials.Authority authority,long now,Set<UUID> worlds)throws SQLException{
        Objects.requireNonNull(draftId);Objects.requireNonNull(authority);var present=Set.copyOf(worlds);
        if(now<0)throw new IllegalArgumentException("Negative procurement time");
        if(!c.getAutoCommit())throw new SQLException("Village procurement requires idle SQL owner");c.setAutoCommit(false);
        try{
            var funding=orders.quoteVillageMaterialsRows(draftId,now,present);
            if(funding.materials().stream().noneMatch(m->m.missing()>0)){
                c.commit();return new Result(List.of(),"All frozen inputs already paid; no duplicate purchases");
            }
            if(!funding.canOrderAllNow())throw new ConstructionSupplyOrders.Declined(funding.explanation());
            var draft=new ConstructionDraftRepository(c).find(draftId).orElseThrow();
            var suppliers=new ArrayList<UUID>();
            try(var s=c.prepareStatement("""
                SELECT b.id FROM businesses b JOIN business_sites p ON p.id=b.site_id
                JOIN industry_stores i ON i.business_id=b.id
                WHERE b.settlement_id=? AND p.world_id=?
                AND EXISTS(SELECT 1 FROM industry_stock k WHERE k.business_id=b.id AND k.units>0)
                ORDER BY b.id LIMIT 129
                """)){
                s.setString(1,draft.settlement().toString());s.setString(2,draft.world().toString());
                try(var r=s.executeQuery()){while(r.next())suppliers.add(UUID.fromString(r.getString(1)));}
            }
            if(suppliers.size()>128)throw new ConstructionSupplyOrders.Declined("Local supplier review budget exceeded; nothing purchased");
            var stores=new LinkedHashMap<UUID,Map<Commodity,Long>>();
            for(var supplier:suppliers)if(orders.staffed(supplier))
                stores.put(supplier,new IndustryRepository(c).store(supplier).orElseThrow().units());
            var requests=new ArrayList<ConstructionSupplyOrders.Request>();
            for(var material:funding.materials().stream().sorted(Comparator.comparing(ConstructionSupplyOrders.MaterialNeed::commodity)).toList()){
                long remaining=material.missing();
                for(var supplier:stores.entrySet()){
                    long units=Math.min(remaining,supplier.getValue().getOrDefault(material.commodity(),0L));
                    if(units==0)continue;
                    // Frozen plan + commodity + supplier + already-paid quantity defines this remaining batch.
                    String identity="construction-public-inputs-v1|"+draftId+"|"+material.commodity().id()+"|"+supplier.getKey()+"|"+material.ordered();
                    UUID operation=UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
                    requests.add(new ConstructionSupplyOrders.Request(operation,draftId,supplier.getKey(),Source.VILLAGE,null,material.commodity(),units,funding.period()));
                    remaining-=units;if(remaining==0)break;
                }
                if(remaining>0)throw new ConstructionSupplyOrders.Declined("Qualified local suppliers lack "+remaining+" "+material.commodity().id()+"; nothing purchased");
            }
            var receipts=new ArrayList<ConstructionSupplyOrders.Receipt>();
            for(var request:requests)receipts.add(orders.purchaseRows(request,authority,now,present));
            c.commit();return new Result(receipts,"Remaining frozen inputs purchased from real local stock; delivery still requires earned owner time");
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}
        finally{c.setAutoCommit(true);}
    }
}
