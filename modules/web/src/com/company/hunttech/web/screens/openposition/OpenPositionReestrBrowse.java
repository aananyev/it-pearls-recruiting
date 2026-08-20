package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.City;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Person;
import com.company.hunttech.entity.SkillTree;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.PopupButton;
import com.haulmont.cuba.gui.components.TreeDataGrid;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;

import javax.inject.Inject;
import java.text.DecimalFormat;
import java.util.Set;

/**
 * Контроллер Split-View реестра открытых вакансий ({@code hunttech_OpenPositionReestr.browse}).
 *
 * <p>Объединяет полную бизнес-логику работы с открытыми позициями (генераторы строк,
 * светофор приоритетов, подписки, печатные формы, рейтинги) с современным двухпанельным
 * Split-View интерфейсом и профильным сайдбаром (312px) в концепции JobCandidateReestr.</p>
 */
@UiController("hunttech_OpenPositionReestr.browse")
@UiDescriptor("open-position-reestr-browse.xml")
@com.haulmont.cuba.gui.screen.LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionReestrBrowse extends OpenPositionBrowse {

    @Inject
    private DataManager dataManager;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private UserSession userSession;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Notifications notifications;

    @Inject
    private TreeDataGrid<OpenPosition> openPositionsTable;
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;

    // Элементы профильного сайдбара (312px)
    @Inject
    private WebOvaFallbackImage projectLogoPic;
    @Inject
    private Label<String> detailVacancyName;
    @Inject
    private Label<String> detailProjectName;
    @Inject
    private Label<String> detailCompanyName;
    @Inject
    private Label<String> detailLocationAndFormat;
    @Inject
    private Label<String> detailSalary;
    @Inject
    private Label<String> detailExperience;
    @Inject
    private Label<String> detailRemoteWork;
    @Inject
    private Label<String> detailOpenClose;
    @Inject
    private Label<String> detailNumberPosition;
    @Inject
    private Label<String> detailProjectOwner;
    @Inject
    private Label<String> detailOwner;
    @Inject
    private Label<String> detailCreatedBy;
    @Inject
    private Label<String> detailIndicators;
    @Inject
    private Label<String> detailRating;
    @Inject
    private Label<String> detailSkills;

    // Кнопки тулбара и быстрых действий
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Button suggestCandidatesBtn;
    @Inject
    private Button subscribeBtn;
    @Inject
    private Button createPositionBtn;
    @Inject
    private Button smartUploadBtn;
    @Inject
    private Button editPositionToolbarBtn;
    @Inject
    private Button removePositionToolbarBtn;

    @Inject
    private PopupButton vacanciesFilterPopupButton;
    @Inject
    private PopupButton priorityFilterPopupButton;
    @Inject
    private PopupButton actionsWithPositionButton;

    private static final DecimalFormat SALARY_FORMAT = new DecimalFormat("#,###");

    @Subscribe
    public void onInit(Screen.InitEvent event) {
        initToolbarActions();
        initFilterPopupActions();
    }

    @Subscribe
    public void onAfterShow(Screen.AfterShowEvent event) {
        initTableSelectionListener();
        updateSidebarWithPosition(null);
    }

    private void initTableSelectionListener() {
        if (openPositionsTable != null) {
            openPositionsTable.addSelectionListener(e -> {
                Set<OpenPosition> selected = e.getSelected();
                OpenPosition position = selected.isEmpty() ? null : selected.iterator().next();
                updateSidebarWithPosition(position);
                updateToolbarButtonsState(position != null);
            });
        }
    }

    private void updateToolbarButtonsState(boolean hasSelection) {
        if (editPositionToolbarBtn != null) {
            editPositionToolbarBtn.setEnabled(hasSelection);
        }
        if (removePositionToolbarBtn != null) {
            removePositionToolbarBtn.setEnabled(hasSelection);
        }
        if (openEditCardBtn != null) {
            openEditCardBtn.setEnabled(hasSelection);
        }
        if (suggestCandidatesBtn != null) {
            suggestCandidatesBtn.setEnabled(hasSelection);
        }
        if (subscribeBtn != null) {
            subscribeBtn.setEnabled(hasSelection);
        }
    }

    /**
     * Обновление данных левого профильного сайдбара выбранной вакансией.
     */
    private void updateSidebarWithPosition(OpenPosition position) {
        if (position == null) {
            detailVacancyName.setValue("Выберите вакансию");
            detailProjectName.setValue("-");
            detailCompanyName.setValue("-");
            detailLocationAndFormat.setValue("-");
            detailSalary.setValue("-");
            detailExperience.setValue("-");
            detailRemoteWork.setValue("-");
            detailOpenClose.setValue("-");
            detailNumberPosition.setValue("-");
            detailProjectOwner.setValue("-");
            detailOwner.setValue("-");
            detailCreatedBy.setValue("-");
            detailIndicators.setValue("<span style='color: #9ca3af;'>Нет выбранной вакансии</span>");
            detailRating.setValue("");
            detailSkills.setValue("<span style='color: #9ca3af;'>-</span>");
            FileDescriptorImageHelper.setImageSource(projectLogoPic, fileLoader, null, "icons/no-company.png");
            return;
        }

        // Логотип проекта или компании
        FileDescriptor logo = null;
        if (position.getProjectName() != null) {
            logo = position.getProjectName().getProjectLogo();
        }
        FileDescriptorImageHelper.setImageSource(projectLogoPic, fileLoader, logo, "icons/no-company.png");

        // Заголовки
        detailVacancyName.setValue(position.getVacansyName() != null ? position.getVacansyName() : "-");
        
        String prjName = position.getProjectName() != null && position.getProjectName().getProjectName() != null
                ? position.getProjectName().getProjectName() : "Без проекта";
        detailProjectName.setValue(prjName);

        String compName = "-";
        if (position.getProjectName() != null && position.getProjectName().getProjectDepartment() != null
                && position.getProjectName().getProjectDepartment().getCompanyName() != null) {
            compName = position.getProjectName().getProjectDepartment().getCompanyName().getComanyName();
        }
        detailCompanyName.setValue(compName != null ? compName : "-");

        // Локация и формат работы
        String cityStr = (position.getCities() != null && !position.getCities().isEmpty())
                ? position.getCities().iterator().next().getCityRuName() : "Локация не указана";
        String remoteStr = formatRemoteWork(position.getRemoteWork());
        detailLocationAndFormat.setValue(cityStr + (remoteStr.isEmpty() ? "" : " / " + remoteStr));

        // Зарплатная вилка
        if (position.getSalaryMin() != null || position.getSalaryMax() != null) {
            StringBuilder sb = new StringBuilder();
            if (position.getSalaryMin() != null) {
                sb.append(SALARY_FORMAT.format(position.getSalaryMin())).append(" ₽");
            }
            if (position.getSalaryMax() != null) {
                if (sb.length() > 0) sb.append(" — ");
                sb.append(SALARY_FORMAT.format(position.getSalaryMax())).append(" ₽");
            }
            detailSalary.setValue(sb.toString());
        } else {
            detailSalary.setValue("По договоренности");
        }

        // Опыт
        detailExperience.setValue(position.getWorkExperience() != null ? position.getWorkExperience().toString() + " лет" : "Не указан");
        detailRemoteWork.setValue(remoteStr.isEmpty() ? "Офис / Удаленно" : remoteStr);
        detailOpenClose.setValue(Boolean.TRUE.equals(position.getOpenClose()) ? "Закрыта" : "Открыта");
        detailNumberPosition.setValue(position.getNumberPosition() != null ? position.getNumberPosition() + " шт." : "1 шт.");

        // Куратор и автор
        String pOwner = "-";
        if (position.getProjectName() != null && position.getProjectName().getProjectOwner() != null) {
            Person person = position.getProjectName().getProjectOwner();
            pOwner = (person.getFirstName() != null ? person.getFirstName() : "") + " " +
                     (person.getSecondName() != null ? person.getSecondName() : "");
            pOwner = pOwner.trim().isEmpty() ? "-" : pOwner.trim();
        }
        detailProjectOwner.setValue(pOwner);
        detailOwner.setValue(position.getOwner() != null ? position.getOwner().getName() : "-");
        detailCreatedBy.setValue(position.getCreatedBy() != null ? position.getCreatedBy() : "-");

        // Индикаторы готовности
        StringBuilder ind = new StringBuilder();
        boolean hasDesc = position.getComment() != null && !position.getComment().trim().isEmpty();
        boolean hasExercise = position.getExercise() != null && !position.getExercise().trim().isEmpty();
        boolean hasTemplate = position.getTemplateLetter() != null && !position.getTemplateLetter().trim().isEmpty();

        ind.append("<div style='display: flex; gap: 8px; font-size: 11px;'>");
        ind.append(hasDesc ? "<span style='color: #16a34a;'>✓ Описание</span>" : "<span style='color: #9ca3af;'>✕ Описание</span>");
        ind.append(hasExercise ? "<span style='color: #16a34a;'>✓ Тестовое</span>" : "<span style='color: #9ca3af;'>✕ Тестовое</span>");
        ind.append(hasTemplate ? "<span style='color: #16a34a;'>✓ Памятка</span>" : "<span style='color: #9ca3af;'>✕ Памятка</span>");
        ind.append("</div>");
        detailIndicators.setValue(ind.toString());

        // Навыки
        if (position.getSkillsList() != null && !position.getSkillsList().isEmpty()) {
            StringBuilder sk = new StringBuilder("<div style='display: flex; flex-wrap: wrap; gap: 4px;'>");
            for (SkillTree st : position.getSkillsList()) {
                sk.append("<span style='background: #e2e8f0; color: #1e293b; padding: 2px 6px; border-radius: 4px; font-size: 11px;'>")
                  .append(st.getSkillName())
                  .append("</span>");
            }
            sk.append("</div>");
            detailSkills.setValue(sk.toString());
        } else {
            detailSkills.setValue("<span style='color: #9ca3af;'>Навыки не указаны</span>");
        }
    }

    private String formatRemoteWork(Integer remoteWork) {
        if (remoteWork == null) return "";
        switch (remoteWork) {
            case 0: return "В офисе";
            case 1: return "Удаленно";
            case 2: return "Гибрид 50/50";
            default: return "Удаленно";
        }
    }

    private void initToolbarActions() {
        if (createPositionBtn != null) {
            createPositionBtn.addClickListener(e -> {
                screenBuilders.editor(OpenPosition.class, this)
                        .newEntity()
                        .withOpenMode(OpenMode.NEW_TAB)
                        .show();
            });
        }
        if (smartUploadBtn != null) {
            smartUploadBtn.addClickListener(e -> {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Умная загрузка")
                        .withDescription("Мастер умного создания вакансии открывается...")
                        .show();
            });
        }
        if (editPositionToolbarBtn != null) {
            editPositionToolbarBtn.addClickListener(e -> openSelectedForEdit());
        }
        if (removePositionToolbarBtn != null) {
            removePositionToolbarBtn.addClickListener(e -> {
                Action removeAction = openPositionsTable.getAction("remove");
                if (removeAction != null) {
                    removeAction.actionPerform(openPositionsTable);
                }
            });
        }
        if (openEditCardBtn != null) {
            openEditCardBtn.addClickListener(e -> openSelectedForEdit());
        }
        if (suggestCandidatesBtn != null) {
            suggestCandidatesBtn.addClickListener(e -> {
                OpenPosition selected = openPositionsTable.getSingleSelected();
                if (selected != null) {
                    Action action = openPositionsTable.getAction("suggestJobCandidate");
                    if (action != null) {
                        action.actionPerform(openPositionsTable);
                    }
                }
            });
        }
        if (subscribeBtn != null) {
            subscribeBtn.addClickListener(e -> {
                OpenPosition selected = openPositionsTable.getSingleSelected();
                if (selected != null) {
                    Action action = openPositionsTable.getAction("subscribe");
                    if (action != null) {
                        action.actionPerform(openPositionsTable);
                    }
                }
            });
        }
    }

    private void openSelectedForEdit() {
        OpenPosition selected = openPositionsTable.getSingleSelected();
        if (selected != null) {
            screenBuilders.editor(OpenPosition.class, this)
                    .editEntity(selected)
                    .withOpenMode(OpenMode.NEW_TAB)
                    .show();
        }
    }

    private void initFilterPopupActions() {
        if (vacanciesFilterPopupButton != null) {
            vacanciesFilterPopupButton.addAction(new BaseAction("filterAll")
                    .withCaption("Все открытые вакансии")
                    .withIcon("COMPASS")
                    .withHandler(e -> {
                        openPositionsDl.removeParameter("openClosePos");
                        openPositionsDl.removeParameter("subscriber");
                        openPositionsDl.removeParameter("newOpenPosition");
                        openPositionsDl.setParameter("openClosePos", false);
                        openPositionsDl.load();
                        vacanciesFilterPopupButton.setCaption("Все открытые");
                    }));

            vacanciesFilterPopupButton.addAction(new BaseAction("filterMySubscriptions")
                    .withCaption("Мои подписки")
                    .withIcon("USER")
                    .withHandler(e -> {
                        openPositionsDl.removeParameter("newOpenPosition");
                        openPositionsDl.setParameter("subscriber", userSession.getUser());
                        openPositionsDl.setParameter("openClosePos", false);
                        openPositionsDl.load();
                        vacanciesFilterPopupButton.setCaption("Мои подписки");
                    }));

            vacanciesFilterPopupButton.addAction(new BaseAction("filterNew")
                    .withCaption("Новые (3 дня)")
                    .withIcon("CLOCK_O")
                    .withHandler(e -> {
                        openPositionsDl.removeParameter("subscriber");
                        openPositionsDl.setParameter("newOpenPosition", 3);
                        openPositionsDl.setParameter("openClosePos", false);
                        openPositionsDl.load();
                        vacanciesFilterPopupButton.setCaption("Новые (3 дня)");
                    }));

            vacanciesFilterPopupButton.addAction(new BaseAction("filterAllWithArchive")
                    .withCaption("Все, включая архивные")
                    .withIcon("ARCHIVE")
                    .withHandler(e -> {
                        openPositionsDl.removeParameter("openClosePos");
                        openPositionsDl.removeParameter("subscriber");
                        openPositionsDl.removeParameter("newOpenPosition");
                        openPositionsDl.load();
                        vacanciesFilterPopupButton.setCaption("Все (с архивом)");
                    }));
        }

        if (priorityFilterPopupButton != null) {
            priorityFilterPopupButton.addAction(new BaseAction("priorityAll")
                    .withCaption("Все приоритеты")
                    .withIcon("LIST")
                    .withHandler(e -> {
                        openPositionsDl.removeParameter("priority");
                        openPositionsDl.load();
                        priorityFilterPopupButton.setCaption("Приоритет");
                    }));

            priorityFilterPopupButton.addAction(new BaseAction("priorityHigh")
                    .withCaption("Высокий приоритет")
                    .withIcon("CIRCLE")
                    .withHandler(e -> {
                        openPositionsDl.setParameter("priority", 1);
                        openPositionsDl.load();
                        priorityFilterPopupButton.setCaption("Высокий");
                    }));

            priorityFilterPopupButton.addAction(new BaseAction("priorityNormal")
                    .withCaption("Обычный приоритет")
                    .withIcon("CIRCLE_O")
                    .withHandler(e -> {
                        openPositionsDl.setParameter("priority", 2);
                        openPositionsDl.load();
                        priorityFilterPopupButton.setCaption("Обычный");
                    }));
        }
    }
}
