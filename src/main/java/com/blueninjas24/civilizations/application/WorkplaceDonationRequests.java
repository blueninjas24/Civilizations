package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.*;
import java.sql.*;
import java.util.*;

/** SQL-owner, player-bound frozen previews. Confirmation requires a new physical survey, never client-edited terms. */
public final class WorkplaceDonationRequests {
    public record Preview(UUID token,UUID player,UUID business,BuildingLayout layout,UUID previousConsent,
                          long createdMillis,long expiresMillis,WorkplaceInspection.Assessment inspection){}
    private static final class Pending{
        final Preview preview;WorkplaceDonation.Request submitted;
        Pending(Preview preview){this.preview=preview;}
    }
    private final Connection c;private final BuildingRegistry registry;private final BuildingConsentRequests.Policy policy;
    private final Map<UUID,Pending> pending=new HashMap<>();
    public WorkplaceDonationRequests(Connection c,BuildingConsentRequests.Policy policy){this.c=Objects.requireNonNull(c);this.registry=new BuildingRegistry(c);this.policy=Objects.requireNonNull(policy);}
    public Preview preview(BuildingConsentRequests.Context context,UUID business,BuildingLayout layout,WorkplaceInspection.Evidence evidence,
                           WorkplaceInspection.Policy inspectionPolicy,long now)throws SQLException{
        prune(now);require(context,layout.world());
        if(!pending.containsKey(context.player())&&pending.size()>=policy.maximumRequests())throw new BuildingRegistry.Declined("Workplace donation reviews busy; retry shortly");
        var assessment=WorkplaceInspection.inspect(layout,evidence,now,5000,inspectionPolicy);
        if(!assessment.suitable())throw new BuildingRegistry.Declined("Workplace is not suitable: "+assessment.issues());
        var firm=new BusinessRepository(c).findFounding(business).orElseThrow(()->new BuildingRegistry.Declined("Unknown workplace business"));var site=firm.site();
        if(!site.worldId().equals(layout.world())||!site.settlementId().equals(layout.settlement())||!evidence.marker().equals(new Position(site.x(),site.y(),site.z()))
           ||!evidence.kind().orElseThrow().industry().equals(firm.business().industry()))throw new BuildingRegistry.Declined("Workplace declaration differs from the saved fixed business");
        var old=registry.reviewDonation(layout,context.player());
        var preview=new Preview(UUID.randomUUID(),context.player(),business,layout,old.map(BuildingRegistry.Entry::latest).map(BuildingRegistry.Consent::operation).orElse(null),
                now,Math.addExact(now,policy.previewMillis()),assessment);
        pending.put(context.player(),new Pending(preview));return preview;
    }
    public WorkplaceDonation.Receipt confirm(BuildingConsentRequests.Context context,UUID token,boolean declaresOwnership,WorkplaceInspection.Evidence fresh,long now)throws SQLException{
        prune(now);var request=pending.get(context.player());
        if(request==null||!request.preview.token().equals(token))throw new BuildingRegistry.Declined("No matching current workplace donation preview");
        var preview=request.preview;require(context,preview.layout().world());
        if(now<preview.createdMillis())throw new BuildingRegistry.Declined("Confirmation clock moved backward; review again");
        if(!declaresOwnership)throw new BuildingRegistry.Declined("Explicit building ownership declaration required");
        boolean recorded=registry.consentReceipt(token).isPresent();WorkplaceInspection.Assessment assessment=null;
        if(!recorded){
            if(fresh==null)throw new BuildingRegistry.Declined("New workplace survey required at confirmation");
            if(fresh.structure().capturedMillis()<preview.createdMillis())throw new BuildingRegistry.Declined("Confirmation survey must follow the frozen preview");
            assessment=WorkplaceInspection.inspect(preview.layout(),fresh,now,5000,preview.inspection().policy());
            // A rolled-back attempt has no accepted timestamp; keep the same operation/layout but use fresh terms.
            request.submitted=new WorkplaceDonation.Request(token,preview.business(),preview.layout(),preview.player(),preview.previousConsent(),now);
        }
        if(request.submitted==null)throw new SQLException("Recorded workplace donation lost its submitted confirmation terms");
        return new WorkplaceDonation(c).confirm(request.submitted,BuildingRegistry.Authorization.playerDeclaration(context.player(),true),assessment,context.loadedWorlds(),now);
    }
    public void cancel(UUID player){pending.remove(player);}
    private void prune(long now){if(now<0)throw new IllegalArgumentException("Negative workplace preview time");pending.values().removeIf(p->now>=p.preview.expiresMillis());}
    private static void require(BuildingConsentRequests.Context context,UUID world)throws BuildingRegistry.Declined{
        if(!context.mayDonate()||!context.world().equals(world)||!context.loadedWorlds().contains(world))throw new BuildingRegistry.Declined("Return to the present workplace world with donation permission");
    }
}
