package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.paper.ConstructionTemplateConfiguration;
import com.blueninjas24.civilizations.population.Household;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FamilyHomeCatalogueTest {
    ConstructionTemplate read(String id)throws Exception{return ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/"+id+".yml"));}
    @Test void largerOriginalHomesFitNativeBudgetRealInputsAndWholeFamilyInspection()throws Exception{
        for(int count:List.of(6,8)){
            var plan=read(count==6?"family_home_six":"family_home_eight");assertEquals(count,HomePlanCapacity.beds(plan));assertEquals(count==6?143:167,plan.blockCount());
            assertEquals(count*3,plan.materials().get(new com.blueninjas24.civilizations.economy.Commodity("wool")));
            var fixtures=plan.stages().stream().filter(s->s.id().equals("fixtures")).findFirst().orElseThrow();assertEquals(1,fixtures.blocks().size());assertEquals(1,fixtures.materials().get(com.blueninjas24.civilizations.economy.Commodity.TIMBER));assertTrue(fixtures.blocks().values().iterator().next().startsWith("minecraft:wall_torch"));
            var prices=ConstructionSupplyPolicy.prototype().prices();long value=plan.materials().entrySet().stream().mapToLong(e->prices.get(e.getKey())*e.getValue()).sum();assertEquals(count==6?3900:4870,value);
            assertTrue(ConstructionBudgetPolicy.prototype().review(ConstructionBudgetPolicy.Source.VILLAGE,Purpose.HOME,value,new ConstructionBudgetPolicy.Evidence(100000,12,0,0,0)).allowed());
            assertTrue(plan.materials().values().stream().mapToLong(Long::longValue).sum()<=com.blueninjas24.civilizations.industry.IndustryPolicy.prototype().localHaulUnitsPerPeriod());
            for(var rotation:Rotation.values())for(boolean mirror:List.of(false,true)){
                var transform=new Transform(rotation,mirror);var required=ConstructionPreview.requiredCells(plan,transform,new Position(0,64,0));assertEquals(count==6?211:253,required.size());assertTrue(required.size()+1<=256);
                var d=new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new Position(0,64,0),transform,plan,1000);assertEquals(count,CivHomeLayout.from(d).units().getFirst().beds().size());
            }
            var d=new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new Position(0,64,0),Transform.identity(),plan,1000);var layout=CivHomeLayout.from(d);var cells=new HashMap<Position,DwellingInspection.Cell>();
            for(var p:DwellingInspection.requiredCells(layout))cells.put(p,DwellingInspection.Cell.of(DwellingInspection.Type.PASSABLE));
            for(var stage:plan.stages())for(var entry:stage.blocks().entrySet()){
                var state=entry.getValue();var at=d.origin().plus(entry.getKey());String material=state.split("\\[",2)[0];
                Direction facing=state.contains("facing=west")?Direction.WEST:Direction.EAST;
                cells.put(at,material.endsWith("_bed")?DwellingInspection.Cell.bed(state.contains("part=head"),material,facing):DwellingInspection.Cell.of(material.endsWith("_door")?DwellingInspection.Type.PASSABLE:material.endsWith("_torch")?DwellingInspection.Type.BLOCKED:DwellingInspection.Type.SUPPORT));
            }
            var inspected=DwellingInspection.inspect(layout,new DwellingInspection.Observation(d.world(),layout.fingerprint(),1000,true,cells),1000,5000,DwellingInspection.Policy.prototype());assertEquals(count,inspected.units().values().iterator().next().accessibleBeds());
        }
    }
    @Test void plannerChoosesSmallestWholeFamilyFitAndKeepsSuitableVacancyAheadOfNewConstruction()throws Exception{
        var plans=List.of(read("family_home_eight"),read("starter_home"),read("family_home_six"));UUID town=UUID.randomUUID(),world=UUID.randomUUID();
        for(int members:List.of(3,5,7,9)){
            var household=new Household(UUID.randomUUID(),town,"Whole fixture family");var needs=List.of(new HousingProjectPlanner.Need(household,members,false));
            var choice=HousingProjectPlanner.prototype().review(town,world,needs,List.of(),plans,Set.of(),true,true,42,0).recommendations().getFirst();
            if(members==9)assertEquals(HousingProjectPlanner.Action.WAIT_PLAN,choice.action());
            else{assertEquals(HousingProjectPlanner.Action.PROPOSE,choice.action());assertEquals(members==3?"starter_home":members==5?"family_home_six":"family_home_eight",choice.templateId());}
            if(members<=8){var vacancy=new HousingProjectPlanner.Vacancy(UUID.randomUUID(),town,world,8,true,true,false);
                assertEquals(HousingProjectPlanner.Action.REUSE,HousingProjectPlanner.prototype().review(town,world,needs,List.of(vacancy),plans,Set.of(),true,true,42,0).recommendations().getFirst().action());}
        }
    }
}
