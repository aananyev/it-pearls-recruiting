package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Person;
import com.company.hunttech.entity.SkillTree;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.PopupButton;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.StandardOutcome;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.text.DecimalFormat;
import java.util.Set;

/**
 * Контроллер Split-View реестра открытых вакансий ({@code hunttech_OpenPositionReestr.browse}).
 *
 * <p>Реализует полновысотный левый сайдбар (312px) с карточками условий, кураторов, индикаторов
 * и навыков, а также тулбар быстрых фильтров и действий над вакансиями.</p>
 */
@UiController("hunttech_OpenPositionReestr.browse")
@UiDescriptor("open-position-reestr-browse.xml")
@LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionReestrBrowse extends StandardLookup<OpenPosition> {

    private static final DecimalFormat SALARY_FORMAT = new DecimalFormat("#,###");

    @Inject
    private Notifications notifications;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private com.haulmont.cuba.gui.UiComponents uiComponents;
    @Inject
    private com.haulmont.cuba.core.global.EntityStates entityStates;

    @Inject
    private GroupTable<OpenPosition> openPositionsTable;
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;

    // Кнопки тулбара и быстрых действий реестра
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
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Button suggestCandidatesBtn;
    @Inject
    private Button subscribeBtn;

    @Subscribe
    public void onInit(Screen.InitEvent event) {
        initTableColumns();
        initToolbarActions();
        initFilterPopupActions();
        initSidebarButtons();
    }

    private void initTableColumns() {
        if (openPositionsTable == null) return;

        // Колонка 1: Логотип проекта/компании (36px oval)
        openPositionsTable.addGeneratedColumn("logo", position -> {
            WebOvaFallbackImage logoImg = uiComponents.create(WebOvaFallbackImage.class);
            logoImg.setWidth("36px");
            logoImg.setHeight("36px");
            logoImg.setOvalWidth("36px");
            logoImg.setOvalHeight("36px");
            logoImg.setFallbackThemePath("icons/briefcase.png");
            logoImg.setScaleMode(com.haulmont.cuba.gui.components.Image.ScaleMode.SCALE_DOWN);
            FileDescriptor logo = null;
            try {
                if (position.getProjectName() != null && entityStates.isLoaded(position.getProjectName(), "projectLogo")) {
                    logo = position.getProjectName().getProjectLogo();
                }
            } catch (Exception ignored) {
            }
            FileDescriptorImageHelper.setImageSource(logoImg, fileLoader, logo, "icons/briefcase.png");
            return logoImg;
        });

        // Колонка 2: Название вакансии (с подзаголовком ID / Опыт)
        openPositionsTable.addGeneratedColumn("vacansyName", position -> {
            String vName = position.getVacansyName() != null ? position.getVacansyName() : "Без названия";
            String sub = position.getVacansyID() != null ? "ID: " + position.getVacansyID() : "";
            if (position.getWorkExperience() != null) {
                sub += (!sub.isEmpty() ? " • " : "") + "Опыт: " + position.getWorkExperience() + " г.";
            }
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidth("100%");
            lbl.setAlignment(com.haulmont.cuba.gui.components.Component.Alignment.MIDDLE_LEFT);
            lbl.setValue("<div style='text-align: left;'><div style='font-weight: 600; color: #2c3e50; font-size: 13px;'>" + vName + "</div>" +
                    (!sub.isEmpty() ? "<div style='font-size: 11px; color: #7f8c8d;'>" + sub + "</div>" : "") + "</div>");
            return lbl;
        });

        // Колонка 3: Проект и компания
        openPositionsTable.addGeneratedColumn("projectName", position -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String pName = "—";
            String comp = "—";
            try {
                if (position.getProjectName() != null) {
                    pName = position.getProjectName().getProjectName() != null ? position.getProjectName().getProjectName() : "—";
                    if (position.getProjectName().getProjectDepartment() != null
                            && position.getProjectName().getProjectDepartment().getCompanyName() != null) {
                        comp = position.getProjectName().getProjectDepartment().getCompanyName().getComanyName();
                    }
                }
            } catch (Exception ignored) {
            }
            lbl.setValue("<div style='text-align: left;'><div style='font-size: 12px; color: #34495e; font-weight: 500;'>" + pName + "</div>" +
                    (comp != null && !comp.equals("—") ? "<div style='font-size: 10.5px; color: #94a3b8;'>" + comp + "</div>" : "") + "</div>");
            return lbl;
        });

        // Колонка 4: Специализация
        openPositionsTable.addGeneratedColumn("positionType", position -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String pos = "Специалист";
            try {
                if (position.getPositionType() != null && position.getPositionType().getPositionRuName() != null) {
                    pos = position.getPositionType().getPositionRuName();
                }
            } catch (Exception ignored) {
            }
            lbl.setValue("<span style='background: rgba(43, 130, 201, 0.12); color: #2b82c9; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; display: inline-block;'>" + pos + "</span>");
            return lbl;
        });

        // Колонка 5: Зарплата
        openPositionsTable.addGeneratedColumn("salary", position -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            if (position.getSalaryMin() != null || position.getSalaryMax() != null) {
                StringBuilder sb = new StringBuilder();
                if (position.getSalaryMin() != null) {
                    sb.append(SALARY_FORMAT.format(position.getSalaryMin())).append(" ₽");
                }
                if (position.getSalaryMax() != null) {
                    if (sb.length() > 0) sb.append(" — ");
                    sb.append(SALARY_FORMAT.format(position.getSalaryMax())).append(" ₽");
                }
                lbl.setValue("<span style='font-size: 12px; font-weight: 600; color: #1e3a8a;'>" + sb + "</span>");
            } else {
                lbl.setValue("<span style='font-size: 11.5px; color: #94a3b8;'>Договорная</span>");
            }
            return lbl;
        });

        // Колонка 6: Требуемые навыки (чипы)
        openPositionsTable.addGeneratedColumn("mainSkills", position -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            try {
                if (entityStates.isLoaded(position, "skillsList") && position.getSkillsList() != null && !position.getSkillsList().isEmpty()) {
                    StringBuilder sb = new StringBuilder("<div style='display: flex; gap: 4px; flex-wrap: wrap;'>");
                    String[] palette = new String[]{"#38bdf8", "#4ade80", "#c084fc", "#fb923c", "#2dd4bf", "#f472b6", "#facc15", "#60a5fa"};
                    int count = 0;
                    for (SkillTree st : position.getSkillsList()) {
                        if (st != null && st.getSkillName() != null && !st.getSkillName().trim().isEmpty()) {
                            if (count >= 3) {
                                sb.append("<span style='font-size: 10px; color: #7f8c8d; align-self: center;'>+").append(position.getSkillsList().size() - 3).append("</span>");
                                break;
                            }
                            String sName = st.getSkillName().trim();
                            String color = palette[Math.abs(sName.hashCode()) % palette.length];
                            sb.append(String.format("<span style='background: %s18; color: %s; border: 1px solid %s44; padding: 1px 6px; border-radius: 10px; font-size: 10.5px; font-weight: 600; white-space: nowrap;'>%s</span>",
                                    color, color, color, sName));
                            count++;
                        }
                    }
                    sb.append("</div>");
                    lbl.setValue(sb.toString());
                } else {
                    lbl.setValue("<span style='color: #a0aec0; font-size: 11px;'>—</span>");
                }
            } catch (Exception ex) {
                lbl.setValue("<span style='color: #a0aec0; font-size: 11px;'>—</span>");
            }
            return lbl;
        });

        // Колонка 7: Статус
        openPositionsTable.addGeneratedColumn("statusBadge", position -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            boolean closed = Boolean.TRUE.equals(position.getOpenClose());
            String bg = closed ? "rgba(239, 68, 68, 0.12)" : "rgba(34, 197, 94, 0.12)";
            String color = closed ? "#ef4444" : "#16a34a";
            String text = closed ? "Закрыта" : "Открыта";
            lbl.setValue(String.format(
                    "<span style='background: %s; color: %s; padding: 2px 8px; border-radius: 4px; font-size: 10.5px; font-weight: 600; white-space: nowrap; display: inline-block;'>%s</span>",
                    bg, color, text
            ));
            return lbl;
        });
    }

    @Subscribe
    public void onBeforeShow(Screen.BeforeShowEvent event) {
        openPositionsDl.setParameter("openClosePos", false);
        openPositionsDl.load();
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

    private void initSidebarButtons() {
        if (openEditCardBtn != null) {
            openEditCardBtn.addClickListener(e -> openSelectedForEdit());
        }
        if (suggestCandidatesBtn != null) {
            suggestCandidatesBtn.addClickListener(e -> {
                OpenPosition selected = openPositionsTable.getSingleSelected();
                if (selected != null) {
                    screenBuilders.screen(this)
                            .withScreenId("hunttech_Suggestjobcandidate")
                            .withOpenMode(OpenMode.NEW_TAB)
                            .build()
                            .show();
                }
            });
        }
        if (subscribeBtn != null) {
            subscribeBtn.addClickListener(e -> {
                OpenPosition selected = openPositionsTable.getSingleSelected();
                if (selected != null) {
                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption("Подписка")
                            .withDescription("Вы подписались на вакансию: " + (selected.getVacansyName() != null ? selected.getVacansyName() : ""))
                            .show();
                }
            });
        }
    }

    private void updateSidebarWithPosition(OpenPosition position) {
        if (detailVacancyName == null) return;

        if (position == null) {
            if (projectLogoPic != null) {
                FileDescriptorImageHelper.setImageSource(projectLogoPic, fileLoader, null, "icons/briefcase.png");
            }
            detailVacancyName.setValue("Выберите вакансию");
            detailProjectName.setValue("—");
            detailCompanyName.setValue("—");
            detailLocationAndFormat.setValue("—");
            detailSalary.setValue("—");
            detailExperience.setValue("—");
            detailRemoteWork.setValue("—");
            detailOpenClose.setValue("—");
            detailNumberPosition.setValue("—");
            detailProjectOwner.setValue("—");
            detailOwner.setValue("—");
            detailCreatedBy.setValue("—");
            detailIndicators.setValue("<span style='color: #9ca3af;'>Нет данных</span>");
            if (detailRating != null) detailRating.setValue("");
            detailSkills.setValue("<span style='color: #9ca3af;'>Навыки не указаны</span>");
            return;
        }

        // Логотип
        FileDescriptor logo = null;
        try {
            if (position.getProjectName() != null && entityStates.isLoaded(position.getProjectName(), "projectLogo")) {
                logo = position.getProjectName().getProjectLogo();
            }
        } catch (Exception ignored) {
        }
        if (projectLogoPic != null) {
            FileDescriptorImageHelper.setImageSource(projectLogoPic, fileLoader, logo, "icons/briefcase.png");
        }

        // Заголовки
        detailVacancyName.setValue(position.getVacansyName() != null ? position.getVacansyName() : "—");

        String prjName = "—";
        String compName = "—";
        try {
            if (position.getProjectName() != null) {
                prjName = position.getProjectName().getProjectName() != null
                        ? position.getProjectName().getProjectName() : "Без проекта";
                if (position.getProjectName().getProjectDepartment() != null
                        && position.getProjectName().getProjectDepartment().getCompanyName() != null) {
                    compName = position.getProjectName().getProjectDepartment().getCompanyName().getComanyName();
                }
            }
        } catch (Exception ignored) {
        }
        detailProjectName.setValue(prjName);
        detailCompanyName.setValue(compName != null ? compName : "—");

        // Локация и формат работы
        String cityStr = "Локация не указана";
        try {
            if (entityStates.isLoaded(position, "cities") && position.getCities() != null && !position.getCities().isEmpty()) {
                cityStr = position.getCities().iterator().next().getCityRuName();
            }
        } catch (Exception ignored) {
        }
        String remoteStr = formatRemoteWorkString(position.getRemoteWork());
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
        String pOwner = "—";
        try {
            if (position.getProjectName() != null && position.getProjectName().getProjectOwner() != null) {
                Person person = position.getProjectName().getProjectOwner();
                pOwner = (person.getFirstName() != null ? person.getFirstName() : "") + " " +
                         (person.getSecondName() != null ? person.getSecondName() : "");
                pOwner = pOwner.trim().isEmpty() ? "—" : pOwner.trim();
            }
        } catch (Exception ignored) {
        }
        detailProjectOwner.setValue(pOwner);
        detailOwner.setValue(position.getOwner() != null ? position.getOwner().getName() : "—");
        detailCreatedBy.setValue(position.getCreatedBy() != null ? position.getCreatedBy() : "—");

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
        try {
            if (entityStates.isLoaded(position, "skillsList") && position.getSkillsList() != null && !position.getSkillsList().isEmpty()) {
                StringBuilder sk = new StringBuilder("<div style='display: flex; flex-wrap: wrap; gap: 4px; padding: 2px 0;'>");
                String[] palette = new String[]{"#38bdf8", "#4ade80", "#c084fc", "#fb923c", "#2dd4bf", "#f472b6", "#facc15", "#60a5fa"};
                for (SkillTree st : position.getSkillsList()) {
                    if (st != null && st.getSkillName() != null && !st.getSkillName().trim().isEmpty()) {
                        String skillName = st.getSkillName().trim();
                        String color = palette[Math.abs(skillName.hashCode()) % palette.length];
                        sk.append(String.format(
                                "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                                "padding: 2px 7px; border-radius: 12px; font-size: 11px; font-weight: 600; " +
                                "white-space: nowrap; display: inline-block;'>%s</span>",
                                color, color, color, skillName
                        ));
                    }
                }
                sk.append("</div>");
                detailSkills.setValue(sk.toString());
            } else {
                detailSkills.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
            }
        } catch (Exception ex) {
            detailSkills.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
        }
    }

    private String formatRemoteWorkString(Integer remoteWork) {
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
                SmartOpenPositionUploadScreen screen = screenBuilders.screen(this)
                        .withScreenClass(SmartOpenPositionUploadScreen.class)
                        .withOpenMode(OpenMode.DIALOG)
                        .build();
                screen.addAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.COMMIT)) {
                        openPositionsDl.load();
                    }
                });
                screen.show();
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
