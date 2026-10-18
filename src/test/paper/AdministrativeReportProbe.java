import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Disposable-only proof that queued administrative reads return on Paper's main thread. Never ship. */
public final class AdministrativeReportProbe extends JavaPlugin {
    private final List<String> messages=new ArrayList<>();
    private boolean authorized;
    private int ticks;

    @Override public void onEnable() {
        try {
            var root=getServer().getWorldContainer().toPath().toRealPath();
            require(Files.readString(root.resolve("civilizations-dev-marker.txt")).trim().equals("Disposable Civilizations Phase 1 test environment")
                    &&getServer().getIp().equals("127.0.0.1")&&getServer().getPort()==25578&&getServer().getOnlinePlayers().isEmpty(),
                    "Marked disposable localhost fixture with zero players required");
            authorized=true;
            var sender=(CommandSender)Proxy.newProxyInstance(CommandSender.class.getClassLoader(),new Class<?>[]{CommandSender.class},(proxy,method,args)->switch(method.getName()) {
                case "hasPermission"->true;
                case "sendMessage"->{require(Bukkit.isPrimaryThread(),"Reports must return on Paper's main thread");messages.add((String)args[0]);yield null;}
                default->throw new AssertionError("Unexpected sender access: "+method.getName());
            });
            new BukkitRunnable(){@Override public void run(){try {
                require(++ticks<=200,"Administrative report timed out");
                if(ticks%20==1)getServer().getPluginCommand("civ").getExecutor().onCommand(sender,getServer().getPluginCommand("civ"),"civ",new String[]{"debug","resources"});
                if(messages.stream().noneMatch(line->line.startsWith("Resource provenance inventory")))return;
                require(messages.stream().anyMatch(line->line.startsWith("Forest tracking:")),"Both saved capture fences must be described");
                require(messages.stream().anyMatch(line->line.contains("no current block truth or recovery applied")),"Read-only boundary must be explicit");
                getLogger().info("NATIVE ADMINISTRATIVE REPORT PASS: registered command queued saved resource reads and delivered bounded diagnostics only on the main thread; no world access or recovery requested");
                cancel();getServer().getPluginManager().disablePlugin(AdministrativeReportProbe.this);getServer().shutdown();
            }catch(Exception|AssertionError failure){cancel();fail(failure);}}}.runTaskTimer(this,1,1);
        }catch(Exception|AssertionError failure){fail(failure);}
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private void fail(Throwable failure){getLogger().log(java.util.logging.Level.SEVERE,"NATIVE ADMINISTRATIVE REPORT FAIL",failure);getServer().getPluginManager().disablePlugin(this);if(authorized)getServer().shutdown();}
}
