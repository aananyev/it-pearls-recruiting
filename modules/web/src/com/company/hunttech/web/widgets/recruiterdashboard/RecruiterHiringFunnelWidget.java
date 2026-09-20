package com.company.hunttech.web.widgets.recruiterdashboard;

import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.RecrutiesTasks;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.events.DashboardEvent;
import com.haulmont.addon.dashboard.web.widget.RefreshableWidget;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.ScreenFragment.InitEvent;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@UiController("hunttech_RecruiterHiringFunnelWidget")
@UiDescriptor("recruiter-hiring-funnel-widget.xml")
@DashboardWidget(name = "Рекрутер: воронка найма")
public class RecruiterHiringFunnelWidget extends ScreenFragment implements RefreshableWidget {

    private static final int PERIOD_DAYS = 30;

    private static final String QUERY_SUBSCRIPTIONS =
            "select e from hunttech_RecrutiesTasks e " +
            "where e.reacrutier = :recrutier and e.closed = false " +
            "and (e.subscribe = true or e.subscribe is null) " +
            "and (e.endDate is null or e.endDate >= current_date)";

    private static final String QUERY_INTERACTIONS =
            "select e from hunttech_IteractionList e " +
            "where e.recrutier = :recrutier and e.vacancy in :vacancies " +
            "and e.dateIteraction >= :startDate order by e.dateIteraction";

    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private HBoxLayout kpiBar;
    @Inject
    private VBoxLayout aggregateFunnel;
    @Inject
    private VBoxLayout vacancyRows;
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
        List<RecrutiesTasks> subscriptions = dataManager.load(RecrutiesTasks.class)
                .query(QUERY_SUBSCRIPTIONS)
                .parameter("recrutier", userSession.getUser())
                .view("recrutiesTasks-view")
                .list();

        List<OpenPosition> vacancies = new ArrayList<>();
        for (RecrutiesTasks task : subscriptions) {
            if (task.getOpenPosition() != null && !vacancies.contains(task.getOpenPosition())) {
                vacancies.add(task.getOpenPosition());
            }
        }

        if (vacancies.isEmpty()) {
            renderEmpty();
            return;
        }

        List<IteractionList> interactions = dataManager.load(IteractionList.class)
                .query(QUERY_INTERACTIONS)
                .parameter("recrutier", userSession.getUser())
                .parameter("vacancies", vacancies)
                .parameter("startDate", daysAgo(PERIOD_DAYS))
                .view("iteractionList-edit-view")
                .list();

        Map<RecruiterDashboardStage, Set<String>> aggregate = new EnumMap<>(RecruiterDashboardStage.class);
        Map<UUID, Map<RecruiterDashboardStage, Set<String>>> byVacancy = new HashMap<>();
        for (RecruiterDashboardStage stage : RecruiterDashboardStage.values()) {
            aggregate.put(stage, new HashSet<>());
        }

        for (IteractionList item : interactions) {
            RecruiterDashboardStage stage = RecruiterDashboardStage.resolve(item.getIteractionType());
            if (stage == RecruiterDashboardStage.RESERVE) {
                continue;
            }
            String key = caseKey(item);
            aggregate.get(stage).add(key);
            if (item.getVacancy() != null) {
                Map<RecruiterDashboardStage, Set<String>> stageMap =
                        byVacancy.computeIfAbsent(item.getVacancy().getId(), id -> emptyStageMap());
                stageMap.get(stage).add(key);
            }
        }

        renderKpis(vacancies.size(), aggregate);
        renderAggregateFunnel(aggregate);
        renderVacancies(vacancies, byVacancy);
        periodLabel.setValue("Подписанные вакансии · последние " + PERIOD_DAYS + " дней");
    }

    private void renderEmpty() {
        kpiBar.removeAll();
        aggregateFunnel.removeAll();
        vacancyRows.removeAll();
        periodLabel.setValue("Нет активных подписок на вакансии");

        Label<String> empty = uiComponents.create(Label.TYPE_STRING);
        empty.setValue("Подпишитесь на вакансию в задачах рекрутера — она появится в воронке.");
        empty.setStyleName("recruiter-dashboard-empty");
        aggregateFunnel.add(empty);
    }

    private void renderKpis(int vacancyCount, Map<RecruiterDashboardStage, Set<String>> aggregate) {
        kpiBar.removeAll();
        addKpi("Вакансий", vacancyCount, "recruiter-kpi-primary");
        addKpi("Интервью рекрутера", aggregate.get(RecruiterDashboardStage.RECRUITER_INTERVIEW).size(),
                "recruiter-kpi-blue");
        addKpi("Интервью заказчика", aggregate.get(RecruiterDashboardStage.CLIENT_INTERVIEW).size(),
                "recruiter-kpi-violet");
        addKpi("Оффер / финал", aggregate.get(RecruiterDashboardStage.OFFER).size(),
                "recruiter-kpi-green");
        addKpi("Исход", aggregate.get(RecruiterDashboardStage.OUTCOME).size(),
                "recruiter-kpi-muted");
    }

    private void renderAggregateFunnel(Map<RecruiterDashboardStage, Set<String>> aggregate) {
        aggregateFunnel.removeAll();
        int max = 1;
        for (RecruiterDashboardStage stage : funnelStages()) {
            max = Math.max(max, aggregate.get(stage).size());
        }

        for (RecruiterDashboardStage stage : funnelStages()) {
            int count = aggregate.get(stage).size();
            int width = Math.max(22, (int) Math.round(count * 100.0 / max));

            VBoxLayout row = uiComponents.create(VBoxLayout.class);
            row.setWidthFull();
            row.setSpacing(false);
            row.setStyleName("recruiter-funnel-row");

            HBoxLayout labels = uiComponents.create(HBoxLayout.class);
            labels.setWidthFull();

            Label<String> caption = uiComponents.create(Label.TYPE_STRING);
            caption.setValue(stage.getCaption());
            caption.setStyleName("recruiter-funnel-caption");

            Label<String> value = uiComponents.create(Label.TYPE_STRING);
            value.setValue(String.valueOf(count));
            value.setStyleName("recruiter-funnel-value");

            labels.add(caption);
            labels.add(value);
            labels.expand(caption);

            HBoxLayout track = uiComponents.create(HBoxLayout.class);
            track.setWidthFull();
            track.setStyleName("recruiter-funnel-track");

            Label<String> bar = uiComponents.create(Label.TYPE_STRING);
            bar.setValue("");
            bar.setWidth(width + "%");
            bar.setStyleName("recruiter-funnel-bar " + stage.getStyleName());
            track.add(bar);

            row.add(labels);
            row.add(track);
            aggregateFunnel.add(row);
        }
    }

    private void renderVacancies(List<OpenPosition> vacancies,
                                 Map<UUID, Map<RecruiterDashboardStage, Set<String>>> byVacancy) {
        vacancyRows.removeAll();

        HBoxLayout header = uiComponents.create(HBoxLayout.class);
        header.setWidthFull();
        header.setStyleName("recruiter-funnel-table-header");
        addCell(header, "Вакансия", true);
        for (RecruiterDashboardStage stage : funnelStages()) {
            addCell(header, shortCaption(stage), false);
        }
        vacancyRows.add(header);

        for (OpenPosition vacancy : vacancies) {
            HBoxLayout row = uiComponents.create(HBoxLayout.class);
            row.setWidthFull();
            row.setStyleName("recruiter-funnel-vacancy-row");

            addCell(row, vacancy.getVacansyName(), true);
            Map<RecruiterDashboardStage, Set<String>> counts =
                    byVacancy.getOrDefault(vacancy.getId(), emptyStageMap());
            for (RecruiterDashboardStage stage : funnelStages()) {
                addCell(row, String.valueOf(counts.get(stage).size()), false);
            }
            vacancyRows.add(row);
        }
    }

    private void addCell(HBoxLayout row, String text, boolean main) {
        Label<String> label = uiComponents.create(Label.TYPE_STRING);
        label.setValue(text == null ? "—" : text);
        label.setStyleName(main ? "recruiter-funnel-cell recruiter-funnel-main-cell" : "recruiter-funnel-cell");
        label.setWidth(main ? "34%" : "11%");
        row.add(label);
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

    private Map<RecruiterDashboardStage, Set<String>> emptyStageMap() {
        Map<RecruiterDashboardStage, Set<String>> result = new EnumMap<>(RecruiterDashboardStage.class);
        for (RecruiterDashboardStage stage : RecruiterDashboardStage.values()) {
            result.put(stage, new HashSet<>());
        }
        return result;
    }

    private RecruiterDashboardStage[] funnelStages() {
        return new RecruiterDashboardStage[]{
                RecruiterDashboardStage.NEW,
                RecruiterDashboardStage.RECRUITER_INTERVIEW,
                RecruiterDashboardStage.CLIENT,
                RecruiterDashboardStage.CLIENT_INTERVIEW,
                RecruiterDashboardStage.OFFER,
                RecruiterDashboardStage.OUTCOME
        };
    }

    private String shortCaption(RecruiterDashboardStage stage) {
        Map<RecruiterDashboardStage, String> captions = new LinkedHashMap<>();
        captions.put(RecruiterDashboardStage.NEW, "Новые");
        captions.put(RecruiterDashboardStage.RECRUITER_INTERVIEW, "Рекрутер");
        captions.put(RecruiterDashboardStage.CLIENT, "Клиент");
        captions.put(RecruiterDashboardStage.CLIENT_INTERVIEW, "Интервью");
        captions.put(RecruiterDashboardStage.OFFER, "Оффер");
        captions.put(RecruiterDashboardStage.OUTCOME, "Исход");
        return captions.get(stage);
    }

    private String caseKey(IteractionList item) {
        return (item.getCandidate() == null ? "no-candidate" : item.getCandidate().getId())
                + ":"
                + (item.getVacancy() == null ? "no-vacancy" : item.getVacancy().getId());
    }

    private Date daysAgo(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        return calendar.getTime();
    }
}
