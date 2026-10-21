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
            assertTrue(session.housingSummary(building,player,false,world).get(5,TimeUnit.SECONDS).stream().anyMatch(s->s.contains("no saved household assigned")));
            assertThrows(ExecutionException.class,()->session.housingSummary(building,UUID.randomUUID(),false,world).get(5,TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,()->session.housingSummary(building,player,false,UUID.randomUUID()).get(5,TimeUnit.SECONDS));
            var apartmentBounds=new Bounds(new Position(200,64,0),new Position(208,67,4));
            var draft=new ApartmentDeclaration.Draft(UUID.randomUUID(),village,world,apartmentBounds,apartmentBounds,
                    new Position(204,65,0),List.of(),List.of())
                    .shared(new Bounds(new Position(200,64,0),new Position(208,67,0)))
                    .unit("west",new Position(201,65,1),new Bounds(new Position(200,64,1),new Position(203,67,4)))
                    .unit("east",new Position(206,65,1),new Bounds(new Position(205,64,1),new Position(208,67,4)));
            var apartmentCells=new HashMap<Position,Cell>();
            for(int x=200;x<=208;x++)for(int y=64;y<=67;y++)for(int z=0;z<=4;z++)
                apartmentCells.put(new Position(x,y,z),Cell.of(y==64||y==67?Type.SUPPORT:Type.PASSABLE));
            for(int x:List.of(201,206)){
                apartmentCells.put(new Position(x,65,2),Cell.bed(false,"minecraft:white_bed",Direction.SOUTH));
                apartmentCells.put(new Position(x,65,3),Cell.bed(true,"minecraft:white_bed",Direction.SOUTH));
            }
            var apartmentFrame=new Observation(world,HomeDeclaration.regionHash(world,apartmentBounds),1000,true,apartmentCells);
            assertThrows(ExecutionException.class,()->session.previewApartmentDonation(player,world,draft,apartmentFrame,Policy.prototype(),false).get(5,TimeUnit.SECONDS));
            var apartmentPreview=session.previewApartmentDonation(player,world,draft,apartmentFrame,Policy.prototype(),true).get(5,TimeUnit.SECONDS);
            assertTrue(apartmentPreview.review().passes());assertEquals(2,apartmentPreview.review().layout().units().size());
            assertThrows(ExecutionException.class,()->session.buildingInspectionLayout(draft.building(),player,false,world).get(5,TimeUnit.SECONDS));
            session.confirmBuildingConsent(player,world,apartmentPreview.consent().token(),true,true,false,false).get(5,TimeUnit.SECONDS);
            assertEquals(apartmentPreview.review().layout(),session.buildingInspectionLayout(draft.building(),player,false,world).get(5,TimeUnit.SECONDS).layout());
            var housingPlan=session.housingCandidates(village,world,List.of(),HousingProjectPlanner.prototype(),Policy.prototype()).get(5,TimeUnit.SECONDS);
            assertFalse(housingPlan.vacanciesVerified()); // Donation alone is not an inspected vacancy.
            assertTrue(housingPlan.describe().getFirst().contains("no projects"));
            assertNull(session.status().failure());
        }
    }
}
