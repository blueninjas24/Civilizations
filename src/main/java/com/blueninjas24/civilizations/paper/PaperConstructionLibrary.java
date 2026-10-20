package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.ConstructionTemplate;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Small, explicitly supported catalogue. Invalid local edits disable previews, never existing simulation. */
public final class PaperConstructionLibrary {
    private static final Set<Material> SUPPORTED=Set.of(Material.COBBLESTONE,Material.OAK_PLANKS,
            Material.OAK_LOG,Material.BARREL,Material.OAK_DOOR,Material.WHITE_BED);
    private final Map<String,ConstructionTemplate> templates;
    private final com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy funding;
    private final com.blueninjas24.civilizations.construction.ConstructionLandPolicy land;
    private final com.blueninjas24.civilizations.construction.ConstructionLaborPolicy labor;
    private final com.blueninjas24.civilizations.construction.HousingProjectPlanner housing;
    private final com.blueninjas24.civilizations.construction.ConstructionSupplyPolicy supplies;
    public PaperConstructionLibrary(Map<String,ConstructionTemplate> templates) {
        this(templates,com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.prototype(),new com.blueninjas24.civilizations.construction.ConstructionLandPolicy(4),com.blueninjas24.civilizations.construction.ConstructionLaborPolicy.prototype(),com.blueninjas24.civilizations.construction.HousingProjectPlanner.prototype(),com.blueninjas24.civilizations.construction.ConstructionSupplyPolicy.prototype());
    }
    private PaperConstructionLibrary(Map<String,ConstructionTemplate> templates,com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy funding,com.blueninjas24.civilizations.construction.ConstructionLandPolicy land,com.blueninjas24.civilizations.construction.ConstructionLaborPolicy labor,com.blueninjas24.civilizations.construction.HousingProjectPlanner housing,com.blueninjas24.civilizations.construction.ConstructionSupplyPolicy supplies) {
        this.templates=Collections.unmodifiableMap(new TreeMap<>(templates));
        this.funding=Objects.requireNonNull(funding);
        this.land=Objects.requireNonNull(land);
        this.labor=Objects.requireNonNull(labor);
        this.housing=Objects.requireNonNull(housing);
        this.supplies=Objects.requireNonNull(supplies);
    }
    public static PaperConstructionLibrary load(JavaPlugin plugin)throws IOException,org.bukkit.configuration.InvalidConfigurationException {
        requireMainThread();var result=new TreeMap<String,ConstructionTemplate>();
        Path settings=plugin.getDataFolder().toPath().resolve("construction.yml");
        if(!Files.exists(settings))plugin.saveResource("construction.yml",false);
        if(Files.isSymbolicLink(settings)||!Files.isRegularFile(settings)||Files.size(settings)>65_536)throw new IOException("Invalid construction.yml");
        var yaml=new org.bukkit.configuration.file.YamlConfiguration();yaml.load(settings.toFile());
        var funding=ConstructionBudgetConfiguration.read(yaml);
        for(String file:List.of("starter_path.yml","starter_storehouse.yml","starter_home.yml")) {
            String resource="construction/"+file;Path path=plugin.getDataFolder().toPath().resolve(resource);
            if(!Files.exists(path))plugin.saveResource(resource,false);
            if(Files.isSymbolicLink(path)||!Files.isRegularFile(path)||Files.size(path)>ConstructionTemplateConfiguration.MAX_FILE_BYTES)
                throw new IOException("Invalid or oversized construction file: "+file);
            ConstructionTemplate template;
            try(var input=Files.newInputStream(path)){template=ConstructionTemplateConfiguration.read(input);}
            if(result.putIfAbsent(template.id(),template)!=null)throw new IllegalArgumentException("Duplicate construction template: "+template.id());
            // All eight orientations must validate before a template is offered to an administrator.
            for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true))prepare(template,new Transform(rotation,mirror));
        }
        return new PaperConstructionLibrary(result,funding,ConstructionBudgetConfiguration.land(yaml),ConstructionBudgetConfiguration.labor(yaml),ConstructionBudgetConfiguration.housing(yaml),ConstructionBudgetConfiguration.supplies(yaml));
    }
    public Map<String,ConstructionTemplate> templates(){return templates;}
    public com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy funding(){return funding;}
    public com.blueninjas24.civilizations.construction.ConstructionLandPolicy land(){return land;}
    public com.blueninjas24.civilizations.construction.ConstructionLaborPolicy labor(){return labor;}
    public com.blueninjas24.civilizations.construction.HousingProjectPlanner housing(){return housing;}
    public com.blueninjas24.civilizations.construction.ConstructionSupplyPolicy supplies(){return supplies;}
    /** Resolve native block states on the server thread. Returned records contain no mutable Paper state. */
    public static ConstructionTemplate prepare(ConstructionTemplate template,Transform transform) {
        requireMainThread();var states=new HashMap<String,String>();var stages=new ArrayList<Stage>();
        for(var stage:template.stages()) {
            var blocks=new TreeMap<Position,String>();
            stage.blocks().forEach((position,state)->blocks.put(transform.apply(position,template.size()),states.computeIfAbsent(state,key->{
                var data=Bukkit.createBlockData(key);
                if(!SUPPORTED.contains(data.getMaterial()))throw new IllegalArgumentException("Unsupported starter construction material: "+data.getMaterial());
                if(transform.mirror())data.mirror(Mirror.FRONT_BACK);
                data.rotate(StructureRotation.valueOf(transform.rotation().name()));
                return data.getAsString();
            })));
            stages.add(new Stage(stage.id(),stage.work(),stage.materials(),blocks));
        }
        var anchors=template.anchors().stream().map(a->new Anchor(a.role(),transform.apply(a.at(),template.size()),a.facing().transform(transform))).toList();
        var prepared=new ConstructionTemplate(template.id(),template.revision(),template.name(),template.attribution(),template.purpose(),transform.size(template.size()),stages,anchors);
        if(prepared.purpose()==Purpose.HOME)com.blueninjas24.civilizations.construction.HomePlanCapacity.beds(prepared);
        return prepared;
    }
    private static void requireMainThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Construction block state preparation requires the server thread");}
}
