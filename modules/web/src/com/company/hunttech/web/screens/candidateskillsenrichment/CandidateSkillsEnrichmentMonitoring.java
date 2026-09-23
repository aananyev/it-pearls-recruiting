package com.company.hunttech.web.screens.candidateskillsenrichment;

import com.company.hunttech.entity.CandidateCvAnalysisStatus;
import com.company.hunttech.entity.CandidateCvSkillAnalysis;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.service.CandidateSkillEnrichmentService;
import com.company.hunttech.service.dto.CandidateSkillsEnrichmentKpiDto;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@UiController("hunttech_CandidateSkillsEnrichmentMonitoring")
@UiDescriptor("candidate-skills-enrichment-monitoring.xml")
public class CandidateSkillsEnrichmentMonitoring extends Screen {

    private static final Logger log = LoggerFactory.getLogger(CandidateSkillsEnrichmentMonitoring.class);

    @Inject
    private CandidateSkillEnrichmentService enrichmentService;
    @Inject
    private CollectionContainer<CandidateCvSkillAnalysis> analysesDc;
    @Inject
    private CollectionLoader<CandidateCvSkillAnalysis> analysesDl;
    @Inject
    private DataGrid<CandidateCvSkillAnalysis> analysesTable;
    @Inject
    private Notifications notifications;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private UiComponents uiComponents;

    // Header badges and settings
    @Inject
    private Label<String> workerStatusBadge;
    @Inject
    private Label<String> workerRunStateBadge;
    @Inject
    private CheckBox freeOnlyCheckBox;

    // KPI labels
    @Inject
    private Label<String> kpiProgressValueLabel;
    @Inject
    private ProgressBar kpiProgressBar;
    @Inject
    private Label<String> kpiProgressSubLabel;
    @Inject
    private Label<String> kpiSpeedValueLabel;
    @Inject
    private Label<String> kpiEtaSubLabel;
    @Inject
    private Label<String> kpiProcessedTodayLabel;
    @Inject
    private Label<String> kpiVolumeSubLabel;
    @Inject
    private Label<String> kpiErrorsValueLabel;
    @Inject
    private Label<String> kpiStaleSubLabel;
    @Inject
    private Label<String> kpiTokensValueLabel;
    @Inject
    private Label<String> kpiTokensSubLabel;

    // Filter controls
    @Inject
    private RadioButtonGroup<String> statusFilterGroup;
    @Inject
    private TextField<String> candidateSearchField;

    private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM.yyyy");
    private boolean isInitialized = false;

    private static final List<String> FILTER_PARAM_NAMES = Arrays.asList(
            "search", "today", "errStatus", "retryStatus", "past24h", "staleStatus", "freshStatus", "queuedStatuses"
    );

    @Subscribe
    public void onInit(InitEvent event) {
        initStatusFilterOptions();
        initTableColumnRenderers();
        candidateSearchField.addEnterPressListener(e -> reloadTableWithFilter());
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        isInitialized = true;
        refreshDashboard();
    }

    private void initStatusFilterOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Все", "ALL");
        options.put("В очереди", "QUEUED");
        options.put("Сегодня", "TODAY");
        options.put("Ошибки", "ERROR");
        options.put("В повторе", "RETRY");
        options.put("За 24 часа", "LAST_24H");
        options.put("Устаревшие", "STALE");
        options.put("Обработано", "FRESH");
        statusFilterGroup.setOptionsMap(options);
        statusFilterGroup.setValue("ALL");
    }

    private void initTableColumnRenderers() {
        // Рендерер приоритета в очереди
        DataGrid.Column priorityCol = analysesTable.addGeneratedColumn("priority",
                new DataGrid.ColumnGenerator<CandidateCvSkillAnalysis, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<CandidateCvSkillAnalysis> event) {
                        CandidateCvSkillAnalysis item = event.getItem();
                        int p = item.getPriority() != null ? item.getPriority() : 0;
                        if (p >= CandidateSkillEnrichmentService.PRIORITY_HIGH) {
                            return "<span class=\"ai-chip-badge badge-orange bold\">⚡ СРОЧНО</span>";
                        }
                        return "<span class=\"ai-chip-badge\">Обычный</span>";
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });
        if (priorityCol != null) {
            priorityCol.setRenderer(analysesTable.createRenderer(DataGrid.HtmlRenderer.class));
        }

        // Рендерер статуса с красивыми цветными HTML-бейджами
        DataGrid.Column statusCol = analysesTable.addGeneratedColumn("status",
                new DataGrid.ColumnGenerator<CandidateCvSkillAnalysis, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<CandidateCvSkillAnalysis> event) {
                        CandidateCvSkillAnalysis item = event.getItem();
                        CandidateCvAnalysisStatus st = item.getStatus();
                        if (st == null) return "—";
                        switch (st) {
                            case NOT_ANALYZED:
                                int prio = item.getPriority() != null ? item.getPriority() : 0;
                                if (prio >= CandidateSkillEnrichmentService.PRIORITY_HIGH) {
                                    return "<span class=\"ai-chip-badge badge-purple bold\">⚡ В ОЧЕРЕДИ</span>";
                                }
                                return "<span class=\"ai-chip-badge badge-purple\">⏳ В ОЧЕРЕДИ</span>";
                            case FRESH:
                                return "<span class=\"ai-chip-badge badge-green\">✓ FRESH</span>";
                            case PROCESSING:
                                return "<span class=\"ai-chip-badge badge-blue\">⟳ В РАБОТЕ</span>";
                            case RETRY:
                                return "<span class=\"ai-chip-badge badge-orange\">⏳ RETRY</span>";
                            case ERROR:
                                return "<span class=\"ai-chip-badge badge-red\">⚠ ОШИБКА</span>";
                            case STALE:
                                return "<span class=\"ai-chip-badge badge-admin\">♻ STALE</span>";
                            case SKIPPED:
                                return "<span class=\"ai-chip-badge\">SKIPPED</span>";
                            default:
                                return "<span class=\"ai-chip-badge\">" + st.name() + "</span>";
                        }
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });
        if (statusCol != null) {
            statusCol.setRenderer(analysesTable.createRenderer(DataGrid.HtmlRenderer.class));
        }

        // Рендерер даты резюме
        analysesTable.addGeneratedColumn("candidateCvDate",
                new DataGrid.ColumnGenerator<CandidateCvSkillAnalysis, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<CandidateCvSkillAnalysis> event) {
                        CandidateCvSkillAnalysis item = event.getItem();
                        if (item.getCandidateCv() != null && item.getCandidateCv().getDatePost() != null) {
                            return dayFormat.format(item.getCandidateCv().getDatePost());
                        }
                        return "—";
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });

        // Рендерер модели и провайдера
        analysesTable.addGeneratedColumn("providerAndModel",
                new DataGrid.ColumnGenerator<CandidateCvSkillAnalysis, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<CandidateCvSkillAnalysis> event) {
                        return formatProviderAndModel(event.getItem());
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });

        // Клик на строке открывает диалог детальных изменений
        analysesTable.addItemClickListener(e -> {
            if (e.getItem() != null) {
                openDeltaDialog(e.getItem());
            }
        });
    }

    public void refreshDashboard() {
        refreshDashboard(true);
    }

    static String formatProviderAndModel(CandidateCvSkillAnalysis item) {
        if (item == null) {
            return "Метаданные недоступны";
        }
        if (CandidateCvSkillAnalysis.EXECUTION_SOURCE_DICTIONARY_FALLBACK.equals(item.getExecutionSource())) {
            return "Fallback: справочник";
        }
        String provider = trimToEmpty(item.getProviderCode());
        String model = trimToEmpty(item.getModelName());
        if (CandidateCvSkillAnalysis.EXECUTION_SOURCE_AI_METADATA_INCOMPLETE.equals(item.getExecutionSource())) {
            if (provider.isEmpty() && model.isEmpty()) {
                return "AI: модель не зафиксирована";
            }
            return "AI: "
                    + (provider.isEmpty() ? "провайдер не зафиксирован" : provider)
                    + " / "
                    + (model.isEmpty() ? "модель не зафиксирована" : model);
        }
        if (provider.isEmpty() && model.isEmpty()) {
            return "Метаданные недоступны";
        }
        if (provider.isEmpty()) {
            return model;
        }
        if (model.isEmpty()) {
            return provider;
        }
        return provider + " / " + model;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public void refreshDashboard(boolean userInitiated) {
        refreshKpiMetrics(userInitiated);
        if (userInitiated || analysesTable.getSingleSelected() == null) {
            reloadTableWithFilter();
        }
    }

    private void refreshKpiMetrics(boolean userInitiated) {
        try {
            CandidateSkillsEnrichmentKpiDto kpi = enrichmentService.getKpiMetrics();

            // Воркер и настройки
            boolean enabled = kpi.isWorkerEnabled();
            workerStatusBadge.setValue(enabled ? "СЛУЖБА ВКЛЮЧЕНА" : "СЛУЖБА ВЫКЛЮЧЕНА");
            workerStatusBadge.setStyleName(enabled ? "ai-chip-badge badge-green" : "ai-chip-badge badge-red");

            workerRunStateBadge.setValue(kpi.getWorkerStatus());
            workerRunStateBadge.setStyleName(enabled ? "ai-chip-badge badge-blue" : "ai-chip-badge");

            if (freeOnlyCheckBox != null) {
                freeOnlyCheckBox.setValue(enrichmentService.isFreeOnly());
            }

            // Прогресс базы
            long total = kpi.getTotalEligibleCvCount();
            long fresh = kpi.getFreshCount();
            double percent = total > 0 ? (fresh * 100.0 / total) : 0.0;
            kpiProgressValueLabel.setValue(String.format("%d из %d CV (%.1f%%)", fresh, total, percent));
            kpiProgressBar.setValue((double) (Math.min(1.0, total > 0 ? (double) fresh / total : 0.0)));
            long inQueue = kpi.getNotAnalyzedCount() + kpi.getStaleCount() + kpi.getRetryCount();
            kpiProgressSubLabel.setValue(String.format("Ожидает: %d | Устарело: %d", inQueue, kpi.getStaleCount()));

            // Скорость и ETA
            kpiSpeedValueLabel.setValue(String.format("%.1f CV/час", kpi.getProcessingSpeedPerHour()));
            kpiEtaSubLabel.setValue(kpi.getEstimatedEtaText() != null ? kpi.getEstimatedEtaText() : "—");

            // Динамика обработки
            kpiProcessedTodayLabel.setValue(String.format("Сегодня: +%d CV", kpi.getProcessedToday()));
            kpiVolumeSubLabel.setValue(String.format("За 24ч: %d | За 7 дней: %d", kpi.getProcessed24h(), kpi.getProcessed7d()));

            // Ошибки
            kpiErrorsValueLabel.setValue(String.format("Ошибок: %d | В повторе: %d", kpi.getErrorCount(), kpi.getRetryCount()));
            kpiStaleSubLabel.setValue(String.format("Устарело (STALE): %d", kpi.getStaleCount()));

            // AI-токены
            kpiTokensValueLabel.setValue(String.format("%,d токенов", kpi.getTotalTokensToday()));
            kpiTokensSubLabel.setValue(String.format("Запросов сегодня: %d", kpi.getAiRequestsToday()));

        } catch (Exception e) {
            log.warn("Не удалось обновить KPI метрики обогащения навыков: {}", e.getMessage());
            if (userInitiated) {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption("Ошибка получения KPI")
                        .withDescription(e.getMessage())
                        .show();
            }
        }
    }

    private void reloadTableWithFilter() {
        String filterValue = statusFilterGroup.getValue();
        if (filterValue == null) filterValue = "ALL";

        StringBuilder jpql = new StringBuilder("select e from hunttech_CandidateCvSkillAnalysis e where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        String search = candidateSearchField.getValue();
        if (search != null && !search.trim().isEmpty()) {
            jpql.append("and lower(e.candidate.fullName) like :search ");
            params.put("search", "%" + search.trim().toLowerCase(Locale.ROOT) + "%");
        }

        switch (filterValue) {
            case "QUEUED":
                // В очереди: новые ожидания анализа, устаревшие и запланированные повторы (согласно KPI панели)
                jpql.append("and e.status in :queuedStatuses ");
                params.put("queuedStatuses", Arrays.asList(
                        CandidateCvAnalysisStatus.NOT_ANALYZED.getId(),
                        CandidateCvAnalysisStatus.STALE.getId(),
                        CandidateCvAnalysisStatus.RETRY.getId()
                ));
                break;
            case "TODAY":
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                jpql.append("and e.processingStartedAt >= :today ");
                params.put("today", cal.getTime());
                break;
            case "ERROR":
                jpql.append("and e.status = :errStatus ");
                params.put("errStatus", CandidateCvAnalysisStatus.ERROR.getId());
                break;
            case "RETRY":
                jpql.append("and e.status = :retryStatus ");
                params.put("retryStatus", CandidateCvAnalysisStatus.RETRY.getId());
                break;
            case "LAST_24H":
                Date past24h = new Date(System.currentTimeMillis() - 24L * 3600_000L);
                jpql.append("and e.processingStartedAt >= :past24h ");
                params.put("past24h", past24h);
                break;
            case "STALE":
                jpql.append("and e.status = :staleStatus ");
                params.put("staleStatus", CandidateCvAnalysisStatus.STALE.getId());
                break;
            case "FRESH":
                jpql.append("and e.status = :freshStatus ");
                params.put("freshStatus", CandidateCvAnalysisStatus.FRESH.getId());
                break;
            default:
                // ALL
                break;
        }

        jpql.append("order by (case when e.status in (10, 20) then 0 else 1 end), coalesce(e.priority, 0) desc, e.createTs desc, e.processingStartedAt desc nulls last");

        for (String p : FILTER_PARAM_NAMES) {
            analysesDl.removeParameter(p);
        }

        analysesDl.setQuery(jpql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            analysesDl.setParameter(entry.getKey(), entry.getValue());
        }
        analysesDl.load();
    }

    @Subscribe("refreshTimer")
    public void onRefreshTimerTimerAction(com.haulmont.cuba.gui.components.Timer.TimerActionEvent event) {
        refreshDashboard(false);
    }

    @Subscribe("freeOnlyCheckBox")
    public void onFreeOnlyCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (!isInitialized) return;
        boolean val = Boolean.TRUE.equals(event.getValue());
        enrichmentService.setFreeOnly(val);
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption(val ? "Режим: только бесплатные нейросети" : "Режим: все доступные нейросети")
                .withDescription(val ? "Фоновый воркер будет обращаться исключительно к моделям с бесплатным тарифом"
                        : "Разрешено использование корпоративных моделей с тарификацией")
                .show();
    }

    @Subscribe("startWorkerBtn")
    public void onStartWorkerBtnClick(Button.ClickEvent event) {
        enrichmentService.setWorkerEnabled(true);
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Фоновый воркер запущен")
                .withDescription("Служба CandidateSkillsEnrichmentWorker активна и обрабатывает резюме кандидатов")
                .show();
        refreshDashboard();
    }

    @Subscribe("stopWorkerBtn")
    public void onStopWorkerBtnClick(Button.ClickEvent event) {
        enrichmentService.setWorkerEnabled(false);
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Фоновый воркер остановлен")
                .withDescription("Новые задачи в обработку не берутся. Текущий запрос корректно завершится")
                .show();
        refreshDashboard();
    }

    @Subscribe("runOnceBtn")
    public void onRunOnceBtnClick(Button.ClickEvent event) {
        enrichmentService.runWorkerCycleNow();
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Шаг воркера запущен")
                .withDescription("Запущен один цикл обработки резюме из очереди")
                .show();
        refreshDashboard();
    }

    @Subscribe("refreshKpiBtn")
    public void onRefreshKpiBtnClick(Button.ClickEvent event) {
        refreshDashboard();
    }

    @Subscribe("refreshTableBtn")
    public void onRefreshTableBtnClick(Button.ClickEvent event) {
        reloadTableWithFilter();
    }

    @Subscribe("statusFilterGroup")
    public void onStatusFilterGroupValueChange(HasValue.ValueChangeEvent<String> event) {
        if (isInitialized) {
            reloadTableWithFilter();
        }
    }

    @Subscribe("viewDeltaBtn")
    public void onViewDeltaBtnClick(Button.ClickEvent event) {
        CandidateCvSkillAnalysis selected = analysesTable.getSingleSelected();
        if (selected == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Внимание")
                    .withDescription("Выберите запись в таблице для просмотра деталей изменений")
                    .show();
            return;
        }
        openDeltaDialog(selected);
    }

    private void openDeltaDialog(CandidateCvSkillAnalysis analysis) {
        CandidateSkillDeltaDialog dialog = screenBuilders.screen(this)
                .withScreenClass(CandidateSkillDeltaDialog.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        dialog.initData(analysis);
        dialog.show();
    }

    @Subscribe("retryErrorBtn")
    public void onRetryErrorBtnClick(Button.ClickEvent event) {
        CandidateCvSkillAnalysis selected = analysesTable.getSingleSelected();
        if (selected == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Внимание")
                    .withDescription("Выберите строку для повторной отправки на анализ")
                    .show();
            return;
        }

        if (selected.getCandidateCv() == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Внимание")
                    .withDescription("У выбранной записи отсутствует резюме")
                    .show();
            return;
        }

        enrichmentService.reprocessCv(selected.getCandidateCv().getId());
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Повтор запланирован")
                .withDescription("CV возвращено в очередь обработки (NOT_ANALYZED)")
                .show();
        refreshDashboard();
    }

    @Subscribe("openCandidateBtn")
    public void onOpenCandidateBtnClick(Button.ClickEvent event) {
        CandidateCvSkillAnalysis selected = analysesTable.getSingleSelected();
        if (selected == null || selected.getCandidate() == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Внимание")
                    .withDescription("Выберите запись в таблице")
                    .show();
            return;
        }
        screenBuilders.editor(JobCandidate.class, this)
                .editEntity(selected.getCandidate())
                .withOpenMode(OpenMode.NEW_TAB)
                .show();
    }
}
