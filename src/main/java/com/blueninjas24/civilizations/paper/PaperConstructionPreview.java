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
import java.util.concurrent.CompletableFuture;
import com.blueninjas24.civilizations.database.ConstructionLandRepository.Column;
import com.blueninjas24.civilizations.construction.ConstructionLandPolicy;

/** Bounded read-only site inspection, with routing to separate confirmed project metadata controls. No placement command. */
public final class PaperConstructionPreview extends BukkitRunnable implements CommandExecutor,TabCompleter {
    public static final String PERMISSION="civilizations.construction.preview";
    private static final int CELLS_PER_TICK=64,MAX_REQUESTS=8;
    private static final Set<Material> SUPPORT=Set.of(Material.STONE,Material.COBBLESTONE,Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE,Material.DIRT,Material.GRASS_BLOCK,Material.DIRT_PATH,Material.COARSE_DIRT,
            Material.ROOTED_DIRT,Material.PODZOL,Material.MYCELIUM,Material.ANDESITE,Material.DIORITE,Material.GRANITE,Material.SANDSTONE,Material.RED_SANDSTONE,Material.BEDROCK);
    private static final Set<Material> NATURAL_LOOKING_GROUND=Set.of(Material.GRASS_BLOCK,Material.DIRT,Material.COARSE_DIRT,Material.ROOTED_DIRT,Material.PODZOL,Material.MYCELIUM);
    private final PaperConstructionLibrary library;
    private final java.util.function.Function<UUID,Player> playerLookup;
    private CommandExecutor housing;
    public void configureHousing(CommandExecutor housing){this.housing=Objects.requireNonNull(housing);}
    private CommandExecutor projects;
    public void configureProjects(CommandExecutor projects){this.projects=Objects.requireNonNull(projects);}
    private final java.util.function.BiFunction<UUID,Bounds,CompletableFuture<Set<Column>>> protectionReader;
    private final LinkedHashMap<UUID,Request> pending=new LinkedHashMap<>();
    private static final class Request {
        final UUID player,world;final ConstructionTemplate template;final Position origin;final long started;
        final ArrayDeque<Position> remaining;final Map<Position,Cell> cells=new HashMap<>();boolean incomplete;
        final Map<Position,ConstructionLandPolicy.Kind> landKinds=new HashMap<>();
        CompletableFuture<Set<Column>> protections;
        Request(Player player,Position start){
            this.player=player.getUniqueId();world=player.getWorld().getUID();template=null;origin=start;started=System.currentTimeMillis();
            remaining=new ArrayDeque<>();for(int y=0;y<3;y++)for(int z=-8;z<=8;z++)for(int x=-8;x<=8;x++)remaining.add(start.plus(new Position(x,y,z)));
        }
        Request(Player player,ConstructionTemplate template,Position origin,ConstructionLandPolicy land) {
            this.player=player.getUniqueId();world=player.getWorld().getUID();this.template=template;this.origin=origin;
            started=System.currentTimeMillis();remaining=new ArrayDeque<>(land.requiredCells(template,origin));
        }
    }
    public PaperConstructionPreview(PaperConstructionLibrary library){this(library,null);}
    public PaperConstructionPreview(PaperConstructionLibrary library,java.util.function.BiFunction<UUID,Bounds,CompletableFuture<Set<Column>>> protectionReader){this(library,protectionReader,Bukkit::getPlayer);}
    /** Native test harness can supply a command actor without creating a fake online player. */
    public PaperConstructionPreview(PaperConstructionLibrary library,java.util.function.BiFunction<UUID,Bounds,CompletableFuture<Set<Column>>> protectionReader,java.util.function.Function<UUID,Player> playerLookup){this.library=Objects.requireNonNull(library);this.protectionReader=protectionReader;this.playerLookup=Objects.requireNonNull(playerLookup);}
    static boolean stableSupport(Material material){return SUPPORT.contains(material);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(args.length>=2&&Set.of("projects","funding","inspectproject","recoverproject","confirmrecovery","cancelproject","confirmcancel","cancelrequest").contains(args[1].toLowerCase(Locale.ROOT))){
            if(projects!=null)return projects.onCommand(sender,command,label,args);
            sender.sendMessage("Home project controls are unavailable; check the server settings.");return true;
        }
        if(!sender.hasPermission(PERMISSION)){sender.sendMessage("You do not have permission to preview construction.");return true;}
        if(args.length>=2&&args[1].equalsIgnoreCase("housing")){
            if(housing!=null)return housing.onCommand(sender,command,label,args);
            sender.sendMessage("Housing planning reports are unavailable; check the server settings.");return true;
        }
        if(args.length==2&&args[1].equalsIgnoreCase("templates")) {
            for(var template:library.templates().values())sender.sendMessage(template.id()+": "+template.name()+"; "+template.size().x()+"x"+template.size().y()+"x"+template.size().z()+"; "+template.stages().size()+" stages; materials "+template.materials());
            return true;
        }
        if(!(sender instanceof Player player)){sender.sendMessage("Use /civ build templates; site previews require a player.");return true;}
        if(args.length==2&&args[1].equalsIgnoreCase("access")){
            if(pending.containsKey(player.getUniqueId())){sender.sendMessage("Your current site preview is still being checked.");return true;}
            if(pending.size()>=MAX_REQUESTS){sender.sendMessage("Construction preview queue is full; try again shortly.");return true;}
            var location=player.getLocation();var forward=location.getDirection().setY(0);if(forward.lengthSquared()<0.01)forward=new org.bukkit.util.Vector(0,0,1);
            location.add(forward.normalize().multiply(8));
            try{
                var start=new Position(location.getBlockX(),Math.subtractExact(player.getLocation().getBlockY(),1),location.getBlockZ());var request=new Request(player,start);
                if(protectionReader!=null)request.protections=protectionReader.apply(request.world,accessFrame(start));
                pending.put(player.getUniqueId(),request);sender.sendMessage("Checking flat road access eight blocks ahead. Read-only: no road, reservation or payment.");
            }catch(IllegalArgumentException|ArithmeticException invalid){sender.sendMessage("Road access preview is outside bounded coverage.");}
            return true;
        }
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
            var request=new Request(player,template,origin,library.land());
            if(protectionReader!=null) {
                var required=library.land().requiredCells(template,origin);
                var bounds=new Bounds(new Position(required.stream().mapToInt(Position::x).min().orElseThrow(),origin.y()-1,required.stream().mapToInt(Position::z).min().orElseThrow()),
                        new Position(required.stream().mapToInt(Position::x).max().orElseThrow(),origin.y()+template.size().y()-1,required.stream().mapToInt(Position::z).max().orElseThrow()));
                request.protections=protectionReader.apply(request.world,bounds);
            }
            pending.put(player.getUniqueId(),request);
            sender.sendMessage("Checking a site eight blocks ahead. Preview only; no blocks, items or money will change.");
        }catch(IllegalArgumentException|ArithmeticException invalid){sender.sendMessage("Cannot preview this plan: "+invalid.getMessage());}
        return true;
    }
    @Override public void run() {
        // Global budget, round robin across requests; no per-player multiplication of the block-read budget.
        int budget=CELLS_PER_TICK;
        for(var iterator=pending.values().iterator();iterator.hasNext();) {
            var request=iterator.next();var player=playerLookup.apply(request.player);var world=Bukkit.getWorld(request.world);
            if(player==null||!player.getWorld().getUID().equals(request.world)||!player.hasPermission(PERMISSION)||world==null){iterator.remove();continue;}
            if(System.currentTimeMillis()-request.started>30_000){player.sendMessage("Site preview expired; try again.");iterator.remove();continue;}
            int allowance=Math.min(budget,Math.max(1,CELLS_PER_TICK/pending.size()));
            while(allowance-->0&&!request.remaining.isEmpty()) {
                budget--;var p=request.remaining.removeFirst();
                if(p.y()<world.getMinHeight()||p.y()>=world.getMaxHeight()||!world.isChunkLoaded(p.x()>>4,p.z()>>4)){request.incomplete=true;continue;}
                var block=world.getBlockAt(p.x(),p.y(),p.z());
                request.cells.put(p,new Cell(block.getType().isAir(),SUPPORT.contains(block.getType()),block.getType()==Material.DIRT_PATH,false));
                request.landKinds.put(p,block.getType().isAir()?ConstructionLandPolicy.Kind.AIR:NATURAL_LOOKING_GROUND.contains(block.getType())?ConstructionLandPolicy.Kind.NATURAL_LOOKING_GROUND
                        :block.getType()==Material.DIRT_PATH?ConstructionLandPolicy.Kind.ROAD:ConstructionLandPolicy.Kind.OTHER);
            }
            if(request.remaining.isEmpty()&&(request.protections==null||request.protections.isDone())) {finish(player,world,request);iterator.remove();}
            if(budget==0)break;
        }
    }
    private void finish(Player player,World world,Request request) {
        if(request.template==null){finishAccess(player,world,request);return;}
        var bounds=ConstructionPreview.footprint(request.template,Transform.identity(),request.origin);
        boolean complete=!request.incomplete;
        boolean protectionComplete=request.protections!=null;
        if(protectionComplete)try {
            var protectedColumns=request.protections.join();
            request.cells.replaceAll((p,c)->new Cell(c.air(),c.stableSupport(),c.road(),c.protectedWork()||protectedColumns.contains(new Column(p.x(),p.z()))));
        }catch(java.util.concurrent.CompletionException|java.util.concurrent.CancellationException failed){protectionComplete=false;player.sendMessage("Saved protection could not be checked; this site remains blocked.");}
        if(!protectionComplete)complete=false;
        // Recheck chunk availability before the entity query, which must never load missing chunks.
        for(int x=bounds.minimum().x()>>4;x<=bounds.maximum().x()>>4;x++)for(int z=bounds.minimum().z()>>4;z<=bounds.maximum().z()>>4;z++)
            if(!world.isChunkLoaded(x,z))complete=false;
        boolean entity=complete&&!world.getNearbyEntities(new BoundingBox(bounds.minimum().x(),bounds.minimum().y(),bounds.minimum().z(),
                bounds.maximum().x()+1.0,bounds.maximum().y()+1.0,bounds.maximum().z()+1.0)).isEmpty();
        var landCells=new HashMap<Position,ConstructionLandPolicy.Cell>();request.cells.forEach((p,c)->landCells.put(p,new ConstructionLandPolicy.Cell(request.landKinds.get(p),c.protectedWork())));
        var land=library.land().review(request.template,request.origin,landCells,complete);
        var templateCells=new HashMap<Position,Cell>();for(var p:ConstructionPreview.requiredCells(request.template,Transform.identity(),request.origin))if(request.cells.containsKey(p))templateCells.put(p,request.cells.get(p));
        long now=System.currentTimeMillis();var frame=new Observation(request.world,request.origin,request.started,complete,entity,templateCells);
        var result=ConstructionPreview.assess(request.template,Transform.identity(),request.world,request.origin,frame,null,now,30_000);
        if(!land.suitable()){var issues=EnumSet.noneOf(Issue.class);issues.addAll(result.issues());issues.add(Issue.LAND_UNSUITABLE);result=new Assessment(result.footprint(),result.plannedBlocks(),issues);}
        player.sendMessage("Plan "+request.template.id()+" at "+request.origin.x()+", "+request.origin.y()+", "+request.origin.z()+": "+result.plannedBlocks()+" blocks; work "+request.template.work()+"; materials "+request.template.materials());
        player.sendMessage("Site findings: "+result.issues().stream().sorted().map(PaperConstructionPreview::explain).toList());
        player.sendMessage("Buffered clear-land screen: "+(land.suitable()?"suitable in this bounded observation":land.findings())+". "+(protectionComplete?"Saved fenced plots checked.":"Saved protection incomplete."));
        player.sendMessage("Preview only: no funded project or construction authority. Terrain and protection must be checked again before future work.");
    }
    private static Bounds accessFrame(Position start){return new Bounds(start.plus(new Position(-8,0,-8)),start.plus(new Position(8,2,8)));}
    private void finishAccess(Player player,World world,Request request){
        var frame=accessFrame(request.origin);boolean complete=!request.incomplete,protectionComplete=request.protections!=null;
        if(protectionComplete)try{
            var protectedColumns=request.protections.join();request.cells.replaceAll((p,c)->new Cell(c.air(),c.stableSupport(),c.road(),c.protectedWork()||protectedColumns.contains(new Column(p.x(),p.z()))));
        }catch(java.util.concurrent.CompletionException|java.util.concurrent.CancellationException failed){protectionComplete=false;}
        for(int x=frame.minimum().x()>>4;x<=frame.maximum().x()>>4;x++)for(int z=frame.minimum().z()>>4;z<=frame.maximum().z()>>4;z++)if(!world.isChunkLoaded(x,z))complete=false;
        var entities=new ArrayList<Bounds>();
        if(complete)for(var entity:world.getNearbyEntities(new BoundingBox(frame.minimum().x(),frame.minimum().y(),frame.minimum().z(),frame.maximum().x()+1.0,frame.maximum().y()+1.0,frame.maximum().z()+1.0))){
            if(entities.size()==com.blueninjas24.civilizations.construction.ConstructionSitePlanner.MAX_EXCLUSIONS){complete=false;break;}
            var b=entity.getBoundingBox();entities.add(new Bounds(new Position((int)Math.floor(b.getMinX()),(int)Math.floor(b.getMinY()),(int)Math.floor(b.getMinZ())),new Position((int)Math.floor(b.getMaxX()),(int)Math.floor(b.getMaxY()),(int)Math.floor(b.getMaxZ()))));
        }
        var ground=new Bounds(frame.minimum(),new Position(frame.maximum().x(),request.origin.y(),frame.maximum().z()));
        var survey=new com.blueninjas24.civilizations.construction.ConstructionSitePlanner.Survey(request.world,request.started,complete,protectionComplete,request.cells,List.of(),entities);
        var result=com.blueninjas24.civilizations.construction.LocalRoadAccessPlanner.select(request.world,request.origin,ground,survey,request.landKinds,com.blueninjas24.civilizations.construction.LocalRoadAccessPlanner.Policy.prototype(),System.currentTimeMillis(),30_000,0);
        player.sendMessage("Flat local road access: "+result.status()+"; new surface cells "+result.newSurfaceCells()+"; search nodes "+result.reviewed()+".");
        if(!result.corridor().isEmpty()){
            var end=result.corridor().getLast();player.sendMessage("Ground route from "+request.origin.x()+", "+request.origin.y()+", "+request.origin.z()+" to existing path at "+end.x()+", "+end.y()+", "+end.z()+".");
        }
        player.sendMessage("Read-only observation: no site rights, supplies, labor or paving. Changed ground/protection needs a fresh check. Homes still require an existing path.");
    }
    private static String explain(Issue issue){return switch(issue){
        case NO_APPROVED_AREA->"validated construction permission required";case WRONG_WORLD->"wrong world";
        case STALE_OR_FUTURE->"expired observation";case INCOMPLETE->"incomplete terrain or saved protection evidence";
        case ENTITY_PRESENT->"entity in building space";case PROTECTED->"protected area";
        case OBSTRUCTED->"existing blocks in building space";case UNSUPPORTED->"uneven or unsupported ground";
        case ROAD_REQUIRED->"entrance needs a dirt-path connection";case LAND_UNSUITABLE->"buffered land check did not pass";};}
    private static void usage(CommandSender sender){
        sender.sendMessage("/civ build templates | housing <village UUID> | projects <village UUID> | funding <project UUID> | access | preview <template> [0|90|180|270] [mirror]");
        sender.sendMessage("Project inspection/recovery: /civ build inspectproject/recoverproject/cancelproject <project UUID> | confirmrecovery/confirmcancel <token> | cancelrequest");
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        boolean preview=sender.hasPermission(PERMISSION),manage=sender.hasPermission(PaperHousingProjectControls.MANAGE);
        if(!preview&&!manage)return List.of();
        if(args.length==2){var choices=new ArrayList<String>();if(preview)choices.addAll(List.of("templates","housing","projects","funding","inspectproject","access","preview"));if(manage)choices.addAll(List.of("recoverproject","confirmrecovery","cancelproject","confirmcancel","cancelrequest"));return choices.stream().filter(s->s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();}
        if(!preview)return List.of();
        List<String> options=switch(args.length){case 2->List.of("templates","housing","preview");case 3->args[1].equalsIgnoreCase("preview")?List.copyOf(library.templates().keySet()):List.of();case 4->args[1].equalsIgnoreCase("preview")?List.of("0","90","180","270"):List.of();case 5->args[1].equalsIgnoreCase("preview")?List.of("mirror"):List.of();default->List.of();};
        return options.stream().filter(s->s.startsWith(args[args.length-1].toLowerCase(Locale.ROOT))).toList();
    }
}
