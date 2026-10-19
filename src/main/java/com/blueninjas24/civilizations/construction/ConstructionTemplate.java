package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.economy.Commodity;
import java.security.*;
import java.util.*;

/** Immutable authored construction inputs. Geometry and costs confer no land rights or economic credit. */
public record ConstructionTemplate(String id,int revision,String name,String attribution,Purpose purpose,
                                   Size size,List<Stage> stages,List<Anchor> anchors) {
    public static final int MAX_BLOCKS=16_384,MAX_STAGES=16,MAX_ANCHORS=64;
    public enum Purpose { PATH, STORAGE, HOME, WORKSHOP, DOCK }
    public enum Role { ENTRANCE, ROAD, WORK, STORAGE, BED }
    public enum Direction {
        NORTH,EAST,SOUTH,WEST;
        public Direction transform(Transform transform) {
            int face=ordinal();if(transform.mirror())face=Math.floorMod(-face,4);
            return values()[(face+transform.rotation().ordinal())%4];
        }
    }
    public enum Rotation { NONE,CLOCKWISE_90,CLOCKWISE_180,COUNTERCLOCKWISE_90 }
    public record Position(int x,int y,int z) implements Comparable<Position> {
        public Position plus(Position other){return new Position(Math.addExact(x,other.x),Math.addExact(y,other.y),Math.addExact(z,other.z));}
        @Override public int compareTo(Position other){int n=Integer.compare(y,other.y);if(n==0)n=Integer.compare(z,other.z);return n==0?Integer.compare(x,other.x):n;}
    }
    public record Size(int x,int y,int z) {
        public Size {
            if(x<1||y<1||z<1||x>64||y>64||z>64||(long)x*y*z>MAX_BLOCKS)
                throw new IllegalArgumentException("Template dimensions must be 1..64 and volume at most "+MAX_BLOCKS);
        }
        public boolean contains(Position p){return p.x()>=0&&p.x()<x&&p.y()>=0&&p.y()<y&&p.z()>=0&&p.z()<z;}
    }
    /** Mirror authored X first, then rotate clockwise about its bounding rectangle. Block state rotation is adapter work. */
    public record Transform(Rotation rotation,boolean mirror) {
        public Transform {Objects.requireNonNull(rotation);}
        public static Transform identity(){return new Transform(Rotation.NONE,false);}
        public Size size(Size original){return rotation.ordinal()%2==0?original:new Size(original.z(),original.y(),original.x());}
        public Position apply(Position p,Size original) {
            int x=mirror?Math.subtractExact(original.x()-1,p.x()):p.x(),z=p.z();
            return switch(rotation) {
                case NONE->new Position(x,p.y(),z);
                case CLOCKWISE_90->new Position(Math.subtractExact(original.z()-1,z),p.y(),x);
                case CLOCKWISE_180->new Position(Math.subtractExact(original.x()-1,x),p.y(),Math.subtractExact(original.z()-1,z));
                case COUNTERCLOCKWISE_90->new Position(z,p.y(),Math.subtractExact(original.x()-1,x));
            };
        }
    }
    public record Stage(String id,long work,Map<Commodity,Long> materials,Map<Position,String> blocks) {
        public Stage {
            requireId(id);materials=Collections.unmodifiableMap(new TreeMap<>(materials));
            blocks=Collections.unmodifiableMap(new TreeMap<>(blocks));
            if(work<1||work>1_000_000_000L||materials.isEmpty()||materials.size()>32||blocks.isEmpty()||blocks.size()>MAX_BLOCKS)
                throw new IllegalArgumentException("Stage requires bounded work, material costs and authored blocks");
            materials.forEach((commodity,amount)->{Objects.requireNonNull(commodity);if(amount<1||amount>1_000_000_000L)throw new IllegalArgumentException("Invalid construction material amount");});
            blocks.forEach((position,state)->{Objects.requireNonNull(position);requireBlockState(state);});
        }
    }
    public record Anchor(Role role,Position at,Direction facing) {
        public Anchor {Objects.requireNonNull(role);Objects.requireNonNull(at);Objects.requireNonNull(facing);}
    }
    public ConstructionTemplate {
        requireId(id);Objects.requireNonNull(purpose);Objects.requireNonNull(size);
        if(revision<1||revision>1_000_000)throw new IllegalArgumentException("Invalid template revision");
        text(name,96);text(attribution,256);stages=List.copyOf(stages);anchors=List.copyOf(anchors);
        if(stages.isEmpty()||stages.size()>MAX_STAGES||anchors.size()>MAX_ANCHORS||new HashSet<>(anchors).size()!=anchors.size())
            throw new IllegalArgumentException("Invalid template stage or anchor count");
        var stageIds=new HashSet<String>();var occupied=new HashSet<Position>();
        for(var stage:stages) {
            if(!stageIds.add(stage.id()))throw new IllegalArgumentException("Duplicate template stage");
            for(var position:stage.blocks().keySet())
                if(!size.contains(position)||!occupied.add(position))throw new IllegalArgumentException("Template blocks overlap or exceed dimensions");
        }
        for(var anchor:anchors) {
            var p=anchor.at();boolean road=anchor.role()==Role.ROAD;
            if(!size.contains(p)&&!(road&&p.x()>=-1&&p.x()<=size.x()&&p.y()>=-1&&p.y()<=size.y()&&p.z()>=-1&&p.z()<=size.z()))
                throw new IllegalArgumentException("Template anchor outside allowed bounds");
        }
        if(purpose!=Purpose.PATH&&(anchors.stream().noneMatch(a->a.role()==Role.ENTRANCE)||anchors.stream().noneMatch(a->a.role()==Role.ROAD)))
            throw new IllegalArgumentException("Building templates need entrance and road connection metadata");
    }
    public Map<Commodity,Long> materials() {
        var result=new TreeMap<Commodity,Long>();for(var stage:stages)stage.materials().forEach((key,value)->result.merge(key,value,Math::addExact));
        return Collections.unmodifiableMap(result);
    }
    public long work(){long total=0;for(var stage:stages)total=Math.addExact(total,stage.work());return total;}
    public int blockCount(){return stages.stream().mapToInt(s->s.blocks().size()).sum();}
    /** Canonical semantic hash; whitespace/palette ordering do not change an accepted blueprint. */
    public String fingerprint() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ConstructionTemplateSnapshot.encode(this)));
        }catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    public static void requireId(String value){if(value==null||!value.matches("[a-z][a-z0-9_]{0,63}"))throw new IllegalArgumentException("Invalid template/stage identifier");}
    private static void text(String value,int maximum){if(value==null||value.isBlank()||value.length()>maximum||value.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid template text");}
    private static void requireBlockState(String state) {
        if(state==null||state.length()>256||!state.matches("minecraft:[a-z0-9_]+(?:\\[[a-z0-9_=,]+\\])?"))throw new IllegalArgumentException("Invalid block state syntax");
        String type=state.split("\\[",2)[0];
        if(Set.of("minecraft:air","minecraft:cave_air","minecraft:void_air","minecraft:lava","minecraft:water","minecraft:tnt",
                "minecraft:fire","minecraft:soul_fire","minecraft:bedrock","minecraft:barrier","minecraft:command_block",
                "minecraft:chain_command_block","minecraft:repeating_command_block","minecraft:structure_block","minecraft:jigsaw",
                "minecraft:end_portal","minecraft:end_gateway","minecraft:nether_portal").contains(type))
            throw new IllegalArgumentException("Block outside the initial non-destructive building catalogue");
    }
}
