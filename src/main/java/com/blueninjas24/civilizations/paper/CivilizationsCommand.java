package com.blueninjas24.civilizations.paper;

import org.bukkit.command.*;
import java.util.List;

/** Route player previews separately from permission-protected administrative diagnostics. */
public final class CivilizationsCommand implements CommandExecutor, TabCompleter {
    private final PopulationDebugCommand debug;
    private final FoodAidPreviewCommand aid;
    private final FoodBargainingCommand bargaining;
    private final FoodBarterCommand barter;
    public CivilizationsCommand(PopulationDebugCommand debug, FoodAidPreviewCommand aid) { this(debug,aid,null); }
    public CivilizationsCommand(PopulationDebugCommand debug,FoodAidPreviewCommand aid,FoodBargainingCommand bargaining) { this(debug,aid,bargaining,null); }
    public CivilizationsCommand(PopulationDebugCommand debug,FoodAidPreviewCommand aid,FoodBargainingCommand bargaining,FoodBarterCommand barter) { this.debug=debug;this.aid=aid;this.bargaining=bargaining;this.barter=barter; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 && sender.hasPermission("civilizations.aid.preview") && !sender.hasPermission("civilizations.debug")) {
            sender.sendMessage("Hold plain food near a village and use /civ aid to preview it."); return true;
        }
        if(args.length>0&&args[0].equalsIgnoreCase("bargain")&&bargaining!=null)return bargaining.onCommand(sender,command,label,args);
        if(args.length>0&&args[0].equalsIgnoreCase("barter")&&barter!=null)return barter.onCommand(sender,command,label,args);
        return args.length > 0 && args[0].equalsIgnoreCase("aid") ? aid.onCommand(sender, command, label, args) : debug.onCommand(sender, command, label, args);
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            var options = new java.util.ArrayList<>(debug.onTabComplete(sender, command, alias, args));
            if (sender.hasPermission("civilizations.aid.preview") && "aid".startsWith(args[0].toLowerCase(java.util.Locale.ROOT))) options.add("aid");
            if(bargaining!=null&&sender.hasPermission("civilizations.trade")&&"bargain".startsWith(args[0].toLowerCase(java.util.Locale.ROOT)))options.add("bargain");
            if(barter!=null&&sender.hasPermission("civilizations.trade")&&"barter".startsWith(args[0].toLowerCase(java.util.Locale.ROOT)))options.add("barter");
            return List.copyOf(options);
        }
        if(args.length>0&&args[0].equalsIgnoreCase("bargain"))return args.length==2&&sender.hasPermission("civilizations.trade")&&"accept".startsWith(args[1].toLowerCase(java.util.Locale.ROOT)) ? List.of("accept") : List.of();
        if(args.length>0&&args[0].equalsIgnoreCase("barter"))return args.length==2&&sender.hasPermission("civilizations.trade") ? List.of("timber","stone","accept").stream().filter(s->s.startsWith(args[1].toLowerCase(java.util.Locale.ROOT))).toList() : List.of();
        return args.length > 0 && args[0].equalsIgnoreCase("aid") ? List.of() : debug.onTabComplete(sender, command, alias, args);
    }
}
