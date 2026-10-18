package com.blueninjas24.civilizations.industry;

import com.blueninjas24.civilizations.economy.Commodity;
import java.util.*;

/** Editable prototype supplies and budgets; accepted courses retain their terms. */
public record StudyPolicy(boolean enabled, long gainPerPeriod, long maximumExperience,
                          int studentsPerPeriod, int activeCoursesPerVillage, int foodReservePeriods,
                          long materialReserveUnits, Map<String, Course> skills) {
    public record Course(long targetExperience, Map<Commodity, Long> materials) {
        public Course {
            materials=Collections.unmodifiableMap(new TreeMap<>(materials));
            if(targetExperience<1||targetExperience>1_000_000_000L||materials.isEmpty()||materials.size()>16)
                throw new IllegalArgumentException("Invalid study course");
            materials.forEach((commodity,units)->{
                if(commodity.equals(Commodity.FOOD)||units<1||units>1_000_000L)
                    throw new IllegalArgumentException("Study requires bounded nonfood practice supplies");
            });
        }
    }
    public StudyPolicy {
        skills=Collections.unmodifiableMap(new TreeMap<>(skills));
        if(gainPerPeriod<1||gainPerPeriod>1000||maximumExperience<1||maximumExperience>1_000_000_000L
                ||studentsPerPeriod<1||studentsPerPeriod>32||activeCoursesPerVillage<studentsPerPeriod
                ||activeCoursesPerVillage>32||foodReservePeriods<0||foodReservePeriods>100
                ||materialReserveUnits<0||materialReserveUnits>1_000_000_000L||skills.size()>16)
            throw new IllegalArgumentException("Invalid study limits");
        skills.forEach((skill,course)->{
            if(!skill.matches("[a-z][a-z0-9_-]{0,63}")||course.targetExperience()>maximumExperience)
                throw new IllegalArgumentException("Invalid study skill or target");
        });
    }
    public static StudyPolicy prototype(){return new StudyPolicy(true,1,100,2,8,1,2,
            Map.of("forestry",new Course(10,Map.of(Commodity.TIMBER,2L,Commodity.STONE,1L)),
                   "mining",new Course(10,Map.of(Commodity.STONE,1L,Commodity.TIMBER,1L)),
                   "smithing",new Course(10,Map.of(Commodity.STONE,1L,Commodity.TIMBER,1L)),
                   "commerce",new Course(10,Map.of(Commodity.TIMBER,1L)),
                   "fishing",new Course(10,Map.of(Commodity.TIMBER,1L)),
                   "crafting",new Course(10,Map.of(Commodity.TIMBER,1L)),
                   "baking",new Course(10,Map.of(Commodity.TIMBER,1L))));}
    public static StudyPolicy disabled(){return new StudyPolicy(false,1,100,2,8,1,2,Map.of());}
}
