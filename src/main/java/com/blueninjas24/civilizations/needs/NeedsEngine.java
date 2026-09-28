package com.blueninjas24.civilizations.needs;

import com.blueninjas24.civilizations.settlement.Settlement;

public class NeedsEngine {

    public enum HousingStatus {
        ADEQUATE,
        HOUSING_SHORTAGE
    }

    public HousingStatus evaluateHousing(Settlement settlement) {
        if (settlement.getPopulation() >= settlement.getBeds()) {
            return HousingStatus.HOUSING_SHORTAGE;
        }

        return HousingStatus.ADEQUATE;
    }
}