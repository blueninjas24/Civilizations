import com.blueninjas24.civilizations.industry.LivestockInventory;
import com.blueninjas24.civilizations.industry.LivestockProductSale.*;
import com.blueninjas24.civilizations.paper.LivestockSaleInventory;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;

/** Native ItemStack/CraftInventory contracts only. No real player, world blocks, animals or trade delivery. */
public final class LivestockMarketInventoryProbe extends JavaPlugin {
    private boolean authorized;
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    @Override public void onEnable(){try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");
        authorized=true;
        getServer().getScheduler().runTaskLater(this,()->{try{
            verify();getLogger().info("LIVESTOCK_MARKET_INVENTORY_PROBE_PASSED actual Paper ItemStack limits, custom metadata, virtual CraftInventory snapshots, full-space refusal, multi-stack eggs, stale protection and partial-write restore; no live-player death/trade/save certification");
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_MARKET_INVENTORY_PROBE_FAILED",failure);}finally{getServer().shutdown();}},10);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"LIVESTOCK_MARKET_INVENTORY_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private final class Slots {
        final Inventory backing=Bukkit.createInventory(null,36,Component.text("Disposable market inventory"));
        boolean failNext;int writes;
        final PlayerInventory inventory=(PlayerInventory)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PlayerInventory.class},(p,m,a)->switch(m.getName()){
            case "getStorageContents"->backing.getContents();
            case "setStorageContents"->{writes++;var contents=(ItemStack[])a[0];if(failNext){failNext=false;backing.setItem(0,contents[0]);throw new IllegalStateException("Explicit native partial-provider fixture");}backing.setContents(contents);yield null;}
            default->throw new AssertionError(m.getName());
        });
    }
    private Terms terms(Product product,int count){var world=getServer().getWorlds().getFirst().getUID();return new Terms(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),world,UUID.randomUUID(),product,count,2,800);}
    private ItemStack[] copy(ItemStack[] items){var result=new ItemStack[items.length];for(int i=0;i<items.length;i++)result[i]=items[i]==null?null:items[i].clone();return result;}
    private long count(Slots slots,Material material){long result=0;for(var item:slots.backing.getContents())if(item!=null&&item.getType()==material)result+=item.getAmount();return result;}
    private void verify(){
        check(Bukkit.isPrimaryThread(),"Native inventory work must stay on main thread");
        var milk=new Slots();milk.backing.setItem(0,new ItemStack(Material.EMERALD,5));milk.backing.setItem(1,new ItemStack(Material.BUCKET,16));
        var sword=new ItemStack(Material.DIAMOND_SWORD);var meta=sword.getItemMeta();meta.displayName(Component.text("Preserve this exact custom sword"));sword.setItemMeta(meta);milk.backing.setItem(2,sword);
        var plan=LivestockSaleInventory.plan(milk.inventory,terms(Product.MILK,1));check(milk.writes==0,"Preflight makes no inventory changes");plan.apply(milk.inventory);
        check(count(milk,Material.EMERALD)==3&&count(milk,Material.BUCKET)==15&&count(milk,Material.MILK_BUCKET)==1,"Real money/bucket/product conservation");check(sword.equals(milk.backing.getItem(2)),"Full custom metadata retained");
        var eggs=new Slots();eggs.backing.setItem(0,new ItemStack(Material.EMERALD,2));LivestockSaleInventory.plan(eggs.inventory,terms(Product.EGGS,64)).apply(eggs.inventory);
        int stacks=0;for(var item:eggs.backing.getContents())if(item!=null&&item.getType()==Material.EGG){stacks++;check(item.getAmount()<=item.getMaxStackSize(),"No oversized native egg stacks");}
        int limit=new ItemStack(Material.EGG).getMaxStackSize();check(count(eggs,Material.EGG)==64&&stacks==(64+limit-1)/limit,"Actual native stack limit used");getLogger().info("Native egg stack maximum="+limit+"; milk maximum="+new ItemStack(Material.MILK_BUCKET).getMaxStackSize());
        var full=new Slots();for(int i=0;i<36;i++)full.backing.setItem(i,new ItemStack(Material.STONE,64));full.backing.setItem(0,new ItemStack(Material.EMERALD,64));full.backing.setItem(1,new ItemStack(Material.BUCKET,16));var before=copy(full.backing.getContents());
        try{LivestockSaleInventory.plan(full.inventory,terms(Product.MILK,1));throw new IllegalStateException("Full inventory incorrectly accepted");}catch(LivestockInventory.Unavailable expected){check(Arrays.equals(before,full.backing.getContents())&&full.writes==0,"Overflow refusal is read-only");}
        full.backing.setItem(0,new ItemStack(Material.EMERALD,2));LivestockSaleInventory.plan(full.inventory,terms(Product.MILK,1)).apply(full.inventory);check(full.backing.getItem(0).getType()==Material.MILK_BUCKET,"Actually emptied payment slot reused");
        var partial=new Slots();partial.backing.setItem(0,new ItemStack(Material.EMERALD,5));partial.backing.setItem(1,new ItemStack(Material.BUCKET));before=copy(partial.backing.getContents());var exchange=LivestockSaleInventory.plan(partial.inventory,terms(Product.MILK,1));partial.failNext=true;
        try{exchange.apply(partial.inventory);throw new AssertionError("Provider fixture did not fail");}catch(IllegalStateException expected){exchange.restore(partial.inventory);}check(Arrays.equals(before,partial.backing.getContents()),"Exact restore after partial native inventory write");
        var stale=LivestockSaleInventory.plan(partial.inventory,terms(Product.MILK,1));partial.backing.setItem(5,sword);before=copy(partial.backing.getContents());int writes=partial.writes;
        try{stale.apply(partial.inventory);throw new AssertionError("Changed native inventory accepted");}catch(IllegalStateException expected){stale.restore(partial.inventory);}check(Arrays.equals(before,partial.backing.getContents())&&writes==partial.writes,"Stale snapshot cannot overwrite new player property");
        var custom=new Slots();var currency=new ItemStack(Material.EMERALD,64);var customMeta=currency.getItemMeta();customMeta.displayName(Component.text("Not ordinary currency"));currency.setItemMeta(customMeta);custom.backing.setItem(0,currency);custom.backing.setItem(1,new ItemStack(Material.BUCKET));
        try{LivestockSaleInventory.plan(custom.inventory,terms(Product.MILK,1));throw new IllegalStateException("Custom currency consumed");}catch(LivestockInventory.Unavailable expected){check(currency.equals(custom.backing.getItem(0)),"Custom currency protected");}
    }
    @Override public void onDisable(){getLogger().info("LIVESTOCK_MARKET_INVENTORY_FIXTURE_RESTORED no helper player/world/entity/database changes");}
}
