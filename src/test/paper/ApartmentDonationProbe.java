import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.settlement.VillageObservation;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual Paper reads, explicit editor/controller and private SQL only in a marked empty disposable fixture. */
public final class ApartmentDonationProbe extends JavaPlugin {
    private final Map<Position,String> originals=new TreeMap<>();
    private final UUID actor=UUID.randomUUID();private final List<String> messages=new ArrayList<>();
    private World world;private PopulationSession session;private PaperHomeDonation controls;private Player player;
    private PaperRenovationConsent renovations;private final List<net.kyori.adventure.text.Component> buttons=new ArrayList<>();
    private PaperHousingCandidates housingReports;private boolean reportPermission;
    private PaperHousingProjectControls projectControls;
    private Bounds selected,structure;private Position origin,here;private Path data;
    private CompletableFuture<?> work;private CompletableFuture<PopulationSession.HomeDonationPreview> preview;
    private BuildingLayout saved;private int phase,ticks;private boolean authorized;
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");
        authorized=true;data=root.resolve("apartment-donation-probe");
        new BukkitRunnable(){public void run(){try{
            check(++ticks<1200&&getServer().getOnlinePlayers().isEmpty(),"Bounded empty fixture");
            if(controls!=null)controls.run();if(renovations!=null)renovations.run();if(housingReports!=null)housingReports.run();if(projectControls!=null)projectControls.run();if(work==null||work.isDone())step();
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_APARTMENT_FAIL phase="+phase,failure);cleanup();cancel();getServer().shutdown();}
        if(phase==99){cancel();getServer().shutdown();}}}.runTaskTimer(this,1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"Apartment fixture guard failed",failure);if(authorized)getServer().shutdown();}}
    private void step()throws Exception{
        if(work!=null)work.join();
        switch(phase){
            case 0->{prepare();session=newSession();work=session.start(false,Set.of(world.getUID()));phase=1;}
            case 1->{var bed=new VillageObservation.Position(origin.x()+1,origin.y()+1,origin.z()+3);
                var second=new VillageObservation.Position(origin.x()+4,origin.y()+1,origin.z()+3);
                work=session.observe(new VillageObservation(world.getUID(),"fixture",System.currentTimeMillis(),true,List.of(bed,second),
                        List.of(new VillageObservation.Person(UUID.randomUUID(),bed,true,"Fixture adult"),
                                new VillageObservation.Person(UUID.randomUUID(),second,true,"Second fixture adult"))));phase=2;}
            case 2->{UUID town=session.settlementNames().keySet().iterator().next();
                player=(Player)java.lang.reflect.Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
                    case "getUniqueId"->actor;case "getWorld"->world;case "hasPermission"->a[0].equals(PaperHomeDonation.PERMISSION)||a[0].equals(PaperRenovationConsent.PERMISSION)||reportPermission&&a[0].equals(PaperConstructionPreview.PERMISSION);
                    case "getLocation"->new Location(world,here.x(),here.y(),here.z());
                    case "sendMessage"->{if(a[0] instanceof String s)messages.add(s);else if(a[0] instanceof net.kyori.adventure.text.Component component)buttons.add(component);yield null;}
                    default->throw new UnsupportedOperationException(m.getName());});
                controls=new PaperHomeDonation(new PaperHomeDonation.Owner(){
                    public CompletableFuture<PopulationSession.HomeDonationPreview> preview(UUID a,UUID w,UUID b,UUID v,Bounds bounds,Position e,DwellingInspection.Observation o){throw new AssertionError("Apartment must not use single-home inference");}
                    public CompletableFuture<PopulationSession.HomeDonationPreview> apartments(UUID a,UUID w,ApartmentDeclaration.Draft d,DwellingInspection.Observation o){
                        return preview=session.previewApartmentDonation(a,w,d,o,DwellingInspection.Policy.prototype(),true);}
                    public CompletableFuture<BuildingRegistry.Consent> confirm(UUID a,UUID w,UUID token){return session.confirmBuildingConsent(a,w,token,true,true,false,false);}
                    public void cancel(UUID a){session.cancelBuildingConsent(a);}
                },p->selected,id->player,(w,b,t)->PaperDwellingSurvey.forHomeDeclaration(w,b,t)::advance,System::currentTimeMillis,getLogger());
                selected=structure;here=origin.plus(new Position(2,1,0));command("apartments",town.toString());
                selected=box(0,2,2,5);here=origin.plus(new Position(2,1,2));command("unit","west");
                selected=box(3,2,5,5);here=origin.plus(new Position(3,1,2));command("unit","east");
                selected=box(0,0,5,1);command("shared");command("reviewapartments");work=null;phase=3;}
            case 3->{if(preview==null||!preview.isDone()||messages.stream().noneMatch(s->s.startsWith("Home donation preview:")))return;
                var p=preview.join();check(p.review().passes()&&p.review().layout().units().size()==2&&p.review().assessment().accessibleBeds()==2,"Real two-unit private/shared review");
                saved=p.review().layout();check(messages.stream().noneMatch(s->s.startsWith("Home donation permission saved")),"Preview grants no permission");
                command("confirmhome",p.consent().token().toString(),"owned");phase=4;}
            case 4->{if(messages.stream().noneMatch(s->s.startsWith("Home donation permission saved")))return;
                work=session.buildingInspectionLayout(saved.id(),actor,false,world.getUID());phase=5;}
            case 5->{check(((BuildingRegistry.Entry)work.join()).layout().equals(saved),"Exact reviewed unit identities and shared geometry saved");
                renovations=new PaperRenovationConsent(new PaperRenovationConsent.Owner(){
                    public CompletableFuture<RenovationConsentRequests.Preview> preview(UUID a,UUID w,UUID b,BuildingRenovations.Action action,boolean permitted,boolean admin){return session.previewRenovation(a,w,b,action,permitted,admin);}
                    public CompletableFuture<BuildingRenovations.Receipt> confirm(UUID a,UUID w,UUID token,boolean permitted,boolean admin){return session.confirmRenovation(a,w,token,permitted,admin);}
                    public void cancel(UUID a){session.cancelRenovation(a);}
                },id->player,System::currentTimeMillis,getLogger());
                renovationCommand("allowrenovation",saved.id().toString());work=null;phase=8;}
            case 8->{if(messages.stream().noneMatch(s->s.startsWith("Separate renovation preview: GRANT")))return;confirmRenovation();phase=9;}
            case 9->{if(messages.stream().noneMatch(s->s.startsWith("Renovation decision saved: GRANT")))return;
                work=session.housingSummary(saved.id(),actor,false,world.getUID());phase=10;}
            case 10->{check(((List<?>)work.join()).stream().anyMatch(s->s.toString().contains("renovations separately allowed")),"Separate grant visible in saved housing report");
                renovationCommand("denyrenovation",saved.id().toString());work=null;phase=11;}
            case 11->{if(messages.stream().noneMatch(s->s.startsWith("Separate renovation preview: REVOKE")))return;confirmRenovation();phase=12;}
            case 12->{if(messages.stream().noneMatch(s->s.startsWith("Renovation decision saved: REVOKE")))return;
                work=session.housingSummary(saved.id(),actor,false,world.getUID());phase=13;}
            case 13->{check(((List<?>)work.join()).stream().anyMatch(s->s.toString().contains("renovations separately denied")),"Revocation visible without changing donation");
                controls.close();controls=null;renovations.close();renovations=null;session.close();session=newSession();work=session.start(false,Set.of(world.getUID()));phase=6;}
            case 6->{work=session.buildingInspectionLayout(saved.id(),actor,false,world.getUID());phase=7;}
            case 7->{check(((BuildingRegistry.Entry)work.join()).layout().equals(saved),"Apartments survive SQL-session restart");
                var library=PaperConstructionLibrary.load((JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations"));
                housingReports=new PaperHousingCandidates((v,w)->session.housingCandidates(v,w,List.copyOf(library.templates().values()),library.housing(),DwellingInspection.Policy.prototype()).thenApply(r->r.describe()),id->player,System::currentTimeMillis,getLogger());
                var route=new PaperConstructionPreview(library);route.configureHousing(housingReports);
                route.onCommand(player,null,"civ",new String[]{"build","housing",saved.settlement().toString()});
                check(messages.getLast().contains("do not have permission"),"Housing planner remains operator-permission protected");
                reportPermission=true;route.onCommand(player,null,"civ",new String[]{"build","housing",saved.settlement().toString()});work=null;phase=14;}
            case 14->{if(messages.stream().noneMatch(s->s.startsWith("Housing candidates only:")))return;
                check(messages.stream().anyMatch(s->s.contains("no projects, assignments, spending")),"Actual SQL housing report states read-only scope");
                housingReports.close();housingReports=null;
                projectControls=new PaperHousingProjectControls(new PaperHousingProjectControls.Owner(){
                    public CompletableFuture<HousingProjectStatus.Report> status(UUID v,UUID w){return session.housingProjectStatus(v,w,DwellingInspection.Policy.prototype());}
                    public CompletableFuture<HousingProjectStatus.Item> inspect(UUID d,UUID w){return session.housingProjectInspection(d,w,DwellingInspection.Policy.prototype());}
                    public CompletableFuture<HousingProjects.Binding> retire(UUID o,UUID d,UUID w,long at){return session.retireUnusedHousingProject(o,d,w,at);}
                },id->player,System::currentTimeMillis,getLogger());
                var library=PaperConstructionLibrary.load((JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations"));
                var route=new PaperConstructionPreview(library);route.configureProjects(projectControls);
                route.onCommand(player,null,"civ",new String[]{"build","cancelproject",saved.id().toString()});
                check(messages.getLast().contains("do not have permission"),"Read-only preview permission cannot cancel projects");
                route.onCommand(player,null,"civ",new String[]{"build","projects",saved.settlement().toString()});phase=15;}
            case 15->{if(messages.stream().noneMatch(s->s.startsWith("Saved home projects: 0")))return;
                projectControls.close();projectControls=null;session.close();session=null;
                try(var c=DriverManager.getConnection("jdbc:sqlite:"+data.resolve("civilizations.db"));var s=c.createStatement()){
                    try(var r=s.executeQuery("SELECT count(*) FROM building_units")){check(r.getInt(1)==2,"Exactly two saved units");}
                    try(var r=s.executeQuery("SELECT count(*) FROM building_renovation_events")){check(r.getInt(1)==2,"Separate grant and revocation history retained");}
                    try(var r=s.executeQuery("SELECT count(*) FROM construction_drafts")){check(r.getInt(1)==0,"Housing command creates no draft");}
                    try(var r=s.executeQuery("SELECT count(*) FROM housing_project_bindings")){check(r.getInt(1)==0,"Housing command reserves no family");}
                    try(var r=s.executeQuery("PRAGMA integrity_check")){check(r.getString(1).equals("ok"),"SQLite integrity");}
                    try(var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Foreign keys");}
                }
                cleanup();getLogger().info("P4_APARTMENT_PASS: donor injected actor without admin/protection; explicit private units/shared corridor; real beds/access/roof; preview grants nothing; explicit frozen confirmation; separate renovation grant/revoke confirmations and saved status; two stable saved units and two consent receipts survive SQL-session restart; routed housing/project reports read real SQL and start no project; read permission cannot cancel;144 cells restored");phase=99;}
            default->throw new IllegalStateException("Unknown phase");
        }
    }
    private PopulationSession newSession(){return new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);}
    private void command(String...args){var all=new ArrayList<>(List.of("building"));all.addAll(List.of(args));controls.onCommand(player,null,"civ",all.toArray(String[]::new));}
    private void renovationCommand(String...args){var all=new ArrayList<>(List.of("building"));all.addAll(List.of(args));renovations.onCommand(player,null,"civ",all.toArray(String[]::new));}
    private void confirmRenovation(){var click=(net.kyori.adventure.text.event.ClickEvent.Payload.Text)buttons.getLast().clickEvent().payload();
        var parts=click.value().split(" ");check(parts[2].equals("confirmrenovation"),"Actual separate confirmation button");renovationCommand(parts[2],parts[3]);}
    private Bounds box(int x1,int z1,int x2,int z2){return new Bounds(origin.plus(new Position(x1,0,z1)),origin.plus(new Position(x2,3,z2)));}
    private void prepare()throws Exception{
        var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");check(plugin!=null&&plugin.isEnabled(),"Production plugin enabled");
        check(getServer().getPluginManager().getPermission(PaperRenovationConsent.PERMISSION).getDefault()==org.bukkit.permissions.PermissionDefault.TRUE,"Installed renovation permission default");
        check(getServer().getPluginManager().getPermission(PaperHousingProjectControls.MANAGE).getDefault()==org.bukkit.permissions.PermissionDefault.OP,"Project cancellation permission defaults to operators");
        world=getServer().getWorlds().getFirst();var spawn=world.getSpawnLocation();int x=spawn.getBlockX()+4,z=spawn.getBlockZ()+4;
        for(int cx=x>>4;cx<=(x+5)>>4;cx++)for(int cz=z>>4;cz<=(z+5)>>4;cz++){check(world.isChunkLoaded(cx,cz),"Loaded fixture only");world.addPluginChunkTicket(cx,cz,this);}
        origin=new Position(x,world.getHighestBlockYAt(x,z)+1,z);structure=box(0,0,5,5);
        var plan=PaperConstructionLibrary.load(plugin).templates().get("starter_home");
        for(int dx=0;dx<6;dx++)for(int y=0;y<4;y++)for(int dz=0;dz<6;dz++){
            var p=origin.plus(new Position(dx,y,dz));originals.put(p,world.getBlockAt(p.x(),p.y(),p.z()).getBlockData().getAsString());world.getBlockAt(p.x(),p.y(),p.z()).setType(Material.AIR,false);}
        for(var stage:plan.stages())for(var cell:stage.blocks().entrySet()){var p=origin.plus(cell.getKey());world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(cell.getValue()),false);}
        for(int dx:List.of(2,3))for(int dz:List.of(2,3)){var p=origin.plus(new Position(dx,1,dz));world.getBlockAt(p.x(),p.y(),p.z()).setType(Material.AIR,false);}
    }
    private void cleanup(){if(controls!=null){controls.close();controls=null;}if(renovations!=null){renovations.close();renovations=null;}if(housingReports!=null){housingReports.close();housingReports=null;}if(projectControls!=null){projectControls.close();projectControls=null;}if(session!=null){session.close();session=null;}
        if(!authorized||world==null)return;for(var e:originals.entrySet()){var p=e.getKey();check(world.isChunkLoaded(p.x()>>4,p.z()>>4),"Restore loaded-only");world.getBlockAt(p.x(),p.y(),p.z()).setBlockData(getServer().createBlockData(e.getValue()),false);}
        originals.clear();world.removePluginChunkTickets(this);}
    @Override public void onDisable(){cleanup();}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
