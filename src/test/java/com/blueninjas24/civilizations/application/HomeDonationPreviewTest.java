package com.blueninjas24.civilizations.application;

import com.blueninjas24.civilizations.construction.*;
import com.blueninjas24.civilizations.construction.BuildingLayout.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.*;
import com.blueninjas24.civilizations.construction.DwellingInspection.*;
import com.blueninjas24.civilizations.database.*;
import com.blueninjas24.civilizations.settlement.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HomeDonationPreviewTest {
    @TempDir Path directory;
    @Test void inspectedDeclarationPreviewsOnlyThenNeedsExplicitOwnershipBeforePersistence()throws Exception {
        UUID world=new UUID(0,1),player=new UUID(0,2),building=new UUID(0,3);
        var settings=PopulationSession.Settings.prototype();
        try(var session=new PopulationSession(directory.toFile(),Logger.getLogger("HomePreviewTest"),settings,()->1000L)) {
            session.start(false,Set.of(world)).get(5,TimeUnit.SECONDS);
            // A test-owned observed village, not a real server database.
            var bed=new VillageObservation.Position(0,64,0);var secondBed=new VillageObservation.Position(2,64,0);
            var villager=new VillageObservation.Person(UUID.randomUUID(),bed,true,"Fixture adult");
            var frame=new VillageObservation(world,"fixture",1000,true,List.of(bed,secondBed),List.of(villager,
                    new VillageObservation.Person(UUID.randomUUID(),secondBed,true,"Second fixture adult")));
            session.observe(frame).get(5,TimeUnit.SECONDS);
            UUID village=session.settlementNames().keySet().iterator().next();
            var bounds=new Bounds(new Position(100,64,0),new Position(104,67,4));var entrance=new Position(100,65,0);
            var cells=new HashMap<Position,Cell>();
            for(int x=100;x<=104;x++)for(int y=64;y<=67;y++)for(int z=0;z<=4;z++)cells.put(new Position(x,y,z),Cell.of(y==64||y==67?Type.SUPPORT:Type.PASSABLE));
            cells.put(new Position(101,65,2),Cell.bed(false,"minecraft:white_bed",Direction.EAST));
            cells.put(new Position(102,65,2),Cell.bed(true,"minecraft:white_bed",Direction.EAST));
            var observation=new Observation(world,HomeDeclaration.regionHash(world,bounds),1000,true,cells);
            var preview=session.previewSingleHomeDonation(player,world,building,village,bounds,bounds,entrance,observation,Policy.prototype(),true).get(5,TimeUnit.SECONDS);
            assertTrue(preview.review().passes());assertEquals(1,preview.review().layout().units().size());
            assertThrows(ExecutionException.class,()->session.buildingInspectionLayout(building,player,false,world).get(5,TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,()->session.confirmBuildingConsent(player,world,preview.consent().token(),false,true,true,false).get(5,TimeUnit.SECONDS));
            var receipt=session.confirmBuildingConsent(player,world,preview.consent().token(),true,true,true,false).get(5,TimeUnit.SECONDS);
            assertEquals(BuildingRegistry.Action.DONATE,receipt.action());
            assertEquals(preview.review().layout(),session.buildingInspectionLayout(building,player,false,world).get(5,TimeUnit.SECONDS).layout());
            assertNull(session.status().failure());
        }
    }
}
