package com.blueninjas24.civilizations.paper;

import org.bukkit.command.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class LivestockMarketRoutingTest {
    @Test void marketRoutesToItsOwnHandlerAndDoesNotFallThroughToAdministrativeCommands() {
        int[] calls={0};var sender=(CommandSender)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{CommandSender.class},(p,m,a)->{throw new AssertionError(m.getName());});
        var router=new CivilizationsCommand(null,null,null,null,(s,c,l,a)->{assertSame(sender,s);assertArrayEquals(new String[]{"MARKET"},a);calls[0]++;return true;});
        assertTrue(router.onCommand(sender,null,"civ",new String[]{"MARKET"}));assertEquals(1,calls[0]);
        assertTrue(router.onTabComplete(sender,null,"civ",new String[]{"market",""}).isEmpty());
    }
}
