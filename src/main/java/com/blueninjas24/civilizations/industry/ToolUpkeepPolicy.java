package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** One abstract tool kit services bounded productive work; no wear merely from elapsed time. */
public record ToolUpkeepPolicy(boolean enabled,int startupGracePeriods,int workPeriodsPerTool,
                               int missingToolsOutputPercent,long toolValueHundredths,Set<String> industries) {
    public static final Commodity TOOLS=new Commodity("tools");
    public ToolUpkeepPolicy {
        industries=Collections.unmodifiableSet(new TreeSet<>(industries));
        if(startupGracePeriods<0||startupGracePeriods>100000||workPeriodsPerTool<1||workPeriodsPerTool>100000
                ||missingToolsOutputPercent<1||missingToolsOutputPercent>100||toolValueHundredths<1||toolValueHundredths>100000000
                ||industries.size()>32||industries.stream().anyMatch(s->!s.matches("[a-z][a-z0-9_]{0,63}")||Set.of("warehouse","farming").contains(s)))
            throw new IllegalArgumentException("Invalid timed-business tool upkeep settings");
    }
    public boolean applies(String industry){return enabled&&industries.contains(industry);}
    public static ToolUpkeepPolicy disabled(){return new ToolUpkeepPolicy(false,3,8,50,4000,Set.of("mining","blacksmith"));}
}
