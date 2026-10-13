import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.*;
import com.blueninjas24.civilizations.settlement.Settlement;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Packaged SQL backend/startup smoke proof, not live animal adoption or client interaction. */
public final class HerdLedgerProbe extends JavaPlugin {
    private boolean authorized;
    private void check(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    @Override public void onEnable(){try {
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty localhost fixture required");
        authorized=true;
        var world=getServer().getWorlds().getFirst().getUID(); // Capture Paper identity only on main thread.
        var folder=getDataFolder().toPath().resolve("run-"+UUID.randomUUID()).toFile();var logger=getLogger();
        var completed=CompletableFuture.runAsync(()->{
            var db=new DatabaseManager(folder,logger);
            try {
                db.initialize();UUID town=UUID.randomUUID(),owner=UUID.randomUUID(),worker=UUID.randomUUID(),business=UUID.randomUUID();
                db.saveSettlement(new Settlement(town,"Ledger fixture","Ledger fixture",0,64,0,1,0,0));
                db.getVillageIdentityRepository().observeWorld(world,"Ledger fixture");db.getVillageIdentityRepository().bindSettlement(town,world);
                for(UUID person:List.of(owner,worker))db.getPopulationRepository().insertResident(new Resident(person,"Fixture adult",town,null,300,Resident.Status.ALIVE));
                var site=new BusinessSite(UUID.randomUUID(),town,world,0,64,0,1,"Test-only backend site; no block ownership");
                var firm=new Business(business,town,"Fixture dairy",owner,owner,"livestock",site.id(),1,0);
                db.getBusinessRepository().found(new BusinessFounding(firm,site,new JobRequirements(216,Map.of()),new JobRequirements(216,Map.of()),0,"Explicit backend fixture"));
                db.getBusinessRepository().hire(UUID.randomUUID(),business,worker,0);
                var grain=new Commodity("grain");var milk=new Commodity("milk");
                db.getIndustryRepository().openStore(business,256,Map.of(grain,1L,milk,1L),0);
                var roster=Map.of(UUID.randomUUID(),0,UUID.randomUUID(),0);
                for(UUID animal:roster.keySet())new AnimalOwnershipRepository(db.getConnection()).record(new AnimalOwnershipRepository.Claim(animal,world,LivestockAdoption.Ownership.VILLAGE,null,town,1,"Synthetic backend fixture identity"));
                var herds=new LivestockRepository(db.getConnection());herds.open(business,LivestockHerd.Species.COW,grain,8,roster);
                try(var s=db.getConnection().prepareStatement("UPDATE industry_stock SET units=100 WHERE business_id=? AND commodity='grain'")){s.setString(1,business.toString());s.executeUpdate();}
                UUID operation=UUID.randomUUID();var first=herds.advance(operation,business,1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true);
                check(first.births()==1&&first.feedUsed()==6&&first.renewableUnits()==2,"Real feed and cohort/product accounting");
                db.close();db=new DatabaseManager(folder,logger);db.initialize();herds=new LivestockRepository(db.getConnection());
                check(herds.advance(operation,business,1,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),true).equals(first),"Restart historical replay");
                var store=db.getIndustryRepository().store(business).orElseThrow();check(store.units().get(grain)==94&&store.units().get(milk)==2&&store.hundredths()==0,"No stock/cash duplication");
                check(herds.advance(UUID.randomUUID(),business,2,10,10,LivestockPolicy.prototype(),BusinessOperationPolicy.prototype(),false).after().equals(first.after()),"Absent world exact pause");
                try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Fixture foreign keys");}
            }catch(Exception failure){throw new CompletionException(failure);}finally{db.close();}
        });
        new BukkitRunnable(){int ticks;@Override public void run(){try {
            check(++ticks<=300,"Ledger fixture timeout");if(!completed.isDone())return;completed.join();
            getLogger().info("HERD_LEDGER_PROBE_PASSED packaged SQL feed/cohort/product accounting, restart replay and absent-world pause; no world/entity mutations");cancel();getServer().shutdown();
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"HERD_LEDGER_PROBE_FAILED",failure);cancel();getServer().shutdown();}}}.runTaskTimer(this,100L,1L);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"HERD_LEDGER_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
}
