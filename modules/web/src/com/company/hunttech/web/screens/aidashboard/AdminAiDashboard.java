package com.company.hunttech.web.screens.aidashboard;

import com.company.hunttech.entity.ai.AiCallLog;
import com.haulmont.charts.gui.components.charts.PieChart;
import com.haulmont.charts.gui.components.charts.SerialChart;
import com.haulmont.charts.gui.data.ListDataProvider;
import com.haulmont.charts.gui.data.MapDataItem;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.model.KeyValueCollectionContainer;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@UiController("hunttech_AdminAiDashboard")
@UiDescriptor("admin-ai-dashboard.xml")
public class AdminAiDashboard extends Screen {

    private static final String PERIOD_TODAY = "today";
    private static final String PERIOD_7_DAYS = "7days";
    private static final String PERIOD_MONTH = "month";
    private static final String PERIOD_CUSTOM = "custom";

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private UiComponents uiComponents;

    @Inject
    private LookupField<String> periodLookup;
    @Inject
    private DateField<Date> dateFromField;
    @Inject
    private DateField<Date> dateToField;
    @Inject
    private LookupPickerField<User> userPicker;
    @Inject
    private LookupField<String> providerLookup;
    @Inject
    private LookupField<String> ownerLookup;

    @Inject
    private Label<String> totalSpendLabel;
    @Inject
    private Label<String> spendSubLabel;
    @Inject
    private Label<String> activeUsersLabel;
    @Inject
    private Label<String> topSpenderLabel;
    @Inject
    private Label<String> totalCallsLabel;
    @Inject
    private Label<String> totalTokensLabel;
    @Inject
    private Label<String> errorRateLabel;
    @Inject
    private Label<String> avgLatencyLabel;

    @Inject
    private SerialChart costDynamicsChart;
    @Inject
    private SerialChart topUsersChart;
    @Inject
    private PieChart functionCostChart;
    @Inject
    private SerialChart modelLatencyChart;

    @Inject
    private Table<KeyValueEntity> userSummaryTable;
    @Inject
    private KeyValueCollectionContainer userSummaryDc;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM");
    private final SimpleDateFormat fullDateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    @Subscribe
    public void onInit(InitEvent event) {
        initFilters();
        initTableColumns();
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        applyPeriod(PERIOD_MONTH);
        refreshData();
    }

    private void initFilters() {
        Map<String, String> periodOptions = new LinkedHashMap<>();
        periodOptions.put("Сегодня", PERIOD_TODAY);
        periodOptions.put("Последние 7 дней", PERIOD_7_DAYS);
        periodOptions.put("Текущий месяц", PERIOD_MONTH);
        periodOptions.put("Произвольный период", PERIOD_CUSTOM);
        periodLookup.setOptionsMap(periodOptions);
        periodLookup.setValue(PERIOD_MONTH);

        periodLookup.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                applyPeriod(e.getValue());
                if (!PERIOD_CUSTOM.equals(e.getValue())) {
                    refreshData();
                }
            }
        });

        Map<String, String> providerOptions = new LinkedHashMap<>();
        providerOptions.put("Все провайдеры", "");
        providerOptions.put("OpenAI", "openai");
        providerOptions.put("DeepSeek", "deepseek");
        providerOptions.put("Anthropic", "anthropic");
        providerOptions.put("GigaChat", "gigachat");
        providerOptions.put("YandexGPT", "yandexgpt");
        providerLookup.setOptionsMap(providerOptions);
        providerLookup.setValue("");

        Map<String, String> ownerOptions = new LinkedHashMap<>();
        ownerOptions.put("Все типы ключей", "");
        ownerOptions.put("Корпоративный (Admin)", "ADMIN");
        ownerOptions.put("Личный (User)", "USER");
        ownerLookup.setOptionsMap(ownerOptions);
        ownerLookup.setValue("");
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
            case PERIOD_7_DAYS:
                cal.add(Calendar.DAY_OF_MONTH, -6);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                from = cal.getTime();
                break;
            case PERIOD_MONTH:
            default:
                cal.set(Calendar.DAY_OF_MONTH, 1);
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

    @Subscribe("applyFilterBtn")
    public void onApplyFilterBtnClick(Button.ClickEvent event) {
        refreshData();
    }

    @Subscribe("resetFilterBtn")
    public void onResetFilterBtnClick(Button.ClickEvent event) {
        userPicker.setValue(null);
        providerLookup.setValue("");
        ownerLookup.setValue("");
        periodLookup.setValue(PERIOD_MONTH);
        applyPeriod(PERIOD_MONTH);
        refreshData();
    }

    private void initTableColumns() {
        userSummaryTable.addGeneratedColumn("estimatedCost", kve -> {
            Label<String> label = uiComponents.create(Label.NAME);
            label.setHtmlEnabled(true);
            BigDecimal cost = kve.getValue("estimatedCost");
            if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
                label.setValue("<span style='color: #27ae60; font-weight: 600;'>$ " + cost.setScale(4, RoundingMode.HALF_UP).toPlainString() + "</span>");
            } else {
                label.setValue("$ 0.00");
            }
            return label;
        });

        userSummaryTable.addGeneratedColumn("errorCount", kve -> {
            Label<String> label = uiComponents.create(Label.NAME);
            label.setHtmlEnabled(true);
            Integer errors = kve.getValue("errorCount");
            int err = errors != null ? errors : 0;
            if (err > 0) {
                label.setValue("<span style='color: #e74c3c; font-weight: 600;'>" + err + "</span>");
            } else {
                label.setValue("<span style='color: #27ae60;'>0</span>");
            }
            return label;
        });

        userSummaryTable.addGeneratedColumn("lastCallTime", kve -> {
            Label<String> label = uiComponents.create(Label.NAME);
            Date d = kve.getValue("lastCallTime");
            label.setValue(d != null ? fullDateTimeFormat.format(d) : "—");
            return label;
        });
    }

    private void refreshData() {
        Date from = dateFromField.getValue();
        Date to = dateToField.getValue();
        if (from == null) from = new Date(0);
        if (to == null) to = new Date();

        Calendar toCal = Calendar.getInstance();
        toCal.setTime(to);
        toCal.set(Calendar.HOUR_OF_DAY, 23);
        toCal.set(Calendar.MINUTE, 59);
        toCal.set(Calendar.SECOND, 59);
        Date toInclusive = toCal.getTime();

        User filterUser = userPicker.getValue();
        String filterProvider = providerLookup.getValue();
        String filterOwner = ownerLookup.getValue();

        StringBuilder query = new StringBuilder("select e from hunttech_AiCallLog e where e.callTime >= :from and e.callTime <= :to ");
        Map<String, Object> params = new HashMap<>();
        params.put("from", from);
        params.put("to", toInclusive);

        if (filterUser != null) {
            query.append("and e.user = :filterUser ");
            params.put("filterUser", filterUser);
        }
        if (filterProvider != null && !filterProvider.trim().isEmpty()) {
            query.append("and e.providerCode = :filterProvider ");
            params.put("filterProvider", filterProvider);
        }
        if (filterOwner != null && !filterOwner.trim().isEmpty()) {
            query.append("and e.credentialOwner = :filterOwner ");
            params.put("filterOwner", filterOwner);
        }
        query.append("order by e.callTime asc");

        FluentLoader.ByQuery<AiCallLog, UUID> loader = dataManager.load(AiCallLog.class).query(query.toString()).view("ai-call-log-browse-view");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            loader.parameter(entry.getKey(), entry.getValue());
        }
        List<AiCallLog> logs = loader.list();

        // 1. KPI Calculation
        int totalCalls = logs.size();
        int errorCalls = 0;
        long totalPromptTokens = 0;
        long totalCompletionTokens = 0;
        BigDecimal totalSpend = BigDecimal.ZERO;
        BigDecimal adminSpend = BigDecimal.ZERO;
        BigDecimal userSpend = BigDecimal.ZERO;
        long totalLatencyMs = 0;

        Set<String> activeUsers = new HashSet<>();
        Map<String, BigDecimal> userSpendMap = new HashMap<>();
        Map<String, KeyValueEntity> userSummaries = new LinkedHashMap<>();
        Map<String, BigDecimal> functionSpendMap = new HashMap<>();
        Map<String, long[]> modelLatencyMap = new HashMap<>(); // [totalMs, count]

        for (AiCallLog log : logs) {
            if ("ERROR".equalsIgnoreCase(log.getStatus())) {
                errorCalls++;
            }
            if (log.getPromptTokens() != null) totalPromptTokens += log.getPromptTokens();
            if (log.getCompletionTokens() != null) totalCompletionTokens += log.getCompletionTokens();
            if (log.getDurationMs() != null) totalLatencyMs += log.getDurationMs();

            BigDecimal cost = log.getEstimatedCost() != null ? log.getEstimatedCost() : BigDecimal.ZERO;
            totalSpend = totalSpend.add(cost);

            if ("ADMIN".equalsIgnoreCase(log.getCredentialOwner())) {
                adminSpend = adminSpend.add(cost);
            } else if ("USER".equalsIgnoreCase(log.getCredentialOwner())) {
                userSpend = userSpend.add(cost);
            }

            String uKey = log.getUserName() != null ? log.getUserName() : (log.getUserLogin() != null ? log.getUserLogin() : "Система");
            activeUsers.add(uKey);
            userSpendMap.put(uKey, userSpendMap.getOrDefault(uKey, BigDecimal.ZERO).add(cost));

            // User Summary KeyValueEntity
            KeyValueEntity kve = userSummaries.computeIfAbsent(uKey, k -> {
                KeyValueEntity e = metadata.create(KeyValueEntity.class);
                e.setValue("userName", k);
                e.setValue("totalCalls", 0);
                e.setValue("promptTokens", 0L);
                e.setValue("completionTokens", 0L);
                e.setValue("totalTokens", 0L);
                e.setValue("estimatedCost", BigDecimal.ZERO);
                e.setValue("errorCount", 0);
                return e;
            });

            kve.setValue("totalCalls", ((Integer) kve.getValue("totalCalls")) + 1);
            long pTok = kve.getValue("promptTokens");
            long cTok = kve.getValue("completionTokens");
            if (log.getPromptTokens() != null) pTok += log.getPromptTokens();
            if (log.getCompletionTokens() != null) cTok += log.getCompletionTokens();
            kve.setValue("promptTokens", pTok);
            kve.setValue("completionTokens", cTok);
            kve.setValue("totalTokens", pTok + cTok);

            BigDecimal prevCost = kve.getValue("estimatedCost");
            kve.setValue("estimatedCost", prevCost.add(cost));

            if ("ERROR".equalsIgnoreCase(log.getStatus())) {
                kve.setValue("errorCount", ((Integer) kve.getValue("errorCount")) + 1);
            }
            Date prevDate = kve.getValue("lastCallTime");
            if (log.getCallTime() != null && (prevDate == null || log.getCallTime().after(prevDate))) {
                kve.setValue("lastCallTime", log.getCallTime());
            }

            // Function spend
            String fName = log.getFunctionName() != null ? log.getFunctionName() : (log.getFunctionCode() != null ? log.getFunctionCode() : "Прочее");
            functionSpendMap.put(fName, functionSpendMap.getOrDefault(fName, BigDecimal.ZERO).add(cost));

            // Model stats
            String mName = log.getModelName() != null ? log.getModelName() : (log.getProviderCode() != null ? log.getProviderCode() : "unknown");
            long[] mStat = modelLatencyMap.computeIfAbsent(mName, k -> new long[]{0, 0});
            if (log.getDurationMs() != null) mStat[0] += log.getDurationMs();
            mStat[1]++;
        }

        long totalTokens = totalPromptTokens + totalCompletionTokens;
        double errorRate = totalCalls > 0 ? (errorCalls * 100.0 / totalCalls) : 0.0;
        double avgLatency = totalCalls > 0 ? (totalLatencyMs / (double) totalCalls) / 1000.0 : 0.0;

        // Top spender
        String topSpender = userSpendMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " ($" + e.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString() + ")")
                .orElse("—");

        totalSpendLabel.setValue("$ " + totalSpend.setScale(2, RoundingMode.HALF_UP).toPlainString());
        spendSubLabel.setValue("Corp: $" + adminSpend.setScale(2, RoundingMode.HALF_UP) + " | User: $" + userSpend.setScale(2, RoundingMode.HALF_UP));

        activeUsersLabel.setValue(String.valueOf(activeUsers.size()));
        topSpenderLabel.setValue("Топ: " + topSpender);

        totalCallsLabel.setValue(String.valueOf(totalCalls));
        totalTokensLabel.setValue(formatTokenCount(totalTokens) + " токенов");

        errorRateLabel.setValue(String.format("%.1f%% ошибок", errorRate));
        avgLatencyLabel.setValue(String.format("Ср. задержка: %.2f с", avgLatency));

        // 2. Cost Dynamics Chart
        Map<String, BigDecimal> dayCostStats = new LinkedHashMap<>();
        Calendar cur = Calendar.getInstance();
        cur.setTime(from);
        while (!cur.getTime().after(toInclusive)) {
            dayCostStats.put(dateFormat.format(cur.getTime()), BigDecimal.ZERO);
            cur.add(Calendar.DAY_OF_MONTH, 1);
        }
        for (AiCallLog log : logs) {
            if (log.getCallTime() != null) {
                String dStr = dateFormat.format(log.getCallTime());
                BigDecimal c = log.getEstimatedCost() != null ? log.getEstimatedCost() : BigDecimal.ZERO;
                dayCostStats.put(dStr, dayCostStats.getOrDefault(dStr, BigDecimal.ZERO).add(c));
            }
        }
        ListDataProvider costProvider = new ListDataProvider();
        for (Map.Entry<String, BigDecimal> entry : dayCostStats.entrySet()) {
            MapDataItem item = new MapDataItem();
            item.add("date", entry.getKey());
            item.add("cost", entry.getValue().setScale(4, RoundingMode.HALF_UP).doubleValue());
            costProvider.addItem(item);
        }
        costDynamicsChart.setDataProvider(costProvider);

        // 3. Top Users Chart (by Spend)
        List<Map.Entry<String, BigDecimal>> topUsersList = userSpendMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(8)
                .collect(Collectors.toList());

        ListDataProvider topUsersProvider = new ListDataProvider();
        for (Map.Entry<String, BigDecimal> entry : topUsersList) {
            MapDataItem item = new MapDataItem();
            item.add("userName", entry.getKey());
            item.add("cost", entry.getValue().setScale(4, RoundingMode.HALF_UP).doubleValue());
            topUsersProvider.addItem(item);
        }
        topUsersChart.setDataProvider(topUsersProvider);

        // 4. Function Spend Pie Chart
        ListDataProvider funcSpendProvider = new ListDataProvider();
        for (Map.Entry<String, BigDecimal> entry : functionSpendMap.entrySet()) {
            MapDataItem item = new MapDataItem();
            item.add("functionName", entry.getKey());
            item.add("cost", entry.getValue().setScale(4, RoundingMode.HALF_UP).doubleValue());
            funcSpendProvider.addItem(item);
        }
        functionCostChart.setDataProvider(funcSpendProvider);

        // 5. Model Latency Chart
        ListDataProvider modelProvider = new ListDataProvider();
        for (Map.Entry<String, long[]> entry : modelLatencyMap.entrySet()) {
            MapDataItem item = new MapDataItem();
            item.add("modelName", entry.getKey());
            double lat = entry.getValue()[1] > 0 ? (entry.getValue()[0] / (double) entry.getValue()[1]) / 1000.0 : 0.0;
            item.add("latency", BigDecimal.valueOf(lat).setScale(2, RoundingMode.HALF_UP).doubleValue());
            modelProvider.addItem(item);
        }
        modelLatencyChart.setDataProvider(modelProvider);

        // 6. User Summary Table
        List<KeyValueEntity> summaryList = new ArrayList<>(userSummaries.values());
        summaryList.sort((a, b) -> {
            BigDecimal cA = a.getValue("estimatedCost");
            BigDecimal cB = b.getValue("estimatedCost");
            return cB.compareTo(cA);
        });
        userSummaryDc.getMutableItems().clear();
        userSummaryDc.getMutableItems().addAll(summaryList);
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
