package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.ConstructionTemplate;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.economy.Commodity;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded authored YAML format. Paper validates actual block types before any world-facing use. */
public final class ConstructionTemplateConfiguration {
    public static final int MAX_FILE_BYTES=524_288;
    private ConstructionTemplateConfiguration() {}
    /** Consumes and closes the supplied stream; files are never rewritten. */
    public static ConstructionTemplate read(InputStream source)throws IOException,InvalidConfigurationException {
        byte[] bytes;try(source){bytes=Objects.requireNonNull(source).readNBytes(MAX_FILE_BYTES+1);}
        if(bytes.length>MAX_FILE_BYTES)throw new IOException("Construction template exceeds "+MAX_FILE_BYTES+" bytes");
        var yaml=new YamlConfiguration();yaml.loadFromString(new String(bytes,StandardCharsets.UTF_8));return read(yaml);
    }
    public static ConstructionTemplate read(ConfigurationSection yaml) {
        var root=map(yaml);keys(root,"format","id","revision","name","attribution","purpose","size","palette","stages","anchors");
        if(number(root.get("format"),1,1)!=1)throw new IllegalArgumentException("Unsupported construction template format");
        var dimensions=map(root.get("size"));keys(dimensions,"x","y","z");
        var size=new Size(number(dimensions.get("x"),1,64),number(dimensions.get("y"),1,64),number(dimensions.get("z"),1,64));
        var palette=map(root.get("palette"));if(palette.isEmpty()||palette.size()>64)throw new IllegalArgumentException("Palette must contain 1..64 block symbols");
        var states=new TreeMap<Character,String>();
        palette.forEach((key,value)->{if(!key.matches("[A-Za-z0-9]"))throw new IllegalArgumentException("Palette symbols must be single letters/digits; dot means skip");states.put(key.charAt(0),string(value));});
        var stages=new ArrayList<Stage>();var used=new HashSet<Character>();
        for(var value:list(root.get("stages"),ConstructionTemplate.MAX_STAGES)) {
            var stage=map(value);keys(stage,"id","work","materials","layers");var materials=new TreeMap<Commodity,Long>();
            map(stage.get("materials")).forEach((key,amount)->materials.put(new Commodity(key),amount(amount)));
            var blocks=new TreeMap<Position,String>();
            for(var layerValue:list(stage.get("layers"),64)) {
                var layer=map(layerValue);keys(layer,"y","rows");int y=number(layer.get("y"),0,size.y()-1);
                var rows=list(layer.get("rows"),size.z());if(rows.size()!=size.z())throw new IllegalArgumentException("Each layer must have exactly size.z rows");
                for(int z=0;z<rows.size();z++) {
                    String row=string(rows.get(z));if(row.length()!=size.x())throw new IllegalArgumentException("Each row must have exactly size.x symbols");
                    for(int x=0;x<row.length();x++) {
                        char symbol=row.charAt(x);if(symbol=='.')continue;String state=states.get(symbol);
                        if(state==null)throw new IllegalArgumentException("Undefined palette symbol "+symbol);
                        if(blocks.put(new Position(x,y,z),state)!=null)throw new IllegalArgumentException("Duplicate layer block");used.add(symbol);
                    }
                }
            }
            stages.add(new Stage(string(stage.get("id")),amount(stage.get("work")),materials,blocks));
        }
        if(!used.equals(states.keySet()))throw new IllegalArgumentException("Unused palette symbols; remove them or include their blocks");
        var anchors=new ArrayList<Anchor>();
        for(var value:list(root.getOrDefault("anchors",List.of()),ConstructionTemplate.MAX_ANCHORS)) {
            var anchor=map(value);keys(anchor,"role","at","facing");var at=list(anchor.get("at"),3);
            if(at.size()!=3)throw new IllegalArgumentException("Anchor needs x,y,z");
            anchors.add(new Anchor(enumeration(Role.class,anchor.get("role")),new Position(number(at.get(0),-1,64),number(at.get(1),-1,64),number(at.get(2),-1,64)),enumeration(Direction.class,anchor.get("facing"))));
        }
        return new ConstructionTemplate(string(root.get("id")),number(root.get("revision"),1,1_000_000),string(root.get("name")),
                string(root.get("attribution")),enumeration(Purpose.class,root.get("purpose")),size,stages,anchors);
    }
    private static Map<String,Object> map(Object raw) {
        if(raw instanceof ConfigurationSection section)return section.getValues(false);
        if(!(raw instanceof Map<?,?> values))throw new IllegalArgumentException("Expected construction settings map");
        var result=new LinkedHashMap<String,Object>();for(var item:values.entrySet()) {
            if(!(item.getKey() instanceof String key)||result.put(key,item.getValue())!=null)throw new IllegalArgumentException("Construction keys must be unique text");
        }
        return result;
    }
    private static List<?> list(Object value,int maximum){if(!(value instanceof List<?> list)||list.size()>maximum)throw new IllegalArgumentException("Expected bounded construction list");return list;}
    private static String string(Object value){if(!(value instanceof String text))throw new IllegalArgumentException("Expected construction text");return text;}
    private static long amount(Object value){if(!(value instanceof Integer||value instanceof Long))throw new IllegalArgumentException("Construction amounts must be integers");long n=((Number)value).longValue();if(n<1||n>1_000_000_000L)throw new IllegalArgumentException("Construction amount outside 1..1000000000");return n;}
    private static int number(Object value,int minimum,int maximum){if(!(value instanceof Integer||value instanceof Long))throw new IllegalArgumentException("Expected integer construction coordinate/version");long n=((Number)value).longValue();if(n<minimum||n>maximum)throw new IllegalArgumentException("Construction integer out of range");return (int)n;}
    private static void keys(Map<String,Object> values,String... allowed){var keys=Set.of(allowed);for(var key:values.keySet())if(!keys.contains(key))throw new IllegalArgumentException("Unknown construction key "+key);}
    private static <T extends Enum<T>> T enumeration(Class<T> type,Object raw){return Enum.valueOf(type,string(raw).toUpperCase(Locale.ROOT));}
}
