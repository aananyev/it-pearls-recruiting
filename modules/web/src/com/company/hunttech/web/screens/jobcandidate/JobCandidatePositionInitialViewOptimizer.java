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
 * Исключает {@code positionList} из стартового view редактора кандидата.
 *
 * <p>Список дополнительных позиций загружается только при явном запросе
 * (HR-Мастер или открытие вкладки «Позиции и вакансии»).
 * Существующие оптимизаторы Stages 1–3 исключают iteractionList,
 * candidateCv и socialNetwork. Данный компонент продлевает тот же подход.</p>
 */
@Component(JobCandidatePositionInitialViewOptimizer.NAME)
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class JobCandidatePositionInitialViewOptimizer implements ControllerDependencyInjector {

    public static final String NAME = "hunttech_JobCandidatePositionInitialViewOptimizer";

    private static final String LOADER_ID = "jobCandidateDl";
    private static final String PROPERTY = "positionList";

    @Override
    public void inject(InjectionContext injectionContext) {
        FrameOwner frameOwner = injectionContext.getFrameOwner();
        if (!(frameOwner instanceof JobCandidateEdit)) {
            return;
        }

        ScreenData screenData = UiControllerUtils.getScreenData(frameOwner);
        InstanceLoader<JobCandidate> loader = screenData.getLoader(LOADER_ID);
        View sourceView = loader.getView();

        if (sourceView == null || !sourceView.containsProperty(PROPERTY)) {
            return;
        }

        loader.setView(createViewWithout(sourceView));
    }

    View createViewWithout(View sourceView) {
        View target = new View(sourceView.getEntityClass(), null, false);
        target.setLoadPartialEntities(sourceView.loadPartialEntities());

        for (ViewProperty property : sourceView.getProperties()) {
            if (!PROPERTY.equals(property.getName())) {
                target.addProperty(
                        property.getName(),
                        property.getView() != null ? View.copy(property.getView()) : null,
                        property.getFetchMode());
            }
        }

        return target;
    }
}
