package com.blueninjas24.civilizations.paper;

import java.lang.reflect.Proxy;
import java.util.List;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldEditCapturePolicyTest {
    private static Plugin plugin(String name,String version,boolean enabled) {
        PluginMeta meta=(PluginMeta)Proxy.newProxyInstance(PluginMeta.class.getClassLoader(),new Class<?>[]{PluginMeta.class},
                (proxy,method,args)->method.getName().equals("getVersion")?version:defaultValue(method.getReturnType()));
        return (Plugin)Proxy.newProxyInstance(Plugin.class.getClassLoader(),new Class<?>[]{Plugin.class},(proxy,method,args)->switch(method.getName()) {
            case "getName"->name;
            case "getPluginMeta"->meta;
            case "isEnabled"->enabled;
            default->defaultValue(method.getReturnType());
        });
    }

    private static Object defaultValue(Class<?> type) {
        if(!type.isPrimitive())return null;
        if(type==boolean.class)return false;
        if(type==char.class)return '\0';
        return 0;
    }

    @Test void onlyThePinnedEnabledWorldEditRuntimeWithoutFaweIsEligible() {
        assertTrue(WorldEditCapturePolicy.supportsPlugin(plugin("WorldEdit",WorldEditCapturePolicy.SUPPORTED_PLUGIN_VERSION,true),false));
        assertFalse(WorldEditCapturePolicy.supportsPlugin(plugin("WorldEdit",WorldEditCapturePolicy.SUPPORTED_PLUGIN_VERSION,true),true));
        assertFalse(WorldEditCapturePolicy.supportsPlugin(plugin("WorldEdit",WorldEditCapturePolicy.SUPPORTED_PLUGIN_VERSION,false),false));
        assertFalse(WorldEditCapturePolicy.supportsPlugin(plugin("WorldEdit","7.4.4",true),false));
        assertFalse(WorldEditCapturePolicy.supportsPlugin(plugin("WorldEdit","7.4.5+7546-f9e033f",true),false));
        assertFalse(WorldEditCapturePolicy.supportsPlugin(plugin("WorldEditHelper",WorldEditCapturePolicy.SUPPORTED_PLUGIN_VERSION,true),false));
        assertFalse(WorldEditCapturePolicy.supportsPlugin(null,false));
    }

    @Test void onlyExtentTestedCommandsRemainEligibleAndUnknownOrNonExtentPathsFence() {
        for(String command:List.of("//set stone","//undo",
                "//pos1","//hpos2","//wand","//chunkinfo","//listchunks"))
            assertEquals("",WorldEditCapturePolicy.unsupportedCommandReason(command),command);
        for(String command:List.of("//regen","//delchunks","//forestgen","//feature","//structure","//paste","//cs script.js","//unknown","//replace coal_ore iron_ore","//redo"))
            assertFalse(WorldEditCapturePolicy.unsupportedCommandReason(command).isEmpty(),command);
        assertEquals("",WorldEditCapturePolicy.unsupportedCommandReason("/say //regen"));
        assertEquals("",WorldEditCapturePolicy.unsupportedCommandReason("/say hello"));
        assertEquals("",WorldEditCapturePolicy.unsupportedCommandReason(""));
        for(String arguments:List.of("/set stone","set stone","/undo","/pos1"))
            assertEquals("",WorldEditCapturePolicy.unsupportedEditorCommandReason(arguments));
        for(String arguments:List.of("/regen","delchunks","worldedit:/set stone","brush sphere stone",""))
            assertFalse(WorldEditCapturePolicy.unsupportedEditorCommandReason(arguments).isEmpty());
    }
}
