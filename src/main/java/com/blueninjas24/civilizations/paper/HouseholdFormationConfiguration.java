package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.population.HouseholdFormationPolicy;
import org.bukkit.configuration.ConfigurationSection;
import java.util.Set;

/** Strict optional settings. Invalid formation settings never disable population or saved housing. */
public final class HouseholdFormationConfiguration {
    private HouseholdFormationConfiguration(){}
    public static HouseholdFormationPolicy read(ConfigurationSection config){
        var section=config.getConfigurationSection("household-formation");
        if(section==null){if(config.contains("household-formation"))throw new IllegalArgumentException("household-formation must be a section");return HouseholdFormationPolicy.prototype();}
        if(!Set.of("enabled","maximum-per-month","maximum-candidates").containsAll(section.getKeys(false)))
            throw new IllegalArgumentException("Unknown household-formation setting");
        Object enabled=section.get("enabled",true),maximum=section.get("maximum-per-month",2),candidates=section.get("maximum-candidates",128);
        if(!(enabled instanceof Boolean flag)||!(maximum instanceof Integer)||!(candidates instanceof Integer))
            throw new IllegalArgumentException("Household formation needs boolean enabled and integer limits");
        return new HouseholdFormationPolicy(flag,(Integer)maximum,(Integer)candidates);
    }
}
