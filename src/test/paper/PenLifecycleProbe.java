import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.industry.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Marked empty loopback fixture only. Synthetic pen/animal IDs in private SQL; no world edits. */
public final class PenLifecycleProbe extends JavaPlugin {
    private CompletableFuture<Void> work;
    private int ticks;
    private void check(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
    private void sql(Connection c,String query)throws SQLException{try(var s=c.createStatement()){s.execute(query);}}
    private long scalar(Connection c,String query)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(query)){check(r.next(),"Missing scalar");return r.getLong(1);}}
    @Override public void onEnable(){boolean authorized=false;try{
        var root=getServer().getWorldContainer().toPath().toRealPath();
        check(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),"Marked empty loopback fixture required");
        authorized=true;
        check(getServer().getPluginManager().isPluginEnabled("Civilizations"),"Packaged application must enable");
        var data=getDataFolder().toPath().resolve("run-"+UUID.randomUUID());check(!Files.exists(data),"Fresh private SQL required");
        work=CompletableFuture.runAsync(()->verify(data));
        getServer().getScheduler().runTaskTimer(this,()->{try{
            check(getServer().getOnlinePlayers().isEmpty()&&++ticks<600,"No-player bounded fixture required");
            if(work.isDone()){
                work.join();getLogger().info("PEN_LIFECYCLE_PROBE_PASSED private populated schema87 migration, retained history/animal ownership, shared fence, owner/admin checks, release retry/restart/reclaim; no native geometry or player UI claim");
                getServer().shutdown();
            }
        }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"PEN_LIFECYCLE_PROBE_FAILED",failure);getServer().shutdown();}},1,1);
    }catch(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"PEN_LIFECYCLE_PROBE_FAILED",failure);if(authorized)getServer().shutdown();}}
    private void verify(Path directory){try{
        var world=UUID.randomUUID();var owner=UUID.randomUUID();var animal=UUID.randomUUID();var first=UUID.randomUUID();
        var pen=pen(first,world,owner,0,Set.of(animal));
        var db=new DatabaseManager(directory.toFile(),getLogger());
        try{
            db.initialize();db.getVillageIdentityRepository().observeWorld(world,"Private pen lifecycle fixture");
            new PenProtectionRepository(db.getConnection()).save(pen,true);
            var c=db.getConnection();
            sql(c,"DROP TRIGGER active_pen_overlap");sql(c,"DROP TABLE pen_releases");sql(c,"DROP TABLE active_pen_claims");
            sql(c,"DROP INDEX protected_pen_location");sql(c,"CREATE UNIQUE INDEX fixture_old_pen_location ON protected_pen_columns(world_id,standing_y,x,z)");sql(c,"PRAGMA user_version=87");
        }finally{db.close();}
        db=new DatabaseManager(directory.toFile(),getLogger());try{
            db.initialize();var c=db.getConnection();var pens=new PenProtectionRepository(c);var lifecycle=new PenProtectionLifecycle(c);
            check(scalar(c,"PRAGMA user_version")==88,"Upgrade version");check(pens.load(first).orElseThrow().equals(pen),"Original footprint/roster preserved");
            var ownership=new AnimalOwnershipRepository(c).load(animal).orElseThrow();
            var neighbour=pen(UUID.randomUUID(),world,owner,4,Set.of());pens.save(neighbour,true);
            check(pens.atAll(world,64,2,0).size()==2,"Shared fence must remain protected twice");
            var request=new PenProtectionLifecycle.Release(UUID.randomUUID(),first,owner,2000,"Private fixture owner release");
            try{lifecycle.release(new PenProtectionLifecycle.Release(UUID.randomUUID(),first,UUID.randomUUID(),2000,"Unauthorized"),false);throw new IllegalStateException("Unauthorized release accepted");}catch(PenProtectionLifecycle.Declined expected){}
            lifecycle.release(request,false);check(lifecycle.release(request,false).equals(request),"Exact receipt replay");
            check(pens.atAll(world,64,0,0).isEmpty()&&pens.atAll(world,64,2,0).equals(List.of(neighbour.id())),"Only released area removed");
            var replacement=pen(UUID.randomUUID(),world,owner,0,Set.of());pens.save(replacement,true);pens.save(pen,true);
            check(pens.atAll(world,64,0,0).equals(List.of(replacement.id())),"Old request cannot reactivate");
            check(ownership.equals(new AnimalOwnershipRepository(c).load(animal).orElseThrow()),"Animal owner/provenance retained");
            db.close();db=new DatabaseManager(directory.toFile(),getLogger());db.initialize();
            check(new PenProtectionLifecycle(db.getConnection()).release(request,false).equals(request),"Release survives restart");
            check(new PenProtectionRepository(db.getConnection()).load(first).orElseThrow().equals(pen),"History survives restart");
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){check(!r.next(),"Foreign keys");}
            try(var s=db.getConnection().createStatement();var r=s.executeQuery("PRAGMA integrity_check")){check(r.next()&&r.getString(1).equals("ok"),"SQLite integrity");}
        }finally{db.close();}
    }catch(Exception failure){throw new CompletionException(failure);}}
    private PenProtection pen(UUID id,UUID world,UUID owner,int offset,Set<UUID> animals){
        var scan=new PenScan(p->Math.abs(p.x()-offset)==2||Math.abs(p.z())==2?PenScan.Cell.BARRIER:PenScan.Cell.INTERIOR,new PenScan.Column(offset,0),new PenScan.Limits(4,16));
        while(!scan.complete())scan.advance(8);
        return new PenProtection(id,world,owner,64,offset+3,64,0,new PenScan.Column(offset,0),scan.evidence(),animals,1000);
    }
}
