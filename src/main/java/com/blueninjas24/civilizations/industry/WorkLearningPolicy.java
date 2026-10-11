package com.blueninjas24.civilizations.industry;

import java.util.Map;

/** Gradual experience from actual productive work; not a salary, course or ownership waiver. */
public record WorkLearningPolicy(boolean enabled,long experiencePerProductivePeriod,long maximumExperience,
                                 int maximumWorkersPerReview,Map<String,String> industrySkills) {
    public WorkLearningPolicy {
        industrySkills=Map.copyOf(industrySkills);
        if(experiencePerProductivePeriod<1||experiencePerProductivePeriod>1000||maximumExperience<1||maximumExperience>1_000_000_000L
                ||maximumWorkersPerReview<1||maximumWorkersPerReview>1000||industrySkills.size()>32)
            throw new IllegalArgumentException("Invalid work-learning limits");
        for(var e:industrySkills.entrySet())for(var key:new String[]{e.getKey(),e.getValue()})
            if(!key.matches("[a-z][a-z0-9_-]{0,63}"))throw new IllegalArgumentException("Invalid learning industry/skill key");
    }
    public long nextExperience(long current) {
        if(current<0)throw new IllegalArgumentException("Negative experience");
        return current>=maximumExperience?current:current+Math.min(experiencePerProductivePeriod,maximumExperience-current);
    }
    public static WorkLearningPolicy prototype(){return new WorkLearningPolicy(true,1,100,64,Map.of("mining","mining","blacksmith","smithing","farming","farming","warehouse","commerce"));}
    public static WorkLearningPolicy disabled(){return new WorkLearningPolicy(false,1,100,64,Map.of());}
}
