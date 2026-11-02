package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import com.blueninjas24.civilizations.industry.WorkplaceObservation.Kind;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkplaceInspectionTest {
    final UUID world=UUID.randomUUID();final Position marker=new Position(3,1,2);
    final BuildingLayout.Bounds bounds=new BuildingLayout.Bounds(new Position(0,0,0),new Position(4,3,4));
    final BuildingLayout layout=new BuildingLayout(UUID.randomUUID(),UUID.randomUUID(),world,BuildingLayout.Kind.WORKPLACE,bounds,bounds,new Position(0,1,2),List.of(),List.of());
    Map<Position,Cell> cells(){var cells=new HashMap<Position,Cell>();for(var p:DwellingInspection.requiredCells(layout))cells.put(p,Cell.of(p.y()==0||p.y()==3?Type.SUPPORT:Type.PASSABLE));cells.put(marker,Cell.of(Type.SUPPORT));return cells;}
    WorkplaceInspection.Evidence evidence(Map<Position,Cell> cells){return new WorkplaceInspection.Evidence(new Observation(world,layout.fingerprint(),1000,true,cells),marker,Optional.of(Kind.SMITHING_TABLE),true);}
    WorkplaceInspection.Assessment inspect(WorkplaceInspection.Evidence evidence){return WorkplaceInspection.inspect(layout,evidence,1100,5000,WorkplaceInspection.Policy.prototype());}
    @Test void safeCoveredRouteIsDeterministicAndReportsAnActualStandingSpace(){
        var cells=cells();var result=inspect(evidence(cells));assertTrue(result.suitable());var at=result.workPosition().orElseThrow();
        assertEquals(1,at.y());assertEquals(1,Math.abs(at.x()-marker.x())+Math.abs(at.z()-marker.z()));
        var reversed=new TreeMap<Position,Cell>(Comparator.reverseOrder());reversed.putAll(cells);assertEquals(result.workPosition(),inspect(evidence(reversed)).workPosition());
    }
    @Test void blockedRoutesUnsafeEntranceAndHazardsRefuse(){
        var cells=cells();for(int z=0;z<5;z++)for(int y=1;y<3;y++)cells.put(new Position(2,y,z),Cell.of(Type.SUPPORT));
        assertTrue(inspect(evidence(cells)).issues().contains(WorkplaceInspection.Issue.WORK_UNREACHABLE));
        cells=cells();cells.put(layout.entrance(),Cell.of(Type.BLOCKED));assertTrue(inspect(evidence(cells)).issues().contains(WorkplaceInspection.Issue.ENTRANCE_UNSAFE));
        cells=cells();cells.put(new Position(4,1,4),Cell.of(Type.HAZARD));assertTrue(inspect(evidence(cells)).issues().contains(WorkplaceInspection.Issue.HAZARD));
    }
    @Test void missingRoofIsExplicitAndTheInspectionPreferenceDoesNotDisableAccessChecks(){
        var cells=cells();for(int x=0;x<5;x++)for(int z=0;z<5;z++)cells.put(new Position(x,3,z),Cell.of(Type.PASSABLE));
        var open=evidence(cells);assertEquals(Set.of(WorkplaceInspection.Issue.WORK_UNCOVERED),inspect(open).issues());
        assertTrue(WorkplaceInspection.inspect(layout,open,1100,5000,new WorkplaceInspection.Policy(false)).suitable());
    }
    @Test void incompleteWrongWorldStaleAndChangedMarkerEvidenceCannotBecomeSuitable(){
        var cells=cells();cells.remove(new Position(4,3,4));assertTrue(inspect(evidence(cells)).issues().contains(WorkplaceInspection.Issue.INCOMPLETE));
        var original=evidence(cells());var foreign=new Observation(UUID.randomUUID(),layout.fingerprint(),1000,true,cells());
        assertTrue(inspect(new WorkplaceInspection.Evidence(foreign,marker,Optional.of(Kind.SMITHING_TABLE),true)).issues().contains(WorkplaceInspection.Issue.WRONG_WORLD));
        assertTrue(WorkplaceInspection.inspect(layout,original,6001,5000,WorkplaceInspection.Policy.prototype()).issues().contains(WorkplaceInspection.Issue.STALE_OR_FUTURE));
        assertTrue(inspect(new WorkplaceInspection.Evidence(original.structure(),marker,original.kind(),false)).issues().contains(WorkplaceInspection.Issue.MARKER_UNVERIFIED));
        var contradictory=cells();contradictory.put(marker,Cell.of(Type.PASSABLE));assertTrue(inspect(evidence(contradictory)).issues().contains(WorkplaceInspection.Issue.MARKER_UNVERIFIED));
        assertTrue(inspect(new WorkplaceInspection.Evidence(original.structure(),new Position(50,1,2),original.kind(),true)).issues().contains(WorkplaceInspection.Issue.MARKER_OUTSIDE));
    }
}
