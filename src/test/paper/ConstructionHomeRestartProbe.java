import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.paper.*;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Two actual JVM runs in one marked disposable footprint. No economic/labor/capacity claim. */
public final class ConstructionHomeRestartProbe extends JavaPlugin {
    private final ExecutorService owner=Executors.newSingleThreadExecutor();
    private final Properties state=new Properties();private final Set<Long> tickets=new HashSet<>();
    private final Map<Position,String> originals=new TreeMap<>();
    private World world;private Path metadata,folder;private DatabaseManager db;private ConstructionDraft draft;
    private List<Map.Entry<Position,String>> blocks;private PaperConstructionStep writer;
    private CompletableFuture<?> pending;private int phase,index,ticks;private boolean authorized,resuming,paused,restored;
    private record Loaded(ConstructionDraft draft,int applied){}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        Files.createDirectories(getDataFolder().toPath());metadata=getDataFolder().toPath().resolve("home-restart.properties");folder=getDataFolder().toPath().resolve("home-journal");
        resuming=Files.exists(metadata);
        if(resuming) {
            require(Files.size(metadata)<131072,"Bounded private metadata");try(var in=Files.newInputStream(metadata)){state.load(in);}
            require(state.getProperty("checkpoint","").equals("RESUME"),"Only the saved half-built fixture can resume");
            world=getServer().getWorld(UUID.fromString(state.getProperty("world")));require(world!=null,"Original world present");
            for(var key:state.stringPropertyNames())if(key.startsWith("original.")) {
                String[] xyz=key.substring(9).split(",");require(xyz.length==3,"Original coordinate");
                originals.put(new Position(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2])),state.getProperty(key));
            }
            require(originals.size()==121,"Exactly the original home blocks and road connector retained");
            // Explicit disposable-fixture staging, not simulation: an empty Paper server may load
            // no spawn chunks on restart. Reopen only these previously generated recorded cells.
            var savedChunks=new HashSet<Long>();
            for(var p:originals.keySet())savedChunks.add(((long)(p.x()>>4)<<32)^((p.z()>>4)&0xffffffffL));
            require(savedChunks.size()<=4,"Bounded saved home footprint");
            for(long key:savedChunks) {
                int cx=(int)(key>>32),cz=(int)key;require(world.isChunkGenerated(cx,cz),"Saved fixture chunk must already exist");
                if(!world.isChunkLoaded(cx,cz))world.getChunkAt(cx,cz);
                if(tickets.add(key))world.addPluginChunkTicket(cx,cz,this);
            }
        }else {
            world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();int x=spawn.getBlockX()+4,z=spawn.getBlockZ()+4;
            require(world.isChunkLoaded(x>>4,z>>4),"Already-loaded test origin");var origin=new Position(x,world.getHighestBlockYAt(x,z)+1,z);
            var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");
            var plan=PaperConstructionLibrary.load(plugin).templates().get("starter_home");require(plan.blockCount()==120,"Original four-bed home required");
            draft=new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),world.getUID(),origin,Transform.identity(),plan,System.currentTimeMillis());
            hold(draft);var footprint=ConstructionPreview.footprint(plan,draft.transform(),origin);
            for(int dx=0;dx<6;dx++)for(int dz=0;dz<6;dz++)for(int dy=0;dy<4;dy++)require(world.getBlockAt(x+dx,origin.y()+dy,z+dz).getType().isAir(),"Empty private home volume required");
            for(var block:placements(draft))originals.put(block.getKey(),world.getBlockAt(block.getKey().x(),block.getKey().y(),block.getKey().z()).getBlockData().getAsString());
            var road=plan.anchors().stream().filter(a->a.role()==Role.ROAD).findFirst().orElseThrow().at().plus(origin);
            originals.put(road,world.getBlockAt(road.x(),road.y(),road.z()).getBlockData().getAsString());
            state.setProperty("draft",draft.id().toString());state.setProperty("world",draft.world().toString());
            for(var e:originals.entrySet())state.setProperty("original."+e.getKey().x()+","+e.getKey().y()+","+e.getKey().z(),e.getValue());
            state.setProperty("checkpoint","PREPARING");saveState(); // Original data durable before the first test change.
            world.getBlockAt(road.x(),road.y(),road.z()).setType(Material.DIRT_PATH,false);
        }
        final ConstructionDraft initial=draft;final long checked=System.currentTimeMillis();
        pending=CompletableFuture.supplyAsync(()->{try {
            db=new DatabaseManager(folder.toFile(),getLogger());db.initialize();var c=db.getConnection();
            if(!resuming) {
                db.saveSettlement(new com.blueninjas24.civilizations.settlement.Settlement(initial.settlement(),"Private home fixture","Disposable world",0,64,0,1,0,0));
                db.getVillageIdentityRepository().observeWorld(initial.world(),"Disposable world");db.getVillageIdentityRepository().bindSettlement(initial.settlement(),initial.world());
                new ConstructionDraftRepository(c).save(initial);
                new ConstructionSiteReservations(c).reserve(initial.id(),new ConstructionMaterials.Authority(initial.id(),initial.world(),checked,true,true),System.currentTimeMillis());
            }
            var saved=new ConstructionDraftRepository(c).find(UUID.fromString(state.getProperty("draft"))).orElseThrow();
            require(new ConstructionSiteReservations(c).find(saved.id()).orElseThrow().active(),"Saved exact site reservation survives JVM restart");
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM construction_block_journal WHERE state='APPLIED'")){return new Loaded(saved,r.getInt(1));}
        }catch(Exception failure){throw new CompletionException(failure);}},owner);
        new BukkitRunnable(){public void run(){try {
            require(++ticks<=2400&&getServer().getOnlinePlayers().isEmpty(),"Bounded empty private run");
            if(pending.isDone())advance();
        }catch(Throwable failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_HOME_RESTART_FAIL",failure);try{restore();}finally{getServer().shutdown();}}}}.runTaskTimer(this,1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4 home setup failed",failure);if(authorized)try{restore();}finally{getServer().shutdown();}}}
    private List<Map.Entry<Position,String>> placements(ConstructionDraft d) {
        var plan=PaperConstructionLibrary.prepare(d.template(),d.transform());var result=new ArrayList<Map.Entry<Position,String>>();
        for(int n=0;n<d.template().stages().size();n++)for(var local:d.template().stages().get(n).blocks().keySet()) {
            var at=d.transform().apply(local,d.template().size());result.add(Map.entry(d.origin().plus(at),plan.stages().get(n).blocks().get(at)));
        }return List.copyOf(result);
    }
    private void hold(ConstructionDraft d) {
        for(var p:ConstructionPreview.requiredCells(d.template(),d.transform(),d.origin())) {
            require(world.isChunkLoaded(p.x()>>4,p.z()>>4),"Test never loads target chunks");long key=((long)(p.x()>>4)<<32)^((p.z()>>4)&0xffffffffL);
            if(tickets.add(key))world.addPluginChunkTicket(p.x()>>4,p.z()>>4,this);
        }
    }
    private void advance()throws Exception {
        var result=pending.join();
        switch(phase) {
            case 0->{var loaded=(Loaded)result;draft=loaded.draft();index=loaded.applied();require(index==(resuming?59:0),"Expected saved interruption boundary");hold(draft);blocks=placements(draft);
                writer=PaperConstructionStep.development(this,draft,new ApprovedArea(UUID.randomUUID(),draft.world(),ConstructionPreview.footprint(draft.template(),draft.transform(),draft.origin())));phase=1;prepare();}
            case 1->{var intent=(ConstructionBlockJournal.Intent)result;var outcome=writer.attempt(intent);
                require(outcome.state()==(resuming&&index==59?PaperConstructionStep.State.ALREADY_PRESENT:PaperConstructionStep.State.APPLIED),"Expected native write/recovery: "+outcome);
                if(!resuming&&index==59){state.setProperty("checkpoint","RESUME");saveState();paused=true;phase=4;pending=CompletableFuture.runAsync(()->{db.close();db=null;},owner);return;}
                phase=2;pending=CompletableFuture.supplyAsync(()->{try{return new ConstructionBlockJournal(db.getConnection()).acknowledge(intent.operation(),intent.after());}catch(Exception failure){throw new CompletionException(failure);}},owner);}
            case 2->{require(((ConstructionBlockJournal.Intent)result).state()==ConstructionBlockJournal.State.APPLIED,"Applied receipt");
                if(++index<blocks.size()){phase=1;prepare();return;}
                for(var block:blocks)require(world.getBlockAt(block.getKey().x(),block.getKey().y(),block.getKey().z()).getBlockData().getAsString().equals(block.getValue()),"All120 native authored states remain exact");
                long beds=blocks.stream().filter(b->b.getValue().startsWith("minecraft:white_bed[")&&b.getValue().contains("part=foot")).count();require(beds==4,"Four actual bed pairs complete");
                restore();state.setProperty("checkpoint","COMPLETE");saveState();phase=3;pending=CompletableFuture.runAsync(()->{db.close();db=null;},owner);}
            case 3->{getLogger().info("P4_HOME_RESTART_PASS:120 native home blocks/four bed pairs; actual JVM restart at physical-write-before-ack boundary recovered once; saved site retained; original121 cells restored; no funded work/capacity or human-server claim");phase=5;getServer().shutdown();}
            case 4->{getLogger().info("P4_HOME_RESTART_CHECKPOINT:60 blocks placed,59 acknowledged; private server stopping for second JVM run");phase=5;getServer().shutdown();}
            default->{}
        }
    }
    private void prepare(){int ordinal=index;var id=UUID.nameUUIDFromBytes((draft.id()+":"+ordinal).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        pending=CompletableFuture.supplyAsync(()->{try{return new ConstructionBlockJournal(db.getConnection()).prepare(id,draft.id(),ordinal,blocks.get(ordinal).getValue());}catch(Exception failure){throw new CompletionException(failure);}},owner);}
    private void saveState()throws Exception{try(var out=Files.newOutputStream(metadata)){state.store(out,"Private disposable construction restart fixture");}}
    private void restore(){if(!authorized||world==null||restored)return;
        for(var e:originals.entrySet()){var p=e.getKey();require(world.isChunkLoaded(p.x()>>4,p.z()>>4),"Restore never loads chunks");var block=world.getBlockAt(p.x(),p.y(),p.z());if(!block.getBlockData().getAsString().equals(e.getValue()))block.setBlockData(getServer().createBlockData(e.getValue()),false);}
        restored=true;world.removePluginChunkTickets(this);tickets.clear();
    }
    @Override public void onDisable(){try{if(!paused)restore();else if(world!=null)world.removePluginChunkTickets(this);}finally{owner.execute(()->{if(db!=null){db.close();db=null;}});owner.shutdown();}}
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
}
