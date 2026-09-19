package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.dto.CandidateVacancyMatchReport;
import com.company.hunttech.entity.CandidateVacancyMatchItem;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.service.CandidateVacancyMatchAiService;
import com.company.hunttech.service.CandidateVacancyWorkflowService;
import com.company.hunttech.service.dto.BulkTakeIntoWorkResult;
import com.company.hunttech.service.dto.TakeIntoWorkResult;
import com.company.hunttech.web.screens.candidatevacancymatch.CandidateOutreachDraftDialog;
import com.company.hunttech.web.screens.candidatevacancymatch.RejectCandidateMatchDialog;
import com.company.hunttech.web.screens.iteractionlist.IteractionListEdit;
import com.company.hunttech.web.screens.openposition.OpenPositionEdit;
import com.company.hunttech.web.util.AiOperationNotifier;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Экран результатов AI-подбора кандидатов и вакансий с рабочим процессом рекрутера (Этап 2 и Этап 3).
 */
@UiController("hunttech_CandidateVacancyMatch")
@UiDescriptor("candidate-vacancy-match-screen.xml")
public class CandidateVacancyMatchScreen extends Screen {

    private static final Logger log = LoggerFactory.getLogger(CandidateVacancyMatchScreen.class);

    private enum Mode {
        CANDIDATE_TO_VACANCIES,
        VACANCY_TO_CANDIDATES
    }

    private Mode mode = Mode.CANDIDATE_TO_VACANCIES;
    private JobCandidate candidate;
    private OpenPosition openPosition;
    private CandidateVacancyMatchReport currentReport;
    private List<CandidateVacancyMatchItem> allReportItems = new ArrayList<>();

    @Inject
    private CandidateVacancyMatchAiService candidateVacancyMatchAiService;

    @Inject
    private CandidateVacancyWorkflowService workflowService;

    @Inject
    private BackgroundWorker backgroundWorker;

    @Inject
    private Notifications notifications;

    @Inject
    private Dialogs dialogs;

    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private DataManager dataManager;

    @Inject
    private UiComponents uiComponents;

    /* Top Bar Components */
    @Inject
    private CollectionContainer<CandidateVacancyMatchItem> matchesDc;

    @Inject
    private Table<CandidateVacancyMatchItem> matchesTable;

    @Inject
    private Label<String> mainTitleLabel;

    @Inject
    private Label<String> subTitleLabel;

    @Inject
    private ProgressBar analysisProgressBar;

    @Inject
    private Label<String> statusLabel;

    @Inject
    private Button refreshAnalysisBtn;

    @Inject
    private Button closeBtn;

    /* Toolbar Action Buttons */
    @Inject
    private Button takeIntoWorkBtn;

    @Inject
    private Button createInteractionBtn;

    @Inject
    private Button outreachDraftBtn;

    @Inject
    private Button bulkTakeIntoWorkBtn;

    @Inject
    private Button postponeBtn;

    @Inject
    private Button rejectBtn;

    @Inject
    private Button openEntityBtn;

    @Inject
    private LookupField<String> decisionFilter;

    @Inject
    private Label<String> countLabel;

    /* Right Detail Card Components */
    @Inject
    private Label<String> detailTitle;

    @Inject
    private Label<String> detailMeta;

    @Inject
    private HBoxLayout quickActionsBox;

    @Inject
    private Button quickTakeBtn;

    @Inject
    private Button quickInteractionBtn;

    @Inject
    private Button quickOutreachBtn;

    @Inject
    private Button quickPostponeBtn;

    @Inject
    private Button quickRejectBtn;

    @Inject
    private VBoxLayout recruiterDecisionBox;

    @Inject
    private Label<String> recruiterDecisionTextLabel;

    @Inject
    private Label<String> recruiterCommentTextLabel;

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
        this.mode = Mode.CANDIDATE_TO_VACANCIES;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
        this.mode = Mode.VACANCY_TO_CANDIDATES;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        initTableColumns();
        initDecisionFilter();
    }

    private void initTableColumns() {
        matchesTable.addGeneratedColumn("score", item -> {
            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            int score = item.getScore() != null ? item.getScore() : 0;
            String bg = score >= 80 ? "#28a745" : (score >= 65 ? "#007bff" : (score >= 45 ? "#e0a800" : "#6c757d"));
            label.setValue(String.format("<span style='background-color: %s; color: white; font-weight: bold; padding: 2px 8px; border-radius: 4px;'>%d/100</span>", bg, score));
            label.setHtmlEnabled(true);
            return label;
        });

        matchesTable.addGeneratedColumn("recruiterDecisionDisplay", item -> {
            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            String dec = item.getRecruiterDecision();
            if (dec == null || dec.trim().isEmpty() || "—".equals(dec)) {
                label.setValue("<span style='color: #6c757d;'>—</span>");
            } else if ("В работе".equalsIgnoreCase(dec) || "IN_WORK".equalsIgnoreCase(dec)) {
                label.setValue("<span style='background-color: #28a745; color: white; font-weight: bold; padding: 2px 6px; border-radius: 3px;'>В работе</span>");
            } else if ("Отложен".equalsIgnoreCase(dec) || "POSTPONED".equalsIgnoreCase(dec)) {
                label.setValue("<span style='background-color: #fd7e14; color: white; font-weight: bold; padding: 2px 6px; border-radius: 3px;'>Отложен</span>");
            } else if ("Не подходит".equalsIgnoreCase(dec) || "REJECTED".equalsIgnoreCase(dec)) {
                label.setValue("<span style='background-color: #dc3545; color: white; font-weight: bold; padding: 2px 6px; border-radius: 3px;'>Не подходит</span>");
            } else {
                label.setValue(escapeHtml(dec));
            }
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

        matchesTable.addGeneratedColumn("candidateName", item -> {
            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            String name = item.getCandidateFullName() != null ? item.getCandidateFullName() : "";
            if (item.getCandidateCity() != null && !item.getCandidateCity().isEmpty()) {
                name += " (" + item.getCandidateCity() + ")";
            }
            label.setValue(escapeHtml(name));
            return label;
        });

        matchesTable.addGeneratedColumn("matchedSkillsDisplay", item -> {
            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            if (item.getMatchedSkills() != null && !item.getMatchedSkills().isEmpty()) {
                label.setValue(escapeHtml(String.join(", ", item.getMatchedSkills())));
            } else {
                label.setValue("—");
            }
            return label;
        });

        matchesTable.addGeneratedColumn("missingCriticalRequirementsDisplay", item -> {
            Label<String> label = uiComponents.create(Label.TYPE_STRING);
            if (item.getMissingCriticalRequirements() != null && !item.getMissingCriticalRequirements().isEmpty()) {
                label.setValue(escapeHtml(String.join(", ", item.getMissingCriticalRequirements())));
            } else {
                label.setValue("—");
            }
            return label;
        });
    }

    private void initDecisionFilter() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Все", "ALL");
        options.put("Не обработаны рекрутером", "UNPROCESSED");
        options.put("В работе", "IN_WORK");
        options.put("Отложены", "POSTPONED");
        options.put("Не подходят", "REJECTED");
        decisionFilter.setOptionsMap(options);
        decisionFilter.setValue("ALL");

        decisionFilter.addValueChangeListener(e -> applyFilter(e.getValue()));
    }

    private void applyFilter(String filterKey) {
        if (filterKey == null || "ALL".equalsIgnoreCase(filterKey)) {
            matchesDc.setItems(allReportItems);
        } else if ("UNPROCESSED".equalsIgnoreCase(filterKey)) {
            matchesDc.setItems(allReportItems.stream()
                    .filter(it -> it.getRecruiterDecision() == null || it.getRecruiterDecision().trim().isEmpty() || "—".equals(it.getRecruiterDecision()))
                    .collect(Collectors.toList()));
        } else if ("IN_WORK".equalsIgnoreCase(filterKey)) {
            matchesDc.setItems(allReportItems.stream()
                    .filter(it -> "В работе".equalsIgnoreCase(it.getRecruiterDecision()) || "IN_WORK".equalsIgnoreCase(it.getRecruiterDecision()))
                    .collect(Collectors.toList()));
        } else if ("POSTPONED".equalsIgnoreCase(filterKey)) {
            matchesDc.setItems(allReportItems.stream()
                    .filter(it -> "Отложен".equalsIgnoreCase(it.getRecruiterDecision()) || "POSTPONED".equalsIgnoreCase(it.getRecruiterDecision()))
                    .collect(Collectors.toList()));
        } else if ("REJECTED".equalsIgnoreCase(filterKey)) {
            matchesDc.setItems(allReportItems.stream()
                    .filter(it -> "Не подходит".equalsIgnoreCase(it.getRecruiterDecision()) || "REJECTED".equalsIgnoreCase(it.getRecruiterDecision()))
                    .collect(Collectors.toList()));
        }
        countLabel.setValue("Найдено совпадений: " + matchesDc.getItems().size());
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (mode == Mode.CANDIDATE_TO_VACANCIES && candidate != null) {
            String fio = candidate.getFullName() != null ? candidate.getFullName() : "Кандидат";
            mainTitleLabel.setValue("Кандидат: " + fio);

            StringBuilder sub = new StringBuilder();
            if (candidate.getPersonPosition() != null && candidate.getPersonPosition().getPositionRuName() != null) {
                sub.append(candidate.getPersonPosition().getPositionRuName());
            }
            if (candidate.getCityOfResidence() != null && candidate.getCityOfResidence().getCityRuName() != null) {
                if (sub.length() > 0) sub.append(" • ");
                sub.append("📍 ").append(candidate.getCityOfResidence().getCityRuName());
            }
            subTitleLabel.setValue(sub.toString());
            openEntityBtn.setCaption("Открыть вакансию");
            startAnalysis();
        } else if (mode == Mode.VACANCY_TO_CANDIDATES && openPosition != null) {
            String vacName = openPosition.getVacansyName() != null ? openPosition.getVacansyName() : "Вакансия";
            mainTitleLabel.setValue("Вакансия: " + vacName);

            StringBuilder sub = new StringBuilder();
            if (openPosition.getProjectName() != null && openPosition.getProjectName().getProjectName() != null) {
                sub.append("Проект: ").append(openPosition.getProjectName().getProjectName());
            }
            if (openPosition.getCityPosition() != null && openPosition.getCityPosition().getCityRuName() != null) {
                if (sub.length() > 0) sub.append(" • ");
                sub.append("📍 ").append(openPosition.getCityPosition().getCityRuName());
            }
            subTitleLabel.setValue(sub.toString());
            openEntityBtn.setCaption("Открыть кандидата");
            startAnalysis();
        } else {
            statusLabel.setValue("Объект для AI-подбора не выбран.");
        }
    }

    private void startAnalysis() {
        setBusy(true, "AI анализирует профили и формирует ранжированные рекомендации...");
        matchesDc.getMutableItems().clear();
        allReportItems.clear();
        clearDetailsPane();
        updateToolbarActionsState();

        BackgroundTask<Integer, CandidateVacancyMatchReport> task =
                new BackgroundTask<Integer, CandidateVacancyMatchReport>(240, this) {
                    @Override
                    public CandidateVacancyMatchReport run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                        taskLifeCycle.publish(1);
                        if (mode == Mode.VACANCY_TO_CANDIDATES && openPosition != null) {
                            return candidateVacancyMatchAiService.matchCandidatesForVacancy(openPosition.getId());
                        } else if (candidate != null) {
                            return candidateVacancyMatchAiService.matchVacanciesForCandidate(candidate.getId());
                        }
                        return null;
                    }

                    @Override
                    public void progress(List<Integer> changes) {
                        statusLabel.setValue("Анализ соответствия и расчёт рейтинга нейросетью...");
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
                                .withDescription("Анализ занял слишком много времени.")
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
                                .withDescription("Не удалось выполнить анализ: " + ex.getMessage())
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
                    .withCaption("AI-подбор")
                    .withDescription(msg)
                    .show();
            return;
        }

        this.currentReport = report;
        this.allReportItems = new ArrayList<>(report.getItems());
        applyFilter(decisionFilter.getValue());

        if (report.isFallbackUsed()) {
            statusLabel.setValue("AI недоступен. Выполнена предварительная оценка по совпадению навыков.");
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Внимание")
                    .withDescription("AI-сервис временно недоступен. Отображён честный эвристический расчёт.")
                    .show();
        } else {
            statusLabel.setValue(String.format("AI-анализ завершен. Рекомендовано: %d.", report.getItems().size()));
            if (report.getAiExecutionResult() != null) {
                AiOperationNotifier.show(notifications, report.getAiExecutionResult(),
                        "AI-подбор завершён",
                        String.format("Проанализировано объектов: %d, сформировано рекомендаций: %d",
                                report.getTotalVacanciesAnalyzed(), report.getItems().size()));
            }
        }

        if (!report.getItems().isEmpty()) {
            matchesTable.setSelected(report.getItems().get(0));
            populateDetailPane(report.getItems().get(0));
        }
    }

    @Subscribe(id = "matchesDc", target = Target.DATA_CONTAINER)
    public void onMatchesDcItemChange(CollectionContainer.ItemChangeEvent<CandidateVacancyMatchItem> event) {
        updateToolbarActionsState();
        CandidateVacancyMatchItem item = event.getItem();
        if (item != null) {
            populateDetailPane(item);
        } else {
            clearDetailsPane();
        }
    }

    @Subscribe("matchesTable")
    public void onMatchesTableSelection(Table.SelectionEvent<CandidateVacancyMatchItem> event) {
        updateToolbarActionsState();
    }

    private void updateToolbarActionsState() {
        Set<CandidateVacancyMatchItem> selected = matchesTable.getSelected();
        int count = selected != null ? selected.size() : 0;

        if (count > 1) {
            bulkTakeIntoWorkBtn.setVisible(true);
            bulkTakeIntoWorkBtn.setCaption("Взять выбранных в работу (" + count + ")");
            takeIntoWorkBtn.setEnabled(false);
            createInteractionBtn.setEnabled(false);
            outreachDraftBtn.setEnabled(false);
            postponeBtn.setEnabled(false);
            rejectBtn.setEnabled(false);
            openEntityBtn.setEnabled(false);
        } else if (count == 1) {
            bulkTakeIntoWorkBtn.setVisible(false);
            takeIntoWorkBtn.setEnabled(true);
            createInteractionBtn.setEnabled(true);
            outreachDraftBtn.setEnabled(true);
            postponeBtn.setEnabled(true);
            rejectBtn.setEnabled(true);
            openEntityBtn.setEnabled(true);
        } else {
            bulkTakeIntoWorkBtn.setVisible(false);
            takeIntoWorkBtn.setEnabled(false);
            createInteractionBtn.setEnabled(false);
            outreachDraftBtn.setEnabled(false);
            postponeBtn.setEnabled(false);
            rejectBtn.setEnabled(false);
            openEntityBtn.setEnabled(false);
        }
    }

    private void populateDetailPane(CandidateVacancyMatchItem item) {
        String title = mode == Mode.VACANCY_TO_CANDIDATES
                ? (item.getCandidateFullName() != null ? item.getCandidateFullName() : "Кандидат")
                : (item.getVacancyName() != null ? item.getVacancyName() : "Вакансия");
        detailTitle.setValue(title);

        StringBuilder meta = new StringBuilder();
        if (item.getPositionName() != null && !item.getPositionName().isEmpty()) {
            meta.append("Должность: ").append(item.getPositionName());
        }
        if (item.getProjectName() != null && !item.getProjectName().isEmpty()) {
            if (meta.length() > 0) meta.append(" • ");
            meta.append("Проект: ").append(item.getProjectName());
        }
        if (item.getCandidateCity() != null && !item.getCandidateCity().isEmpty()) {
            if (meta.length() > 0) meta.append(" • ");
            meta.append("📍 ").append(item.getCandidateCity());
        }
        detailMeta.setValue(meta.toString());

        quickActionsBox.setVisible(true);

        // Recruiter Decision
        if (item.getRecruiterDecision() != null && !item.getRecruiterDecision().trim().isEmpty() && !"—".equals(item.getRecruiterDecision())) {
            recruiterDecisionBox.setVisible(true);
            recruiterDecisionTextLabel.setValue(item.getRecruiterDecision());
            StringBuilder commentBuilder = new StringBuilder();
            if (item.getRejectionReason() != null && !item.getRejectionReason().isEmpty()) {
                commentBuilder.append("Причина: ").append(item.getRejectionReason()).append(". ");
            }
            if (item.getRecruiterComment() != null && !item.getRecruiterComment().isEmpty()) {
                commentBuilder.append(item.getRecruiterComment());
            }
            recruiterCommentTextLabel.setValue(commentBuilder.toString());
        } else {
            recruiterDecisionBox.setVisible(false);
        }

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

    private void clearDetailsPane() {
        detailTitle.setValue("Выберите строку из списка слева");
        detailMeta.setValue("");
        quickActionsBox.setVisible(false);
        recruiterDecisionBox.setVisible(false);
        scoresBox.setVisible(false);
        reasonsBox.setVisible(false);
        matchedSkillsBox.setVisible(false);
        missingReqsBox.setVisible(false);
        risksBox.setVisible(false);
        summaryBox.setVisible(false);
        aiMetaBox.setVisible(false);
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

    /* Actions Implementation */

    @Subscribe("takeIntoWorkBtn")
    public void onTakeIntoWorkBtnClick(Button.ClickEvent event) {
        executeTakeIntoWork();
    }

    @Subscribe("quickTakeBtn")
    public void onQuickTakeBtnClick(Button.ClickEvent event) {
        executeTakeIntoWork();
    }

    private void executeTakeIntoWork() {
        CandidateVacancyMatchItem selected = matchesTable.getSingleSelected();
        if (selected == null) return;

        UUID candId = resolveCandidateId(selected);
        UUID vacId = resolveVacancyId(selected);

        if (candId == null || vacId == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Недостаточно данных для привязки кандидата к вакансии.")
                    .show();
            return;
        }

        TakeIntoWorkResult res = workflowService.takeIntoWork(candId, vacId, selected.getScore(), selected.getMatchRunId());
        if (res.isSuccess()) {
            if (res.isAlreadyInWork()) {
                dialogs.createOptionDialog()
                        .withCaption("Кандидат уже в работе")
                        .withMessage(res.getMessage() + "\n\nЖелаете открыть существующую запись взаимодействия?")
                        .withActions(
                                new DialogAction(DialogAction.Type.OK)
                                        .withCaption("Открыть существующую")
                                        .withHandler(e -> openInteraction(res.getInteractionId())),
                                new DialogAction(DialogAction.Type.CANCEL).withCaption("Закрыть")
                        )
                        .show();
            } else {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Кандидат взят в работу")
                        .withDescription(res.getMessage())
                        .show();
            }
            selected.setAlreadyInWork(true);
            selected.setRecruiterDecision("В работе");
            matchesDc.replaceItem(selected);
            populateDetailPane(selected);
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка")
                    .withDescription(res.getMessage())
                    .show();
        }
    }

    @Subscribe("bulkTakeIntoWorkBtn")
    public void onBulkTakeIntoWorkBtnClick(Button.ClickEvent event) {
        Set<CandidateVacancyMatchItem> selectedSet = matchesTable.getSelected();
        if (selectedSet == null || selectedSet.isEmpty()) return;

        UUID targetVacId = mode == Mode.VACANCY_TO_CANDIDATES && openPosition != null
                ? openPosition.getId() : null;

        if (targetVacId == null && mode == Mode.CANDIDATE_TO_VACANCIES) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Массовое добавление доступно при подборе кандидатов на выбранную вакансию.")
                    .show();
            return;
        }

        List<UUID> candIds = selectedSet.stream()
                .map(this::resolveCandidateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String vacTitle = openPosition != null ? openPosition.getVacansyName() : "вакансию";

        dialogs.createOptionDialog()
                .withCaption("Подтверждение пакетного действия")
                .withMessage(String.format("Добавить %d кандидатов в работу по вакансии «%s»?", candIds.size(), vacTitle))
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withCaption("Добавить в работу")
                                .withHandler(e -> {
                                    UUID runId = selectedSet.iterator().next().getMatchRunId();
                                    BulkTakeIntoWorkResult bulkRes = workflowService.bulkTakeIntoWork(candIds, targetVacId, runId);
                                    dialogs.createMessageDialog()
                                            .withCaption("Результат пакетного добавления")
                                            .withMessage(String.format("Добавлено: %d\nУже находились в работе: %d\nОшибок: %d",
                                                    bulkRes.getAddedCount(), bulkRes.getAlreadyInWorkCount(), bulkRes.getErrorCount()))
                                            .show();

                                    for (CandidateVacancyMatchItem item : selectedSet) {
                                        item.setAlreadyInWork(true);
                                        item.setRecruiterDecision("В работе");
                                        matchesDc.replaceItem(item);
                                    }
                                }),
                        new DialogAction(DialogAction.Type.NO).withCaption("Отмена")
                )
                .show();
    }

    @Subscribe("createInteractionBtn")
    public void onCreateInteractionBtnClick(Button.ClickEvent event) {
        executeCreateInteraction();
    }

    @Subscribe("quickInteractionBtn")
    public void onQuickInteractionBtnClick(Button.ClickEvent event) {
        executeCreateInteraction();
    }

    private void executeCreateInteraction() {
        CandidateVacancyMatchItem selected = matchesTable.getSingleSelected();
        if (selected == null) return;

        UUID candId = resolveCandidateId(selected);
        UUID vacId = resolveVacancyId(selected);

        IteractionList draft = workflowService.prepareInteractionDraft(candId, vacId, selected.getScore(),
                selected.getMatchedSkills(), selected.getMissingCriticalRequirements());

        screenBuilders.editor(IteractionList.class, this)
                .withScreenClass(IteractionListEdit.class)
                .newEntity(draft)
                .withOpenMode(OpenMode.DIALOG)
                .show();
    }

    @Subscribe("outreachDraftBtn")
    public void onOutreachDraftBtnClick(Button.ClickEvent event) {
        executeOutreachDraft();
    }

    @Subscribe("quickOutreachBtn")
    public void onQuickOutreachBtnClick(Button.ClickEvent event) {
        executeOutreachDraft();
    }

    private void executeOutreachDraft() {
        CandidateVacancyMatchItem selected = matchesTable.getSingleSelected();
        if (selected == null) return;

        UUID candId = resolveCandidateId(selected);
        UUID vacId = resolveVacancyId(selected);

        String candName = selected.getCandidateFullName() != null ? selected.getCandidateFullName()
                : (candidate != null ? candidate.getFullName() : "Кандидат");
        String vacName = selected.getVacancyName() != null ? selected.getVacancyName()
                : (openPosition != null ? openPosition.getVacansyName() : "Вакансия");

        CandidateOutreachDraftDialog dialog = screenBuilders.screen(this)
                .withScreenClass(CandidateOutreachDraftDialog.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();

        dialog.initParams(candId, vacId, candName, vacName, selected.getReasonsToOffer(), selected.getMatchedSkills(), selected.getScore());
        dialog.show();
    }

    @Subscribe("postponeBtn")
    public void onPostponeBtnClick(Button.ClickEvent event) {
        executePostpone();
    }

    @Subscribe("quickPostponeBtn")
    public void onQuickPostponeBtnClick(Button.ClickEvent event) {
        executePostpone();
    }

    private void executePostpone() {
        CandidateVacancyMatchItem selected = matchesTable.getSingleSelected();
        if (selected == null) return;

        UUID candId = resolveCandidateId(selected);
        UUID vacId = resolveVacancyId(selected);

        workflowService.recordFeedback(candId, vacId, "POSTPONED", null, null, selected.getScore(), selected.getMatchRunId());

        selected.setRecruiterDecision("Отложен");
        matchesDc.replaceItem(selected);
        populateDetailPane(selected);
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Кандидат отложен")
                .show();
    }

    @Subscribe("rejectBtn")
    public void onRejectBtnClick(Button.ClickEvent event) {
        executeReject();
    }

    @Subscribe("quickRejectBtn")
    public void onQuickRejectBtnClick(Button.ClickEvent event) {
        executeReject();
    }

    private void executeReject() {
        CandidateVacancyMatchItem selected = matchesTable.getSingleSelected();
        if (selected == null) return;

        UUID candId = resolveCandidateId(selected);
        UUID vacId = resolveVacancyId(selected);

        String candName = selected.getCandidateFullName() != null ? selected.getCandidateFullName()
                : (candidate != null ? candidate.getFullName() : "Кандидат");
        String vacName = selected.getVacancyName() != null ? selected.getVacancyName()
                : (openPosition != null ? openPosition.getVacansyName() : "Вакансия");

        RejectCandidateMatchDialog dialog = screenBuilders.screen(this)
                .withScreenClass(RejectCandidateMatchDialog.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();

        dialog.initParams(candName, vacName);
        dialog.addAfterCloseListener(afterCloseEvent -> {
            if (afterCloseEvent.closedWith(StandardOutcome.COMMIT)) {
                String reason = dialog.getSelectedReason();
                String comment = dialog.getComment();

                workflowService.recordFeedback(candId, vacId, "REJECTED", reason, comment, selected.getScore(), selected.getMatchRunId());

                selected.setRecruiterDecision("Не подходит");
                selected.setRejectionReason(reason);
                selected.setRecruiterComment(comment);
                matchesDc.replaceItem(selected);
                populateDetailPane(selected);

                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Решение сохранено")
                        .withDescription("Кандидат отмечен как «Не подходит». AI-балл сохранён без изменений.")
                        .show();
            }
        });
        dialog.show();
    }

    @Subscribe("openEntityBtn")
    public void onOpenEntityBtnClick(Button.ClickEvent event) {
        CandidateVacancyMatchItem selected = matchesTable.getSingleSelected();
        if (selected == null) return;

        if (mode == Mode.VACANCY_TO_CANDIDATES) {
            UUID candId = resolveCandidateId(selected);
            if (candId != null) {
                JobCandidate cand = dataManager.load(JobCandidate.class).id(candId).view("jobCandidate-full-view").optional().orElse(null);
                if (cand != null) {
                    screenBuilders.editor(JobCandidate.class, this)
                            .editEntity(cand)
                            .withOpenMode(OpenMode.NEW_TAB)
                            .show();
                }
            }
        } else {
            UUID vacId = resolveVacancyId(selected);
            if (vacId != null) {
                OpenPosition op = dataManager.load(OpenPosition.class).id(vacId).view("openPosition-full-view").optional().orElse(null);
                if (op != null) {
                    screenBuilders.editor(OpenPosition.class, this)
                            .withScreenClass(OpenPositionEdit.class)
                            .editEntity(op)
                            .withOpenMode(OpenMode.NEW_TAB)
                            .show();
                }
            }
        }
    }

    private void openInteraction(UUID interactionId) {
        if (interactionId == null) return;
        IteractionList it = dataManager.load(IteractionList.class).id(interactionId).view("iteractionList-full-view").optional().orElse(null);
        if (it != null) {
            screenBuilders.editor(IteractionList.class, this)
                    .withScreenClass(IteractionListEdit.class)
                    .editEntity(it)
                    .withOpenMode(OpenMode.DIALOG)
                    .show();
        }
    }

    private UUID resolveCandidateId(CandidateVacancyMatchItem item) {
        if (item.getCandidateId() != null) return item.getCandidateId();
        if (candidate != null) return candidate.getId();
        return null;
    }

    private UUID resolveVacancyId(CandidateVacancyMatchItem item) {
        if (item.getOpenPositionId() != null) return item.getOpenPositionId();
        if (openPosition != null) return openPosition.getId();
        return null;
    }

    @Subscribe("refreshAnalysisBtn")
    public void onRefreshAnalysisBtnClick(Button.ClickEvent event) {
        startAnalysis();
    }

    @Subscribe("closeBtn")
    public void onCloseBtnClick(Button.ClickEvent event) {
        closeWithDefaultAction();
    }
}
