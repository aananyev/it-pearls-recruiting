package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.SocialNetworkURLs;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewProperty;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.model.DataContext;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Исключает социальные сети из первичной загрузки редактора кандидата.
 *
 * <p>Коллекция {@code socialNetwork} загружается только при первом открытии
 * вкладки «Контакты» или «Социальные сети». Компонент не изменяет контроллер,
 * XML-дескриптор, CRUD и правила заполнения справочников.</p>
 */
@Component(JobCandidateSocialNetworkInitialViewOptimizer.NAME)
@Order(Ordered.LOWEST_PRECEDENCE)
public class JobCandidateSocialNetworkInitialViewOptimizer implements ControllerDependencyInjector {

    public static final String NAME = "hunttech_JobCandidateSocialNetworkInitialViewOptimizer";

    private static final String JOB_CANDIDATE_LOADER_ID = "jobCandidateDl";
    private static final String JOB_CANDIDATE_CONTAINER_ID = "jobCandidateDc";
    private static final String SOCIAL_NETWORK_PROPERTY = "socialNetwork";
    private static final String TAB_SHEET_ID = "tabSheetSocialNetworks";
    private static final String CONTACT_TAB_ID = "tabContactInfo";
    private static final String SOCIAL_NETWORK_TAB_ID = "tabSocialNetworks";
    private static final String SOCIAL_NETWORK_VIEW = "socialNetworkURLs-view";
    private static final String OPTIMIZED_VIEW_NAME = "jobCandidate-initial-without-social-networks";
    private static final String QUERY_SOCIAL_NETWORKS =
            "select e from hunttech_SocialNetworkURLs e "
                    + "where e.jobCandidate.id = :candidateId "
                    + "order by e.networkName";

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

        if (sourceView != null && sourceView.containsProperty(SOCIAL_NETWORK_PROPERTY)) {
            // Сохраняет результат предыдущих этапов и исключает только socialNetwork.
            jobCandidateLoader.setView(copyWithoutSocialNetwork(sourceView));
        }

        screen.addAfterShowListener(event -> installLazySocialNetworkLoading(
                screen,
                screenData,
                jobCandidateLoader.getDataContext()));
    }

    /**
     * Копирует runtime-view со всеми вложенными представлениями и fetch mode,
     * исключая только коллекцию социальных сетей.
     */
    View copyWithoutSocialNetwork(View sourceView) {
        View optimizedView = new View(
                sourceView.getEntityClass(),
                OPTIMIZED_VIEW_NAME,
                false);
        optimizedView.setLoadPartialEntities(sourceView.loadPartialEntities());

        for (ViewProperty property : sourceView.getProperties()) {
            if (!SOCIAL_NETWORK_PROPERTY.equals(property.getName())) {
                optimizedView.addProperty(
                        property.getName(),
                        View.copy(property.getView()),
                        property.getFetchMode());
            }
        }

        return optimizedView;
    }

    /**
     * Подключает загрузку после штатного listener-а контроллера. На вкладке
     * «Контакты» контроллер может загрузить коллекцию первым; проверка
     * {@link PersistenceHelper#isLoaded(Object, String)} исключает второй запрос.
     */
    @SuppressWarnings("unchecked")
    private void installLazySocialNetworkLoading(JobCandidateEdit screen,
                                                 ScreenData screenData,
                                                 DataContext dataContext) {
        TabSheet tabSheet = (TabSheet) screen.getWindow().getComponent(TAB_SHEET_ID);
        InstanceContainer<JobCandidate> candidateContainer =
                screenData.getContainer(JOB_CANDIDATE_CONTAINER_ID);
        if (tabSheet == null || candidateContainer == null) {
            return;
        }

        AtomicBoolean loading = new AtomicBoolean(false);
        AtomicBoolean loaded = new AtomicBoolean(false);
        Runnable loadForSelectedTab = () -> {
            if (isSocialNetworkDataTabSelected(tabSheet)) {
                ensureSocialNetworksLoaded(candidateContainer, dataContext, loading, loaded);
            }
        };

        tabSheet.addSelectedTabChangeListener(event -> loadForSelectedTab.run());
        // Обрабатывает сценарий, когда нужная вкладка выбрана до AfterShow.
        loadForSelectedTab.run();
    }

    private boolean isSocialNetworkDataTabSelected(TabSheet tabSheet) {
        TabSheet.Tab selectedTab = tabSheet.getSelectedTab();
        if (selectedTab == null) {
            return false;
        }
        String tabName = selectedTab.getName();
        return CONTACT_TAB_ID.equals(tabName) || SOCIAL_NETWORK_TAB_ID.equals(tabName);
    }

    /**
     * Загружает социальные сети один раз и merge-ит строки в экранный DataContext.
     * Если штатный код вкладки уже загрузил свойство, повторный SQL не выполняется.
     */
    private void ensureSocialNetworksLoaded(InstanceContainer<JobCandidate> candidateContainer,
                                            DataContext dataContext,
                                            AtomicBoolean loading,
                                            AtomicBoolean loaded) {
        JobCandidate candidate = candidateContainer.getItemOrNull();
        if (candidate == null || loaded.get()) {
            return;
        }

        if (PersistenceHelper.isNew(candidate) || candidate.getId() == null) {
            // Для новой сущности CUBA использует доступную пустую композицию без SQL.
            loaded.set(true);
            return;
        }

        if (PersistenceHelper.isLoaded(candidate, SOCIAL_NETWORK_PROPERTY)) {
            // Вкладка «Контакты» могла выполнить существующую загрузку раньше этого listener-а.
            loaded.set(true);
            return;
        }

        if (!loading.compareAndSet(false, true)) {
            return;
        }

        try {
            List<SocialNetworkURLs> socialNetworks = dataManager.load(SocialNetworkURLs.class)
                    .query(QUERY_SOCIAL_NETWORKS)
                    .parameter("candidateId", candidate.getId())
                    .view(SOCIAL_NETWORK_VIEW)
                    .list();

            List<SocialNetworkURLs> mergedSocialNetworks = socialNetworks.stream()
                    .map(dataContext::merge)
                    .collect(Collectors.toList());
            candidate.setSocialNetwork(mergedSocialNetworks);
            loaded.set(true);
        } catch (RuntimeException exception) {
            // Разрешает повторную попытку после временной ошибки загрузки.
            loaded.set(false);
            throw exception;
        } finally {
            loading.set(false);
        }
    }
}
