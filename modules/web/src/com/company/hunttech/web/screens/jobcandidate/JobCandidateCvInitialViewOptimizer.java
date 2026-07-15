package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewProperty;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.model.ScreenData;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.gui.screen.UiControllerUtils;
import com.haulmont.cuba.gui.sys.ControllerDependencyInjector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Исключает коллекцию резюме из первичной загрузки редактора кандидата.
 *
 * <p>Полная коллекция {@code candidateCv} загружается существующим методом
 * {@link JobCandidateEdit#initTabResume()} только при первом открытии вкладки
 * «Резюме». Наличие резюме в боковой карточке определяется отдельным
 * скалярным запросом, который не материализует содержимое CV.</p>
 */
@Component(JobCandidateCvInitialViewOptimizer.NAME)
@Order(Ordered.LOWEST_PRECEDENCE)
public class JobCandidateCvInitialViewOptimizer implements ControllerDependencyInjector {

    public static final String NAME = "hunttech_JobCandidateCvInitialViewOptimizer";

    private static final String JOB_CANDIDATE_LOADER_ID = "jobCandidateDl";
    private static final String JOB_CANDIDATE_CONTAINER_ID = "jobCandidateDc";
    private static final String CANDIDATE_CV_PROPERTY = "candidateCv";
    private static final String CV_LABEL_ID = "labelCV";
    private static final String OPTIMIZED_VIEW_NAME = "jobCandidate-initial-without-cv";
    private static final String QUERY_CANDIDATE_CV_COUNT =
            "select count(e.id) from hunttech_CandidateCV e where e.candidate.id = :candidateId";

    @Inject
    private DataManager dataManager;

    @Override
    public void inject(InjectionContext injectionContext) {
        FrameOwner frameOwner = injectionContext.getFrameOwner();
        if (!(frameOwner instanceof JobCandidateEdit)) {
            return;
        }

        JobCandidateEdit screen = (JobCandidateEdit) frameOwner;
        ScreenData screenData = UiControllerUtils.getScreenData(frameOwner);
        InstanceLoader<JobCandidate> jobCandidateLoader = screenData.getLoader(JOB_CANDIDATE_LOADER_ID);
        View sourceView = jobCandidateLoader.getView();

        if (sourceView != null && sourceView.containsProperty(CANDIDATE_CV_PROPERTY)) {
            // Подменяет только view первичной загрузки, сохраняя XML и штатную загрузку вкладки резюме.
            jobCandidateLoader.setView(copyWithoutCandidateCv(sourceView));
        }

        // После показа корректирует индикатор скалярным COUNT без чтения unfetched-коллекции.
        screen.addAfterShowListener(event -> updateResumeAvailabilityLabel(screen, screenData));
    }

    /**
     * Копирует runtime-view со всеми fetch mode и вложенными представлениями,
     * исключая только коллекцию резюме из критического пути открытия формы.
     */
    View copyWithoutCandidateCv(View sourceView) {
        View optimizedView = new View(
                sourceView.getEntityClass(),
                OPTIMIZED_VIEW_NAME,
                false);
        optimizedView.setLoadPartialEntities(sourceView.loadPartialEntities());

        for (ViewProperty property : sourceView.getProperties()) {
            if (!CANDIDATE_CV_PROPERTY.equals(property.getName())) {
                optimizedView.addProperty(
                        property.getName(),
                        View.copy(property.getView()),
                        property.getFetchMode());
            }
        }

        return optimizedView;
    }

    /**
     * Определяет наличие резюме узким агрегатным запросом и обновляет боковой индикатор.
     */
    @SuppressWarnings("unchecked")
    private void updateResumeAvailabilityLabel(JobCandidateEdit screen, ScreenData screenData) {
        InstanceContainer<JobCandidate> candidateContainer =
                screenData.getContainer(JOB_CANDIDATE_CONTAINER_ID);
        JobCandidate candidate = candidateContainer.getItemOrNull();
        Label<String> cvLabel = (Label<String>) screen.getWindow().getComponent(CV_LABEL_ID);

        if (cvLabel == null) {
            return;
        }

        cvLabel.setValue(hasCandidateCv(candidate) ? "Резюме: ДА" : "Резюме: НЕТ");
    }

    /**
     * Не загружает сущности CandidateCV и их текстовые или файловые поля.
     */
    boolean hasCandidateCv(JobCandidate candidate) {
        if (candidate == null || PersistenceHelper.isNew(candidate) || candidate.getId() == null) {
            return false;
        }

        Long count = dataManager.loadValue(QUERY_CANDIDATE_CV_COUNT, Long.class)
                .parameter("candidateId", candidate.getId())
                .one();
        return count != null && count > 0L;
    }
}
