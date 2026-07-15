package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.Company;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.ScreenData;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.gui.screen.UiControllerUtils;
import com.haulmont.cuba.gui.sys.ControllerDependencyInjector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Исключает полную загрузку справочника компаний из критического пути
 * открытия {@link JobCandidateEdit}.
 *
 * <p>Поле {@code currentCompanyField} использует серверные подсказки и не
 * зависит от {@code currentCompaniesDc}. Оставшийся loader сохраняется для
 * совместимости существующего контроллера, но его автоматический запуск через
 * {@code @LoadDataBeforeShow} блокируется до выполнения SQL.</p>
 */
@Component(JobCandidateCompanyLoaderOptimizer.NAME)
@Order(Ordered.LOWEST_PRECEDENCE)
public class JobCandidateCompanyLoaderOptimizer implements ControllerDependencyInjector {

    public static final String NAME = "hunttech_JobCandidateCompanyLoaderOptimizer";

    private static final String CURRENT_COMPANIES_LOADER_ID = "currentCompaniesLc";

    @Override
    public void inject(InjectionContext injectionContext) {
        FrameOwner frameOwner = injectionContext.getFrameOwner();
        if (!(frameOwner instanceof JobCandidateEdit)) {
            return;
        }

        ScreenData screenData = UiControllerUtils.getScreenData(frameOwner);
        CollectionLoader<Company> currentCompaniesLoader =
                screenData.getLoader(CURRENT_COMPANIES_LOADER_ID);

        // SuggestionPickerField выполняет собственный ограниченный запрос по вводу,
        // поэтому полный список компаний не должен загружаться при открытии формы.
        currentCompaniesLoader.addPreLoadListener(loadEvent -> loadEvent.preventLoad());
    }
}
