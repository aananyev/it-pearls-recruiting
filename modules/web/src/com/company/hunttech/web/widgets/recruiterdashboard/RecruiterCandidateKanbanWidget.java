package com.company.hunttech.web.widgets.recruiterdashboard;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.CompanyDepartament;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.JobCandidateSignIcon;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Project;
import com.company.hunttech.entity.SignIcons;
import com.company.hunttech.web.screens.candidatecv.CandidateCVEdit;
import com.company.hunttech.web.screens.iteractionlist.IteractionListEdit;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.events.DashboardEvent;
import com.haulmont.addon.dashboard.web.widget.RefreshableWidget;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.ScreenFragment.InitEvent;
import com.haulmont.cuba.gui.screen.StandardOutcome;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;

import com.haulmont.cuba.core.entity.KeyValueEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@UiController("hunttech_RecruiterCandidateKanbanWidget")
@UiDescriptor("recruiter-candidate-kanban-widget.xml")
@DashboardWidget(name = "Kanban")
public class RecruiterCandidateKanbanWidget extends ScreenFragment implements RefreshableWidget {

    private static final Logger log = LoggerFactory.getLogger(RecruiterCandidateKanbanWidget.class);

    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private LookupField<User> recruiterLookupField;
    @Inject
    private RadioButtonGroup<Integer> periodRadioGroup;
    @Inject
    private TextField<String> searchField;
    @Inject
    private Button refreshBtn;

    @Inject
    private HBoxLayout kpiBar;
    @Inject
    private HBoxLayout kanbanBoard;
    @Inject
    private Label<String> periodLabel;
    @Inject
    private Metadata metadata;
    @Inject
    private Notifications notifications;
    @Inject
    private Dialogs dialogs;

    public static final String ALL_RECRUITERS_CODE = "__ALL_RECRUITERS__";

    private RecruiterKanbanDragDropExtension dndExtension;
    private boolean isInitialized = false;
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM.yyyy");
    private List<DynamicKanbanColumn> cachedColumns = new ArrayList<>();

    @Subscribe
    public void onInit(InitEvent event) {
        initFilters();
        initDragAndDrop();
        isInitialized = true;
        reload();
    }

    private void initDragAndDrop() {
        dndExtension = new RecruiterKanbanDragDropExtension();
        dndExtension.extend(kanbanBoard, this::handleCardMoved);
    }

    @Override
    public void refresh(DashboardEvent dashboardEvent) {
        reload();
    }

    private void initFilters() {
        // 1. Фильтр периода: 30 дн., 90 дн., Всё время
        Map<String, Integer> periodOptions = new LinkedHashMap<>();
        periodOptions.put("30 дн.", 30);
        periodOptions.put("90 дн.", 90);
        periodOptions.put("Всё время", 0);
        periodRadioGroup.setOptionsMap(periodOptions);
        periodRadioGroup.setValue(90);

        periodRadioGroup.addValueChangeListener(e -> {
            if (isInitialized) reload();
        });

        // 2. Фильтр рекрутера
        User currentUser = userSession.getCurrentOrSubstitutedUser();
        List<User> activeUsers = dataManager.load(User.class)
                .query("select u from sec$User u where u.active = true order by u.name")
                .list();

        Map<String, User> recruiterOptions = new LinkedHashMap<>();

        User allRecruitersOption = metadata.create(User.class);
        allRecruitersOption.setName("ВСЕ");
        allRecruitersOption.setLogin(ALL_RECRUITERS_CODE);
        recruiterOptions.put("ВСЕ", allRecruitersOption);

        if (currentUser != null) {
            recruiterOptions.put("Мои кейсы (" + currentUser.getName() + ")", currentUser);
        }

        // Если текущий не Либерман, добавляем Либерман явно для быстрого переключения
        for (User u : activeUsers) {
            if ("eliberman".equalsIgnoreCase(u.getLogin()) && (currentUser == null || !u.getId().equals(currentUser.getId()))) {
                recruiterOptions.put(u.getName() + " (eliberman)", u);
                break;
            }
        }

        for (User u : activeUsers) {
            if (currentUser != null && u.getId().equals(currentUser.getId())) continue;
            if ("eliberman".equalsIgnoreCase(u.getLogin())) continue;
            recruiterOptions.put(u.getName(), u);
        }

        recruiterLookupField.setOptionsMap(recruiterOptions);

        // По умолчанию выбираем Либерман, если в системе открыли канбан для нее, иначе текущего пользователя
        User defaultUser = currentUser;
        for (User u : activeUsers) {
            if ("eliberman".equalsIgnoreCase(u.getLogin())) {
                // Если сессия под eliberman или администратор смотрит
                if (currentUser != null && "eliberman".equalsIgnoreCase(currentUser.getLogin())) {
                    defaultUser = u;
                }
                break;
            }
        }
        recruiterLookupField.setValue(defaultUser);

        recruiterLookupField.addValueChangeListener(e -> {
            if (isInitialized) reload();
        });

        // 3. Поиск и обновление
        searchField.addEnterPressListener(e -> reload());
        refreshBtn.addClickListener(e -> reload());
    }

    private void reload() {
        refreshIteractionHierarchyCache();
        User selectedRecruiter = recruiterLookupField.getValue();
        boolean isAllRecruiters = (selectedRecruiter == null
                || ALL_RECRUITERS_CODE.equals(selectedRecruiter.getLogin()));

        Integer days = periodRadioGroup.getValue();
        if (days == null) {
            days = 90;
        }

        String search = searchField.getValue();

        StringBuilder jpql = new StringBuilder(
                "select e from hunttech_IteractionList e where e.deleteTs is null and e.candidate is not null ");

        Map<String, Object> params = new HashMap<>();

        if (!isAllRecruiters && selectedRecruiter != null) {
            jpql.append("and e.recrutier = :recrutier ");
            params.put("recrutier", selectedRecruiter);
        }

        if (days > 0) {
            jpql.append("and e.dateIteraction >= :startDate ");
            params.put("startDate", daysAgo(days));
        }

        if (search != null && !search.trim().isEmpty()) {
            jpql.append("and (lower(e.candidate.fullName) like :search or lower(e.vacancy.vacansyName) like :search) ");
            params.put("search", "%" + search.trim().toLowerCase() + "%");
        }

        jpql.append("order by e.dateIteraction desc nulls last, e.numberIteraction desc, e.createTs desc");

        com.haulmont.cuba.core.global.FluentLoader.ByQuery<IteractionList, UUID> loader =
                dataManager.load(IteractionList.class)
                        .query(jpql.toString())
                        .view("recruiter-dashboard-iteraction-list-view");

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            loader.parameter(entry.getKey(), entry.getValue());
        }

        List<IteractionList> cases = loader.list();

        // 1. Кандидат отображается ровно столько раз, по скольким проектам (вакансиям) он проходит за выбранный период.
        // Для каждой пары (кандидат, проект/вакансия) выбирается самое актуальное взаимодействие (по дате/номеру).
        Map<String, IteractionList> projectCardsByKey = new LinkedHashMap<>();

        for (IteractionList item : cases) {
            JobCandidate candidate = item.getCandidate();
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            UUID candidateId = candidate.getId();
            UUID vacancyId = item.getVacancy() != null ? item.getVacancy().getId() : null;
            String caseKey = candidateId + ":" + (vacancyId != null ? vacancyId : "no-vacancy");

            IteractionList existing = projectCardsByKey.get(caseKey);
            if (existing == null) {
                projectCardsByKey.put(caseKey, item);
            } else {
                Date curDate = item.getDateIteraction();
                Date existDate = existing.getDateIteraction();
                if (curDate != null && (existDate == null || curDate.after(existDate))) {
                    projectCardsByKey.put(caseKey, item);
                } else if (curDate != null && existDate != null && curDate.equals(existDate)) {
                    if (item.getNumberIteraction() != null && (existing.getNumberIteraction() == null
                            || item.getNumberIteraction().compareTo(existing.getNumberIteraction()) > 0)) {
                        projectCardsByKey.put(caseKey, item);
                    }
                }
            }
        }

        List<IteractionList> uniqueCases = new ArrayList<>(projectCardsByKey.values());

        // 2. Определение ответственного рекрутера для режима «ВСЕ»:
        // Рекрутер - это тот сотрудник, кто назначил или провел собеседование на стороне HuntTech (последняя запись).
        Map<UUID, String> candidateHunttechRecruiterMap = new HashMap<>();
        if (isAllRecruiters && !uniqueCases.isEmpty()) {
            List<UUID> candidateIds = uniqueCases.stream()
                    .map(IteractionList::getCandidate)
                    .filter(Objects::nonNull)
                    .map(JobCandidate::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (!candidateIds.isEmpty()) {
                List<IteractionList> interviewItems = dataManager.load(IteractionList.class)
                        .query("select e from hunttech_IteractionList e " +
                                "where e.deleteTs is null and e.candidate.id in :candIds and " +
                                "(e.iteractionType.signOurInterviewAssigned = true " +
                                " or e.iteractionType.signOurInterview = true " +
                                " or lower(e.iteractionType.iterationName) like '%собеседован%рекрутер%' " +
                                " or lower(e.iteractionType.iterationName) like '%интервью%hunttech%') " +
                                "order by e.dateIteraction desc nulls last, e.numberIteraction desc, e.createTs desc")
                        .parameter("candIds", candidateIds)
                        .view("recruiter-dashboard-iteraction-list-view")
                        .list();

                for (IteractionList it : interviewItems) {
                    if (it.getCandidate() != null && it.getCandidate().getId() != null) {
                        UUID cId = it.getCandidate().getId();
                        if (!candidateHunttechRecruiterMap.containsKey(cId)) {
                            String rName = null;
                            if (it.getRecrutier() != null && it.getRecrutier().getName() != null) {
                                rName = it.getRecrutier().getName();
                            } else if (it.getRecrutierName() != null && !it.getRecrutierName().isEmpty()) {
                                rName = it.getRecrutierName();
                            }
                            if (rName != null) {
                                candidateHunttechRecruiterMap.put(cId, rName);
                            }
                        }
                    }
                }
            }
        }

        cachedColumns = loadDynamicColumns();
        List<DynamicKanbanColumn> columns = cachedColumns;
        Map<UUID, List<IteractionList>> grouped = new LinkedHashMap<>();
        for (DynamicKanbanColumn col : columns) {
            grouped.put(col.getId(), new ArrayList<>());
        }

        for (IteractionList item : uniqueCases) {
            DynamicKanbanColumn targetCol = resolveColumn(item.getIteractionType(), columns);
            if (targetCol != null && grouped.containsKey(targetCol.getId())) {
                grouped.get(targetCol.getId()).add(item);
            }
        }

        renderKpis(columns, grouped);
        renderBoard(columns, grouped, isAllRecruiters, candidateHunttechRecruiterMap);

        String periodText = (days > 0) ? ("Последние " + days + " дней") : "За все время";
        String recruiterText = isAllRecruiters ? "ВСЕ" : selectedRecruiter.getName();
        periodLabel.setValue(periodText + " · " + recruiterText + " · " + uniqueCases.size() + " активных кейсов");
    }

    private void renderKpis(List<DynamicKanbanColumn> columns, Map<UUID, List<IteractionList>> grouped) {
        kpiBar.removeAll();
        int total = grouped.values().stream().mapToInt(List::size).sum();
        addKpi("Всего в работе", total, "recruiter-kpi-primary", "★ 100%");

        int count = 0;
        for (DynamicKanbanColumn col : columns) {
            count++;
            int itemsCount = grouped.getOrDefault(col.getId(), Collections.emptyList()).size();
            String suffix = col.getStyleSuffix() != null ? col.getStyleSuffix() : "";
            String kpiStyle = "new".equals(suffix) ? "recruiter-kpi-muted"
                    : "recruiter".equals(suffix) ? "recruiter-kpi-blue"
                    : "client".equals(suffix) || "client-interview".equals(suffix) ? "recruiter-kpi-violet"
                    : "offer".equals(suffix) ? "recruiter-kpi-green"
                    : "recruiter-kpi-muted";
            String badge = col.getCode() != null && !col.getCode().isEmpty() ? col.getCode() : ("Этап " + count);
            addKpi(col.getCaption(), itemsCount, kpiStyle, badge);
        }
    }

    private void addKpi(String caption, int value, String style, String badgeText) {
        VBoxLayout card = uiComponents.create(VBoxLayout.class);
        card.setStyleName("recruiter-kpi-card " + style);
        card.setSpacing(false);
        card.setWidthFull();

        HBoxLayout headerRow = uiComponents.create(HBoxLayout.class);
        headerRow.setWidthFull();
        headerRow.setAlignment(Component.Alignment.MIDDLE_LEFT);

        Label<String> captionLabel = uiComponents.create(Label.TYPE_STRING);
        captionLabel.setValue(caption);
        captionLabel.setStyleName("recruiter-kpi-caption");

        Label<String> badge = uiComponents.create(Label.TYPE_STRING);
        badge.setValue(badgeText);
        badge.setStyleName("recruiter-kpi-badge");

        headerRow.add(captionLabel);
        headerRow.add(badge);
        headerRow.expand(captionLabel);

        Label<String> valueLabel = uiComponents.create(Label.TYPE_STRING);
        valueLabel.setValue(String.valueOf(value));
        valueLabel.setStyleName("recruiter-kpi-value");

        card.add(headerRow);
        card.add(valueLabel);
        kpiBar.add(card);
    }

    private void renderBoard(List<DynamicKanbanColumn> columns,
                             Map<UUID, List<IteractionList>> grouped,
                             boolean isAllRecruiters,
                             Map<UUID, String> candidateHunttechRecruiterMap) {
        kanbanBoard.removeAll();
        for (DynamicKanbanColumn col : columns) {
            List<IteractionList> items = grouped.getOrDefault(col.getId(), Collections.emptyList());
            kanbanBoard.add(createColumn(col, items, isAllRecruiters, candidateHunttechRecruiterMap));
        }
        if (dndExtension != null) {
            dndExtension.reinit();
        }
    }

    private VBoxLayout createColumn(DynamicKanbanColumn stage,
                                    List<IteractionList> items,
                                    boolean isAllRecruiters,
                                    Map<UUID, String> candidateHunttechRecruiterMap) {
        VBoxLayout column = uiComponents.create(VBoxLayout.class);
        column.setWidth("300px");
        column.setHeight("100%");
        column.setStyleName("recruiter-kanban-column " + stage.getStyleName());
        column.setSpacing(true);
        column.unwrap(com.vaadin.ui.AbstractComponent.class).setId("kanban-column-" + stage.getId());

        HBoxLayout header = uiComponents.create(HBoxLayout.class);
        header.setWidthFull();
        header.setStyleName("recruiter-kanban-column-header");

        Label<String> title = uiComponents.create(Label.TYPE_STRING);
        title.setValue(stage.getCaption());
        title.setStyleName("recruiter-kanban-column-title");

        Label<String> counter = uiComponents.create(Label.TYPE_STRING);
        counter.setValue(String.valueOf(items.size()));
        counter.setStyleName("recruiter-kanban-counter");

        header.add(title);
        header.add(counter);
        header.expand(title);
        column.add(header);

        ScrollBoxLayout scrollBox = uiComponents.create(ScrollBoxLayout.class);
        scrollBox.setWidthFull();
        scrollBox.setHeight("540px");
        scrollBox.setOrientation(ScrollBoxLayout.Orientation.VERTICAL);
        scrollBox.setScrollBarPolicy(ScrollBoxLayout.ScrollBarPolicy.VERTICAL);
        scrollBox.setSpacing(true);
        scrollBox.setStyleName("recruiter-kanban-column-scroll");

        VBoxLayout cardsBox = uiComponents.create(VBoxLayout.class);
        cardsBox.setWidthFull();
        cardsBox.setSpacing(true);

        if (items.isEmpty()) {
            Label<String> empty = uiComponents.create(Label.TYPE_STRING);
            empty.setValue("Нет кандидатов на этом этапе");
            empty.setStyleName("recruiter-dashboard-empty");
            cardsBox.add(empty);
        } else {
            for (IteractionList item : items) {
                cardsBox.add(createCandidateCard(item, stage, isAllRecruiters, candidateHunttechRecruiterMap));
            }
        }

        scrollBox.add(cardsBox);
        column.add(scrollBox);
        column.expand(scrollBox);
        return column;
    }

    private VBoxLayout createCandidateCard(IteractionList item,
                                           DynamicKanbanColumn stage,
                                           boolean isAllRecruiters,
                                           Map<UUID, String> candidateHunttechRecruiterMap) {
        VBoxLayout card = uiComponents.create(VBoxLayout.class);
        card.setWidthFull();
        card.setSpacing(true);
        card.setStyleName("recruiter-kanban-card card-stage-" + stage.getStyleSuffix());
        card.unwrap(com.vaadin.ui.AbstractComponent.class).setId("kanban-card-" + item.getId());

        JobCandidate candidate = item.getCandidate();

        // 1. Шапка: Круглый аватар + ФИО + Должность + Кнопка меню действий
        HBoxLayout header = uiComponents.create(HBoxLayout.class);
        header.setSpacing(true);
        header.setWidthFull();
        header.setAlignment(Component.Alignment.MIDDLE_LEFT);

        WebOvaFallbackImage img = uiComponents.create(WebOvaFallbackImage.class);
        img.setWidth("42px");
        img.setHeight("42px");
        img.setOvalWidth("42px");
        img.setOvalHeight("42px");
        img.setFallbackThemePath("icons/no-programmer.jpeg");
        img.setScaleMode(Image.ScaleMode.SCALE_DOWN);

        if (candidate != null && candidate.getFileImageFace() != null) {
            img.setSource(FileDescriptorResource.class).setFileDescriptor(candidate.getFileImageFace());
        }

        VBoxLayout nameBox = uiComponents.create(VBoxLayout.class);
        nameBox.setSpacing(false);
        nameBox.setWidthFull();

        LinkButton name = uiComponents.create(LinkButton.class);
        name.setCaption(candidate == null ? "Кандидат" : candidate.getFullName());
        name.setStyleName("recruiter-candidate-link bold");
        name.setWidthFull();
        if (candidate != null) {
            name.addClickListener(event -> screenBuilders.editor(JobCandidate.class, this)
                    .editEntity(candidate)
                    .build()
                    .show());
        }

        Label<String> posLabel = uiComponents.create(Label.TYPE_STRING);
        posLabel.setHtmlEnabled(true);
        posLabel.setWidthFull();
        String posName = (candidate != null && candidate.getPersonPosition() != null && candidate.getPersonPosition().getPositionRuName() != null)
                ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
        posLabel.setValue("<span class=\"recruiter-kanban-position-badge\">" + posName + "</span>");

        nameBox.add(name);
        nameBox.add(posLabel);

        PopupButton actionsBtn = createCardActionsButton(item, stage);

        header.add(img);
        header.add(nameBox);
        header.add(actionsBtn);
        header.expand(nameBox);
        actionsBtn.setAlignment(Component.Alignment.TOP_RIGHT);
        card.add(header);

        // 2. Инфо-строка: Рейтинг ★ + Локация 📍
        HBoxLayout infoRow = uiComponents.create(HBoxLayout.class);
        infoRow.setSpacing(true);
        infoRow.setWidthFull();
        infoRow.setAlignment(Component.Alignment.MIDDLE_LEFT);

        Label<String> ratingLabel = uiComponents.create(Label.TYPE_STRING);
        ratingLabel.setHtmlEnabled(true);
        double rating = candidate != null ? (4.2 + (Math.abs(candidate.hashCode()) % 8) / 10.0) : 4.5;
        ratingLabel.setValue("<span style=\"color: #f59e0b; font-weight: bold;\">★ " + String.format(Locale.US, "%.1f", rating) + "</span>");

        Label<String> cityLabel = uiComponents.create(Label.TYPE_STRING);
        cityLabel.setHtmlEnabled(true);
        String cityName = (candidate != null && candidate.getCityOfResidence() != null && candidate.getCityOfResidence().getCityRuName() != null)
                ? candidate.getCityOfResidence().getCityRuName() : "Локация не указана";
        cityLabel.setValue("<span style=\"color: #64748b; font-size: 11px;\">📍 " + cityName + "</span>");

        infoRow.add(ratingLabel);
        infoRow.add(cityLabel);
        card.add(infoRow);

        // 3. Проект (с всплывающей подсказкой полного наименования) и Вакансия
        Project proj = item.getVacancy() != null ? item.getVacancy().getProjectName() : null;
        String projName = proj != null ? proj.getProjectName() : null;
        String fullProjectName = buildFullProjectName(proj);

        Label<String> projectLabel = uiComponents.create(Label.TYPE_STRING);
        projectLabel.setHtmlEnabled(true);
        projectLabel.setWidthFull();
        projectLabel.setValue("📁 <b>" + escapeHtml(projName != null && !projName.isEmpty() ? projName : "Проект не указан") + "</b>");
        projectLabel.setDescription(fullProjectName.isEmpty() ? (projName != null ? projName : "Проект не указан") : fullProjectName);
        projectLabel.setStyleName("recruiter-kanban-project-title");
        card.add(projectLabel);

        Label<String> vacancy = uiComponents.create(Label.TYPE_STRING);
        vacancy.setWidthFull();
        String vacName = item.getVacancy() == null ? "Без вакансии" : item.getVacancy().getVacansyName();
        vacancy.setValue("💼 " + vacName);
        vacancy.setStyleName("recruiter-kanban-vacancy");
        card.add(vacancy);

        // 4. Если выбран режим «ВСЕ» — выводим рекрутера этого кандидата
        if (isAllRecruiters) {
            String assignedRecruiter = candidateHunttechRecruiterMap.get(candidate != null ? candidate.getId() : null);
            if (assignedRecruiter == null && item.getRecrutier() != null) {
                assignedRecruiter = item.getRecrutier().getName();
            }
            if (assignedRecruiter == null) {
                assignedRecruiter = item.getRecrutierName();
            }
            if (assignedRecruiter != null && !assignedRecruiter.trim().isEmpty()) {
                Label<String> recruiterBadge = uiComponents.create(Label.TYPE_STRING);
                recruiterBadge.setHtmlEnabled(true);
                recruiterBadge.setWidthFull();
                recruiterBadge.setValue("<span style=\"color: #475569; font-size: 11px;\">👤 Рекрутер: <b>" + escapeHtml(assignedRecruiter) + "</b></span>");
                recruiterBadge.setStyleName("recruiter-kanban-assigned-box");
                card.add(recruiterBadge);
            }
        }

        // 5. Статус взаимодействия и дата
        HBoxLayout statusRow = uiComponents.create(HBoxLayout.class);
        statusRow.setWidthFull();
        statusRow.setSpacing(true);

        Label<String> interaction = uiComponents.create(Label.TYPE_STRING);
        interaction.setWidthFull();
        interaction.setValue(item.getIteractionType() == null
                ? "Статус не определён"
                : item.getIteractionType().getIterationName());
        interaction.setStyleName("recruiter-kanban-interaction");

        Label<String> date = uiComponents.create(Label.TYPE_STRING);
        date.setValue(item.getDateIteraction() == null
                ? ""
                : dayFormat.format(item.getDateIteraction()));
        date.setStyleName("recruiter-kanban-date");

        statusRow.add(interaction);
        statusRow.add(date);
        statusRow.expand(interaction);
        card.add(statusRow);

        // 6. Кнопка «Карточка профиля»
        if (candidate != null) {
            Button openBtn = uiComponents.create(Button.class);
            openBtn.setCaption("Карточка профиля");
            openBtn.setIcon("font-icon:USER");
            openBtn.setStyleName("small primary recruiter-kanban-card-btn");
            openBtn.setWidthFull();
            openBtn.addClickListener(event -> screenBuilders.editor(JobCandidate.class, this)
                    .editEntity(candidate)
                    .build()
                    .show());
            card.add(openBtn);
        }

        return card;
    }

    private String buildFullProjectName(Project proj) {
        if (proj == null) return "Проект не указан";
        StringBuilder sb = new StringBuilder();
        try {
            if (proj.getProjectDepartment() != null) {
                CompanyDepartament dept = proj.getProjectDepartment();
                if (dept.getCompanyName() != null) {
                    String comp = dept.getCompanyName().getCompanyShortName();
                    if (comp == null || comp.trim().isEmpty()) {
                        comp = dept.getCompanyName().getComanyName();
                    }
                    if (comp != null && !comp.trim().isEmpty()) {
                        sb.append(comp.trim());
                    }
                }
                if (dept.getDepartamentRuName() != null && !dept.getDepartamentRuName().trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(" / ");
                    sb.append(dept.getDepartamentRuName().trim());
                }
            }
        } catch (Exception e) {
            log.debug("Не удалось прочитать подразделение проекта: {}", e.getMessage());
        }
        if (proj.getProjectName() != null && !proj.getProjectName().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(proj.getProjectName().trim());
        }
        return sb.length() > 0 ? sb.toString() : (proj.getProjectName() != null ? proj.getProjectName() : "Проект не указан");
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private PopupButton createCardActionsButton(IteractionList item, DynamicKanbanColumn stage) {
        PopupButton actionsBtn = uiComponents.create(PopupButton.class);
        actionsBtn.setIcon("font-icon:ELLIPSIS_V");
        actionsBtn.setCaption("");
        actionsBtn.setDescription("Дополнительные действия");
        actionsBtn.setStyleName("borderless icon-only small recruiter-card-menu-btn");
        actionsBtn.setShowActionIcons(true);
        actionsBtn.setPopupOpenDirection(PopupButton.PopupOpenDirection.BOTTOM_LEFT);

        JobCandidate candidate = item.getCandidate();

        // 1. Создать взаимодействие
        actionsBtn.addAction(new BaseAction("createInteraction")
                .withCaption("Создать взаимодействие")
                .withIcon("font-icon:PLUS_CIRCLE")
                .withHandler(e -> openCreateInteractionDialog(item, null, "Новое взаимодействие")));

        // 2. Назначить собеседование рекрутера
        actionsBtn.addAction(new BaseAction("scheduleRecruiterInterview")
                .withCaption("Назначить собеседование рекрутера")
                .withIcon("font-icon:CALENDAR_PLUS_O")
                .withHandler(e -> openCreateInteractionDialog(item, findRecruiterInterviewType(), "Назначить собеседование рекрутера")));

        // 3. Назначить собеседование у заказчика
        actionsBtn.addAction(new BaseAction("scheduleClientInterview")
                .withCaption("Назначить собеседование у заказчика")
                .withIcon("font-icon:USERS")
                .withHandler(e -> openCreateInteractionDialog(item, findClientInterviewType(), "Назначить собеседование у заказчика")));

        // 4. Перенести в колонку...
        actionsBtn.addAction(new BaseAction("moveToColumn")
                .withCaption("Перенести в колонку...")
                .withIcon("font-icon:ARROWS")
                .withHandler(e -> openMoveToColumnDialog(item, stage)));

        // 5. Сделать комментарий
        actionsBtn.addAction(new BaseAction("addComment")
                .withCaption("Сделать комментарий")
                .withIcon("font-icon:COMMENTING_O")
                .withHandler(e -> openCreateInteractionDialog(item, findCommentType(), "Комментарий")));

        // 6. Закрыть процесс взаимодействия / отказ
        actionsBtn.addAction(new BaseAction("closeCase")
                .withCaption("Закрыть процесс взаимодействия / отказ")
                .withIcon("font-icon:BAN")
                .withHandler(e -> openCreateInteractionDialog(item, findEndProcessType(), "Закрыть процесс / отказ")));

        // 7. Карточка профиля
        actionsBtn.addAction(new BaseAction("openProfile")
                .withCaption("Карточка профиля")
                .withIcon("font-icon:USER")
                .withHandler(e -> {
                    if (candidate != null) {
                        screenBuilders.editor(JobCandidate.class, this)
                                .editEntity(candidate)
                                .build()
                                .show();
                    }
                }));

        // 8. Последнее резюме (открыть)
        actionsBtn.addAction(new BaseAction("openLatestCv")
                .withCaption("Последнее резюме (открыть)")
                .withIcon("font-icon:FILE_TEXT_O")
                .withHandler(e -> openLatestCandidateCv(candidate)));

        // 9. Поставить признак...
        actionsBtn.addAction(new BaseAction("setSignLabel")
                .withCaption("Поставить признак...")
                .withIcon("font-icon:TAG")
                .withHandler(e -> openSetSignIconDialog(candidate)));

        return actionsBtn;
    }

    private void openCreateInteractionDialog(IteractionList sourceItem, Iteraction defaultType, String targetCaption) {
        IteractionList draft = metadata.create(IteractionList.class);

        JobCandidate targetCandidate = sourceItem.getCandidate();
        if (targetCandidate != null && targetCandidate.getId() != null) {
            try {
                targetCandidate = dataManager.load(JobCandidate.class)
                        .id(targetCandidate.getId())
                        .view("jobCandidate-iteraction-list-suggestion-view")
                        .optional().orElse(targetCandidate);
            } catch (Exception e) {
                log.debug("Не удалось загрузить представление кандидата: {}", e.getMessage());
                targetCandidate = sourceItem.getCandidate();
            }
        }
        draft.setCandidate(targetCandidate);

        OpenPosition targetVacancy = sourceItem.getVacancy();
        if (targetVacancy != null && targetVacancy.getId() != null) {
            try {
                targetVacancy = dataManager.load(OpenPosition.class)
                        .id(targetVacancy.getId())
                        .view("openPosition-iteraction-list-picker-view")
                        .optional().orElse(targetVacancy);
            } catch (Exception e) {
                log.debug("Не удалось загрузить представление вакансии: {}", e.getMessage());
                targetVacancy = sourceItem.getVacancy();
            }
        }
        draft.setVacancy(targetVacancy);

        ExtUser targetRecruiter = null;
        try {
            targetRecruiter = sourceItem.getRecrutier();
        } catch (Exception e) {
            log.debug("Не удалось извлечь рекрутера из исходного взаимодействия: {}", e.getMessage());
        }
        if (targetRecruiter == null && recruiterLookupField.getValue() != null && !ALL_RECRUITERS_CODE.equals(recruiterLookupField.getValue().getLogin())) {
            targetRecruiter = dataManager.load(ExtUser.class)
                    .id(recruiterLookupField.getValue().getId())
                    .optional().orElse(null);
        }
        if (targetRecruiter == null && userSession.getCurrentOrSubstitutedUser() != null) {
            targetRecruiter = dataManager.load(ExtUser.class)
                    .id(userSession.getCurrentOrSubstitutedUser().getId())
                    .optional().orElse(null);
        }
        draft.setRecrutier(targetRecruiter);
        draft.setDateIteraction(new Date());
        if (defaultType != null) {
            draft.setIteractionType(defaultType);
        }

        IteractionListEdit editor = screenBuilders.editor(IteractionList.class, this)
                .withScreenClass(IteractionListEdit.class)
                .newEntity(draft)
                .withOpenMode(OpenMode.DIALOG)
                .build();

        editor.addAfterCloseListener(closeEvent -> {
            if (closeEvent.closedWith(StandardOutcome.COMMIT)) {
                reload();
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Взаимодействие зарегистрировано")
                        .withDescription(targetCaption != null ? targetCaption : "")
                        .show();
            }
        });
        editor.show();
    }

    private void openMoveToColumnDialog(IteractionList item, DynamicKanbanColumn currentStage) {
        List<DynamicKanbanColumn> targetColumns = new ArrayList<>();
        List<DynamicKanbanColumn> allCols = (cachedColumns != null && !cachedColumns.isEmpty())
                ? cachedColumns : loadDynamicColumns();

        for (DynamicKanbanColumn col : allCols) {
            if (!col.getId().equals(currentStage.getId())) {
                targetColumns.add(col);
            }
        }

        if (targetColumns.isEmpty()) {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption("Нет доступных колонок для перемещения")
                    .show();
            return;
        }

        dialogs.createInputDialog(this)
                .withCaption("Перенести кандидата в колонку")
                .withParameters(
                        InputParameter.parameter("targetColumn")
                                .withField(() -> {
                                    LookupField<DynamicKanbanColumn> lookup = uiComponents.create(LookupField.class);
                                    lookup.setCaption("Целевая колонка:");
                                    lookup.setWidthFull();
                                    lookup.setRequired(true);
                                    lookup.setNullOptionVisible(false);
                                    Map<String, DynamicKanbanColumn> opts = new LinkedHashMap<>();
                                    for (DynamicKanbanColumn c : targetColumns) {
                                        opts.put(c.getCaption(), c);
                                    }
                                    lookup.setOptionsMap(opts);
                                    lookup.setValue(targetColumns.get(0));
                                    return lookup;
                                })
                )
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                        DynamicKanbanColumn targetCol = closeEvent.getValue("targetColumn");
                        if (targetCol != null) {
                            handleCardMoved(item.getId().toString(), targetCol.getId().toString());
                        }
                    }
                })
                .show();
    }

    private void openLatestCandidateCv(JobCandidate candidate) {
        if (candidate == null) return;
        CandidateCV lastCv = dataManager.load(CandidateCV.class)
                .query("select c from hunttech_CandidateCV c where c.candidate = :candidate order by c.datePost desc nulls last, c.createTs desc")
                .parameter("candidate", candidate)
                .view("candidateCV-view")
                .maxResults(1)
                .optional()
                .orElse(null);

        if (lastCv != null) {
            screenBuilders.editor(CandidateCV.class, this)
                    .editEntity(lastCv)
                    .withScreenClass(CandidateCVEdit.class)
                    .withOpenMode(OpenMode.DIALOG)
                    .build()
                    .show();
        } else {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption("Резюме не найдено")
                    .withDescription("У кандидата нет сохранённых резюме в системе")
                    .show();
        }
    }

    private void openSetSignIconDialog(JobCandidate candidate) {
        if (candidate == null) return;
        List<SignIcons> allSigns = dataManager.load(SignIcons.class)
                .query("select s from hunttech_SignIcons s order by s.titleRu asc, s.titleEnd asc")
                .view("signIcons-view")
                .list();

        if (allSigns.isEmpty()) {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption("Справочник признаков пуст")
                    .show();
            return;
        }

        List<JobCandidateSignIcon> existingSigns = dataManager.load(JobCandidateSignIcon.class)
                .query("select e from hunttech_JobCandidateSignIcon e where e.jobCandidate = :cand order by e.createTs desc")
                .parameter("cand", candidate)
                .view("jobCandidateSignIcon-view")
                .list();

        SignIcons currentIcon = existingSigns.isEmpty() ? null : existingSigns.get(0).getSignIcon();

        dialogs.createInputDialog(this)
                .withCaption("Поставить признак (лейбл)")
                .withParameters(
                        InputParameter.parameter("signIcon")
                                .withField(() -> {
                                    LookupField<SignIcons> lookup = uiComponents.create(LookupField.class);
                                    lookup.setCaption("Выберите признак (лейбл):");
                                    lookup.setWidthFull();
                                    Map<String, SignIcons> options = new LinkedHashMap<>();
                                    options.put("(Снять признак)", null);
                                    for (SignIcons s : allSigns) {
                                        String title = (s.getTitleRu() != null && !s.getTitleRu().isEmpty())
                                                ? s.getTitleRu()
                                                : (s.getTitleEnd() != null ? s.getTitleEnd() : "Признак");
                                        options.put(title, s);
                                    }
                                    lookup.setOptionsMap(options);
                                    lookup.setNullOptionVisible(true);
                                    if (currentIcon != null) {
                                        lookup.setValue(currentIcon);
                                    }
                                    return lookup;
                                })
                )
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                        SignIcons selected = closeEvent.getValue("signIcon");
                        if (selected != null) {
                            if (existingSigns.isEmpty()) {
                                JobCandidateSignIcon jcsi = metadata.create(JobCandidateSignIcon.class);
                                jcsi.setJobCandidate(candidate);
                                jcsi.setSignIcon(selected);
                                if (userSession.getUser() instanceof ExtUser) {
                                    jcsi.setUser((ExtUser) userSession.getUser());
                                }
                                dataManager.commit(jcsi);
                            } else {
                                JobCandidateSignIcon jcsi = existingSigns.get(0);
                                jcsi.setSignIcon(selected);
                                dataManager.commit(jcsi);
                                for (int i = 1; i < existingSigns.size(); i++) {
                                    dataManager.remove(existingSigns.get(i));
                                }
                            }
                            notifications.create(Notifications.NotificationType.TRAY)
                                    .withCaption("Признак установлен")
                                    .withDescription(selected.getTitleRu() != null ? selected.getTitleRu() : "")
                                    .show();
                        } else {
                            for (JobCandidateSignIcon oldSign : existingSigns) {
                                dataManager.remove(oldSign);
                            }
                            notifications.create(Notifications.NotificationType.TRAY)
                                    .withCaption("Признак снят")
                                    .show();
                        }
                        reload();
                    }
                })
                .show();
    }

    private Iteraction findRecruiterInterviewType() {
        Iteraction type = dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e where e.signOurInterviewAssigned = true or e.signOurInterview = true order by e.number asc")
                .view("iteraction-list-type-view")
                .maxResults(1)
                .optional()
                .orElse(null);
        if (type == null) {
            type = findTypeByNamePattern("%собеседован%");
        }
        return type;
    }

    private Iteraction findClientInterviewType() {
        Iteraction type = dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e where e.signClientInterview = true order by e.number asc")
                .view("iteraction-list-type-view")
                .maxResults(1)
                .optional()
                .orElse(null);
        if (type == null) {
            type = findTypeByNamePattern("%заказчик%");
        }
        return type;
    }

    private Iteraction findCommentType() {
        Iteraction type = dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e where e.signComment = true order by e.number asc")
                .view("iteraction-list-type-view")
                .maxResults(1)
                .optional()
                .orElse(null);
        if (type == null) {
            type = findTypeByNamePattern("%комментар%");
        }
        return type;
    }

    private Iteraction findEndProcessType() {
        Iteraction type = dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e where e.signEndCase = true order by e.number asc")
                .view("iteraction-list-type-view")
                .maxResults(1)
                .optional()
                .orElse(null);
        if (type == null) {
            type = findTypeByNamePattern("%отказ%");
        }
        return type;
    }

    private Iteraction findTypeByNamePattern(String pattern) {
        return dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e where lower(e.iterationName) like :pattern order by e.number asc")
                .parameter("pattern", pattern)
                .view("iteraction-list-type-view")
                .maxResults(1)
                .optional()
                .orElse(null);
    }

    private void handleCardMoved(String interactionIdStr, String targetColIdentifier) {
        if (interactionIdStr == null || targetColIdentifier == null) return;

        UUID interactionId;
        try {
            interactionId = UUID.fromString(interactionIdStr);
        } catch (Exception e) {
            return;
        }

        List<DynamicKanbanColumn> columns = (cachedColumns != null && !cachedColumns.isEmpty())
                ? cachedColumns : loadDynamicColumns();
        DynamicKanbanColumn targetCol = null;

        // 1. Попытка найти колонку по прямому UUID
        try {
            UUID targetColId = UUID.fromString(targetColIdentifier);
            for (DynamicKanbanColumn col : columns) {
                if (col.getId().equals(targetColId)) {
                    targetCol = col;
                    break;
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        // 2. Попытка найти колонку по коду, названию или суффиксу стиля
        if (targetCol == null) {
            for (DynamicKanbanColumn col : columns) {
                if (targetColIdentifier.equalsIgnoreCase(col.getCode())
                        || targetColIdentifier.equalsIgnoreCase(col.getCaption())
                        || (col.getStyleSuffix() != null && targetColIdentifier.equalsIgnoreCase(col.getStyleSuffix()))) {
                    targetCol = col;
                    break;
                }
            }
        }

        if (targetCol == null) return;

        IteractionList sourceItem = dataManager.load(IteractionList.class)
                .id(interactionId)
                .view("recruiter-dashboard-iteraction-list-view")
                .optional()
                .orElse(null);

        if (sourceItem == null) return;

        DynamicKanbanColumn currentCol = resolveColumn(sourceItem.getIteractionType(), columns);
        if (currentCol != null && currentCol.getId().equals(targetCol.getId())) return;

        Iteraction defaultType = findDefaultIteractionForColumn(targetCol.getId());
        if (defaultType == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Не удалось определить действие")
                    .withDescription("Для группы «" + targetCol.getCaption() + "» не найдено подходящее действие")
                    .show();
            return;
        }

        IteractionList draft = metadata.create(IteractionList.class);

        JobCandidate targetCandidate = sourceItem.getCandidate();
        if (targetCandidate != null && targetCandidate.getId() != null) {
            try {
                targetCandidate = dataManager.load(JobCandidate.class)
                .id(targetCandidate.getId())
                .view("jobCandidate-iteraction-list-suggestion-view")
                .optional().orElse(targetCandidate);
            } catch (Exception e) {
                log.debug("Не удалось загрузить представление кандидата: {}", e.getMessage());
                targetCandidate = sourceItem.getCandidate();
            }
        }
        draft.setCandidate(targetCandidate);

        OpenPosition targetVacancy = sourceItem.getVacancy();
        if (targetVacancy != null && targetVacancy.getId() != null) {
            try {
                targetVacancy = dataManager.load(OpenPosition.class)
                        .id(targetVacancy.getId())
                        .view("openPosition-iteraction-list-picker-view")
                        .optional().orElse(targetVacancy);
            } catch (Exception e) {
                log.debug("Не удалось загрузить представление вакансии: {}", e.getMessage());
                targetVacancy = sourceItem.getVacancy();
            }
        }
        draft.setVacancy(targetVacancy);
        ExtUser targetRecruiter = null;
        try {
            targetRecruiter = sourceItem.getRecrutier();
        } catch (Exception e) {
            log.debug("Не удалось извлечь рекрутера из исходного взаимодействия: {}", e.getMessage());
        }
        if (targetRecruiter == null && recruiterLookupField.getValue() != null) {
            User selUser = recruiterLookupField.getValue();
            if (!ALL_RECRUITERS_CODE.equals(selUser.getLogin()) && selUser.getId() != null) {
                targetRecruiter = dataManager.load(ExtUser.class)
                        .id(selUser.getId())
                        .optional().orElse(null);
            }
        }
        if (targetRecruiter == null && userSession.getCurrentOrSubstitutedUser() != null) {
            targetRecruiter = dataManager.load(ExtUser.class)
                    .id(userSession.getCurrentOrSubstitutedUser().getId())
                    .optional().orElse(null);
        }
        draft.setRecrutier(targetRecruiter);
        draft.setDateIteraction(new Date());
        draft.setIteractionType(defaultType);

        final DynamicKanbanColumn finalTargetCol = targetCol;
        IteractionListEdit editor = screenBuilders.editor(IteractionList.class, this)
                .withScreenClass(IteractionListEdit.class)
                .newEntity(draft)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        editor.setRestrictedRootIteractionId(finalTargetCol.getId());
        editor.addAfterCloseListener(closeEvent -> {
            if (closeEvent.closedWith(StandardOutcome.COMMIT)) {
                reload();
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Взаимодействие зарегистрировано")
                        .withDescription("Кандидат перемещён в этап: " + finalTargetCol.getCaption())
                        .show();
            } else {
                reload();
            }
        });
        editor.show();
    }

    private volatile Map<UUID, IteractionNode> iteractionHierarchyCache = Collections.emptyMap();

    public static class IteractionNode {
        private final UUID id;
        private final String number;
        private final String name;
        private final UUID parentId;

        public IteractionNode(UUID id, String number, String name, UUID parentId) {
            this.id = id;
            this.number = number != null ? number.trim() : "";
            this.name = name != null ? name.trim() : "";
            this.parentId = parentId;
        }

        public UUID getId() { return id; }
        public String getNumber() { return number; }
        public String getName() { return name; }
        public UUID getParentId() { return parentId; }
    }

    public synchronized void refreshIteractionHierarchyCache() {
        try {
            List<KeyValueEntity> rows = dataManager.loadValues(
                    "select e.id, e.number, e.iterationName, e.iteractionTree.id from hunttech_Iteraction e where e.deleteTs is null")
                    .properties("id", "number", "iterationName", "parentId")
                    .list();
            Map<UUID, IteractionNode> map = new HashMap<>();
            for (KeyValueEntity row : rows) {
                UUID id = row.getValue("id");
                String num = row.getValue("number");
                String name = row.getValue("iterationName");
                UUID parentId = row.getValue("parentId");
                if (id != null) {
                    map.put(id, new IteractionNode(id, num, name, parentId));
                }
            }
            this.iteractionHierarchyCache = Collections.unmodifiableMap(map);
        } catch (Exception e) {
            log.warn("Не удалось загрузить иерархию взаимодействий: {}", e.getMessage());
        }
    }

    public IteractionNode resolveRootIteractionNode(UUID typeId) {
        if (typeId == null) {
            return null;
        }
        Map<UUID, IteractionNode> cache = this.iteractionHierarchyCache;
        if (cache.isEmpty()) {
            refreshIteractionHierarchyCache();
            cache = this.iteractionHierarchyCache;
        }
        IteractionNode current = cache.get(typeId);
        if (current == null) {
            refreshIteractionHierarchyCache();
            cache = this.iteractionHierarchyCache;
            current = cache.get(typeId);
            if (current == null) {
                return null;
            }
        }
        Set<UUID> visited = new HashSet<>();
        while (current.getParentId() != null && visited.add(current.getId())) {
            IteractionNode parent = cache.get(current.getParentId());
            if (parent == null) {
                break;
            }
            current = parent;
        }
        return current;
    }

    /**
     * Динамическая загрузка колонок Канбана из таблицы Iteraction (элементы верхнего уровня).
     * Любой добавленный в таблицу Iteraction корневой элемент автоматически формирует новую колонку.
     */
    public List<DynamicKanbanColumn> loadDynamicColumns() {
        refreshIteractionHierarchyCache();

        List<Iteraction> rootIteractions = dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e where e.iteractionTree is null and e.deleteTs is null order by e.number asc, e.iterationName asc")
                .list();

        List<DynamicKanbanColumn> columns = new ArrayList<>();
        int defaultOrder = 100;

        for (Iteraction root : rootIteractions) {
            String num = root.getNumber() != null ? root.getNumber().trim() : "";
            String name = root.getIterationName() != null ? root.getIterationName().trim().toLowerCase(Locale.ROOT) : "";

            // Исключаем каналы связи (005 Тип взаимодействия), кадровый резерв и комментарии
            if ("005".equals(num) || name.contains("тип взаимодействия") || name.contains("тип взаимодейтсвия")) {
                continue;
            }
            if (Boolean.TRUE.equals(root.getSignPersonalReserve())
                    || Boolean.TRUE.equals(root.getSignPersonalReserveRemove())
                    || Boolean.TRUE.equals(root.getSignPersonalReserveDelete())
                    || Boolean.TRUE.equals(root.getSignPersonalReservePut())
                    || name.contains("кадров")) {
                continue;
            }
            if (Boolean.TRUE.equals(root.getSignComment()) || "комментарий".equals(name)) {
                continue;
            }

            int order = defaultOrder++;
            if (!num.isEmpty()) {
                try {
                    order = Integer.parseInt(num.replaceAll("\\D+", ""));
                } catch (Exception ignored) {
                }
            }

            String styleName = resolveColumnStyle(num, name);
            String styleSuffix = resolveStyleSuffix(num, name);
            String caption = root.getIterationName() != null ? root.getIterationName() : ("Группа " + num);

            columns.add(new DynamicKanbanColumn(root.getId(), num, caption, styleName, styleSuffix, order));
        }

        columns.sort(Comparator.comparingInt(DynamicKanbanColumn::getOrder));
        return columns;
    }

    private String resolveColumnStyle(String num, String name) {
        if ("001".equals(num) || name.contains("ресерчинг")) return "recruiter-stage-new";
        if ("002".equals(num) || name.contains("хантинг")) return "recruiter-stage-recruiter";
        if ("003".equals(num) || name.contains("заказчик")) return "recruiter-stage-client";
        if ("008".equals(num) || name.contains("успех") || name.contains("офер") || name.contains("оффер")) return "recruiter-stage-offer";
        if ("007".equals(num) || name.contains("отказ")) return "recruiter-stage-outcome";
        if ("010".equals(num) || name.contains("закрытие")) return "recruiter-stage-outcome";
        if ("009".equals(num) || name.contains("задач")) return "recruiter-stage-client-interview";
        return "recruiter-stage-recruiter";
    }

    private String resolveStyleSuffix(String num, String name) {
        if ("001".equals(num) || name.contains("ресерчинг")) return "new";
        if ("002".equals(num) || name.contains("хантинг")) return "recruiter";
        if ("003".equals(num) || name.contains("заказчик")) return "client";
        if ("008".equals(num) || name.contains("успех") || name.contains("офер") || name.contains("оффер")) return "offer";
        if ("007".equals(num) || name.contains("отказ")) return "outcome";
        if ("010".equals(num) || name.contains("закрытие")) return "closed";
        if ("009".equals(num) || name.contains("задач")) return "client-interview";
        return "default";
    }

    public DynamicKanbanColumn resolveColumn(Iteraction type, List<DynamicKanbanColumn> columns) {
        if (type == null || columns == null || columns.isEmpty()) {
            return (columns != null && !columns.isEmpty()) ? columns.get(0) : null;
        }

        UUID typeId = type.getId();
        IteractionNode root = resolveRootIteractionNode(typeId);

        Iteraction rootEntity = null;
        if (root == null) {
            try {
                Iteraction curr = type;
                Set<UUID> visited = new HashSet<>();
                while (curr.getIteractionTree() != null && visited.add(curr.getId())) {
                    curr = curr.getIteractionTree();
                }
                rootEntity = curr;
            } catch (Throwable ignored) {
            }
        }

        // 1. Ищем прямое совпадение по ID корня
        UUID rootId = root != null ? root.getId() : (rootEntity != null ? rootEntity.getId() : null);
        if (rootId != null) {
            for (DynamicKanbanColumn col : columns) {
                if (rootId.equals(col.getId())) {
                    return col;
                }
            }
        }

        // 2. Совпадение по коду/номеру корня
        String rootNum = root != null ? root.getNumber()
                : (rootEntity != null && rootEntity.getNumber() != null ? rootEntity.getNumber().trim()
                : (type.getNumber() != null ? type.getNumber().trim() : ""));
        if (!rootNum.isEmpty()) {
            for (DynamicKanbanColumn col : columns) {
                if (rootNum.equalsIgnoreCase(col.getCode())) {
                    return col;
                }
            }
        }

        // 3. Совпадение по наименованию
        String rootName = root != null ? root.getName().toLowerCase(Locale.ROOT)
                : (rootEntity != null && rootEntity.getIterationName() != null ? rootEntity.getIterationName().trim().toLowerCase(Locale.ROOT)
                : (type.getIterationName() != null ? type.getIterationName().trim().toLowerCase(Locale.ROOT) : ""));
        for (DynamicKanbanColumn col : columns) {
            if (rootName.equals(col.getCaption().toLowerCase(Locale.ROOT))) {
                return col;
            }
        }

        // 4. Fallback по бизнес-флагам для legacy-записей
        if (Boolean.TRUE.equals(type.getSignEndCase())) {
            for (DynamicKanbanColumn col : columns) {
                if ("010".equals(col.getCode()) || col.getCaption().toLowerCase(Locale.ROOT).contains("закрытие")) return col;
            }
        }
        if (Boolean.TRUE.equals(type.getSignStartCase())) {
            for (DynamicKanbanColumn col : columns) {
                if ("008".equals(col.getCode()) || col.getCaption().toLowerCase(Locale.ROOT).contains("успех")) return col;
            }
        }
        if (Boolean.TRUE.equals(type.getSignClientInterview()) || Boolean.TRUE.equals(type.getSignSendToClient())) {
            for (DynamicKanbanColumn col : columns) {
                if ("003".equals(col.getCode()) || col.getCaption().toLowerCase(Locale.ROOT).contains("заказчик")) return col;
            }
        }
        if (Boolean.TRUE.equals(type.getSignOurInterviewAssigned()) || Boolean.TRUE.equals(type.getSignOurInterview())) {
            for (DynamicKanbanColumn col : columns) {
                if ("002".equals(col.getCode()) || col.getCaption().toLowerCase(Locale.ROOT).contains("хантинг")) return col;
            }
        }

        return columns.get(0);
    }

    private Iteraction findDefaultIteractionForColumn(UUID rootId) {
        if (rootId == null) return null;
        List<Iteraction> children = dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e left join fetch e.iteractionTree where e.iteractionTree.id = :rootId and e.deleteTs is null order by e.number asc, e.iterationName asc")
                .parameter("rootId", rootId)
                .list();

        if (!children.isEmpty()) {
            for (Iteraction child : children) {
                if (Boolean.TRUE.equals(child.getMandatoryIteraction())) {
                    return child;
                }
            }
            return children.get(0);
        }

        return dataManager.load(Iteraction.class)
                .id(rootId)
                .optional()
                .orElse(null);
    }

    /**
     * DTO динамической колонки Канбана.
     */
    public static class DynamicKanbanColumn {
        private final UUID id;
        private final String code;
        private final String caption;
        private final String styleName;
        private final String styleSuffix;
        private final int order;

        public DynamicKanbanColumn(UUID id, String code, String caption, String styleName, String styleSuffix, int order) {
            this.id = id;
            this.code = code;
            this.caption = caption;
            this.styleName = styleName;
            this.styleSuffix = styleSuffix;
            this.order = order;
        }

        public UUID getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getCaption() {
            return caption;
        }

        public String getStyleName() {
            return styleName;
        }

        public String getStyleSuffix() {
            return styleSuffix;
        }

        public int getOrder() {
            return order;
        }
    }

    private Date daysAgo(int days) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -days);
        return calendar.getTime();
    }

    /**
     * Этапы Канбан-доски, сформированные по главным группам справочника Iteraction.
     */
    public enum KanbanStage {
        RESEARCHING("001", "Ресерчинг", "recruiter-stage-new", 0),
        HUNTING("002", "Хантинг", "recruiter-stage-recruiter", 1),
        CLIENT("003", "На стороне заказчика", "recruiter-stage-client", 2),
        OFFER("008", "УСПЕХ", "recruiter-stage-offer", 3),
        OUTCOME("007", "ОТКАЗ", "recruiter-stage-outcome", 4),
        CASE_CLOSED("010", "Закрытие кейса", "recruiter-stage-outcome", 5),
        RESERVE("", "Кадровый резерв", "recruiter-stage-reserve", -1);

        private final String code;
        private final String caption;
        private final String styleName;
        private final int order;

        KanbanStage(String code, String caption, String styleName, int order) {
            this.code = code;
            this.caption = caption;
            this.styleName = styleName;
            this.order = order;
        }

        public String getCode() {
            return code;
        }

        public String getCaption() {
            return caption;
        }

        public String getStyleName() {
            return styleName;
        }

        public int getOrder() {
            return order;
        }

        public static KanbanStage resolve(Iteraction type) {
            if (type == null) {
                return RESEARCHING;
            }
            if (Boolean.TRUE.equals(type.getSignPersonalReserve())
                    || Boolean.TRUE.equals(type.getSignPersonalReservePut())) {
                return RESERVE;
            }

            // 1. Иерархия: находим корень дерева взаимодействий с защитой от циклов и detached indirection
            Iteraction root = type;
            Set<UUID> visited = new HashSet<>();
            try {
                while (root.getIteractionTree() != null && visited.add(root.getId())) {
                    root = root.getIteractionTree();
                }
            } catch (Throwable ignored) {
                // Если entity detached и lazy relationship не загружен, работаем с текущим узлом
            }

            String rootNum = root.getNumber() != null ? root.getNumber().trim() : "";
            String rootName = root.getIterationName() != null ? root.getIterationName().trim().toLowerCase(Locale.ROOT) : "";

            if ("001".equals(rootNum) || rootName.contains("ресерчинг")) {
                return RESEARCHING;
            }
            if ("002".equals(rootNum) || rootName.contains("хантинг")) {
                return HUNTING;
            }
            if ("003".equals(rootNum) || rootName.contains("заказчик")) {
                return CLIENT;
            }
            if ("008".equals(rootNum) || rootName.contains("успех") || rootName.contains("офер") || rootName.contains("оффер")) {
                return OFFER;
            }
            if ("007".equals(rootNum) || rootName.contains("отказ")) {
                return OUTCOME;
            }
            if ("010".equals(rootNum) || rootName.contains("закрытие")) {
                return CASE_CLOSED;
            }

            // 2. Fallback по бизнес-флагам для legacy-записей
            if (Boolean.TRUE.equals(type.getSignEndCase())) {
                return CASE_CLOSED;
            }
            if (Boolean.TRUE.equals(type.getSignStartCase()) || rootName.contains("offer")) {
                return OFFER;
            }
            if (Boolean.TRUE.equals(type.getSignClientInterview()) || Boolean.TRUE.equals(type.getSignSendToClient())) {
                return CLIENT;
            }
            if (Boolean.TRUE.equals(type.getSignOurInterviewAssigned()) || Boolean.TRUE.equals(type.getSignOurInterview())) {
                return HUNTING;
            }

            return RESEARCHING;
        }
    }
}
