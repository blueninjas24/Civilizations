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
            Material.OAK_LOG,Material.BARREL,Material.OAK_DOOR);
    private final Map<String,ConstructionTemplate> templates;
    public PaperConstructionLibrary(Map<String,ConstructionTemplate> templates) {
        this.templates=Collections.unmodifiableMap(new TreeMap<>(templates));
    }
    public static PaperConstructionLibrary load(JavaPlugin plugin)throws IOException,org.bukkit.configuration.InvalidConfigurationException {
        requireMainThread();var result=new TreeMap<String,ConstructionTemplate>();
        for(String file:List.of("starter_path.yml","starter_storehouse.yml")) {
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
        return new PaperConstructionLibrary(result);
    }
    public Map<String,ConstructionTemplate> templates(){return templates;}
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
        return new ConstructionTemplate(template.id(),template.revision(),template.name(),template.attribution(),template.purpose(),transform.size(template.size()),stages,anchors);
    }
    private static void requireMainThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Construction block state preparation requires the server thread");}
}
