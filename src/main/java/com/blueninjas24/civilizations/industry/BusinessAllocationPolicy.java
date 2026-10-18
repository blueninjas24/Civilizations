package com.blueninjas24.civilizations.industry;

import java.util.*;

/** Editable priorities over viable choices; ranking never grants skills, workers, goods or funds. */
public record BusinessAllocationPolicy(boolean enabled,long reviewMillis,boolean urgentFirst,
                                       Set<String> urgentIndustries,long maximumDemandValue) {
    public BusinessAllocationPolicy {
        urgentIndustries=Collections.unmodifiableSet(new TreeSet<>(urgentIndustries));
        if(reviewMillis<1000||reviewMillis>3_600_000
                ||maximumDemandValue<1||maximumDemandValue>1_000_000_000L||urgentIndustries.size()>32
                ||urgentIndustries.stream().anyMatch(k->!k.matches("[a-z][a-z0-9_]{0,63}")))
            throw new IllegalArgumentException("Invalid shared business allocation policy");
    }
    public record Candidate(UUID business,boolean urgent,long fundedDemandValue) {
        public Candidate {Objects.requireNonNull(business);if(fundedDemandValue<0)throw new IllegalArgumentException("Negative funded demand");}
    }
    public List<Candidate> rank(Collection<Candidate> candidates,String lastBusiness) {
        var result=new ArrayList<>(candidates);Objects.requireNonNull(lastBusiness);
        if(result.size()>32||result.stream().map(Candidate::business).distinct().count()!=result.size())
            throw new IllegalArgumentException("Complete distinct bounded business comparison required");
        result.sort(Comparator.<Candidate>comparingInt(c->urgentFirst&&c.urgent()?0:1)
                .thenComparing(Comparator.comparingLong((Candidate c)->Math.min(maximumDemandValue,c.fundedDemandValue())).reversed())
                .thenComparingInt(c->c.business().toString().compareTo(lastBusiness)>0?0:1)
                .thenComparing(c->c.business().toString()));
        return List.copyOf(result);
    }
    public static BusinessAllocationPolicy prototype(){return new BusinessAllocationPolicy(true,5000,true,Set.of("bakery","fishing"),1_000_000);}
    public static BusinessAllocationPolicy disabled(){var p=prototype();return new BusinessAllocationPolicy(false,p.reviewMillis,p.urgentFirst,p.urgentIndustries,p.maximumDemandValue);}
}
