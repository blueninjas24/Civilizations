package com.blueninjas24.civilizations.application;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/** SQL owner waits; a bounded native callback only tries, never waits for SQL. No Paper/SQLite calls here. */
public final class NativeConstructionGate {
    public record Permit(UUID owner,long generation,UUID operation,UUID draft,UUID world,long checkedMillis){}
    private final UUID identity=UUID.randomUUID();private final ReentrantReadWriteLock lock=new ReentrantReadWriteLock();
    private final AtomicBoolean stopped=new AtomicBoolean();private long generation;

    /** All accepted owner tasks, including reads/failed tasks, conservatively invalidate earlier authority. */
    public <T>T ownerTask(Callable<T> action)throws Exception{
        Objects.requireNonNull(action);
        if(nativeStepHeldByCurrentThread())throw new IllegalStateException("Native callback cannot enter the SQL owner");
        lock.writeLock().lock();
        try{generation=Math.incrementExact(generation);return action.call();}finally{lock.writeLock().unlock();}
    }
    public Permit capture(UUID operation,UUID draft,UUID world,long checkedMillis){
        if(!lock.isWriteLockedByCurrentThread())throw new IllegalStateException("Native permit must be captured inside its SQL owner task");
        if(checkedMillis<0)throw new IllegalArgumentException("Negative native authority time");
        return new Permit(identity,generation,Objects.requireNonNull(operation),Objects.requireNonNull(draft),Objects.requireNonNull(world),checkedMillis);
    }
    /** Callback must be one bounded main-thread native step, with no blocking/future/SQL calls. */
    public <T>Optional<T> tryNative(Permit permit,long now,Supplier<T> action){
        Objects.requireNonNull(permit);Objects.requireNonNull(action);
        if(stopped.get()||!identity.equals(permit.owner())||now<permit.checkedMillis()||now-permit.checkedMillis()>5000
                ||lock.isWriteLockedByCurrentThread()||!lock.readLock().tryLock())return Optional.empty();
        try{
            if(stopped.get()||generation!=permit.generation())return Optional.empty();
            return Optional.of(Objects.requireNonNull(action.get(),"Native step outcome required"));
        }finally{lock.readLock().unlock();}
    }
    boolean nativeStepHeldByCurrentThread(){return lock.getReadHoldCount()>0;}
    /** Immediate shutdown refusal; previously accepted SQL tasks may still drain normally. */
    public void stop(){stopped.set(true);}
}
