package com.company.hunttech.web.screens.aidashboard;

import com.company.hunttech.entity.ai.AiCallLog;
import com.haulmont.charts.gui.components.charts.PieChart;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.ListDataProvider;
import com.haulmont.charts.gui.data.MapDataItem;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

@UiController("hunttech_UserAiDashboard")
@UiDescriptor("user-ai-dashboard.xml")
public class UserAiDashboard extends Screen {

    private static final String PERIOD_TODAY = "today";
    private static final String PERIOD_7_DAYS = "7days";
    private static final String PERIOD_MONTH = "month";
    private static final String PERIOD_CUSTOM = "custom";

    @Inject
    private DataManager dataManager;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private UiComponents uiComponents;

    @Inject
    private LookupField<String> periodLookup;
    @Inject
    private DateField<Date> dateFromField;
    @Inject
    private DateField<Date> dateToField;
    @Inject
    private Label<String> totalCallsLabel;
    @Inject
    private Label<String> totalCallsSubLabel;
    @Inject
    private Label<String> tokensLabel;
    @Inject
    private Label<String> tokensSubLabel;
    @Inject
    private Label<String> costLabel;
    @Inject
    private Label<String> costSubLabel;
    @Inject
    private Label<String> speedLabel;
    @Inject
    private Label<String> successRateLabel;

    @Inject
    private SerialChart dynamicsChart;
    @Inject
    private PieChart functionPieChart;
    @Inject
    private Table<AiCallLog> recentCallsTable;
    @Inject
    private CollectionContainer<AiCallLog> recentCallsDc;
    @Inject
    private CollectionLoader<AiCallLog> recentCallsDl;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM");

    @Subscribe
    public void onInit(InitEvent event) {
        initPeriodLookup();
        initTableColumns();
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        applyPeriod(PERIOD_7_DAYS);
        refreshData();
    }

    private void initPeriodLookup() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Сегодня", PERIOD_TODAY);
        options.put("Последние 7 дней", PERIOD_7_DAYS);
        options.put("Текущий месяц", PERIOD_MONTH);
        options.put("Произвольный период", PERIOD_CUSTOM);
        periodLookup.setOptionsMap(options);
        periodLookup.setValue(PERIOD_7_DAYS);

        periodLookup.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                applyPeriod(e.getValue());
                if (!PERIOD_CUSTOM.equals(e.getValue())) {
                    refreshData();
                }
            }
        });
    }

    private void applyPeriod(String period) {
        Calendar cal = Calendar.getInstance();
        Date to = cal.getTime();
        Date from;

        switch (period) {
            case PERIOD_TODAY:
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                from = cal.getTime();
                break;
            case PERIOD_MONTH:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                from = cal.getTime();
                break;
            case PERIOD_7_DAYS:
            default:
                cal.add(Calendar.DAY_OF_MONTH, -6);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                from = cal.getTime();
                break;
        }

        dateFromField.setValue(from);
        dateToField.setValue(to);
        boolean custom = PERIOD_CUSTOM.equals(period);
        dateFromField.setEnabled(custom);
        dateToField.setEnabled(custom);
    }

    @Subscribe("refreshBtn")
    public void onRefreshBtnClick(Button.ClickEvent event) {
        refreshData();
    }

    private void initTableColumns() {
        recentCallsTable.addGeneratedColumn("tokensDisplay", log -> {
            Label<String> label = uiComponents.create(Label.NAME);
            label.setHtmlEnabled(true);
            int prompt = log.getPromptTokens() != null ? log.getPromptTokens() : 0;
            int completion = log.getCompletionTokens() != null ? log.getCompletionTokens() : 0;
            int total = log.getTotalTokens() != null ? log.getTotalTokens() : (prompt + completion);
            if (total > 0) {
                label.setValue("<span style='font-size: 11px;'><b>" + total + "</b> (" + prompt + "/" + completion + ")</span>");
            } else {
                label.setValue("—");
            }
            return label;
        });

        recentCallsTable.addGeneratedColumn("costDisplay", log -> {
            Label<String> label = uiComponents.create(Label.NAME);
            label.setHtmlEnabled(true);
            BigDecimal cost = log.getEstimatedCost();
            String curr = log.getCurrency() != null ? log.getCurrency() : "USD";
            if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
                label.setValue("<span style='color: #27ae60; font-weight: 600; font-size: 11px;'>"
                        + cost.toPlainString() + " " + curr + "</span>");
            } else {
                label.setValue("—");
            }
            return label;
        });

        recentCallsTable.addGeneratedColumn("durationDisplay", log -> {
            Label<String> label = uiComponents.create(Label.NAME);
            Long ms = log.getDurationMs();
            label.setValue(ms != null ? String.format("%.2f с", ms / 1000.0) : "—");
            return label;
        });

        recentCallsTable.addGeneratedColumn("statusDisplay", log -> {
            Label<String> label = uiComponents.create(Label.NAME);
            label.setHtmlEnabled(true);
            String st = log.getStatus();
            if ("SUCCESS".equalsIgnoreCase(st)) {
                label.setValue("<span style='background: rgba(39, 174, 96, 0.15); color: #27ae60; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px;'>OK</span>");
            } else if ("ERROR".equalsIgnoreCase(st)) {
                label.setValue("<span style='background: rgba(231, 76, 60, 0.15); color: #e74c3c; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px;'>ERROR</span>");
            } else {
                label.setValue(st != null ? st : "—");
            }
            return label;
        });
    }

    private void refreshData() {
        User currentUser = userSessionSource.getUserSession().getUser();
        Date from = dateFromField.getValue();
        Date to = dateToField.getValue();
        if (from == null) from = new Date(0);
        if (to == null) to = new Date();

        // Include the entire end day
        Calendar toCal = Calendar.getInstance();
        toCal.setTime(to);
        toCal.set(Calendar.HOUR_OF_DAY, 23);
        toCal.set(Calendar.MINUTE, 59);
        toCal.set(Calendar.SECOND, 59);
        Date toInclusive = toCal.getTime();

        List<AiCallLog> logs = dataManager.load(AiCallLog.class)
                .query("select e from hunttech_AiCallLog e where e.user = :user and e.callTime >= :from and e.callTime <= :to order by e.callTime asc")
                .parameter("user", currentUser)
                .parameter("from", from)
                .parameter("to", toInclusive)
                .view("ai-call-log-browse-view")
                .list();

        // 1. Update KPI
        int totalCalls = logs.size();
        int successCalls = 0;
        int errorCalls = 0;
        long totalPromptTokens = 0;
        long totalCompletionTokens = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        long totalDurationMs = 0;

        for (AiCallLog log : logs) {
            if ("SUCCESS".equalsIgnoreCase(log.getStatus())) {
                successCalls++;
            } else {
                errorCalls++;
            }
            if (log.getPromptTokens() != null) totalPromptTokens += log.getPromptTokens();
            if (log.getCompletionTokens() != null) totalCompletionTokens += log.getCompletionTokens();
            if (log.getEstimatedCost() != null) totalCost = totalCost.add(log.getEstimatedCost());
            if (log.getDurationMs() != null) totalDurationMs += log.getDurationMs();
        }

        long totalTokens = totalPromptTokens + totalCompletionTokens;
        double avgSpeed = totalCalls > 0 ? (totalDurationMs / (double) totalCalls) / 1000.0 : 0.0;
        double successRate = totalCalls > 0 ? (successCalls * 100.0 / totalCalls) : 100.0;

        totalCallsLabel.setValue(String.valueOf(totalCalls));
        totalCallsSubLabel.setValue(successCalls + " успешно / " + errorCalls + " сбоев");

        tokensLabel.setValue(formatTokenCount(totalTokens));
        tokensSubLabel.setValue(formatTokenCount(totalPromptTokens) + " in / " + formatTokenCount(totalCompletionTokens) + " out");

        costLabel.setValue("$ " + totalCost.setScale(4, RoundingMode.HALF_UP).toPlainString());
        costSubLabel.setValue(totalCost.compareTo(BigDecimal.ZERO) > 0 ? "~" + totalCost.multiply(BigDecimal.valueOf(92.0)).setScale(2, RoundingMode.HALF_UP) + " ₽" : "—");

        speedLabel.setValue(String.format("%.2f с", avgSpeed));
        successRateLabel.setValue(String.format("%.1f%% успешных", successRate));

        // 2. Dynamics chart (by day)
        Map<String, int[]> dayStats = new LinkedHashMap<>();
        Calendar cur = Calendar.getInstance();
        cur.setTime(from);
        while (!cur.getTime().after(toInclusive)) {
            String dStr = dateFormat.format(cur.getTime());
            dayStats.put(dStr, new int[]{0, 0});
            cur.add(Calendar.DAY_OF_MONTH, 1);
        }

        for (AiCallLog log : logs) {
            if (log.getCallTime() != null) {
                String dStr = dateFormat.format(log.getCallTime());
                int[] st = dayStats.computeIfAbsent(dStr, k -> new int[]{0, 0});
                if ("SUCCESS".equalsIgnoreCase(log.getStatus())) {
                    st[0]++;
                } else {
                    st[1]++;
                }
            }
        }

        ListDataProvider dynamicsProvider = new ListDataProvider();
        for (Map.Entry<String, int[]> entry : dayStats.entrySet()) {
            MapDataItem item = new MapDataItem();
            item.add("date", entry.getKey());
            item.add("success", entry.getValue()[0]);
            item.add("error", entry.getValue()[1]);
            dynamicsProvider.addItem(item);
        }
        dynamicsChart.setDataProvider(dynamicsProvider);

        // 3. Function share pie chart
        Map<String, Integer> funcCounts = new HashMap<>();
        for (AiCallLog log : logs) {
            String fName = log.getFunctionName() != null ? log.getFunctionName() : (log.getFunctionCode() != null ? log.getFunctionCode() : "Прочее");
            funcCounts.put(fName, funcCounts.getOrDefault(fName, 0) + 1);
        }

        ListDataProvider funcProvider = new ListDataProvider();
        for (Map.Entry<String, Integer> entry : funcCounts.entrySet()) {
            MapDataItem item = new MapDataItem();
            item.add("functionName", entry.getKey());
            item.add("count", entry.getValue());
            funcProvider.addItem(item);
        }
        functionPieChart.setDataProvider(funcProvider);

        // 4. Update recent calls table loader
        recentCallsDl.setParameter("currentUser", currentUser);
        recentCallsDl.load();
    }

    private String formatTokenCount(long count) {
        if (count >= 1_000_000) {
            return String.format("%.2fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
