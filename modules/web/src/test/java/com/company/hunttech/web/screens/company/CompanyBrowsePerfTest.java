package com.company.hunttech.web.screens.company;

import com.company.hunttech.HunttechWebTestContainer;
import com.company.hunttech.entity.Company;
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

public class CompanyBrowsePerfTest {

    @Rule
    public final TestUiEnvironment environment = new TestUiEnvironment(HunttechWebTestContainer.Common.INSTANCE)
            .withUserLogin(CompanyPerfTestSupport.ADMIN_LOGIN)
            .withScreenPackages("com.company.hunttech.web.screens");

    @Mocked
    private DataService dataService;

    private CompanyPerfTestSupport.Metrics metrics;

    @Before
    public void setUp() {
        metrics = CompanyPerfTestSupport.registerDataServiceDelegate(dataService, environment);
    }

    @After
    public void tearDown() {
        CompanyPerfTestSupport.clearServiceMocks();
    }

    @Test
    public void testBrowseOpeningSpeed() {
        Screens screens = environment.getScreens();

        long startNanos = System.nanoTime();
        CompanyBrowse browse = screens.create(CompanyBrowse.class, OpenMode.ROOT);
        browse.show();
        long elapsedMicros = (System.nanoTime() - startNanos) / 1000L;

        CollectionContainer<Company> companiesDc =
                UiControllerUtils.getScreenData(browse).getContainer("companiesDc");
        int loadedCount = companiesDc.getItems().size();

        System.out.printf("PERF_RESULT CompanyBrowse openMicros=%d loadedCompanies=%d " +
                        "load=%d loadList=%d getCount=%d loadValues=%d " +
                        "companyLoad=%d companyLoadList=%d companyGetCount=%d%n",
                elapsedMicros,
                loadedCount,
                metrics.getLoadCalls(),
                metrics.getLoadListCalls(),
                metrics.getGetCountCalls(),
                metrics.getLoadValuesCalls(),
                metrics.getCompanyLoadCalls(),
                metrics.getCompanyLoadListCalls(),
                metrics.getCompanyGetCountCalls());
    }
}
