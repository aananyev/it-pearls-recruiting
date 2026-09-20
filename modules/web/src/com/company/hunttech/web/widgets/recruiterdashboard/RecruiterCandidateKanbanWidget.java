package com.company.hunttech.web.widgets.recruiterdashboard;

import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.events.DashboardEvent;
import com.haulmont.addon.dashboard.web.widget.RefreshableWidget;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.CssLayout;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LinkButton;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.ScreenFragment.InitEvent;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@UiController("hunttech_RecruiterCandidateKanbanWidget")
@UiDescriptor("recruiter-candidate-kanban-widget.xml")
@DashboardWidget(name = "Рекрутер: кандидаты в работе — Kanban")
public class RecruiterCandidateKanbanWidget extends ScreenFragment implements RefreshableWidget {

    private static final int PERIOD_DAYS = 30;

    private static final String QUERY_LATEST_CASES =
            "select e from hunttech_IteractionList e " +
            "where e.recrutier = :recrutier and e.dateIteraction >= :startDate " +
            "and e.numberIteraction = (" +
            " select max(x.numberIteraction) from hunttech_IteractionList x " +
            " where x.recrutier = :recrutier and x.candidate = e.candidate and x.vacancy = e.vacancy" +
            ") order by e.dateIteraction desc";

    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private HBoxLayout kpiBar;
    @Inject
    private CssLayout kanbanBoard;
    @Inject
    private Label<String> periodLabel;

    @Subscribe
    public void onInit(InitEvent event) {
        reload();
    }

    @Override
    public void refresh(DashboardEvent dashboardEvent) {
        reload();
    }

    private void reload() {
        List<IteractionList> cases = dataManager.load(IteractionList.class)
                .query(QUERY_LATEST_CASES)
                .parameter("recrutier", userSession.getUser())
                .parameter("startDate", daysAgo(PERIOD_DAYS))
                .view("iteractionList-edit-view")
                .list();

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
        periodLabel.setValue("Последние " + PERIOD_DAYS + " дней · " + cases.size() + " активных кейсов");
    }

    private void renderKpis(Map<RecruiterDashboardStage, List<IteractionList>> grouped) {
        kpiBar.removeAll();
        addKpi("В работе", grouped.values().stream().mapToInt(List::size).sum(), "recruiter-kpi-primary");
        addKpi("Интервью",
                grouped.get(RecruiterDashboardStage.RECRUITER_INTERVIEW).size()
                        + grouped.get(RecruiterDashboardStage.CLIENT_INTERVIEW).size(),
                "recruiter-kpi-blue");
        addKpi("У заказчика", grouped.get(RecruiterDashboardStage.CLIENT).size(), "recruiter-kpi-violet");
        addKpi("Оффер / финал", grouped.get(RecruiterDashboardStage.OFFER).size(), "recruiter-kpi-green");
        addKpi("Исход", grouped.get(RecruiterDashboardStage.OUTCOME).size(), "recruiter-kpi-muted");
    }

    private void addKpi(String caption, int value, String style) {
        VBoxLayout card = uiComponents.create(VBoxLayout.class);
        card.setStyleName("recruiter-kpi-card " + style);
        card.setSpacing(false);

        Label<String> valueLabel = uiComponents.create(Label.TYPE_STRING);
        valueLabel.setValue(String.valueOf(value));
        valueLabel.setStyleName("recruiter-kpi-value");

        Label<String> captionLabel = uiComponents.create(Label.TYPE_STRING);
        captionLabel.setValue(caption);
        captionLabel.setStyleName("recruiter-kpi-caption");

        card.add(valueLabel);
        card.add(captionLabel);
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
        column.setWidthFull();
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

        if (items.isEmpty()) {
            Label<String> empty = uiComponents.create(Label.TYPE_STRING);
            empty.setValue("Нет кандидатов на этом этапе");
            empty.setStyleName("recruiter-dashboard-empty");
            column.add(empty);
            return column;
        }

        for (IteractionList item : items) {
            column.add(createCandidateCard(item));
        }
        return column;
    }

    private VBoxLayout createCandidateCard(IteractionList item) {
        VBoxLayout card = uiComponents.create(VBoxLayout.class);
        card.setWidthFull();
        card.setSpacing(false);
        card.setStyleName("recruiter-kanban-card");

        JobCandidate candidate = item.getCandidate();
        LinkButton name = uiComponents.create(LinkButton.class);
        name.setCaption(candidate == null ? "Кандидат" : candidate.getFullName());
        name.setStyleName("recruiter-candidate-link");
        if (candidate != null) {
            name.addClickListener(event -> screenBuilders.editor(JobCandidate.class, this)
                    .editEntity(candidate)
                    .build()
                    .show());
        }

        Label<String> vacancy = uiComponents.create(Label.TYPE_STRING);
        vacancy.setValue(item.getVacancy() == null ? "Без вакансии" : item.getVacancy().getVacansyName());
        vacancy.setStyleName("recruiter-kanban-vacancy");

        Label<String> interaction = uiComponents.create(Label.TYPE_STRING);
        interaction.setValue(item.getIteractionType() == null
                ? "Статус не определён"
                : item.getIteractionType().getIterationName());
        interaction.setStyleName("recruiter-kanban-interaction");

        Label<String> date = uiComponents.create(Label.TYPE_STRING);
        date.setValue(item.getDateIteraction() == null
                ? "Дата не указана"
                : new SimpleDateFormat("dd.MM.yyyy HH:mm").format(item.getDateIteraction()));
        date.setStyleName("recruiter-kanban-date");

        card.add(name);
        card.add(vacancy);
        card.add(interaction);
        card.add(date);
        return card;
    }

    private Date daysAgo(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        return calendar.getTime();
    }
}
