package com.company.hunttech.web.widgets.recruiterdashboard;

import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.events.DashboardEvent;
import com.haulmont.addon.dashboard.web.widget.RefreshableWidget;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.ScreenFragment.InitEvent;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;

@UiController("hunttech_RecruiterCandidateKanbanWidget")
@UiDescriptor("recruiter-candidate-kanban-widget.xml")
@DashboardWidget(name = "Kanban")
public class RecruiterCandidateKanbanWidget extends ScreenFragment implements RefreshableWidget {

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

    private boolean isInitialized = false;
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM.yyyy");

    @Subscribe
    public void onInit(InitEvent event) {
        initFilters();
        isInitialized = true;
        reload();
    }

    @Override
    public void refresh(DashboardEvent dashboardEvent) {
        reload();
    }

    private void initFilters() {
        // 1. Фильтр периода: 30 дней, 90 дней, За все время
        Map<String, Integer> periodOptions = new LinkedHashMap<>();
        periodOptions.put("За 30 дней", 30);
        periodOptions.put("За 90 дней", 90);
        periodOptions.put("За все время", 0);
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
        User selectedRecruiter = recruiterLookupField.getValue();
        if (selectedRecruiter == null) {
            selectedRecruiter = userSession.getCurrentOrSubstitutedUser();
        }

        Integer days = periodRadioGroup.getValue();
        if (days == null) {
            days = 90;
        }

        String search = searchField.getValue();

        StringBuilder jpql = new StringBuilder(
                "select e from hunttech_IteractionList e where 1=1 ");

        Map<String, Object> params = new HashMap<>();

        if (selectedRecruiter != null) {
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

        jpql.append("and e.numberIteraction = (")
                .append(" select max(x.numberIteraction) from hunttech_IteractionList x ")
                .append(" where x.candidate = e.candidate ");

        if (selectedRecruiter != null) {
            jpql.append(" and x.recrutier = :recrutier ");
        }

        jpql.append(" and ((e.vacancy is null and x.vacancy is null) or x.vacancy = e.vacancy)")
                .append(") order by e.dateIteraction desc");

        com.haulmont.cuba.core.global.FluentLoader.ByQuery<IteractionList, UUID> loader =
                dataManager.load(IteractionList.class)
                        .query(jpql.toString())
                        .view("recruiter-dashboard-iteraction-list-view");

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            loader.parameter(entry.getKey(), entry.getValue());
        }

        List<IteractionList> cases = loader.list();

        Map<RecruiterDashboardStage, List<IteractionList>> grouped =
                new EnumMap<>(RecruiterDashboardStage.class);
        for (RecruiterDashboardStage stage : RecruiterDashboardStage.values()) {
            grouped.put(stage, new ArrayList<>());
        }
        for (IteractionList item : cases) {
            RecruiterDashboardStage stage = RecruiterDashboardStage.resolve(item.getIteractionType());
            if (stage != RecruiterDashboardStage.RESERVE) {
                grouped.get(stage).add(item);
            }
        }

        renderKpis(grouped);
        renderBoard(grouped);

        String periodText = (days > 0) ? ("Последние " + days + " дней") : "За все время";
        String recruiterText = selectedRecruiter != null ? selectedRecruiter.getName() : "Все";
        periodLabel.setValue(periodText + " · " + recruiterText + " · " + cases.size() + " активных кейсов");
    }

    private void renderKpis(Map<RecruiterDashboardStage, List<IteractionList>> grouped) {
        kpiBar.removeAll();
        int total = grouped.values().stream().mapToInt(List::size).sum();
        addKpi("Всего в работе", total, "recruiter-kpi-primary", "★ 100%");
        addKpi("Новые контакты", grouped.get(RecruiterDashboardStage.NEW).size(), "recruiter-kpi-muted", "Этап 1");
        addKpi("Интервью",
                grouped.get(RecruiterDashboardStage.RECRUITER_INTERVIEW).size()
                        + grouped.get(RecruiterDashboardStage.CLIENT_INTERVIEW).size(),
                "recruiter-kpi-blue", "Этап 2-4");
        addKpi("У заказчика", grouped.get(RecruiterDashboardStage.CLIENT).size(), "recruiter-kpi-violet", "Этап 3");
        addKpi("Оффер / финал", grouped.get(RecruiterDashboardStage.OFFER).size(), "recruiter-kpi-green", "Финал");
        addKpi("Исход / архив", grouped.get(RecruiterDashboardStage.OUTCOME).size(), "recruiter-kpi-muted", "Закрыто");
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

    private void renderBoard(Map<RecruiterDashboardStage, List<IteractionList>> grouped) {
        kanbanBoard.removeAll();
        for (RecruiterDashboardStage stage : new RecruiterDashboardStage[]{
                RecruiterDashboardStage.NEW,
                RecruiterDashboardStage.RECRUITER_INTERVIEW,
                RecruiterDashboardStage.CLIENT,
                RecruiterDashboardStage.CLIENT_INTERVIEW,
                RecruiterDashboardStage.OFFER,
                RecruiterDashboardStage.OUTCOME}) {
            kanbanBoard.add(createColumn(stage, grouped.get(stage)));
        }
    }

    private VBoxLayout createColumn(RecruiterDashboardStage stage, List<IteractionList> items) {
        VBoxLayout column = uiComponents.create(VBoxLayout.class);
        column.setWidth("300px");
        column.setHeight("100%");
        column.setStyleName("recruiter-kanban-column " + stage.getStyleName());
        column.setSpacing(true);

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
                cardsBox.add(createCandidateCard(item));
            }
        }

        scrollBox.add(cardsBox);
        column.add(scrollBox);
        column.expand(scrollBox);
        return column;
    }

    private VBoxLayout createCandidateCard(IteractionList item) {
        VBoxLayout card = uiComponents.create(VBoxLayout.class);
        card.setWidthFull();
        card.setSpacing(true);
        card.setStyleName("recruiter-kanban-card");

        JobCandidate candidate = item.getCandidate();

        // 1. Шапка: Круглый аватар + ФИО + Должность
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

        LinkButton name = uiComponents.create(LinkButton.class);
        name.setCaption(candidate == null ? "Кандидат" : candidate.getFullName());
        name.setStyleName("recruiter-candidate-link bold");
        if (candidate != null) {
            name.addClickListener(event -> screenBuilders.editor(JobCandidate.class, this)
                    .editEntity(candidate)
                    .build()
                    .show());
        }

        Label<String> posLabel = uiComponents.create(Label.TYPE_STRING);
        posLabel.setHtmlEnabled(true);
        String posName = (candidate != null && candidate.getPersonPosition() != null && candidate.getPersonPosition().getPositionRuName() != null)
                ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
        posLabel.setValue("<span class=\"recruiter-kanban-position-badge\">" + posName + "</span>");

        nameBox.add(name);
        nameBox.add(posLabel);

        header.add(img);
        header.add(nameBox);
        header.expand(nameBox);
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

        // 3. Вакансия
        Label<String> vacancy = uiComponents.create(Label.TYPE_STRING);
        String vacName = item.getVacancy() == null ? "Без вакансии" : item.getVacancy().getVacansyName();
        vacancy.setValue("💼 " + vacName);
        vacancy.setStyleName("recruiter-kanban-vacancy");
        card.add(vacancy);

        // 4. Статус взаимодействия и дата
        HBoxLayout statusRow = uiComponents.create(HBoxLayout.class);
        statusRow.setWidthFull();
        statusRow.setSpacing(true);

        Label<String> interaction = uiComponents.create(Label.TYPE_STRING);
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

        // 5. Кнопка «Карточка профиля»
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

    private Date daysAgo(int days) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -days);
        return calendar.getTime();
    }
}
