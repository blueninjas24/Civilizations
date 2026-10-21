package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Rotation;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.economy.Commodity;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperConstructionWorkSurveyTest {
    boolean loaded=true,border=true,primary=true,occupied;int reads;
    final Map<Position,Material> changed=new HashMap<>();final Position origin=new Position(0,64,0);
    final ConstructionTemplate plan=new ConstructionTemplate("fixture",1,"Fixture","Original fixture",Purpose.STORAGE,new Size(2,1,1),
            List.of(new Stage("floor",2,Map.of(Commodity.STONE,1L),Map.of(new Position(0,0,0),"minecraft:cobblestone",new Position(1,0,0),"minecraft:cobblestone"))),
            List.of(new Anchor(Role.ENTRANCE,new Position(0,0,0),Direction.NORTH),new Anchor(Role.ROAD,new Position(0,-1,-1),Direction.NORTH)));
    final ConstructionDraft draft=new ConstructionDraft(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),origin,Transform.identity(),plan,1000);
    @SuppressWarnings("unchecked") <T>T proxy(Class<T> type,BiFunction<String,Object[],Object> call){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->switch(m.getName()){
        case "hashCode"->System.identityHashCode(p);case "equals"->p==a[0];case "toString"->"Fixture";default->call.apply(m.getName(),a);
    });}
    final World world=proxy(World.class,(name,args)->switch(name){
        case "getMinHeight"->-64;case "getMaxHeight"->320;case "isChunkLoaded"->loaded;
        case "getWorldBorder"->proxy(WorldBorder.class,(m,a)->{assertEquals("isInside",m);return border;});
        case "getBlockAt"->{reads++;var at=new Position((Integer)args[0],(Integer)args[1],(Integer)args[2]);
            var material=changed.getOrDefault(at,at.equals(new Position(0,63,-1))?Material.DIRT_PATH:at.y()==63?Material.STONE:Material.AIR);
            yield proxy(Block.class,(m,a)->switch(m){case "getType"->material;case "getBlockData"->proxy(BlockData.class,(n,b)->{assertEquals("getAsString",n);return "minecraft:"+material.name().toLowerCase(Locale.ROOT);});default->throw new AssertionError("No block mutation allowed: "+m);});
        }
        case "getNearbyEntities"->occupied?List.of(proxy(Entity.class,(m,a)->null)):List.of();
        default->throw new AssertionError("No chunk loading/world mutation allowed: "+name);
    });
    ConstructionWorkQueue.Work work(int ordinal,Optional<ConstructionBlockJournal.Intent> pending){return new ConstructionWorkQueue.Work(draft,pending.isEmpty()?ConstructionWorkQueue.State.READY:ConstructionWorkQueue.State.RESUME,ordinal,pending,"Fixture saved work");}
    PaperConstructionWorkSurvey survey(int ordinal,Optional<ConstructionBlockJournal.Intent> pending){return new PaperConstructionWorkSurvey(work(ordinal,pending),plan,1000,id->world,()->primary);}
    PaperConstructionWorkSurvey.Frame finish(PaperConstructionWorkSurvey survey){for(int i=0;i<20;i++){int before=reads;var frame=survey.advance(1,1001+i);assertTrue(reads-before<=1);if(frame.isPresent())return frame.get();}throw new AssertionError("Bounded fixture never finished");}
    @Test void twoPassBudgetAndCurrentEntitiesNeverGrantWorldWritePermission(){
        var frame=finish(survey(0,Optional.empty()));assertTrue(frame.authority().siteCleared());assertEquals("minecraft:air",frame.nextObserved());
        occupied=true;assertFalse(finish(survey(0,Optional.empty())).authority().siteCleared());
    }
    @Test void onlyAcknowledgedPrefixOrExactPendingAfterStateMayOccupyTheVolume(){
        changed.put(origin,Material.COBBLESTONE);assertFalse(finish(survey(0,Optional.empty())).authority().siteCleared(),"An unjournaled matching block is not ours");
        assertTrue(finish(survey(1,Optional.empty())).authority().siteCleared());
        var pending=new ConstructionBlockJournal.Intent(UUID.randomUUID(),draft.id(),0,origin,"minecraft:air","minecraft:cobblestone",ConstructionBlockJournal.State.PREPARED);
        var frame=finish(survey(0,Optional.of(pending)));assertTrue(frame.authority().siteCleared());assertEquals(pending.after(),frame.nextObserved());
        changed.put(origin,Material.DIRT);assertFalse(finish(survey(1,Optional.empty())).authority().siteCleared());
    }
    @Test void absentChunkBorderWrongThreadAndExpiredFramesNeverReadOrLoadMissingBlocks(){
        loaded=false;assertFalse(finish(survey(0,Optional.empty())).authority().worldLoaded());assertEquals(0,reads);
        loaded=true;border=false;assertFalse(finish(survey(0,Optional.empty())).authority().siteCleared());assertEquals(0,reads);
        border=true;var old=survey(0,Optional.empty());assertFalse(old.advance(256,31001).orElseThrow().authority().siteCleared());assertEquals(0,reads);
        var wrong=survey(0,Optional.empty());primary=false;assertThrows(IllegalStateException.class,()->wrong.advance(128,1001));assertEquals(0,reads);
    }
    @Test void changedSecondPassEvidenceCannotBecomeSafeAir(){
        var survey=survey(0,Optional.empty());assertTrue(survey.advance(256,1001).isEmpty());changed.put(origin,Material.COBBLESTONE);
        assertFalse(survey.advance(256,1002).orElseThrow().authority().siteCleared());
    }
    @Test void mirroredJournalOrdinalStillMeansOriginalAuthoredBlockBeforeTransform(){
        var mirror=new Transform(Rotation.NONE,true);var d=new ConstructionDraft(UUID.randomUUID(),draft.settlement(),draft.world(),origin,mirror,plan,1000);
        var nativePlan=new ConstructionTemplate(plan.id(),plan.revision(),plan.name(),plan.attribution(),plan.purpose(),plan.size(),plan.stages(),
                List.of(new Anchor(Role.ENTRANCE,new Position(1,0,0),Direction.NORTH),new Anchor(Role.ROAD,new Position(1,-1,-1),Direction.NORTH)));
        var at=origin.plus(new Position(1,0,0));changed.put(at,Material.COBBLESTONE);changed.put(new Position(1,63,-1),Material.DIRT_PATH);
        var intent=new ConstructionBlockJournal.Intent(UUID.randomUUID(),d.id(),0,at,"minecraft:air","minecraft:cobblestone",ConstructionBlockJournal.State.PREPARED);
        var resume=new ConstructionWorkQueue.Work(d,ConstructionWorkQueue.State.RESUME,0,Optional.of(intent),"Saved mirrored intent");
        assertTrue(finish(new PaperConstructionWorkSurvey(resume,nativePlan,1000,id->world,()->primary)).authority().siteCleared());
        var acknowledged=new ConstructionWorkQueue.Work(d,ConstructionWorkQueue.State.READY,1,Optional.empty(),"Mirrored first block acknowledged");
        assertTrue(finish(new PaperConstructionWorkSurvey(acknowledged,nativePlan,1000,id->world,()->primary)).authority().siteCleared());
    }
    @Test void newPathCanStartOnSafeGroundWithoutPretendingItsEndpointsAreExistingRoads(){
        var path=new ConstructionTemplate(plan.id(),plan.revision(),plan.name(),plan.attribution(),Purpose.PATH,plan.size(),plan.stages(),plan.anchors());
        var d=new ConstructionDraft(UUID.randomUUID(),draft.settlement(),draft.world(),origin,Transform.identity(),path,1000);
        changed.put(new Position(0,63,-1),Material.STONE);
        var ready=new ConstructionWorkQueue.Work(d,ConstructionWorkQueue.State.READY,0,Optional.empty(),"Paid new path");
        assertTrue(finish(new PaperConstructionWorkSurvey(ready,path,1000,id->world,()->primary)).authority().siteCleared());
        assertFalse(finish(survey(0,Optional.empty())).authority().siteCleared(),"Buildings still require an actual entrance path");
    }
}
