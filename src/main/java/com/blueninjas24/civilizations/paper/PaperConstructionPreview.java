package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.ConstructionPreview;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Rotation;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import java.util.*;

/** Read-only, bounded main-thread inspection. There is deliberately no placement or approval command. */
public final class PaperConstructionPreview extends BukkitRunnable implements CommandExecutor,TabCompleter {
    public static final String PERMISSION="civilizations.construction.preview";
    private static final int CELLS_PER_TICK=64,MAX_REQUESTS=8;
    private static final Set<Material> SUPPORT=Set.of(Material.STONE,Material.COBBLESTONE,Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE,Material.DIRT,Material.GRASS_BLOCK,Material.DIRT_PATH,Material.COARSE_DIRT,
            Material.ROOTED_DIRT,Material.ANDESITE,Material.DIORITE,Material.GRANITE,Material.SANDSTONE,Material.RED_SANDSTONE,Material.BEDROCK);
    private final PaperConstructionLibrary library;
    private final LinkedHashMap<UUID,Request> pending=new LinkedHashMap<>();
    private static final class Request {
        final UUID player,world;final ConstructionTemplate template;final Position origin;final long started;
        final ArrayDeque<Position> remaining;final Map<Position,Cell> cells=new HashMap<>();boolean incomplete;
        Request(Player player,ConstructionTemplate template,Position origin) {
            this.player=player.getUniqueId();world=player.getWorld().getUID();this.template=template;this.origin=origin;
            started=System.currentTimeMillis();remaining=new ArrayDeque<>(ConstructionPreview.requiredCells(template,Transform.identity(),origin));
        }
    }
    public PaperConstructionPreview(PaperConstructionLibrary library){this.library=Objects.requireNonNull(library);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!sender.hasPermission(PERMISSION)){sender.sendMessage("You do not have permission to preview construction.");return true;}
        if(args.length==2&&args[1].equalsIgnoreCase("templates")) {
            for(var template:library.templates().values())sender.sendMessage(template.id()+": "+template.name()+"; "+template.size().x()+"x"+template.size().y()+"x"+template.size().z()+"; "+template.stages().size()+" stages; materials "+template.materials());
            return true;
        }
        if(!(sender instanceof Player player)){sender.sendMessage("Use /civ build templates; site previews require a player.");return true;}
        if(args.length<3||args.length>5||!args[1].equalsIgnoreCase("preview")){usage(sender);return true;}
        var template=library.templates().get(args[2]);if(template==null){sender.sendMessage("Unknown template. Use /civ build templates.");return true;}
        int rotation=0;
        try{if(args.length>=4)rotation=Integer.parseInt(args[3]);}catch(NumberFormatException invalid){usage(sender);return true;}
        if(!Set.of(0,90,180,270).contains(rotation)||args.length==5&&!args[4].equalsIgnoreCase("mirror")){usage(sender);return true;}
        if(pending.containsKey(player.getUniqueId())){sender.sendMessage("Your current site preview is still being checked.");return true;}
        if(pending.size()>=MAX_REQUESTS){sender.sendMessage("Construction preview queue is full; try again shortly.");return true;}
        try {
            template=PaperConstructionLibrary.prepare(template,new Transform(Rotation.values()[rotation/90],args.length==5));
            var location=player.getLocation();var forward=location.getDirection().setY(0);
            if(forward.lengthSquared()<0.01)forward=new org.bukkit.util.Vector(0,0,1);
            location.add(forward.normalize().multiply(8));
            var origin=new Position(location.getBlockX()-template.size().x()/2,player.getLocation().getBlockY(),location.getBlockZ()-template.size().z()/2);
            pending.put(player.getUniqueId(),new Request(player,template,origin));
            sender.sendMessage("Checking a site eight blocks ahead. Preview only; no blocks, items or money will change.");
        }catch(IllegalArgumentException|ArithmeticException invalid){sender.sendMessage("Cannot preview this plan: "+invalid.getMessage());}
        return true;
    }
    @Override public void run() {
        // Global budget, round robin across requests; no per-player multiplication of the block-read budget.
        int budget=CELLS_PER_TICK;
        for(var iterator=pending.values().iterator();iterator.hasNext();) {
            var request=iterator.next();var player=Bukkit.getPlayer(request.player);var world=Bukkit.getWorld(request.world);
            if(player==null||!player.getWorld().getUID().equals(request.world)||!player.hasPermission(PERMISSION)||world==null){iterator.remove();continue;}
            if(System.currentTimeMillis()-request.started>30_000){player.sendMessage("Site preview expired; try again.");iterator.remove();continue;}
            int allowance=Math.min(budget,Math.max(1,CELLS_PER_TICK/pending.size()));
            while(allowance-->0&&!request.remaining.isEmpty()) {
                budget--;var p=request.remaining.removeFirst();
                if(p.y()<world.getMinHeight()||p.y()>=world.getMaxHeight()||!world.isChunkLoaded(p.x()>>4,p.z()>>4)){request.incomplete=true;continue;}
                var block=world.getBlockAt(p.x(),p.y(),p.z());
                request.cells.put(p,new Cell(block.getType().isAir(),SUPPORT.contains(block.getType()),block.getType()==Material.DIRT_PATH,false));
            }
            if(request.remaining.isEmpty()) {finish(player,world,request);iterator.remove();}
            if(budget==0)break;
        }
    }
    private void finish(Player player,World world,Request request) {
        var bounds=ConstructionPreview.footprint(request.template,Transform.identity(),request.origin);
        boolean complete=!request.incomplete;
        // Recheck chunk availability before the entity query, which must never load missing chunks.
        for(int x=bounds.minimum().x()>>4;x<=bounds.maximum().x()>>4;x++)for(int z=bounds.minimum().z()>>4;z<=bounds.maximum().z()>>4;z++)
            if(!world.isChunkLoaded(x,z))complete=false;
        boolean entity=complete&&!world.getNearbyEntities(new BoundingBox(bounds.minimum().x(),bounds.minimum().y(),bounds.minimum().z(),
                bounds.maximum().x()+1.0,bounds.maximum().y()+1.0,bounds.maximum().z()+1.0)).isEmpty();
        long now=System.currentTimeMillis();var frame=new Observation(request.world,request.origin,request.started,complete,entity,request.cells);
        var result=ConstructionPreview.assess(request.template,Transform.identity(),request.world,request.origin,frame,null,now,30_000);
        player.sendMessage("Plan "+request.template.id()+" at "+request.origin.x()+", "+request.origin.y()+", "+request.origin.z()+": "+result.plannedBlocks()+" blocks; work "+request.template.work()+"; materials "+request.template.materials());
        player.sendMessage("Site findings: "+result.issues().stream().sorted().map(PaperConstructionPreview::explain).toList());
        player.sendMessage("No development area is approved. Saved land protections are not checked yet. This preview cannot authorize construction; terrain must be checked again before any future work.");
    }
    private static String explain(Issue issue){return switch(issue){
        case NO_APPROVED_AREA->"development permission required";case WRONG_WORLD->"wrong world";
        case STALE_OR_FUTURE->"expired observation";case INCOMPLETE->"unloaded or out-of-height cells";
        case ENTITY_PRESENT->"entity in building space";case PROTECTED->"protected area";
        case OBSTRUCTED->"existing blocks in building space";case UNSUPPORTED->"uneven or unsupported ground";
        case ROAD_REQUIRED->"entrance needs a dirt-path connection";};}
    private static void usage(CommandSender sender){sender.sendMessage("/civ build templates or /civ build preview <template> [0|90|180|270] [mirror]");}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if(!sender.hasPermission(PERMISSION))return List.of();
        List<String> options=switch(args.length){case 2->List.of("templates","preview");case 3->args[1].equalsIgnoreCase("preview")?List.copyOf(library.templates().keySet()):List.of();case 4->List.of("0","90","180","270");case 5->List.of("mirror");default->List.of();};
        return options.stream().filter(s->s.startsWith(args[args.length-1].toLowerCase(Locale.ROOT))).toList();
    }
}
