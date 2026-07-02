package com.company.itpearls.web.screens.company;

import com.company.itpearls.ItpearlsWebTestContainer;
import com.company.itpearls.entity.Company;
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

public class CompanyEditPerfTest {

    @Rule
    public final TestUiEnvironment environment = new TestUiEnvironment(ItpearlsWebTestContainer.Common.INSTANCE)
            .withUserLogin(CompanyPerfTestSupport.ADMIN_LOGIN)
            .withScreenPackages("com.company.itpearls.web.screens");

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
    public void testEditOpeningSpeed() {
        Company company = CompanyPerfTestSupport.createCompanyForEdit(environment);

        Screens screens = environment.getScreens();
        CompanyBrowse browse = screens.create(CompanyBrowse.class, OpenMode.ROOT);
        browse.show();
        metrics.reset();

        ScreenBuilders screenBuilders = AppBeans.get(ScreenBuilders.class);

        long startNanos = System.nanoTime();
        CompanyEdit edit = (CompanyEdit) screenBuilders.editor(Company.class, browse)
                .editEntity(company)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        edit.show();
        long elapsedMicros = (System.nanoTime() - startNanos) / 1000L;

        System.out.printf("PERF_RESULT CompanyEdit openMicros=%d " +
                        "load=%d loadList=%d getCount=%d loadValues=%d " +
                        "companyLoad=%d companyLoadList=%d companyGetCount=%d%n",
                elapsedMicros,
                metrics.getLoadCalls(),
                metrics.getLoadListCalls(),
                metrics.getGetCountCalls(),
                metrics.getLoadValuesCalls(),
                metrics.getCompanyLoadCalls(),
                metrics.getCompanyLoadListCalls(),
                metrics.getCompanyGetCountCalls());
    }
}
