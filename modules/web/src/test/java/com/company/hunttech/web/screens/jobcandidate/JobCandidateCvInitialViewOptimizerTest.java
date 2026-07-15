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

public class JobCandidateCvInitialViewOptimizerTest {

    @Test
    public void copyWithoutCandidateCvPreservesStageOneAndOtherProperties() {
        View candidateCvView = new View(CandidateCV.class, false)
                .addProperty("datePost");
        View socialNetworkView = new View(SocialNetworkURLs.class, false)
                .addProperty("networkName");
        View interactionsView = new View(IteractionList.class, false)
                .addProperty("numberIteraction");

        View stageOneView = new View(JobCandidate.class, "stage-one", false)
                .addProperty("firstName")
                .addProperty("candidateCv", candidateCvView, FetchMode.BATCH)
                .addProperty("socialNetwork", socialNetworkView, FetchMode.BATCH)
                .addProperty("iteractionList", interactionsView, FetchMode.BATCH)
                .setLoadPartialEntities(true);

        View optimizedView = new JobCandidateCvInitialViewOptimizer()
                .copyWithoutCandidateCv(stageOneView);

        // Stage 2 удаляет только CV и не расширяет область изменения Stage 1.
        assertFalse(optimizedView.containsProperty("candidateCv"));
        assertTrue(optimizedView.containsProperty("firstName"));
        assertTrue(optimizedView.containsProperty("socialNetwork"));
        assertTrue(optimizedView.containsProperty("iteractionList"));
        assertTrue(stageOneView.containsProperty("candidateCv"));
        assertTrue(optimizedView.loadPartialEntities());

        assertEquals(FetchMode.BATCH, optimizedView.getProperty("socialNetwork").getFetchMode());
        assertEquals(FetchMode.BATCH, optimizedView.getProperty("iteractionList").getFetchMode());
        assertNotSame(socialNetworkView, optimizedView.getProperty("socialNetwork").getView());
        assertNotSame(interactionsView, optimizedView.getProperty("iteractionList").getView());
    }
}
