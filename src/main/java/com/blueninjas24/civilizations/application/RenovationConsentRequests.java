package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.construction.BuildingLayout;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.database.BuildingRenovations.*;
import java.sql.*;
import java.util.*;

/** Separate expiring owner consent; never inferred from donation or a client-supplied layout at confirmation. */
public final class RenovationConsentRequests {
    public record Context(UUID player,UUID world,boolean permitted,boolean administrator,Set<UUID> loadedWorlds){
        public Context{Objects.requireNonNull(player);Objects.requireNonNull(world);loadedWorlds=loadedWorlds==null?Set.of():Set.copyOf(loadedWorlds);}
    }
    public record Preview(UUID token,UUID player,UUID world,UUID donor,BuildingLayout layout,
                          Request terms,long createdMillis,long expiresMillis){}
    private static final class Pending {
        final Preview preview;Request submitted;Authority authority;Receipt saved;
        Pending(Preview preview){this.preview=preview;}
    }
    private final BuildingRegistry buildings;private final BuildingRenovations renovations;
    private final Map<UUID,Pending> pending=new HashMap<>();
    public RenovationConsentRequests(Connection c){buildings=new BuildingRegistry(c);renovations=new BuildingRenovations(c);}
    public Preview preview(Context context,UUID building,Action action,long now)throws SQLException{
        prune(now);var entry=buildings.find(building).orElseThrow(()->new BuildingRegistry.Declined("Unknown donated building"));
        require(context,entry.layout().world(),entry.donor(),action);
        if(!pending.containsKey(context.player())&&pending.size()>=64)throw new BuildingRegistry.Declined("Renovation reviews busy; retry shortly");
        if(entry.latest()==null||action==Action.GRANT&&!buildings.hasDonationPermission(building))throw new BuildingRegistry.Declined("An active permitted donation is required to allow renovations");
        boolean effective=renovations.hasPermission(building,context.world());
        boolean recorded=renovations.latest(building).map(r->r.request().action()==Action.GRANT).orElse(false);
        if(action==Action.GRANT&&effective||action==Action.REVOKE&&!recorded)
            throw new BuildingRegistry.Declined(effective?"Renovations are already separately allowed":"No separate renovation grant remains to withdraw");
        UUID token=UUID.randomUUID();var terms=new Request(token,building,entry.latest().operation(),
                renovations.latest(building).map(r->r.request().operation()).orElse(null),entry.layout().fingerprint(),action,now);
        var preview=new Preview(token,context.player(),context.world(),entry.donor(),entry.layout(),terms,now,Math.addExact(now,60_000));
        pending.put(context.player(),new Pending(preview));return preview;
    }
    public Receipt confirm(Context context,UUID token,long now)throws SQLException{
        prune(now);var p=pending.get(context.player());
        if(p==null||!p.preview.token().equals(token))throw new BuildingRegistry.Declined("No matching renovation preview; review again");
        var preview=p.preview;require(context,preview.world(),preview.donor(),preview.terms().action());
        if(now<preview.createdMillis())throw new BuildingRegistry.Declined("Renovation clock moved backward; review again");
        if(p.saved!=null)return p.saved;
        if(p.submitted==null){var t=preview.terms();p.submitted=new Request(t.operation(),t.building(),t.useConsent(),t.previous(),t.layoutHash(),t.action(),now);
            p.authority=new Authority(context.player(),context.world(),true,true,context.administrator(),true);}
        try{return p.saved=renovations.confirm(p.submitted,p.authority);}
        catch(BuildingRegistry.Declined refused){p.submitted=null;p.authority=null;throw refused;}
    }
    public void cancel(UUID actor){pending.remove(actor);}
    private void prune(long now){if(now<0)throw new IllegalArgumentException("Negative renovation time");pending.values().removeIf(p->now>=p.preview.expiresMillis());}
    private static void require(Context c,UUID world,UUID donor,Action action)throws BuildingRegistry.Declined{
        if(!c.world().equals(world)||!c.loadedWorlds().contains(world))throw new BuildingRegistry.Declined("Return to the building's present world");
        if(action==Action.GRANT?(!c.permitted()||!c.player().equals(donor)):(!c.permitted()&&!c.administrator()||!c.player().equals(donor)&&!c.administrator()))
            throw new BuildingRegistry.Declined("Only the recorded donor may allow renovations; an administrator may deny them");
    }
}
