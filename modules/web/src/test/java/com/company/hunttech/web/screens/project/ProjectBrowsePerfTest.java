package com.company.hunttech.web.screens.project;

import com.company.hunttech.HunttechWebTestContainer;
import com.company.hunttech.entity.Project;
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

public class ProjectBrowsePerfTest {

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
    public void testBrowseOpeningSpeed() {
        Screens screens = environment.getScreens();

        long startNanos = System.nanoTime();
        ProjectBrowse browse = screens.create(ProjectBrowse.class, OpenMode.ROOT);
        browse.show();
        long elapsedMicros = (System.nanoTime() - startNanos) / 1000L;

        CollectionContainer<Project> projectsDc =
                UiControllerUtils.getScreenData(browse).getContainer("projectsDc");
        int loadedCount = projectsDc.getItems().size();

        System.out.printf("PERF_RESULT ProjectBrowse openMicros=%d loadedProjects=%d " +
                        "load=%d loadList=%d getCount=%d loadValues=%d " +
                        "projectLoad=%d projectLoadList=%d projectGetCount=%d%n",
                elapsedMicros,
                loadedCount,
                metrics.getLoadCalls(),
                metrics.getLoadListCalls(),
                metrics.getGetCountCalls(),
                metrics.getLoadValuesCalls(),
                metrics.getProjectLoadCalls(),
                metrics.getProjectLoadListCalls(),
                metrics.getProjectGetCountCalls());
    }
}
