package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewProperty;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.model.ScreenData;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.gui.screen.UiControllerUtils;
import com.haulmont.cuba.gui.sys.ControllerDependencyInjector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Сокращает стартовый view редактора кандидата до запуска его lifecycle.
 *
 * <p>Коллекция взаимодействий исключается только из первичной загрузки.
 * Существующий {@link JobCandidateEdit} загружает её штатным методом
 * {@code ensureInteractionsLoaded()} при первом открытии вкладки взаимодействий.</p>
 */
@Component(JobCandidateInitialViewOptimizer.NAME)
@Order(Ordered.LOWEST_PRECEDENCE)
public class JobCandidateInitialViewOptimizer implements ControllerDependencyInjector {

    public static final String NAME = "hunttech_JobCandidateInitialViewOptimizer";

    private static final String JOB_CANDIDATE_LOADER_ID = "jobCandidateDl";
    private static final String INTERACTIONS_PROPERTY = "iteractionList";
    private static final String OPTIMIZED_VIEW_NAME = "jobCandidate-initial-without-interactions";

    @Override
    public void inject(InjectionContext injectionContext) {
        FrameOwner frameOwner = injectionContext.getFrameOwner();
        if (!(frameOwner instanceof JobCandidateEdit)) {
            return;
        }

        ScreenData screenData = UiControllerUtils.getScreenData(frameOwner);
        InstanceLoader<JobCandidate> jobCandidateLoader = screenData.getLoader(JOB_CANDIDATE_LOADER_ID);
        View sourceView = jobCandidateLoader.getView();

        if (sourceView == null || !sourceView.containsProperty(INTERACTIONS_PROPERTY)) {
            return;
        }

        // Подменяет только view первичной загрузки, не изменяя XML, loader ID и DataContext экрана.
        jobCandidateLoader.setView(copyWithoutInteractions(sourceView));
    }

    /**
     * Копирует исходный view со всеми fetch mode и вложенными представлениями,
     * исключая коллекцию взаимодействий из критического пути открытия формы.
     */
    View copyWithoutInteractions(View sourceView) {
        View optimizedView = new View(
                sourceView.getEntityClass(),
                OPTIMIZED_VIEW_NAME,
                false);
        optimizedView.setLoadPartialEntities(sourceView.loadPartialEntities());

        for (ViewProperty property : sourceView.getProperties()) {
            if (!INTERACTIONS_PROPERTY.equals(property.getName())) {
                optimizedView.addProperty(
                        property.getName(),
                        View.copy(property.getView()),
                        property.getFetchMode());
            }
        }

        return optimizedView;
    }
}
