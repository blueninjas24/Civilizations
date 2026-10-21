package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.ConstructionPreview.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.database.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PaperAutomaticHomePlanningTest {
    private final UUID world=new UUID(0,1),village=new UUID(0,2),family=new UUID(0,3),draft=new UUID(0,4);
    private final HousingPlanningJobs.Job job=new HousingPlanningJobs.Job(draft,family,village,world,new Position(0,64,0));
    private long now=1000;private int pages,advances,preparations,largestBudget;private boolean complete=true;
    private final List<String> diagnostics=new ArrayList<>();
    private CompletableFuture<HousingPlanningJobs.Page> page=new CompletableFuture<>();
    private CompletableFuture<HousingProjectPreparation.Receipt> saving=new CompletableFuture<>();
    private PaperAutomaticHomePlanning runtime(boolean enabled){
        return new PaperAutomaticHomePlanning(new PaperAutomaticHomePlanning.Owner(){
            public CompletableFuture<HousingPlanningJobs.Page> page(UUID after){pages++;return page;}
            public CompletableFuture<HousingProjectPreparation.Receipt> prepare(HousingPlanningJobs.Job j,PaperHomeSiteSurvey.Frame frame,long time){preparations++;assertEquals(job,j);assertEquals(now,time);return saving;}
        },new AutomaticHomePlanningSettings(enabled,128,30,1,12),(j,start)->(budget,time)->{
            advances++;largestBudget=Math.max(largestBudget,budget);
            return Optional.of(new PaperHomeSiteSurvey.Frame(new Bounds(new Position(-12,61,-12),new Position(12,66,12)),
                    new ConstructionSitePlanner.Survey(world,start,complete,complete,Map.of(),List.of(),List.of()),Map.of()));
        },()->now,diagnostics::add,Logger.getLogger("AutomaticHomePlanningTest"));
    }
    private HousingProjectPreparation.Receipt receipt()throws Exception{
        var template=ConstructionTemplateConfiguration.read(getClass().getResourceAsStream("/construction/starter_home.yml"));
        var plan=new ConstructionDraft(draft,village,world,job.need(),Transform.identity(),template,now);
        return new HousingProjectPreparation.Receipt(plan,new HousingProjects.Binding(new HousingProjects.Proposal(draft,family,now),village,world,template.fingerprint(),2,0,null,null),
                new ConstructionSiteReservations.Reservation(draft,world,ConstructionPreview.footprint(template,Transform.identity(),job.need()),now,now,null),Optional.empty());
    }
    @Test void pendingOwnerReadsAndSavesCannotRepeatSurveyOrOverrunOneGlobalBudget()throws Exception{
        var r=runtime(true);r.run();r.run();assertEquals(1,pages);assertEquals(0,advances);
        page.complete(new HousingPlanningJobs.Page(List.of(job),null,1,0,0));r.run();assertEquals(1,advances);assertEquals(128,largestBudget);assertEquals(1,preparations);
        r.run();r.run();assertEquals(1,advances);assertEquals(1,preparations);
        saving.complete(receipt());r.run();assertTrue(diagnostics.getLast().contains("retained unused project"));assertEquals(1,pages);
        now+=30_001;page=new CompletableFuture<>();r.run();assertEquals(2,pages);r.close();r.run();assertEquals(2,pages);
    }
    @Test void incompleteNativeEvidenceAndDisabledSettingsNeverSubmitAProject(){
        var disabled=runtime(false);disabled.run();assertEquals(0,pages);
        var r=runtime(true);complete=false;r.run();page.complete(new HousingPlanningJobs.Page(List.of(job),null,1,0,0));r.run();
        assertEquals(0,preparations);assertTrue(diagnostics.getLast().contains("incomplete"));assertEquals(1,advances);
    }
    @Test void declinedSavedNeedPausesForReviewAndClosingNeverCancelsAcceptedOwnerWork(){
        var r=runtime(true);r.run();page.complete(new HousingPlanningJobs.Page(List.of(job),null,1,0,0));
        saving=CompletableFuture.failedFuture(new BuildingRegistry.Declined("Family already housed"));r.run();r.run();
        assertTrue(diagnostics.getLast().contains("Family already housed"));r.run();assertEquals(1,pages);
        now+=30_001;page=new CompletableFuture<>();saving=new CompletableFuture<>();r.run();page.complete(new HousingPlanningJobs.Page(List.of(job),null,1,0,0));r.run();
        r.close();assertFalse(saving.isCancelled());assertEquals(2,preparations);
    }
}
