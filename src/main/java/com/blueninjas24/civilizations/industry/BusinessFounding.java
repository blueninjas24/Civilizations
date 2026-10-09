package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.population.JobRequirements;
import java.util.Objects;

/** Explicit, immutable founding terms supplied by a future opportunity planner. No guessed funding. */
public record BusinessFounding(Business business, BusinessSite site, JobRequirements founderRequirements,
                              JobRequirements workerRequirements, long startupHundredths, String reason) {
    public BusinessFounding {
        Objects.requireNonNull(business); Objects.requireNonNull(site);
        Objects.requireNonNull(founderRequirements); Objects.requireNonNull(workerRequirements);
        Business.text(reason, 2048);
        if (!business.siteId().equals(site.id()) || !business.settlementId().equals(site.settlementId())
                || !business.founderId().equals(business.ownerId()) || startupHundredths < 0)
            throw new IllegalArgumentException("Invalid founding identity, site or capital");
        for (var requirements : java.util.List.of(founderRequirements, workerRequirements))
            requirements.minimumExperience().keySet().forEach(skill -> Business.text(skill, 64));
    }
}
