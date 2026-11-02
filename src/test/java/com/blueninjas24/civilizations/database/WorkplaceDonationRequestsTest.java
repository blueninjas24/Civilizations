package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.application.*;
import com.blueninjas24.civilizations.construction.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkplaceDonationRequestsTest {
    @TempDir Path directory;WorkplaceDonationTest fixture;WorkplaceDonationRequests requests;
    @BeforeEach void setup()throws Exception{fixture=new WorkplaceDonationTest();fixture.directory=directory;fixture.setup();requests=new WorkplaceDonationRequests(fixture.c(),BuildingConsentRequests.Policy.prototype());}
    @AfterEach void close(){fixture.close();}
    BuildingConsentRequests.Context context(){return new BuildingConsentRequests.Context(fixture.donor,fixture.world,true,false,false,Set.of(fixture.world));}
    WorkplaceDonationRequests.Preview preview()throws Exception{return requests.preview(context(),fixture.business,fixture.layout,fixture.assessment.evidence(),WorkplaceInspection.Policy.prototype(),1020);}
    WorkplaceInspection.Evidence fresh(long captured){var old=fixture.assessment.evidence();return new WorkplaceInspection.Evidence(new DwellingInspection.Observation(old.structure().world(),old.structure().layoutHash(),captured,true,old.structure().cells()),old.marker(),old.kind(),true);}
    @Test void previewCreatesNoRightsAndOwnedFreshConfirmationUsesTheFrozenTerms()throws Exception{
        var preview=preview();assertEquals(0,fixture.count("building_records"));
        var saved=requests.confirm(context(),preview.token(),true,fresh(1030),1030);
        assertEquals(preview.token(),saved.consent().operation());assertEquals(fixture.layout.id(),saved.consent().building());
        assertEquals(saved,requests.confirm(context(),preview.token(),true,null,1040));assertEquals(1,fixture.count("business_premises"));
    }
    @Test void actorWorldOwnershipAndFreshPhysicalProofAreRequiredAtConfirmation()throws Exception{
        var preview=preview();var stranger=new BuildingConsentRequests.Context(UUID.randomUUID(),fixture.world,true,false,false,Set.of(fixture.world));
        assertThrows(SQLException.class,()->requests.confirm(stranger,preview.token(),true,fixture.assessment.evidence(),1030));
        var absent=new BuildingConsentRequests.Context(fixture.donor,fixture.world,true,false,false,Set.of());
        assertThrows(SQLException.class,()->requests.confirm(absent,preview.token(),true,fixture.assessment.evidence(),1030));
        assertThrows(SQLException.class,()->requests.confirm(context(),preview.token(),false,fixture.assessment.evidence(),1030));
        assertThrows(SQLException.class,()->requests.confirm(context(),preview.token(),true,null,1030));
        assertThrows(SQLException.class,()->requests.confirm(context(),preview.token(),true,fixture.assessment.evidence(),1030));
        assertThrows(SQLException.class,()->requests.confirm(context(),preview.token(),true,fixture.assessment.evidence(),7000));
        assertEquals(0,fixture.count("building_records"));
    }
    @Test void rolledBackConfirmationCanRetryTheSamePreviewWithAFreshAcceptedTimestamp()throws Exception{
        var preview=preview();fixture.sql("CREATE TRIGGER fail_preview_save AFTER INSERT ON business_premises BEGIN SELECT RAISE(ABORT,'Late fixture failure'); END");
        assertThrows(SQLException.class,()->requests.confirm(context(),preview.token(),true,fresh(1030),1030));
        assertEquals(0,fixture.count("building_consent_events"));fixture.sql("DROP TRIGGER fail_preview_save");
        var saved=requests.confirm(context(),preview.token(),true,fresh(1040),1040);
        assertEquals(preview.token(),saved.consent().operation());assertEquals(1040,saved.consent().atMillis());assertEquals(1,fixture.count("business_premises"));
    }
    @Test void cancellationExpiryAndBoundedPlayerSlotsNeverImplicitlyDonate()throws Exception{
        requests=new WorkplaceDonationRequests(fixture.c(),new BuildingConsentRequests.Policy(1,1000));var first=preview();
        var stranger=new BuildingConsentRequests.Context(UUID.randomUUID(),fixture.world,true,false,false,Set.of(fixture.world));
        assertThrows(SQLException.class,()->requests.preview(stranger,fixture.business,fixture.layout,fixture.assessment.evidence(),WorkplaceInspection.Policy.prototype(),1021));
        requests.cancel(fixture.donor);assertThrows(SQLException.class,()->requests.confirm(context(),first.token(),true,fixture.assessment.evidence(),1030));
        var next=preview();assertThrows(SQLException.class,()->requests.confirm(context(),next.token(),true,fixture.assessment.evidence(),2020));assertEquals(0,fixture.count("building_records"));
    }
}
