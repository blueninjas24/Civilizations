import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Marked empty disposable server only. Exercises actual block evidence and private SQL-owner housing. */
public final class HousingRuntimeProbe extends JavaPlugin {
    private final Map<Position,String> originals=new TreeMap<>();
    private final UUID actor=UUID.randomUUID();private final AtomicInteger reviews=new AtomicInteger();
    private World world;private BuildingLayout layout;private PopulationSession session;private PaperHousingRuntime runtime;
    private Path data;private CompletableFuture<?> work;private int phase,ticks;private boolean authorized;
    private UUID family;private Set<UUID> residents;private Position roof;
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        data=root.resolve("housing-runtime-probe");
        new BukkitRunnable(){public void run(){try {
            check(++ticks<1200&&getServer().getOnlinePlayers().isEmpty(),"Bounded empty fixture phase="+phase);
            if(runtime!=null)runtime.run();if(work!=null&&!work.isDone())return;step();
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_HOUSING_RUNTIME_FAIL phase="+phase,failure);cleanup();cancel();getServer().shutdown();}
        if(phase==99){cancel();getServer().shutdown();}}}.runTaskTimer(this,1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"Housing fixture guard failed",failure);if(authorized)getServer().shutdown();}}
    private void step()throws Exception {
        if(work!=null)work.join();
        switch(phase) {
            case 0->{prepare();session=newSession();work=session.start(false,Set.of(world.getUID()));phase=1;}
            case 1->{
                var beds=layout.units().getFirst().beds().stream().map(b->new VillageObservation.Position(b.head().x(),b.head().y(),b.head().z())).toList();
                var people=List.of(new VillageObservation.Person(UUID.randomUUID(),beds.getFirst(),true,"Fixture first"),new VillageObservation.Person(UUID.randomUUID(),beds.get(1),true,"Fixture second"));
                work=session.observe(new VillageObservation(world.getUID(),"fixture",System.currentTimeMillis(),true,beds,people));phase=2;
            }
            case 2->{UUID town=session.settlementNames().keySet().iterator().next();layout=new BuildingLayout(layout.id(),town,world.getUID(),layout.kind(),layout.plot(),layout.structure(),layout.entrance(),layout.shared(),layout.units());
                work=session.previewBuildingDonation(actor,world.getUID(),layout,true);phase=3;}
            case 3->{var preview=(com.blueninjas24.civilizations.application.BuildingConsentRequests.Preview)work.join();
                work=session.confirmBuildingConsent(actor,world.getUID(),preview.token(),true,true,false,false);phase=4;}
            case 4->{startRuntime();work=null;phase=5;}
            case 5->{if(reviews.get()<1)return;stopRuntime();work=session.pulse();phase=6;}
            case 6->{var state=session.snapshot().orElseThrow();check(state.residents().size()==2,"No population created");family=state.residents().getFirst().householdId();
                residents=new HashSet<>();for(var person:state.residents())residents.add(person.id());
                check(state.householdCapacity().get(family)==4,"Actual four-bed home replaces old eight-person bed-group capacity");
                world.getBlockAt(roof.x(),roof.y(),roof.z()).setType(Material.AIR,false);startRuntime();work=null;phase=7;}
            case 7->{if(reviews.get()<1)return;stopRuntime();work=session.pulse();phase=8;}
            case 8->{check(session.snapshot().orElseThrow().householdCapacity().get(family)==0,"Confirmed roof loss removes capacity, preserves household");
                world.getBlockAt(roof.x(),roof.y(),roof.z()).setType(Material.OAK_PLANKS,false);
                var changing=new PaperDwellingSurvey(layout,System.currentTimeMillis(),true);
                check(changing.advance(256,System.currentTimeMillis()).isEmpty(),"Runtime survey requires second bounded pass");
                world.getBlockAt(roof.x(),roof.y(),roof.z()).setType(Material.AIR,false);
                check(!changing.advance(256,System.currentTimeMillis()).orElseThrow().complete(),"Edit between passes yields unknown, never fresh authority");
                world.getBlockAt(roof.x(),roof.y(),roof.z()).setType(Material.OAK_PLANKS,false);startRuntime();work=null;phase=9;}
            case 9->{if(reviews.get()<1)return;stopRuntime();work=session.pulse();phase=10;}
            case 10->{check(session.snapshot().orElseThrow().householdCapacity().get(family)==4,"Repair restores reviewed home capacity");work=session.updateLoadedWorlds(Set.of());phase=11;}
            case 11->{work=session.housingPage(null,8);phase=12;}
            case 12->{check(((PopulationSession.HousingPage)work.join()).layouts().isEmpty(),"Absent worlds not scheduled");session.close();session=newSession();work=session.start(false,Set.of(world.getUID()));phase=13;}
            case 13->{work=session.pulse();phase=14;}
            case 14->{var state=session.snapshot().orElseThrow();check(state.householdCapacity().get(family)==4,"Assigned home survives restart");
                var ids=new HashSet<UUID>();for(var person:state.residents())ids.add(person.id());check(ids.equals(residents),"Permanent residents unchanged");
                work=session.previewBuildingReclamation(actor,world.getUID(),layout.id(),true,false);phase=15;}
            case 15->{var preview=(com.blueninjas24.civilizations.application.BuildingConsentRequests.Preview)work.join();
                check(preview.housingAssignments().size()==1&&preview.legacyHousingFamilies().isEmpty(),"Reclamation impact names exactly the current assignment");
                work=session.confirmBuildingConsent(actor,world.getUID(),preview.token(),false,false,true,false);phase=16;}
            case 16->{work=session.pulse();phase=17;}
            case 17->{var state=session.snapshot().orElseThrow();check(state.householdCapacity().get(family)==0&&state.residents().size()==2,"Reclamation removes capacity without removing residents or reviving legacy beds");
                session.close();session=null;
                try(var c=DriverManager.getConnection("jdbc:sqlite:"+data.resolve("civilizations.db"));var s=c.createStatement()) {
                    try(var r=s.executeQuery("SELECT count(*) FROM dwelling_active_occupancy")){check(r.getInt(1)==0,"Reclaimed unit is vacant");}
                    try(var r=s.executeQuery("SELECT count(*) FROM dwelling_assignments")){check(r.getInt(1)==1,"Repeated reviews never duplicate assignments");}
                    try(var r=s.executeQuery("SELECT count(*) FROM dwelling_bed_claims")){check(r.getInt(1)==4,"Four permanent bed exclusions from legacy accounting");}
                    try(var r=s.executeQuery("PRAGMA integrity_check")){check(r.getString(1).equals("ok"),"SQLite integrity");}
                    try(var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"SQLite foreign keys");}
                }
                cleanup();getLogger().info("P4_HOUSING_RUNTIME_PASS: real four-bed home; explicit donation; existing family adopted whole; old eight-capacity becomes four; roof loss zero and repair four; changed two-pass survey unknown; absent-world pause; restart keeps residents/assignment; confirmed donor reclamation removes capacity without legacy fallback or resident loss; no duplicate homes;144 cells restored");phase=99;}
            default->throw new IllegalStateException("Unexpected phase");
        }
    }
    private PopulationSession newSession(){return new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);}
    private void startRuntime(){reviews.set(0);runtime=new PaperHousingRuntime(new PaperHousingRuntime.Owner(){
        public CompletableFuture<PopulationSession.HousingPage> page(UUID after){return session.housingPage(after,8);}
        public CompletableFuture<DwellingHousing.Report> review(BuildingLayout l,DwellingInspection.Observation o){return session.reconcileHousing(l,o,DwellingInspection.Policy.prototype()).thenApply(report->{reviews.incrementAndGet();return report;});}
    },new PaperHousingRuntime.Settings(true,32,5),(l,t)->new PaperDwellingSurvey(l,t,true)::advance,System::currentTimeMillis,getLogger());}
    private void stopRuntime(){if(runtime!=null){runtime.close();runtime=null;}}
    private void prepare()throws Exception {
        var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");check(plugin!=null&&plugin.isEnabled(),"Production plugin enabled");
        world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();int x=spawn.getBlockX()+4,z=spawn.getBlockZ()+4;
        for(int cx=x>>4;cx<=(x+5)>>4;cx++)for(int cz=z>>4;cz<=(z+5)>>4;cz++){check(world.isChunkLoaded(cx,cz),"Loaded fixture only");world.addPluginChunkTicket(cx,cz,this);}
        var origin=new Position(x,world.getHighestBlockYAt(x,z)+1,z);var plan=PaperConstructionLibrary.load(plugin).templates().get("starter_home");
        var bounds=new Bounds(origin,origin.plus(new Position(plan.size().x()-1,plan.size().y()-1,plan.size().z()-1)));
        layout=HomeBuildingLayout.fromDraft(new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),world.getUID(),origin,Transform.identity(),plan,System.currentTimeMillis()),bounds);
        for(var p:DwellingInspection.requiredCells(layout))originals.put(p,world.getBlockAt(p.x(),p.y(),p.z()).getBlockData().getAsString());
        for(var p:originals.keySet())world.getBlockAt(p.x(),p.y(),p.z()).setType(Material.AIR,false);
        for(var stage:plan.stages())for(var cell:stage.blocks().entrySet()){var p=origin.plus(cell.getKey());world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(cell.getValue()),false);}
        roof=layout.units().getFirst().beds().getFirst().foot().plus(new Position(0,2,0));
    }
    private void cleanup(){stopRuntime();if(session!=null){session.close();session=null;}if(!authorized||world==null)return;
        for(var entry:originals.entrySet()){var p=entry.getKey();check(world.isChunkLoaded(p.x()>>4,p.z()>>4),"Restore loaded-only");world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(entry.getValue()),false);}
        originals.clear();world.removePluginChunkTickets(this);}
    @Override public void onDisable(){cleanup();}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
