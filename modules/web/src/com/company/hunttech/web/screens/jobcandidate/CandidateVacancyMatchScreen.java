package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.dto.CandidateVacancyMatchReport;
import com.company.hunttech.dto.CandidateVacancyMatchProgress;
import com.company.hunttech.entity.CandidateVacancyMatchItem;
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.service.CandidateSkillEnrichmentService;
import com.company.hunttech.service.CandidateVacancyMatchAiService;
import com.company.hunttech.service.CandidateVacancyWorkflowService;
import com.company.hunttech.service.dto.CandidateSkillsScanResult;
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
import com.haulmont.cuba.gui.components.Timer;
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
@DialogMode(width = "95%", height = "90%", modal = true, resizable = true)
public class CandidateVacancyMatchScreen extends Screen {

    private static final Logger log = LoggerFactory.getLogger(CandidateVacancyMatchScreen.class);

    private enum Mode {
        CANDIDATE_TO_VACANCIES,
        VACANCY_TO_CANDIDATES
    }

    private enum CandidateAnalysisStage {
        CHECKING_SKILLS,
        REFRESHING_SKILLS,
        LOADING_VACANCIES,
        MATCHING_VACANCIES
    }

    private static class CandidateAnalysisOutcome {
        private final CandidateVacancyMatchReport report;
        private final CandidateSkillsScanResult skillScanResult;
        private final String skillStatus;

        private CandidateAnalysisOutcome(CandidateVacancyMatchReport report,
                                         CandidateSkillsScanResult skillScanResult,
                                         String skillStatus) {
            this.report = report;
            this.skillScanResult = skillScanResult;
            this.skillStatus = skillStatus;
        }
    }

    private Mode mode = Mode.CANDIDATE_TO_VACANCIES;
    private JobCandidate candidate;
    private OpenPosition openPosition;
    private CandidateVacancyMatchReport currentReport;
    private List<CandidateVacancyMatchItem> allReportItems = new ArrayList<>();
    private UUID currentMatchOperationId;
    private long analysisStartedAtMillis;

    @Inject
    private CandidateVacancyMatchAiService candidateVacancyMatchAiService;

    @Inject
    private CandidateSkillEnrichmentService candidateSkillEnrichmentService;

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
    private Timer analysisProgressTimer;

    @Inject
    private HBoxLayout skillRefreshStatusBox;

    @Inject
    private Label<String> skillRefreshStatusLabel;

    @Inject
    private Label<String> analysisPhaseLabel;

    @Inject
    private Label<String> analysisStepsLabel;

    @Inject
    private Label<String> analysisPercentLabel;

    @Inject
    private Label<String> analysisEtaLabel;

    @Inject
    private Label<String> progressStepSkillsLabel;

    @Inject
    private Label<String> progressStepVacanciesLabel;

    @Inject
    private Label<String> progressStepAiLabel;

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
        if (candidate != null && candidate.getId() != null) {
            try {
                this.candidate = dataManager.load(JobCandidate.class)
                        .id(candidate.getId())
                        .view("jobCandidate-full-view")
                        .optional()
                        .orElse(candidate);
            } catch (Exception e) {
                log.warn("Не удалось дозагрузить JobCandidate по jobCandidate-full-view для id {}, используется переданный экземпляр", candidate.getId(), e);
                this.candidate = candidate;
            }
        } else {
            this.candidate = candidate;
        }
        this.mode = Mode.CANDIDATE_TO_VACANCIES;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        if (openPosition != null && openPosition.getId() != null) {
            try {
                this.openPosition = dataManager.load(OpenPosition.class)
                        .id(openPosition.getId())
                        .view("openPosition-full-view")
                        .optional()
                        .orElse(openPosition);
            } catch (Exception e) {
                log.warn("Не удалось дозагрузить OpenPosition по openPosition-full-view для id {}, используется переданный экземпляр", openPosition.getId(), e);
                this.openPosition = openPosition;
            }
        } else {
            this.openPosition = openPosition;
        }
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
        matchesDc.getMutableItems().clear();
        allReportItems.clear();
        currentReport = null;
        clearDetailsPane();
        updateToolbarActionsState();

        if (mode == Mode.CANDIDATE_TO_VACANCIES && candidate != null) {
            startCandidateVacancyAnalysis();
        } else {
            startVacancyCandidateAnalysis();
        }
    }

    private void startCandidateVacancyAnalysis() {
        currentMatchOperationId = UUID.randomUUID();
        analysisStartedAtMillis = System.currentTimeMillis();
        progressStepSkillsLabel.setValue("1  Навыки");
        progressStepVacanciesLabel.setValue("2  Открытые вакансии");
        progressStepAiLabel.setValue("3  AI-сопоставление");
        skillRefreshStatusBox.setVisible(true);
        skillRefreshStatusLabel.setValue("Проверяем даты резюме и навыков кандидата...");
        setCandidateProgress(CandidateAnalysisStage.CHECKING_SKILLS, 0, "Проверка актуальности навыков");
        analysisProgressTimer.start();

        // Подробный прогресс уже встроен в это диалоговое окно: отдельный общий spinner скрыл бы
        // текущий этап, процент, количество пакетов и оценку оставшегося времени.
        AiOperationNotifier.showStarted(notifications,
                "Запущен AI-подбор вакансий для кандидата…",
                "Сначала проверим актуальность навыков, затем сравним профиль с открытыми вакансиями.");

        final JobCandidate candidateForAnalysis = candidate;
        final UUID candidateId = candidate.getId();
        BackgroundTask<CandidateAnalysisStage, CandidateAnalysisOutcome> task =
                new BackgroundTask<CandidateAnalysisStage, CandidateAnalysisOutcome>(600, this) {
                    @Override
                    public CandidateAnalysisOutcome run(TaskLifeCycle<CandidateAnalysisStage> taskLifeCycle) throws Exception {
                        CandidateSkillsScanResult scanResult = null;
                        String skillStatus;

                        taskLifeCycle.publish(CandidateAnalysisStage.CHECKING_SKILLS);
                        CandidateCV latestCv = dataManager.load(CandidateCV.class)
                                .query("select e from hunttech_CandidateCV e " +
                                        "where e.candidate.id = :candidateId and e.textCV is not null " +
                                        "and length(trim(e.textCV)) > 0 order by e.datePost desc, e.createTs desc")
                                .parameter("candidateId", candidateId)
                                .view(viewBuilder -> viewBuilder.addAll("textCV", "datePost", "createTs", "updateTs"))
                                .maxResults(1)
                                .optional()
                                .orElse(null);

                        if (latestCv == null) {
                            skillStatus = "Нет резюме с распознанным текстом; поиск продолжится по данным профиля.";
                        } else {
                            Date latestCvTimestamp = latestUpdateTimestamp(latestCv.getCreateTs(), latestCv.getUpdateTs());
                            Date latestSkillTimestamp = dataManager.loadValue(
                                            "select max(coalesce(e.updateTs, e.createTs)) " +
                                                    "from hunttech_CandidateSkill e where e.candidate.id = :candidateId", Date.class)
                                    .parameter("candidateId", candidateId)
                                    .optional()
                                    .orElse(null);

                            boolean skillsAreStale = latestSkillTimestamp == null
                                    || (latestCvTimestamp != null && latestCvTimestamp.after(latestSkillTimestamp));
                            if (!skillsAreStale) {
                                skillStatus = "Навыки актуальны: их последняя запись не старше изменения резюме.";
                            } else {
                                taskLifeCycle.publish(CandidateAnalysisStage.REFRESHING_SKILLS);
                                try {
                                    // Используется стандартная функция извлечения навыков, но только если
                                    // последнее резюме новее последней записи навыков или навыков ещё нет.
                                    scanResult = candidateSkillEnrichmentService.scanAndEnrich(
                                            candidateForAnalysis, latestCv, null, false, true);
                                    if (scanResult.isSuccess()) {
                                        skillStatus = buildSkillRefreshStatus(scanResult);
                                    } else {
                                        skillStatus = "Не удалось обновить навыки; подбор использует сохранённые данные. "
                                                + safeText(scanResult.getRawError());
                                    }
                                } catch (Exception ex) {
                                    log.warn("Could not refresh skills before vacancy matching for candidate {}", candidateId, ex);
                                    skillStatus = "Не удалось обновить навыки; подбор использует сохранённые данные.";
                                }
                            }
                        }

                        taskLifeCycle.publish(CandidateAnalysisStage.LOADING_VACANCIES);
                        taskLifeCycle.publish(CandidateAnalysisStage.MATCHING_VACANCIES);
                        CandidateVacancyMatchReport report = candidateVacancyMatchAiService
                                .matchVacanciesForCandidate(candidateId, currentMatchOperationId);
                        return new CandidateAnalysisOutcome(report, scanResult, skillStatus);
                    }

                    @Override
                    public void progress(List<CandidateAnalysisStage> changes) {
                        if (changes == null || changes.isEmpty()) {
                            return;
                        }
                        CandidateAnalysisStage stage = changes.get(changes.size() - 1);
                        switch (stage) {
                            case CHECKING_SKILLS:
                                skillRefreshStatusLabel.setValue("Проверяем даты последнего резюме и навыков...");
                                setCandidateProgress(stage, 0, "Проверка актуальности навыков");
                                break;
                            case REFRESHING_SKILLS:
                                skillRefreshStatusLabel.setValue("Резюме новее навыков — запускаем стандартный AI-анализ навыков...");
                                setCandidateProgress(stage, 0, "Обновляем навыки по резюме");
                                break;
                            case LOADING_VACANCIES:
                                setCandidateProgress(stage, 15, "Подготавливаем открытые вакансии");
                                break;
                            case MATCHING_VACANCIES:
                                setCandidateProgress(stage, 15, "Сопоставляем профиль с требованиями вакансий");
                                break;
                            default:
                                break;
                        }
                    }

                    @Override
                    public void done(CandidateAnalysisOutcome outcome) {
                        stopAnalysisProgressTimer();
                        setBusy(false, null);
                        if (outcome != null && outcome.skillStatus != null) {
                            skillRefreshStatusLabel.setValue(outcome.skillStatus);
                        }
                        setCandidateProgressComplete();
                        if (outcome != null && outcome.report != null && outcome.report.getAiExecutionResult() == null
                                && outcome.skillScanResult != null && outcome.skillScanResult.getAiExecution() != null) {
                            AiOperationNotifier.show(notifications, outcome.skillScanResult.getAiExecution(),
                                    "Навыки кандидата обновлены", outcome.skillStatus);
                        }
                        handleReport(outcome != null ? outcome.report : null);
                    }

                    @Override
                    public boolean handleTimeoutException() {
                        stopAnalysisProgressTimer();
                        setBusy(false, null);
                        analysisProgressBar.setVisible(true);
                        analysisProgressBar.setIndeterminate(false);
                        statusLabel.setValue("Подбор не завершён за отведённое время. Можно повторить анализ.");
                        analysisPhaseLabel.setValue("Время ожидания истекло");
                        analysisEtaLabel.setValue("Осталось: оценка недоступна");
                        log.error("Timeout during candidate-vacancy matching for candidate {}", candidateId);
                        return true;
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        stopAnalysisProgressTimer();
                        setBusy(false, null);
                        analysisProgressBar.setVisible(true);
                        analysisProgressBar.setIndeterminate(false);
                        log.error("Error during candidate-vacancy matching for candidate {}", candidateId, ex);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Ошибка AI-подбора")
                                .withDescription("Не удалось выполнить анализ. Подробности записаны в журнале операции.")
                                .show();
                        statusLabel.setValue("Ошибка при выполнении AI-анализа. Сохранённые навыки не изменены.");
                        analysisPhaseLabel.setValue("Не удалось завершить подбор");
                        analysisEtaLabel.setValue("Осталось: оценка недоступна");
                        return true;
                    }
                };

        backgroundWorker.handle(task).execute();
    }

    private void startVacancyCandidateAnalysis() {
        currentMatchOperationId = UUID.randomUUID();
        final UUID operationId = currentMatchOperationId;
        analysisStartedAtMillis = System.currentTimeMillis();
        skillRefreshStatusBox.setVisible(false);
        setBusy(true, "AI анализирует профили и формирует ранжированные рекомендации...");
        analysisProgressBar.setIndeterminate(false);
        analysisProgressBar.setValue(0.05);
        analysisPercentLabel.setValue("5%");
        analysisPhaseLabel.setValue("Определение должностей");
        analysisStepsLabel.setValue("Этап 1 из 3 · Должности");
        statusLabel.setValue("Определение целевых должностей вакансии и сопоставление со справочником...");
        analysisEtaLabel.setValue("Прошло: 0 сек · Осталось: оценивается");

        progressStepSkillsLabel.setValue("1  Должности вакансии");
        progressStepVacanciesLabel.setValue("2  Выборка кандидатов");
        progressStepAiLabel.setValue("3  AI-сопоставление");
        setProgressStepActive(progressStepSkillsLabel, true);
        setProgressStepActive(progressStepVacanciesLabel, false);
        setProgressStepActive(progressStepAiLabel, false);

        analysisProgressTimer.start();

        BackgroundTask<Integer, CandidateVacancyMatchReport> task =
                new BackgroundTask<Integer, CandidateVacancyMatchReport>(240, this) {
                    @Override
                    public CandidateVacancyMatchReport run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                        taskLifeCycle.publish(1);
                        if (mode == Mode.VACANCY_TO_CANDIDATES && openPosition != null) {
                            return candidateVacancyMatchAiService.matchCandidatesForVacancy(openPosition.getId(), operationId);
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
                        stopAnalysisProgressTimer();
                        setBusy(false, null);
                        analysisProgressBar.setIndeterminate(false);
                        analysisProgressBar.setValue(1.0);
                        analysisPercentLabel.setValue("100%");
                        long elapsedMillis = Math.max(0, System.currentTimeMillis() - analysisStartedAtMillis);
                        analysisEtaLabel.setValue("Прошло: " + formatDuration(elapsedMillis) + " · Завершено");
                        handleReport(report);
                    }

                    @Override
                    public boolean handleTimeoutException() {
                        stopAnalysisProgressTimer();
                        setBusy(false, null);
                        analysisProgressBar.setIndeterminate(false);
                        log.error("Timeout during vacancy-to-candidates matching: operationId={}", operationId);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Превышено время AI-подбора")
                                .withDescription("Анализ занял слишком много времени. Код обращения: " + operationId)
                                .show();
                        statusLabel.setValue("Превышено время выполнения AI-анализа. Код обращения: " + operationId);
                        return true;
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        stopAnalysisProgressTimer();
                        setBusy(false, null);
                        analysisProgressBar.setIndeterminate(false);
                        log.error("Vacancy-to-candidates task failed: operationId={}, errorType={}",
                                operationId, ex.getClass().getSimpleName());
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Ошибка AI-подбора")
                                .withDescription("Не удалось завершить анализ. Сообщите код обращения службе поддержки: " + operationId)
                                .show();
                        statusLabel.setValue("Ошибка при выполнении AI-анализа. Код обращения: " + operationId);
                        return true;
                    }
                };

        backgroundWorker.handle(task).execute();
    }

    private void setCandidateProgress(CandidateAnalysisStage stage, int percent, String status) {
        analysisProgressBar.setVisible(true);
        analysisProgressBar.setIndeterminate(stage == CandidateAnalysisStage.CHECKING_SKILLS
                || stage == CandidateAnalysisStage.REFRESHING_SKILLS);
        analysisProgressBar.setValue(Math.max(0, Math.min(100, percent)) / 100.0);
        analysisPercentLabel.setValue(percent + "%");
        analysisEtaLabel.setValue(stage == CandidateAnalysisStage.CHECKING_SKILLS
                || stage == CandidateAnalysisStage.REFRESHING_SKILLS
                || stage == CandidateAnalysisStage.LOADING_VACANCIES
                ? "Осталось: оценка после первого AI-пакета" : "Осталось: рассчитываем");
        analysisPhaseLabel.setValue(status);
        analysisStepsLabel.setValue(formatStepCount(stage));
        statusLabel.setValue(status);
        updateProgressStepStyles(stage);
    }

    private String formatStepCount(CandidateAnalysisStage stage) {
        switch (stage) {
            case CHECKING_SKILLS:
            case REFRESHING_SKILLS:
                return "Этап 1 из 3";
            case LOADING_VACANCIES:
                return "Этап 2 из 3";
            case MATCHING_VACANCIES:
                return "Этап 3 из 3";
            default:
                return "Этап 1 из 3";
        }
    }

    private void updateProgressStepStyles(CandidateAnalysisStage stage) {
        boolean skillsActive = stage == CandidateAnalysisStage.CHECKING_SKILLS
                || stage == CandidateAnalysisStage.REFRESHING_SKILLS;
        boolean vacanciesActive = stage == CandidateAnalysisStage.LOADING_VACANCIES;
        setProgressStepActive(progressStepSkillsLabel,
                skillsActive || vacanciesActive || stage == CandidateAnalysisStage.MATCHING_VACANCIES);
        setProgressStepActive(progressStepVacanciesLabel,
                vacanciesActive || stage == CandidateAnalysisStage.MATCHING_VACANCIES);
        setProgressStepActive(progressStepAiLabel, stage == CandidateAnalysisStage.MATCHING_VACANCIES);
    }

    private void setProgressStepActive(Label<String> stepLabel, boolean active) {
        stepLabel.removeStyleName("candidate-vacancy-match-step-active");
        if (active) {
            stepLabel.addStyleName("candidate-vacancy-match-step-active");
        }
    }

    @Subscribe("analysisProgressTimer")
    public void onAnalysisProgressTimer(Timer.TimerActionEvent event) {
        if (currentMatchOperationId == null) {
            return;
        }
        if (mode == Mode.VACANCY_TO_CANDIDATES) {
            updateVacancyCandidateProgress(currentMatchOperationId);
            return;
        }
        try {
            CandidateVacancyMatchProgress progress = candidateVacancyMatchAiService
                    .getVacancyMatchProgress(currentMatchOperationId);
            if (progress == null) {
                long elapsedSeconds = Math.max(0, (System.currentTimeMillis() - analysisStartedAtMillis) / 1000);
                analysisEtaLabel.setValue("Прошло: " + formatDuration(elapsedSeconds * 1000)
                        + " · Осталось: оценивается");
                return;
            }

            boolean matchingStarted = progress.getTotalVacancies() > 0 && progress.getTotalChunks() > 0;
            if (!matchingStarted) {
                setCandidateProgress(CandidateAnalysisStage.LOADING_VACANCIES, 15,
                        "Загружаем открытые вакансии и готовим контекст кандидата");
                statusLabel.setValue(progress.getStatusMessage());
                return;
            }

            int percent = 15 + (int) Math.round(progress.getProgressPercent() * 0.85);
            percent = Math.max(15, Math.min(99, percent));
            analysisProgressBar.setIndeterminate(false);
            analysisProgressBar.setValue(percent / 100.0);
            analysisPercentLabel.setValue(percent + "%");
            analysisPhaseLabel.setValue("AI-сопоставление вакансий " + progress.getCompletedChunks()
                    + " из " + progress.getTotalChunks());
            analysisStepsLabel.setValue("Этап 3 из 3 · " + progress.getProcessedVacancies()
                    + " из " + progress.getTotalVacancies() + " вакансий");
            statusLabel.setValue(progress.getStatusMessage());
            updateProgressStepStyles(CandidateAnalysisStage.MATCHING_VACANCIES);
            Long remainingMillis = progress.getEstimatedRemainingMillis();
            analysisEtaLabel.setValue(remainingMillis == null
                    ? "Прошло: " + formatDuration(progress.getElapsedMillis()) + " · Осталось: оценивается"
                    : "Прошло: " + formatDuration(progress.getElapsedMillis())
                    + " · Осталось: примерно " + formatDuration(remainingMillis));
        } catch (Exception ex) {
            // Не прерываем AI-операцию, если очередной снимок прогресса временно недоступен.
            log.debug("Could not poll candidate-vacancy match progress", ex);
        }
    }

    private void updateVacancyCandidateProgress(UUID operationId) {
        try {
            CandidateVacancyMatchProgress progress = candidateVacancyMatchAiService
                    .getVacancyMatchProgress(operationId);
            long elapsedMillis = Math.max(0, System.currentTimeMillis() - analysisStartedAtMillis);
            String elapsedFormatted = formatDuration(elapsedMillis);

            if (progress == null) {
                analysisEtaLabel.setValue("Прошло: " + elapsedFormatted + " · Осталось: оценивается");
                return;
            }

            int totalCandidates = progress.getTotalVacancies();
            int processedCandidates = progress.getProcessedVacancies();
            String phase = progress.getPhase();
            String statusMsg = progress.getStatusMessage();

            if (totalCandidates <= 0) {
                boolean isSelecting = phase != null && (phase.contains("Выборка") || phase.contains("Поиск"));
                setProgressStepActive(progressStepSkillsLabel, true);
                setProgressStepActive(progressStepVacanciesLabel, isSelecting);
                setProgressStepActive(progressStepAiLabel, false);

                int percent = isSelecting ? 15 : 8;
                analysisProgressBar.setValue(percent / 100.0);
                analysisPercentLabel.setValue(percent + "%");
                analysisStepsLabel.setValue(isSelecting ? "Этап 2 из 3 · Выборка кандидатов" : "Этап 1 из 3 · Должности");
                analysisPhaseLabel.setValue(phase != null ? phase : "Определение должностей");
                statusLabel.setValue(statusMsg != null ? statusMsg : "Определение целевых должностей и поиск в справочнике...");
                analysisEtaLabel.setValue("Прошло: " + elapsedFormatted + " · Осталось: оценивается");
            } else {
                setProgressStepActive(progressStepSkillsLabel, true);
                setProgressStepActive(progressStepVacanciesLabel, true);
                setProgressStepActive(progressStepAiLabel, true);

                int percent = 20 + (int) Math.round(progress.getProgressPercent() * 0.78);
                percent = Math.max(20, Math.min(99, percent));
                analysisProgressBar.setValue(percent / 100.0);
                analysisPercentLabel.setValue(percent + "%");

                analysisStepsLabel.setValue("Этап 3 из 3 · Кандидатов: " + processedCandidates + " из " + totalCandidates);
                analysisPhaseLabel.setValue(phase != null ? phase : "AI-анализ кандидатов");
                statusLabel.setValue(statusMsg != null ? statusMsg : "Сопоставление резюме и навыков с вакансией...");

                Long remainingMillis = progress.getEstimatedRemainingMillis();
                if (remainingMillis != null && remainingMillis > 0) {
                    analysisEtaLabel.setValue("Прошло: " + elapsedFormatted
                            + " · Кандидатов: " + processedCandidates + " из " + totalCandidates
                            + " · Осталось: примерно " + formatDuration(remainingMillis));
                } else {
                    analysisEtaLabel.setValue("Прошло: " + elapsedFormatted
                            + " · Кандидатов: " + processedCandidates + " из " + totalCandidates
                            + " · Осталось: оценивается");
                }
            }
        } catch (Exception ex) {
            log.debug("Could not poll vacancy-candidate match progress", ex);
        }
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        stopAnalysisProgressTimer();
    }

    private void stopAnalysisProgressTimer() {
        if (analysisProgressTimer != null) {
            analysisProgressTimer.stop();
        }
    }

    private void setCandidateProgressComplete() {
        analysisProgressBar.setVisible(true);
        analysisProgressBar.setIndeterminate(false);
        analysisProgressBar.setValue(1.0);
        analysisPercentLabel.setValue("100%");
        analysisStepsLabel.setValue("Этап 3 из 3 · завершено");
        analysisPhaseLabel.setValue("Подбор завершён");
        analysisEtaLabel.setValue("Осталось: 0");
        statusLabel.setValue("Подготовлен отчёт по открытым вакансиям.");
    }

    private Date latestUpdateTimestamp(Date createdAt, Date updatedAt) {
        if (createdAt == null) return updatedAt;
        if (updatedAt == null) return createdAt;
        return updatedAt.after(createdAt) ? updatedAt : createdAt;
    }

    private String buildSkillRefreshStatus(CandidateSkillsScanResult result) {
        int added = result.getAddedSkills() != null ? result.getAddedSkills().size() : 0;
        int updated = result.getUpdatedSkills() != null ? result.getUpdatedSkills().size() : 0;
        int unchanged = result.getUnchangedSkills() != null ? result.getUnchangedSkills().size() : 0;
        if (added == 0 && updated == 0 && unchanged == 0) {
            return "Проверка навыков завершена; изменения не требуются.";
        }
        return String.format("Навыки актуализированы: новых — %d, обновлено — %d, без изменений — %d.",
                added, updated, unchanged);
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "" : value.trim();
    }

    private String formatDuration(long millis) {
        long seconds = Math.max(0, millis / 1000);
        if (seconds < 60) {
            return seconds + " сек";
        }
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return remainingSeconds == 0 ? minutes + " мин" : minutes + " мин " + remainingSeconds + " сек";
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
            Notifications.NotificationType notificationType = msg.contains("Код обращения:")
                    ? Notifications.NotificationType.ERROR
                    : Notifications.NotificationType.WARNING;
            notifications.create(notificationType)
                    .withCaption("AI-подбор")
                    .withDescription(msg)
                    .show();
            return;
        }

        this.currentReport = report;
        this.allReportItems = new ArrayList<>(report.getItems());
        applyFilter(decisionFilter.getValue());

        if (report.isFallbackUsed()) {
            String fallbackMessage = report.getStatusMessage() != null && !report.getStatusMessage().trim().isEmpty()
                    ? report.getStatusMessage()
                    : "AI-анализ недоступен. Выполнена предварительная оценка по сохранённым данным.";
            statusLabel.setValue(fallbackMessage);
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Внимание")
                    .withDescription(fallbackMessage)
                    .show();
        } else if (report.getStatusMessage() != null && !report.getStatusMessage().trim().isEmpty()) {
            statusLabel.setValue(report.getStatusMessage());
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("AI-подбор завершён с замечаниями")
                    .withDescription(report.getStatusMessage())
                    .show();
            if (report.getAiExecutionResult() != null) {
                AiOperationNotifier.show(notifications, report.getAiExecutionResult(),
                        "AI-подбор завершён с замечаниями",
                        String.format("Проанализировано объектов: %d, сформировано рекомендаций: %d",
                                report.getTotalVacanciesAnalyzed(), report.getItems().size()));
            }
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
            bulkTakeIntoWorkBtn.setCaption("Взять выбранных (" + count + ")");
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
        IteractionList it = dataManager.load(IteractionList.class).id(interactionId).view("iteractionList-view").optional().orElse(null);
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
