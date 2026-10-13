package com.blueninjas24.civilizations.economy;

import com.blueninjas24.civilizations.settlement.VillageObservation.Position;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CropScanTest {
    @Test void typedGrainIsAStableSubsetAndLegacySourcesCannotInventIt() {
        var scan=new CropScan(new CropScan.Source() {
            public boolean emptySection(int section){return false;}
            public boolean plantedCrop(int x,int y,int z){throw new AssertionError("Typed scan must not duplicate reads");}
            public CropScan.Kind cropKind(int x,int y,int z){return z==0&&x==0?CropScan.Kind.GRAIN:z==0&&x==1?CropScan.Kind.OTHER:CropScan.Kind.NONE;}
        },-1,2,64,65);
        assertThrows(IllegalStateException.class,scan::grainCrops);
        while(!scan.complete())assertTrue(scan.advance(13)<=13);
        assertEquals(java.util.List.of(new Position(-16,64,32),new Position(-15,64,32)),scan.crops());
        assertEquals(java.util.List.of(new Position(-16,64,32)),scan.grainCrops());
        assertThrows(UnsupportedOperationException.class,()->scan.grainCrops().clear());
        var legacy=new CropScan(new CropScan.Source(){public boolean emptySection(int section){return false;}public boolean plantedCrop(int x,int y,int z){return x==0&&z==0;}},0,0,64,65);
        legacy.advance(256);assertEquals(1,legacy.crops().size());assertTrue(legacy.grainCrops().isEmpty());
    }
    @Test void incrementalNegativeCoordinateScanSkipsEmptySectionsAndRespectsReadBudget() {
        var scan = new CropScan(new CropScan.Source() {
            public boolean emptySection(int section) { return section != -1; }
            public boolean plantedCrop(int x,int y,int z) { return x==15 && z==15 && y==-1; }
        },-1,-1,-32,16);
        assertThrows(IllegalStateException.class,scan::crops);
        int reads=0; while(!scan.complete()) { int used=scan.advance(17); assertTrue(used<=17); reads+=used; }
        assertEquals(4096,reads); assertEquals(java.util.List.of(new Position(-1,-1,-1)),scan.crops());
    }
    @Test void horizontalAndVerticalEligibilityIsBoundedWithoutIntegerOverflow() {
        var policy=FarmObservation.Policy.prototype(); var home=new Position(0,64,0);
        assertTrue(CropScan.near(new Position(32,80,0),home,policy)); assertFalse(CropScan.near(new Position(32,81,0),home,policy));
        assertFalse(CropScan.near(new Position(32,64,32),home,policy));
        assertFalse(CropScan.near(new Position(Integer.MAX_VALUE,64,0),new Position(Integer.MIN_VALUE,64,0),policy));
    }
}
