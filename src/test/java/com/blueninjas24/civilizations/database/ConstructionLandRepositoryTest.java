package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.construction.ConstructionPreview.Bounds;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionLandRepositoryTest {
    @TempDir Path directory;
    @Test void emptyProtectedPlotsExcludeTheirColumnsAcrossHeightsAndWorldsStaySeparate()throws Exception {
        var fixture=new PenProtectionRepositoryTest();fixture.directory=directory;fixture.open();
        try {
            fixture.repo().save(fixture.pen(fixture.id,fixture.owner,Set.of()),true);
            var bounds=new Bounds(new Position(-1,200,-1),new Position(1,210,1));
            var repository=new ConstructionLandRepository(fixture.db.getConnection());var columns=repository.protectedColumns(fixture.world,bounds);
            assertEquals(9,columns.size());assertTrue(columns.contains(new ConstructionLandRepository.Column(0,0)));
            assertThrows(UnsupportedOperationException.class,()->columns.clear());
            assertTrue(repository.protectedColumns(UUID.randomUUID(),bounds).isEmpty());
            assertTrue(repository.protectedColumns(fixture.world,new Bounds(new Position(20,64,20),new Position(30,70,30))).isEmpty());
            assertThrows(IllegalArgumentException.class,()->repository.protectedColumns(fixture.world,new Bounds(new Position(-100,64,-100),new Position(100,70,100))));
        }finally{fixture.close();}
    }
    @Test void explicitReleaseRemovesExclusionWithoutDeletingPlotHistory()throws Exception {
        var fixture=new PenProtectionRepositoryTest();fixture.directory=directory;fixture.open();
        try {
            var pen=fixture.pen(fixture.id,fixture.owner,Set.of());fixture.repo().save(pen,true);
            var bounds=new Bounds(new Position(-2,64,-2),new Position(2,68,2));
            assertEquals(21,new ConstructionLandRepository(fixture.db.getConnection()).protectedColumns(fixture.world,bounds).size());
            new PenProtectionLifecycle(fixture.db.getConnection()).release(new PenProtectionLifecycle.Release(UUID.randomUUID(),fixture.id,fixture.owner,2000,"Explicit fixture release"),false);
            fixture.close();fixture.open();
            assertTrue(new ConstructionLandRepository(fixture.db.getConnection()).protectedColumns(fixture.world,bounds).isEmpty());
            assertEquals(pen,fixture.repo().load(fixture.id).orElseThrow());
        }finally{fixture.close();}
    }
}
