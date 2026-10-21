import com.blueninjas24.civilizations.application.PopulationSession;
import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Real Paper command adapter over a private synthetic paid save. Never accesses inventory or changes world blocks. */
public final class ConstructionRecoveryControlProbe extends JavaPlugin {
    private final ExecutorService owner=Executors.newSingleThreadExecutor();private final AtomicLong clock=new AtomicLong(System.currentTimeMillis());
    private final UUID actor=UUID.randomUUID(),family=UUID.randomUUID(),supplier=UUID.randomUUID();private UUID operation;private long submitted;
    private final List<String> messages=new ArrayList<>();private final List<Component> buttons=new ArrayList<>();
    private DatabaseManager database;private PopulationSession session;private Path folder;private World world;private ConstructionDraft draft;
    private EconomySettings economySettings;private IndustryRuntimePolicy industry;private ConstructionRecoveryReview.Quote quote;
    private PaperHousingProjectControls controls;private PaperConstructionPreview route;private Player player;private boolean manage=true,online=true,authorized;
    private CompletableFuture<?> pending;private CompletableFuture<ConstructionRecoveryReview.Result> accepted;private int phase,ticks,refundCalls,cancelCalls;
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        require(root.getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath())&&root.getFileName().toString().startsWith("civilizations-p4-preview-")&&Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")&&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty Host Temp loopback fixture required");
        authorized=true;world=getServer().getWorlds().getFirst();var plugin=(JavaPlugin)getServer().getPluginManager().getPlugin("Civilizations");
        draft=new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),world.getUID(),new Position(100,64,100),Transform.identity(),PaperConstructionLibrary.load(plugin).templates().get("starter_home"),1000);
        folder=getDataFolder().toPath().resolve("private-recovery");require(!Files.exists(folder),"Fresh private save required");
        pending=CompletableFuture.runAsync(()->{try{seed();}catch(Exception e){throw new CompletionException(e);}},owner);
        new BukkitRunnable(){@Override public void run(){try{if(++ticks>600)throw new IllegalStateException("Recovery probe timeout phase "+phase);if(controls!=null)controls.run();if(pending.isDone())advance();}
            catch(Throwable e){cancel();getLogger().log(java.util.logging.Level.SEVERE,"P4_RECOVERY_CONTROLS_FAIL",e);getServer().shutdown();}}}.runTaskTimer(this,1,1);
    }catch(Throwable e){getLogger().log(java.util.logging.Level.SEVERE,"P4_RECOVERY_CONTROLS_SETUP_FAIL",e);if(authorized)getServer().shutdown();}}
    private void advance()throws Exception{
        var result=pending.join();switch(phase){
            case 0->{newSession();phase=1;pending=session.start(false,Set.of(draft.world()));}
            case 1->{
                player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
                    case "getUniqueId"->actor;case "getWorld"->world;case "hasPermission"->a[0].equals(PaperConstructionPreview.PERMISSION)||manage&&a[0].equals(PaperHousingProjectControls.MANAGE);
                    case "sendMessage"->{if(a[0] instanceof String s)messages.add(s);else buttons.add((Component)a[0]);yield null;}default->throw new AssertionError("Unexpected player operation: "+m.getName());});
                controls=new PaperHousingProjectControls(new PaperHousingProjectControls.Owner(){
                    public CompletableFuture<HousingProjectStatus.Report> status(UUID v,UUID w){return session.housingProjectStatus(v,w,DwellingInspection.Policy.prototype());}
                    public CompletableFuture<HousingProjectStatus.Item> inspect(UUID d,UUID w){return session.housingProjectInspection(d,w,DwellingInspection.Policy.prototype());}
                    public CompletableFuture<HousingProjects.Binding> retire(UUID o,UUID d,UUID w,long time){cancelCalls++;return session.retireUnusedHousingProject(o,d,w,time);}
                    public CompletableFuture<List<String>> details(UUID d,UUID w){return session.constructionProjectInspection(d,w);}
                    public CompletableFuture<ConstructionRecoveryReview.Quote> recovery(UUID d,UUID w){return session.constructionRecoveryReview(d,w).thenApply(q->{quote=q;return q;});}
                    public CompletableFuture<ConstructionRecoveryReview.Result> recover(UUID o,ConstructionRecoveryReview.Quote q,long time){refundCalls++;operation=o;submitted=time;accepted=session.recoverConstructionSupplies(o,q.draft(),q.world(),q.action(),q.terms(),time);return accepted;}
                },id->online&&id.equals(actor)?player:null,clock::get,getLogger());
                route=new PaperConstructionPreview(new PaperConstructionLibrary(Map.of()));route.configureProjects(controls);
                manage=false;command("recoverproject",draft.id());require(messages.getLast().contains("permission"),"Read-only actor cannot request refund");
                require(route.onTabComplete(player,null,"civ",new String[]{"build","inspect"}).equals(List.of("inspectproject")),"Real Paper inspection routing/tab completion");
                command("inspectproject",draft.id());phase=2;pending=CompletableFuture.completedFuture(null);
            }
            case 2->{if(messages.stream().noneMatch(s->s.startsWith("Saved work:")))return;require(buttons.isEmpty()&&refundCalls==0,"Saved inspection is read-only");manage=true;command("recoverproject",draft.id());phase=3;}
            case 3->{if(buttons.isEmpty())return;require(quote.action()==ConstructionRecoveryReview.Action.RECALL_PENDING&&quote.refund()==2940&&refundCalls==0,"Actual original supplier terms previewed before save");
                var click=(ClickEvent.Payload.Text)buttons.getLast().clickEvent().payload();UUID token=UUID.fromString(click.value().split(" ")[3]);
                command("confirmcancel",token);require(cancelCalls==0,"Recovery token cannot cancel another action");command("confirmrecovery",token);command("confirmrecovery",token);require(refundCalls==1,"Exact confirmation submitted once");online=false;phase=4;}
            case 4->{if(!accepted.isDone())return;var saved=accepted.join();require(saved.refunded()==2940&&saved.orders()==3,"Actual saved real supplier refund");controls.run();controls.close();session.close();newSession();phase=5;pending=session.start(false,Set.of(draft.world()));}
            case 5->{phase=6;pending=session.recoverConstructionSupplies(operation,draft.id(),draft.world(),quote.action(),quote.terms(),submitted);}
            case 6->{require(((ConstructionRecoveryReview.Result)result).refunded()==2940,"Exact accepted refund replays after actual session restart");session.close();session=null;phase=7;pending=CompletableFuture.runAsync(()->{try{verify();}catch(Exception e){throw new CompletionException(e);}},owner);}
            case 7->{getLogger().info("P4_RECOVERY_CONTROLS_PASS: real Paper read-only inspection/routing and manage permission; concrete original supplier terms; action-bound explicit confirmation; one accepted save despite duplicate click/disconnect;3 actual refunds totaling2940; exact restart replay conserved treasury/stock; original site/family/history retained; both private actions and fixture used no world block or inventory operations");phase=8;getServer().shutdown();}
            default->{}
        }
    }
    private void command(String action,UUID id){route.onCommand(player,null,"civ",new String[]{"build",action,id.toString()});}
    private void newSession(){var p=PopulationSession.Settings.prototype();session=new PopulationSession(folder.toFile(),getLogger(),new PopulationSession.Settings(0,p.millisPerMonth(),p.maximumOfflineMonths(),p.monthsPerBatch(),p.population(),p.migration(),p.recognition(),p.enrollment()),clock::get,economySettings,FarmObservation.Policy.prototype());session.configureIndustryRuntime(industry);}
    private void seed()throws Exception{
        database=new DatabaseManager(folder.toFile(),getLogger());database.initialize();var c=database.getConnection();var town=draft.settlement();
        try(var s=c.prepareStatement("INSERT INTO settlements(id,name,world,center_x,center_y,center_z,discovered_at,population,beds) VALUES(?,'Private recovery fixture','test',0,64,0,1,0,0)")){s.setString(1,town.toString());s.executeUpdate();}
        database.getVillageIdentityRepository().observeWorld(draft.world(),"Private recovery fixture");try(var s=c.prepareStatement("INSERT INTO settlement_worlds VALUES(?,?)")){s.setString(1,town.toString());s.setString(2,draft.world().toString());s.executeUpdate();}
        new ConstructionDraftRepository(c).save(draft);new ConstructionSiteReservations(c).reserve(draft.id(),new ConstructionMaterials.Authority(draft.id(),draft.world(),clock.get(),true,true),clock.get());
        UUID ownerId=UUID.randomUUID(),builder=UUID.randomUUID();var people=database.getPopulationRepository();people.insertHousehold(new Household(family,town,"Private recovery beneficiary"));people.insertResident(new Resident(ownerId,"Private supplier",town,null,300,Resident.Status.ALIVE));people.insertResident(new Resident(builder,"Private beneficiary",town,family,300,Resident.Status.ALIVE));
        try(var s=c.prepareStatement("INSERT INTO household_capacity VALUES(?,0)")){s.setString(1,family.toString());s.executeUpdate();}
        try(var s=c.prepareStatement("INSERT INTO housing_project_bindings(draft_id,household_id,settlement_id,world_id,plan_hash,residents,previous_capacity,created_ms,cancel_operation,cancelled_ms) VALUES(?,?,?,?,?,1,0,1000,NULL,NULL)")){
            s.setString(1,draft.id().toString());s.setString(2,family.toString());s.setString(3,town.toString());s.setString(4,draft.world().toString());s.setString(5,draft.template().fingerprint());s.executeUpdate();}
        new TreasuryRepository(c).initialize(UUID.randomUUID(),town,100000,"Explicit disposable fixture funds");var site=new BusinessSite(UUID.randomUUID(),town,draft.world(),0,64,0,10,"Explicit fixture site");var company=new Business(supplier,town,"Fixture warehouse",ownerId,ownerId,"warehouse",site.id(),2,0);var requirements=new JobRequirements(216,Map.of());
        database.getBusinessRepository().found(new BusinessFounding(company,site,requirements,requirements,500,"Explicit fixture opportunity"));database.getBusinessRepository().hire(UUID.randomUUID(),supplier,ownerId,0);database.getIndustryRepository().openStore(supplier,256,Map.of(Commodity.STONE,1L,Commodity.TIMBER,1L,new Commodity("wool"),1L),0);
        try(var s=c.prepareStatement("UPDATE industry_stock SET units=50 WHERE business_id=?")){s.setString(1,supplier.toString());require(s.executeUpdate()==3,"Explicit finite fixture stock");}
        var bands=new EnumMap<CommodityAccounting.Condition,Integer>(CommodityAccounting.Condition.class);for(var b:CommodityAccounting.Condition.values())bands.put(b,100);
        economySettings=new EconomySettings(1,3,2,2,1,4,100,new CommodityAccounting.Policy(5000,12500,20000,3),Map.of(Commodity.FOOD,100L),Map.of(Commodity.FOOD,java.math.BigDecimal.ONE),bands);
        long now=clock.get();var economy=new EconomyRuntimeRepository(c,economySettings,FarmObservation.Policy.prototype(),216);economy.observe(new FarmObservation(town,draft.world(),now,true,0),now);economy.startOnline(now,Set.of(draft.world()));economy.pulse(now+1000,false);
        var p=IndustryPolicy.prototype();var transport=new IndustryPolicy(p.depositBatchesPerObservedOre(),p.batchesPerWorker(),p.siteBatchCapacity(),p.storageCapacity(),1,128,p.miningBundle(),p.storageSpacePerUnit());industry=new IndustryRuntimePolicy(true,1000,8,transport,Map.of(),WorkshopSupplyPolicy.disabled(),new WarehouseSupplyPolicy(true,16,16),ToolUpkeepPolicy.disabled());
        // Seed actual paid pending order history for this UI-only fixture; no native work is authorized or attempted.
        require(new ConstructionVillageProcurement(c,ConstructionBudgetPolicy.prototype(),ConstructionSupplyPolicy.prototype(),industry,BusinessOperationPolicy.prototype()).purchaseRemaining(draft.id(),new ConstructionMaterials.Authority(draft.id(),draft.world(),now+1000,true,true),now+1001,Set.of(draft.world())).purchases().size()==3,"Actual public fixture purchases");clock.set(now+1001);database.close();database=null;
    }
    private void verify()throws Exception{
        database=new DatabaseManager(folder.toFile(),getLogger());database.initialize();var c=database.getConnection();require(new TreasuryRepository(c).load(draft.settlement()).orElseThrow().hundredths()==99500,"Original construction payment refunded exactly; real warehouse founding expense retained");
        var store=database.getIndustryRepository().store(supplier).orElseThrow();require(store.hundredths()==0&&store.units().values().stream().allMatch(n->n==50),"Original finite supplier goods/cash restored once");
        require(new HousingProjects(c).find(draft.id()).orElseThrow().active()&&new ConstructionSiteReservations(c).find(draft.id()).orElseThrow().active(),"Recovery does not silently cancel family/site");
        for(var table:List.of("construction_block_journal","construction_stage_work","civ_built_buildings"))try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){require(r.getInt(1)==0,"No invented native work/housing");}
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA integrity_check")){require(r.getString(1).equals("ok"),"Healthy private DB");}try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){require(!r.next(),"Valid private FKs");}database.close();database=null;
    }
    @Override public void onDisable(){if(controls!=null)controls.close();if(session!=null){session.close();session=null;}owner.execute(()->{if(database!=null){database.close();database=null;}});owner.shutdown();}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
