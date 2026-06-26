package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.ItpearlsWebTestContainer;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.UiControllerUtils;
import com.haulmont.cuba.web.testsupport.TestUiEnvironment;
import mockit.Mocked;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Measures {@link JobCandidateBrowse} open time (init through {@link JobCandidateBrowse#show()}).
 */
public class JobCandidateBrowsePerfTest {

    @Rule
    public final TestUiEnvironment environment = new TestUiEnvironment(ItpearlsWebTestContainer.Common.INSTANCE)
            .withUserLogin(JobCandidatePerfTestSupport.ADMIN_LOGIN)
            .withScreenPackages("com.company.itpearls.web.screens");

    @Mocked
    private DataService dataService;

    @Before
    public void setUp() {
        JobCandidatePerfTestSupport.registerDataServiceDelegate(dataService, environment);
    }

    @After
    public void tearDown() {
        JobCandidatePerfTestSupport.clearServiceMocks();
    }

    @Test
    public void testBrowseOpeningSpeed() {
        Screens screens = environment.getScreens();

        long startNanos = System.nanoTime();
        JobCandidateBrowse browse = screens.create(JobCandidateBrowse.class, OpenMode.ROOT);
        browse.show();
        long elapsedMicros = (System.nanoTime() - startNanos) / 1000L;

        CollectionContainer<JobCandidate> jobCandidatesDc =
                UiControllerUtils.getScreenData(browse).getContainer("jobCandidatesDc");
        int loadedCount = jobCandidatesDc.getItems().size();

        System.out.printf("Screen [JobCandidateBrowse] opened in %d µs%n", elapsedMicros);
        System.out.printf("JobCandidate records in jobCandidatesDc: %d%n", loadedCount);
    }
}
