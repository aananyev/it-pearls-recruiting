package com.company.hunttech.web.screens.candidatecontactsenrichment;

import com.company.hunttech.entity.CandidateCvAnalysisStatus;
import com.company.hunttech.entity.CandidateCvContactAnalysis;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.service.CandidateContactEnrichmentService;
import com.company.hunttech.service.dto.CandidateContactsEnrichmentKpiDto;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.ProgressBar;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.Timer;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@UiController("hunttech_CandidateContactsEnrichmentMonitoring")
@UiDescriptor("candidate-contacts-enrichment-monitoring.xml")
public class CandidateContactsEnrichmentMonitoring extends Screen {

    @Inject
    private CandidateContactEnrichmentService enrichmentService;
    @Inject
    private Messages messages;
    @Inject
    private CollectionContainer<CandidateCvContactAnalysis> analysesDc;
    @Inject
    private CollectionLoader<CandidateCvContactAnalysis> analysesDl;
    @Inject
    private DataGrid<CandidateCvContactAnalysis> analysesTable;
    @Inject
    private Notifications notifications;
    @Inject
    private ScreenBuilders screenBuilders;

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
    private volatile boolean isRefreshing = false;

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
        // Рендерер приоритета
        DataGrid.Column priorityCol = analysesTable.addGeneratedColumn("priority",
                new DataGrid.ColumnGenerator<CandidateCvContactAnalysis, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<CandidateCvContactAnalysis> event) {
                        CandidateCvContactAnalysis item = event.getItem();
                        int p = item.getPriority() != null ? item.getPriority() : 0;
                        if (p >= CandidateContactEnrichmentService.PRIORITY_HIGH) {
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

        // Рендерер статуса
        DataGrid.Column statusCol = analysesTable.addGeneratedColumn("status",
                new DataGrid.ColumnGenerator<CandidateCvContactAnalysis, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<CandidateCvContactAnalysis> event) {
                        CandidateCvContactAnalysis item = event.getItem();
                        CandidateCvAnalysisStatus st = item.getStatus();
                        if (st == null) return "—";
                        switch (st) {
                            case NOT_ANALYZED:
                                int prio = item.getPriority() != null ? item.getPriority() : 0;
                                if (prio >= CandidateContactEnrichmentService.PRIORITY_HIGH) {
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

        // Рендерер фото
        DataGrid.Column photoCol = analysesTable.addGeneratedColumn("photoStatus",
                new DataGrid.ColumnGenerator<CandidateCvContactAnalysis, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<CandidateCvContactAnalysis> event) {
                        CandidateCvContactAnalysis item = event.getItem();
                        if (Boolean.TRUE.equals(item.getPhotoExtracted())) {
                            return "<span class=\"ai-chip-badge badge-green\">📷 ЕСТЬ</span>";
                        }
                        return "<span style=\"color:#888;\">—</span>";
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });
        if (photoCol != null) {
            photoCol.setRenderer(analysesTable.createRenderer(DataGrid.HtmlRenderer.class));
        }

        // Рендерер даты резюме
        analysesTable.addGeneratedColumn("candidateCvDate",
                new DataGrid.ColumnGenerator<CandidateCvContactAnalysis, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<CandidateCvContactAnalysis> event) {
                        CandidateCvContactAnalysis item = event.getItem();
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
                new DataGrid.ColumnGenerator<CandidateCvContactAnalysis, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<CandidateCvContactAnalysis> event) {
                        return formatProviderAndModel(event.getItem());
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });

        analysesTable.addItemClickListener(e -> {
            if (e.getItem() != null) {
                openDeltaDialog(e.getItem());
            }
        });
    }

    public void refreshDashboard() {
        refreshDashboard(true);
    }

    public void refreshDashboard(boolean reloadTable) {
        if (isRefreshing) {
            return;
        }
        isRefreshing = true;
        try {
            freeOnlyCheckBox.setValue(enrichmentService.isFreeOnly());

            CandidateContactsEnrichmentKpiDto kpi = enrichmentService.getKpiMetrics();

            // 1. Верхние бейджи
            if (kpi.isWorkerEnabled()) {
                workerStatusBadge.setValue("АКТИВЕН");
                workerStatusBadge.setStyleName("ai-chip-badge badge-green");
                workerRunStateBadge.setValue("В ОЧЕРЕДИ");
                workerRunStateBadge.setStyleName("ai-chip-badge badge-purple");
            } else {
                workerStatusBadge.setValue("ВЫКЛЮЧЕН");
                workerStatusBadge.setStyleName("ai-chip-badge badge-red");
                workerRunStateBadge.setValue("ОСТАНОВЛЕН");
                workerRunStateBadge.setStyleName("ai-chip-badge badge-admin");
            }

            // 2. Карточка прогресса
            long eligible = kpi.getTotalEligibleCvCount();
            long fresh = kpi.getFreshCount();
            double percent = (eligible > 0) ? ((double) fresh / eligible) * 100.0 : 0.0;
            kpiProgressValueLabel.setValue(String.format(Locale.US, "%d из %d CV (%.1f%%)", fresh, eligible, percent));
            kpiProgressBar.setValue(percent / 100.0);
            kpiProgressSubLabel.setValue(String.format("Ожидает: %d | Фото: %d",
                    (eligible - fresh > 0 ? eligible - fresh : 0), kpi.getTotalPhotosExtractedAllTime()));

            // 3. Карточка скорости
            kpiSpeedValueLabel.setValue(String.format(Locale.US, "%.1f CV/час", kpi.getProcessingSpeedPerHour()));
            kpiEtaSubLabel.setValue("Ориентировочный ETA: " + kpi.getEstimatedEtaText());

            // 4. Динамика
            kpiProcessedTodayLabel.setValue("Сегодня: +" + kpi.getProcessedToday() + " CV");
            kpiVolumeSubLabel.setValue(String.format("Контактов: %d | Фото: %d | За 7д: %d",
                    kpi.getTotalContactsExtractedToday(), kpi.getTotalPhotosExtractedToday(), kpi.getProcessed7d()));

            // 5. Ошибки
            kpiErrorsValueLabel.setValue(String.format("Ошибок: %d | В повторе: %d", kpi.getErrorCount(), kpi.getRetryCount()));
            kpiStaleSubLabel.setValue("Устарело (STALE): " + kpi.getStaleCount());

            // 6. Токены
            kpiTokensValueLabel.setValue(String.format(Locale.US, "%,d токенов", kpi.getTotalTokensToday()));
            kpiTokensSubLabel.setValue(String.format("Запросов: %d | Стоимость: $%.4f",
                    kpi.getAiRequestsToday(),
                    kpi.getEstimatedCostToday() != null ? kpi.getEstimatedCostToday().doubleValue() : 0.0));

            if (reloadTable) {
                reloadTableWithFilter();
            }
        } finally {
            isRefreshing = false;
        }
    }

    private void reloadTableWithFilter() {
        StringBuilder query = new StringBuilder(
                "select e from hunttech_CandidateCvContactAnalysis e where 1=1 ");

        for (String param : FILTER_PARAM_NAMES) {
            analysesDl.removeParameter(param);
        }

        String search = candidateSearchField.getValue();
        if (search != null && !search.trim().isEmpty()) {
            query.append(" and lower(e.candidate.fullName) like :search ");
            analysesDl.setParameter("search", "%" + search.trim().toLowerCase() + "%");
        }

        String statusFilter = statusFilterGroup.getValue();
        if (statusFilter != null) {
            switch (statusFilter) {
                case "QUEUED":
                    query.append(" and e.status in :queuedStatuses ");
                    analysesDl.setParameter("queuedStatuses", Arrays.asList(
                            CandidateCvAnalysisStatus.NOT_ANALYZED.getId(),
                            CandidateCvAnalysisStatus.PROCESSING.getId(),
                            CandidateCvAnalysisStatus.RETRY.getId()
                    ));
                    break;
                case "TODAY":
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    cal.set(java.util.Calendar.SECOND, 0);
                    query.append(" and e.processingFinishedAt >= :today ");
                    analysesDl.setParameter("today", cal.getTime());
                    break;
                case "ERROR":
                    query.append(" and e.status = :errStatus ");
                    analysesDl.setParameter("errStatus", CandidateCvAnalysisStatus.ERROR.getId());
                    break;
                case "RETRY":
                    query.append(" and e.status = :retryStatus ");
                    analysesDl.setParameter("retryStatus", CandidateCvAnalysisStatus.RETRY.getId());
                    break;
                case "LAST_24H":
                    Date past24h = new Date(System.currentTimeMillis() - 24 * 3600_000L);
                    query.append(" and e.processingFinishedAt >= :past24h ");
                    analysesDl.setParameter("past24h", past24h);
                    break;
                case "STALE":
                    query.append(" and e.status = :staleStatus ");
                    analysesDl.setParameter("staleStatus", CandidateCvAnalysisStatus.STALE.getId());
                    break;
                case "FRESH":
                    query.append(" and e.status = :freshStatus ");
                    analysesDl.setParameter("freshStatus", CandidateCvAnalysisStatus.FRESH.getId());
                    break;
            }
        }

        query.append(" order by (case when e.status in (10, 20) then 0 else 1 end), coalesce(e.priority, 0) desc, e.createTs desc, e.processingStartedAt desc nulls last");
        analysesDl.setQuery(query.toString());
        analysesDl.load();
    }

    static String formatProviderAndModel(CandidateCvContactAnalysis item) {
        if (item == null) return "Метаданные недоступны";
        String provider = item.getProviderCode() != null ? item.getProviderCode().trim() : "";
        String model = item.getModelName() != null ? item.getModelName().trim() : "";
        if (provider.isEmpty() && model.isEmpty()) return "—";
        if (provider.isEmpty()) return model;
        if (model.isEmpty()) return provider;
        return provider + " / " + model;
    }

    private void openDeltaDialog(CandidateCvContactAnalysis item) {
        CandidateContactDeltaDialog dialog = screenBuilders.screen(this)
                .withScreenClass(CandidateContactDeltaDialog.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        dialog.initData(item);
        dialog.show();
    }

    @Subscribe("freeOnlyCheckBox")
    public void onFreeOnlyCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (!isInitialized || !event.isUserOriginated()) return;
        boolean val = Boolean.TRUE.equals(event.getValue());
        enrichmentService.setFreeOnly(val);
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Настройки AI обновлены")
                .withDescription(val ? "Режим FREE ONLY активен: только бесплатные модели" : "Разрешено использование всех моделей (fallback)")
                .show();
    }

    @Subscribe("startWorkerBtn")
    public void onStartWorkerBtnClick(Button.ClickEvent event) {
        enrichmentService.setWorkerEnabled(true);
        refreshDashboard(false);
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Служба CandidateContactsEnrichmentWorker включена")
                .show();
    }

    @Subscribe("stopWorkerBtn")
    public void onStopWorkerBtnClick(Button.ClickEvent event) {
        enrichmentService.setWorkerEnabled(false);
        refreshDashboard(false);
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Служба CandidateContactsEnrichmentWorker остановлена")
                .show();
    }

    @Subscribe("runOnceBtn")
    public void onRunOnceBtnClick(Button.ClickEvent event) {
        enrichmentService.runSingleCycleNow();
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Запущен 1 шаг анализа контактов")
                .show();
    }

    @Subscribe("refreshKpiBtn")
    public void onRefreshKpiBtnClick(Button.ClickEvent event) {
        refreshDashboard(true);
    }

    @Subscribe("refreshTableBtn")
    public void onRefreshTableBtnClick(Button.ClickEvent event) {
        reloadTableWithFilter();
    }

    @Subscribe("viewDeltaBtn")
    public void onViewDeltaBtnClick(Button.ClickEvent event) {
        CandidateCvContactAnalysis selected = analysesTable.getSingleSelected();
        if (selected == null) {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption("Выберите запись в таблице для просмотра деталей")
                    .show();
            return;
        }
        openDeltaDialog(selected);
    }

    @Subscribe("retryErrorBtn")
    public void onRetryErrorBtnClick(Button.ClickEvent event) {
        CandidateCvContactAnalysis selected = analysesTable.getSingleSelected();
        if (selected == null) {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption("Выберите запись в таблице для повтора")
                    .show();
            return;
        }
        if (selected.getCandidateCv() != null) {
            enrichmentService.reprocessCv(selected.getCandidateCv().getId());
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Резюме поставлено в начало очереди для повтора")
                    .show();
            reloadTableWithFilter();
        }
    }

    @Subscribe("openCandidateBtn")
    public void onOpenCandidateBtnClick(Button.ClickEvent event) {
        CandidateCvContactAnalysis selected = analysesTable.getSingleSelected();
        if (selected == null || selected.getCandidate() == null) {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption("Выберите кандидата в таблице")
                    .show();
            return;
        }
        screenBuilders.editor(JobCandidate.class, this)
                .editEntity(selected.getCandidate())
                .withOpenMode(OpenMode.NEW_TAB)
                .show();
    }

    @Subscribe("statusFilterGroup")
    public void onStatusFilterGroupValueChange(HasValue.ValueChangeEvent<String> event) {
        if (isInitialized) {
            reloadTableWithFilter();
        }
    }

    @Subscribe("refreshTimer")
    public void onRefreshTimerTimerAction(Timer.TimerActionEvent event) {
        if (!isRefreshing) {
            refreshDashboard(false);
        }
    }
}
