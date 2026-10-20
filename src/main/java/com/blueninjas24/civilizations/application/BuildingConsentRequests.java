package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.construction.BuildingLayout;
import com.blueninjas24.civilizations.database.BuildingRegistry;
import com.blueninjas24.civilizations.database.BuildingRegistry.*;
import java.sql.SQLException;
import java.util.*;

/** Bounded SQL-owner preview/confirmation workflow. Paper supplies authenticated current context.
 * No client layout, actor or ownership assertion can replace the stored preview at confirmation. */
public final class BuildingConsentRequests {
    public record Policy(int maximumRequests,long previewMillis) {
        public Policy {
            if(maximumRequests<1 || maximumRequests>128 || previewMillis<1000 || previewMillis>300_000)
                throw new IllegalArgumentException("Invalid building confirmation limits");
        }
        public static Policy prototype(){return new Policy(64,60_000);}
    }
    public record Context(UUID player,UUID world,boolean mayDonate,boolean mayReclaim,boolean administrator,Set<UUID> loadedWorlds) {
        public Context {Objects.requireNonNull(player);Objects.requireNonNull(world);loadedWorlds=loadedWorlds==null?Set.of():Set.copyOf(loadedWorlds);}
    }
    public record Preview(UUID token,UUID player,UUID world,UUID building,Action action,BuildingLayout layout,
                          UUID donor,UUID previous,long createdMillis,long expiresMillis,String explanation) {}
    private static final class Pending {
        final Preview preview;Consent saved;Long submittedMillis;Authorization submittedAuthority;
        Pending(Preview preview){this.preview=preview;}
    }
    private final BuildingRegistry registry;
    private final Policy policy;
    private final Map<UUID,Pending> pending=new HashMap<>();
    public BuildingConsentRequests(BuildingRegistry registry,Policy policy) {
        this.registry=Objects.requireNonNull(registry);this.policy=Objects.requireNonNull(policy);
    }
    public Preview donation(Context context,BuildingLayout layout,long now)throws SQLException {
        Objects.requireNonNull(layout);requireContext(context,layout.world(),Action.DONATE);room(context.player(),now);
        var old=registry.reviewDonation(layout,context.player());
        UUID previous=old.map(Entry::latest).map(Consent::operation).orElse(null);
        var preview=new Preview(UUID.randomUUID(),context.player(),layout.world(),layout.id(),Action.DONATE,layout,
                context.player(),previous,now,Math.addExact(now,policy.previewMillis()),
                "Confirming declares that this building belongs to you and grants village use. It does not transfer title, move goods or activate housing.");
        pending.put(context.player(),new Pending(preview));return preview;
    }
    public Preview reclamation(Context context,UUID building,long now)throws SQLException {
        var entry=registry.find(building).orElseThrow(()->new Declined("Unknown building"));
        requireContext(context,entry.layout().world(),Action.RECLAIM);room(context.player(),now);
        if(!context.player().equals(entry.donor()) && !context.administrator())throw new Declined("Only the recorded donor or an administrator can reclaim");
        if(entry.latest()==null || entry.latest().action()!=Action.DONATE)throw new Declined("Building has no active donation to reclaim");
        var preview=new Preview(UUID.randomUUID(),context.player(),entry.layout().world(),building,Action.RECLAIM,entry.layout(),
                entry.donor(),entry.latest().operation(),now,Math.addExact(now,policy.previewMillis()),
                "Withdraw village use of this building; retain physical structure and history. Live household displacement must be connected before exposing this control.");
        pending.put(context.player(),new Pending(preview));return preview;
    }
    /** Ownership declaration is an explicit second step under the user-approved player-trust policy. */
    public Consent confirm(Context context,UUID token,boolean declaresOwnership,long now)throws SQLException {
        prune(now);var request=pending.get(context.player());
        if(request==null || !request.preview.token().equals(token))throw new Declined("No matching building preview; review again");
        var preview=request.preview;
        requireContext(context,preview.world(),preview.action());
        if(now<preview.createdMillis())throw new Declined("Building confirmation clock moved backward; review again");
        if(preview.action()==Action.DONATE && !declaresOwnership)throw new Declined("Explicit ownership declaration is required to donate");
        if(preview.action()==Action.RECLAIM && !context.player().equals(preview.donor()) && !context.administrator())
            throw new Declined("Administrator permission changed; review again");
        if(request.saved!=null)return request.saved;
        if(request.submittedMillis==null) {
            request.submittedMillis=now;
            request.submittedAuthority=preview.action()==Action.DONATE?Authorization.playerDeclaration(preview.player(),true)
                    :new Authorization(context.player(),context.administrator(),false,true);
        }
        long submitted=request.submittedMillis;
        try {
            request.saved=preview.action()==Action.DONATE
                    ?registry.donate(preview.token(),preview.layout(),preview.player(),request.submittedAuthority,preview.previous(),submitted)
                    :registry.reclaim(preview.token(),preview.building(),request.submittedAuthority,preview.previous(),submitted);
        }catch(Declined conflict){request.submittedMillis=null;request.submittedAuthority=null;throw conflict;}
        return request.saved;
    }
    public void cancel(UUID player){pending.remove(player);}
    private void room(UUID player,long now)throws Declined {
        prune(now);
        if(!pending.containsKey(player) && pending.size()>=policy.maximumRequests())throw new Declined("Building review is busy; retry shortly");
    }
    private void prune(long now) {
        if(now<0)throw new IllegalArgumentException("Negative building confirmation time");
        pending.values().removeIf(p->now>=p.preview.expiresMillis());
    }
    private static void requireContext(Context context,UUID world,Action action)throws Declined {
        Objects.requireNonNull(context);
        if(!context.world().equals(world) || !context.loadedWorlds().contains(world))throw new Declined("Return to this building's present world before confirming");
        if(action==Action.DONATE?!context.mayDonate():!context.mayReclaim()&&!context.administrator())
            throw new Declined("Building permission is unavailable");
    }
}
