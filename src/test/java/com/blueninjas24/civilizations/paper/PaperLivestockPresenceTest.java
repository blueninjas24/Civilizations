package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.database.LivestockPresence.*;
import com.blueninjas24.civilizations.industry.LivestockHerd;
import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperLivestockPresenceTest {
    final UUID world=new UUID(0,1),animal=new UUID(0,2),business=new UUID(0,3);
    final Member member=new Member(animal,business,LivestockHerd.Species.COW,world,new Position(0,64,0),0);
    final Sight sight=new Sight(animal,LivestockHerd.Species.COW,world,new Position(40,64,0),1000);
    class Owner implements PaperLivestockPresence.Owner {
        boolean ready=true;int pages,records;UUID after;List<Sight> last;CompletableFuture<List<Member>> page=new CompletableFuture<>();CompletableFuture<Integer> save=new CompletableFuture<>();
        public boolean ready(){return ready;}
        public CompletableFuture<List<Member>> page(UUID cursor,int limit){assertEquals(16,limit);pages++;after=cursor;return page;}
        public CompletableFuture<Integer> record(List<Sight> sights){records++;last=sights;return save;}
    }
    PaperLivestockPresence observer(Owner owner,java.util.function.Function<Member,Optional<Sight>> capture,java.util.function.BooleanSupplier main,java.util.function.LongSupplier clock) {
        return new PaperLivestockPresence(owner,capture,main,Logger.getLogger("Presence fixture"),clock);
    }
    @Test void waitsForOwnerAndPageThenCapturesOnlyBoundedSavedIdentitiesOnMainThread() {
        var owner=new Owner();var captures=new AtomicInteger();var observer=observer(owner,m->{assertEquals(member,m);captures.incrementAndGet();return Optional.of(sight);},()->true,()->1000);
        owner.ready=false;observer.run();assertEquals(0,owner.pages);owner.ready=true;observer.run();observer.run();assertEquals(1,owner.pages);assertEquals(0,captures.get());
        owner.page.complete(List.of(member));observer.run();assertEquals(1,captures.get());assertEquals(List.of(sight),owner.last);assertEquals(1,owner.records);observer.run();assertEquals(1,owner.records);
        owner.save.complete(1);owner.page=new CompletableFuture<>();observer.run();assertEquals(animal,owner.after);assertTrue(observer.diagnostic().contains("saved: 1"));
    }
    @Test void absentLoadedBodyProducesNoNegativeEvidenceAndEndOfPageRestartsTheCursor() {
        var owner=new Owner();var observer=observer(owner,m->Optional.empty(),()->true,()->1000);
        observer.run();owner.page.complete(List.of(member));observer.run();assertEquals(0,owner.records);assertEquals(animal,owner.after);
        owner.page=CompletableFuture.completedFuture(List.of());observer.run();observer.run();assertNull(owner.after);assertEquals(0,owner.records);
    }
    @Test void failedSaveRetainsExactEvidenceAndDoesNotRecaptureOrAdvanceUntilSaved() {
        var owner=new Owner();var now=new AtomicLong(1000);var captures=new AtomicInteger();var observer=observer(owner,m->{captures.incrementAndGet();return Optional.of(sight);},()->true,now::get);
        observer.run();owner.page.complete(List.of(member));observer.run();owner.save.completeExceptionally(new IllegalStateException("fixture save failure"));observer.run();
        assertEquals(List.of(sight),owner.last);owner.save=new CompletableFuture<>();observer.run();assertEquals(1,owner.records);now.set(2000);observer.run();assertEquals(2,owner.records);assertEquals(1,captures.get());
        observer.flush();assertEquals(3,owner.records);assertEquals(List.of(sight),owner.last);
    }
    @Test void readFailureRetriesSameCursorAndMainThreadGuardPrecedesOwnerCalls() {
        var owner=new Owner();assertThrows(IllegalStateException.class,()->observer(owner,m->Optional.empty(),()->false,()->1000).run());assertEquals(0,owner.pages);
        var now=new AtomicLong(1000);var observer=observer(owner,m->Optional.empty(),()->true,now::get);observer.run();owner.page.completeExceptionally(new IllegalStateException("fixture read failure"));observer.run();assertEquals(1,owner.pages);
        owner.page=new CompletableFuture<>();now.set(2000);observer.run();assertEquals(2,owner.pages);assertNull(owner.after);
    }
    @Test void pageBudgetAndCapturedIdentityCannotBeBypassed() {
        var owner=new Owner();var observer=observer(owner,m->Optional.of(sight),()->true,()->1000);observer.run();owner.page.complete(Collections.nCopies(17,member));assertThrows(IllegalStateException.class,observer::run);assertEquals(0,owner.records);
        var other=new Owner();var wrong=new Sight(UUID.randomUUID(),sight.species(),world,sight.position(),1000);var bad=observer(other,m->Optional.of(wrong),()->true,()->1000);bad.run();other.page.complete(List.of(member));assertThrows(IllegalStateException.class,bad::run);assertEquals(0,other.records);
    }
}
