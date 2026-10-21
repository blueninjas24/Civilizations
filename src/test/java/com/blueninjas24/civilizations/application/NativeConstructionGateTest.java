package com.blueninjas24.civilizations.application;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeConstructionGateTest {
    NativeConstructionGate.Permit capture(NativeConstructionGate gate)throws Exception{
        return gate.ownerTask(()->gate.capture(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1000));
    }
    @Test void busyOwnerNeverBlocksNativeCallerAndAnyLaterOwnerTaskInvalidatesOldAuthority()throws Exception{
        var gate=new NativeConstructionGate();var permit=capture(gate);var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var callbacks=new AtomicInteger();
        try(var pool=Executors.newFixedThreadPool(2)){
            var owner=pool.submit(()->gate.ownerTask(()->{entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return true;}));
            try{
                assertTrue(entered.await(2,TimeUnit.SECONDS));
                assertTrue(pool.submit(()->gate.tryNative(permit,1001,callbacks::incrementAndGet)).get(1,TimeUnit.SECONDS).isEmpty());assertEquals(0,callbacks.get());
            }finally{release.countDown();}
            assertTrue(owner.get(2,TimeUnit.SECONDS));assertTrue(gate.tryNative(permit,1001,callbacks::incrementAndGet).isEmpty());
        }
        var fresh=capture(gate);assertEquals(1,gate.tryNative(fresh,1001,callbacks::incrementAndGet).orElseThrow());
    }
    @Test void actualNativeStepExcludesOwnerCommitUntilItsBoundedCallbackFinishes()throws Exception{
        var gate=new NativeConstructionGate();var permit=capture(gate);var nativeEntered=new CountDownLatch(1);var ownerEntered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var nativeWork=pool.submit(()->gate.tryNative(permit,1001,()->{nativeEntered.countDown();try{assertTrue(release.await(5,TimeUnit.SECONDS));}catch(InterruptedException failure){Thread.currentThread().interrupt();throw new AssertionError(failure);}return true;}));
            assertTrue(nativeEntered.await(2,TimeUnit.SECONDS));var owner=pool.submit(()->gate.ownerTask(()->{ownerEntered.countDown();return true;}));
            try{assertFalse(ownerEntered.await(100,TimeUnit.MILLISECONDS));}finally{release.countDown();}
            assertTrue(nativeWork.get(2,TimeUnit.SECONDS).orElseThrow());assertTrue(owner.get(2,TimeUnit.SECONDS));assertTrue(gate.tryNative(permit,1001,()->true).isEmpty());
        }
    }
    @Test void expiryOtherSessionStopAndCallbackFailureNeverLeaveAnExecutionLockHeld()throws Exception{
        var gate=new NativeConstructionGate();var permit=capture(gate);var other=new NativeConstructionGate();
        assertTrue(gate.tryNative(permit,999,()->true).isEmpty());assertTrue(gate.tryNative(permit,6001,()->true).isEmpty());assertTrue(other.tryNative(permit,1001,()->true).isEmpty());
        assertThrows(IllegalStateException.class,()->gate.tryNative(permit,1001,()->{throw new IllegalStateException("fixture native failure");}));
        var next=capture(gate);gate.stop();assertTrue(gate.tryNative(next,1001,()->true).isEmpty());assertEquals(1,gate.ownerTask(()->1),"Accepted SQL work may drain after stop");
    }
    @Test void firstWriteClaimIsOneTimeAndCannotBeRestoredByRefreshingOwnerAuthority()throws Exception{
        var gate=new NativeConstructionGate();UUID operation=UUID.randomUUID(),draft=UUID.randomUUID(),world=UUID.randomUUID();
        var permit=gate.ownerTask(()->{gate.rememberFresh(operation);return gate.capture(operation,draft,world,1000);});
        assertFalse(gate.claimFirstWrite(permit),"Cannot claim outside the gated native callback");
        assertTrue(gate.tryNative(permit,1001,()->gate.claimFirstWrite(permit)).orElseThrow());
        assertFalse(gate.tryNative(permit,1001,()->gate.claimFirstWrite(permit)).orElseThrow());
        var refreshed=gate.ownerTask(()->gate.capture(operation,draft,world,1000));
        assertFalse(gate.tryNative(refreshed,1001,()->gate.claimFirstWrite(refreshed)).orElseThrow(),"Renewing a lease cannot create replacement materials");
    }
}
