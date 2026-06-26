package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.ItpearlsWebTestContainer;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.core.InteractionService;
import com.company.itpearls.service.GetRoleService;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.web.testsupport.TestUiEnvironment;
import mockit.Mocked;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Measures {@link JobCandidateEdit} open time via {@link ScreenBuilders#editor(Class, com.haulmont.cuba.gui.components.FrameOwner)}.
 */
public class JobCandidateEditPerfTest {

    @Rule
    public final TestUiEnvironment environment = new TestUiEnvironment(ItpearlsWebTestContainer.Common.INSTANCE)
            .withUserLogin(JobCandidatePerfTestSupport.ADMIN_LOGIN)
            .withScreenPackages("com.company.itpearls.web.screens");

    @Mocked
    private DataService dataService;

    @Mocked
    private GetRoleService getRoleService;

    @Mocked
    private InteractionService interactionService;

    @Before
    public void setUp() {
        JobCandidatePerfTestSupport.registerDataServiceDelegate(
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

        System.out.printf("Screen [JobCandidateEdit] opened in %d µs%n", elapsedMicros);
        System.out.printf("Edited JobCandidate id: %s%n", candidate.getId());
    }
}
