import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.paper.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual Paper inventories/PDC/player files and SQL owner, synthetic player facade and death/respawn event.
 * Does not log in a client, materialize/adopt animals or claim to exercise the actual network/death pipeline. */
public final class LivestockMarketTradeProbe extends JavaPlugin {
    private boolean authorized,withholdPrepared,withholdExchange,dead;
    private PopulationSession session;private LivestockSaleTransfers transfers;private LivestockMarketMenu menu;
    private Player craft,player;private Plugin facade;private Inventory opened;private InventoryView view;
    private Field saveDisabled;private boolean originalSaveDisabled;
    private Path data,players;private UUID world,town,dairy,chickens;private Location location;
    private ItemStack sword;
    private CompletableFuture<?> work;private int stage,ticks,waiting;
    private final LivestockMarketPolicy policy=LivestockMarketPolicy.prototype();
    private void check(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");authorized=true;
        world=getServer().getWorlds().getFirst().getUID();location=getServer().getWorlds().getFirst().getSpawnLocation();
        players=getServer().getLevelDirectory().resolve("players/data");data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());
        work=CompletableFuture.runAsync(this::seed);
        new BukkitRunnable(){public void run(){try {
            check(++ticks<=800,"Native market timeout at stage "+stage);step();
            if(stage==99){getLogger().info("LIVESTOCK_MARKET_TRADE_PROBE_PASSED real Paper item/PDC/player saves and SQL owner, menu double-click, failed preparation/exchange saves, selective synthetic death/respawn, milk bucket/egg conservation, native reload, owner/controller restart and stable logical newborn identities/maturity; not a real client death/crash, physical offspring or automatic herd test");cancel();getServer().shutdown();}
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_MARKET_TRADE_PROBE_FAILED stage="+stage,failure);cancel();getServer().shutdown();}}}.runTaskTimer(this,10,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_MARKET_TRADE_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private boolean ready(){if(!work.isDone())return false;work.join();return true;}
    private void openOwner(){session=new PopulationSession(data.toFile(),getLogger(),PopulationSession.Settings.prototype(),System::currentTimeMillis);session.configureLivestockMarket(policy);session.configureBusinessOperations(BusinessOperationPolicy.prototype());work=session.start(false,Set.of(world));}
    private void controller(){transfers=new LivestockSaleTransfers(facade,session,policy,players);menu=new LivestockMarketMenu(facade,session,transfers,policy);}
    private PendingLivestockSale receipt(){var raw=player.getPersistentDataContainer().get(LivestockSaleTransfers.KEY,PersistentDataType.STRING);return raw==null?null:PendingLivestockSale.decode(raw);}
    private long count(Material material){long count=0;for(var item:craft.getInventory().getStorageContents())if(item!=null&&item.getType()==material)count+=item.getAmount();return count;}
    private void poll(){transfers.poll(player);}
    private void step()throws Exception {
        check(Bukkit.isPrimaryThread(),"Paper inventory work on main thread only");
        switch(stage) {
            case 0 -> {if(!ready())return;openOwner();stage=1;}
            case 1 -> {if(!ready())return;createPlayer();controller();
                craft.getInventory().setItem(0,new ItemStack(Material.EMERALD,5));craft.getInventory().setItem(1,new ItemStack(Material.BUCKET));craft.getInventory().setItem(2,new ItemStack(Material.EGG,8));
                sword=new ItemStack(Material.DIAMOND_SWORD);var meta=sword.getItemMeta();meta.displayName(Component.text("Exact native market property"));sword.setItemMeta(meta);craft.getInventory().setItem(3,sword);
                withholdPrepared=true;menu.onCommand(player,null,"civ",new String[]{"market"});stage=2;}
            case 2 -> {menu.run();if(opened==null)return;check(opened.getSize()==54,"Actual native market inventory");int slot=-1;for(int i=0;i<32;i++)if(opened.getItem(i)!=null&&opened.getItem(i).getType()==Material.MILK_BUCKET){slot=i;break;}check(slot>=0,"Actual saved dairy offer in menu");
                var click=new InventoryClickEvent(view,InventoryType.SlotType.CONTAINER,slot,ClickType.LEFT,InventoryAction.PICKUP_ALL);menu.click(click);menu.click(click);check(click.isCancelled(),"Menu protects display and double click");stage=3;}
            case 3 -> {poll();if(receipt()==null)return;check(receipt().stage()==PendingLivestockSale.Stage.PREPARED,"Prepared before any delivery");if(++waiting<20)return;
                check(count(Material.MILK_BUCKET)==0&&count(Material.EMERALD)==5&&count(Material.BUCKET)==1,"Void native save cannot debit before proof");
                check(!SavedAidProof.contains(players,player.getUniqueId(),PendingLivestockSale.KEY,receipt().encode()),"No false preparation proof");
                work=session.livestockOffers(player.getUniqueId(),new LivestockMarketPolicy.Position(world,location.getX(),location.getY(),location.getZ()));stage=4;}
            case 4 -> {if(!ready())return;@SuppressWarnings("unchecked")var offers=(List<LivestockMarket.Offer>)work.join();check(offers.stream().anyMatch(o->o.terms().product()==LivestockProductSale.Product.MILK&&o.availableUnits()==2),"Unverified intent did not reserve stock");withholdPrepared=false;withholdExchange=true;stage=5;}
            case 5 -> {poll();if(count(Material.MILK_BUCKET)!=1)return;var receipt=receipt();check(receipt.stage()==PendingLivestockSale.Stage.EXCHANGED,"Exchange receipt before physical delivery");
                check(count(Material.EMERALD)==3&&count(Material.BUCKET)==0&&count(Material.EGG)==8,"Native exact payment, real bucket and older eggs");check(sword.equals(craft.getInventory().getItem(3)),"Custom property untouched");
                check(PendingLivestockItems.count(craft.getInventory(),receipt.terms())==1,"Exact purchased milk marked");check(!SavedAidProof.contains(players,player.getUniqueId(),PendingLivestockSale.KEY,receipt.encode()),"Unsaved delivery cannot credit seller");
                verifyDeath(receipt);dead=true;waiting=0;stage=6;}
            case 6 -> {poll();if(++waiting<5)return;check(receipt()!=null&&PendingLivestockItems.count(craft.getInventory(),receipt().terms())==1,"Dead player goods remain pending");
                dead=false;withholdExchange=false;transfers.close();menu.close();controller();stage=7;}
            case 7 -> {poll();if(receipt()!=null)return;check(count(Material.MILK_BUCKET)==1&&count(Material.EMERALD)==0&&count(Material.EGG)==0,"Only unfinished order kept; other death property followed normal drops");
                check(!craft.getInventory().getItem(0).hasItemMeta(),"Confirmed milk loses special marker");craft.loadData();check(count(Material.MILK_BUCKET)==1&&!craft.getPersistentDataContainer().has(LivestockSaleTransfers.KEY),"Native saved product and cleared receipt reload");
                transfers.close();menu.close();session.close();openOwner();stage=8;}
            case 8 -> {if(!ready())return;controller();craft.getInventory().setItem(1,new ItemStack(Material.EMERALD,5));craft.getInventory().setItem(2,new ItemStack(Material.EGG,8));
                work=session.livestockOffers(player.getUniqueId(),new LivestockMarketPolicy.Position(world,location.getX(),location.getY(),location.getZ()));stage=9;}
            case 9 -> {if(!ready())return;@SuppressWarnings("unchecked")var offers=(List<LivestockMarket.Offer>)work.join();var eggs=offers.stream().filter(o->o.terms().product()==LivestockProductSale.Product.EGGS).findFirst().orElseThrow();
                check(eggs.terms().count()==4&&eggs.availableUnits()==5,"Chicken output from explicit test feed");withholdExchange=true;check(transfers.buy(player,eggs),"Egg order accepted");stage=10;}
            case 10 -> {poll();if(receipt()==null||receipt().stage()!=PendingLivestockSale.Stage.EXCHANGED)return;
                check(count(Material.EGG)==12&&PendingLivestockItems.count(craft.getInventory(),receipt().terms())==4,"Old eggs separate from four pending eggs");check(craft.getInventory().getItem(2).getAmount()==8&&!craft.getInventory().getItem(2).hasItemMeta(),"Older eggs receive no death protection");
                var event=death();new PendingLivestockItems().death(event);check(event.getItemsToKeep().stream().mapToInt(ItemStack::getAmount).sum()==4,"Only pending eggs get retention");check(event.getDrops().stream().anyMatch(i->i.getType()==Material.MILK_BUCKET),"Confirmed earlier milk ordinary death drop");
                withholdExchange=false;transfers.close();menu.close();controller();stage=11;}
            case 11 -> {poll();if(receipt()!=null)return;check(count(Material.EGG)==12&&count(Material.EMERALD)==4&&count(Material.BUCKET)==0,"Egg payment does not invent containers");
                craft.loadData();check(count(Material.EGG)==12&&count(Material.EMERALD)==4&&!craft.getPersistentDataContainer().has(LivestockSaleTransfers.KEY),"Actual saved egg inventory/receipt reload");
                transfers.close();menu.close();session.close();session=null;work=CompletableFuture.runAsync(this::audit);stage=12;}
            case 12 -> {if(!ready())return;stage=99;}
            default -> throw new IllegalStateException("Unknown market test stage");
        }
    }
    private PlayerDeathEvent death() {
        var source=(DamageSource)Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{DamageSource.class},(p,m,a)->{throw new AssertionError(m.getName());});
        var drops=new ArrayList<ItemStack>();for(var item:craft.getInventory().getStorageContents())if(item!=null&&item.getAmount()>0)drops.add(item.clone());
        return new PlayerDeathEvent(player,source,drops,7,Component.text("Synthetic native retention check"),true);
    }
    private void verifyDeath(PendingLivestockSale receipt) {
        var milk=Arrays.stream(craft.getInventory().getStorageContents()).filter(i->PendingLivestockItems.matches(i,receipt.terms())).findFirst().orElseThrow();
        var consume=new PlayerItemConsumeEvent(player,milk,EquipmentSlot.HAND);new PendingLivestockItems().consume(consume);check(consume.isCancelled(),"Pending native milk consumption blocked");
        var recipe=new com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent(player,new NamespacedKey("minecraft","cake"),true);new PendingTradeInventoryGuard().recipe(recipe);check(recipe.isCancelled(),"Pending recipe-book movement blocked");
        var event=death();new PendingLivestockItems().death(event);check(!event.getKeepInventory()&&event.getDroppedExp()==7,"Normal keepInventory and XP unchanged");
        check(event.getItemsToKeep().size()==1&&event.getItemsToKeep().getFirst().getType()==Material.MILK_BUCKET,"Only exact pending native milk retained");
        check(event.getDrops().stream().anyMatch(i->i.equals(sword))&&event.getDrops().stream().anyMatch(i->i.getType()==Material.EGG&&i.getAmount()==8),"Old eggs and custom sword remain ordinary drops");
        // Explicit synthetic respawn model, not a claim about Paper's actual death packet/save pipeline.
        craft.getInventory().clear();int slot=0;for(var item:event.getItemsToKeep())craft.getInventory().setItem(slot++,item.clone());
    }
    private void createPlayer()throws Exception {
        var server=getServer();var nativeServer=server.getClass().getMethod("getServer").invoke(server);var nativeWorld=location.getWorld().getClass().getMethod("getHandle").invoke(location.getWorld());
        var profile=Class.forName("com.mojang.authlib.GameProfile");var info=Class.forName("net.minecraft.server.level.ClientInformation");var type=Class.forName("net.minecraft.server.level.ServerPlayer");
        var nativePlayer=type.getConstructor(Class.forName("net.minecraft.server.MinecraftServer"),Class.forName("net.minecraft.server.level.ServerLevel"),profile,info).newInstance(nativeServer,nativeWorld,profile.getConstructor(UUID.class,String.class).newInstance(UUID.randomUUID(),"CivMarketFixture"),info.getMethod("createDefault").invoke(null));
        craft=(Player)type.getMethod("getBukkitEntity").invoke(nativePlayer);saveDisabled=Class.forName("org.spigotmc.SpigotConfig").getField("disablePlayerDataSaving");originalSaveDisabled=saveDisabled.getBoolean(null);check(!originalSaveDisabled,"Native player saves must start enabled");
        player=(Player)Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()) {
            case "hasPermission","isOnline"->true;case "isDead"->dead;case "getLocation"->location.clone();case "sendMessage"->{getLogger().info("Synthetic market customer: "+a[0]);yield null;}
            case "getItemOnCursor"->null;case "openInventory"->{opened=(Inventory)a[0];yield null;}case "getOpenInventory"->view;case "closeInventory"->{opened=null;yield null;}
            case "saveData"->{var receipt=receipt();boolean withhold=receipt!=null&&(withholdPrepared&&receipt.stage()==PendingLivestockSale.Stage.PREPARED||withholdExchange&&receipt.stage()==PendingLivestockSale.Stage.EXCHANGED);
                try{if(withhold)saveDisabled.setBoolean(null,true);craft.saveData();}finally{saveDisabled.setBoolean(null,originalSaveDisabled);}yield null;}
            default->{try{yield m.invoke(craft,a);}catch(InvocationTargetException failure){throw failure.getCause();}}
        });
        view=(InventoryView)Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{InventoryView.class},(p,m,a)->switch(m.getName()){
            case "getTopInventory"->opened;case "getBottomInventory"->craft.getInventory();case "getPlayer"->player;case "getType"->InventoryType.CHEST;case "convertSlot"->a[0];default->throw new AssertionError(m.getName());
        });
        var serverFacade=(Server)Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{Server.class},(p,m,a)->{
            if(m.getName().equals("getPlayer")&&player.getUniqueId().equals(a[0]))return player;
            try{return m.invoke(server,a);}catch(InvocationTargetException failure){throw failure.getCause();}
        });
        facade=(Plugin)Proxy.newProxyInstance(getClassLoader(),new Class<?>[]{Plugin.class},(p,m,a)->{
            if(m.getName().equals("getServer"))return serverFacade;
            try{return m.invoke(this,a);}catch(InvocationTargetException failure){throw failure.getCause();}
        });
    }
    private void seed() {
        var db=new DatabaseManager(data.toFile(),getLogger());try {
            db.initialize();town=UUID.randomUUID();db.saveSettlement(new Settlement(town,"Explicit native market fixture","Fixture",location.getBlockX(),location.getBlockY(),location.getBlockZ(),2,0,0));
            db.getVillageIdentityRepository().observeWorld(world,"Fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
            for(var species:LivestockHerd.Species.values()) {
                UUID owner=UUID.randomUUID(),worker=UUID.randomUUID(),id=UUID.randomUUID();if(species==LivestockHerd.Species.COW)dairy=id;else chickens=id;
                for(var person:List.of(owner,worker))db.getPopulationRepository().insertResident(new Resident(person,"Explicit market worker",town,null,300,Resident.Status.ALIVE));
                var site=new BusinessSite(UUID.randomUUID(),town,world,location.getBlockX()+(species==LivestockHerd.Species.COW?0:2),location.getBlockY(),location.getBlockZ(),1,"Trusted market fixture; not automatic adoption");
                db.getBusinessRepository().found(new BusinessFounding(new Business(id,town,"Fixture "+species,owner,owner,"livestock",site.id(),1,0),site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of()),0,"Trusted fixture"));
                db.getBusinessRepository().hire(UUID.randomUUID(),id,worker,0);var grain=new Commodity("grain");db.getIndustryRepository().openStore(id,256,Map.of(grain,1L,new Commodity("milk"),1L,new Commodity("eggs"),1L),0);
                var roster=new HashMap<UUID,Integer>();for(int n=0;n<3;n++){UUID animal=UUID.randomUUID();roster.put(animal,n==2?2:0);new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(animal,world,LivestockAdoption.Ownership.VILLAGE,null,town,1,"Explicit synthetic unmaterialized fixture identity"));}
                var repo=new LivestockRepository(db.getConnection());repo.open(id,species,grain,8,roster);
                try(var s=db.getConnection().prepareStatement("UPDATE industry_stock SET units=100 WHERE business_id=? AND commodity='grain'")){s.setString(1,id.toString());s.executeUpdate();}
                repo.advance(UUID.randomUUID(),id,1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);
                if(species==LivestockHerd.Species.CHICKEN)repo.advance(UUID.randomUUID(),id,2,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);
            }
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    private void audit() {
        var db=new DatabaseManager(data.toFile(),getLogger());try {
            db.initialize();check(db.getIndustryRepository().store(dairy).orElseThrow().hundredths()==1600,"Milk seller credited once");check(db.getIndustryRepository().store(chickens).orElseThrow().hundredths()==800,"Egg seller credited once");
            check(db.getIndustryRepository().store(dairy).orElseThrow().units().get(new Commodity("milk"))==1,"One milk stock debit");check(db.getIndustryRepository().store(chickens).orElseThrow().units().get(new Commodity("eggs"))==1,"Four egg stock debit");
            var births=new LivestockBirths(db.getConnection());
            for(var business:List.of(dairy,chickens)) {
                check(births.page(business,0,-1,256).equals(LivestockBirth.batch(business,1,1,12)),"Stable newborn identities/maturity retained through native owner restart");
                check(births.unmappedBirths(business)==0,"All new simulated births have identity records; not physical spawn proof");
            }
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*),SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) FROM livestock_product_sales")){check(r.next()&&r.getInt(1)==2&&r.getInt(2)==2,"Exactly two completed saved orders");}
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA integrity_check")){check(r.next()&&r.getString(1).equals("ok"),"Native fixture SQLite integrity");}
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Native fixture foreign keys");}
        }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
    }
    @Override public void onDisable(){try {
        if(saveDisabled!=null)saveDisabled.setBoolean(null,originalSaveDisabled);
        if(menu!=null)menu.close();if(transfers!=null)transfers.close();if(session!=null)session.close();
        getLogger().info("LIVESTOCK_MARKET_TRADE_FIXTURE_RESTORED player-save flag restored; synthetic save and separate database retained; no helper world blocks/animals/online players changed");
    }catch(Exception failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_MARKET_TRADE_PROBE_FAILED cleanup",failure);}}
}
