package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import java.util.*;

/** Bounded current-world housing assessment. Bed/access checks alone never imply full habitability, permission or occupancy. */
public final class DwellingInspection {
    private DwellingInspection() {}
    public static final int MAX_PATH_VISITS = 262_144;
    public enum Type { PASSABLE, SUPPORT, COVER, BLOCKED, HAZARD, UNKNOWN, BED_FOOT, BED_HEAD }
    /** Prototype housing rule; disabling cover never disables bed/support/access checks. */
    public record Policy(boolean requireCoveredBeds) {
        public static Policy prototype(){return new Policy(true);}
    }
    public enum Issue { WRONG_WORLD, LAYOUT_CHANGED, STALE_OR_FUTURE, INCOMPLETE, ENTRANCE_UNSAFE,
        BED_CHANGED, BED_UNSUPPORTED, BED_OBSTRUCTED, BED_UNCOVERED, UNDECLARED_PRIVATE_BED, UNIT_UNREACHABLE, BED_UNREACHABLE, PATH_BUDGET }
    /** PASSABLE means two-sided safe traversability; native classification must be conservative. */
    public record Cell(Type type, String bedMaterial, Direction facing) {
        public Cell {
            Objects.requireNonNull(type);
            boolean bed = type == Type.BED_FOOT || type == Type.BED_HEAD;
            if (bed ? bedMaterial == null || !bedMaterial.matches("minecraft:[a-z_]+_bed") || facing == null
                    : bedMaterial != null || facing != null) throw new IllegalArgumentException("Invalid dwelling cell");
        }
        public static Cell of(Type type) { return new Cell(type,null,null); }
        public static Cell bed(boolean head,String material,Direction facing) {
            return new Cell(head?Type.BED_HEAD:Type.BED_FOOT,material,facing);
        }
    }
    public record Observation(UUID world, String layoutHash, long capturedMillis, boolean complete, Map<Position,Cell> cells) {
        public Observation {
            Objects.requireNonNull(world); Objects.requireNonNull(layoutHash);
            if(capturedMillis<0 || cells.size()>BuildingLayout.MAX_STRUCTURE_VOLUME) throw new IllegalArgumentException("Invalid dwelling observation budget/time");
            cells=Map.copyOf(cells);
        }
    }
    public record UnitResult(UUID unit, int accessibleBeds, Set<Issue> issues) {
        public UnitResult { issues=Set.copyOf(issues); }
    }
    /** Only inspect() can create this assessment. Enclosure, consent and occupancy are separate gates. */
    public static final class Assessment {
        private final UUID world;
        private final String layoutHash;
        private final long capturedMillis;
        private final Policy policy;
        private final Set<Issue> issues;
        private final Map<UUID,UnitResult> units;
        private Assessment(Observation observation,Policy policy,Set<Issue> issues,Map<UUID,UnitResult> units) {
            world=observation.world();layoutHash=observation.layoutHash();capturedMillis=observation.capturedMillis();
            this.policy=policy;
            this.issues=Set.copyOf(issues);this.units=Map.copyOf(units);
        }
        public UUID world(){return world;}
        public String layoutHash(){return layoutHash;}
        public long capturedMillis(){return capturedMillis;}
        public Policy policy(){return policy;}
        public Set<Issue> issues(){return issues;}
        public Map<UUID,UnitResult> units(){return units;}
        public int accessibleBeds(){return units.values().stream().mapToInt(UnitResult::accessibleBeds).sum();}
    }

    public static Assessment inspect(BuildingLayout layout,Observation observation,long now,long maxAgeMillis) {
        return inspect(layout,observation,now,maxAgeMillis,Policy.prototype());
    }
    public static Assessment inspect(BuildingLayout layout,Observation observation,long now,long maxAgeMillis,Policy policy) {
        Objects.requireNonNull(policy);
        if(layout.kind()!=Kind.HOUSING) throw new IllegalArgumentException("Dwelling inspection requires housing");
        if(now<0 || maxAgeMillis<1 || maxAgeMillis>60_000) throw new IllegalArgumentException("Invalid inspection freshness limit");
        var issues=EnumSet.noneOf(Issue.class);var results=new TreeMap<UUID,UnitResult>();
        if(!layout.world().equals(observation.world()))issues.add(Issue.WRONG_WORLD);
        if(!layout.fingerprint().equals(observation.layoutHash()))issues.add(Issue.LAYOUT_CHANGED);
        if(observation.capturedMillis()>now || now-observation.capturedMillis()>maxAgeMillis)issues.add(Issue.STALE_OR_FUTURE);
        var required=cells(layout.structure());
        if(!observation.complete() || !observation.cells().keySet().equals(required)
                || observation.cells().values().stream().anyMatch(c->c.type()==Type.UNKNOWN))issues.add(Issue.INCOMPLETE);
        if(!issues.isEmpty())return new Assessment(observation,policy,issues,results);
        var observed=observation.cells();
        // One bounded pass, rather than a vertical world scan for every bed.
        record Column(int x,int z){}
        var covers=new HashMap<Column,Integer>();
        if(policy.requireCoveredBeds())observed.forEach((p,c)->{
            if(c.type()==Type.SUPPORT||c.type()==Type.COVER)covers.merge(new Column(p.x(),p.z()),p.y(),Math::max);
        });
        if(!standable(layout.entrance(),observed)) {
            issues.add(Issue.ENTRANCE_UNSAFE);return new Assessment(observation,policy,issues,results);
        }
        var shared=new HashSet<Position>();layout.shared().forEach(b->shared.addAll(cells(b)));
        int[] visits={0};
        for(var unit:layout.units()) {
            var unitIssues=EnumSet.noneOf(Issue.class);var privateSpace=new HashSet<Position>();
            unit.spaces().forEach(b->privateSpace.addAll(cells(b)));
            var declared=new HashSet<Position>();
            for(var bed:unit.beds()) {
                declared.add(bed.foot());declared.add(bed.head());
                var foot=observed.get(bed.foot());var head=observed.get(bed.head());
                if(foot.type()!=Type.BED_FOOT || head.type()!=Type.BED_HEAD || foot.facing()!=bed.facing()
                        || head.facing()!=bed.facing() || !Objects.equals(foot.bedMaterial(),head.bedMaterial()))unitIssues.add(Issue.BED_CHANGED);
                if(!support(shift(bed.foot(),0,-1,0),observed) || !support(shift(bed.head(),0,-1,0),observed))unitIssues.add(Issue.BED_UNSUPPORTED);
                if(!passable(shift(bed.foot(),0,1,0),observed) || !passable(shift(bed.head(),0,1,0),observed))unitIssues.add(Issue.BED_OBSTRUCTED);
                if(policy.requireCoveredBeds())for(var half:List.of(bed.foot(),bed.head())) {
                    var cover=covers.get(new Column(half.x(),half.z()));
                    if(cover==null||(long)cover<(long)half.y()+2)unitIssues.add(Issue.BED_UNCOVERED);
                }
            }
            for(var p:privateSpace) {
                var type=observed.get(p).type();
                if((type==Type.BED_FOOT || type==Type.BED_HEAD) && !declared.contains(p))unitIssues.add(Issue.UNDECLARED_PRIVATE_BED);
            }
            if(unitIssues.isEmpty()) {
                var allowed=new HashSet<>(shared);allowed.addAll(privateSpace);
                Set<Position> reachable=reachable(layout.entrance(),allowed,observed,visits);
                if(reachable==null)unitIssues.add(Issue.PATH_BUDGET);
                else {
                    if(!reachable.contains(unit.entrance()))unitIssues.add(Issue.UNIT_UNREACHABLE);
                    for(var bed:unit.beds())if(!adjacent(bed.foot(),reachable) && !adjacent(bed.head(),reachable))unitIssues.add(Issue.BED_UNREACHABLE);
                }
            }
            results.put(unit.id(),new UnitResult(unit.id(),unitIssues.isEmpty()?unit.declaredBeds():0,unitIssues));
        }
        return new Assessment(observation,policy,issues,results);
    }

    public static Set<Position> requiredCells(BuildingLayout layout) { return Collections.unmodifiableSet(cells(layout.structure())); }
    private static Set<Position> cells(Bounds b) {
        var result=new HashSet<Position>();
        for(long y=b.min().y();y<=b.max().y();y++)for(long z=b.min().z();z<=b.max().z();z++)for(long x=b.min().x();x<=b.max().x();x++)
            result.add(new Position((int)x,(int)y,(int)z));
        return result;
    }
    private static Set<Position> reachable(Position entrance,Set<Position> allowed,Map<Position,Cell> observed,int[] visits) {
        var seen=new HashSet<Position>();var pending=new ArrayDeque<Position>();
        if(allowed.contains(entrance) && allowed.contains(shift(entrance,0,1,0)) && standable(entrance,observed)){seen.add(entrance);pending.add(entrance);}
        while(!pending.isEmpty()) {
            if(++visits[0]>MAX_PATH_VISITS)return null;
            var p=pending.removeFirst();
            for(int[] offset:HORIZONTAL)for(int dy=-1;dy<=1;dy++) {
                var next=shift(p,offset[0],dy,offset[1]);
                if(next==null || !allowed.contains(next) || !allowed.contains(shift(next,0,1,0)) || seen.contains(next) || !standable(next,observed))continue;
                // One-block steps need head clearance over the lower endpoint too; no vertical flying edges.
                if(dy!=0 && !passable(shift(dy>0?p:next,0,2,0),observed))continue;
                seen.add(next);pending.addLast(next);
            }
        }
        return seen;
    }
    private static final int[][] HORIZONTAL={{1,0},{-1,0},{0,1},{0,-1}};
    private static boolean adjacent(Position bed,Set<Position> reachable) {
        for(int[] delta:HORIZONTAL)if(reachable.contains(shift(bed,delta[0],0,delta[1])))return true;
        return false;
    }
    private static boolean standable(Position p,Map<Position,Cell> cells) {
        return passable(p,cells) && passable(shift(p,0,1,0),cells) && support(shift(p,0,-1,0),cells);
    }
    private static boolean passable(Position p,Map<Position,Cell> cells) { if(p==null)return false;var cell=cells.get(p);return cell!=null && cell.type()==Type.PASSABLE; }
    private static boolean support(Position p,Map<Position,Cell> cells) { if(p==null)return false;var cell=cells.get(p);return cell!=null && cell.type()==Type.SUPPORT; }
    private static Position shift(Position p,int dx,int dy,int dz) {
        if(p==null)return null;
        long x=(long)p.x()+dx,y=(long)p.y()+dy,z=(long)p.z()+dz;
        if(x<Integer.MIN_VALUE || x>Integer.MAX_VALUE || y<Integer.MIN_VALUE || y>Integer.MAX_VALUE || z<Integer.MIN_VALUE || z>Integer.MAX_VALUE)return null;
        return new Position((int)x,(int)y,(int)z);
    }
}
