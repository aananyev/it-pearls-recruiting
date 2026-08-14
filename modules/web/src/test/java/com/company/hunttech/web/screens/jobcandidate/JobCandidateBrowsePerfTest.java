package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.HunttechWebTestContainer;
import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewProperty;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.UiControllerUtils;
import com.haulmont.cuba.web.testsupport.TestUiEnvironment;
import mockit.Mocked;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Measures {@link JobCandidateBrowse} open time (init through {@link JobCandidateBrowse#show()}).
 */
public class JobCandidateBrowsePerfTest {

    @Rule
    public final TestUiEnvironment environment = new TestUiEnvironment(HunttechWebTestContainer.Common.INSTANCE)
            .withUserLogin(JobCandidatePerfTestSupport.ADMIN_LOGIN)
            .withScreenPackages("com.company.hunttech.web.screens");

    @Mocked
    private DataService dataService;

    private JobCandidatePerfTestSupport.Metrics metrics;

    @Before
    public void setUp() {
        // Capture DataService counters so browse optimizations can be compared before/after.
        metrics = JobCandidatePerfTestSupport.registerDataServiceDelegate(dataService, environment);
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

        System.out.printf("PERF_RESULT JobCandidateBrowse openMicros=%d loadedCandidates=%d " +
                        "load=%d loadList=%d getCount=%d loadValues=%d " +
                        "jobCandidateLoad=%d jobCandidateLoadList=%d jobCandidateGetCount=%d%n",
                elapsedMicros,
                loadedCount,
                metrics.getLoadCalls(),
                metrics.getLoadListCalls(),
                metrics.getGetCountCalls(),
                metrics.getLoadValuesCalls(),
                metrics.getJobCandidateLoadCalls(),
                metrics.getJobCandidateLoadListCalls(),
                metrics.getJobCandidateGetCountCalls());
    }

    @Test
    public void testEmployeeStatusCacheViewLoadsInStaff() {
        View employeeView = JobCandidateBrowse.EMPLOYEE_STATUS_CACHE_VIEW;
        ViewProperty workStatusProperty = employeeView.getProperty("workStatus");

        Assert.assertNotNull("Employee cache view must load workStatus", workStatusProperty);
        Assert.assertNotNull("Employee workStatus must use a nested view", workStatusProperty.getView());
        Assert.assertTrue("Employee workStatus view must load inStaff for the status column",
                workStatusProperty.getView().containsProperty("inStaff"));
    }
}
