package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.economy.*;
import com.blueninjas24.civilizations.economy.CommodityAccounting.*;
import com.blueninjas24.civilizations.industry.IndustryPolicy;
import org.bukkit.configuration.ConfigurationSection;
import java.math.BigDecimal;
import java.util.*;

/** Strict, human-readable economy.yml adapter. Invalid edits fail before database startup. */
public final class EconomyConfiguration {
    private EconomyConfiguration() {}
    public static EconomySettings read(ConfigurationSection config) {
        var rootKeys = new HashSet<>(Set.of("format-version", "pace", "food", "shortages", "starting-units-per-resident", "prices"));
        if (config.contains("workplace-operations")) rootKeys.add("workplace-operations");
        if (config.contains("workplace-observer")) rootKeys.add("workplace-observer");
        if (config.contains("farms")) rootKeys.add("farms");
        if (config.contains("priorities")) rootKeys.add("priorities");
        if (config.contains("responses")) rootKeys.add("responses");
        if (config.contains("industry")) rootKeys.add("industry");
        if (config.contains("quarry-resources")) rootKeys.add("quarry-resources");
        if (config.contains("fishing-resources")) rootKeys.add("fishing-resources");
        if (config.contains("village-supply-purchases")) rootKeys.add("village-supply-purchases");
        if (config.contains("farm-businesses")) rootKeys.add("farm-businesses");
        if (config.contains("recipes")) rootKeys.add("recipes");
        if (config.contains("ore-observer")) rootKeys.add("ore-observer");
        if (config.contains("ore-provenance")) rootKeys.add("ore-provenance");
        if (config.contains("industry-runtime")) rootKeys.add("industry-runtime");
        if (config.contains("industry-supply")) rootKeys.add("industry-supply");
        if (config.contains("workshop-starter-supplies")) rootKeys.add("workshop-starter-supplies");
        if (config.contains("workshop-founding")) rootKeys.add("workshop-founding");
        if (config.contains("workshop-opportunities")) rootKeys.add("workshop-opportunities");
        if (config.contains("warehouse-opportunities")) rootKeys.add("warehouse-opportunities");
        if (config.contains("warehouse-founding")) rootKeys.add("warehouse-founding");
        if (config.contains("warehouse-supply")) rootKeys.add("warehouse-supply");
        if (config.contains("business-tools")) rootKeys.add("business-tools");
        if (config.contains("upkeep-contributions")) rootKeys.add("upkeep-contributions");
        if (config.contains("farm-tools")) rootKeys.add("farm-tools");
        if (config.contains("farm-tool-budget")) rootKeys.add("farm-tool-budget");
        if (config.contains("automatic-career-study")) rootKeys.add("automatic-career-study");
        if (config.contains("automatic-business-staffing")) rootKeys.add("automatic-business-staffing");
        if (config.contains("business-resale-funding")) rootKeys.add("business-resale-funding");
        if (config.contains("business-operations")) rootKeys.add("business-operations");
        if (config.contains("automatic-business-resale")) rootKeys.add("automatic-business-resale");
        if (config.contains("resident-study")) rootKeys.add("resident-study");
        if (config.contains("automatic-mines")) rootKeys.add("automatic-mines");
        if (config.contains("startup-grants")) rootKeys.add("startup-grants");
        if (config.contains("work-learning")) rootKeys.add("work-learning");
        if (config.contains("aid-preview")) rootKeys.add("aid-preview");
        if (config.contains("donations")) rootKeys.add("donations");
        if (config.contains("finance")) rootKeys.add("finance");
        if (config.contains("purchases")) rootKeys.add("purchases");
        if (config.contains("wellbeing")) rootKeys.add("wellbeing");
        if (config.contains("bargaining")) rootKeys.add("bargaining");
        if (config.contains("barter")) rootKeys.add("barter");
        exactKeys(config, rootKeys);
        readAdditionalOreEditors(config);
        readIndustryRuntime(config);
        readQuarryResources(config);
        readFishingResources(config);
        readVillageSupplies(config);
        readWorkshopSupply(config);
        readWorkshopStarterSupplies(config);
        readWorkshopOpportunities(config);
        readWarehouseOpportunities(config);
        readWarehouseFounding(config);
        readWorkshopFounding(config);
        readWarehouseSupply(config);
        readToolUpkeep(config);
        readBusinessStaffing(config);
        readCareerStudy(config);
        readFarmToolBudget(config);
        readFarmTools(config);
        readUpkeepContributions(config);
        readBusinessPurchasePolicy(config);
        readBusinessOperations(config);
        readWorkLearning(config);
        readStudy(config);
        readStartupGrants(config);
        readMineFounding(config);
        readWorkplaceObserver(config);
        readWorkplaceOperations(config);
        readAutomaticResale(config);
        readFarms(config);
        readPriorities(config);
        readAidPreview(config);
        readDonationsEnabled(config);
        readOpeningFunds(config);
        readPurchases(config);
        readWellbeing(config);
        readBargaining(config);
        readBarter(config);
        readResponses(config);
        readIndustry(config);
        readFarmBusinesses(config);
        readRecipes(config);
        readOreObserver(config);
        if (integer(config, "format-version") != 1) throw new IllegalArgumentException("Unsupported economy.yml format-version; expected 1");
        exactKeys(section(config, "pace"), Set.of("seconds-per-period", "maximum-offline-periods", "periods-per-batch"));
        exactKeys(section(config, "food"), Set.of("units-per-adult", "units-per-child", "units-per-worker", "output-percent"));
        exactKeys(section(config, "shortages"), Set.of("severe-coverage-percent", "severe-after-periods", "surplus-coverage-percent", "abundant-coverage-percent"));
        exactKeys(section(config, "prices"), Set.of("base-trade-value", "condition-percent"));
        var stocks = new TreeMap<Commodity, Long>(); var values = new TreeMap<Commodity, BigDecimal>();
        var inventory = section(config, "starting-units-per-resident");
        for (var id : inventory.getKeys(false)) stocks.put(new Commodity(id), integer(inventory, id));
        var prices = section(config, "prices.base-trade-value");
        for (var id : prices.getKeys(false)) {
            Object value = prices.get(id);
            if (!(value instanceof Number)) throw new IllegalArgumentException("prices.base-trade-value." + id + " must be a number");
            try { values.put(new Commodity(id), new BigDecimal(value.toString())); }
            catch (NumberFormatException invalid) { throw new IllegalArgumentException("prices.base-trade-value." + id + " must be finite", invalid); }
        }
        var bands = section(config, "prices.condition-percent"); var percentages = new EnumMap<Condition, Integer>(Condition.class);
        var keys = new HashSet<String>();
        for (var condition : Condition.values()) {
            var key = condition.name().toLowerCase(Locale.ROOT).replace('_', '-'); keys.add(key);
            percentages.put(condition, smallInteger(bands, key));
        }
        exactKeys(bands, keys);
        return new EconomySettings(integer(config, "pace.seconds-per-period"), smallInteger(config, "pace.maximum-offline-periods"),
                smallInteger(config, "pace.periods-per-batch"), integer(config, "food.units-per-adult"), integer(config, "food.units-per-child"),
                integer(config, "food.units-per-worker"), smallInteger(config, "food.output-percent"),
                new Policy(basisPoints(config, "shortages.severe-coverage-percent"), basisPoints(config, "shortages.surplus-coverage-percent"),
                        basisPoints(config, "shortages.abundant-coverage-percent"), integer(config, "shortages.severe-after-periods")), stocks, values, percentages);
    }
    public static com.blueninjas24.civilizations.industry.UpkeepContributionPolicy readUpkeepContributions(ConfigurationSection config) {
        if(!config.contains("upkeep-contributions"))return com.blueninjas24.civilizations.industry.UpkeepContributionPolicy.disabled();
        var s=section(config,"upkeep-contributions");
        exactKeys(s,Set.of("enabled","operating-reserve-hundredths","spare-cash-percent","per-business-period-limit-hundredths","per-village-period-limit-hundredths"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("upkeep-contributions.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.UpkeepContributionPolicy(enabled,integer(s,"operating-reserve-hundredths"),smallInteger(s,"spare-cash-percent"),
                integer(s,"per-business-period-limit-hundredths"),integer(s,"per-village-period-limit-hundredths"));
    }
    public static com.blueninjas24.civilizations.industry.FarmToolUpkeepPolicy readFarmTools(ConfigurationSection config) {
        if(!config.contains("farm-tools"))return com.blueninjas24.civilizations.industry.FarmToolUpkeepPolicy.disabled();
        var s=section(config,"farm-tools");exactKeys(s,Set.of("enabled","startup-grace-periods","work-periods-per-tool","missing-tools-output-percent","suppliers-per-review"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("farm-tools.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.FarmToolUpkeepPolicy(enabled,smallInteger(s,"startup-grace-periods"),smallInteger(s,"work-periods-per-tool"),smallInteger(s,"missing-tools-output-percent"),smallInteger(s,"suppliers-per-review"));
    }
    public static com.blueninjas24.civilizations.industry.FarmToolBudgetPolicy readFarmToolBudget(ConfigurationSection config) {
        if(!config.contains("farm-tool-budget"))return com.blueninjas24.civilizations.industry.FarmToolBudgetPolicy.disabled();
        var s=section(config,"farm-tool-budget");exactKeys(s,Set.of("enabled","tool-trade-value-hundredths","per-period-limit-hundredths","minimum-treasury-hundredths","food-reserve-periods","food-unit-value-hundredths"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("farm-tool-budget.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.FarmToolBudgetPolicy(enabled,integer(s,"tool-trade-value-hundredths"),
                integer(s,"per-period-limit-hundredths"),integer(s,"minimum-treasury-hundredths"),smallInteger(s,"food-reserve-periods"),integer(s,"food-unit-value-hundredths"));
    }
    public static com.blueninjas24.civilizations.industry.WorkplaceObserverPolicy readWorkplaceObserver(ConfigurationSection config) {
        if(!config.contains("workplace-observer"))return com.blueninjas24.civilizations.industry.WorkplaceObserverPolicy.disabled();
        var s=section(config,"workplace-observer");exactKeys(s,Set.of("enabled","vertical-radius","cells-per-tick","interval-ticks","horizontal-radius-chunks","markers"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("workplace-observer.enabled must be true or false");
        if(!(s.get("markers") instanceof List<?> raw)||raw.isEmpty()||raw.size()>2||raw.stream().anyMatch(x->!(x instanceof String))||new HashSet<>(raw).size()!=raw.size())throw new IllegalArgumentException("Workplace markers must be a distinct supported list");
        var kinds=new HashSet<com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind>();
        for(var name:raw)kinds.add(switch((String)name){case "barrel"->com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.BARREL;case "smithing_table"->com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind.SMITHING_TABLE;default->throw new IllegalArgumentException("Unsupported workplace marker "+name);});
        return new com.blueninjas24.civilizations.industry.WorkplaceObserverPolicy(enabled,smallInteger(s,"vertical-radius"),smallInteger(s,"cells-per-tick"),smallInteger(s,"interval-ticks"),smallInteger(s,"horizontal-radius-chunks"),kinds);
    }
    public static com.blueninjas24.civilizations.industry.MineFoundingPolicy readMineFounding(ConfigurationSection config) {
        if(!config.contains("automatic-mines"))return com.blueninjas24.civilizations.industry.MineFoundingPolicy.disabled();
        var s=section(config,"automatic-mines");exactKeys(s,Set.of("enabled","settlements-per-pulse","patches-per-review","founders-per-review","survey-lifetime-seconds","horizontal-range","vertical-range","minimum-age-months","founder-mining-experience","worker-capacity","startup-hundredths","capital-hundredths","mineral-yield-per-batch"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("automatic-mines.enabled must be true or false");
        var yield=new TreeMap<Commodity,Long>();var raw=section(s,"mineral-yield-per-batch");for(var key:raw.getKeys(false))yield.put(new Commodity(key),integer(raw,key));
        return new com.blueninjas24.civilizations.industry.MineFoundingPolicy(enabled,smallInteger(s,"settlements-per-pulse"),smallInteger(s,"patches-per-review"),smallInteger(s,"founders-per-review"),Math.multiplyExact(integer(s,"survey-lifetime-seconds"),1000),smallInteger(s,"horizontal-range"),smallInteger(s,"vertical-range"),integer(s,"minimum-age-months"),integer(s,"founder-mining-experience"),smallInteger(s,"worker-capacity"),integer(s,"startup-hundredths"),integer(s,"capital-hundredths"),yield);
    }
    public static com.blueninjas24.civilizations.industry.CareerStudyPolicy readCareerStudy(ConfigurationSection config) {
        if(!config.contains("automatic-career-study"))return com.blueninjas24.civilizations.industry.CareerStudyPolicy.disabled();
        var s=section(config,"automatic-career-study");exactKeys(s,Set.of("enabled","candidates-per-review","enrollments-per-review","industries"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("automatic-career-study.enabled must be true or false");
        if(!(s.get("industries") instanceof List<?> raw)||raw.size()>32||raw.stream().anyMatch(x->!(x instanceof String))||new HashSet<>(raw).size()!=raw.size())
            throw new IllegalArgumentException("automatic-career-study.industries must be a distinct bounded list");
        var kinds=new HashSet<String>();raw.forEach(x->kinds.add((String)x));
        return new com.blueninjas24.civilizations.industry.CareerStudyPolicy(enabled,smallInteger(s,"candidates-per-review"),smallInteger(s,"enrollments-per-review"),kinds);
    }
    public static com.blueninjas24.civilizations.industry.BusinessStaffingPolicy readBusinessStaffing(ConfigurationSection config) {
        if(!config.contains("automatic-business-staffing"))return com.blueninjas24.civilizations.industry.BusinessStaffingPolicy.disabled();
        var s=section(config,"automatic-business-staffing");exactKeys(s,Set.of("enabled","candidates-per-review","hires-per-review","industries"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("automatic-business-staffing.enabled must be true or false");
        if(!(s.get("industries") instanceof List<?> raw)||raw.size()>32||raw.stream().anyMatch(x->!(x instanceof String))||new HashSet<>(raw).size()!=raw.size())
            throw new IllegalArgumentException("automatic-business-staffing.industries must be a distinct bounded list");
        var kinds=new HashSet<String>();raw.forEach(x->kinds.add((String)x));
        return new com.blueninjas24.civilizations.industry.BusinessStaffingPolicy(enabled,smallInteger(s,"candidates-per-review"),smallInteger(s,"hires-per-review"),kinds);
    }
    public static com.blueninjas24.civilizations.industry.StartupGrantPolicy readStartupGrants(ConfigurationSection config) {
        if(!config.contains("startup-grants"))return com.blueninjas24.civilizations.industry.StartupGrantPolicy.disabled();
        var s=section(config,"startup-grants");
        exactKeys(s,Set.of("enabled","maximum-grant-hundredths","per-period-limit-hundredths","lifetime-limit-hundredths","minimum-treasury-hundredths","food-reserve-periods","hundredths-per-food-unit","industries"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("startup-grants.enabled must be true or false");
        if(!(s.get("industries") instanceof List<?> raw) || raw.stream().anyMatch(v->!(v instanceof String)) || new HashSet<>(raw).size()!=raw.size())
            throw new IllegalArgumentException("startup-grants.industries must be a unique text list");
        var kinds=new HashSet<String>();raw.forEach(v->kinds.add((String)v));
        return new com.blueninjas24.civilizations.industry.StartupGrantPolicy(enabled,integer(s,"maximum-grant-hundredths"),integer(s,"per-period-limit-hundredths"),integer(s,"lifetime-limit-hundredths"),integer(s,"minimum-treasury-hundredths"),smallInteger(s,"food-reserve-periods"),integer(s,"hundredths-per-food-unit"),kinds);
    }
    public static com.blueninjas24.civilizations.industry.BusinessResalePolicy readAutomaticResale(ConfigurationSection config) {
        if(!config.contains("automatic-business-resale"))return com.blueninjas24.civilizations.industry.BusinessResalePolicy.disabled();
        var s=section(config,"automatic-business-resale");exactKeys(s,Set.of("enabled","minimum-site-value-hundredths","startup-recovery-percent","sale-deadline-seconds","buyers-per-review","stock-value-hundredths-per-unit"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("automatic-business-resale.enabled must be true or false");
        var values=section(s,"stock-value-hundredths-per-unit");var prices=new TreeMap<Commodity,Long>();
        for(var key:values.getKeys(false))prices.put(new Commodity(key),integer(values,key));
        return new com.blueninjas24.civilizations.industry.BusinessResalePolicy(enabled,integer(s,"minimum-site-value-hundredths"),smallInteger(s,"startup-recovery-percent"),Math.multiplyExact(integer(s,"sale-deadline-seconds"),1000),smallInteger(s,"buyers-per-review"),prices);
    }
    public static com.blueninjas24.civilizations.industry.StudyPolicy readStudy(ConfigurationSection config) {
        if(!config.contains("resident-study"))return com.blueninjas24.civilizations.industry.StudyPolicy.disabled();
        var s=section(config,"resident-study");exactKeys(s,Set.of("enabled","gain-per-period","maximum-experience","students-per-period","active-courses-per-village","food-reserve-periods","material-reserve-units","skills"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("resident-study.enabled must be true or false");
        var courses=new TreeMap<String,com.blueninjas24.civilizations.industry.StudyPolicy.Course>();var catalog=section(s,"skills");
        for(var skill:catalog.getKeys(false)) {
            var course=section(catalog,skill);exactKeys(course,Set.of("target-experience","practice-supplies"));
            var materials=section(course,"practice-supplies");var costs=new TreeMap<Commodity,Long>();
            for(var key:materials.getKeys(false))costs.put(new Commodity(key),integer(materials,key));
            courses.put(skill,new com.blueninjas24.civilizations.industry.StudyPolicy.Course(integer(course,"target-experience"),costs));
        }
        return new com.blueninjas24.civilizations.industry.StudyPolicy(enabled,integer(s,"gain-per-period"),integer(s,"maximum-experience"),smallInteger(s,"students-per-period"),smallInteger(s,"active-courses-per-village"),smallInteger(s,"food-reserve-periods"),integer(s,"material-reserve-units"),courses);
    }
    public static com.blueninjas24.civilizations.industry.WorkLearningPolicy readWorkLearning(ConfigurationSection config) {
        if(!config.contains("work-learning"))return com.blueninjas24.civilizations.industry.WorkLearningPolicy.disabled();
        var s=section(config,"work-learning");exactKeys(s,Set.of("enabled","experience-per-productive-period","maximum-experience","maximum-workers-per-review","industry-skills"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("work-learning.enabled must be true or false");
        var mappings=section(s,"industry-skills");var skills=new TreeMap<String,String>();
        for(var key:mappings.getKeys(false)) {
            if(!(mappings.get(key) instanceof String skill))throw new IllegalArgumentException("work-learning.industry-skills values must be skill names");
            skills.put(key,skill);
        }
        return new com.blueninjas24.civilizations.industry.WorkLearningPolicy(enabled,integer(s,"experience-per-productive-period"),integer(s,"maximum-experience"),smallInteger(s,"maximum-workers-per-review"),skills);
    }
    public static com.blueninjas24.civilizations.industry.BusinessPurchasePolicy readBusinessPurchasePolicy(ConfigurationSection config) {
        if(!config.contains("business-resale-funding"))return com.blueninjas24.civilizations.industry.BusinessPurchasePolicy.disabled();
        var s=section(config,"business-resale-funding");
        exactKeys(s,Set.of("minimum-retained-business-cash-hundredths","maximum-spare-business-percent","maximum-estate-percent"));
        return new com.blueninjas24.civilizations.industry.BusinessPurchasePolicy(integer(s,"minimum-retained-business-cash-hundredths"),
                smallInteger(s,"maximum-spare-business-percent"),smallInteger(s,"maximum-estate-percent"));
    }
    public static com.blueninjas24.civilizations.industry.WarehouseFoundingPolicy readWarehouseFounding(ConfigurationSection config) {
        if(!config.contains("warehouse-founding"))return com.blueninjas24.civilizations.industry.WarehouseFoundingPolicy.disabled();
        var s=section(config,"warehouse-founding");exactKeys(s,Set.of("enabled","founders-per-review","minimum-age-months","founder-commerce-experience","worker-commerce-experience","worker-capacity"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("warehouse-founding.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.WarehouseFoundingPolicy(enabled,smallInteger(s,"founders-per-review"),integer(s,"minimum-age-months"),integer(s,"founder-commerce-experience"),integer(s,"worker-commerce-experience"),smallInteger(s,"worker-capacity"));
    }
    public static com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy readWorkshopFounding(ConfigurationSection config) {
        if(!config.contains("workshop-founding"))return com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy.disabled();
        var s=section(config,"workshop-founding");exactKeys(s,Set.of("enabled","founders-per-review","minimum-age-months","founder-smithing-experience","worker-smithing-experience","worker-capacity"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("workshop-founding.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.WorkshopFoundingPolicy(enabled,smallInteger(s,"founders-per-review"),integer(s,"minimum-age-months"),integer(s,"founder-smithing-experience"),integer(s,"worker-smithing-experience"),smallInteger(s,"worker-capacity"));
    }
    public static com.blueninjas24.civilizations.industry.WorkplaceOperationPolicy readWorkplaceOperations(ConfigurationSection config) {
        if(!config.contains("workplace-operations"))return com.blueninjas24.civilizations.industry.WorkplaceOperationPolicy.prototype();
        var s=section(config,"workplace-operations");exactKeys(s,Set.of("pause-on-missing"));
        if(!(s.get("pause-on-missing") instanceof Boolean pause))throw new IllegalArgumentException("workplace-operations.pause-on-missing must be true or false");
        return new com.blueninjas24.civilizations.industry.WorkplaceOperationPolicy(pause);
    }
    public static com.blueninjas24.civilizations.industry.BusinessOperationPolicy readBusinessOperations(ConfigurationSection config) {
        if(!config.contains("business-operations"))return com.blueninjas24.civilizations.industry.BusinessOperationPolicy.disabled();
        var s=section(config,"business-operations");exactKeys(s,Set.of("enabled","continue-staffed-after-owner-death"));
        if(!(s.get("enabled") instanceof Boolean enabled)||!(s.get("continue-staffed-after-owner-death") instanceof Boolean continued))
            throw new IllegalArgumentException("business-operations settings must be true or false");
        return new com.blueninjas24.civilizations.industry.BusinessOperationPolicy(enabled,continued);
    }
    public static com.blueninjas24.civilizations.industry.OreObserverPolicy readOreObserver(ConfigurationSection config) {
        if(!config.contains("ore-observer"))return com.blueninjas24.civilizations.industry.OreObserverPolicy.disabled();
        var s=section(config,"ore-observer");var keys=new HashSet<>(Set.of("enabled","vertical-radius","cells-per-tick","interval-ticks"));
        if(s.contains("horizontal-radius-chunks"))keys.add("horizontal-radius-chunks");exactKeys(s,keys);
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("ore-observer.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.OreObserverPolicy(enabled,smallInteger(s,"vertical-radius"),smallInteger(s,"cells-per-tick"),smallInteger(s,"interval-ticks"),
                s.contains("horizontal-radius-chunks")?smallInteger(s,"horizontal-radius-chunks"):0);
    }
    public static com.blueninjas24.civilizations.industry.FarmBusinessPolicy readFarmBusinesses(ConfigurationSection config) {
        if(!config.contains("farm-businesses"))return com.blueninjas24.civilizations.industry.FarmBusinessPolicy.disabled();
        var settings=section(config,"farm-businesses");
        exactKeys(settings,Set.of("enabled","worker-capacity","startup-trade-value-hundredths","minimum-age-months","minimum-farming-experience"));
        if(!(settings.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("farm-businesses.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.FarmBusinessPolicy(enabled,smallInteger(settings,"worker-capacity"),
                integer(settings,"startup-trade-value-hundredths"),integer(settings,"minimum-age-months"),integer(settings,"minimum-farming-experience"));
    }
    /** Backend recipe catalog. Parsing does not establish a workshop, provide stock or run production. */
    public static Map<String,com.blueninjas24.civilizations.industry.ProductionRecipe> readRecipes(ConfigurationSection config) {
        if(!config.contains("recipes"))return Map.of("smith_tools",com.blueninjas24.civilizations.industry.ProductionRecipe.smithTools());
        var catalog=section(config,"recipes");
        if(catalog.getKeys(false).size()>32)throw new IllegalArgumentException("At most 32 prototype recipes supported");
        var result=new TreeMap<String,com.blueninjas24.civilizations.industry.ProductionRecipe>();
        for(var key:catalog.getKeys(false)) {
            var entry=section(catalog,key);exactKeys(entry,Set.of("industry","inputs","outputs","batches-per-worker","site-batch-capacity"));
            if(!(entry.get("industry") instanceof String industry))throw new IllegalArgumentException("Recipe industry must be text");
            var inputs=new TreeMap<Commodity,Long>();var outputs=new TreeMap<Commodity,Long>();
            var in=section(entry,"inputs");var out=section(entry,"outputs");
            for(var good:in.getKeys(false))inputs.put(new Commodity(good),integer(in,good));
            for(var good:out.getKeys(false))outputs.put(new Commodity(good),integer(out,good));
            result.put(key,new com.blueninjas24.civilizations.industry.ProductionRecipe(key,industry,inputs,outputs,
                    integer(entry,"batches-per-worker"),integer(entry,"site-batch-capacity")));
        }
        return Collections.unmodifiableMap(result);
    }
    /** Backend settings only until reliable discovery/founding/production adapters are implemented. */
    public static IndustryPolicy readIndustry(ConfigurationSection config) {
        if(!config.contains("industry"))return IndustryPolicy.prototype();
        var settings=section(config,"industry");
        exactKeys(settings,Set.of("deposit-batches-per-observed-ore","batches-per-worker","site-batch-capacity","storage-capacity",
                "local-delivery-periods","local-haul-units-per-period","mining-bundle","storage-space-per-unit"));
        Map<Commodity,Long> bundle=new TreeMap<>(),space=new TreeMap<>();
        var yields=section(settings,"mining-bundle");
        for(var key:yields.getKeys(false))bundle.put(new Commodity(key),integer(yields,key));
        var sizes=section(settings,"storage-space-per-unit");
        for(var key:sizes.getKeys(false))space.put(new Commodity(key),integer(sizes,key));
        return new IndustryPolicy(integer(settings,"deposit-batches-per-observed-ore"),integer(settings,"batches-per-worker"),
                integer(settings,"site-batch-capacity"),integer(settings,"storage-capacity"),integer(settings,"local-delivery-periods"),
                integer(settings,"local-haul-units-per-period"),bundle,space);
    }

    /** Preserve existing commented format-1 files: an entirely absent new section gets documented defaults. */
    public static FarmObservation.Policy readFarms(ConfigurationSection config) {
        if (!config.contains("farms")) return FarmObservation.Policy.prototype();
        exactKeys(section(config, "farms"), Set.of("radius-blocks", "vertical-radius-blocks", "crops-per-worker"));
        return new FarmObservation.Policy(smallInteger(config, "farms.radius-blocks"), smallInteger(config, "farms.vertical-radius-blocks"),
                smallInteger(config, "farms.crops-per-worker"));
    }
    public static HouseholdWellbeing.Policy readWellbeing(ConfigurationSection config) {
        if (!config.contains("wellbeing")) return HouseholdWellbeing.Policy.disabled();
        var section = section(config, "wellbeing");
        exactKeys(section, Set.of("enabled", "starting-score", "recovery-per-period", "food-shortage-penalty", "food-crisis-penalty", "crowding-penalty"));
        if (!(section.get("enabled") instanceof Boolean enabled)) throw new IllegalArgumentException("wellbeing.enabled must be true or false");
        return new HouseholdWellbeing.Policy(enabled, smallInteger(section,"starting-score"), smallInteger(section,"recovery-per-period"),
                smallInteger(section,"food-shortage-penalty"), smallInteger(section,"food-crisis-penalty"), smallInteger(section,"crowding-penalty"));
    }
    public static FoodBargaining.Policy readBargaining(ConfigurationSection config) {
        if(!config.contains("bargaining"))return FoodBargaining.Policy.disabled();
        var section=section(config,"bargaining");exactKeys(section,Set.of("enabled","maximum-failed-offers","cooldown-seconds","offer-seconds"));
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("bargaining.enabled must be true or false");
        return new FoodBargaining.Policy(enabled,smallInteger(section,"maximum-failed-offers"),smallInteger(section,"cooldown-seconds"),smallInteger(section,"offer-seconds"));
    }
    public static FoodBarters.Policy readBarter(ConfigurationSection config) {
        if(!config.contains("barter"))return FoodBarters.Policy.disabled();
        var section=section(config,"barter");exactKeys(section,Set.of("enabled","food-reserve-periods","maximum-items","goods"));
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("barter.enabled must be true or false");
        var goods=new TreeMap<Commodity,FoodBarters.Goods>();var entries=section(section,"goods");
        for(var id:entries.getKeys(false)) {
            var entry=section(entries,id);exactKeys(entry,Set.of("item","units-per-item","keep-units"));
            if(!(entry.get("item") instanceof String item))throw new IllegalArgumentException("barter item must be a material name");
            goods.put(new Commodity(id),new FoodBarters.Goods(item,integer(entry,"units-per-item"),integer(entry,"keep-units")));
        }
        return new FoodBarters.Policy(enabled,smallInteger(section,"food-reserve-periods"),smallInteger(section,"maximum-items"),goods);
    }
    public static VillagePriorities.Policy readPriorities(ConfigurationSection config) {
        if (!config.contains("priorities")) return VillagePriorities.Policy.prototype();
        var keys=new java.util.HashSet<>(Set.of("review-after-periods","food-weight","crisis-multiplier","housing-weight","monitor-weight","minimum-relative-percent"));
        if(config.contains("priorities.learning"))keys.add("learning");
        exactKeys(section(config,"priorities"),keys);
        var policy=new VillagePriorities.Policy(smallInteger(config,"priorities.review-after-periods"),smallInteger(config,"priorities.food-weight"),
                smallInteger(config,"priorities.crisis-multiplier"),smallInteger(config,"priorities.housing-weight"),smallInteger(config,"priorities.monitor-weight"),
                smallInteger(config,"priorities.minimum-relative-percent"));
        if(!config.contains("priorities.learning"))return policy;
        exactKeys(section(config,"priorities.learning"),Set.of("window-periods","maximum-food-bonus-percent"));
        return policy.withLearning(smallInteger(config,"priorities.learning.window-periods"),smallInteger(config,"priorities.learning.maximum-food-bonus-percent"));
    }
    public static FoodResponses.Settings readResponses(ConfigurationSection config) {
        var policy=FoodResponses.Policy.disabled();
        if(config.contains("responses")) {
            var section=section(config,"responses");exactKeys(section,Set.of("enabled","donation-weight","emerald-weight","barter-weight","wait-weight","minimum-relative-percent"));
            if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("responses.enabled must be true or false");
            policy=new FoodResponses.Policy(enabled,smallInteger(section,"donation-weight"),smallInteger(section,"emerald-weight"),smallInteger(section,"barter-weight"),smallInteger(section,"wait-weight"),smallInteger(section,"minimum-relative-percent"));
        }
        return new FoodResponses.Settings(policy,readDonationsEnabled(config),readPurchases(config),readBarter(config),readAidPreview(config));
    }
    /** Old owner files retain their read-only behavior until donations are explicitly enabled. */
    public static boolean readDonationsEnabled(ConfigurationSection config) {
        if(!config.contains("donations"))return false;
        var section=section(config,"donations");exactKeys(section,Set.of("enabled"));
        if(!(section.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("donations.enabled must be true or false");
        return enabled;
    }
    public static FoodAidPreview.Policy readAidPreview(ConfigurationSection config) {
        if (!config.contains("aid-preview")) return FoodAidPreview.Policy.prototype();
        var section = section(config, "aid-preview");
        exactKeys(section, Set.of("enabled", "radius-blocks", "units-per-item"));
        if (!(section.get("enabled") instanceof Boolean enabled)) throw new IllegalArgumentException("aid-preview.enabled must be true or false");
        var items = section(section, "units-per-item");
        var units = new TreeMap<String, Long>();
        var allowed = Set.of("WHEAT", "BREAD", "CARROT", "POTATO", "BAKED_POTATO", "BEETROOT");
        for (var id : items.getKeys(false)) {
            if (!allowed.contains(id)) throw new IllegalArgumentException("Unsupported aid-preview food: " + id);
            units.put(id, integer(items, id));
        }
        return new FoodAidPreview.Policy(enabled, smallInteger(section, "radius-blocks"), units);
    }
    /** Existing owner files require explicit opt-in; never silently mint opening capital. */
    public static OpeningFunds readOpeningFunds(ConfigurationSection config) {
        if (!config.contains("finance")) return OpeningFunds.disabled();
        var finance = section(config, "finance");
        exactKeys(finance, Set.of("opening-funds-enabled", "opening-trade-value-per-resident"));
        if (!(finance.get("opening-funds-enabled") instanceof Boolean enabled))
            throw new IllegalArgumentException("finance.opening-funds-enabled must be true or false");
        Object value = finance.get("opening-trade-value-per-resident");
        if (!(value instanceof Number)) throw new IllegalArgumentException("finance.opening-trade-value-per-resident must be a number");
        try {
            long cents = new BigDecimal(value.toString()).movePointRight(2).longValueExact();
            return new OpeningFunds(enabled, cents);
        } catch (NumberFormatException | ArithmeticException invalid) {
            throw new IllegalArgumentException("finance.opening-trade-value-per-resident must be finite with at most two decimal places", invalid);
        }
    }
    private static ConfigurationSection section(ConfigurationSection config, String path) {
        var section = config.getConfigurationSection(path);
        if (section == null) throw new IllegalArgumentException(path + " must be a YAML section"); return section;
    }
    public static FoodPurchases.Policy readPurchases(ConfigurationSection config) {
        if (!config.contains("purchases")) return FoodPurchases.Policy.disabled();
        var purchases = section(config, "purchases");
        exactKeys(purchases, Set.of("enabled", "trade-value-per-emerald", "food-reserve-periods", "maximum-emeralds"));
        if (!(purchases.get("enabled") instanceof Boolean enabled)) throw new IllegalArgumentException("purchases.enabled must be true or false");
        Object rate = purchases.get("trade-value-per-emerald");
        if (!(rate instanceof Number)) throw new IllegalArgumentException("purchases.trade-value-per-emerald must be a number");
        try {
            return new FoodPurchases.Policy(enabled, new BigDecimal(rate.toString()).movePointRight(2).longValueExact(),
                    smallInteger(purchases, "food-reserve-periods"), smallInteger(purchases, "maximum-emeralds"));
        } catch (NumberFormatException | ArithmeticException invalid) {
            throw new IllegalArgumentException("purchases.trade-value-per-emerald must be finite with at most two decimal places", invalid);
        }
    }
    public static com.blueninjas24.civilizations.industry.IndustryRuntimePolicy readIndustryRuntime(ConfigurationSection config) {
        if(!config.contains("industry-runtime"))return com.blueninjas24.civilizations.industry.IndustryRuntimePolicy.disabled();
        var runtime=section(config,"industry-runtime");
        exactKeys(runtime,Set.of("enabled","real-millis-per-period","businesses-per-pulse"));
        if(!(runtime.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("industry-runtime.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.IndustryRuntimePolicy(enabled,integer(runtime,"real-millis-per-period"),
                smallInteger(runtime,"businesses-per-pulse"),readIndustry(config),readRecipes(config),readWorkshopSupply(config),readWarehouseSupply(config),readToolUpkeep(config));
    }
    public static com.blueninjas24.civilizations.industry.ToolUpkeepPolicy readToolUpkeep(ConfigurationSection config) {
        if(!config.contains("business-tools"))return com.blueninjas24.civilizations.industry.ToolUpkeepPolicy.disabled();
        var s=section(config,"business-tools");
        exactKeys(s,Set.of("enabled","startup-grace-periods","work-periods-per-tool","missing-tools-output-percent","tool-value-hundredths","industries"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("business-tools.enabled must be true or false");
        if(!(s.get("industries") instanceof List<?> raw)||raw.size()>32||raw.stream().anyMatch(x->!(x instanceof String))||new HashSet<>(raw).size()!=raw.size())
            throw new IllegalArgumentException("business-tools.industries must be a distinct bounded industry list");
        var kinds=new HashSet<String>();raw.forEach(x->kinds.add((String)x));
        return new com.blueninjas24.civilizations.industry.ToolUpkeepPolicy(enabled,smallInteger(s,"startup-grace-periods"),smallInteger(s,"work-periods-per-tool"),
                smallInteger(s,"missing-tools-output-percent"),integer(s,"tool-value-hundredths"),kinds);
    }
    public static com.blueninjas24.civilizations.industry.WarehouseSupplyPolicy readWarehouseSupply(ConfigurationSection config) {
        if(!config.contains("warehouse-supply"))return com.blueninjas24.civilizations.industry.WarehouseSupplyPolicy.disabled();
        var supply=section(config,"warehouse-supply");
        exactKeys(supply,Set.of("enabled","customers-per-review","producers-per-review"));
        if(!(supply.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("warehouse-supply.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.WarehouseSupplyPolicy(enabled,smallInteger(supply,"customers-per-review"),smallInteger(supply,"producers-per-review"));
    }
    /** Reuse editable base prices; no second currency, grant or invented supplier. */
    public static com.blueninjas24.civilizations.industry.WorkshopSupplyPolicy readWorkshopSupply(ConfigurationSection config) {
        if(!config.contains("industry-supply"))return com.blueninjas24.civilizations.industry.WorkshopSupplyPolicy.disabled();
        var supply=section(config,"industry-supply");
        exactKeys(supply,Set.of("enabled","input-periods","warehouses-per-review"));
        if(!(supply.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("industry-supply.enabled must be true or false");
        var values=new TreeMap<Commodity,Long>();
        var prices=section(config,"prices.base-trade-value");
        for(var id:prices.getKeys(false)) {
            Object value=prices.get(id);
            if(!(value instanceof Number))throw new IllegalArgumentException("prices.base-trade-value."+id+" must be a number");
            try {values.put(new Commodity(id),new BigDecimal(value.toString()).movePointRight(2).longValueExact());}
            catch(NumberFormatException|ArithmeticException invalid) {throw new IllegalArgumentException("Supply prices must be finite with at most two decimal places",invalid);}
        }
        return new com.blueninjas24.civilizations.industry.WorkshopSupplyPolicy(enabled,smallInteger(supply,"input-periods"),smallInteger(supply,"warehouses-per-review"),values);
    }
    public static com.blueninjas24.civilizations.industry.WorkshopStarterPolicy readWorkshopStarterSupplies(ConfigurationSection config) {
        if(!config.contains("workshop-starter-supplies"))return com.blueninjas24.civilizations.industry.WorkshopStarterPolicy.disabled();
        var supply=section(config,"workshop-starter-supplies");
        exactKeys(supply,Set.of("enabled","maximum-recipe-batches","maximum-purchase-hundredths","protected-units-per-material"));
        if(!(supply.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("workshop-starter-supplies.enabled must be true or false");
        var values=new TreeMap<Commodity,Long>();var prices=section(config,"prices.base-trade-value");
        for(var id:prices.getKeys(false)) {
            var good=new Commodity(id);if(good.equals(Commodity.FOOD))continue;
            Object value=prices.get(id);
            if(!(value instanceof Number))throw new IllegalArgumentException("Starter material prices must be numeric");
            try{values.put(good,new BigDecimal(value.toString()).movePointRight(2).longValueExact());}
            catch(NumberFormatException|ArithmeticException invalid){throw new IllegalArgumentException("Starter prices must be finite with at most two decimal places",invalid);}
        }
        return new com.blueninjas24.civilizations.industry.WorkshopStarterPolicy(enabled,smallInteger(supply,"maximum-recipe-batches"),
            integer(supply,"maximum-purchase-hundredths"),integer(supply,"protected-units-per-material"),values);
    }
    public static com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy readWorkshopOpportunities(ConfigurationSection config) {
        if(!config.contains("workshop-opportunities"))return com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy.disabled();
        var s=section(config,"workshop-opportunities");
        exactKeys(s,Set.of("enabled","markers-per-review","evidence-lifetime-millis","horizontal-range","vertical-range","startup-hundredths","capital-hundredths","storage-capacity"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("workshop-opportunities.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.WorkshopOpportunityPolicy(enabled,smallInteger(s,"markers-per-review"),integer(s,"evidence-lifetime-millis"),
            smallInteger(s,"horizontal-range"),smallInteger(s,"vertical-range"),integer(s,"startup-hundredths"),integer(s,"capital-hundredths"),integer(s,"storage-capacity"));
    }
    public static com.blueninjas24.civilizations.industry.WarehouseOpportunityPolicy readWarehouseOpportunities(ConfigurationSection config) {
        if(!config.contains("warehouse-opportunities"))return com.blueninjas24.civilizations.industry.WarehouseOpportunityPolicy.disabled();
        var s=section(config,"warehouse-opportunities");
        exactKeys(s,Set.of("enabled","markers-per-review","evidence-lifetime-millis","horizontal-range","vertical-range","startup-hundredths","capital-hundredths","storage-capacity"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("warehouse-opportunities.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.WarehouseOpportunityPolicy(enabled,smallInteger(s,"markers-per-review"),integer(s,"evidence-lifetime-millis"),
                smallInteger(s,"horizontal-range"),smallInteger(s,"vertical-range"),integer(s,"startup-hundredths"),integer(s,"capital-hundredths"),integer(s,"storage-capacity"));
    }
    public static com.blueninjas24.civilizations.industry.FishingPolicy readFishingResources(ConfigurationSection config) {
        if (!config.contains("fishing-resources")) return com.blueninjas24.civilizations.industry.FishingPolicy.disabled();
        var s = section(config, "fishing-resources");
        exactKeys(s, Set.of("enabled", "minimum-water-columns", "stock-per-water-column", "maximum-stock",
                "initial-stock-percent", "recovery-basis-points", "catch-per-worker", "site-catch-capacity"));
        if (!(s.get("enabled") instanceof Boolean enabled))
            throw new IllegalArgumentException("fishing-resources.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.FishingPolicy(enabled, smallInteger(s, "minimum-water-columns"),
                integer(s, "stock-per-water-column"), integer(s, "maximum-stock"), smallInteger(s, "initial-stock-percent"),
                smallInteger(s, "recovery-basis-points"), integer(s, "catch-per-worker"), integer(s, "site-catch-capacity"));
    }
    public static com.blueninjas24.civilizations.industry.QuarryPolicy readQuarryResources(ConfigurationSection config) {
        if(!config.contains("quarry-resources"))return com.blueninjas24.civilizations.industry.QuarryPolicy.disabled();
        var s=section(config,"quarry-resources");exactKeys(s,Set.of("enabled","stone","sand"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("quarry-resources.enabled must be true or false");
        return new com.blueninjas24.civilizations.industry.QuarryPolicy(enabled,quarryTerms(section(s,"stone")),quarryTerms(section(s,"sand")));
    }
    private static com.blueninjas24.civilizations.industry.QuarryPolicy.Terms quarryTerms(ConfigurationSection s) {
        exactKeys(s,Set.of("reserve-batches-per-block","units-per-batch","batches-per-worker","site-batch-capacity"));
        return new com.blueninjas24.civilizations.industry.QuarryPolicy.Terms(integer(s,"reserve-batches-per-block"),
            integer(s,"units-per-batch"),integer(s,"batches-per-worker"),integer(s,"site-batch-capacity"));
    }
    public static com.blueninjas24.civilizations.industry.VillageSupplyPolicy readVillageSupplies(ConfigurationSection config) {
        if(!config.contains("village-supply-purchases"))return com.blueninjas24.civilizations.industry.VillageSupplyPolicy.disabled();
        var s=section(config,"village-supply-purchases");
        exactKeys(s,Set.of("enabled","food-budget-per-period","material-budget-per-period","minimum-treasury","food-reserve-periods","delivery-periods","haul-units-per-period","prices"));
        if(!(s.get("enabled") instanceof Boolean enabled))throw new IllegalArgumentException("village-supply-purchases.enabled must be true or false");
        var p=section(s,"prices");var prices=new TreeMap<Commodity,Long>();
        for(var good:p.getKeys(false))prices.put(new Commodity(good),integer(p,good));
        return new com.blueninjas24.civilizations.industry.VillageSupplyPolicy(enabled,integer(s,"food-budget-per-period"),integer(s,"material-budget-per-period"),
            integer(s,"minimum-treasury"),Math.toIntExact(integer(s,"food-reserve-periods")),Math.toIntExact(integer(s,"delivery-periods")),integer(s,"haul-units-per-period"),prices);
    }
    public static List<String> readAdditionalOreEditors(ConfigurationSection config) {
        if (!config.contains("ore-provenance")) return List.of();
        var provenance = section(config, "ore-provenance");
        exactKeys(provenance, Set.of("additional-bulk-editor-plugins"));
        Object raw = provenance.get("additional-bulk-editor-plugins");
        if (!(raw instanceof List<?> names) || names.size() > 32)
            throw new IllegalArgumentException("additional-bulk-editor-plugins must be a list of at most 32 names");
        var result = new ArrayList<String>();
        for (Object value : names) {
            if (!(value instanceof String name) || !name.matches("[A-Za-z0-9_-]{1,64}"))
                throw new IllegalArgumentException("Bulk-editor names must be 1..64 letters, digits, underscore or hyphen");
            result.add(name);
        }
        return List.copyOf(result);
    }
    private static void exactKeys(ConfigurationSection section, Set<String> expected) {
        if (!section.getKeys(false).equals(expected)) throw new IllegalArgumentException("Economy keys at "
                + (section.getCurrentPath().isEmpty() ? "root" : section.getCurrentPath()) + " must be " + new TreeSet<>(expected));
    }
    private static long integer(ConfigurationSection config, String path) {
        Object value = config.get(path);
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) return ((Number)value).longValue();
        throw new IllegalArgumentException(path + " must be a whole number");
    }
    private static int smallInteger(ConfigurationSection config, String path) {
        try { return Math.toIntExact(integer(config, path)); }
        catch (ArithmeticException invalid) { throw new IllegalArgumentException(path + " is too large", invalid); }
    }
    private static int basisPoints(ConfigurationSection config, String path) {
        try { return Math.multiplyExact(smallInteger(config, path), 100); }
        catch (ArithmeticException invalid) { throw new IllegalArgumentException(path + " is too large", invalid); }
    }
}
