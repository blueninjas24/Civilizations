package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Vacancy response at existing live-owner firms; not a wage or new-business policy. */
public record BusinessStaffingPolicy(boolean enabled,int candidatesPerReview,int hiresPerReview,Set<String> industries) {
    public BusinessStaffingPolicy {
        industries=Collections.unmodifiableSet(new TreeSet<>(industries));
        if(candidatesPerReview<1||candidatesPerReview>512||hiresPerReview<1||hiresPerReview>16||industries.size()>32
                ||industries.stream().anyMatch(kind->!kind.matches("[a-z][a-z0-9_]{0,63}")||kind.equals("farming")))
            throw new IllegalArgumentException("Invalid existing-business staffing settings; farms use their own staffing adapter");
    }
    public static BusinessStaffingPolicy disabled(){return new BusinessStaffingPolicy(false,64,2,Set.of("mining","blacksmith","warehouse"));}
}
