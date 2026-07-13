package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.HunttechWebTestContainer;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.core.InteractionService;
import com.company.hunttech.service.GetRoleService;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.web.testsupport.TestUiEnvironment;
import mockit.Mocked;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Measures {@link JobCandidateEdit} open time via {@link ScreenBuilders#editor(Class, com.haulmont.cuba.gui.components.FrameOwner)}.
 */
public class JobCandidateEditPerfTest {

    @Rule
    public final TestUiEnvironment environment = new TestUiEnvironment(HunttechWebTestContainer.Common.INSTANCE)
            .withUserLogin(JobCandidatePerfTestSupport.ADMIN_LOGIN)
            .withScreenPackages("com.company.hunttech.web.screens");

    @Mocked
    private DataService dataService;

    @Mocked
    private GetRoleService getRoleService;

    @Mocked
    private InteractionService interactionService;

    private JobCandidatePerfTestSupport.Metrics metrics;

    @Before
    public void setUp() {
        // Capture DataService counters around edit opening, including lazy-tab improvements.
        metrics = JobCandidatePerfTestSupport.registerDataServiceDelegate(
                dataService, environment, getRoleService, interactionService);
    }

    @After
    public void tearDown() {
        JobCandidatePerfTestSupport.clearServiceMocks();
    }

    @Test
    public void testEditOpeningSpeed() {
        JobCandidate candidate = JobCandidatePerfTestSupport.createJobCandidateForEdit(environment);

        Screens screens = environment.getScreens();
        JobCandidateBrowse browse = screens.create(JobCandidateBrowse.class, OpenMode.ROOT);
        browse.show();

        ScreenBuilders screenBuilders = AppBeans.get(ScreenBuilders.class);

        long startNanos = System.nanoTime();
        JobCandidateEdit edit = (JobCandidateEdit) screenBuilders.editor(JobCandidate.class, browse)
                .editEntity(candidate)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        edit.show();
        long elapsedMicros = (System.nanoTime() - startNanos) / 1000L;

        System.out.printf("PERF_RESULT JobCandidateEdit openMicros=%d editedCandidate=%s " +
                        "load=%d loadList=%d getCount=%d loadValues=%d " +
                        "jobCandidateLoad=%d jobCandidateLoadList=%d jobCandidateGetCount=%d%n",
                elapsedMicros,
                candidate.getId(),
                metrics.getLoadCalls(),
                metrics.getLoadListCalls(),
                metrics.getGetCountCalls(),
                metrics.getLoadValuesCalls(),
                metrics.getJobCandidateLoadCalls(),
                metrics.getJobCandidateLoadListCalls(),
                metrics.getJobCandidateGetCountCalls());
    }

    @Test
    public void testCurrentCompanyFieldHasCreateCompanyAction() {
        JobCandidate candidate = JobCandidatePerfTestSupport.createJobCandidateForEdit(environment);

        Screens screens = environment.getScreens();
        JobCandidateBrowse browse = screens.create(JobCandidateBrowse.class, OpenMode.ROOT);
        browse.show();

        ScreenBuilders screenBuilders = AppBeans.get(ScreenBuilders.class);

        JobCandidateEdit edit = (JobCandidateEdit) screenBuilders.editor(JobCandidate.class, browse)
                .editEntity(candidate)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        edit.show();

        TabSheet tabSheet = (TabSheet) edit.getWindow().getComponent("tabSheetSocialNetworks");
        tabSheet.setSelectedTab("tabCandidate");

        SuggestionPickerField<Company> currentCompanyField =
                (SuggestionPickerField<Company>) edit.getWindow().getComponent("currentCompanyField");

        assertNotNull(currentCompanyField.getAction("createCompany"));
    }

    @Test
    public void testCandidateTabDoesNotLoadFullCompanyOptions() {
        JobCandidate candidate = JobCandidatePerfTestSupport.createJobCandidateForEdit(environment);

        Screens screens = environment.getScreens();
        JobCandidateBrowse browse = screens.create(JobCandidateBrowse.class, OpenMode.ROOT);
        browse.show();

        ScreenBuilders screenBuilders = AppBeans.get(ScreenBuilders.class);

        JobCandidateEdit edit = (JobCandidateEdit) screenBuilders.editor(JobCandidate.class, browse)
                .editEntity(candidate)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        edit.show();

        TabSheet tabSheet = (TabSheet) edit.getWindow().getComponent("tabSheetSocialNetworks");
        int companyLoadListCallsBeforeTab = metrics.getCompanyLoadListCalls();
        tabSheet.setSelectedTab("tabCandidate");

        assertEquals("tabCandidate must not preload the full Company options container",
                companyLoadListCallsBeforeTab, metrics.getCompanyLoadListCalls());
    }
}
