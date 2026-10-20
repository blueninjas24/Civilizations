package com.blueninjas24.civilizations.construction;

import com.blueninjas24.civilizations.construction.ConstructionLaborPolicy.*;
import com.blueninjas24.civilizations.population.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionLaborPolicyTest {
    private final UUID town=new UUID(1,1),business=new UUID(1,2);
    private Worker worker(int id,long age,Resident.Status status,UUID employer,int remaining) {
        var resident=new Resident(new UUID(2,id),"Worker",town,null,age,status);
        return new Worker(resident,new WorkProfile(resident.id(),Map.of()),employer,remaining);
    }
    @Test void villageProposalsUseOnlyQualifiedUnemployedAdultsAndNeverAlterJobs() {
        var policy=ConstructionLaborPolicy.prototype();var workers=List.of(worker(1,300,Resident.Status.ALIVE,null,100),worker(2,300,Resident.Status.ALIVE,null,100),
                worker(3,100,Resident.Status.ALIVE,null,100),worker(4,300,Resident.Status.ALIVE,business,100),worker(5,300,Resident.Status.DECEASED,null,100));
        var plan=policy.propose(town,null,new JobRequirements(216,Map.of()),workers,42,1,true);
        assertEquals(2,plan.allocations().size());assertEquals(200,plan.proposedWork());
        assertTrue(plan.allocations().stream().allMatch(a->Set.of(new UUID(2,1),new UUID(2,2)).contains(a.resident())&&a.remainingEffort()==0));
        assertEquals(business,workers.get(3).employer());
    }
    @Test void businessEffortIsConservedAndSelectionIsDeterministicRegardlessOfInputOrder() {
        var policy=ConstructionLaborPolicy.prototype();var workers=new ArrayList<>(List.of(worker(1,300,Resident.Status.ALIVE,business,100),worker(2,300,Resident.Status.ALIVE,business,100)));
        var plan=policy.propose(town,business,new JobRequirements(216,Map.of()),workers,42,1,true);
        assertEquals(1,plan.allocations().size());assertEquals(25,plan.proposedWork());assertEquals(75,plan.allocations().getFirst().remainingEffort());
        Collections.reverse(workers);assertEquals(plan,policy.propose(town,business,new JobRequirements(216,Map.of()),workers,42,1,true));
        assertEquals(100,plan.allocations().getFirst().constructionEffort()+plan.allocations().getFirst().remainingEffort());
    }
    @Test void unavailableWorldSpentEffortAndDuplicatePeopleCannotProduceExtraWork() {
        var policy=ConstructionLaborPolicy.prototype();var worker=worker(1,300,Resident.Status.ALIVE,business,10);var qualifications=new JobRequirements(216,Map.of());
        assertEquals(10,policy.propose(town,business,qualifications,List.of(worker),42,1,true).proposedWork());
        assertEquals(0,policy.propose(town,business,qualifications,List.of(worker),42,1,false).proposedWork());
        assertEquals(0,policy.propose(town,business,qualifications,List.of(worker(2,300,Resident.Status.ALIVE,business,0)),42,1,true).proposedWork());
        assertThrows(IllegalArgumentException.class,()->policy.propose(town,business,qualifications,List.of(worker,worker),42,1,true));
    }
}
