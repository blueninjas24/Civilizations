package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.ApartmentDeclaration.Draft;
import com.blueninjas24.civilizations.construction.BuildingLayout.Bounds;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.function.*;

/** Main-thread, bounded, unsaved editor for NEW buildings only. No world reads or existing-layout mutation. */
final class PaperApartmentDrafts {
    static final Set<String> ACTIONS=Set.of("apartments","unit","room","removeunit","shared","clearshared",
            "apartmentplot","apartmentstatus","reviewapartments","cancelapartments");
    static final int MAX_DRAFTS=4;
    static final long IDLE_MILLIS=600_000;
    private record Editing(Draft draft,long touched) {}
    private final Map<UUID,Editing> drafts=new HashMap<>();
    private final Function<Player,Bounds> selections;
    private final BiConsumer<Player,Draft> submit;
    private final LongSupplier clock;
    PaperApartmentDrafts(Function<Player,Bounds> selections,BiConsumer<Player,Draft> submit,LongSupplier clock){
        this.selections=selections;this.submit=submit;this.clock=clock;
    }
    void prune(Function<UUID,Player> players) {
        long now=clock.getAsLong();drafts.entrySet().removeIf(e->{var p=players.apply(e.getKey());var edit=e.getValue();
            return p==null||!p.hasPermission(PaperHomeDonation.PERMISSION)||!p.getWorld().getUID().equals(edit.draft.world())
                    ||now<edit.touched||now-edit.touched>=IDLE_MILLIS;});
    }
    void remove(UUID actor){drafts.remove(actor);}
    void clear(){drafts.clear();}
    void command(Player player,String[] args) {
        var actor=player.getUniqueId();String action=args[1].toLowerCase(Locale.ROOT);long now=clock.getAsLong();
        if(action.equals("cancelapartments")&&args.length==2){remove(actor);player.sendMessage("Unsaved apartment draft cancelled. Submitted reviews use cancelhome.");return;}
        var old=drafts.get(actor);
        if(action.equals("apartments")&&args.length==3){
            if(old!=null)throw new IllegalArgumentException("An apartment draft is open; cancelapartments first");
            if(drafts.size()>=MAX_DRAFTS)throw new IllegalArgumentException("Apartment editors busy; retry shortly");
            var structure=selections.apply(player);var entry=position(player);
            var draft=new Draft(UUID.randomUUID(),UUID.fromString(args[2]),player.getWorld().getUID(),structure,structure,entry,List.of(),List.of());
            drafts.put(actor,new Editing(draft,now));
            player.sendMessage("Apartment structure frozen. Select each private unit's floor-to-roof corners, stand at its entrance, then /civ building unit <name>. Mark shared corridors/stairs separately. Nothing donated.");return;
        }
        if(old==null)throw new IllegalArgumentException("Select the entire building and stand at its entrance, then /civ building apartments <village UUID>");
        var draft=old.draft;Draft next;
        switch(action){
            case "unit"->{requireLength(args,3);next=draft.unit(args[2],position(player),selections.apply(player));}
            case "room"->{requireLength(args,3);next=draft.room(args[2],selections.apply(player));}
            case "removeunit"->{requireLength(args,3);next=draft.remove(args[2]);}
            case "shared"->{requireLength(args,2);next=draft.shared(selections.apply(player));}
            case "clearshared"->{requireLength(args,2);next=new Draft(draft.building(),draft.village(),draft.world(),draft.plot(),draft.structure(),draft.entrance(),List.of(),draft.units());}
            case "apartmentplot"->{requireLength(args,2);next=draft.plot(selections.apply(player));}
            case "reviewapartments"->{requireLength(args,2);submit.accept(player,draft);remove(actor);return;}
            case "apartmentstatus"->{requireLength(args,2);next=draft;}
            default->throw new IllegalArgumentException("Unknown apartment editor command");
        }
        drafts.put(actor,new Editing(next,now));
        player.sendMessage("Unsaved apartment draft: "+next.units().size()+" units, "+next.shared().size()+" shared boxes. Structure "+next.structure()+"; plot "+next.plot());
        for(var u:next.units().stream().limit(8).toList())player.sendMessage("Unit "+u.name()+": "+u.spaces().size()+" private boxes; entrance "+u.entrance());
        if(next.units().size()>8)player.sendMessage("Plus "+(next.units().size()-8)+" units. Review checks every declared unit.");
    }
    private static void requireLength(String[] args,int count){if(args.length!=count)throw new IllegalArgumentException("Wrong apartment editor arguments");}
    private static Position position(Player p){var l=p.getLocation();return new Position(l.getBlockX(),l.getBlockY(),l.getBlockZ());}
}
