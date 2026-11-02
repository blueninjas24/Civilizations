package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Atomic trusted-owner publication of explicit donor use and a fixed existing business's premises hold.
 * Authentication/expiring confirmation belong to the adapter; this never moves goods, title or blocks. */
public final class WorkplaceDonation {
    public record Request(UUID operation,UUID business,BuildingLayout layout,UUID donor,UUID previousConsent,long atMillis){
        public Request{Objects.requireNonNull(operation);Objects.requireNonNull(business);Objects.requireNonNull(layout);Objects.requireNonNull(donor);
            if(layout.kind()!=BuildingLayout.Kind.WORKPLACE||atMillis<0)throw new IllegalArgumentException("Workplace donation needs a typed layout and valid time");}
    }
    public record Receipt(BuildingRegistry.Consent consent,BusinessPremises.Hold premises){}
    private final Connection c;
    public WorkplaceDonation(Connection c){this.c=Objects.requireNonNull(c);}
    static UUID holdOperation(UUID donation){return UUID.nameUUIDFromBytes(("Civilizations:donated-workplace-v1:"+donation).getBytes(StandardCharsets.UTF_8));}
    private static BusinessPremises.Request hold(Request request){return new BusinessPremises.Request(holdOperation(request.operation()),request.business(),request.layout().id(),request.operation(),request.layout().fingerprint(),request.atMillis());}
    /** A replay is history only, including a subsequently released hold; no freshness/permission is renewed. */
    public Receipt confirm(Request request,BuildingRegistry.Authorization authority,WorkplaceInspection.Assessment assessment,Set<UUID> worlds,long now)throws SQLException{
        Objects.requireNonNull(request);Objects.requireNonNull(authority);worlds=Set.copyOf(worlds);
        if(now<request.atMillis())throw new IllegalArgumentException("Donation confirmation is in the future");
        if(!c.getAutoCommit())throw new SQLException("Workplace donation requires idle SQL owner");c.setAutoCommit(false);
        try{
            var registry=new BuildingRegistry(c);var premises=new BusinessPremises(c);var expected=hold(request);
            if(registry.consentReceipt(request.operation()).isPresent()){
                var consent=registry.donateRows(request.operation(),request.layout(),request.donor(),authority,request.previousConsent(),request.atMillis());
                var saved=premises.find(expected.operation()).orElseThrow(()->new SQLException("Donation receipt lacks its atomic premises receipt"));
                if(!saved.request().equals(expected))throw new BuildingRegistry.Declined("Workplace donation retry changed business or premises terms");
                c.commit();return new Receipt(consent,saved);
            }
            if(!worlds.contains(request.layout().world())||now-request.atMillis()>5000)throw new BuildingRegistry.Declined("Fresh workplace confirmation in its present world required");
            Objects.requireNonNull(assessment);var evidence=assessment.evidence();
            if(!assessment.suitable()||evidence.kind().isEmpty())throw new BuildingRegistry.Declined("Suitable current workplace inspection required");
            var consent=registry.donateRows(request.operation(),request.layout(),request.donor(),authority,request.previousConsent(),request.atMillis());
            // Re-donation can retire only this same business/building's already withdrawn use epoch.
            // The new consent and replacement hold still commit together, including this retained release.
            if(request.previousConsent()!=null){
                UUID prior=null;
                try(var s=c.prepareStatement("SELECT operation_id FROM business_premises WHERE business_id=? AND building_id=? AND released_ms IS NULL")){
                    s.setString(1,request.business().toString());s.setString(2,request.layout().id().toString());try(var r=s.executeQuery()){if(r.next())prior=UUID.fromString(r.getString(1));}
                }
                if(prior!=null)premises.releaseRows(UUID.nameUUIDFromBytes(("Civilizations:workplace-redonation-release-v1:"+request.operation()).getBytes(StandardCharsets.UTF_8)),prior,now);
            }
            var at=evidence.marker();var anchor=new WorkplaceOperations.Anchor(request.business(),request.layout().world(),new VillageObservation.Position(at.x(),at.y(),at.z()),evidence.kind().orElseThrow());
            new WorkplaceOperations(c).confirmRows(List.of(new WorkplaceOperations.Confirmation(anchor,true,evidence.structure().capturedMillis())),now,worlds);
            var saved=premises.reserveInspectedRows(expected,assessment,worlds,now);
            c.commit();return new Receipt(consent,saved);
        }catch(SQLException|RuntimeException failure){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}throw failure;}finally{c.setAutoCommit(true);}
    }
}
