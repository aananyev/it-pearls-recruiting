package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.entity.CandidateSkillPriority;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.OpenPositionSkill;
import com.company.hunttech.entity.Person;
import com.company.hunttech.entity.Project;
import com.company.hunttech.entity.SkillTree;
import com.company.hunttech.service.SkillAnalysisResult;
import com.company.hunttech.service.SkillAnalysisService;
import com.company.hunttech.web.util.AiOperationNotifier;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.EntityStates;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.PopupButton;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.StandardOutcome;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.Target;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Контроллер Split-View реестра открытых вакансий ({@code hunttech_OpenPositionReestr.browse}).
 *
 * <p>Реализует полновысотный левый сайдбар (312px) с карточками условий, кураторов, индикаторов
 * и навыков (сгруппированных по 3 категориям), тулбар быстрых фильтров и действий, а также
 * AI-сканирование 3 уровней навыков (основные, вторичные, прочие) с актуализацией в БД.</p>
 */
@UiController("hunttech_OpenPositionReestr.browse")
@UiDescriptor("open-position-reestr-browse.xml")
@LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionReestrBrowse extends StandardLookup<OpenPosition> {
    private static final Logger log = LoggerFactory.getLogger(OpenPositionReestrBrowse.class);

    private static final DecimalFormat SALARY_FORMAT = new DecimalFormat("#,###");

    @Inject
    private Notifications notifications;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private UserSession userSession;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private EntityStates entityStates;
    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private SkillAnalysisService skillAnalysisService;
    @Inject
    private MessageBundle messageBundle;

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
    private Label<String> detailSalaryTk;
    @Inject
    private Label<String> detailSalaryIe;
    @Inject
    private Label<String> detailExperience;
    @Inject
    private Label<String> detailGrade;
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
    private VBoxLayout projectDescriptionCard;
    @Inject
    private Label<String> projectDescriptionCardTitle;
    @Inject
    private Label<String> detailProjectShortDescription;
    @Inject
    private Label<String> detailProjectDescription;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Button suggestCandidatesBtn;
    @Inject
    private Button subscribeBtn;

    /** Кэш требуемых навыков для всех видимых вакансий (Zero N+1) */
    private Map<UUID, List<OpenPositionSkill>> skillsByPositionId = Collections.emptyMap();
    /** Кэш среднего рейтинга для всех видимых вакансий (Zero N+1) */
    private Map<UUID, Double> avgRatingByPositionId = Collections.emptyMap();
    /** Кэш краткого описания и полного описания проекта для всех видимых вакансий (Zero N+1) */
    private Map<UUID, String> projectShortDescriptionCache = Collections.emptyMap();
    private Map<UUID, String> projectDescriptionCache = Collections.emptyMap();

    @Subscribe
    public void onInit(Screen.InitEvent event) {
        // Выравнивание заголовков сайдбара по центру
        detailVacancyName.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailProjectName.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailCompanyName.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailLocationAndFormat.setAlignment(Component.Alignment.MIDDLE_CENTER);

        initTableColumns();
        initToolbarActions();
        initFilterPopupActions();
        initSidebarButtons();
    }

    @Subscribe(id = "openPositionsDl", target = Target.DATA_LOADER)
    public void onOpenPositionsDlPostLoad(CollectionLoader.PostLoadEvent<OpenPosition> event) {
        List<OpenPosition> positions = event.getLoadedEntities();
        if (positions == null || positions.isEmpty()) {
            skillsByPositionId = Collections.emptyMap();
            avgRatingByPositionId = Collections.emptyMap();
            return;
        }

        try {
            List<OpenPositionSkill> allSkills = dataManager.load(OpenPositionSkill.class)
                    .query("select e from hunttech_OpenPositionSkill e where e.openPosition in :positions order by e.priority, e.skill.skillName")
                    .parameter("positions", positions)
                    .view("openPositionSkill-view")
                    .list();

            Map<UUID, List<OpenPositionSkill>> skillsMap = new HashMap<>();
            for (OpenPositionSkill ops : allSkills) {
                if (ops.getOpenPosition() != null) {
                    skillsMap.computeIfAbsent(ops.getOpenPosition().getId(), k -> new ArrayList<>()).add(ops);
                }
            }
            skillsByPositionId = skillsMap;
        } catch (Exception ignored) {
            skillsByPositionId = Collections.emptyMap();
        }

        try {
            Map<UUID, Double> ratingsMap = new HashMap<>();
            List<com.haulmont.cuba.core.entity.KeyValueEntity> rows = dataManager.loadValues(
                    "select e.openPosition.id as positionId, avg(e.rating + 1) as avgRating " +
                    "from hunttech_OpenPositionComment e " +
                    "where e.openPosition in :positions and e.rating is not null " +
                    "group by e.openPosition.id")
                    .parameter("positions", positions)
                    .properties("positionId", "avgRating")
                    .list();
            for (com.haulmont.cuba.core.entity.KeyValueEntity row : rows) {
                UUID posId = row.getValue("positionId");
                Number avg = row.getValue("avgRating");
                if (posId != null && avg != null) {
                    ratingsMap.put(posId, avg.doubleValue());
                }
            }
            avgRatingByPositionId = ratingsMap;
        } catch (Exception ignored) {
            avgRatingByPositionId = Collections.emptyMap();
        }

        // Кэш краткого описания и полного описания проекта (как в ProjectReestrBrowse)
        try {
            List<UUID> projectIds = positions.stream()
                    .map(OpenPosition::getProjectName)
                    .filter(Objects::nonNull)
                    .map(Project::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (!projectIds.isEmpty()) {
                List<com.haulmont.cuba.core.entity.KeyValueEntity> descRows = dataManager.loadValues(
                        "select e.id, e.shortDescription, e.projectDescription from hunttech_Project e where e.id in :ids")
                        .parameter("ids", projectIds)
                        .properties("id", "shortDescription", "projectDescription")
                        .list();
                Map<UUID, String> shortDescCache = new HashMap<>();
                Map<UUID, String> descCache = new HashMap<>();
                for (com.haulmont.cuba.core.entity.KeyValueEntity row : descRows) {
                    UUID pid = row.getValue("id");
                    String shortDesc = row.getValue("shortDescription");
                    String fullDesc = row.getValue("projectDescription");
                    if (pid != null) {
                        if (shortDesc != null && !shortDesc.trim().isEmpty()) {
                            shortDescCache.put(pid, shortDesc);
                        }
                        if (fullDesc != null && !fullDesc.trim().isEmpty()) {
                            descCache.put(pid, fullDesc);
                        }
                    }
                }
                projectShortDescriptionCache = shortDescCache;
                projectDescriptionCache = descCache;
            } else {
                projectShortDescriptionCache = Collections.emptyMap();
                projectDescriptionCache = Collections.emptyMap();
            }
        } catch (Exception ignored) {
            projectShortDescriptionCache = Collections.emptyMap();
            projectDescriptionCache = Collections.emptyMap();
        }
    }

    private void initTableColumns() {
        if (openPositionsTable == null) return;

        // Колонка 1: Приоритет (векторный индикатор с подсветкой и подсказкой)
        openPositionsTable.addGeneratedColumn("priority", position -> renderPriorityBadge(position.getPriority()));

        // Колонка 2: Номер вакансии (ID)
        openPositionsTable.addGeneratedColumn("vacansyID", position -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidth("100%");
            lbl.setAlignment(Component.Alignment.MIDDLE_CENTER);
            String id = position.getVacansyID() != null ? position.getVacansyID() : "—";
            lbl.setValue("<span style='font-size: 12px; font-weight: 600; color: #2b82c9; font-family: monospace; white-space: normal; word-break: break-word;'>" + id + "</span>");
            return lbl;
        });

        // Колонка 3: Логотип проекта/компании (36px oval)
        openPositionsTable.addGeneratedColumn("logo", position -> {
            WebOvaFallbackImage logoImg = uiComponents.create(WebOvaFallbackImage.class);
            logoImg.setWidth("36px");
            logoImg.setHeight("36px");
            logoImg.setOvalWidth("36px");
            logoImg.setOvalHeight("36px");
            logoImg.setFallbackThemePath("icons/briefcase.png");
            logoImg.setScaleMode(Image.ScaleMode.SCALE_DOWN);
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

        // Колонка 3: Название вакансии (с подзаголовком ID / Опыт)
        openPositionsTable.addGeneratedColumn("vacansyName", position -> {
            String vName = position.getVacansyName() != null ? position.getVacansyName() : "Без названия";
            String sub = position.getVacansyID() != null ? "ID: " + position.getVacansyID() : "";
            if (position.getWorkExperience() != null) {
                sub += (!sub.isEmpty() ? " • " : "") + "Опыт: " + position.getWorkExperience() + " г.";
            }
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidth("100%");
            lbl.setAlignment(Component.Alignment.MIDDLE_LEFT);
            lbl.setValue("<div style='text-align: left;'><div style='font-weight: 600; color: #2c3e50; font-size: 13px; white-space: normal; word-break: break-word; line-height: 1.35;'>" + vName + "</div>" +
                    (!sub.isEmpty() ? "<div style='font-size: 11px; color: #7f8c8d; white-space: normal; word-break: break-word; line-height: 1.35;'>" + sub + "</div>" : "") + "</div>");
            return lbl;
        });

        // Колонка 4: Специализация
        openPositionsTable.addGeneratedColumn("positionType", position -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidthFull();
            String pos = "Специалист";
            try {
                if (position.getPositionType() != null && position.getPositionType().getPositionRuName() != null) {
                    pos = position.getPositionType().getPositionRuName();
                }
            } catch (Exception ignored) {
            }
            lbl.setValue("<span style='background: rgba(43, 130, 201, 0.12); color: #2b82c9; padding: 3px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; display: inline-block; white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + pos + "</span>");
            return lbl;
        });

        // Колонка 5: Зарплата (максимальная планка по ТК / по запросу кандидата / не определено)
        openPositionsTable.addGeneratedColumn("salary", position -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            if (position.getSalaryMax() != null) {
                lbl.setValue("<span style='font-size: 12px; font-weight: 600; color: #1e3a8a; white-space: normal; word-break: break-word;'>до "
                        + SALARY_FORMAT.format(position.getSalaryMax()) + " ₽</span>");
            } else if (Boolean.TRUE.equals(position.getSalaryCandidateRequest())) {
                lbl.setValue("<span style='font-size: 11px; color: #4b5563; white-space: normal; word-break: break-word;'>По запросу кандидата</span>");
            } else {
                lbl.setValue("<span style='font-size: 11px; color: #94a3b8; white-space: normal; word-break: break-word;'>Не определено</span>");
            }
            return lbl;
        });

        // Колонка 6: Требуемые навыки (чипы из пакетной карты OpenPositionSkill)
        openPositionsTable.addGeneratedColumn("mainSkills", position -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            try {
                List<OpenPositionSkill> skills = skillsByPositionId.getOrDefault(position.getId(), Collections.emptyList());
                if (!skills.isEmpty()) {
                    StringBuilder sb = new StringBuilder("<div style='display: flex; gap: 4px; flex-wrap: wrap;'>");
                    String[] palette = new String[]{"#38bdf8", "#4ade80", "#c084fc", "#fb923c", "#2dd4bf", "#f472b6", "#facc15", "#60a5fa"};
                    int count = 0;
                    for (OpenPositionSkill ops : skills) {
                        if (ops.getSkill() != null && ops.getSkill().getSkillName() != null) {
                            if (count >= 3) {
                                sb.append("<span style='font-size: 10px; color: #7f8c8d; align-self: center;'>+").append(skills.size() - 3).append("</span>");
                                break;
                            }
                            String sName = ops.getSkill().getSkillName().trim();
                            String color = palette[Math.abs(sName.hashCode()) % palette.length];
                            String priorityIcon = ops.getPriority() == CandidateSkillPriority.MAIN ? "★ " : "";
                            sb.append(String.format("<span style='background: %s18; color: %s; border: 1px solid %s44; padding: 1px 6px; border-radius: 10px; font-size: 10.5px; font-weight: 600; white-space: nowrap;'>%s%s</span>",
                                    color, color, color, priorityIcon, sName));
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
                    "<span style='background: %s; color: %s; padding: 2px 8px; border-radius: 4px; font-size: 10.5px; font-weight: 600; white-space: normal; word-break: break-word; display: inline-block;'>%s</span>",
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
            if (detailSalaryTk != null) detailSalaryTk.setValue("—");
            if (detailSalaryIe != null) detailSalaryIe.setValue("—");
            detailExperience.setValue("—");
            if (detailGrade != null) detailGrade.setValue("—");
            detailRemoteWork.setValue("—");
            detailOpenClose.setValue("—");
            detailNumberPosition.setValue("—");
            detailProjectOwner.setValue("—");
            detailOwner.setValue("—");
            detailCreatedBy.setValue("—");
            detailIndicators.setValue("<span style='color: #9ca3af;'>Нет данных</span>");
            if (detailRating != null) detailRating.setValue("");
            detailSkills.setValue("<span style='color: #9ca3af;'>Навыки не указаны</span>");
            if (projectDescriptionCard != null) projectDescriptionCard.setVisible(false);
            if (detailProjectShortDescription != null) detailProjectShortDescription.setVisible(false);
            if (detailProjectDescription != null) detailProjectDescription.setVisible(false);
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

        // Заголовки: должность в заголовке, проект в подзаголовке
        String posName = "Вакансия";
        try {
            if (position.getPositionType() != null && position.getPositionType().getPositionRuName() != null) {
                posName = position.getPositionType().getPositionRuName();
            } else if (position.getVacansyName() != null) {
                posName = position.getVacansyName();
            }
        } catch (Exception ignored) {
        }
        detailVacancyName.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%; text-align: center; font-weight: 600;'>" + posName + "</div>");

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
        detailProjectName.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%; text-align: center;'>" + prjName + "</div>");
        detailCompanyName.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%; text-align: center;'>" + (compName != null ? compName : "—") + "</div>");

        // Локация и формат работы
        String cityStr = "Локация не указана";
        try {
            if (entityStates.isLoaded(position, "cities") && position.getCities() != null && !position.getCities().isEmpty()) {
                cityStr = position.getCities().iterator().next().getCityRuName();
            }
        } catch (Exception ignored) {
        }
        String remoteStr = formatRemoteWorkString(position.getRemoteWork());
        detailLocationAndFormat.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%; text-align: center;'>" + cityStr + (remoteStr.isEmpty() ? "" : " / " + remoteStr) + "</div>");

        // Зарплата по ТК (подробная вилка)
        if (detailSalaryTk != null) {
            if (position.getSalaryMin() != null || position.getSalaryMax() != null) {
                StringBuilder sb = new StringBuilder();
                if (position.getSalaryMin() != null && position.getSalaryMax() != null) {
                    sb.append("от ").append(SALARY_FORMAT.format(position.getSalaryMin()))
                      .append(" до ").append(SALARY_FORMAT.format(position.getSalaryMax())).append(" ₽");
                } else if (position.getSalaryMax() != null) {
                    sb.append("до ").append(SALARY_FORMAT.format(position.getSalaryMax())).append(" ₽");
                } else {
                    sb.append("от ").append(SALARY_FORMAT.format(position.getSalaryMin())).append(" ₽");
                }
                detailSalaryTk.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + sb.toString() + "</div>");
            } else if (Boolean.TRUE.equals(position.getSalaryCandidateRequest())) {
                detailSalaryTk.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>По запросу кандидата</div>");
            } else {
                detailSalaryTk.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>Не определено</div>");
            }
        }

        // Ставка по ИП
        if (detailSalaryIe != null) {
            if (position.getSalaryIE() != null) {
                detailSalaryIe.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + SALARY_FORMAT.format(position.getSalaryIE()) + " ₽</div>");
            } else {
                detailSalaryIe.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>—</div>");
            }
        }

        // Опыт работы
        detailExperience.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (position.getWorkExperience() != null ? position.getWorkExperience().toString() + " г." : "Не указан") + "</div>");

        // Грейд
        String gradeName = "Не указан";
        try {
            if (position.getGrade() != null && position.getGrade().getGradeName() != null) {
                gradeName = position.getGrade().getGradeName();
            }
        } catch (Exception ignored) {
        }
        if (detailGrade != null) {
            detailGrade.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + gradeName + "</div>");
        }

        detailRemoteWork.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (remoteStr.isEmpty() ? "Офис / Удаленно" : remoteStr) + "</div>");
        detailOpenClose.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (Boolean.TRUE.equals(position.getOpenClose()) ? "Закрыта" : "Открыта") + "</div>");
        detailNumberPosition.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (position.getNumberPosition() != null ? position.getNumberPosition() + " шт." : "1 шт.") + "</div>");

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
        detailProjectOwner.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + pOwner + "</div>");
        detailOwner.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (position.getOwner() != null ? position.getOwner().getName() : "—") + "</div>");
        detailCreatedBy.setValue("<div style='white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + (position.getCreatedBy() != null ? position.getCreatedBy() : "—") + "</div>");

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

        // Рейтинг вакансии (звёздочки)
        try {
            Double avgRating = avgRatingByPositionId.get(position.getId());
            if (avgRating == null) {
                avgRating = dataManager.loadValue(
                        "select avg(e.rating + 1) from hunttech_OpenPositionComment e where e.openPosition.id = :openPositionId and e.rating is not null",
                        Double.class)
                        .parameter("openPositionId", position.getId())
                        .optional()
                        .orElse(null);
            }
            if (avgRating == null) {
                avgRating = dataManager.loadValue(
                        "select avg(e.rating + 1) from hunttech_IteractionList e where e.vacancy.id = :openPositionId and e.rating is not null",
                        Double.class)
                        .parameter("openPositionId", position.getId())
                        .optional()
                        .orElse(null);
            }

            if (avgRating != null && avgRating > 0) {
                int rounded = (int) Math.round(avgRating);
                if (rounded < 1) rounded = 1;
                if (rounded > 5) rounded = 5;
                StringBuilder sb = new StringBuilder("<div style='display: flex; align-items: center; gap: 3px; margin-top: 3px;'>");
                for (int i = 1; i <= 5; i++) {
                    if (i <= rounded) {
                        sb.append("<span style='color: #f59e0b; font-size: 20px;'>★</span>");
                    } else {
                        sb.append("<span style='color: #cbd5e1; font-size: 20px;'>★</span>");
                    }
                }
                sb.append(String.format("<span style='font-size: 14px; font-weight: 700; color: #334155; margin-left: 5px;'>%.1f</span>", avgRating));
                sb.append("</div>");
                detailRating.setValue(sb.toString());
            } else {
                detailRating.setValue("<span style='color: #9ca3af; font-size: 11px;'>Нет оценок вакансии</span>");
            }
        } catch (Exception ex) {
            detailRating.setValue("<span style='color: #9ca3af; font-size: 11px;'>Нет оценок вакансии</span>");
        }

        // Описание проекта (краткое / полное) — по алгоритму ProjectReestrBrowse
        if (projectDescriptionCard != null && projectDescriptionCardTitle != null
                && detailProjectShortDescription != null && detailProjectDescription != null) {
            String projectShortDesc = null;
            String projectFullDesc = null;
            try {
                if (position.getProjectName() != null && position.getProjectName().getId() != null) {
                    UUID projectId = position.getProjectName().getId();
                    projectShortDesc = projectShortDescriptionCache.get(projectId);
                    projectFullDesc = projectDescriptionCache.get(projectId);
                }
            } catch (Exception ignored) {
            }
            boolean hasShort = projectShortDesc != null && !projectShortDesc.trim().isEmpty();
            boolean hasFull = projectFullDesc != null && !projectFullDesc.trim().isEmpty();
            if (hasShort) {
                projectDescriptionCard.setVisible(true);
                projectDescriptionCardTitle.setValue("КРАТКОЕ ОПИСАНИЕ");
                detailProjectShortDescription.setVisible(true);
                detailProjectShortDescription.setValue(projectShortDesc);
                detailProjectDescription.setVisible(false);
            } else if (hasFull) {
                projectDescriptionCard.setVisible(true);
                projectDescriptionCardTitle.setValue("ОПИСАНИЕ ПРОЕКТА");
                detailProjectShortDescription.setVisible(false);
                detailProjectShortDescription.setValue("");
                detailProjectDescription.setVisible(true);
                String plain = Jsoup.parse(projectFullDesc).text();
                detailProjectDescription.setValue(plain.length() > 300 ? plain.substring(0, 300) + "..." : plain);
            } else {
                projectDescriptionCard.setVisible(false);
                detailProjectShortDescription.setVisible(false);
                detailProjectShortDescription.setValue("");
                detailProjectDescription.setVisible(false);
                detailProjectDescription.setValue("");
            }
        }

        // Навыки, сгруппированные по 3 категориям (Обязательные, Желательные, Прочие)
        try {
            List<OpenPositionSkill> skills = skillsByPositionId.getOrDefault(position.getId(), Collections.emptyList());
            if (skills.isEmpty()) {
                skills = dataManager.load(OpenPositionSkill.class)
                        .query("select e from hunttech_OpenPositionSkill e where e.openPosition = :openPosition order by e.priority, e.skill.skillName")
                        .parameter("openPosition", position)
                        .view("openPositionSkill-view")
                        .list();
            }

            if (!skills.isEmpty()) {
                List<OpenPositionSkill> mainSkills = new ArrayList<>();
                List<OpenPositionSkill> secondarySkills = new ArrayList<>();
                List<OpenPositionSkill> tertiarySkills = new ArrayList<>();

                for (OpenPositionSkill ops : skills) {
                    if (ops.getPriority() == CandidateSkillPriority.MAIN) {
                        mainSkills.add(ops);
                    } else if (ops.getPriority() == CandidateSkillPriority.SECONDARY) {
                        secondarySkills.add(ops);
                    } else {
                        tertiarySkills.add(ops);
                    }
                }

                StringBuilder sk = new StringBuilder("<div style='display: flex; flex-direction: column; gap: 6px; padding: 2px 0;'>");
                String[] palette = new String[]{"#38bdf8", "#4ade80", "#c084fc", "#fb923c", "#2dd4bf", "#f472b6", "#facc15", "#60a5fa"};

                if (!mainSkills.isEmpty()) {
                    sk.append("<div style='font-size: 10.5px; font-weight: 700; color: #16a34a; text-transform: uppercase; margin-top: 2px;'>Обязательные:</div>");
                    sk.append("<div style='display: flex; flex-wrap: wrap; gap: 4px;'>");
                    for (OpenPositionSkill ops : mainSkills) {
                        if (ops.getSkill() != null && ops.getSkill().getSkillName() != null) {
                            String sName = ops.getSkill().getSkillName().trim();
                            String color = palette[Math.abs(sName.hashCode()) % palette.length];
                            sk.append(String.format(
                                    "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                                    "padding: 2px 7px; border-radius: 12px; font-size: 11px; font-weight: 600; " +
                                    "white-space: nowrap; display: inline-block;'>★ %s</span>",
                                    color, color, color, sName
                            ));
                        }
                    }
                    sk.append("</div>");
                }

                if (!secondarySkills.isEmpty()) {
                    sk.append("<div style='font-size: 10.5px; font-weight: 700; color: #3b82f6; text-transform: uppercase; margin-top: 4px;'>Желательные:</div>");
                    sk.append("<div style='display: flex; flex-wrap: wrap; gap: 4px;'>");
                    for (OpenPositionSkill ops : secondarySkills) {
                        if (ops.getSkill() != null && ops.getSkill().getSkillName() != null) {
                            String sName = ops.getSkill().getSkillName().trim();
                            String color = palette[Math.abs(sName.hashCode()) % palette.length];
                            sk.append(String.format(
                                    "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                                    "padding: 2px 7px; border-radius: 12px; font-size: 11px; font-weight: 600; " +
                                    "white-space: nowrap; display: inline-block;'>%s</span>",
                                    color, color, color, sName
                            ));
                        }
                    }
                    sk.append("</div>");
                }

                if (!tertiarySkills.isEmpty()) {
                    sk.append("<div style='font-size: 10.5px; font-weight: 700; color: #64748b; text-transform: uppercase; margin-top: 4px;'>Прочие:</div>");
                    sk.append("<div style='display: flex; flex-wrap: wrap; gap: 4px;'>");
                    for (OpenPositionSkill ops : tertiarySkills) {
                        if (ops.getSkill() != null && ops.getSkill().getSkillName() != null) {
                            String sName = ops.getSkill().getSkillName().trim();
                            String color = palette[Math.abs(sName.hashCode()) % palette.length];
                            sk.append(String.format(
                                    "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                                    "padding: 2px 7px; border-radius: 12px; font-size: 11px; font-weight: 600; " +
                                    "white-space: nowrap; display: inline-block;'>%s</span>",
                                    color, color, color, sName
                            ));
                        }
                    }
                    sk.append("</div>");
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
                log.info("[SMART_VACANCY_OPENING_UI] Открытие диалога умной загрузки вакансий SmartOpenPositionUploadScreen");
                SmartOpenPositionUploadScreen screen = screenBuilders.screen(this)
                        .withScreenClass(SmartOpenPositionUploadScreen.class)
                        .withOpenMode(OpenMode.DIALOG)
                        .build();
                screen.addAfterCloseListener(closeEvent -> {
                    log.info("[SMART_VACANCY_OPENING_UI] Диалог умной загрузки закрыт с результатом: {}", closeEvent.getCloseAction());
                    if (closeEvent.closedWith(StandardOutcome.COMMIT)) {
                        log.info("[SMART_VACANCY_OPENING_UI] Обновление реестра вакансий (openPositionsDl.load()) после успешного открытия");
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

            priorityFilterPopupButton.addAction(new BaseAction("priorityUnderReview")
                    .withCaption("На проверку")
                    .withIcon("CLOCK")
                    .withHandler(e -> {
                        openPositionsDl.setParameter("priority", -2);
                        openPositionsDl.load();
                        priorityFilterPopupButton.setCaption("На проверку");
                    }));

            priorityFilterPopupButton.addAction(new BaseAction("priorityHigh")
                    .withCaption("Высокий приоритет")
                    .withIcon("CIRCLE")
                    .withHandler(e -> {
                        openPositionsDl.setParameter("priority", 3);
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

        if (actionsWithPositionButton != null) {
            actionsWithPositionButton.addAction(new BaseAction("refreshAction")
                    .withCaption("Обновить")
                    .withIcon("REFRESH")
                    .withHandler(e -> openPositionsDl.load()));

            actionsWithPositionButton.addAction(new BaseAction("openPositionAction")
                    .withCaption("Открыть карточку")
                    .withIcon("EDIT_ACTION")
                    .withHandler(e -> openSelectedForEdit()));

            actionsWithPositionButton.addAction(new BaseAction("scanSkillsAction")
                    .withCaption("Сканировать навыки")
                    .withIcon("font-icon:MAGIC")
                    .withHandler(e -> scanOpenPositionSkills()));

            actionsWithPositionButton.addAction(new BaseAction("suggestCandidatesAction")
                    .withCaption("Подобрать кандидатов")
                    .withIcon("font-icon:MAGIC")
                    .withHandler(e -> {
                        OpenPosition selected = openPositionsTable.getSingleSelected();
                        if (selected != null) {
                            screenBuilders.screen(this)
                                    .withScreenId("hunttech_Suggestjobcandidate")
                                    .withOpenMode(OpenMode.NEW_TAB)
                                    .build()
                                    .show();
                        }
                    }));
        }
    }

    /**
     * AI-анализ требований вакансии (основные, вторичные, прочие) с актуализацией в БД.
     */
    private void scanOpenPositionSkills() {
        OpenPosition position = openPositionsTable.getSingleSelected();
        if (position == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("ВНИМАНИЕ!")
                    .withDescription("Вакансия не выбрана. Для анализа навыков выберите вакансию в таблице.")
                    .show();
            return;
        }

        // Загружаем полный текст описания вакансии
        String rawText = position.getComment();
        if (rawText == null || rawText.trim().isEmpty()) {
            OpenPosition reloaded = dataManager.load(OpenPosition.class)
                    .id(position.getId())
                    .view(View.LOCAL)
                    .optional()
                    .orElse(null);
            if (reloaded != null) {
                rawText = reloaded.getComment() != null ? reloaded.getComment() : reloaded.getShortDescription();
            }
        }

        if (rawText == null || rawText.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Пустое описание")
                    .withDescription("Описание вакансии пусто. Заполните описание вакансии перед запуском анализа.")
                    .show();
            return;
        }

        String plainText = Jsoup.parse(rawText).text();
        if (plainText.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Пустое описание")
                    .withDescription("Описание вакансии не содержит читаемого текста для анализа.")
                    .show();
            return;
        }

        AiOperationNotifier.showStarted(notifications, "Запущен AI-анализ требований вакансии…", null);

        final OpenPosition positionForScan = position;
        final String textForScan = plainText;
        final Screen progressDialog = AiOperationNotifier.showProgress(this, "Анализ требований вакансии…");

        BackgroundTask<Integer, VacancySkillScanOutcome> task =
                new BackgroundTask<Integer, VacancySkillScanOutcome>(240, this) {
                    @Override
                    public VacancySkillScanOutcome run(TaskLifeCycle<Integer> taskLifeCycle) {
                        // 1. Загружаем текущие OpenPositionSkill вакансии
                        List<OpenPositionSkill> existingSkills = dataManager.load(OpenPositionSkill.class)
                                .query("select e from hunttech_OpenPositionSkill e where e.openPosition = :openPosition")
                                .parameter("openPosition", positionForScan)
                                .view("openPositionSkill-view")
                                .list();

                        Map<UUID, OpenPositionSkill> existingMap = new HashMap<>();
                        for (OpenPositionSkill ops : existingSkills) {
                            if (ops.getSkill() != null) {
                                existingMap.put(ops.getSkill().getId(), ops);
                            }
                        }

                        // 2. Сканируем 3 уровня требований через SkillAnalysisService
                        SkillAnalysisResult mainResult = skillAnalysisService.analyzeMain(textForScan);
                        SkillAnalysisResult secondaryResult = skillAnalysisService.analyzeSecondary(textForScan);
                        SkillAnalysisResult tertiaryResult = skillAnalysisService.analyzeTertiary(textForScan);

                        List<SkillTree> mainSkills = mainResult != null ? mainResult.getSkills() : null;
                        List<SkillTree> secondarySkills = secondaryResult != null ? secondaryResult.getSkills() : null;
                        List<SkillTree> tertiarySkills = tertiaryResult != null ? tertiaryResult.getSkills() : null;
                        SkillAnalysisResult allResult = null;

                        if (mainSkills == null) mainSkills = Collections.emptyList();
                        if (secondarySkills == null) secondarySkills = Collections.emptyList();
                        if (tertiarySkills == null) tertiarySkills = Collections.emptyList();

                        if (mainSkills.isEmpty() && secondarySkills.isEmpty() && tertiarySkills.isEmpty()) {
                            allResult = skillAnalysisService.analyzeAll(textForScan);
                            mainSkills = allResult != null && allResult.getSkills() != null ? allResult.getSkills() : Collections.emptyList();
                        }

                        SkillAnalysisResult aiSourceResult = firstNonNull(mainResult, secondaryResult, tertiaryResult, allResult);
                        AiExecutionResult aiExecution = aiSourceResult == null ? null : aiSourceResult.getAiExecution();

                        // 3. Формируем актуальный набор распознанных навыков по уровням без дублей
                        Map<UUID, CandidateSkillPriority> targetSkills = new LinkedHashMap<>();
                        Map<UUID, SkillTree> targetSkillEntities = new HashMap<>();

                        for (SkillTree st : mainSkills) {
                            if (st != null && !targetSkills.containsKey(st.getId())) {
                                targetSkills.put(st.getId(), CandidateSkillPriority.MAIN);
                                targetSkillEntities.put(st.getId(), st);
                            }
                        }
                        for (SkillTree st : secondarySkills) {
                            if (st != null && !targetSkills.containsKey(st.getId())) {
                                targetSkills.put(st.getId(), CandidateSkillPriority.SECONDARY);
                                targetSkillEntities.put(st.getId(), st);
                            }
                        }
                        for (SkillTree st : tertiarySkills) {
                            if (st != null && !targetSkills.containsKey(st.getId())) {
                                targetSkills.put(st.getId(), CandidateSkillPriority.TERTIARY);
                                targetSkillEntities.put(st.getId(), st);
                            }
                        }

                        // 4. Актуализация: определяем добавление, обновление и удаление лишних
                        List<Entity> toSave = new ArrayList<>();
                        List<Entity> toRemove = new ArrayList<>();
                        int addedCount = 0;
                        int updatedCount = 0;

                        // Находим те, что больше не присутствуют в требованиях -> удаляем
                        for (Map.Entry<UUID, OpenPositionSkill> entry : existingMap.entrySet()) {
                            if (!targetSkills.containsKey(entry.getKey())) {
                                toRemove.add(entry.getValue());
                            }
                        }

                        // Создаем новые или обновляем приоритеты
                        for (Map.Entry<UUID, CandidateSkillPriority> entry : targetSkills.entrySet()) {
                            UUID skillId = entry.getKey();
                            CandidateSkillPriority priority = entry.getValue();
                            OpenPositionSkill existing = existingMap.get(skillId);

                            if (existing == null) {
                                OpenPositionSkill newOps = metadata.create(OpenPositionSkill.class);
                                newOps.setOpenPosition(positionForScan);
                                newOps.setSkill(targetSkillEntities.get(skillId));
                                newOps.setPriority(priority);
                                toSave.add(newOps);
                                addedCount++;
                            } else {
                                if (existing.getPriority() != priority) {
                                    existing.setPriority(priority);
                                    toSave.add(existing);
                                    updatedCount++;
                                }
                            }
                        }

                        if (!toSave.isEmpty() || !toRemove.isEmpty()) {
                            CommitContext commitContext = new CommitContext(toSave, toRemove);
                            dataManager.commit(commitContext);
                        }

                        int totalDetected = targetSkills.size();
                        int removedCount = toRemove.size();

                        long mainCount = targetSkills.values().stream().filter(p -> p == CandidateSkillPriority.MAIN).count();
                        long secondaryCount = targetSkills.values().stream().filter(p -> p == CandidateSkillPriority.SECONDARY).count();
                        long tertiaryCount = targetSkills.values().stream().filter(p -> p == CandidateSkillPriority.TERTIARY).count();

                        String statsDescription = String.format(
                                "Всего актуализировано требований: <b>%d</b><br/>" +
                                "• Обязательных: <b>%d</b><br/>" +
                                "• Желательных: <b>%d</b><br/>" +
                                "• Прочих: <b>%d</b><br/>" +
                                "Добавлено новых: <b>%d</b>, обновлено: <b>%d</b>, удалено лишних: <b>%d</b>",
                                totalDetected, mainCount, secondaryCount, tertiaryCount,
                                addedCount, updatedCount, removedCount
                        );

                        return new VacancySkillScanOutcome(statsDescription, aiExecution);
                    }

                    @Override
                    public void done(VacancySkillScanOutcome outcome) {
                        AiOperationNotifier.closeProgress(progressDialog);
                        openPositionsDl.load();
                        OpenPosition current = openPositionsTable.getSingleSelected();
                        if (current != null) {
                            updateSidebarWithPosition(current);
                        }
                        AiOperationNotifier.show(
                                notifications,
                                outcome.aiExecution,
                                "AI-анализ требований вакансии завершён",
                                outcome.statsDescription
                        );
                    }
                };

        BackgroundTaskHandler<VacancySkillScanOutcome> handler =
                backgroundWorker.handle(task);
        handler.execute();
    }

    private static class VacancySkillScanOutcome {
        final String statsDescription;
        final AiExecutionResult aiExecution;

        VacancySkillScanOutcome(String statsDescription, AiExecutionResult aiExecution) {
            this.statsDescription = statsDescription;
            this.aiExecution = aiExecution;
        }
    }

    private Component renderPriorityBadge(Integer priority) {
        Label<String> lbl = uiComponents.create(Label.NAME);
        lbl.setHtmlEnabled(true);
        lbl.setDescriptionAsHtml(true);
        lbl.setAlignment(Component.Alignment.MIDDLE_CENTER);

        OpenPositionPriorityUiHelper.BadgeData badge = OpenPositionPriorityUiHelper.getPriorityBadge(priority, 22, messageBundle);
        lbl.setValue(badge.getHtml());
        lbl.setDescription(badge.getDescription());
        return lbl;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T val : values) {
            if (val != null) return val;
        }
        return null;
    }
}
