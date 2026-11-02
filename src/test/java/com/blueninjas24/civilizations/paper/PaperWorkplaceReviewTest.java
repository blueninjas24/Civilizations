package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import com.blueninjas24.civilizations.database.BuildingRegistry;
import com.blueninjas24.civilizations.industry.*;
import com.blueninjas24.civilizations.population.JobRequirements;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperWorkplaceReviewTest {
    final UUID worldId=UUID.randomUUID(),town=UUID.randomUUID(),business=UUID.randomUUID(),person=UUID.randomUUID();
    UUID currentWorld=worldId;boolean permission=true,online=true;long time=1000;int reads,lookups,inspections;
    BuildingLayout.Bounds bounds=new BuildingLayout.Bounds(new Position(0,0,0),new Position(4,3,4));
    final List<String> messages=new ArrayList<>();CompletableFuture<BusinessFounding> pending;
    final World world=(World)Proxy.newProxyInstance(World.class.getClassLoader(),new Class<?>[]{World.class},(p,m,a)->{if(m.getName().equals("getUID"))return currentWorld;throw new AssertionError(m.getName());});
    Player player(UUID actor){return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->switch(m.getName()){
        case "getUniqueId"->actor;case "getWorld"->world;case "getLocation"->new Location(world,0,1,2);case "hasPermission"->permission;
        case "sendMessage"->{messages.add((String)a[0]);yield null;}default->throw new AssertionError(m.getName());});}
    BusinessFounding founding(){var site=new BusinessSite(UUID.randomUUID(),town,worldId,3,1,2,0,"Saved test marker");var firm=new Business(business,town,"Saved business",person,person,"blacksmith",site.id(),2,0);var requirement=new JobRequirements(0,Map.of());return new BusinessFounding(firm,site,requirement,requirement,0,"Explicit fixture");}
    PaperWorkplaceReview review(){
        return new PaperWorkplaceReview(new PaperWorkplaceReview.Owner(){
            public CompletableFuture<BusinessFounding> target(UUID id,UUID world,boolean permitted){assertEquals(business,id);assertEquals(worldId,world);assertTrue(permitted);lookups++;return pending==null?CompletableFuture.completedFuture(founding()):pending;}
            public CompletableFuture<WorkplaceInspection.Assessment> inspect(UUID id,BuildingLayout layout,WorkplaceInspection.Evidence evidence){inspections++;return CompletableFuture.completedFuture(WorkplaceInspection.inspect(layout,evidence,time,5000,WorkplaceInspection.Policy.prototype()));}
        },p->bounds,id->online?player(id):null,(layout,marker,now)->(budget,at)->{
            reads+=budget;var cells=new HashMap<Position,Cell>();for(var p:DwellingInspection.requiredCells(layout))cells.put(p,Cell.of(p.y()==0||p.y()==3?Type.SUPPORT:Type.PASSABLE));cells.put(marker,Cell.of(Type.SUPPORT));
            return Optional.of(new WorkplaceInspection.Evidence(new Observation(worldId,layout.fingerprint(),now,true,cells),marker,Optional.of(WorkplaceObservation.Kind.SMITHING_TABLE),true));
        },()->time,Logger.getAnonymousLogger());
    }
    void start(PaperWorkplaceReview review,UUID actor){review.onCommand(player(actor),null,"civ",new String[]{"building","workplacecheck",business.toString()});}
    @Test void operatorPreviewIsBoundedAndNeverClaimsDonationOrCapacity(){
        var review=review();for(int i=0;i<5;i++)start(review,new UUID(0,i));assertEquals(4,lookups);assertTrue(messages.getLast().contains("busy"));
        review.run();assertEquals(128,reads);assertEquals(1,inspections);assertTrue(messages.stream().anyMatch(s->s.contains("check passed")));assertTrue(messages.stream().anyMatch(s->s.contains("No building registered")));
        review.run();assertEquals(256,reads);assertEquals(2,inspections);review.close();review.run();assertEquals(256,reads);
    }
    @Test void changedAuthorityWorldSelectionAndExpiryCancelBeforeNativeReads(){
        UUID actor=UUID.randomUUID();var review=review();start(review,actor);permission=false;review.run();assertEquals(0,reads);permission=true;
        start(review,actor);currentWorld=UUID.randomUUID();review.run();assertEquals(0,reads);currentWorld=worldId;
        start(review,actor);bounds=new BuildingLayout.Bounds(new Position(0,0,0),new Position(5,3,4));review.run();assertEquals(0,reads);
        start(review,actor);time+=30000;review.run();assertEquals(0,reads);assertTrue(messages.getLast().contains("expired"));review.close();
    }
    @Test void unresolvedOrFailedOwnerLookupCannotReadWorldAndDisconnectDiscardsRequest(){
        UUID actor=UUID.randomUUID();pending=new CompletableFuture<>();var review=review();start(review,actor);review.run();assertEquals(0,reads);
        pending.completeExceptionally(new BuildingRegistry.Declined("Unknown business"));review.run();assertEquals(0,reads);assertTrue(messages.getLast().contains("Unknown business"));
        pending=null;start(review,actor);online=false;review.run();assertEquals(0,reads);online=true;permission=false;start(review,actor);assertEquals(2,lookups);review.close();
    }
}
