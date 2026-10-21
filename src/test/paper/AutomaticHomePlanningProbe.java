import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Genuine loaded-only native survey and SQL/admin cancellation, solely in the marked empty disposable world. */
public class AutomaticHomePlanningProbe extends JavaPlugin {
    protected int familyMembers(){return 1;}
    protected String expectedTemplate(){return "starter_home";}
    private final UUID town=UUID.randomUUID(),family=UUID.randomUUID(),resident=UUID.randomUUID(),actor=UUID.randomUUID();
    private final Map<Position,String> originals=new HashMap<>();private final List<String> messages=new ArrayList<>();private final List<Component> buttons=new ArrayList<>();
    private World world;private Path data;private Position need;private PopulationSession session;private PaperConstructionLibrary library;
    private PaperAutomaticHomePlanning runtime;private PaperHousingProjectControls controls;private PaperConstructionPreview route;private CompletableFuture<?> work;
    private Player player;private UUID project;private boolean authorized,manage;private int phase,ticks,largestBudget;private String diagnostic="";
    private PaperConstructionWorkSurvey workSurvey;private ConstructionWorkQueue.Work nativeWork;private Position workCell;
    private PaperHomeSiteSurvey changedSurvey;private int surveyTicks;private Position changedCell;
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");
        check(root.getFileName().toString().startsWith("civilizations-p4-preview-")&&root.getParent().toRealPath().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath()),"Fresh host-temporary fixture only");
        authorized=true;data=root.resolve("automatic-home-probe");
        new BukkitRunnable(){public void run(){try{
            check(++ticks<1200&&getServer().getOnlinePlayers().isEmpty(),"Bounded empty disposable fixture");
            if(runtime!=null)runtime.run();if(controls!=null)controls.run();if(work==null||work.isDone())step();
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_AUTOMATIC_HOME_FAIL phase="+phase,failure);cleanup();cancel();getServer().shutdown();}
        if(phase==99){cancel();getServer().shutdown();}}}.runTaskTimer(this,1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"Automatic home fixture guard failed",failure);if(authorized)getServer().shutdown();}}
    private void step()throws Exception{
        if(work!=null)work.join();
        switch(phase){
            case 0->{prepare();seed();session=newSession();work=session.start(false,Set.of(world.getUID()));phase=1;}
            case 1->{var settings=library.homePlanning();check(settings.enabled(),"Fresh shipped config enables dry planning");
                runtime=new PaperAutomaticHomePlanning(new PaperAutomaticHomePlanning.Owner(){
                    public CompletableFuture<HousingPlanningJobs.Page> page(UUID after){return session.housingPlanningJobs(after,8,List.copyOf(library.templates().values()),library.housing(),DwellingInspection.Policy.prototype(),settings.maximumActiveHomes());}
                    public CompletableFuture<HousingProjectPreparation.Receipt> prepare(HousingPlanningJobs.Job job,PaperHomeSiteSurvey.Frame frame,long now){
                        var request=new HousingProjectPreparation.Request(new HousingProjects.Proposal(job.proposal(),job.household(),now),job.village(),job.world(),job.need(),frame.search(),frame.survey(),frame.terrain());
                        return session.prepareLimitedHousingProject(request,List.copyOf(library.templates().values()),library.housing(),library.land(),DwellingInspection.Policy.prototype(),settings.maximumActiveHomes());
                    }
                },settings,(job,now)->{var source=new PaperHomeSiteSurvey(job.world(),job.need(),settings.searchRadius(),job.templateHeight(),now);
                    return(budget,time)->{largestBudget=Math.max(largestBudget,budget);return source.advance(budget,time);};
                },System::currentTimeMillis,s->diagnostic=s,getLogger());work=null;phase=2;}
            case 2->{if(!diagnostic.contains("retained unused project"))return;runtime.close();runtime=null;
                work=session.housingProjectStatus(town,world.getUID(),DwellingInspection.Policy.prototype());phase=3;}
            case 3->{var report=(HousingProjectStatus.Report)work.join();check(report.items().size()==1,"One capped automatic project");var item=report.items().getFirst();project=item.family().proposal().draft();
                check(item.unused()&&item.siteHeld()&&item.paidOrders()==0&&item.intents()==0,"Real dry site/family proposal with no paid or physical work");
                work=session.constructionWorkPage(null,8);phase=10;}
            case 10->{var page=(ConstructionWorkQueue.Page)work.join();check(page.projects().size()==1,"Actual bounded saved-work page contains dry home");nativeWork=page.projects().getFirst();
                check(nativeWork.draft().id().equals(project)&&nativeWork.state()==ConstructionWorkQueue.State.WAIT_MATERIALS,"Dry work waits for real materials");
                check(nativeWork.draft().template().id().equals(expectedTemplate()),"Automatic source chooses a fitting whole-family home");
                workSurvey=new PaperConstructionWorkSurvey(nativeWork,System.currentTimeMillis());work=null;phase=11;}
            case 11->{var frame=workSurvey.advance(128,System.currentTimeMillis());if(frame.isEmpty())return;
                check(frame.get().authority().siteCleared(),"Actual two-pass saved-work survey verifies unchanged empty site");
                workCell=nativeWork.draft().origin();check(world.getBlockAt(workCell.x(),workCell.y(),workCell.z()).getType()==Material.AIR,"Unbuilt home cell is exact air");
                originals.putIfAbsent(workCell,world.getBlockAt(workCell.x(),workCell.y(),workCell.z()).getBlockData().getAsString());world.getBlockAt(workCell.x(),workCell.y(),workCell.z()).setType(Material.COBBLESTONE,false);
                workSurvey=new PaperConstructionWorkSurvey(nativeWork,System.currentTimeMillis());phase=12;}
            case 12->{var frame=workSurvey.advance(128,System.currentTimeMillis());if(frame.isEmpty())return;
                check(!frame.get().authority().siteCleared(),"Matching but unjournaled native block never becomes Civ property or overwrite permission");
                world.getBlockAt(workCell.x(),workCell.y(),workCell.z()).setBlockData(getServer().createBlockData(originals.get(workCell)),false);phase=13;}
            case 13->{
                player=(Player)java.lang.reflect.Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
                    case "getUniqueId"->actor;case "getWorld"->world;case "hasPermission"->a[0].equals(PaperConstructionPreview.PERMISSION)||manage&&a[0].equals(PaperHousingProjectControls.MANAGE);
                    case "sendMessage"->{if(a[0] instanceof String text)messages.add(text);else buttons.add((Component)a[0]);yield null;}default->throw new IllegalStateException("Unexpected player access "+m.getName());});
                controls=new PaperHousingProjectControls(new PaperHousingProjectControls.Owner(){
                    public CompletableFuture<HousingProjectStatus.Report> status(UUID v,UUID w){return session.housingProjectStatus(v,w,DwellingInspection.Policy.prototype());}
                    public CompletableFuture<HousingProjectStatus.Item> inspect(UUID d,UUID w){return session.housingProjectInspection(d,w,DwellingInspection.Policy.prototype());}
                    public CompletableFuture<HousingProjects.Binding> retire(UUID o,UUID d,UUID w,long at){return session.retireUnusedHousingProject(o,d,w,at);}
                    public CompletableFuture<ConstructionSupplyOrders.VillageFunding> funding(UUID d,UUID w){return session.constructionVillageFunding(d,w,library.funding(),library.supplies());}
                },id->player,System::currentTimeMillis,getLogger());route=new PaperConstructionPreview(library);route.configureProjects(controls);
                command("funding",project.toString());work=null;phase=9;}
            case 9->{if(messages.stream().noneMatch(s->s.startsWith("Project request could not finish;")))return;
                check(messages.stream().anyMatch(s->s.contains("Current saved owner clock required")||s.contains("current complete fed population assessment")||s.contains("Real village treasury required")),"No current food/treasury context becomes invented funding");
                check(session.status().failure()==null,"Expected funding hold never poisons population failure status");
                command("cancelproject",project.toString());check(messages.getLast().contains("do not have permission"),"Read permission cannot cancel");
                manage=true;command("cancelproject",project.toString());work=null;phase=4;}
            case 4->{if(messages.stream().noneMatch(s->s.startsWith("Unused cancellation preview:")))return;
                var payload=(ClickEvent.Payload.Text)buttons.getLast().clickEvent().payload();var parts=payload.value().split(" ");check(parts[2].equals("confirmcancel"),"Real separate confirmation button");
                command("confirmcancel",parts[3]);phase=5;}
            case 5->{if(messages.stream().noneMatch(s->s.startsWith("Unused home project cancelled:")))return;
                controls.close();controls=null;session.close();session=newSession();work=session.start(false,Set.of(world.getUID()));phase=6;}
            case 6->{work=session.housingProjectStatus(town,world.getUID(),DwellingInspection.Policy.prototype());phase=7;}
            case 7->{var item=((HousingProjectStatus.Report)work.join()).items().getFirst();
                check(item.family().proposal().draft().equals(project)&&!item.family().active()&&!item.siteHeld(),"Exact cancelled family/site history survives owner restart");
                session.close();session=null;
                try(var c=DriverManager.getConnection("jdbc:sqlite:"+data.resolve("civilizations.db"));var s=c.createStatement()){
                    for(String table:List.of("construction_supply_orders","construction_block_journal","construction_stage_work"))try(var r=s.executeQuery("SELECT count(*) FROM "+table)){check(r.getInt(1)==0,"No goods, work or native placement manufactured");}
                    try(var r=s.executeQuery("SELECT count(*) FROM residents WHERE status='ALIVE'")){check(r.getInt(1)==familyMembers(),"Original whole family retained");}
                    try(var r=s.executeQuery("SELECT capacity FROM household_capacity")){check(r.getInt(1)==0,"Proposal grants no housing capacity");}
                    try(var r=s.executeQuery("PRAGMA integrity_check")){check(r.getString(1).equals("ok"),"SQLite integrity");}
                    try(var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Foreign keys");}
                }
                check(largestBudget==128,"Bounded native survey budget");
                check(!new PaperHomeSiteSurvey(UUID.randomUUID(),need,12,System.currentTimeMillis()).advance(128,System.currentTimeMillis()).orElseThrow().survey().complete(),"Absent world yields unknown, never loads or approves");
                changedSurvey=new PaperHomeSiteSurvey(world.getUID(),need,12,System.currentTimeMillis());work=null;phase=8;}
            case 8->{var frame=changedSurvey.advance(128,System.currentTimeMillis());surveyTicks++;
                if(surveyTicks==30){check(frame.isEmpty(),"First pass has no site authority");changedCell=need.plus(new Position(0,2,0));
                    originals.put(changedCell,world.getBlockAt(changedCell.x(),changedCell.y(),changedCell.z()).getBlockData().getAsString());
                    check(world.getBlockAt(changedCell.x(),changedCell.y(),changedCell.z()).getType().isAir(),"Tracked disposable air test cell");world.getBlockAt(changedCell.x(),changedCell.y(),changedCell.z()).setType(Material.COBBLESTONE,false);}
                if(frame.isEmpty())return;check(surveyTicks>30&&!frame.orElseThrow().survey().complete(),"Changed second-pass evidence remains unknown");cleanup();
                getLogger().info("P4_AUTOMATIC_HOME_PASS: actual loaded-only two-pass site survey; saved-work queue and native partial-work survey; unjournaled matching block refused; deterministic real SQL family/plan/site preparation; capped one project; funding report refuses missing current financial context; operator preview and real confirmed unused cancellation; restart retains released site/cancelled identity; resident and zero housing capacity preserved; no paid stock or physical intents; absent world and changed second-pass evidence held unknown; one path marker and two air test cells restored;128 cell advance limit");phase=99;}
            default->throw new IllegalStateException("Unknown phase");
        }
    }
    private void command(String...parts){var args=new ArrayList<>(List.of("build"));args.addAll(List.of(parts));route.onCommand(player,null,"civ",args.toArray(String[]::new));}
    private PopulationSession newSession(){var s=PopulationSession.Settings.prototype();return new PopulationSession(data.toFile(),getLogger(),
            new PopulationSession.Settings(42,s.millisPerMonth(),s.maximumOfflineMonths(),s.monthsPerBatch(),s.population(),s.migration(),s.recognition(),s.enrollment()),System::currentTimeMillis);}
    private void prepare()throws Exception{
        var host=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");check(host!=null&&host.isEnabled(),"Production plugin enabled");library=PaperConstructionLibrary.load(host);
        check(getServer().getPluginManager().getPermission(PaperHousingProjectControls.MANAGE).getDefault()==org.bukkit.permissions.PermissionDefault.OP,"Installed operator management permission");
        world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();int x=spawn.getBlockX()+12,z=spawn.getBlockZ()+12;
        // Explicitly prepare four chunks in this new, marked temporary world only.
        // The production simulation/survey never calls getChunkAt, loadChunk or adds a ticket.
        for(int cx=(x-12)>>4;cx<=(x+12)>>4;cx++)for(int cz=(z-12)>>4;cz<=(z+12)>>4;cz++){
            if(!world.isChunkLoaded(cx,cz))world.getChunkAt(cx,cz);
            check(world.isChunkLoaded(cx,cz),"Prepared disposable chunk must be loaded before observation");world.addPluginChunkTicket(cx,cz,this);
        }
        int y=world.getHighestBlockYAt(x,z);need=new Position(x,y+1,z);var at=new Position(x,y,z); // Real discovered bed-level village center.
        originals.put(at,world.getBlockAt(x,y,z).getBlockData().getAsString());world.getBlockAt(x,y,z).setType(Material.DIRT_PATH,false);
    }
    private void seed()throws Exception{
        var db=new DatabaseManager(data.toFile(),getLogger());try{db.initialize();db.saveSettlement(new Settlement(town,"Automatic home fixture","fixture",need.x(),need.y(),need.z(),1,1,0));
            try(var s=db.getConnection().prepareStatement("INSERT INTO observed_worlds VALUES(?,'fixture')")){s.setString(1,world.getUID().toString());s.executeUpdate();}
            try(var s=db.getConnection().prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,town.toString());s.setString(2,world.getUID().toString());s.executeUpdate();}
            db.getPopulationRepository().insertHousehold(new Household(family,town,"Fixture household"));db.getPopulationRepository().insertResident(new Resident(resident,"Fixture adult",town,family,240,Resident.Status.ALIVE));
            for(int child=1;child<familyMembers();child++)db.getPopulationRepository().insertResident(new Resident(UUID.randomUUID(),"Fixture child "+child,town,family,5,Resident.Status.ALIVE));
            try(var s=db.getConnection().prepareStatement("INSERT INTO household_capacity VALUES(?,0)")){s.setString(1,family.toString());s.executeUpdate();}
            db.getDemographicRepository().initializeClock(42,PopulationPolicy.prototype(),MigrationPolicy.prototype());
        }finally{db.close();}
    }
    private void cleanup(){if(runtime!=null){runtime.close();runtime=null;}if(controls!=null){controls.close();controls=null;}if(session!=null){session.close();session=null;}
        if(world!=null){originals.forEach((p,state)->world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(state),false));originals.clear();world.removePluginChunkTickets(this);}}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    @Override public void onDisable(){cleanup();}
}
