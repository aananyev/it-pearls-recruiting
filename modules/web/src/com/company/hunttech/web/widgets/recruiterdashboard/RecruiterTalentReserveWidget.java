package com.company.hunttech.web.widgets.recruiterdashboard;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.events.DashboardEvent;
import com.haulmont.addon.dashboard.web.widget.RefreshableWidget;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LinkButton;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.AfterInitEvent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@UiController("hunttech_RecruiterTalentReserveWidget")
@UiDescriptor("recruiter-talent-reserve-widget.xml")
@DashboardWidget(name = "Рекрутер: кадровый резерв")
public class RecruiterTalentReserveWidget extends ScreenFragment implements RefreshableWidget {

    private static final String QUERY_RESERVE =
            "select e.id, e.jobCandidate.id, e.jobCandidate.fullName, " +
            "e.personPosition.id, e.personPosition.positionRuName, " +
            "e.openPosition.vacansyName, e.date, e.endDate, e.inProcess " +
            "from hunttech_PersonelReserve e " +
            "where e.recruter = :recrutier and (e.removedFromReserve is null or e.removedFromReserve = false) " +
            "order by e.endDate, e.jobCandidate.fullName";

    private static final String QUERY_OPEN_POSITIONS =
            "select e.id, e.vacansyName, e.positionType.id, e.projectName.projectName " +
            "from hunttech_OpenPosition e " +
            "where e.openClose = false and (e.signDraft is null or e.signDraft = false)";

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
    private VBoxLayout reserveRows;
    @Inject
    private Label<String> periodLabel;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        reload();
    }

    @Override
    public void refresh(DashboardEvent dashboardEvent) {
        reload();
    }

    private void reload() {
        List<KeyValueEntity> reserve = dataManager.loadValues(QUERY_RESERVE)
                .properties("reserveId", "candidateId", "candidateName", "positionId", "positionName",
                        "sourceVacancy", "date", "endDate", "inProcess")
                .parameter("recrutier", userSession.getUser())
                .list();

        List<KeyValueEntity> vacancies = dataManager.loadValues(QUERY_OPEN_POSITIONS)
                .properties("vacancyId", "vacancyName", "positionId", "projectName")
                .list();

        Map<UUID, List<KeyValueEntity>> vacanciesByPosition = new HashMap<>();
        for (KeyValueEntity vacancy : vacancies) {
            UUID positionId = vacancy.getValue("positionId");
            if (positionId != null) {
                vacanciesByPosition.computeIfAbsent(positionId, id -> new ArrayList<>()).add(vacancy);
            }
        }

        int active = 0;
        int expiring = 0;
        int withMatches = 0;
        Date monthAhead = daysAhead(30);
        for (KeyValueEntity row : reserve) {
            Boolean inProcess = row.getValue("inProcess");
            Date endDate = row.getValue("endDate");
            UUID positionId = row.getValue("positionId");
            if (Boolean.TRUE.equals(inProcess)) {
                active++;
            }
            if (endDate != null && !endDate.before(new Date()) && !endDate.after(monthAhead)) {
                expiring++;
            }
            if (positionId != null && !vacanciesByPosition.getOrDefault(positionId, new ArrayList<>()).isEmpty()) {
                withMatches++;
            }
        }

        renderKpis(reserve.size(), active, expiring, withMatches);
        renderRows(reserve, vacanciesByPosition);
        periodLabel.setValue("Мой кадровый резерв · открытых вакансий для сопоставления: " + vacancies.size());
    }

    private void renderKpis(int total, int active, int expiring, int withMatches) {
        kpiBar.removeAll();
        addKpi("В резерве", total, "recruiter-kpi-primary");
        addKpi("В процессе", active, "recruiter-kpi-blue");
        addKpi("Истекает ≤ 30 дней", expiring, "recruiter-kpi-violet");
        addKpi("Есть вакансии", withMatches, "recruiter-kpi-green");
    }

    private void renderRows(List<KeyValueEntity> reserve,
                            Map<UUID, List<KeyValueEntity>> vacanciesByPosition) {
        reserveRows.removeAll();

        HBoxLayout header = uiComponents.create(HBoxLayout.class);
        header.setWidthFull();
        header.setStyleName("recruiter-reserve-table-header");
        addHeaderCell(header, "Кандидат", "32%");
        addHeaderCell(header, "Позиция", "20%");
        addHeaderCell(header, "Источник", "20%");
        addHeaderCell(header, "До", "12%");
        addHeaderCell(header, "Возможности", "16%");
        reserveRows.add(header);

        if (reserve.isEmpty()) {
            Label<String> empty = uiComponents.create(Label.TYPE_STRING);
            empty.setValue("В вашем кадровом резерве сейчас нет активных кандидатов.");
            empty.setStyleName("recruiter-dashboard-empty");
            reserveRows.add(empty);
            return;
        }

        for (KeyValueEntity item : reserve) {
            HBoxLayout row = uiComponents.create(HBoxLayout.class);
            row.setWidthFull();
            row.setStyleName("recruiter-reserve-row");

            UUID candidateId = item.getValue("candidateId");
            String candidateName = item.getValue("candidateName");
            LinkButton candidate = uiComponents.create(LinkButton.class);
            candidate.setCaption(candidateName == null ? "Кандидат" : candidateName);
            candidate.setWidth("32%");
            candidate.setStyleName("recruiter-candidate-link");
            if (candidateId != null) {
                candidate.addClickListener(event -> {
                    JobCandidate entity = dataManager.load(JobCandidate.class).id(candidateId).one();
                    screenBuilders.editor(JobCandidate.class, this).editEntity(entity).build().show();
                });
            }
            row.add(candidate);

            addValueCell(row, item.getValue("positionName"), "20%");
            addValueCell(row, item.getValue("sourceVacancy"), "20%");

            Date endDate = item.getValue("endDate");
            addValueCell(row, endDate == null ? "—" : new SimpleDateFormat("dd.MM.yyyy").format(endDate), "12%");

            UUID positionId = item.getValue("positionId");
            int matchCount = positionId == null
                    ? 0
                    : vacanciesByPosition.getOrDefault(positionId, new ArrayList<>()).size();
            Label<String> matches = uiComponents.create(Label.TYPE_STRING);
            matches.setValue(matchCount == 0 ? "Нет совпадений" : matchCount + " ваканс.");
            matches.setWidth("16%");
            matches.setStyleName(matchCount == 0
                    ? "recruiter-reserve-match recruiter-reserve-match-empty"
                    : "recruiter-reserve-match recruiter-reserve-match-positive");
            row.add(matches);

            reserveRows.add(row);
        }
    }

    private void addHeaderCell(HBoxLayout row, String value, String width) {
        Label<String> label = uiComponents.create(Label.TYPE_STRING);
        label.setValue(value);
        label.setWidth(width);
        label.setStyleName("recruiter-reserve-header-cell");
        row.add(label);
    }

    private void addValueCell(HBoxLayout row, Object value, String width) {
        Label<String> label = uiComponents.create(Label.TYPE_STRING);
        label.setValue(value == null ? "—" : String.valueOf(value));
        label.setWidth(width);
        label.setStyleName("recruiter-reserve-cell");
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

    private Date daysAhead(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }
}
