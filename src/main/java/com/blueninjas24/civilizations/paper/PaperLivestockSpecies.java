package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.industry.LivestockHerd.Species;
import org.bukkit.entity.EntityType;
import java.util.Optional;

final class PaperLivestockSpecies {
    private PaperLivestockSpecies(){}
    static Optional<Species> from(EntityType type){return switch(type){case COW->Optional.of(Species.COW);case CHICKEN->Optional.of(Species.CHICKEN);case SHEEP->Optional.of(Species.SHEEP);default->Optional.empty();};}
    static EntityType type(Species species){return switch(species){case COW->EntityType.COW;case CHICKEN->EntityType.CHICKEN;case SHEEP->EntityType.SHEEP;};}
}
