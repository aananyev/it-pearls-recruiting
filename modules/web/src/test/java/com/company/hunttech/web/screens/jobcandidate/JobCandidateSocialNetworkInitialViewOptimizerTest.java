package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.SocialNetworkURLs;
import com.haulmont.cuba.core.global.FetchMode;
import com.haulmont.cuba.core.global.View;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class JobCandidateSocialNetworkInitialViewOptimizerTest {

    @Test
    public void copyWithoutSocialNetworkRemovesOnlyTargetProperty() {
        View socialNetworkView = new View(SocialNetworkURLs.class, false)
                .addProperty("networkName");
        View candidateCvView = new View(CandidateCV.class, false)
                .addProperty("datePost");
        View interactionsView = new View(IteractionList.class, false)
                .addProperty("numberIteraction");
        View companyView = new View(Company.class, false)
                .addProperty("comanyName");

        View sourceView = new View(JobCandidate.class, "stage-three-source", false)
                .addProperty("firstName")
                .addProperty("socialNetwork", socialNetworkView, FetchMode.BATCH)
                .addProperty("candidateCv", candidateCvView, FetchMode.BATCH)
                .addProperty("iteractionList", interactionsView, FetchMode.BATCH)
                .addProperty("currentCompany", companyView, FetchMode.BATCH)
                .setLoadPartialEntities(true);

        View optimizedView = new JobCandidateSocialNetworkInitialViewOptimizer()
                .copyWithoutSocialNetwork(sourceView);

        // Stage 3 исключает только socialNetwork и не изменяет исходный runtime-view.
        assertFalse(optimizedView.containsProperty("socialNetwork"));
        assertTrue(optimizedView.containsProperty("candidateCv"));
        assertTrue(optimizedView.containsProperty("iteractionList"));
        assertTrue(optimizedView.containsProperty("currentCompany"));
        assertTrue(optimizedView.containsProperty("firstName"));
        assertTrue(sourceView.containsProperty("socialNetwork"));
        assertTrue(optimizedView.loadPartialEntities());

        assertEquals(FetchMode.BATCH, optimizedView.getProperty("currentCompany").getFetchMode());
        assertNotSame(companyView, optimizedView.getProperty("currentCompany").getView());
    }

    @Test
    public void allProgressiveLoadingStagesCanBeAppliedSequentially() {
        View sourceView = new View(JobCandidate.class, "full-initial-view", false)
                .addProperty("firstName")
                .addProperty("iteractionList", new View(IteractionList.class, false), FetchMode.BATCH)
                .addProperty("candidateCv", new View(CandidateCV.class, false), FetchMode.BATCH)
                .addProperty("socialNetwork", new View(SocialNetworkURLs.class, false), FetchMode.BATCH);

        View withoutInteractions = new JobCandidateInitialViewOptimizer()
                .copyWithoutInteractions(sourceView);
        View withoutCandidateCv = new JobCandidateCvInitialViewOptimizer()
                .copyWithoutCandidateCv(withoutInteractions);
        View optimizedView = new JobCandidateSocialNetworkInitialViewOptimizer()
                .copyWithoutSocialNetwork(withoutCandidateCv);

        assertFalse(optimizedView.containsProperty("iteractionList"));
        assertFalse(optimizedView.containsProperty("candidateCv"));
        assertFalse(optimizedView.containsProperty("socialNetwork"));
        assertTrue(optimizedView.containsProperty("firstName"));
    }
}
