package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.dto.CandidateVacancyMatchReport;
import com.company.hunttech.entity.CandidateVacancyMatchItem;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.service.CandidateVacancyMatchAiService;
import com.company.hunttech.web.screens.openposition.OpenPositionEdit;
import com.company.hunttech.web.util.AiOperationNotifier;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * Экран экспертного аналитического отчёта AI-сопоставления кандидата с вакансиями.
 */
@UiController("hunttech_CandidateVacancyMatch")
@UiDescriptor("candidate-vacancy-match-screen.xml")
public class CandidateVacancyMatchScreen extends Screen {

    private static final Logger log = LoggerFactory.getLogger(CandidateVacancyMatchScreen.class);

    private JobCandidate candidate;
    private CandidateVacancyMatchReport currentReport;

    @Inject
    private CandidateVacancyMatchAiService candidateVacancyMatchAiService;

    @Inject
    private BackgroundWorker backgroundWorker;

    @Inject
    private Notifications notifications;

    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private DataManager dataManager;

    /* UI Components */
    @Inject
    private CollectionContainer<CandidateVacancyMatchItem> matchesDc;

    @Inject
    private Table<CandidateVacancyMatchItem> matchesTable;

    @Inject
    private Label<String> candidateTitleLabel;

    @Inject
    private Label<String> candidateSubTitleLabel;

    @Inject
    private ProgressBar analysisProgressBar;

    @Inject
    private Label<String> statusLabel;

    @Inject
    private Button refreshAnalysisBtn;

    @Inject
    private Button openVacancyBtn;

    @Inject
    private Label<String> countLabel;

    /* Detail Card Components */
    @Inject
    private Label<String> detailVacancyTitle;

    @Inject
    private Label<String> detailVacancyMeta;

    @Inject
    private VBoxLayout scoresBox;

    @Inject
    private Label<String> roleFitLabel;

    @Inject
    private Label<String> skillsFitLabel;

    @Inject
    private Label<String> experienceFitLabel;

    @Inject
    private Label<String> preferencesFitLabel;

    @Inject
    private Label<String> domainFitLabel;

    @Inject
    private Label<String> totalScoreLabel;

    @Inject
    private VBoxLayout reasonsBox;

    @Inject
    private Label<String> reasonsLabel;

    @Inject
    private VBoxLayout matchedSkillsBox;

    @Inject
    private Label<String> matchedSkillsDetailLabel;

    @Inject
    private VBoxLayout missingReqsBox;

    @Inject
    private Label<String> missingReqsLabel;

    @Inject
    private VBoxLayout risksBox;

    @Inject
    private Label<String> risksLabel;

    @Inject
    private VBoxLayout summaryBox;

    @Inject
    private Label<String> summaryTextLabel;

    @Inject
    private VBoxLayout aiMetaBox;

    @Inject
    private Label<String> aiMetaLabel;

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        // Настройка визуального оформления колонок таблицы
        matchesTable.addGeneratedColumn("score", item -> {
            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            int score = item.getScore() != null ? item.getScore() : 0;
            String bg = score >= 80 ? "#28a745" : (score >= 65 ? "#007bff" : (score >= 45 ? "#e0a800" : "#6c757d"));
            label.setValue(String.format("<span style='background-color: %s; color: white; font-weight: bold; padding: 2px 8px; border-radius: 4px;'>%d/100</span>", bg, score));
            label.setHtmlEnabled(true);
            return label;
        });

        matchesTable.addGeneratedColumn("verdict", item -> {
            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            String verdict = item.getVerdict() != null ? item.getVerdict() : "—";
            int score = item.getScore() != null ? item.getScore() : 0;
            String color = score >= 80 ? "#28a745" : (score >= 65 ? "#007bff" : (score >= 45 ? "#d39e00" : "#6c757d"));
            label.setValue(String.format("<span style='color: %s; font-weight: bold;'>%s</span>", color, escapeHtml(verdict)));
            label.setHtmlEnabled(true);
            return label;
        });
    }

    @Inject
    private com.haulmont.cuba.gui.UiComponents uiComponents;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (candidate != null) {
            String fio = candidate.getFullName() != null ? candidate.getFullName() : "Кандидат";
            candidateTitleLabel.setValue("Кандидат: " + fio);

            StringBuilder sub = new StringBuilder();
            if (candidate.getPersonPosition() != null && candidate.getPersonPosition().getPositionRuName() != null) {
                sub.append(candidate.getPersonPosition().getPositionRuName());
            }
            if (candidate.getCityOfResidence() != null && candidate.getCityOfResidence().getCityRuName() != null) {
                if (sub.length() > 0) sub.append(" • ");
                sub.append("📍 ").append(candidate.getCityOfResidence().getCityRuName());
            }
            candidateSubTitleLabel.setValue(sub.toString());

            startAnalysis();
        } else {
            statusLabel.setValue("Кандидат не выбран.");
        }
    }

    private void startAnalysis() {
        if (candidate == null) {
            return;
        }

        setBusy(true, "AI анализирует резюме кандидата и открытые вакансии...");
        matchesDc.getMutableItems().clear();
        clearDetailsPane();

        BackgroundTask<Integer, CandidateVacancyMatchReport> task =
                new BackgroundTask<Integer, CandidateVacancyMatchReport>(240, this) {
                    @Override
                    public CandidateVacancyMatchReport run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                        taskLifeCycle.publish(1);
                        return candidateVacancyMatchAiService.matchVacanciesForCandidate(candidate.getId());
                    }

                    @Override
                    public void progress(List<Integer> changes) {
                        statusLabel.setValue("Анализ и ранжирование вакансий нейросетью...");
                    }

                    @Override
                    public void done(CandidateVacancyMatchReport report) {
                        setBusy(false, null);
                        handleReport(report);
                    }

                    @Override
                    public boolean handleTimeoutException() {
                        setBusy(false, null);
                        log.error("Timeout during candidate-vacancy matching");
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Превышено время AI-подбора")
                                .withDescription("Анализ вакансий занял слишком много времени.")
                                .show();
                        statusLabel.setValue("Превышено время выполнения AI-анализа.");
                        return true;
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        setBusy(false, null);
                        log.error("Error during candidate-vacancy matching", ex);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Ошибка AI-подбора")
                                .withDescription("Не удалось выполнить анализ вакансий: " + ex.getMessage())
                                .show();
                        statusLabel.setValue("Ошибка при выполнении AI-анализа.");
                        return true;
                    }
                };

        backgroundWorker.handle(task).execute();
    }

    private void setBusy(boolean busy, String message) {
        analysisProgressBar.setVisible(busy);
        refreshAnalysisBtn.setEnabled(!busy);
        if (message != null) {
            statusLabel.setValue(message);
        }
    }

    private void handleReport(CandidateVacancyMatchReport report) {
        if (report == null) {
            statusLabel.setValue("Не удалось получить отчёт сопоставления.");
            return;
        }

        if (!report.isSuccess()) {
            String msg = report.getStatusMessage() != null ? report.getStatusMessage() : "Ошибка анализа.";
            statusLabel.setValue(msg);
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Подбор вакансий")
                    .withDescription(msg)
                    .show();
            return;
        }

        this.currentReport = report;
        List<CandidateVacancyMatchItem> items = report.getItems();
        matchesDc.setItems(items);
        countLabel.setValue("Найдено вакансий: " + items.size());

        if (report.isFallbackUsed()) {
            statusLabel.setValue("AI недоступен. Выполнена предварительная оценка без AI.");
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Внимание")
                    .withDescription("AI-сервис временно недоступен. Расчёт выполнен по эвристическим правилам совпадения навыков.")
                    .show();
        } else {
            statusLabel.setValue(String.format("AI-анализ завершен. Проанализировано %d вакансий.", report.getTotalVacanciesAnalyzed()));
            if (report.getAiExecutionResult() != null) {
                AiOperationNotifier.show(notifications, report.getAiExecutionResult(),
                        "AI-подбор вакансий выполнен",
                        String.format("Проанализировано %d вакансий, рекомендовано к рассмотрению %d",
                                report.getTotalVacanciesAnalyzed(), items.size()));
            }
        }

        if (!items.isEmpty()) {
            matchesTable.setSelected(items.get(0));
            populateDetailPane(items.get(0));
        }
    }

    @Subscribe(id = "matchesDc", target = Target.DATA_CONTAINER)
    public void onMatchesDcItemChange(CollectionContainer.ItemChangeEvent<CandidateVacancyMatchItem> event) {
        CandidateVacancyMatchItem item = event.getItem();
        if (item != null) {
            openVacancyBtn.setEnabled(true);
            populateDetailPane(item);
        } else {
            openVacancyBtn.setEnabled(false);
            clearDetailsPane();
        }
    }

    private void populateDetailPane(CandidateVacancyMatchItem item) {
        detailVacancyTitle.setValue(item.getVacancyName() != null ? item.getVacancyName() : "Вакансия");
        StringBuilder meta = new StringBuilder();
        if (item.getProjectName() != null && !item.getProjectName().isEmpty()) {
            meta.append("Проект: ").append(item.getProjectName());
        }
        if (item.getPositionName() != null && !item.getPositionName().isEmpty()) {
            if (meta.length() > 0) meta.append(" • ");
            meta.append(item.getPositionName());
        }
        detailVacancyMeta.setValue(meta.toString());

        // Subscores
        scoresBox.setVisible(true);
        roleFitLabel.setValue(String.format("%d / 25", item.getRoleFit() != null ? item.getRoleFit() : 0));
        skillsFitLabel.setValue(String.format("%d / 35", item.getSkillsFit() != null ? item.getSkillsFit() : 0));
        experienceFitLabel.setValue(String.format("%d / 20", item.getExperienceFit() != null ? item.getExperienceFit() : 0));
        preferencesFitLabel.setValue(String.format("%d / 10", item.getPreferencesFit() != null ? item.getPreferencesFit() : 0));
        domainFitLabel.setValue(String.format("%d / 10", item.getDomainFit() != null ? item.getDomainFit() : 0));
        totalScoreLabel.setValue(String.format("%d / 100", item.getScore() != null ? item.getScore() : 0));

        // Reasons to offer
        if (item.getReasonsToOffer() != null && !item.getReasonsToOffer().isEmpty()) {
            reasonsBox.setVisible(true);
            reasonsLabel.setValue(formatListAsHtml(item.getReasonsToOffer()));
        } else {
            reasonsBox.setVisible(false);
        }

        // Matched skills & evidence
        if (item.getMatchedSkills() != null && !item.getMatchedSkills().isEmpty()) {
            matchedSkillsBox.setVisible(true);
            StringBuilder sb = new StringBuilder();
            sb.append(formatListAsHtml(item.getMatchedSkills()));
            if (item.getCandidateEvidence() != null && !item.getCandidateEvidence().isEmpty()) {
                sb.append("<div style='margin-top: 6px; font-style: italic; color: #555;'>Факты из резюме:</div>");
                sb.append(formatListAsHtml(item.getCandidateEvidence()));
            }
            matchedSkillsDetailLabel.setValue(sb.toString());
        } else {
            matchedSkillsBox.setVisible(false);
        }

        // Missing requirements
        if (item.getMissingCriticalRequirements() != null && !item.getMissingCriticalRequirements().isEmpty()) {
            missingReqsBox.setVisible(true);
            missingReqsLabel.setValue(formatListAsHtml(item.getMissingCriticalRequirements()));
        } else {
            missingReqsBox.setVisible(false);
        }

        // Risks
        if (item.getRisks() != null && !item.getRisks().isEmpty()) {
            risksBox.setVisible(true);
            risksLabel.setValue(formatListAsHtml(item.getRisks()));
        } else {
            risksBox.setVisible(false);
        }

        // Summary
        if (item.getSummary() != null && !item.getSummary().trim().isEmpty()) {
            summaryBox.setVisible(true);
            summaryTextLabel.setValue(escapeHtml(item.getSummary()));
        } else {
            summaryBox.setVisible(false);
        }

        // AI Meta
        AiExecutionResult aiResult = this.currentReport != null ? this.currentReport.getAiExecutionResult() : null;
        if (aiResult != null) {
            aiMetaBox.setVisible(true);
            aiMetaLabel.setValue(String.format("Модель: %s • Провайдер: %s",
                    aiResult.getModelName() != null ? aiResult.getModelName() : "—",
                    aiResult.getProviderCode() != null ? aiResult.getProviderCode() : "—"));
        } else {
            aiMetaBox.setVisible(false);
        }
    }

    private String formatListAsHtml(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<ul style='margin: 4px 0; padding-left: 20px;'>");
        for (String s : list) {
            sb.append("<li>").append(escapeHtml(s)).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void clearDetailsPane() {
        detailVacancyTitle.setValue("Выберите вакансию из списка слева");
        detailVacancyMeta.setValue("");
        scoresBox.setVisible(false);
        reasonsBox.setVisible(false);
        matchedSkillsBox.setVisible(false);
        missingReqsBox.setVisible(false);
        risksBox.setVisible(false);
        summaryBox.setVisible(false);
        aiMetaBox.setVisible(false);
    }

    @Subscribe("refreshAnalysisBtn")
    public void onRefreshAnalysisBtnClick(Button.ClickEvent event) {
        startAnalysis();
    }

    @Subscribe("closeBtn")
    public void onCloseBtnClick(Button.ClickEvent event) {
        closeWithDefaultAction();
    }

    @Subscribe("openVacancyBtn")
    public void onOpenVacancyBtnClick(Button.ClickEvent event) {
        CandidateVacancyMatchItem selected = matchesTable.getSingleSelected();
        if (selected == null || selected.getOpenPositionId() == null) {
            return;
        }

        OpenPosition op = selected.getOpenPosition();
        if (op == null) {
            op = dataManager.load(OpenPosition.class).id(selected.getOpenPositionId()).optional().orElse(null);
        }

        if (op != null) {
            screenBuilders.editor(OpenPosition.class, this)
                    .withScreenClass(OpenPositionEdit.class)
                    .editEntity(op)
                    .withOpenMode(OpenMode.NEW_TAB)
                    .show();
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Вакансия не найдена")
                    .show();
        }
    }
}
