package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Immutable caller-verified bank frame; the original habitat is validated separately by its owner. */
public record ShorelineEvidence(UUID world,ShorelineScan.Position origin,long capturedMillis,List<ShorelineScan.Assessment> assessments,String supportFingerprint) {
    public ShorelineEvidence(UUID world,ShorelineScan.Position origin,long capturedMillis,List<ShorelineScan.Assessment> assessments){this(world,origin,capturedMillis,assessments,"");}
    public ShorelineEvidence {
        Objects.requireNonNull(world);Objects.requireNonNull(origin);Objects.requireNonNull(supportFingerprint);
        if(!supportFingerprint.isEmpty()&&!supportFingerprint.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid support fingerprint");
        if(capturedMillis<0||assessments.isEmpty()||assessments.size()>4096)throw new IllegalArgumentException("Invalid bank frame");
        var ordered=new TreeMap<ShorelineScan.Position,ShorelineScan.State>(Comparator.comparingInt(ShorelineScan.Position::x).thenComparingInt(ShorelineScan.Position::y).thenComparingInt(ShorelineScan.Position::z));
        for(var a:assessments){Objects.requireNonNull(a);Objects.requireNonNull(a.position());Objects.requireNonNull(a.state());if(ordered.put(a.position(),a.state())!=null)throw new IllegalArgumentException("Duplicate bank position");}
        if(!ordered.containsKey(origin))throw new IllegalArgumentException("Bank frame omits its original standing point");
        assessments=ordered.entrySet().stream().map(e->new ShorelineScan.Assessment(e.getKey(),e.getValue())).toList();
    }
}
