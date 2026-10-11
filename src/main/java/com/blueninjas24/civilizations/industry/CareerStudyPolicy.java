package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Real-vacancy training budgets; accepted courses still use the study supply policy. */
public record CareerStudyPolicy(boolean enabled,int candidatesPerReview,int enrollmentsPerReview,Set<String> industries) {
    public CareerStudyPolicy {
        industries=Collections.unmodifiableSet(new TreeSet<>(industries));
        if(candidatesPerReview<1||candidatesPerReview>512||enrollmentsPerReview<1||enrollmentsPerReview>16
                ||industries.size()>32||industries.stream().anyMatch(k->!k.matches("[a-z][a-z0-9_]{0,63}")||k.equals("farming")))
            throw new IllegalArgumentException("Invalid vacancy career study policy");
    }
    public static CareerStudyPolicy prototype(){return new CareerStudyPolicy(true,64,1,Set.of("mining","blacksmith","warehouse"));}
    public static CareerStudyPolicy disabled(){return new CareerStudyPolicy(false,64,1,Set.of());}
}
