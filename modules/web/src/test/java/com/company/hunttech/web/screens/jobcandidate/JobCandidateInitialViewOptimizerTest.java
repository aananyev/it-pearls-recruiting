package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.CandidateCV;
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

public class JobCandidateInitialViewOptimizerTest {

    @Test
    public void copyWithoutInteractionsPreservesOtherPropertiesAndFetchModes() {
        View candidateCvView = new View(CandidateCV.class, false)
                .addProperty("datePost");
        View socialNetworkView = new View(SocialNetworkURLs.class, false)
                .addProperty("networkName");
        View interactionsView = new View(IteractionList.class, false)
                .addProperty("numberIteraction");

        View sourceView = new View(JobCandidate.class, "source", false)
                .addProperty("firstName")
                .addProperty("candidateCv", candidateCvView, FetchMode.BATCH)
                .addProperty("socialNetwork", socialNetworkView, FetchMode.BATCH)
                .addProperty("iteractionList", interactionsView, FetchMode.BATCH)
                .setLoadPartialEntities(true);

        View optimizedView = new JobCandidateInitialViewOptimizer()
                .copyWithoutInteractions(sourceView);

        assertFalse(optimizedView.containsProperty("iteractionList"));
        assertTrue(optimizedView.containsProperty("firstName"));
        assertTrue(optimizedView.containsProperty("candidateCv"));
        assertTrue(optimizedView.containsProperty("socialNetwork"));
        assertTrue(optimizedView.loadPartialEntities());
        assertEquals(FetchMode.BATCH, optimizedView.getProperty("candidateCv").getFetchMode());
        assertEquals(FetchMode.BATCH, optimizedView.getProperty("socialNetwork").getFetchMode());
        assertNotSame(candidateCvView, optimizedView.getProperty("candidateCv").getView());
        assertNotSame(socialNetworkView, optimizedView.getProperty("socialNetwork").getView());
    }
}
