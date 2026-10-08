package com.blueninjas24.civilizations.settlement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bounded pure recognition of populated bed clusters; candidates are not permanent settlement IDs. */
public final class VillageRecognition {
    private VillageRecognition() {}
    public record Policy(int connectionRadius, int residentRadius, int minimumBeds, int minimumVillagers,
                         int maximumBeds, int maximumVillagers) {
        public Policy {
            if (connectionRadius < 1 || connectionRadius > 512 || residentRadius < 1 || residentRadius > 512
                    || minimumBeds < 1 || minimumVillagers < 1 || maximumBeds < minimumBeds
                    || maximumVillagers < minimumVillagers || maximumBeds > 512 || maximumVillagers > 1024)
                throw new IllegalArgumentException("Invalid recognition bounds");
        }
        public static Policy prototype() { return new Policy(32, 32, 2, 2, 512, 256); }
    }
    public record Candidate(List<VillageObservation.Position> beds, List<VillageObservation.Person> villagers,
                            boolean complete) {
        public Candidate { beds = List.copyOf(beds); villagers = List.copyOf(villagers); }
    }
    public record Result(List<Candidate> candidates, int unassignedVillagers, int rejectedClusters) {
        public Result { candidates = List.copyOf(candidates); }
    }

    public static Result recognize(VillageObservation observation, Policy policy) {
        Objects.requireNonNull(observation); Objects.requireNonNull(policy);
        if (observation.beds().size() > policy.maximumBeds() || observation.villagers().size() > policy.maximumVillagers())
            throw new IllegalArgumentException("Observation exceeds recognition budget; partition/rescan explicitly");
        int size = observation.beds().size();
        int[] cluster = new int[size];
        java.util.Arrays.fill(cluster, -1);
        var bedGroups = new ArrayList<List<VillageObservation.Position>>();
        double connectionSquared = (double) policy.connectionRadius() * policy.connectionRadius();
        for (int root = 0; root < size; root++) {
            if (cluster[root] != -1) continue;
            int group = bedGroups.size();
            var members = new ArrayList<VillageObservation.Position>();
            var pending = new ArrayDeque<Integer>();
            cluster[root] = group; pending.add(root);
            while (!pending.isEmpty()) {
                int index = pending.removeFirst();
                var bed = observation.beds().get(index);
                members.add(bed);
                for (int other = 0; other < size; other++) {
                    if (cluster[other] == -1 && bed.squaredDistance(observation.beds().get(other)) <= connectionSquared) {
                        cluster[other] = group; pending.add(other);
                    }
                }
            }
            members.sort(VillageObservation.Position.ORDER);
            bedGroups.add(List.copyOf(members));
        }
        var peopleGroups = new ArrayList<List<VillageObservation.Person>>();
        bedGroups.forEach(ignored -> peopleGroups.add(new ArrayList<>()));
        int unassigned = 0;
        double residentSquared = (double) policy.residentRadius() * policy.residentRadius();
        for (var person : observation.villagers()) {
            int closest = -1;
            double distance = Double.POSITIVE_INFINITY;
            for (int index = 0; index < size; index++) {
                double candidate = person.position().squaredDistance(observation.beds().get(index));
                // Equal-distance ties select the first canonical bed, independent of scan order.
                if (candidate <= residentSquared && candidate < distance) { closest = index; distance = candidate; }
            }
            if (closest == -1) unassigned++;
            else peopleGroups.get(cluster[closest]).add(person);
        }
        var candidates = new ArrayList<Candidate>();
        int rejected = 0;
        for (int group = 0; group < bedGroups.size(); group++) {
            if (bedGroups.get(group).size() < policy.minimumBeds() || peopleGroups.get(group).size() < policy.minimumVillagers()) {
                rejected++;
                continue;
            }
            candidates.add(new Candidate(bedGroups.get(group), peopleGroups.get(group), observation.complete()));
        }
        return new Result(candidates, unassigned, rejected);
    }
}
