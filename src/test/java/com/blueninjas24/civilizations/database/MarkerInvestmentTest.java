package com.blueninjas24.civilizations.database;

import com.blueninjas24.civilizations.industry.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real funded workshop preparation/point proof composed with the shared startup ledger. */
class MarkerInvestmentTest {
    @TempDir Path directory;WorkshopInvestmentTest f;ResourceInvestment selection;
    @BeforeEach void setup()throws Exception {
        f=new WorkshopInvestmentTest();f.directory=directory;f.setup();f.qualified();
        var c=f.db.getConnection();var operations=BusinessOperationPolicy.prototype();var grants=StartupGrantPolicy.prototype();
        selection=new ResourceInvestment(c,BusinessAllocationPolicy.prototype(),f.industry,
                new AutomaticMineFounding(c,MineFoundingPolicy.disabled(),f.industry,operations,grants,216,73,()->true),
                new AutomaticQuarryFounding(c,QuarryFoundingPolicy.disabled(),QuarryPolicy.disabled(),f.industry,operations,grants,216,73,()->true),
                new AutomaticForestryFounding(c,ForestryFoundingPolicy.disabled(),ForestryPolicy.disabled(),f.industry,operations,grants,216,73,()->true),
                new LocalIndustryDemand(c,f.industry,operations).withFarmTools(f.upkeep,FarmToolBudgetPolicy.prototype()),
                new VillageSupplyPurchases(c,VillageSupplyPolicy.disabled(),StudyPolicy.disabled(),operations))
                .withMarkerCandidates((town,step,now,worlds)->f.engine().prepare(town,step,now,worlds).draft().stream().map(d->
                        new ResourceInvestment.Proposal(d.startup().founding().business().id(),"blacksmith",d.supplies().recipe().outputs().keySet(),
                                d.startup().founding().startupHundredths()+d.startup().capital(),"Real qualified funded workshop preflight")).toList());
    }
    @AfterEach void close(){if(f!=null)f.close();}
    long count(String table)throws Exception{try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}}
    @Test void winningMarkerWaitsForPointProofWithoutReservingMoneyJobsOrSupplies()throws Exception {
        var before=f.snapshot();String result=selection.review(f.town,0,3000,Set.of(f.world));
        assertTrue(result.contains("awaits fresh loaded workplace confirmation"),result);assertEquals(before,f.snapshot());
        var draft=f.draft();var audit=selection.markerApproval(f.town,draft.anchor().business(),0,3000,Set.of(f.world));
        assertEquals(before,f.snapshot());
        var missing=new WorkplaceOperations.Confirmation(draft.anchor(),false,3000);
        assertThrows(SQLException.class,()->f.engine().accept(draft,missing,3000,Set.of(f.world),audit));assertEquals(before,f.snapshot());
    }
    @Test void selectedWorkshopCommitsOnePaidStartupAndFairnessRecordTogether()throws Exception {
        var draft=f.draft();var business=draft.anchor().business();
        f.engine().accept(draft,f.proof(draft),3000,Set.of(f.world),selection.markerApproval(f.town,business,0,3000,Set.of(f.world)));
        assertEquals(1,count("resource_investment_reviews"));assertEquals(1,count("startup_grants"));assertEquals(1,count("workshop_starter_orders"));
        try(var s=f.db.getConnection().createStatement();var r=s.executeQuery("SELECT last_proposal,sequence FROM resource_investment_state")) {
            r.next();assertEquals(business.toString(),r.getString(1));assertEquals(1,r.getLong(2));
        }
        assertEquals(28500,new TreasuryRepository(f.db.getConnection()).load(f.town).orElseThrow().hundredths());
        var after=f.snapshot();f.engine().accept(draft,f.proof(draft),3000,Set.of(f.world));assertEquals(after,f.snapshot());
        assertThrows(BusinessStartupGrants.Declined.class,()->selection.markerApproval(f.town,business,0,4000,Set.of(f.world)));
    }
    @Test void lateSelectionAuditFailureRollsBackTheWholePaidMarkerStartupAndCanRetry()throws Exception {
        var draft=f.draft();f.sql("CREATE TRIGGER reject_marker_priority BEFORE INSERT ON resource_investment_reviews BEGIN SELECT RAISE(ABORT,'fixture audit failure'); END");
        var before=f.snapshot();
        assertThrows(SQLException.class,()->f.engine().accept(draft,f.proof(draft),3000,Set.of(f.world),
                selection.markerApproval(f.town,draft.anchor().business(),0,3000,Set.of(f.world))));
        assertEquals(before,f.snapshot());assertEquals(0,count("resource_investment_state"));
        f.sql("DROP TRIGGER reject_marker_priority");
        f.engine().accept(draft,f.proof(draft),3000,Set.of(f.world),selection.markerApproval(f.town,draft.anchor().business(),0,3000,Set.of(f.world)));
        assertEquals(1,count("resource_investment_reviews"));
    }
}
