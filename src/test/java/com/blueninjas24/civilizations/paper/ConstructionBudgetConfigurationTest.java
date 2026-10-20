package com.blueninjas24.civilizations.paper;

import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy;
import com.blueninjas24.civilizations.construction.ConstructionBudgetPolicy.*;
import com.blueninjas24.civilizations.construction.ConstructionTemplate.Purpose;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionBudgetConfigurationTest {
    private String defaults()throws Exception{try(var in=getClass().getResourceAsStream("/construction.yml")){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
    private ConstructionBudgetPolicy read(String text)throws Exception{var yaml=new YamlConfiguration();yaml.loadFromString(text);return ConstructionBudgetConfiguration.read(yaml);}
    @Test void editableDefaultsProtectFoodPlusUpkeepAndExistingCommitmentsBeforeAnyQuote()throws Exception {
        var policy=read(defaults());assertEquals(ConstructionBudgetPolicy.prototype(),policy);
        var evidence=new Evidence(10_000,10,500,1000,0);
        var quote=policy.review(Source.VILLAGE,Purpose.HOME,4000,evidence);
        assertTrue(quote.allowed());assertEquals(6000,quote.protectedCash());assertEquals(4000,quote.availableCash());
        assertFalse(policy.review(Source.VILLAGE,Purpose.HOME,4001,evidence).allowed());
        assertFalse(policy.review(Source.VILLAGE,Purpose.PATH,4000,new Evidence(10_000,10,500,1000,2000)).allowed());
        assertFalse(policy.review(Source.VILLAGE,Purpose.WORKSHOP,1,evidence).allowed());
    }
    @Test void businessesProtectTheirRealOperatingObligationsAndDoNotPayAgainForOwnedGoods()throws Exception {
        var policy=read(defaults());var evidence=new Evidence(5000,0,1500,1000,0);
        var quote=policy.review(Source.BUSINESS,Purpose.STORAGE,2000,evidence);
        assertTrue(quote.allowed());assertEquals(3000,quote.protectedCash());
        assertFalse(policy.review(Source.BUSINESS,Purpose.STORAGE,2001,evidence).allowed());
        assertTrue(policy.review(Source.BUSINESS,Purpose.STORAGE,0,new Evidence(0,0,1500,0,0)).allowed());
        assertFalse(policy.review(Source.BUSINESS,Purpose.HOME,0,evidence).allowed());
    }
    @Test void administratorCanChangeReserveAndDisableAConstructionSourceWithoutChangingCode()throws Exception {
        var edited=read(defaults().replace("food-reserve-periods: 2","food-reserve-periods: 4"));
        assertEquals(9000,edited.review(Source.VILLAGE,Purpose.HOME,1,new Evidence(20_000,10,0,0,0)).protectedCash());
        var disabled=read(defaults().replace("enabled: true","enabled: false"));
        assertFalse(disabled.review(Source.BUSINESS,Purpose.STORAGE,0,new Evidence(5000,0,0,0,0)).allowed());
    }
    @Test void overflowAndMalformedOrMisspelledSettingsFailClosed()throws Exception {
        var policy=read(defaults());assertFalse(policy.review(Source.VILLAGE,Purpose.HOME,1,new Evidence(Long.MAX_VALUE,Long.MAX_VALUE,0,0,0)).allowed());
        assertThrows(IllegalArgumentException.class,()->read(defaults().replace("minimum-cash: 500","minimum-cash: 1.5")));
        assertThrows(IllegalArgumentException.class,()->read(defaults().replace("food-reserve-periods: 2","food-reserve-periods: 9223372036854775807")));
        assertThrows(IllegalArgumentException.class,()->read(defaults().replace("maximum-purchase:","maximum-puchase:")));
        assertThrows(IllegalArgumentException.class,()->read(defaults().replace("purposes: [home, path]","purposes: [home, home]")));
    }
    @Test void bufferedRecognitionIsEditableAndEarlierFundingFilesKeepTheSafeDefault()throws Exception {
        var yaml=new YamlConfiguration();yaml.loadFromString(defaults());
        assertEquals(4,ConstructionBudgetConfiguration.land(yaml).buildingBuffer());
        yaml.set("land.building-buffer",8);assertEquals(8,ConstructionBudgetConfiguration.land(yaml).buildingBuffer());
        yaml.set("land.building-buffer",17);assertThrows(IllegalArgumentException.class,()->ConstructionBudgetConfiguration.land(yaml));
        yaml.set("land",null);assertEquals(4,ConstructionBudgetConfiguration.land(yaml).buildingBuffer());
        assertEquals(ConstructionBudgetPolicy.prototype(),ConstructionBudgetConfiguration.read(yaml));
    }
    @Test void laborSharesAreEditableAndOldFilesReceiveDefaultsWithoutRewriting()throws Exception {
        var yaml=new YamlConfiguration();yaml.loadFromString(defaults());
        assertEquals(com.blueninjas24.civilizations.construction.ConstructionLaborPolicy.prototype(),ConstructionBudgetConfiguration.labor(yaml));
        yaml.set("labor.business-share-percent",50);assertEquals(50,ConstructionBudgetConfiguration.labor(yaml).businessSharePercent());
        yaml.set("labor.business-share-percent",101);assertThrows(IllegalArgumentException.class,()->ConstructionBudgetConfiguration.labor(yaml));
        yaml.set("labor",null);assertEquals(com.blueninjas24.civilizations.construction.ConstructionLaborPolicy.prototype(),ConstructionBudgetConfiguration.labor(yaml));
    }
}
