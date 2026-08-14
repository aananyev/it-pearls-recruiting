package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.Project;
import com.company.hunttech.entity.SocialNetworkType;
import com.company.hunttech.entity.SocialNetworkURLs;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewProperty;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.model.CollectionPropertyContainer;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.model.ScreenData;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.gui.screen.UiControllerUtils;
import com.haulmont.cuba.gui.sys.ControllerDependencyInjector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Исключает коллекцию резюме из первичной загрузки редактора кандидата.
 *
 * <p>Коллекция {@code candidateCv} загружается только при первом открытии
 * вкладки «Резюме». Индикатор ДА/НЕТ полностью принадлежит фоновой задаче
 * {@link JobCandidateEdit}. Оптимизатор не выполняет CandidateCV COUNT.</p>
 *
 * <p>После ленивой загрузки резюме и социальных сетей оптимизатор точечно
 * гидратирует только проекты и типы социальных сетей, необходимые для логотипов.</p>
 */
@Component(JobCandidateCvInitialViewOptimizer.NAME)
@Order(Ordered.LOWEST_PRECEDENCE)
public class JobCandidateCvInitialViewOptimizer implements ControllerDependencyInjector {

    public static final String NAME = "hunttech_JobCandidateCvInitialViewOptimizer";

    private static final String JOB_CANDIDATE_LOADER_ID = "jobCandidateDl";
    private static final String CANDIDATE_CV_CONTAINER_ID = "jobCandidateCandidateCvsDc";
    private static final String SOCIAL_NETWORK_CONTAINER_ID = "jobCandidateSocialNetworksDc";
    private static final String CANDIDATE_CV_PROPERTY = "candidateCv";
    private static final String TAB_SHEET_ID = "tabSheetSocialNetworks";
    private static final String RESUME_TAB_ID = "tabResume";
    private static final String CONTACT_TAB_ID = "tabContactInfo";
    private static final String SOCIAL_NETWORK_TAB_ID = "tabSocialNetworks";
    private static final String PROJECT_WITH_LOGO_VIEW = "project-browse-view";
    private static final String SOCIAL_NETWORK_TYPE_WITH_LOGO_VIEW = "socialNetworkType-view";
    private static final String OPTIMIZED_VIEW_NAME = "jobCandidate-initial-without-cv";
    private static final String QUERY_PROJECTS_WITH_LOGOS =
            "select e from hunttech_Project e where e.id in :projectIds";
    private static final String QUERY_SOCIAL_NETWORK_TYPES_WITH_LOGOS =
            "select e from hunttech_SocialNetworkType e where e.id in :typeIds";

    @Inject
    private DataManager dataManager;

    /**
     * Подменяет только runtime-view первичного loader и подключает обработчики
     * гидратации после создания экрана. XML, loader ID и DataContext не изменяются.
     */
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
            // Убирает только candidateCv, сохраняя остальные свойства и fetch mode runtime-view.
            jobCandidateLoader.setView(copyWithoutCandidateCv(sourceView));
        }

        screen.addAfterShowListener(event -> {
            DataContext dataContext = jobCandidateLoader.getDataContext();
            // Hydration подключается после штатных listener-ов ленивых вкладок.
            installResumeProjectLogoHydration(screen, screenData, dataContext);
            installSocialNetworkLogoHydration(screen, screenData, dataContext);
        });
    }

    /**
     * Копирует runtime-view со всеми вложенными представлениями и fetch mode,
     * исключая только композицию candidateCv из initial load.
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
     * После ленивой загрузки таблицы резюме одним запросом догружает проекты
     * и их логотипы. Повторная гидратация разрешается после CRUD коллекции CV.
     */
    @SuppressWarnings("unchecked")
    private void installResumeProjectLogoHydration(JobCandidateEdit screen,
                                                    ScreenData screenData,
                                                    DataContext dataContext) {
        TabSheet tabSheet = (TabSheet) screen.getWindow().getComponent(TAB_SHEET_ID);
        CollectionPropertyContainer<CandidateCV> candidateCvContainer =
                screenData.getContainer(CANDIDATE_CV_CONTAINER_ID);
        if (tabSheet == null || candidateCvContainer == null) {
            return;
        }

        AtomicBoolean projectLogosLoaded = new AtomicBoolean(false);
        Runnable hydrateLogos = () -> hydrateResumeProjectLogosOnce(
                candidateCvContainer,
                dataContext,
                projectLogosLoaded);

        candidateCvContainer.addCollectionChangeListener(event -> {
            projectLogosLoaded.set(false);
            if (isResumeTabSelected(tabSheet)) {
                hydrateLogos.run();
            }
        });

        tabSheet.addSelectedTabChangeListener(event -> {
            if (isResumeTabSelected(tabSheet)) {
                hydrateLogos.run();
            }
        });
    }

    /**
     * После ленивой загрузки контактов или социальных сетей одним запросом
     * догружает типы сетей и их логотипы, не расширяя initial view кандидата.
     */
    @SuppressWarnings("unchecked")
    private void installSocialNetworkLogoHydration(JobCandidateEdit screen,
                                                   ScreenData screenData,
                                                   DataContext dataContext) {
        TabSheet tabSheet = (TabSheet) screen.getWindow().getComponent(TAB_SHEET_ID);
        CollectionPropertyContainer<SocialNetworkURLs> socialNetworkContainer =
                screenData.getContainer(SOCIAL_NETWORK_CONTAINER_ID);
        if (tabSheet == null || socialNetworkContainer == null) {
            return;
        }

        AtomicBoolean socialNetworkLogosLoaded = new AtomicBoolean(false);
        AtomicBoolean initialHydrationCompleted = new AtomicBoolean(false);
        Runnable hydrateLogos = () -> {
            hydrateSocialNetworkTypeLogosOnce(
                    socialNetworkContainer,
                    dataContext,
                    socialNetworkLogosLoaded);
            initialHydrationCompleted.set(true);
        };

        socialNetworkContainer.addCollectionChangeListener(event -> {
            // Во время первичного заполнения контейнера не запускает запрос на каждую строку.
            if (!initialHydrationCompleted.get()) {
                return;
            }
            socialNetworkLogosLoaded.set(false);
            if (isSocialNetworkTabSelected(tabSheet)) {
                hydrateLogos.run();
            }
        });

        tabSheet.addSelectedTabChangeListener(event -> {
            if (isSocialNetworkTabSelected(tabSheet)) {
                hydrateLogos.run();
            }
        });
    }

    /** Проверяет, открыта ли вкладка резюме. */
    private boolean isResumeTabSelected(TabSheet tabSheet) {
        TabSheet.Tab selectedTab = tabSheet.getSelectedTab();
        return selectedTab != null && RESUME_TAB_ID.equals(selectedTab.getName());
    }

    /** Проверяет, открыта ли вкладка контактов или социальных сетей. */
    private boolean isSocialNetworkTabSelected(TabSheet tabSheet) {
        TabSheet.Tab selectedTab = tabSheet.getSelectedTab();
        if (selectedTab == null) {
            return false;
        }
        String selectedTabName = selectedTab.getName();
        return CONTACT_TAB_ID.equals(selectedTabName)
                || SOCIAL_NETWORK_TAB_ID.equals(selectedTabName);
    }

    /**
     * Однократно загружает проекты по уже известным UUID и merge-ит их в
     * экранный DataContext, не изменяя связи CandidateCV.
     */
    private void hydrateResumeProjectLogosOnce(CollectionPropertyContainer<CandidateCV> candidateCvContainer,
                                                DataContext dataContext,
                                                AtomicBoolean projectLogosLoaded) {
        if (!projectLogosLoaded.compareAndSet(false, true)) {
            return;
        }

        try {
            Set<UUID> projectIds = collectProjectIds(candidateCvContainer.getItems());
            if (projectIds.isEmpty()) {
                return;
            }

            List<Project> projects = dataManager.load(Project.class)
                    .query(QUERY_PROJECTS_WITH_LOGOS)
                    .parameter("projectIds", projectIds)
                    .view(PROJECT_WITH_LOGO_VIEW)
                    .list();

            // Merge гидратирует недостающие поля managed-проектов без изменения коллекции CV.
            projects.forEach(dataContext::merge);
        } catch (RuntimeException exception) {
            projectLogosLoaded.set(false);
            throw exception;
        }
    }

    /**
     * Однократно загружает типы социальных сетей по UUID и merge-ит логотипы
     * в DataContext без изменения пользовательских ссылок SocialNetworkURLs.
     */
    private void hydrateSocialNetworkTypeLogosOnce(
            CollectionPropertyContainer<SocialNetworkURLs> socialNetworkContainer,
            DataContext dataContext,
            AtomicBoolean socialNetworkLogosLoaded) {
        if (!socialNetworkLogosLoaded.compareAndSet(false, true)) {
            return;
        }

        try {
            Set<UUID> typeIds = collectSocialNetworkTypeIds(socialNetworkContainer.getItems());
            if (typeIds.isEmpty()) {
                return;
            }

            List<SocialNetworkType> socialNetworkTypes = dataManager.load(SocialNetworkType.class)
                    .query(QUERY_SOCIAL_NETWORK_TYPES_WITH_LOGOS)
                    .parameter("typeIds", typeIds)
                    .view(SOCIAL_NETWORK_TYPE_WITH_LOGO_VIEW)
                    .list();

            // Merge добавляет logo к managed-справочникам и не создаёт новых связей.
            socialNetworkTypes.forEach(dataContext::merge);
        } catch (RuntimeException exception) {
            socialNetworkLogosLoaded.set(false);
            throw exception;
        }
    }

    /**
     * Собирает уникальные UUID проектов только из уже загруженной цепочки
     * CandidateCV → OpenPosition → Project, не обращаясь к projectLogo.
     */
    Set<UUID> collectProjectIds(Collection<CandidateCV> candidateCvs) {
        if (candidateCvs == null || candidateCvs.isEmpty()) {
            return Collections.emptySet();
        }

        Set<UUID> projectIds = new LinkedHashSet<>();
        for (CandidateCV candidateCv : candidateCvs) {
            if (candidateCv != null
                    && candidateCv.getToVacancy() != null
                    && candidateCv.getToVacancy().getProjectName() != null
                    && candidateCv.getToVacancy().getProjectName().getId() != null) {
                projectIds.add(candidateCv.getToVacancy().getProjectName().getId());
            }
        }
        return projectIds;
    }

    /**
     * Собирает уникальные UUID типов только из уже загруженных SocialNetworkURLs,
     * не обращаясь к полю logo и не инициируя дополнительный fetch.
     */
    Set<UUID> collectSocialNetworkTypeIds(Collection<SocialNetworkURLs> socialNetworks) {
        if (socialNetworks == null || socialNetworks.isEmpty()) {
            return Collections.emptySet();
        }

        Set<UUID> typeIds = new LinkedHashSet<>();
        for (SocialNetworkURLs socialNetwork : socialNetworks) {
            if (socialNetwork != null
                    && socialNetwork.getSocialNetworkURL() != null
                    && socialNetwork.getSocialNetworkURL().getId() != null) {
                typeIds.add(socialNetwork.getSocialNetworkURL().getId());
            }
        }
        return typeIds;
    }
}