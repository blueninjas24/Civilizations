import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;

/** Real native surveys + synthetic player + actual owner-thread confirmation in a separate disposable database. */
public final class WorkplaceDonationProbe extends JavaPlugin {
    private final ExecutorService owner=Executors.newSingleThreadExecutor();
    private final Map<Position,String> originals=new TreeMap<>();private boolean authorized,restored,previewShown,confirmed;private World world;
    private final UUID town=UUID.randomUUID(),person=UUID.randomUUID(),donor=UUID.randomUUID(),business=UUID.randomUUID();
    private volatile UUID token;private volatile BuildingLayout donated;private DatabaseManager db;private WorkplaceDonationRequests requests;
    private PaperWorkplaceDonation controller;private BuildingLayout.Bounds bounds;private Position entrance,marker;
    private <T> CompletableFuture<T> onOwner(Callable<T> task){return CompletableFuture.supplyAsync(()->{try{return task.call();}catch(Exception failure){throw new CompletionException(failure);}},owner);}
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        require(root.getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath())&&root.getFileName().toString().startsWith("civilizations-p4-preview-"),"Host temporary fixture required");
        require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");authorized=true;
        world=getServer().getWorlds().getFirst();UUID worldId=world.getUID();String worldName=world.getName();var chunk=world.getLoadedChunks()[0];world.addPluginChunkTicket(chunk.getX(),chunk.getZ(),this);
        int x=(chunk.getX()<<4)+4,z=(chunk.getZ()<<4)+4,y=world.getHighestBlockYAt(x,z);var origin=new Position(x,y,z);marker=origin.plus(new Position(3,1,2));entrance=origin.plus(new Position(0,1,2));bounds=new BuildingLayout.Bounds(origin,origin.plus(new Position(4,3,4)));
        for(var at:DwellingInspection.requiredCells(new BuildingLayout(UUID.randomUUID(),town,worldId,BuildingLayout.Kind.WORKPLACE,bounds,bounds,entrance,List.of(),List.of()))){
            var block=world.getBlockAt(at.x(),at.y(),at.z());require(at.y()==y?block.getType()==Material.GRASS_BLOCK:block.getType()==Material.AIR,"Empty fixture required");originals.put(at,block.getBlockData().getAsString());
        }
        for(var at:originals.keySet())world.getBlockAt(at.x(),at.y(),at.z()).setType(at.y()==y||at.y()==y+3?Material.COBBLESTONE:Material.AIR,false);world.getBlockAt(marker.x(),marker.y(),marker.z()).setType(Material.SMITHING_TABLE,false);
        var setup=onOwner(()->{
            db=new DatabaseManager(root.resolve("workplace-donation-fixture").toFile(),getLogger());db.initialize();db.saveSettlement(new Settlement(town,"Native fixture",worldName,x,y+1,z,1,0,0));db.getVillageIdentityRepository().observeWorld(worldId,worldName);db.getVillageIdentityRepository().bindSettlement(town,worldId);
            db.getPopulationRepository().insertResident(new Resident(person,"Fixture owner",town,null,300,Resident.Status.ALIVE));
            var site=new BusinessSite(UUID.randomUUID(),town,worldId,marker.x(),marker.y(),marker.z(),0,"Explicit disposable fixed marker");var firm=new Business(business,town,"Fixture smith",person,person,"blacksmith",site.id(),2,0);var requirement=new JobRequirements(0,Map.of());new BusinessRepository(db.getConnection()).found(new BusinessFounding(firm,site,requirement,requirement,0,"Explicit disposable founding"));
            try(var s=db.getConnection().prepareStatement("INSERT INTO business_workplace_anchors VALUES(?,?,?,?,?,'SMITHING_TABLE',0,0)")){s.setString(1,business.toString());s.setString(2,worldId.toString());s.setInt(3,marker.x());s.setInt(4,marker.y());s.setInt(5,marker.z());s.executeUpdate();}
            requests=new WorkplaceDonationRequests(db.getConnection(),BuildingConsentRequests.Policy.prototype());return true;
        });
        Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(proxy,method,args)->switch(method.getName()){
            case "getUniqueId"->donor;case "getWorld"->world;case "getLocation"->new Location(world,entrance.x(),entrance.y(),entrance.z());case "hasPermission"->true;
            case "sendMessage"->{if(args[0] instanceof String text){if(text.startsWith("Workplace donation preview:"))previewShown=true;if(text.startsWith("Workplace donation receipt confirmed:"))confirmed=true;}yield null;}
            default->throw new AssertionError("Unexpected synthetic player call: "+method.getName());});
        new BukkitRunnable(){int ticks,phase;CompletableFuture<Boolean> verified;public void run(){try{
            require(++ticks<800,"Donation flow timeout");if(!setup.isDone())return;setup.join();
            if(controller==null){
                controller=new PaperWorkplaceDonation(new PaperWorkplaceDonation.Owner(){
                    public CompletableFuture<BusinessFounding> target(UUID id,UUID world){return onOwner(()->new BusinessRepository(db.getConnection()).findFounding(id).orElseThrow());}
                    public CompletableFuture<WorkplaceDonationRequests.Preview> preview(UUID actor,UUID world,UUID firm,BuildingLayout layout,WorkplaceInspection.Evidence evidence){return onOwner(()->{var result=requests.preview(new BuildingConsentRequests.Context(actor,world,true,false,false,Set.of(world)),firm,layout,evidence,WorkplaceInspection.Policy.prototype(),System.currentTimeMillis());token=result.token();donated=layout;return result;});}
                    public CompletableFuture<WorkplaceDonation.Receipt> confirm(UUID actor,UUID world,UUID token,WorkplaceInspection.Evidence evidence){return onOwner(()->requests.confirm(new BuildingConsentRequests.Context(actor,world,true,false,false,Set.of(world)),token,true,evidence,System.currentTimeMillis()));}
                    public void cancel(UUID actor){onOwner(()->{requests.cancel(actor);return true;});}
                },p->bounds,id->player,(layout,at,now)->new PaperWorkplaceSurvey(layout,at,now)::advance,System::currentTimeMillis,getLogger());
                controller.onCommand(player,null,"civ",new String[]{"building","workplace",business.toString()});
            }
            controller.run();
            if(phase==0&&previewShown){controller.onCommand(player,null,"civ",new String[]{"building","confirmworkplace",token.toString(),"owned"});phase=1;}
            if(phase==1&&confirmed){
                verified=onOwner(()->{
                    var registry=new BuildingRegistry(db.getConnection());require(registry.hasDonationPermission(donated.id()),"Consent must persist");
                    require(new BusinessOperations(db.getConnection(),BusinessOperationPolicy.disabled()).evaluate(business).permitted(),"Existing firm retains ordinary eligibility");
                    registry.reclaim(UUID.randomUUID(),donated.id(),BuildingRegistry.Authorization.playerDeclaration(donor,true),token,System.currentTimeMillis());
                    require(!new BusinessOperations(db.getConnection(),BusinessOperationPolicy.disabled()).evaluate(business).permitted(),"Reclamation must block formal premises use");
                    // Read the saved pair directly; the UI has correctly discarded its completed expiring preview.
                    try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT count(*) FROM business_premises")){require(r.getInt(1)==1,"One retained receipt required");}
                    for(var table:List.of("treasury_entries","industry_stores","construction_drafts","dwelling_assignments"))try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){require(r.getInt(1)==0,"No fabricated "+table);}
                    return true;
                });phase=2;
            }
            if(phase==2&&verified.isDone()){
                verified.join();previewShown=false;confirmed=false;
                controller.onCommand(player,null,"civ",new String[]{"building","workplace",business.toString(),donated.id().toString()});phase=3;
            }
            if(phase==3&&previewShown){controller.onCommand(player,null,"civ",new String[]{"building","confirmworkplace",token.toString(),"owned"});phase=4;}
            if(phase==4&&confirmed){
                verified=onOwner(()->{
                    require(new BusinessOperations(db.getConnection(),BusinessOperationPolicy.disabled()).evaluate(business).permitted(),"Reviewed re-donation must restore eligibility");
                    try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT count(*),sum(released_ms IS NULL) FROM business_premises")){require(r.getInt(1)==2&&r.getInt(2)==1,"One current and one retained old hold required");}
                    for(var table:List.of("treasury_entries","industry_stores","construction_drafts","dwelling_assignments"))try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){require(r.getInt(1)==0,"No fabricated "+table);}
                    return true;
                });phase=5;
            }
            if(phase==5&&verified.isDone()){verified.join();cancel();controller.close();restore();getLogger().info("P4_WORKPLACE_DONATION_PASS: synthetic player preview and fresh confirmation atomically save use/hold; reclaim pauses; explicit re-donation restores with one current and one historical hold; no goods/cash/capacity created; all100 terrain cells restored");getServer().shutdown();}
        }catch(Throwable failure){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_WORKPLACE_DONATION_FAIL",failure);try{restore();}finally{getServer().shutdown();}}}}.runTaskTimer(this,1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"P4_WORKPLACE_DONATION_SETUP_FAIL",failure);try{restore();}finally{if(authorized)getServer().shutdown();}}}
    private void restore(){if(!authorized||restored||world==null)return;for(var e:originals.entrySet()){var at=e.getKey();require(world.isChunkLoaded(at.x()>>4,at.z()>>4),"Cleanup cannot load chunks");world.getBlockAt(at.x(),at.y(),at.z()).setBlockData(getServer().createBlockData(e.getValue()),false);}restored=true;world.removePluginChunkTickets(this);}
    @Override public void onDisable(){if(controller!=null)controller.close();restore();onOwner(()->{if(db!=null)db.close();return true;});owner.shutdown();}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
