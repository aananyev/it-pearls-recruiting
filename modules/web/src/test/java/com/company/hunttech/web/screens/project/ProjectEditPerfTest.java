package com.company.hunttech.web.screens.project;

import com.company.hunttech.HunttechWebTestContainer;
import com.company.hunttech.entity.Project;
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

public class ProjectEditPerfTest {

    @Rule
    public final TestUiEnvironment environment = new TestUiEnvironment(HunttechWebTestContainer.Common.INSTANCE)
            .withUserLogin(ProjectPerfTestSupport.ADMIN_LOGIN)
            .withScreenPackages("com.company.hunttech.web.screens");

    @Mocked
    private DataService dataService;

    private ProjectPerfTestSupport.Metrics metrics;

    @Before
    public void setUp() {
        metrics = ProjectPerfTestSupport.registerDataServiceDelegate(dataService, environment);
    }

    @After
    public void tearDown() {
        ProjectPerfTestSupport.clearServiceMocks();
    }

    @Test
    public void testEditOpeningSpeed() {
        Project project = ProjectPerfTestSupport.createProjectForEdit(environment);

        Screens screens = environment.getScreens();
        ProjectBrowse browse = screens.create(ProjectBrowse.class, OpenMode.ROOT);
        browse.show();
        metrics.reset();

        ScreenBuilders screenBuilders = AppBeans.get(ScreenBuilders.class);

        long startNanos = System.nanoTime();
        ProjectEdit edit = (ProjectEdit) screenBuilders.editor(Project.class, browse)
                .editEntity(project)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        edit.show();
        long elapsedMicros = (System.nanoTime() - startNanos) / 1000L;

        System.out.printf("PERF_RESULT ProjectEdit openMicros=%d " +
                        "load=%d loadList=%d getCount=%d loadValues=%d " +
                        "projectLoad=%d projectLoadList=%d projectGetCount=%d%n",
                elapsedMicros,
                metrics.getLoadCalls(),
                metrics.getLoadListCalls(),
                metrics.getGetCountCalls(),
                metrics.getLoadValuesCalls(),
                metrics.getProjectLoadCalls(),
                metrics.getProjectLoadListCalls(),
                metrics.getProjectGetCountCalls());
    }
}
