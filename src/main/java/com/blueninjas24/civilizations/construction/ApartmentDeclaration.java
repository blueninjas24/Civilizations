package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Explicit new-building drafts. Editing a draft never edits registered geometry or grants permission. */
public final class ApartmentDeclaration {
    private ApartmentDeclaration() {}
    public record PrivateUnit(String name,Position entrance,List<Bounds> spaces) {
        public PrivateUnit {
            Objects.requireNonNull(name);Objects.requireNonNull(entrance);spaces=List.copyOf(spaces);
            if(!name.matches("[a-z0-9_-]{1,24}")||spaces.isEmpty()||spaces.size()>BuildingLayout.MAX_SPACES
                    ||spaces.stream().noneMatch(b->b.contains(entrance)))
                throw new IllegalArgumentException("Unit needs a short lowercase name, private rooms and an entrance in its rooms");
        }
    }
    public record Draft(UUID building,UUID village,UUID world,Bounds plot,Bounds structure,Position entrance,
                        List<Bounds> shared,List<PrivateUnit> units) {
        public Draft {
            Objects.requireNonNull(building);Objects.requireNonNull(village);Objects.requireNonNull(world);
            Objects.requireNonNull(plot);Objects.requireNonNull(structure);Objects.requireNonNull(entrance);
            shared=List.copyOf(shared);units=List.copyOf(units);
            if(!plot.contains(structure)||!structure.contains(entrance)||structure.volume()>BuildingLayout.MAX_STRUCTURE_VOLUME
                    ||shared.size()>BuildingLayout.MAX_SHARED||units.size()>BuildingLayout.MAX_UNITS)
                throw new IllegalArgumentException("Apartment structure/plot or geometry budget is invalid");
            var regions=new ArrayList<>(shared);var names=new HashSet<String>();
            for(var unit:units){if(!names.add(unit.name()))throw new IllegalArgumentException("Unit name already exists");regions.addAll(unit.spaces());}
            for(int i=0;i<regions.size();i++){
                if(!structure.contains(regions.get(i)))throw new IllegalArgumentException("All private/shared rooms must be inside the frozen structure");
                for(int j=0;j<i;j++)if(regions.get(i).overlaps(regions.get(j)))
                    throw new IllegalArgumentException("Private/shared rooms overlap; boundaries are inclusive");
            }
        }
        public Draft unit(String name,Position entry,Bounds space) {
            var next=new ArrayList<>(units);next.add(new PrivateUnit(name,entry,List.of(space)));
            return new Draft(building,village,world,plot,structure,entrance,shared,next);
        }
        public Draft room(String name,Bounds space) {
            var next=new ArrayList<PrivateUnit>();boolean found=false;
            for(var u:units)if(u.name().equals(name)){
                var rooms=new ArrayList<>(u.spaces());rooms.add(space);next.add(new PrivateUnit(name,u.entrance(),rooms));found=true;
            }else next.add(u);
            if(!found)throw new IllegalArgumentException("Unknown unit; declare it with unit first");
            return new Draft(building,village,world,plot,structure,entrance,shared,next);
        }
        public Draft remove(String name) {
            var next=units.stream().filter(u->!u.name().equals(name)).toList();
            if(next.size()==units.size())throw new IllegalArgumentException("Unknown unit");
            return new Draft(building,village,world,plot,structure,entrance,shared,next);
        }
        public Draft shared(Bounds space) {
            var next=new ArrayList<>(shared);next.add(space);
            return new Draft(building,village,world,plot,structure,entrance,next,units);
        }
        public Draft plot(Bounds bounds) {return new Draft(building,village,world,bounds,structure,entrance,shared,units);}
    }
    public static HomeDeclaration.Review review(Draft draft,Observation region,long now,Policy policy) {
        Objects.requireNonNull(draft);Objects.requireNonNull(region);Objects.requireNonNull(policy);
        var structure=draft.structure();
        if(!region.world().equals(draft.world())||!region.layoutHash().equals(HomeDeclaration.regionHash(draft.world(),structure))
                ||!region.complete()||region.capturedMillis()>now||now-region.capturedMillis()>30_000
                ||region.cells().size()!=structure.volume()||region.cells().entrySet().stream()
                    .anyMatch(e->!structure.contains(e.getKey())||e.getValue().type()==Type.UNKNOWN))
            throw new IllegalArgumentException("Apartment evidence is incomplete, changed or expired; review again");
        if(draft.units().isEmpty())throw new IllegalArgumentException("Explicitly declare at least one private unit");
        var units=new ArrayList<Unit>();
        for(var u:draft.units()) {
            var beds=region.cells().entrySet().stream().filter(e->e.getValue().type()==Type.BED_FOOT
                    &&u.spaces().stream().anyMatch(b->b.contains(e.getKey())))
                    .map(e->new BuildingLayout.Bed(e.getKey(),e.getValue().facing())).sorted().toList();
            var id=UUID.nameUUIDFromBytes(("civilizations:apartment-unit:"+draft.building()+":"+u.name()).getBytes(StandardCharsets.UTF_8));
            units.add(new Unit(id,u.entrance(),u.spaces(),beds));
        }
        var layout=new BuildingLayout(draft.building(),draft.village(),draft.world(),Kind.HOUSING,draft.plot(),structure,
                draft.entrance(),draft.shared(),units);
        var observation=new Observation(draft.world(),layout.fingerprint(),region.capturedMillis(),true,region.cells());
        return new HomeDeclaration.Review(layout,DwellingInspection.inspect(layout,observation,now,30_000,policy));
    }
}
