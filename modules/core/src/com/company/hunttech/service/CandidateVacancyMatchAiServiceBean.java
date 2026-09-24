package com.company.hunttech.service;

import com.company.hunttech.dto.CandidateAiSummary;
import com.company.hunttech.dto.CandidateVacancyMatchReport;
import com.company.hunttech.dto.CandidateVacancyMatchProgress;
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.CandidateSkill;
import com.company.hunttech.entity.CandidateSkillPriority;
import com.company.hunttech.entity.CandidateVacancyMatchItem;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.SkillTree;
import com.company.hunttech.entity.VacancyCandidateMatchRun;
import com.company.hunttech.entity.JobHistory;
import com.company.hunttech.entity.Position;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Реализация сервиса AI-сопоставления кандидата с открытыми вакансиями компании.
 */
@Service(CandidateVacancyMatchAiService.NAME)
public class CandidateVacancyMatchAiServiceBean implements CandidateVacancyMatchAiService {

    private static final Logger log = LoggerFactory.getLogger(CandidateVacancyMatchAiServiceBean.class);

    private static final int MAX_VACANCIES_PER_CHUNK = 12;
    private static final int MAX_VACANCY_TEXT_CHARS_PER_CHUNK = 40000;
    private static final int MAX_RESUME_TEXT_CHARS = 20000;
    private static final int MAX_ADDITIONAL_SNIPPETS_CHARS = 5000;
    private static final int FALLBACK_BASE_EXPERIENCE_FIT = 10;
    private static final long PROGRESS_TTL_MILLIS = 60L * 60L * 1000L;

    @Inject
    private DataManager dataManager;

    @Inject
    private AiExecutionService aiExecutionService;

    @Inject
    private CandidateVacancyWorkflowService workflowService;

    @Inject
    private FileLoader fileLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<UUID, MatchProgressState> progressStates = new ConcurrentHashMap<>();

    @Override
    public CandidateVacancyMatchReport matchVacanciesForCandidate(UUID candidateId) {
        return matchVacanciesForCandidate(candidateId, UUID.randomUUID());
    }

    @Override
    public CandidateVacancyMatchReport matchVacanciesForCandidate(UUID candidateId, UUID operationId) {
        cleanupExpiredProgress();
        UUID effectiveOperationId = operationId != null ? operationId : UUID.randomUUID();
        MatchProgressState progress = new MatchProgressState(effectiveOperationId, System.currentTimeMillis());
        progressStates.put(effectiveOperationId, progress);

        try {
            CandidateVacancyMatchReport report = doMatchVacanciesForCandidate(candidateId, progress);
            progress.finish(report.isSuccess(), report.getStatusMessage());
            return report;
        } catch (RuntimeException e) {
            progress.finish(false, "Подбор прерван из-за ошибки обработки.");
            throw e;
        }
    }

    @Override
    public CandidateVacancyMatchProgress getVacancyMatchProgress(UUID operationId) {
        if (operationId == null) {
            return null;
        }
        cleanupExpiredProgress();
        MatchProgressState progress = progressStates.get(operationId);
        return progress != null ? progress.snapshot(System.currentTimeMillis()) : null;
    }

    private CandidateVacancyMatchReport doMatchVacanciesForCandidate(UUID candidateId, MatchProgressState progress) {
        if (candidateId == null) {
            CandidateVacancyMatchReport emptyReport = new CandidateVacancyMatchReport();
            emptyReport.setSuccess(false);
            emptyReport.setStatusMessage("Идентификатор кандидата не указан.");
            return emptyReport;
        }

        // 1. Загрузка кандидата
        JobCandidate candidate = dataManager.load(JobCandidate.class)
                .id(candidateId)
                .view("jobCandidate-llm-view")
                .optional()
                .orElse(null);

        if (candidate == null) {
            CandidateVacancyMatchReport emptyReport = new CandidateVacancyMatchReport();
            emptyReport.setSuccess(false);
            emptyReport.setStatusMessage("Кандидат с указанным ID не найден в системе.");
            return emptyReport;
        }

        // 2. Загрузка резюме с непустым текстом, отсортированных по datePost DESC
        List<CandidateCV> cvList = dataManager.load(CandidateCV.class)
                .query("select e from hunttech_CandidateCV e where e.candidate.id = :candId and e.textCV is not null and length(trim(e.textCV)) > 0 order by e.datePost desc, e.createTs desc")
                .parameter("candId", candidateId)
                .view(viewBuilder -> viewBuilder.addAll("textCV", "datePost", "resumePosition", "toVacancy", "letter", "commentLetter", "originalFileCV", "fileCV"))
                .list();

        if (cvList.isEmpty()) {
            CandidateVacancyMatchReport emptyReport = new CandidateVacancyMatchReport();
            emptyReport.setCandidateId(candidateId);
            emptyReport.setCandidateFullName(candidate.getFullName());
            emptyReport.setSuccess(false);
            emptyReport.setStatusMessage("Для AI-подбора вакансии необходимо резюме кандидата с распознанным текстом.");
            return emptyReport;
        }

        // 3. Загрузка структурированных навыков кандидата
        List<CandidateSkill> candidateSkills = dataManager.load(CandidateSkill.class)
                .query("select e from hunttech_CandidateSkill e where e.candidate.id = :candId")
                .parameter("candId", candidateId)
                .view(viewBuilder -> viewBuilder.add("skill.skillName").add("priority"))
                .list();

        // 4. Загрузка открытых вакансий: openClose != true (openClose = false or openClose is null)
        List<OpenPosition> openPositions = dataManager.load(OpenPosition.class)
                .query("select e from hunttech_OpenPosition e where (e.openClose = false or e.openClose is null) order by e.priority desc, e.vacansyName asc")
                .view(viewBuilder -> viewBuilder.addAll(
                        "vacansyID", "vacansyName", "positionType.positionRuName", "comment", "shortDescription",
                        "workExperience", "grade", "skillsList.skillName", "remoteWork",
                        "remoteComment", "cityPosition.cityRuName", "cities.cityRuName",
                        "projectName.projectName", "priority", "openClose",
                        "salaryMin", "salaryMax", "salaryIE", "salaryFixLimit", "salaryCandidateRequest", "salaryComment"
                ))
                .list();

        // Строгая фильтрация: гарантируем, что используются ТОЛЬКО открытые вакансии (openClose != true)
        openPositions = openPositions.stream()
                .filter(CandidateVacancyMatchAiServiceBean::isOpenVacancy)
                .collect(Collectors.toList());

        if (openPositions.isEmpty()) {
            CandidateVacancyMatchReport emptyReport = new CandidateVacancyMatchReport();
            emptyReport.setCandidateId(candidateId);
            emptyReport.setCandidateFullName(candidate.getFullName());
            emptyReport.setSuccess(false);
            emptyReport.setStatusMessage("В системе нет открытых вакансий для анализа.");
            return emptyReport;
        }

        // 5. Формирование контекста кандидата с глубоким анализом истории взаимодействий и прошлых отказов
        CandidateInteractionProfile interactionProfile = loadCandidateInteractionProfile(candidateId);
        String candidateSkillsText = buildCandidateSkillsString(candidateSkills);
        String candidateResumeText = buildCandidateResumeText(cvList);
        if (interactionProfile.candidateSalaryExpectations == null || interactionProfile.candidateSalaryExpectations.trim().isEmpty()) {
            interactionProfile.candidateSalaryExpectations = extractCandidateSalaryFromResume(candidateResumeText);
        }
        String candidateProfile = buildCandidateProfileString(candidate)
                + "\n\n" + interactionProfile.toPromptSection();

        // 6. Батчинг вакансий на чанки
        List<List<OpenPosition>> chunks = splitIntoChunks(openPositions);
        progress.beginAnalysis(openPositions.size(), chunks.size());
        log.info("AI matching for candidate {}: {} open positions split into {} chunks",
                candidate.getFullName(), openPositions.size(), chunks.size());

        CandidateVacancyMatchReport report = new CandidateVacancyMatchReport();
        report.setCandidateId(candidateId);
        report.setCandidateFullName(candidate.getFullName());
        report.setCandidatePosition(candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : null);
        report.setCandidateCity(candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : null);
        report.setCandidateCurrentCompany(candidate.getCurrentCompany() != null ?
                (candidate.getCurrentCompany().getComanyName() != null ? candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName()) : null);
        report.setTotalVacanciesAnalyzed(openPositions.size());

        Map<UUID, OpenPosition> positionById = openPositions.stream()
                .collect(Collectors.toMap(OpenPosition::getId, p -> p, (p1, p2) -> p1));

        List<CandidateVacancyMatchItem> allItems = new ArrayList<>();
        Set<UUID> seenVacancyIds = new HashSet<>();
        AiExecutionResult lastAiResult = null;
        boolean anyAiSuccess = false;

        for (int i = 0; i < chunks.size(); i++) {
            List<OpenPosition> chunk = chunks.get(i);
            progress.startChunk(i + 1, chunk.size());
            String vacanciesJson = buildVacanciesJson(chunk);

            Map<String, Object> context = new LinkedHashMap<>();
            context.put(PARAM_CANDIDATE_PROFILE, candidateProfile);
            context.put(PARAM_CANDIDATE_SKILLS, candidateSkillsText);
            context.put(PARAM_CANDIDATE_RESUME_TEXT, candidateResumeText);
            context.put(PARAM_VACANCIES_JSON, vacanciesJson);
            context.put("callerSource", "CandidateVacancyMatch:candidate-to-vacancies");
            context.put("requestId", progress.getOperationId() + "-chunk-" + (i + 1));

            boolean chunkSucceeded = false;
            try {
                AiExecutionResult aiResult = aiExecutionService.executeText(FUNCTION_CODE, context);
                if (aiResult != null && aiResult.getText() != null && !aiResult.getText().trim().isEmpty()) {
                    lastAiResult = aiResult;
                    List<CandidateVacancyMatchItem> parsedItems = parseAiResponse(aiResult.getText(), chunk, positionById, seenVacancyIds, report);
                    if (!parsedItems.isEmpty()) {
                        allItems.addAll(parsedItems);
                        anyAiSuccess = true;
                        chunkSucceeded = true;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed AI execution for candidate-vacancy chunk {}/{}: {}", i + 1, chunks.size(), e.getMessage());
            } finally {
                progress.completeChunk(chunk.size(), chunkSucceeded);
            }
        }

        // Если AI отработал успешно хотя бы по части вакансий
        if (anyAiSuccess) {
            // Для вакансий, которые по какой-то причине не вернулись в ответе LLM,
            // добавляем нейтральный элемент, чтобы общее количество соответствовало открытым вакансиям
            for (OpenPosition op : openPositions) {
                if (!seenVacancyIds.contains(op.getId())) {
                    CandidateVacancyMatchItem missingItem = new CandidateVacancyMatchItem();
                    missingItem.setOpenPositionId(op.getId());
                    missingItem.setOpenPosition(op);
                    missingItem.setVacancyName(op.getVacansyName());
                    missingItem.setProjectName(op.getProjectName() != null ? op.getProjectName().getProjectName() : "");
                    missingItem.setPositionName(op.getPositionType() != null ? op.getPositionType().getPositionRuName() : "");
                    missingItem.setPriority(op.getPriority());
                    missingItem.setScore(0);
                    missingItem.setRoleFit(0);
                    missingItem.setSkillsFit(0);
                    missingItem.setExperienceFit(0);
                    missingItem.setPreferencesFit(0);
                    missingItem.setDomainFit(0);
                    missingItem.setVerdict("Не оценен AI");
                    missingItem.setMatchedSkills(Collections.emptyList());
                    missingItem.setMissingCriticalRequirements(Collections.emptyList());
                    missingItem.setRisks(Collections.emptyList());
                    missingItem.setReasonsToOffer(Collections.emptyList());
                    missingItem.setSummary("Вакансия не была возвращена нейросетью в аналитическом пакете.");
                    allItems.add(missingItem);
                    seenVacancyIds.add(op.getId());
                }
            }
            enrichItemsWithCandidateAndStatus(allItems, candidate, null, interactionProfile);
            allItems.sort(getComparator());
            report.setItems(allItems);
            report.setMatchedVacanciesCount(allItems.size());
            applySalaryExpectationsFallback(report, interactionProfile);
            report.setAiExecutionResult(lastAiResult);
            report.setFallbackUsed(false);
            report.setSuccess(true);
            return report;
        }

        // 7. Честный Fallback при недоступности AI: рассчитываем базовую эвристическую оценку без фальсификации
        log.warn("AI service unavailable for candidate-vacancy match. Using honest rule-based fallback.");
        List<CandidateVacancyMatchItem> fallbackItems = buildHonestFallbackItems(candidate, candidateSkills, candidateResumeText, openPositions);
        enrichItemsWithCandidateAndStatus(fallbackItems, candidate, null, interactionProfile);
        fallbackItems.sort(getComparator());

        report.setItems(fallbackItems);
        report.setMatchedVacanciesCount(fallbackItems.size());
        applySalaryExpectationsFallback(report, interactionProfile);
        report.setFallbackUsed(true);
        report.setGeneralConclusion("Внимание: экспертный AI-анализ временно недоступен. Отображена предварительная оценка по совпадению ключевых параметров.");
        report.setStatusMessage("Предварительная оценка без AI");
        report.setSuccess(true);
        return report;
    }

    private void applySalaryExpectationsFallback(CandidateVacancyMatchReport report, CandidateInteractionProfile profile) {
        if (report != null && report.getCandidateSummary() != null &&
                (report.getCandidateSummary().getCandidateSalaryExpectations() == null || report.getCandidateSummary().getCandidateSalaryExpectations().isEmpty())) {
            report.getCandidateSummary().setCandidateSalaryExpectations(
                    profile != null && profile.candidateSalaryExpectations != null ? profile.candidateSalaryExpectations : "Не указаны"
            );
        }
    }

    @Override
    public CandidateVacancyMatchReport matchCandidatesForVacancy(UUID openPositionId) {
        return matchCandidatesForVacancy(openPositionId, UUID.randomUUID());
    }

    @Override
    public CandidateVacancyMatchReport matchCandidatesForVacancy(UUID openPositionId, UUID requestedOperationId) {
        UUID operationId = requestedOperationId != null ? requestedOperationId : UUID.randomUUID();
        if (openPositionId == null) {
            CandidateVacancyMatchReport emptyReport = new CandidateVacancyMatchReport();
            emptyReport.setSuccess(false);
            emptyReport.setStatusMessage("Идентификатор вакансии не указан.");
            return emptyReport;
        }

        log.info("Starting vacancy-to-candidates matching: operationId={}, vacancyId={}", operationId, openPositionId);
        cleanupExpiredProgress();
        MatchProgressState progress = new MatchProgressState(operationId, System.currentTimeMillis());
        progressStates.put(operationId, progress);

        try {
            progress.updateStage("Чтение вакансии", "Загрузка параметров и требований вакансии...");

            // 1. Загрузка вакансии
            OpenPosition vacancy = dataManager.load(OpenPosition.class)
                    .id(openPositionId)
                    .view(viewBuilder -> viewBuilder.addAll(
                            "vacansyID", "vacansyName", "positionType.positionRuName", "positionType.positionEnName",
                            "comment", "shortDescription", "workExperience", "grade", "skillsList.skillName",
                            "remoteWork", "remoteComment", "cityPosition.cityRuName", "cities.cityRuName",
                            "projectName.projectName", "priority", "openClose", "searchMap",
                            "salaryMin", "salaryMax", "salaryIE", "salaryFixLimit", "salaryCandidateRequest", "salaryComment"
                    ))
                    .optional()
                    .orElse(null);

            if (vacancy == null) {
                CandidateVacancyMatchReport emptyReport = new CandidateVacancyMatchReport();
                emptyReport.setSuccess(false);
                emptyReport.setStatusMessage("Вакансия с указанным ID не найдена.");
                progress.finish(false, emptyReport.getStatusMessage());
                return emptyReport;
            }

            if (Boolean.TRUE.equals(vacancy.getOpenClose())) {
                CandidateVacancyMatchReport emptyReport = new CandidateVacancyMatchReport();
                emptyReport.setSuccess(false);
                emptyReport.setStatusMessage("Вакансия закрыта (openClose = true). AI-подбор кандидатов доступен только для открытых вакансий.");
                progress.finish(false, emptyReport.getStatusMessage());
                return emptyReport;
            }

            // 2. Определение должностей вакансии через карту поиска / AI и справочник Position
            List<Position> targetPositions = resolveVacancyPositions(vacancy, operationId, progress);

            // 3. Выборка релевантного пула кандидатов из JobCandidate с учетом доступности
            progress.updateStage("Выборка кандидатов", "Поиск подходящих кандидатов в базе данных HRM...");
            List<JobCandidate> candidates = Collections.emptyList();
            if (!targetPositions.isEmpty()) {
                List<UUID> posIds = targetPositions.stream().map(Position::getId).collect(Collectors.toList());
                try {
                    candidates = dataManager.load(JobCandidate.class)
                            .query("select distinct e from hunttech_JobCandidate e where (e.blockCandidate = false or e.blockCandidate is null) and (e.personPosition.id in :posIds or exists (select pl from hunttech_JobCandidatePositionLists pl where pl.jobCandidate.id = e.id and pl.positionList.id in :posIds)) order by e.updateTs desc")
                            .parameter("posIds", posIds)
                            .view("jobCandidate-full-view")
                            .maxResults(25)
                            .list();
                } catch (Exception ex) {
                    log.debug("Query candidates by positions failed: {}", ex.getMessage());
                }
            }

            // Fallback-выборка при отсутствии кандидатов по точным должностям
            if (candidates.isEmpty()) {
                try {
                    candidates = dataManager.load(JobCandidate.class)
                            .query("select distinct e from hunttech_JobCandidate e where (e.blocked = false or e.blocked is null) and exists (select cv from hunttech_CandidateCV cv where cv.candidate.id = e.id and cv.textCV is not null and length(trim(cv.textCV)) > 0) order by e.updateTs desc")
                            .view("jobCandidate-full-view")
                            .maxResults(25)
                            .list();
                } catch (Exception ex) {
                    log.debug("Resume candidate query failed: {}", ex.getMessage());
                }
            }

            if (candidates.isEmpty()) {
                try {
                    candidates = dataManager.load(JobCandidate.class)
                            .query("select e from hunttech_JobCandidate e where e.blocked = false or e.blocked is null order by e.updateTs desc")
                            .view("jobCandidate-full-view")
                            .maxResults(25)
                            .list();
                } catch (Exception ex) {
                    log.debug("Fallback candidate query failed: {}", ex.getMessage());
                }
            }

            if (candidates.isEmpty()) {
                CandidateVacancyMatchReport emptyReport = new CandidateVacancyMatchReport();
                emptyReport.setSuccess(false);
                emptyReport.setStatusMessage(targetPositions.isEmpty() && vacancy.getPositionType() == null
                        ? "Не удалось сопоставить должности вакансии со справочником должностей системы. Укажите должность вакансии в карточке или проверьте карту поиска. Код обращения: " + operationId
                        : "В базе нет подходящих кандидатов для анализа. Код обращения: " + operationId);
                progress.finish(false, emptyReport.getStatusMessage());
                return emptyReport;
            }

            progress.beginCandidateAnalysis(candidates.size());

            CandidateVacancyMatchReport report = new CandidateVacancyMatchReport();
            report.setTotalVacanciesAnalyzed(candidates.size());
            report.setSuccess(true);

            List<CandidateVacancyMatchItem> allItems = new ArrayList<>();
            List<OpenPosition> singleVacancyList = Collections.singletonList(vacancy);
            String vacanciesJson = buildVacanciesJson(singleVacancyList);
            Map<UUID, OpenPosition> positionById = Collections.singletonMap(vacancy.getId(), vacancy);

            AiExecutionResult lastAiResult = null;
            int candidateSequence = 0;
            int failedCandidates = 0;
            int fallbackCandidates = 0;
            int aiCallFailures = 0;
            int emptyAiResponses = 0;
            int unusableAiResponses = 0;
            boolean anyAiMatchParsed = false;

            for (JobCandidate cand : candidates) {
                candidateSequence++;
                String candName = cand.getFullName() != null ? cand.getFullName() : ("Кандидат " + candidateSequence);
                String candPos = cand.getPersonPosition() != null ? cand.getPersonPosition().getPositionRuName() : "";
                progress.startCandidate(candidateSequence, candName, candPos);

                String phase = "load-resume";
                String requestId = operationId + "-candidate-" + candidateSequence;
                CandidateVacancyMatchItem matchedItem = null;
                try {
                    List<CandidateCV> cvList = Collections.emptyList();
                    try {
                        cvList = dataManager.load(CandidateCV.class)
                                .query("select e from hunttech_CandidateCV e where e.candidate.id = :candId order by e.datePost desc, e.createTs desc")
                                .parameter("candId", cand.getId())
                                .view(viewBuilder -> viewBuilder.addAll("textCV", "datePost", "resumePosition", "toVacancy", "letter", "commentLetter", "originalFileCV", "fileCV"))
                                .list();
                    } catch (Exception ex) {
                        log.debug("Load CV list failed for candidate {}: {}", cand.getId(), ex.getMessage());
                    }

                    phase = "load-skills";
                    List<CandidateSkill> candidateSkills = Collections.emptyList();
                    try {
                        candidateSkills = dataManager.load(CandidateSkill.class)
                                .query("select e from hunttech_CandidateSkill e where e.candidate.id = :candId")
                                .parameter("candId", cand.getId())
                                .view(viewBuilder -> viewBuilder.add("skill.skillName").add("priority"))
                                .list();
                    } catch (Exception ex) {
                        log.debug("Load skills failed for candidate {}: {}", cand.getId(), ex.getMessage());
                    }

                    phase = "prepare-context";
                    CandidateInteractionProfile interactionProfile = loadCandidateInteractionProfile(cand.getId());
                    String candidateSkillsText = buildCandidateSkillsString(candidateSkills);
                    String candidateResumeText = buildCandidateResumeText(cvList);
                    if (interactionProfile.candidateSalaryExpectations == null || interactionProfile.candidateSalaryExpectations.trim().isEmpty()) {
                        interactionProfile.candidateSalaryExpectations = extractCandidateSalaryFromResume(candidateResumeText);
                    }
                    String lastJobDomain = resolveLastJobDomainDescription(cand);

                    String candidateProfile = buildCandidateProfileString(cand)
                            + "\nПредметная область последнего места работы: " + lastJobDomain
                            + "\n\n" + interactionProfile.toPromptSection();

                    Map<String, Object> context = new HashMap<>();
                    context.put(PARAM_CANDIDATE_PROFILE, candidateProfile);
                    context.put(PARAM_CANDIDATE_SKILLS, candidateSkillsText);
                    context.put(PARAM_CANDIDATE_RESUME_TEXT, candidateResumeText);
                    context.put(PARAM_VACANCIES_JSON, vacanciesJson);
                    context.put("callerSource", "CandidateVacancyMatch:vacancy-to-candidates");
                    context.put("requestId", requestId);

                    phase = "ai-execution";
                    progress.updateStage("AI-анализ: " + candName,
                            "Кандидат " + candidateSequence + " из " + candidates.size() + ": "
                            + candName + (candPos.isEmpty() ? "" : " (" + candPos + ")")
                            + " — сопоставление с требованиями вакансии нейросетью...");
                    try {
                        AiExecutionResult aiResult = aiExecutionService.executeText(FUNCTION_CODE, context);
                        if (aiResult != null && aiResult.getText() != null && !aiResult.getText().trim().isEmpty()) {
                            lastAiResult = aiResult;
                            Set<UUID> seenVacancyIds = new HashSet<>();
                            List<CandidateVacancyMatchItem> parsed = parseAiResponse(aiResult.getText(), singleVacancyList, positionById, seenVacancyIds, report);
                            if (!parsed.isEmpty()) {
                                matchedItem = parsed.get(0);
                                anyAiMatchParsed = true;
                            } else {
                                unusableAiResponses++;
                                log.warn("Vacancy-to-candidates AI response produced no match: operationId={}, requestId={}, vacancyId={}, candidateId={}, phase=ai-parse, errorCategory=no-match-record",
                                        operationId, requestId, openPositionId, cand.getId());
                            }
                        } else {
                            emptyAiResponses++;
                            log.warn("Vacancy-to-candidates AI response is empty: operationId={}, requestId={}, vacancyId={}, candidateId={}, phase=ai-response, errorCategory=empty-response",
                                    operationId, requestId, openPositionId, cand.getId());
                        }
                    } catch (Exception e) {
                        aiCallFailures++;
                        log.warn("Vacancy-to-candidates AI call failed: operationId={}, requestId={}, vacancyId={}, candidateId={}, phase=ai-execution, errorType={}",
                                operationId, requestId, openPositionId, cand.getId(), e.getClass().getSimpleName());
                    }

                    if (matchedItem == null) {
                        phase = "rule-based-fallback";
                        List<CandidateVacancyMatchItem> fallbackList = buildHonestFallbackItems(cand, candidateSkills, candidateResumeText, singleVacancyList);
                        if (!fallbackList.isEmpty()) {
                            matchedItem = fallbackList.get(0);
                            fallbackCandidates++;
                        }
                    }

                    if (matchedItem != null) {
                        phase = "workflow-state";
                        enrichSalaryFit(matchedItem, vacancy, interactionProfile);
                        applyInteractionAnalysisAndWeights(matchedItem, vacancy, interactionProfile);
                        matchedItem.setCandidateId(cand.getId());
                        matchedItem.setCandidateFullName(cand.getFullName());
                        matchedItem.setCandidatePosition(cand.getPersonPosition() != null ? cand.getPersonPosition().getPositionRuName() : "");
                        matchedItem.setCandidateCity(cand.getCityOfResidence() != null ? cand.getCityOfResidence().getCityRuName() : "");
                        matchedItem.setCandidateCurrentCompany(cand.getCurrentCompany() != null ?
                                (cand.getCurrentCompany().getComanyName() != null ? cand.getCurrentCompany().getComanyName() : cand.getCurrentCompany().getCompanyShortName()) : "");

                        if (workflowService != null) {
                            try {
                                IteractionList existing = workflowService.getExistingRelation(cand.getId(), vacancy.getId());
                                if (existing != null) {
                                    matchedItem.setAlreadyInWork(true);
                                    matchedItem.setRecruiterDecision("В работе");
                                }
                            } catch (Exception ex) {
                                log.debug("Workflow relation check failed: {}", ex.getMessage());
                            }
                        }

                        allItems.add(matchedItem);
                    }
                } catch (Exception e) {
                    failedCandidates++;
                    log.warn("Vacancy-to-candidates candidate processing failed: operationId={}, requestId={}, vacancyId={}, candidateSequence={}, candidateId={}, phase={}, errorType={}",
                            operationId, requestId, openPositionId, candidateSequence, cand.getId(), phase, e.getClass().getSimpleName());
                } finally {
                    Integer candScore = matchedItem != null ? matchedItem.getScore() : null;
                    boolean candSuccess = matchedItem != null;
                    progress.completeCandidate(candidateSequence, candName, candScore, candSuccess);
                }
            }

            VacancyCandidateMatchRun run = null;
            if (workflowService != null) {
                try {
                    run = workflowService.createMatchRun(openPositionId, null, FUNCTION_CODE, allItems.size());
                } catch (Exception e) {
                    log.warn("Failed to create vacancy-candidate match run: operationId={}, vacancyId={}, phase=workflow-run, errorType={}",
                            operationId, openPositionId, e.getClass().getSimpleName());
                }
            }

            if (run != null) {
                final UUID runId = run.getId();
                allItems.forEach(it -> it.setMatchRunId(runId));
            }

            allItems.sort((i1, i2) -> {
                int s1 = i1.getScore() != null ? i1.getScore() : 0;
                int s2 = i2.getScore() != null ? i2.getScore() : 0;
                if (s1 != s2) return Integer.compare(s2, s1);
                String n1 = i1.getCandidateFullName() != null ? i1.getCandidateFullName() : "";
                String n2 = i2.getCandidateFullName() != null ? i2.getCandidateFullName() : "";
                return n1.compareToIgnoreCase(n2);
            });

            report.setItems(allItems);
            report.setMatchedVacanciesCount(allItems.size());
            report.setAiExecutionResult(lastAiResult);
            report.setFallbackUsed(fallbackCandidates > 0);
            if (allItems.isEmpty()) {
                report.setSuccess(false);
                report.setStatusMessage(failedCandidates == candidates.size()
                        ? "Не удалось обработать кандидатов. Код обращения: " + operationId
                        : "AI-анализ завершен, рекомендации не сформированы. Код обращения: " + operationId);
            } else if (failedCandidates > 0 || fallbackCandidates > 0) {
                List<String> statusDetails = new ArrayList<>();
                if (fallbackCandidates > 0 && !anyAiMatchParsed) {
                    if (aiCallFailures > 0) {
                        statusDetails.add(String.format(Locale.ROOT,
                                "AI-сервис не выполнил запрос для %d кандидатов", aiCallFailures));
                    }
                    if (emptyAiResponses > 0) {
                        statusDetails.add(String.format(Locale.ROOT,
                                "получен пустой ответ AI для %d кандидатов", emptyAiResponses));
                    }
                    if (unusableAiResponses > 0) {
                        statusDetails.add(String.format(Locale.ROOT,
                                "ответ AI не содержал разбираемого совпадения для %d кандидатов", unusableAiResponses));
                    }
                }
                if (fallbackCandidates > 0) {
                    statusDetails.add(String.format(Locale.ROOT,
                            "для %d из %d кандидатов показана предварительная оценка по сохранённым данным",
                            fallbackCandidates, candidates.size()));
                }
                if (failedCandidates > 0) {
                    statusDetails.add(String.format(Locale.ROOT,
                            "не удалось обработать %d из %d кандидатов",
                            failedCandidates, candidates.size()));
                }
                report.setStatusMessage(String.join("; ", statusDetails) + ". Код обращения: " + operationId);
            }

            progress.updateStage("Финализация", "Ранжирование результатов подбора и формирование рекомендаций...");
            progress.finish(report.isSuccess(), report.getStatusMessage());
            return report;
        } catch (Exception e) {
            log.error("Vacancy-to-candidates matching failed before completion: operationId={}, vacancyId={}, phase=operation, errorType={}",
                    operationId, openPositionId, e.getClass().getSimpleName());
            CandidateVacancyMatchReport failedReport = new CandidateVacancyMatchReport();
            failedReport.setSuccess(false);
            failedReport.setStatusMessage("Не удалось завершить AI-подбор. Код обращения: " + operationId);
            progress.finish(false, failedReport.getStatusMessage());
            return failedReport;
        }
    }

    private void enrichItemsWithCandidateAndStatus(List<CandidateVacancyMatchItem> items, JobCandidate candidate, UUID vacancyId, CandidateInteractionProfile profile) {
        VacancyCandidateMatchRun run = null;
        if (workflowService != null) {
            try {
                run = workflowService.createMatchRun(vacancyId, candidate != null ? candidate.getId() : null, FUNCTION_CODE, items.size());
            } catch (Exception e) {
                log.warn("Failed to create match run: {}", e.getMessage());
            }
        }

        final UUID rId = run != null ? run.getId() : null;
        for (CandidateVacancyMatchItem it : items) {
            if (candidate != null) {
                it.setCandidateId(candidate.getId());
                it.setCandidateFullName(candidate.getFullName());
                it.setCandidatePosition(candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "");
                it.setCandidateCity(candidate.getCityOfResidence() != null ? candidate.getCityOfResidence().getCityRuName() : "");
            }
            if (rId != null) {
                it.setMatchRunId(rId);
            }
            enrichSalaryFit(it, it.getOpenPosition(), profile);
            if (profile != null) {
                applyInteractionAnalysisAndWeights(it, it.getOpenPosition(), profile);
            }
            if (workflowService != null && it.getCandidateId() != null && it.getOpenPositionId() != null) {
                IteractionList existing = workflowService.getExistingRelation(it.getCandidateId(), it.getOpenPositionId());
                if (existing != null) {
                    it.setAlreadyInWork(true);
                    it.setRecruiterDecision("В работе");
                }
            }
        }
    }

    static boolean isOpenVacancy(OpenPosition openPosition) {
        return openPosition != null && !Boolean.TRUE.equals(openPosition.getOpenClose());
    }

    Comparator<CandidateVacancyMatchItem> getComparator() {
        return (i1, i2) -> {
            // 1. score DESC
            int c1 = Integer.compare(i2.getScore() != null ? i2.getScore() : 0, i1.getScore() != null ? i1.getScore() : 0);
            if (c1 != 0) return c1;
            // 2. priority DESC (nulls last)
            int p1 = i1.getPriority() != null ? i1.getPriority() : -1;
            int p2 = i2.getPriority() != null ? i2.getPriority() : -1;
            int c2 = Integer.compare(p2, p1);
            if (c2 != 0) return c2;
            // 3. vacancyName ASC
            String n1 = i1.getVacancyName() != null ? i1.getVacancyName() : "";
            String n2 = i2.getVacancyName() != null ? i2.getVacancyName() : "";
            return n1.compareToIgnoreCase(n2);
        };
    }

    private String buildCandidateProfileString(JobCandidate candidate) {
        StringBuilder sb = new StringBuilder();
        sb.append("ФИО: ").append(candidate.getFullName() != null ? candidate.getFullName() : "Не указано").append("\n");
        if (candidate.getPersonPosition() != null) {
            sb.append("Текущая/целевая должность: ").append(candidate.getPersonPosition().getPositionRuName()).append("\n");
        }
        if (candidate.getCityOfResidence() != null) {
            sb.append("Город проживания: ").append(candidate.getCityOfResidence().getCityRuName()).append("\n");
        }
        if (candidate.getCurrentCompany() != null) {
            String company = candidate.getCurrentCompany().getComanyName() != null ?
                    candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName();
            if (company != null && !company.trim().isEmpty()) {
                sb.append("Текущая компания: ").append(company).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildCandidateSkillsString(List<CandidateSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            return "Навыки в базе данных не зафиксированы.";
        }
        StringBuilder sb = new StringBuilder();
        for (CandidateSkill cs : skills) {
            if (cs.getSkill() != null && cs.getSkill().getSkillName() != null) {
                sb.append("- ").append(cs.getSkill().getSkillName());
                if (cs.getPriority() != null) {
                    if (cs.getPriority() == CandidateSkillPriority.MAIN) sb.append(" (основной)");
                    else if (cs.getPriority() == CandidateSkillPriority.SECONDARY) sb.append(" (второстепенный)");
                    else if (cs.getPriority() == CandidateSkillPriority.TERTIARY) sb.append(" (третьестепенный)");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String extractFileTextSafely(FileDescriptor fd) {
        if (fd == null || fileLoader == null) {
            return "";
        }
        try (InputStream is = fileLoader.openStream(fd)) {
            byte[] bytes = is.readAllBytes();
            if (bytes == null || bytes.length == 0) {
                return "";
            }
            String ext = fd.getExtension() != null ? fd.getExtension().toLowerCase(Locale.ROOT) : "";
            if ("pdf".equals(ext)) {
                try (PDDocument doc = Loader.loadPDF(bytes)) {
                    return new PDFTextStripper().getText(doc);
                }
            } else if ("docx".equals(ext)) {
                try (InputStream bis = new java.io.ByteArrayInputStream(bytes);
                     XWPFDocument doc = new XWPFDocument(bis);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            } else if ("txt".equals(ext)) {
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception | LinkageError e) {
            log.debug("extractFileTextSafely failed for file {}: {}", fd.getId(), e.getMessage());
        }
        return "";
    }

    private String buildCandidateResumeText(List<CandidateCV> cvList) {
        if (cvList == null || cvList.isEmpty()) {
            return "Резюме с распознанным текстом отсутствует. Для анализа используйте только профиль и сохранённые навыки кандидата; не предполагайте неподтверждённый опыт.";
        }
        CandidateCV mainCV = cvList.get(0);
        StringBuilder sb = new StringBuilder();

        // 1. Оригинальный файл резюме, если он есть
        String fileText = "";
        if (mainCV.getOriginalFileCV() != null) {
            fileText = extractFileTextSafely(mainCV.getOriginalFileCV());
        }
        if (fileText.trim().isEmpty() && mainCV.getFileCV() != null) {
            fileText = extractFileTextSafely(mainCV.getFileCV());
        }

        // 2. textCV, если оригинального файла нет
        String bodyText = "";
        if (!fileText.trim().isEmpty()) {
            bodyText = fileText.trim();
        } else if (mainCV.getTextCV() != null && !mainCV.getTextCV().trim().isEmpty()) {
            bodyText = mainCV.getTextCV().trim();
        }

        // 3. Чек-лист кандидата / комментарий
        String checklistText = mainCV.getCommentLetter() != null ? mainCV.getCommentLetter().trim() : "";

        // 4. Сопроводительное письмо из резюме, если оно есть
        String coverLetterText = mainCV.getLetter() != null ? mainCV.getLetter().trim() : "";

        if (bodyText.isEmpty() && checklistText.isEmpty() && coverLetterText.isEmpty()) {
            boolean foundAnyInPast = false;
            for (int i = 1; i < cvList.size(); i++) {
                CandidateCV past = cvList.get(i);
                if (past.getTextCV() != null && !past.getTextCV().trim().isEmpty()) {
                    foundAnyInPast = true;
                    break;
                }
            }
            if (!foundAnyInPast) {
                return "Резюме с распознанным текстом отсутствует. Для анализа используйте только профиль и сохранённые навыки кандидата; не предполагайте неподтверждённый опыт.";
            }
        }

        sb.append("--- ОСНОВНОЕ РЕЗЮМЕ ---\n");
        if (!bodyText.isEmpty()) {
            if (bodyText.length() > MAX_RESUME_TEXT_CHARS) {
                bodyText = bodyText.substring(0, MAX_RESUME_TEXT_CHARS) + "\n...[текст сокращен]";
            }
            sb.append(bodyText).append("\n");
        }

        if (!checklistText.isEmpty()) {
            sb.append("\n--- ЧЕК-ЛИСТ И ЗАМЕТКИ РЕКРУТЕРА ПО КАНДИДАТУ ---\n").append(checklistText).append("\n");
        }

        if (!coverLetterText.isEmpty()) {
            sb.append("\n--- СОПРОВОДИТЕЛЬНОЕ ПИСЬМО ИЗ РЕЗЮМЕ ---\n").append(coverLetterText).append("\n");
        }

        // Если есть более ранние резюме, добавляем сжатую историческую справку
        if (cvList.size() > 1) {
            sb.append("\n--- ДОПОЛНИТЕЛЬНЫЙ ОПЫТ ИЗ ПРЕДЫДУЩИХ РЕЗЮМЕ ---\n");
            int pastCharsAdded = 0;
            for (int i = 1; i < cvList.size() && pastCharsAdded < MAX_ADDITIONAL_SNIPPETS_CHARS; i++) {
                CandidateCV past = cvList.get(i);
                if (past.getTextCV() != null && !past.getTextCV().trim().isEmpty()) {
                    String pastSnippet = past.getTextCV().trim();
                    if (pastSnippet.length() > 2000) {
                        pastSnippet = pastSnippet.substring(0, 2000) + "...";
                    }
                    String block = "[Резюме от " + (past.getDatePost() != null ? past.getDatePost().toString() : "") + "]:\n"
                            + pastSnippet + "\n\n";
                    sb.append(block);
                    pastCharsAdded += block.length();
                }
            }
        }
        return sb.toString();
    }

    private List<Position> resolveVacancyPositions(OpenPosition vacancy, UUID operationId) {
        return resolveVacancyPositions(vacancy, operationId, null);
    }

    private List<Position> resolveVacancyPositions(OpenPosition vacancy, UUID operationId, MatchProgressState progress) {
        Set<String> titles = new LinkedHashSet<>();

        // 1. Попытка извлечь должности из карты поиска searchMap
        String searchMap = vacancy.getSearchMap();
        if (searchMap != null && !searchMap.trim().isEmpty()) {
            if (progress != null) {
                progress.updateStage("Определение должностей", "Анализ карты поиска сорсера и поиск ключевых позиций...");
            }
            extractPositionsFromSearchMap(searchMap, titles);
            if (progress != null && !titles.isEmpty()) {
                progress.updateStage("Определение должностей", "Из карты поиска извлечено должностей: " + titles.size()
                        + " (" + String.join(", ", titles) + "). Сопоставление со справочником Position...");
            }
        }

        // 2. Если карта поиска должности не задает, генерируем список подходящих должностей
        if (titles.isEmpty()) {
            if (progress != null) {
                progress.updateStage("AI-подбор должностей", "Карта поиска не задаёт должности. AI формирует список подходящих должностей...");
            }
            titles.addAll(generatePositionsWithAi(vacancy, operationId));
            if (progress != null && !titles.isEmpty()) {
                progress.updateStage("Определение должностей", "AI определил релевантные должности: "
                        + String.join(", ", titles) + ". Сопоставление со справочником Position...");
            }
        }

        // 3. Сопоставляем каждую должность с точным значением справочника Position
        List<Position> matchedPositions = new ArrayList<>();
        Set<UUID> matchedIds = new HashSet<>();

        for (String title : titles) {
            if (progress != null) {
                progress.updateStage("Сопоставление со справочником", "Поиск в справочнике должностей для: " + title + "...");
            }
            Position pos = matchExactPositionInDictionary(title);
            if (pos != null && matchedIds.add(pos.getId())) {
                matchedPositions.add(pos);
            }
        }

        // 4. Безопасное поведение, если точное сопоставление не найдено: используем должность вакансии
        if (matchedPositions.isEmpty() && vacancy.getPositionType() != null) {
            matchedPositions.add(vacancy.getPositionType());
        }

        if (progress != null) {
            if (!matchedPositions.isEmpty()) {
                String names = matchedPositions.stream().map(Position::getPositionRuName).filter(Objects::nonNull).collect(Collectors.joining(", "));
                progress.updateStage("Поиск кандидатов", "Определены должности для поиска: " + names + ". Выборка кандидатов из базы данных...");
            } else {
                progress.updateStage("Поиск кандидатов", "Целевые должности в справочнике не найдены. Выборка активного пула кандидатов...");
            }
        }

        return matchedPositions;
    }

    private void extractPositionsFromSearchMap(String rawSearchMap, Set<String> titles) {
        if (rawSearchMap == null || rawSearchMap.trim().isEmpty()) {
            return;
        }
        String formatted = rawSearchMap
                .replaceAll("(?i)<(?:br|hr|/p|/div|/li|/h[1-6]|/tr)>", "\n")
                .replaceAll("(?i)<[^>]+>", " ");

        String[] lines = formatted.split("[\\r\\n]+");
        boolean inAltSection = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (inAltSection && (lower.contains("компании-донор")
                    || lower.contains("требовани")
                    || lower.contains("обязанност")
                    || lower.contains("приложени")
                    || lower.contains("стек")
                    || lower.contains("условия"))) {
                inAltSection = false;
            }

            if (lower.contains("альтернативные названия должностей")
                    || lower.contains("альтернативные должности")
                    || lower.contains("вспомогательные должности")) {
                inAltSection = true;
                int colonIdx = trimmed.indexOf(':');
                if (colonIdx >= 0 && colonIdx + 1 < trimmed.length()) {
                    String afterColon = trimmed.substring(colonIdx + 1).trim();
                    for (String item : afterColon.split("[,;•]+")) {
                        String clean = cleanPositionTitle(item);
                        if (!clean.isEmpty() && clean.length() >= 3 && clean.length() <= 60) {
                            titles.add(clean);
                        }
                    }
                }
                continue;
            }

            if (inAltSection) {
                for (String item : trimmed.split("[,;•]+")) {
                    String clean = cleanPositionTitle(item);
                    if (!clean.isEmpty() && clean.length() >= 3 && clean.length() <= 60) {
                        titles.add(clean);
                    }
                }
                continue;
            }

            Pattern p = Pattern.compile("(?i)^(?:.*?(?:Карта поиска для рекрутера|Должность))[:\\s]+([^:;,\\n\\r]{3,60})");
            Matcher m = p.matcher(trimmed);
            if (m.find()) {
                String clean = cleanPositionTitle(m.group(1));
                if (!clean.isEmpty()) {
                    titles.add(clean);
                }
            }
        }
    }

    private String cleanPositionTitle(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        s = s.replaceAll("(?i)\\[|\\]", "");
        s = s.replaceAll("(?i)\\((?:УТОЧНИТЬ|основная|вспомогательная|приоритет)[^)]*\\)", "");
        s = s.replaceAll("^[-*•0-9.\\s]+", "");
        s = s.trim();
        if (s.startsWith(":") || s.startsWith("-")) {
            s = s.substring(1).trim();
        }
        return s;
    }

    private String stripGradeModifiers(String title) {
        if (title == null) return "";
        return title.replaceAll("(?i)\\b(senior|middle|junior|lead|teamlead|techlead|ведущий|главный|старший|младший|руководитель группы)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> generatePositionsWithAi(OpenPosition vacancy, UUID operationId) {
        List<String> result = new ArrayList<>();
        if (vacancy == null) {
            return result;
        }

        if (aiExecutionService != null) {
            try {
                Map<String, Object> ctx = new HashMap<>();
                String vacancyDesc = vacancy.getVacansyName() != null ? vacancy.getVacansyName() : "";
                if (vacancy.getComment() != null) {
                    String cleanComment = Jsoup.parse(vacancy.getComment()).text();
                    if (cleanComment.length() > 500) {
                        cleanComment = cleanComment.substring(0, 500);
                    }
                    vacancyDesc += ". " + cleanComment;
                }
                ctx.put("vacancyText", vacancyDesc);
                ctx.put("requestId", operationId + "-positions-gen");
                ctx.put("callerSource", "CandidateVacancyMatch:positions-gen");

                AiExecutionResult aiRes = aiExecutionService.executeText("VACANCY_SMART_POSITIONS", ctx);
                if (aiRes != null && aiRes.getText() != null && !aiRes.getText().trim().isEmpty()) {
                    String text = aiRes.getText().trim();
                    if (text.startsWith("```")) {
                        text = text.replaceAll("^```(?:json)?", "").replaceAll("```$", "").trim();
                    }
                    JsonNode root = objectMapper.readTree(text);
                    if (root.isArray()) {
                        for (JsonNode n : root) {
                            String t = cleanPositionTitle(n.asText());
                            if (!t.isEmpty()) {
                                result.add(t);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("generatePositionsWithAi call failed: {}", e.getMessage());
            }
        }

        if (result.isEmpty() && vacancy.getVacansyName() != null) {
            String raw = vacancy.getVacansyName().trim();
            String cleaned = cleanPositionTitle(raw);
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
                String stripped = stripGradeModifiers(cleaned);
                if (!stripped.isEmpty() && !stripped.equalsIgnoreCase(cleaned)) {
                    result.add(stripped);
                }
            }
        }
        return result;
    }

    private Position matchExactPositionInDictionary(String rawTitle) {
        if (rawTitle == null || rawTitle.trim().isEmpty() || dataManager == null) {
            return null;
        }
        String clean = cleanPositionTitle(rawTitle);
        if (clean.isEmpty()) {
            return null;
        }
        try {
            // 1. Точное совпадение по positionRuName или positionEnName (регистронезависимо)
            List<Position> list = dataManager.load(Position.class)
                    .query("select p from hunttech_Position p where lower(trim(p.positionRuName)) = :t or lower(trim(p.positionEnName)) = :t")
                    .parameter("t", clean.toLowerCase(Locale.ROOT))
                    .list();
            if (!list.isEmpty()) {
                return list.get(0);
            }

            // 2. Очистка от грейдов (Senior/Middle/Junior/Lead) и повторный поиск точного совпадения
            String stripped = stripGradeModifiers(clean);
            if (!stripped.isEmpty() && !stripped.equalsIgnoreCase(clean)) {
                list = dataManager.load(Position.class)
                        .query("select p from hunttech_Position p where lower(trim(p.positionRuName)) = :s or lower(trim(p.positionEnName)) = :s")
                        .parameter("s", stripped.toLowerCase(Locale.ROOT))
                        .list();
                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }

            // 3. Поиск по началу названия (префиксное совпадение в справочнике)
            String prefix = (stripped.isEmpty() ? clean : stripped).toLowerCase(Locale.ROOT);
            if (prefix.length() >= 4) {
                list = dataManager.load(Position.class)
                        .query("select p from hunttech_Position p where lower(trim(p.positionRuName)) like :p or lower(trim(p.positionEnName)) like :p")
                        .parameter("p", prefix + "%")
                        .list();
                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }
        } catch (Exception e) {
            log.debug("matchExactPositionInDictionary query failed for title '{}': {}", rawTitle, e.getMessage());
        }
        return null;
    }

    static class InteractionDetail {
        Date date;
        String interactionTypeNumber;
        String interactionTypeName;
        String vacancyTitle;
        String comment;
        String recruiterName;
        boolean isInterview;
        boolean isClientInterview;
        Integer rating;
    }

    static class CandidateInteractionProfile {
        UUID candidateId;
        String recruiterActivityText;
        int totalInteractionsCount = 0;
        String candidateSalaryExpectations;
        List<InteractionDetail> employerRejections = new ArrayList<>();
        List<InteractionDetail> candidateRefusals = new ArrayList<>();
        List<InteractionDetail> interviews = new ArrayList<>();
        List<InteractionDetail> otherInteractions = new ArrayList<>();

        String toPromptSection() {
            StringBuilder sb = new StringBuilder();
            sb.append("ИСТОРИЯ ВЗАИМОДЕЙСТВИЙ, СОБЕСЕДОВАНИЙ И ПРИЧИНЫ ПРОШЛЫХ ОТКАЗОВ В СИСТЕМЕ HRM:\n");
            sb.append("• Актуальность контактов: ").append(recruiterActivityText != null ? recruiterActivityText : "Новый кандидат").append("\n");
            sb.append("• Всего зафиксировано взаимодействий в базе: ").append(totalInteractionsCount).append("\n");

            if (!interviews.isEmpty()) {
                sb.append("• ПРОВЕДЕННЫЕ СОБЕСЕДОВАНИЯ И ИХ ИТОГИ (").append(interviews.size()).append("):\n");
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);
                for (InteractionDetail d : interviews) {
                    sb.append("  - [").append(d.date != null ? sdf.format(d.date) : "—")
                      .append(" | ").append(d.isClientInterview ? "На стороне заказчика" : "С рекрутером компании")
                      .append(" | Рекрутер: ").append(d.recruiterName != null ? d.recruiterName : "не указан");
                    if (d.vacancyTitle != null && !d.vacancyTitle.isEmpty()) {
                        sb.append(" | Вакансия: '").append(d.vacancyTitle).append("'");
                    }
                    sb.append("]: ").append(d.interactionTypeName != null ? d.interactionTypeName : "Собеседование");
                    if (d.comment != null && !d.comment.trim().isEmpty()) {
                        sb.append(". Итоги: \"").append(d.comment.trim()).append("\"");
                    }
                    sb.append("\n");
                }
            } else {
                sb.append("• Собеседований в истории HRM ранее не проводилось.\n");
            }

            if (!employerRejections.isEmpty()) {
                sb.append("• ПРОШЛЫЕ ОТКАЗЫ РАБОТОДАТЕЛЕЙ/КЛИЕНТОВ КАНДИДАТУ (").append(employerRejections.size()).append("):\n");
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);
                for (InteractionDetail d : employerRejections) {
                    sb.append("  - [").append(d.date != null ? sdf.format(d.date) : "—");
                    if (d.vacancyTitle != null && !d.vacancyTitle.isEmpty()) {
                        sb.append(", Вакансия: '").append(d.vacancyTitle).append("'");
                    }
                    sb.append("]: ").append(d.interactionTypeName != null ? d.interactionTypeName : "Отказ");
                    if (d.comment != null && !d.comment.trim().isEmpty()) {
                        sb.append(". Комментарий заказчика/рекрутера: \"").append(d.comment.trim()).append("\"");
                    }
                    sb.append("\n");
                }
            } else {
                sb.append("• Отказов кандидату со стороны работодателей/клиентов не зафиксировано.\n");
            }

            if (!candidateRefusals.isEmpty()) {
                sb.append("• ПРИЧИНЫ ОТКАЗОВ САМОГО КАНДИДАТА ОТ ПРОШЛЫХ ОФЕРОВ ИЛИ ПРЕДЛОЖЕНИЙ (").append(candidateRefusals.size()).append("):\n");
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);
                for (InteractionDetail d : candidateRefusals) {
                    sb.append("  - [").append(d.date != null ? sdf.format(d.date) : "—");
                    if (d.vacancyTitle != null && !d.vacancyTitle.isEmpty()) {
                        sb.append(", Вакансия: '").append(d.vacancyTitle).append("'");
                    }
                    sb.append("]: ").append(d.interactionTypeName != null ? d.interactionTypeName : "Отказ кандидата");
                    if (d.comment != null && !d.comment.trim().isEmpty()) {
                        sb.append(". Что не устроило кандидата: \"").append(d.comment.trim()).append("\"");
                    }
                    sb.append("\n");
                }
            } else {
                sb.append("• Отказов самого кандидата от оферов и предложений в истории не зафиксировано.\n");
            }

            if (candidateSalaryExpectations != null && !candidateSalaryExpectations.trim().isEmpty()) {
                sb.append("• Зарплатные ожидания кандидата: ").append(candidateSalaryExpectations.trim()).append("\n");
            }

            sb.append("\nИНСТРУКЦИЯ ДЛЯ AI ПО ИСТОРИИ СОБЕСЕДОВАНИЙ, ОТКАЗОВ И ЗАРПЛАТЕ:\n");
            sb.append("1. Учти результаты прошлых собеседований (с рекрутером или на стороне заказчика): если на прошлых интервью кандидат уже подтвердил ключевые навыки вакансии — отрази это в candidateEvidence и reasonsToOffer; если получил замечания от заказчика — учти в рисках.\n");
            sb.append("2. Проанализируй причины прошлых отказов работодателей кандидату (недостаток стека, грейд, ставка, софты, отказ в офере) и сопоставь с текущей вакансией. Если дефицитный стек требуется — обязательно укажи в рисках/пробелах; если не требуется — отметь снятие риска.\n");
            sb.append("3. Проанализируй, что не устраивало кандидата в прошлых оферах (удаленка против офиса, уровень зарплаты, тестовые задания, легаси). Если условия текущей вакансии закрывают эти боли (например, 100% удаленка) — укажи в reasonsToOffer; если вакансия повторяет нежелательные условия (офис, тестовое) — укажи критический риск отказа кандидата.\n");
            sb.append("4. Сопоставь зарплатные ожидания кандидата с зарплатным предложением заказчика (salaryOffer). Укажи зарплатные ожидания и оценку их соответствия предложению заказчика в итоговом выводе (summary).");

            return sb.toString();
        }
    }

    private CandidateInteractionProfile loadCandidateInteractionProfile(UUID candidateId) {
        CandidateInteractionProfile profile = new CandidateInteractionProfile();
        if (candidateId == null || dataManager == null) {
            profile.recruiterActivityText = "Нет данных о взаимодействиях";
            return profile;
        }
        profile.candidateId = candidateId;

        try {
            List<IteractionList> list = dataManager.load(IteractionList.class)
                    .query("select e from hunttech_IteractionList e where e.candidate.id = :candId order by e.dateIteraction desc, e.createTs desc")
                    .parameter("candId", candidateId)
                    .view(viewBuilder -> viewBuilder.addAll(
                            "numberIteraction", "dateIteraction", "comment", "rating",
                            "iteractionType.number", "iteractionType.iterationName",
                            "iteractionType.signOurInterview", "iteractionType.signOurInterviewAssigned",
                            "iteractionType.signClientInterview",
                            "vacancy.vacansyName", "vacancy.positionType.positionRuName",
                            "vacancy.remoteWork", "vacancy.remoteComment", "vacancy.comment",
                            "vacancy.cityPosition.cityRuName", "vacancy.projectName.projectName",
                            "recrutier.lastName", "recrutier.firstName", "recrutier.name",
                            "recrutierName", "addString"
                    ))
                    .list();

            if (list == null || list.isEmpty()) {
                profile.recruiterActivityText = "Ранее не взаимодействовали (новый кандидат в базе HRM)";
                return profile;
            }

            profile.totalInteractionsCount = list.size();

            Date lastDate = list.get(0).getDateIteraction();
            if (lastDate != null) {
                long diffMillis = System.currentTimeMillis() - lastDate.getTime();
                long days = diffMillis / (1000L * 60 * 60 * 24);
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);
                if (days <= 30) {
                    profile.recruiterActivityText = "В работе / контакт в течение последнего месяца (" + sdf.format(lastDate) + ", " + days + " дн. назад) — наивысшая актуальность";
                } else if (days <= 180) {
                    profile.recruiterActivityText = "Контакт в течение полугода (" + sdf.format(lastDate) + ", " + days + " дн. назад) — высокая актуальность";
                } else if (days <= 365) {
                    profile.recruiterActivityText = "Контакт в течение года (" + sdf.format(lastDate) + ", " + days + " дн. назад) — средняя актуальность";
                } else {
                    long years = days / 365;
                    profile.recruiterActivityText = "Контакт более " + (years >= 2 ? years + " лет" : "года") + " назад (" + sdf.format(lastDate) + ") — низкий приоритет взаимодействия";
                }
            } else {
                profile.recruiterActivityText = "Взаимодействия зафиксированы (дата не указана)";
            }

            for (IteractionList il : list) {
                InteractionDetail detail = new InteractionDetail();
                detail.date = il.getDateIteraction();
                detail.comment = il.getComment();
                detail.rating = il.getRating();
                if (il.getIteractionType() != null) {
                    detail.interactionTypeNumber = il.getIteractionType().getNumber();
                    detail.interactionTypeName = il.getIteractionType().getIterationName();
                }
                if (il.getVacancy() != null) {
                    detail.vacancyTitle = il.getVacancy().getVacansyName();
                }
                detail.recruiterName = extractRecruiterName(il);
                detail.isInterview = classifyIsInterview(il, detail);
                detail.isClientInterview = classifyIsClientInterview(il, detail);

                // Извлечение зарплатных ожиданий из истории взаимодействий (тип "Зарплатные ожидания" или поле addString)
                if (profile.candidateSalaryExpectations == null
                        && il.getAddString() != null && !il.getAddString().trim().isEmpty()
                        && il.getIteractionType() != null && il.getIteractionType().getIterationName() != null
                        && (il.getIteractionType().getIterationName().toLowerCase(Locale.ROOT).contains("зарплат")
                            || il.getIteractionType().getIterationName().toLowerCase(Locale.ROOT).contains("ожидан"))) {
                    profile.candidateSalaryExpectations = il.getAddString().trim();
                }

                if (detail.isInterview) {
                    profile.interviews.add(detail);
                }
                if (classifyIsEmployerRejection(detail)) {
                    profile.employerRejections.add(detail);
                } else if (classifyIsCandidateRefusal(detail)) {
                    profile.candidateRefusals.add(detail);
                } else if (!detail.isInterview) {
                    profile.otherInteractions.add(detail);
                }
            }
        } catch (Exception e) {
            log.warn("loadCandidateInteractionProfile failed for candidate {}: {}", candidateId, e.getMessage());
            profile.recruiterActivityText = "Информация о взаимодействиях не определена";
        }

        return profile;
    }

    private String extractRecruiterName(IteractionList il) {
        if (il == null) return null;
        if (il.getRecrutier() != null) {
            String lastName = il.getRecrutier().getLastName();
            String firstName = il.getRecrutier().getFirstName();
            if (lastName != null && !lastName.trim().isEmpty()) {
                if (firstName != null && !firstName.trim().isEmpty()) {
                    return lastName.trim() + " " + firstName.trim().charAt(0) + ".";
                }
                return lastName.trim();
            }
            if (il.getRecrutier().getName() != null && !il.getRecrutier().getName().trim().isEmpty()) {
                return il.getRecrutier().getName().trim();
            }
        }
        if (il.getRecrutierName() != null && !il.getRecrutierName().trim().isEmpty()) {
            return il.getRecrutierName().trim();
        }
        return null;
    }

    private boolean classifyIsInterview(IteractionList il, InteractionDetail d) {
        if (il.getIteractionType() != null) {
            Boolean signClient = il.getIteractionType().getSignClientInterview();
            Boolean signOur = il.getIteractionType().getSignOurInterview();
            Boolean signOurAssigned = il.getIteractionType().getSignOurInterviewAssigned();
            if (Boolean.TRUE.equals(signClient) || Boolean.TRUE.equals(signOur) || Boolean.TRUE.equals(signOurAssigned)) {
                return true;
            }
        }
        String num = d.interactionTypeNumber != null ? d.interactionTypeNumber.trim() : "";
        if (num.startsWith("3.") || num.startsWith("4.")) {
            return true;
        }
        String name = d.interactionTypeName != null ? d.interactionTypeName.toLowerCase(Locale.ROOT) : "";
        if (name.contains("собеседован") || name.contains("интервью")) {
            return true;
        }
        String comment = d.comment != null ? d.comment.toLowerCase(Locale.ROOT) : "";
        if (comment.contains("провели собеседование") || comment.contains("проведено интервью")
                || comment.contains("интервью с рекрутером") || comment.contains("собеседование у заказчика")) {
            return true;
        }
        return false;
    }

    private boolean classifyIsClientInterview(IteractionList il, InteractionDetail d) {
        if (il.getIteractionType() != null) {
            Boolean signClient = il.getIteractionType().getSignClientInterview();
            if (Boolean.TRUE.equals(signClient)) {
                return true;
            }
        }
        String num = d.interactionTypeNumber != null ? d.interactionTypeNumber.trim() : "";
        if (num.startsWith("4.")) {
            return true;
        }
        String name = d.interactionTypeName != null ? d.interactionTypeName.toLowerCase(Locale.ROOT) : "";
        if (name.contains("заказчик") && (name.contains("собеседован") || name.contains("интервью") || name.contains("встреча"))) {
            return true;
        }
        String comment = d.comment != null ? d.comment.toLowerCase(Locale.ROOT) : "";
        if (comment.contains("у заказчика") || comment.contains("с заказчиком") || comment.contains("со стороны клиента")) {
            return true;
        }
        return false;
    }

    private boolean classifyIsEmployerRejection(InteractionDetail d) {
        String num = d.interactionTypeNumber != null ? d.interactionTypeNumber.trim() : "";
        String name = d.interactionTypeName != null ? d.interactionTypeName.toLowerCase(Locale.ROOT) : "";
        String comment = d.comment != null ? d.comment.toLowerCase(Locale.ROOT) : "";

        if (num.startsWith("8.05") || num.startsWith("8.06") || num.startsWith("8.07")
                || num.startsWith("8.08") || num.startsWith("8.09") || num.startsWith("8.10")
                || num.startsWith("8.11") || num.startsWith("8.12") || num.startsWith("8.13")
                || num.startsWith("8.14") || num.startsWith("8.19") || num.startsWith("8.24")
                || num.startsWith("8.25") || "007".equals(num) || "2".equals(num) || "5".equals(num)) {
            return true;
        }

        if (name.contains("отказано в офере") || name.contains("отказано на стороне")
                || name.contains("отказано по резюме") || name.contains("отказ на стороне")
                || name.contains("недостаточно компетенций") || name.contains("завысил свой грейд")
                || name.contains("завышены зарплатные") || name.contains("не тот профиль")
                || name.contains("не вписывается в ставку") || name.contains("отказано по софтам")
                || name.contains("отказано рекрутером") || name.contains("не прошел испытательный")
                || name.contains("отказать кандидаты") || name.contains("не пришел на собеседование")
                || name.contains("отказ заказчика")) {
            return true;
        }

        if (comment.contains("заказчик отказал") || comment.contains("отказ заказчика")
                || comment.contains("отказали кандидату") || comment.contains("не подошел заказчику")
                || comment.contains("отказано заказчиком")) {
            return true;
        }

        return false;
    }

    private boolean classifyIsCandidateRefusal(InteractionDetail d) {
        String num = d.interactionTypeNumber != null ? d.interactionTypeNumber.trim() : "";
        String name = d.interactionTypeName != null ? d.interactionTypeName.toLowerCase(Locale.ROOT) : "";
        String comment = d.comment != null ? d.comment.toLowerCase(Locale.ROOT) : "";

        if (num.startsWith("8.01") || num.startsWith("8.02") || num.startsWith("8.03")
                || num.startsWith("8.04") || num.startsWith("8.17") || num.startsWith("8.23")
                || num.startsWith("3.6")) {
            return true;
        }

        if (name.contains("отказался от офера") || name.contains("отказался: не заинтересовала")
                || name.contains("отказался от собеседования") || name.contains("отказался делать тестовое")
                || name.contains("отказался выполнять тестовое") || name.contains("отказался: нашел работу")
                || name.contains("отказался: получил контроффер") || name.contains("отмена собеседования кандидатом")) {
            return true;
        }

        if (comment.contains("кандидат отказался") || comment.contains("отказался от оффера")
                || comment.contains("отказался от офера") || comment.contains("отказался от предложения")
                || comment.contains("не устроила зарплата") || comment.contains("не устроил офер")
                || comment.contains("не хочет в офис") || comment.contains("отклонил офер")
                || comment.contains("отклонил предложение")) {
            return true;
        }

        return false;
    }

    private void applyInteractionAnalysisAndWeights(CandidateVacancyMatchItem item,
                                                   OpenPosition vacancy,
                                                   CandidateInteractionProfile profile) {
        if (item == null) return;
        if (profile == null) {
            item.setInteractionWeightAdjustment(0);
            item.setInteractionHistoryAnalysis("История взаимодействий: данных нет (новый кандидат). Весовой коэффициент: 0%.");
            return;
        }

        int totalWeightAdjustment = 0;
        int prefAdjustment = 0;
        int skillAdjustment = 0;

        List<String> employerRejectionReport = new ArrayList<>();
        List<String> candidateRefusalReport = new ArrayList<>();
        List<String> riskAdditions = new ArrayList<>();
        List<String> offerReasonAdditions = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);

        // 1. Анализ причин отказов самого кандидата от прошлых оферов и предложений
        for (InteractionDetail d : profile.candidateRefusals) {
            String dateStr = d.date != null ? sdf.format(d.date) : "В архиве";
            String vacStr = d.vacancyTitle != null && !d.vacancyTitle.isEmpty() ? " ('" + d.vacancyTitle + "')" : "";
            String fullText = ((d.interactionTypeName != null ? d.interactionTypeName : "") + " " + (d.comment != null ? d.comment : "")).toLowerCase(Locale.ROOT);

            // A) Удаленка против офиса
            boolean mentionsRemoteOffice = fullText.contains("удален") || fullText.contains("remote")
                    || fullText.contains("офис") || fullText.contains("гибрид") || fullText.contains("присутстви")
                    || fullText.contains("релокац") || fullText.contains("переезд");

            // B) Зарплатные ожидания
            boolean mentionsSalary = fullText.contains("зарплат") || fullText.contains("з/п") || fullText.contains("ставка")
                    || fullText.contains("мало") || fullText.contains("деньг") || fullText.contains("доход") || fullText.contains("бюджет");

            // C) Контроффер
            boolean mentionsCounter = fullText.contains("контроффер") || fullText.contains("контр-офер") || fullText.contains("остался в своей компании");

            // D) Тестовое задание
            boolean mentionsTestTask = fullText.contains("тестов") || fullText.contains("тестовое задание") || fullText.contains("тестового задания");

            if (mentionsRemoteOffice) {
                // OpenPosition.remoteWork: 1 = Remote, 0 = Office, 2 = Hybrid
                boolean vacancyIsRemote = vacancy != null && Integer.valueOf(1).equals(vacancy.getRemoteWork());
                boolean vacancyIsOffice = vacancy != null && Integer.valueOf(0).equals(vacancy.getRemoteWork());

                if (vacancyIsRemote) {
                    candidateRefusalReport.add(String.format(Locale.ROOT,
                            "[%s%s] Отказ от офера/предложения из-за очного формата работы. В ТЕКУЩЕЙ ВАКАНСИИ: предусмотрена 100%% удалённая работа (устраняет прошлый блокер кандидата, весовой бонус +10%% к предпочтениям).",
                            dateStr, vacStr));
                    prefAdjustment += 10;
                    totalWeightAdjustment += 10;
                    offerReasonAdditions.add("Устранение ключевого блокера: вакансия предлагает 100% удаленный формат работы, из-за отсутствия которого кандидат ранее отказался от оффера.");
                } else if (vacancyIsOffice) {
                    candidateRefusalReport.add(String.format(Locale.ROOT,
                            "[%s%s] ВНИМАНИЕ: Кандидат ранее отказался от офера/предложения из-за очного формата в офисе. ТЕКУЩАЯ ВАКАНСИЯ: требует работы в офисе/гибрида (критический риск повторного отказа кандидата, штраф -15%% к рейтингу).",
                            dateStr, vacStr));
                    prefAdjustment -= 15;
                    totalWeightAdjustment -= 15;
                    riskAdditions.add("Критический риск отказа кандидата: ранее кандидат отклонял офер из-за очного формата в офисе, а данная вакансия не является полностью удаленной.");
                } else {
                    candidateRefusalReport.add(String.format(Locale.ROOT,
                            "[%s%s] Отказ от офера из-за формата работы (офис/удаленка). Формат текущей вакансии: %s.",
                            dateStr, vacStr, vacancy != null && vacancy.getRemoteComment() != null ? vacancy.getRemoteComment() : "уточняется"));
                }
            } else if (mentionsSalary) {
                candidateRefusalReport.add(String.format(Locale.ROOT,
                        "[%s%s] Отказ кандидата по зарплатным ожиданиям (%s). Рекомендуется согласовать финансовые условия до финала (штраф -5%% к рейтингу).",
                        dateStr, vacStr, d.comment != null && !d.comment.isEmpty() ? d.comment.trim() : "уровень офера"));
                totalWeightAdjustment -= 5;
            } else if (mentionsCounter) {
                candidateRefusalReport.add(String.format(Locale.ROOT,
                        "[%s%s] Кандидат ранее принял контроффер в текущей компании (%s). Риск контр-предложения при найме (штраф -5%% к рейтингу).",
                        dateStr, vacStr, d.comment != null && !d.comment.isEmpty() ? d.comment.trim() : "удержание работодателем"));
                totalWeightAdjustment -= 5;
                riskAdditions.add("Риск контроффера: кандидат склонен принимать встречные предложения текущего работодателя.");
            } else if (mentionsTestTask) {
                candidateRefusalReport.add(String.format(Locale.ROOT,
                        "[%s%s] Кандидат отказывается выполнять объемные тестовые задания. Рекомендуется оценивать опыт на техническом интервью.",
                        dateStr, vacStr));
            } else {
                candidateRefusalReport.add(String.format(Locale.ROOT,
                        "[%s%s] %s%s",
                        dateStr, vacStr,
                        d.interactionTypeName != null ? d.interactionTypeName : "Отказ кандидата",
                        d.comment != null && !d.comment.isEmpty() ? ": " + d.comment.trim() : ""));
            }
        }

        // 2. Анализ прошлых отказов работодателей кандидату
        for (InteractionDetail d : profile.employerRejections) {
            String dateStr = d.date != null ? sdf.format(d.date) : "В архиве";
            String vacStr = d.vacancyTitle != null && !d.vacancyTitle.isEmpty() ? " ('" + d.vacancyTitle + "')" : "";
            String fullText = ((d.interactionTypeName != null ? d.interactionTypeName : "") + " " + (d.comment != null ? d.comment : "")).toLowerCase(Locale.ROOT);

            // A) Отказ в офере работодателем
            boolean isOfferRejection = fullText.contains("отказано в офере") || (d.interactionTypeNumber != null && d.interactionTypeNumber.startsWith("8.05"));

            // B) Недостаточно компетенций / стек технологий
            boolean isCompetencyRejection = fullText.contains("недостаточно компетенций") || fullText.contains("не тот профиль")
                    || fullText.contains("отказано по резюме") || (d.interactionTypeNumber != null && (d.interactionTypeNumber.startsWith("8.24") || d.interactionTypeNumber.startsWith("8.25") || d.interactionTypeNumber.startsWith("8.14")));

            // C) Завысил свой грейд
            boolean isGradeRejection = fullText.contains("завысил свой грейд") || fullText.contains("грейд")
                    || (d.interactionTypeNumber != null && (d.interactionTypeNumber.startsWith("8.06") || d.interactionTypeNumber.startsWith("8.09")));

            // D) Завышены зарплатные ожидания со стороны работодателя
            boolean isSalaryRejection = fullText.contains("завышены зарплатные") || fullText.contains("не вписывается в ставку")
                    || (d.interactionTypeNumber != null && (d.interactionTypeNumber.startsWith("8.07") || d.interactionTypeNumber.startsWith("8.10") || d.interactionTypeNumber.startsWith("8.13")));

            // E) Софт-скиллы
            boolean isSoftSkillsRejection = fullText.contains("софт") || fullText.contains("собеседован") || fullText.contains("коммуникац");

            if (isOfferRejection) {
                employerRejectionReport.add(String.format(Locale.ROOT,
                        "[%s%s] ВНИМАНИЕ: Работодатель отказал кандидату в офере на финальном этапе (%s). Требуется уточнить причину отзыва офера заказчиком (штраф -10%% к рейтингу).",
                        dateStr, vacStr, d.comment != null && !d.comment.isEmpty() ? d.comment.trim() : "финальный этап"));
                totalWeightAdjustment -= 10;
                riskAdditions.add("Ранее кандидату было отказано в офере работодателем: " + (d.comment != null && !d.comment.isEmpty() ? d.comment.trim() : "уточнить причины отказа"));
            } else if (isCompetencyRejection) {
                boolean deficitMentionedInVacancy = isDeficitStackMentionedInVacancy(d.comment, vacancy);
                if (deficitMentionedInVacancy) {
                    employerRejectionReport.add(String.format(Locale.ROOT,
                            "[%s%s] Отказ заказчика по компетенциям/стеку: '%s'. ВНИМАНИЕ: указанный стек требуется в текущей вакансии (прямой риск повторного несоответствия, штраф -10%% к навыкам).",
                            dateStr, vacStr, d.comment != null && !d.comment.isEmpty() ? d.comment.trim() : d.interactionTypeName));
                    skillAdjustment -= 10;
                    totalWeightAdjustment -= 10;
                    riskAdditions.add("Прошлый отказ заказчика связан с дефицитом компетенций, которые требуются в данной вакансии: " + (d.comment != null ? d.comment.trim() : "стек вакансии"));
                } else {
                    employerRejectionReport.add(String.format(Locale.ROOT,
                            "[%s%s] Отказ заказчика по стеку/профилю: '%s'. СМЯГЧАЮЩИЙ ФАКТОР: в текущей вакансии данный стек не требуется (риск нейтрализован, бонус +3%%).",
                            dateStr, vacStr, d.comment != null && !d.comment.isEmpty() ? d.comment.trim() : d.interactionTypeName));
                    totalWeightAdjustment += 3;
                }
            } else if (isGradeRejection) {
                employerRejectionReport.add(String.format(Locale.ROOT,
                        "[%s%s] Отказ заказчика: завышен заявленный грейд (%s). Требуется точная калибровка грейда кандидата (штраф -5%% к рейтингу).",
                        dateStr, vacStr, d.comment != null && !d.comment.isEmpty() ? d.comment.trim() : "middle vs senior"));
                totalWeightAdjustment -= 5;
            } else if (isSalaryRejection) {
                employerRejectionReport.add(String.format(Locale.ROOT,
                        "[%s%s] Отказ работодателя: кандидат не вписался в бюджетную ставку вакансии (%s).",
                        dateStr, vacStr, d.comment != null && !d.comment.isEmpty() ? d.comment.trim() : "вилка заказчика"));
                totalWeightAdjustment -= 5;
            } else if (isSoftSkillsRejection) {
                employerRejectionReport.add(String.format(Locale.ROOT,
                        "[%s%s] Отказ заказчика по soft skills / интервью (%s). Рекомендуется обратить внимание на коммуникацию кандидата.",
                        dateStr, vacStr, d.comment != null && !d.comment.isEmpty() ? d.comment.trim() : "обратная связь"));
                totalWeightAdjustment -= 5;
            } else {
                employerRejectionReport.add(String.format(Locale.ROOT,
                        "[%s%s] %s%s",
                        dateStr, vacStr,
                        d.interactionTypeName != null ? d.interactionTypeName : "Отказ работодателя",
                        d.comment != null && !d.comment.isEmpty() ? ": " + d.comment.trim() : ""));
            }
        }

        // Ограничение диапазонов корректировок
        totalWeightAdjustment = clamp(totalWeightAdjustment, -25, 15);
        prefAdjustment = clamp(prefAdjustment, -15, 10);
        skillAdjustment = clamp(skillAdjustment, -15, 0);

        List<String> interviewReport = new ArrayList<>();
        for (InteractionDetail d : profile.interviews) {
            String dateStr = d.date != null ? sdf.format(d.date) : "Дата не указана";
            String partyStr = d.isClientInterview ? "На стороне заказчика" : "С рекрутером компании";
            String recStr = d.recruiterName != null && !d.recruiterName.isEmpty() ? d.recruiterName : "Не указан";
            String vacStr = d.vacancyTitle != null && !d.vacancyTitle.isEmpty() ? d.vacancyTitle : "Не указана";
            String resultStr = d.interactionTypeName != null && !d.interactionTypeName.isEmpty() ? d.interactionTypeName : "Собеседование проведено";
            if (d.comment != null && !d.comment.trim().isEmpty()) {
                resultStr += ". " + d.comment.trim();
            }
            if (d.rating != null && d.rating >= 0) {
                resultStr += " (Оценка: " + (d.rating + 1) + "/5)";
            }

            String reportItem = String.format(Locale.ROOT,
                    "[%s] Сторона: %s | Рекрутер: %s | Вакансия: %s | Итоги: %s",
                    dateStr, partyStr, recStr, vacStr, resultStr);
            interviewReport.add(reportItem);
        }

        item.setInteractionWeightAdjustment(totalWeightAdjustment);
        item.setPastRejectionsEmployerSide(employerRejectionReport);
        item.setPastRejectionsCandidateSide(candidateRefusalReport);
        item.setPastInterviews(interviewReport);

        // Формирование итогового аналитического резюме
        StringBuilder summary = new StringBuilder();
        if (profile.totalInteractionsCount == 0) {
            summary.append("История взаимодействий: новый кандидат в базе HRM. Прошлых собеседований и отказов не зафиксировано. Весовой коэффициент нейтральный (0%).");
        } else {
            summary.append(String.format(Locale.ROOT,
                    "Умный анализ истории взаимодействия (всего контактов: %d, собеседований: %d, отказов работодателей: %d, отказов кандидата: %d):\n",
                    profile.totalInteractionsCount, profile.interviews.size(), profile.employerRejections.size(), profile.candidateRefusals.size()));

            if (totalWeightAdjustment > 0) {
                summary.append(String.format(Locale.ROOT,
                        "• Сопоставление с вакансией благоприятное: условия вакансии устраняют прошлые блокеры кандидата. Применён весовой бонус: +%d%% к итоговому AI Score.\n",
                        totalWeightAdjustment));
            } else if (totalWeightAdjustment < 0) {
                summary.append(String.format(Locale.ROOT,
                        "• Сопоставление выявило существенные риски: пересечение с прошлыми причинами отказов (%s). Применён штрафной весовой коэффициент: %d%% к итоговому AI Score.\n",
                        totalWeightAdjustment < -10 ? "критические факторы" : "зоны внимания", totalWeightAdjustment));
            } else {
                summary.append("• Сопоставление с вакансией нейтральное: прямых противоречий с историей прошлых отказов не выявлено (корректировка 0%).\n");
            }

            if (!profile.interviews.isEmpty()) {
                long clientCount = profile.interviews.stream().filter(i -> i.isClientInterview).count();
                long recruiterCount = profile.interviews.size() - clientCount;
                summary.append(String.format(Locale.ROOT,
                        "• Собеседования в истории: проведено %d (на стороне заказчика: %d, с рекрутером компании: %d).\n",
                        profile.interviews.size(), clientCount, recruiterCount));
            }

            summary.append("• Актуальность взаимодействия: ").append(profile.recruiterActivityText != null ? profile.recruiterActivityText : "—");
        }
        item.setInteractionHistoryAnalysis(summary.toString());

        // Корректировка subscores и score
        int originalScore = item.getScore() != null ? item.getScore() : 0;
        int adjustedScore = clamp(originalScore + totalWeightAdjustment, 0, 100);
        item.setScore(adjustedScore);

        int originalPref = item.getPreferencesFit() != null ? item.getPreferencesFit() : 0;
        item.setPreferencesFit(clamp(originalPref + prefAdjustment, 0, 10));

        int originalSkill = item.getSkillsFit() != null ? item.getSkillsFit() : 0;
        item.setSkillsFit(clamp(originalSkill + skillAdjustment, 0, 35));

        item.setVerdict(resolveVerdict(adjustedScore));

        // Дополнение рисков и причин предложить
        if (!riskAdditions.isEmpty()) {
            List<String> currentRisks = new ArrayList<>(item.getRisks() != null ? item.getRisks() : Collections.emptyList());
            for (String r : riskAdditions) {
                if (!currentRisks.contains(r)) currentRisks.add(r);
            }
            item.setRisks(currentRisks);
        }

        if (!offerReasonAdditions.isEmpty()) {
            List<String> currentReasons = new ArrayList<>(item.getReasonsToOffer() != null ? item.getReasonsToOffer() : Collections.emptyList());
            for (String reason : offerReasonAdditions) {
                if (!currentReasons.contains(reason)) currentReasons.add(reason);
            }
            item.setReasonsToOffer(currentReasons);
        }
    }

    private boolean isDeficitStackMentionedInVacancy(String rejectionComment, OpenPosition vacancy) {
        if (rejectionComment == null || rejectionComment.trim().isEmpty() || vacancy == null) {
            return false;
        }

        // 1. Check if any structured skill of the vacancy appears in the rejection comment
        if (vacancy.getSkillsList() != null) {
            String lowerComment = rejectionComment.toLowerCase(Locale.ROOT);
            for (SkillTree st : vacancy.getSkillsList()) {
                if (st.getSkillName() != null && st.getSkillName().trim().length() >= 2) {
                    String sName = st.getSkillName().toLowerCase(Locale.ROOT).trim();
                    if (lowerComment.contains(sName)) {
                        return true;
                    }
                }
            }
        }

        // 2. Check tech tokens against vacancy text
        String vacText = ((vacancy.getVacansyName() != null ? vacancy.getVacansyName() : "") + " "
                + (vacancy.getComment() != null ? vacancy.getComment() : "") + " "
                + (vacancy.getShortDescription() != null ? vacancy.getShortDescription() : "")).toLowerCase(Locale.ROOT);

        String[] tokens = rejectionComment.split("[\\s,;:.()\"'/\\[\\]]+");
        for (String t : tokens) {
            String token = t.toLowerCase(Locale.ROOT).trim();
            if (token.length() >= 3 && isTechKeyword(token)) {
                if (vacText.contains(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTechKeyword(String word) {
        return word.matches("^[a-z0-9#+.]+$")
                || Arrays.asList("kafka", "docker", "kubernetes", "k8s", "spring", "react", "angular", "vue",
                "postgres", "postgresql", "oracle", "redis", "rabbitmq", "ci/cd", "devops", "qa", "sql", "nosql",
                "linux", "python", "golang", "java", "kotlin", "scala", "c#", ".net", "php", "typescript", "javascript",
                "английский", "микросервис", "highload", "архитектур", "тестирован", "безопасност").contains(word);
    }

    private String resolveRecruiterActivityDescription(UUID candidateId) {
        if (candidateId == null || dataManager == null) {
            return "Нет данных о взаимодействиях (новый кандидат)";
        }
        try {
            List<Date> dates = dataManager.loadValue(
                    "select max(e.dateIteraction) from hunttech_IteractionList e where e.candidate.id = :candId", Date.class)
                    .parameter("candId", candidateId)
                    .list();
            Date lastDate = (dates != null && !dates.isEmpty()) ? dates.get(0) : null;
            if (lastDate == null) {
                return "Ранее не взаимодействовали (новый кандидат в базе HRM)";
            }
            long diffMillis = System.currentTimeMillis() - lastDate.getTime();
            long days = diffMillis / (1000L * 60 * 60 * 24);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);
            if (days <= 30) {
                return "В работе / контакт в течение последнего месяца (" + sdf.format(lastDate) + ", " + days + " дн. назад) — наивысшая актуальность";
            } else if (days <= 180) {
                return "Контакт в течение полугода (" + sdf.format(lastDate) + ", " + days + " дн. назад) — высокая актуальность";
            } else if (days <= 365) {
                return "Контакт в течение года (" + sdf.format(lastDate) + ", " + days + " дн. назад) — средняя актуальность";
            } else {
                long years = days / 365;
                return "Контакт более " + (years >= 2 ? years + " лет" : "года") + " назад (" + sdf.format(lastDate) + ") — низкий приоритет взаимодействия";
            }
        } catch (Exception e) {
            log.debug("resolveRecruiterActivityDescription failed for candidate {}: {}", candidateId, e.getMessage());
            return "Информация о взаимодействиях не определена";
        }
    }

    private String resolveLastJobDomainDescription(JobCandidate candidate) {
        if (candidate == null) {
            return "Не указана";
        }
        StringBuilder sb = new StringBuilder();
        if (dataManager != null) {
            try {
                List<JobHistory> history = dataManager.load(JobHistory.class)
                        .query("select e from hunttech_JobHistory e where e.candidate.id = :candId order by e.endDate desc, e.startDate desc, e.createTs desc")
                        .parameter("candId", candidate.getId())
                        .view(viewBuilder -> viewBuilder.addAll("currentCompany.comanyName", "currentCompany.companyShortName",
                                "currentPosition.positionRuName", "rawCompanyName", "rawPositionName", "duties"))
                        .maxResults(1)
                        .list();
                if (!history.isEmpty()) {
                    JobHistory jh = history.get(0);
                    String comp = jh.getCurrentCompany() != null ?
                            (jh.getCurrentCompany().getComanyName() != null ? jh.getCurrentCompany().getComanyName() : jh.getCurrentCompany().getCompanyShortName()) :
                            jh.getRawCompanyName();
                    String pos = jh.getCurrentPosition() != null ? jh.getCurrentPosition().getPositionRuName() : jh.getRawPositionName();

                    if (comp != null && !comp.isEmpty()) {
                        sb.append("Компания: ").append(comp);
                    }
                    if (pos != null && !pos.isEmpty()) {
                        sb.append(", Должность: ").append(pos);
                    }
                    if (jh.getDuties() != null && !jh.getDuties().trim().isEmpty()) {
                        String duties = jh.getDuties().trim();
                        if (duties.length() > 300) duties = duties.substring(0, 300) + "...";
                        sb.append(". Обязанности: ").append(duties);
                    }
                }
            } catch (Exception e) {
                log.debug("resolveLastJobDomainDescription query failed for candidate {}: {}", candidate.getId(), e.getMessage());
            }
        }

        if (sb.length() == 0) {
            if (candidate.getCurrentCompany() != null) {
                String cName = candidate.getCurrentCompany().getComanyName() != null ?
                        candidate.getCurrentCompany().getComanyName() : candidate.getCurrentCompany().getCompanyShortName();
                sb.append("Текущая компания: ").append(cName);
            }
            if (candidate.getSpecialisation() != null && candidate.getSpecialisation().getSpecRuName() != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Специализация: ").append(candidate.getSpecialisation().getSpecRuName());
            }
        }

        return sb.length() > 0 ? sb.toString() : "Предметная область последнего места работы не зафиксирована";
    }

    private List<List<OpenPosition>> splitIntoChunks(List<OpenPosition> openPositions) {
        List<List<OpenPosition>> chunks = new ArrayList<>();
        List<OpenPosition> currentChunk = new ArrayList<>();
        int currentChars = 0;

        for (OpenPosition op : openPositions) {
            String commentText = op.getComment() != null ? Jsoup.parse(op.getComment()).text() : "";
            int commentLength = Math.min(commentText.length(), 3000);
            int approxLength = commentLength + 1000; // комментарий + реквизиты, проект, навыки и JSON-структура

            if (!currentChunk.isEmpty() && (currentChunk.size() >= MAX_VACANCIES_PER_CHUNK || currentChars + approxLength > MAX_VACANCY_TEXT_CHARS_PER_CHUNK)) {
                chunks.add(currentChunk);
                currentChunk = new ArrayList<>();
                currentChars = 0;
            }

            currentChunk.add(op);
            currentChars += approxLength;
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }
        return chunks;
    }

    private String buildVacanciesJson(List<OpenPosition> chunk) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OpenPosition op : chunk) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", op.getId().toString());
            v.put("vacancyName", op.getVacansyName());
            v.put("projectName", op.getProjectName() != null ? op.getProjectName().getProjectName() : null);
            v.put("positionType", op.getPositionType() != null ? op.getPositionType().getPositionRuName() : null);
            v.put("workExperienceYears", op.getWorkExperience());
            v.put("grade", op.getGrade() != null ? op.getGrade().getId() : null);
            v.put("priority", op.getPriority());

            if (op.getSkillsList() != null && !op.getSkillsList().isEmpty()) {
                List<String> skills = op.getSkillsList().stream()
                        .map(SkillTree::getSkillName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                v.put("requiredSkills", skills);
            }

            if (op.getCityPosition() != null) {
                v.put("city", op.getCityPosition().getCityRuName());
            }
            if (op.getRemoteWork() != null) {
                v.put("remoteWork", op.getRemoteWork());
            }
            if (op.getRemoteComment() != null) {
                v.put("remoteComment", op.getRemoteComment());
            }

            String plainComment = op.getComment() != null ? Jsoup.parse(op.getComment()).text() : "";
            if (plainComment.length() > 3000) {
                plainComment = plainComment.substring(0, 3000) + "...";
            }
            v.put("comment", plainComment);

            if (op.getSalaryMin() != null || op.getSalaryMax() != null || op.getSalaryIE() != null
                    || op.getSalaryFixLimit() != null || op.getSalaryComment() != null || Boolean.TRUE.equals(op.getSalaryCandidateRequest())) {
                Map<String, Object> salaryOffer = new LinkedHashMap<>();
                if (op.getSalaryMin() != null) salaryOffer.put("salaryMin", op.getSalaryMin());
                if (op.getSalaryMax() != null) salaryOffer.put("salaryMax", op.getSalaryMax());
                if (op.getSalaryIE() != null) salaryOffer.put("salaryIE", op.getSalaryIE());
                if (op.getSalaryFixLimit() != null) salaryOffer.put("salaryFixLimit", op.getSalaryFixLimit());
                if (op.getSalaryComment() != null) salaryOffer.put("salaryComment", op.getSalaryComment());
                salaryOffer.put("formattedOffer", formatVacancySalaryOffer(op));
                v.put("salaryOffer", salaryOffer);
            }

            list.add(v);
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
        } catch (Exception e) {
            log.error("Failed to serialize vacancies to JSON", e);
            return "[]";
        }
    }

    public static class SalaryEvaluation {
        public final String status;
        public final String analysisText;
        public SalaryEvaluation(String status, String analysisText) {
            this.status = status;
            this.analysisText = analysisText;
        }
    }

    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(() -> {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        return new DecimalFormat("#,###", symbols);
    });

    private static DecimalFormat getDecimalFormat() {
        return DECIMAL_FORMAT.get();
    }

    public static String formatVacancySalaryOffer(OpenPosition op) {
        if (op == null) return null;
        DecimalFormat df = getDecimalFormat();
        StringBuilder sb = new StringBuilder();
        if (op.getSalaryMin() != null && op.getSalaryMax() != null) {
            sb.append("от ").append(df.format(op.getSalaryMin())).append(" до ").append(df.format(op.getSalaryMax())).append(" ₽");
        } else if (op.getSalaryMin() != null) {
            sb.append("от ").append(df.format(op.getSalaryMin())).append(" ₽");
        } else if (op.getSalaryMax() != null) {
            sb.append("до ").append(df.format(op.getSalaryMax())).append(" ₽");
        }
        if (op.getSalaryIE() != null) {
            if (sb.length() > 0) sb.append(" (ИП: ").append(df.format(op.getSalaryIE())).append(" ₽)");
            else sb.append("ИП: ").append(df.format(op.getSalaryIE())).append(" ₽");
        }
        if (op.getSalaryComment() != null && !op.getSalaryComment().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" [").append(op.getSalaryComment().trim()).append("]");
            else sb.append(op.getSalaryComment().trim());
        }
        if (Boolean.TRUE.equals(op.getSalaryCandidateRequest())) {
            if (sb.length() > 0) sb.append(" (по запросу кандидата)");
            else sb.append("По договоренности (по запросу кандидата)");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private static final Pattern RESUME_SALARY_PATTERN = Pattern.compile(
            "(?iu)(?:зарплат[а-я]*|з/п|желаемый\\s+доход|ожидания|вознаграждение|ставка)[^\\d\\n\\r]{0,25}?(?:от\\s*)?(\\d{1,3}(?:[\\s.,]\\d{3})+|\\d{2,7})\\s*(руб[а-я]*|rub|₽|usd|\\$|eur|€|тыс[а-я]*|т\\.р\\.?)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern STANDALONE_SALARY_PATTERN = Pattern.compile(
            "(?iu)(?:^|[\\s,(])(?:от\\s*)?(\\d{1,3}(?:[\\s.,]\\d{3})+|\\d{4,7})\\s*(руб[а-я]*|rub|₽|usd|\\$|eur|€|долл[а-я]*|евро)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public static String extractCandidateSalaryFromResume(String resumeText) {
        if (resumeText == null || resumeText.trim().isEmpty()) return null;
        Matcher m1 = RESUME_SALARY_PATTERN.matcher(resumeText);
        while (m1.find()) {
            String numStr = m1.group(1).replaceAll("[\\s.,]", "");
            String unit = m1.group(2).toLowerCase(Locale.ROOT);
            try {
                long num = Long.parseLong(numStr);
                if (unit.contains("тыс") || unit.contains("т.р")) num *= 1000L;
                if (num >= 15000 && num <= 5000000) {
                    DecimalFormat df = getDecimalFormat();
                    String currency = (unit.contains("usd") || unit.contains("$") || unit.contains("долл")) ? " $" : (unit.contains("eur") || unit.contains("€") || unit.contains("евро")) ? " €" : " ₽";
                    return df.format(num) + currency;
                }
            } catch (Exception ignored) {}
        }
        Matcher m2 = STANDALONE_SALARY_PATTERN.matcher(resumeText);
        while (m2.find()) {
            String numStr = m2.group(1).replaceAll("[\\s.,]", "");
            String unit = m2.group(2).toLowerCase(Locale.ROOT);
            try {
                long num = Long.parseLong(numStr);
                if (num >= 30000 && num <= 5000000) {
                    DecimalFormat df = getDecimalFormat();
                    String currency = (unit.contains("usd") || unit.contains("$") || unit.contains("долл")) ? " $" : (unit.contains("eur") || unit.contains("€") || unit.contains("евро")) ? " €" : " ₽";
                    return df.format(num) + currency;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static final Pattern NUMERIC_SALARY_PATTERN = Pattern.compile("(\\d[\\d\\s.,]{0,10})\\s*(тыс[а-я]*|т\\.р\\.?)?");

    public static Long parseSalaryNumericValue(String salaryStr) {
        if (salaryStr == null || salaryStr.trim().isEmpty()) return null;
        String cleaned = salaryStr.toLowerCase(Locale.ROOT).trim();
        Matcher matcher = NUMERIC_SALARY_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            try {
                String raw = matcher.group(1).trim();
                boolean isThousand = matcher.group(2) != null || cleaned.contains("тыс") || cleaned.contains("т.р");
                if (isThousand) {
                    raw = raw.replaceAll("\\s", "");
                    if (raw.contains(".") && raw.contains(",")) {
                        int lastDot = raw.lastIndexOf('.');
                        int lastComma = raw.lastIndexOf(',');
                        if (lastComma > lastDot) {
                            raw = raw.replace(".", "").replace(',', '.');
                        } else {
                            raw = raw.replace(",", "");
                        }
                    } else {
                        raw = raw.replace(',', '.');
                    }
                    double d = Double.parseDouble(raw);
                    return (long) (d * 1000.0);
                } else {
                    if (raw.matches(".*[.,]\\d{2}$")) {
                        raw = raw.substring(0, raw.length() - 3);
                    }
                    return Long.parseLong(raw.replaceAll("[\\s.,]", ""));
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static SalaryEvaluation evaluateSalary(String candidateSalary, OpenPosition vacancy) {
        if (vacancy == null) {
            return new SalaryEvaluation(CandidateVacancyMatchItem.SALARY_FIT_BY_AGREEMENT, "Не указаны параметры вакансии");
        }
        String vacancyOffer = formatVacancySalaryOffer(vacancy);
        String displayOffer = vacancyOffer != null ? vacancyOffer : "По договоренности / не указано";
        if (Boolean.TRUE.equals(vacancy.getSalaryCandidateRequest())) {
            String note = vacancyOffer != null
                    ? "По договоренности (" + vacancyOffer + ")"
                    : "По договоренности (заказчик готов обсуждать ставку по запросу кандидата)";
            return new SalaryEvaluation(CandidateVacancyMatchItem.SALARY_FIT_BY_AGREEMENT, note);
        }
        if (candidateSalary != null) {
            String lowerSal = candidateSalary.toLowerCase(Locale.ROOT);
            if (lowerSal.contains("$") || lowerSal.contains("usd") || lowerSal.contains("долл")
                    || lowerSal.contains("€") || lowerSal.contains("eur") || lowerSal.contains("евро")) {
                return new SalaryEvaluation(CandidateVacancyMatchItem.SALARY_FIT_BY_AGREEMENT, String.format(Locale.ROOT,
                        "Ожидания указаны в валюте (%s), вилка заказчика: %s (требуется конвертация/уточнение)",
                        candidateSalary, displayOffer));
            }
        }
        Long min = vacancy.getSalaryMin() != null ? vacancy.getSalaryMin().longValue() : null;
        Long max = vacancy.getSalaryMax() != null ? vacancy.getSalaryMax().longValue() : null;
        if (min == null && max == null) {
            String text = candidateSalary != null && !candidateSalary.trim().isEmpty()
                    ? "Вилка заказчика не зафиксирована (ожидания кандидата: " + candidateSalary + ")"
                    : "По договоренности (финансовые условия не зафиксированы)";
            return new SalaryEvaluation(CandidateVacancyMatchItem.SALARY_FIT_BY_AGREEMENT, text);
        }
        Long candVal = parseSalaryNumericValue(candidateSalary);
        if (candVal == null) {
            return new SalaryEvaluation(CandidateVacancyMatchItem.SALARY_FIT_BY_AGREEMENT, "Ожидания кандидата не указаны в резюме (вилка заказчика: " + displayOffer + ")");
        }

        DecimalFormat df = getDecimalFormat();
        if (max != null && candVal > max) {
            long diff = candVal - max;
            String text = max > 0
                    ? String.format(Locale.ROOT, "Ожидания превышают вилку заказчика на %d%% (запрос: %s ₽, максимум вилки: %s ₽, превышение: +%s ₽)",
                            (diff * 100) / max, df.format(candVal), df.format(max), df.format(diff))
                    : String.format(Locale.ROOT, "Ожидания превышают максимальную ставку заказчика (запрос: %s ₽, максимум: %s ₽, превышение: +%s ₽)",
                            df.format(candVal), df.format(max), df.format(diff));
            return new SalaryEvaluation(CandidateVacancyMatchItem.SALARY_FIT_ABOVE, text);
        } else if (min != null && candVal < min) {
            String text = String.format(Locale.ROOT,
                    "Ожидания ниже начальной вилки заказчика (запрос: %s ₽, вилка заказчика: %s)",
                    df.format(candVal), displayOffer);
            return new SalaryEvaluation(CandidateVacancyMatchItem.SALARY_FIT_BELOW, text);
        } else if (min != null && max == null) {
            String text = String.format(Locale.ROOT,
                    "Соответствие: ожидания не ниже начального предложения заказчика (%s ₽, от %s ₽)",
                    df.format(candVal), df.format(min));
            return new SalaryEvaluation(CandidateVacancyMatchItem.SALARY_FIT_IN_RANGE, text);
        } else {
            String text = String.format(Locale.ROOT,
                    "Полное соответствие: ожидания укладываются в вилку заказчика (%s ₽ в диапазоне %s)",
                    df.format(candVal), displayOffer);
            return new SalaryEvaluation(CandidateVacancyMatchItem.SALARY_FIT_IN_RANGE, text);
        }
    }

    public static String evaluateSalaryFit(String candidateSalary, OpenPosition vacancy) {
        return evaluateSalary(candidateSalary, vacancy).analysisText;
    }

    public static String classifySalaryFitStatus(String candidateSalary, OpenPosition vacancy) {
        return evaluateSalary(candidateSalary, vacancy).status;
    }

    private void enrichSalaryFit(CandidateVacancyMatchItem item,
                                OpenPosition vacancy,
                                CandidateInteractionProfile profile) {
        if (item == null) return;
        String candSalary = item.getCandidateSalary();
        if (candSalary == null || candSalary.trim().isEmpty()) {
            if (profile != null && profile.candidateSalaryExpectations != null && !profile.candidateSalaryExpectations.trim().isEmpty()) {
                candSalary = profile.candidateSalaryExpectations.trim();
            }
        }
        if (candSalary != null && !candSalary.trim().isEmpty()) {
            item.setCandidateSalary(candSalary);
        }

        String offerStr = formatVacancySalaryOffer(vacancy);
        if (offerStr != null) {
            item.setVacancySalary(offerStr);
        }

        boolean hasAnySalaryData = (candSalary != null && !candSalary.trim().isEmpty()) || offerStr != null;
        if (hasAnySalaryData) {
            SalaryEvaluation evaluation = evaluateSalary(item.getCandidateSalary(), vacancy);
            String fitAnalysis = item.getSalaryFitAnalysis();
            if (fitAnalysis == null || fitAnalysis.trim().isEmpty()) {
                fitAnalysis = evaluation.analysisText;
                item.setSalaryFitAnalysis(fitAnalysis);
                item.setSalaryFitStatus(evaluation.status);
            } else if (item.getSalaryFitStatus() == null || item.getSalaryFitStatus().trim().isEmpty()) {
                item.setSalaryFitStatus(evaluation.status);
            }

            // Обогащаем итоговое резюме (summary) информацией о зарплате
            String currentSummary = item.getSummary();
            String salarySummaryNote = String.format(Locale.ROOT,
                    "Зарплатные ожидания: %s | Предложение заказчика: %s (%s).",
                    item.getCandidateSalary() != null ? item.getCandidateSalary() : "не указаны",
                    offerStr != null ? offerStr : "по договоренности",
                    fitAnalysis != null ? fitAnalysis : evaluation.analysisText);

            if (currentSummary == null || currentSummary.trim().isEmpty()) {
                item.setSummary(salarySummaryNote);
            } else if (!currentSummary.contains("Зарплатные ожидания") && !currentSummary.contains("Предложение заказчика")) {
                item.setSummary(currentSummary.trim() + "\n\n• " + salarySummaryNote);
            }
        }
    }

    private List<CandidateVacancyMatchItem> parseAiResponse(String jsonText,
                                                           List<OpenPosition> chunk,
                                                           Map<UUID, OpenPosition> positionById,
                                                           Set<UUID> seenVacancyIds,
                                                           CandidateVacancyMatchReport report) {
        List<CandidateVacancyMatchItem> result = new ArrayList<>();
        String cleaned = cleanJsonText(jsonText);

        try {
            JsonNode root = objectMapper.readTree(cleaned);

            // Саммари кандидата
            if (root.has("candidateSummary") && report.getCandidateSummary().getTargetRoles().isEmpty()) {
                JsonNode summaryNode = root.get("candidateSummary");
                CandidateAiSummary summary = new CandidateAiSummary();
                if (summaryNode.has("targetRoles") && summaryNode.get("targetRoles").isArray()) {
                    summary.setTargetRoles(extractStringList(summaryNode.get("targetRoles")));
                }
                if (summaryNode.has("keySkills") && summaryNode.get("keySkills").isArray()) {
                    summary.setKeySkills(extractStringList(summaryNode.get("keySkills")));
                }
                if (summaryNode.has("experienceSummary")) {
                    summary.setExperienceSummary(summaryNode.get("experienceSummary").asText());
                }
                if (summaryNode.has("explicitPreferences") && summaryNode.get("explicitPreferences").isArray()) {
                    summary.setExplicitPreferences(extractStringList(summaryNode.get("explicitPreferences")));
                }
                if (summaryNode.hasNonNull("candidateSalaryExpectations")) {
                    summary.setCandidateSalaryExpectations(summaryNode.get("candidateSalaryExpectations").asText());
                }
                report.setCandidateSummary(summary);
            }

            if (root.has("generalConclusion") && report.getGeneralConclusion() == null) {
                report.setGeneralConclusion(root.get("generalConclusion").asText());
            }

            // Массив matches
            JsonNode matchesNode = root.has("matches") ? root.get("matches") : (root.isArray() ? root : null);
            if (matchesNode != null && matchesNode.isArray()) {
                Set<UUID> chunkPositionIds = chunk.stream().map(OpenPosition::getId).collect(Collectors.toSet());

                for (JsonNode m : matchesNode) {
                    if (!m.has("vacancyId")) continue;
                    String vidStr = m.get("vacancyId").asText();
                    UUID vid;
                    try {
                        vid = UUID.fromString(vidStr);
                    } catch (Exception ex) {
                        continue;
                    }

                    if (!chunkPositionIds.contains(vid) || seenVacancyIds.contains(vid)) {
                        continue;
                    }

                    OpenPosition op = positionById.get(vid);
                    if (op == null) continue;

                    CandidateVacancyMatchItem item = new CandidateVacancyMatchItem();
                    item.setOpenPositionId(vid);
                    item.setOpenPosition(op);
                    item.setVacancyName(op.getVacansyName());
                    item.setProjectName(op.getProjectName() != null ? op.getProjectName().getProjectName() : "");
                    item.setPositionName(op.getPositionType() != null ? op.getPositionType().getPositionRuName() : "");
                    item.setPriority(op.getPriority());

                    // Subscores
                    int roleFit = clamp(m.path("roleFit").asInt(0), 0, 25);
                    int skillsFit = clamp(m.path("skillsFit").asInt(0), 0, 35);
                    int expFit = clamp(m.path("experienceFit").asInt(0), 0, 20);
                    int prefFit = clamp(m.path("preferencesFit").asInt(0), 0, 10);
                    int domFit = clamp(m.path("domainFit").asInt(0), 0, 10);

                    item.setRoleFit(roleFit);
                    item.setSkillsFit(skillsFit);
                    item.setExperienceFit(expFit);
                    item.setPreferencesFit(prefFit);
                    item.setDomainFit(domFit);

                    // Расчет нормализованного score: сумма subscores
                    int computedScore = roleFit + skillsFit + expFit + prefFit + domFit;
                    int finalScore = clamp(computedScore, 0, 100);
                    item.setScore(finalScore);

                    // Verdict по стандартной шкале
                    item.setVerdict(resolveVerdict(finalScore));

                    if (m.has("matchedSkills")) {
                        item.setMatchedSkills(extractStringList(m.get("matchedSkills")));
                    }
                    if (m.has("missingCriticalRequirements")) {
                        item.setMissingCriticalRequirements(extractStringList(m.get("missingCriticalRequirements")));
                    }
                    if (m.has("risks")) {
                        item.setRisks(extractStringList(m.get("risks")));
                    }
                    if (m.has("reasonsToOffer")) {
                        item.setReasonsToOffer(extractStringList(m.get("reasonsToOffer")));
                    }
                    if (m.has("candidateEvidence")) {
                        item.setCandidateEvidence(extractStringList(m.get("candidateEvidence")));
                    }
                    if (m.has("vacancyEvidence")) {
                        item.setVacancyEvidence(extractStringList(m.get("vacancyEvidence")));
                    }
                    if (m.has("summary")) {
                        item.setSummary(m.get("summary").asText());
                    }
                    if (m.has("candidateSalary") && !m.get("candidateSalary").asText().isEmpty()) {
                        item.setCandidateSalary(m.get("candidateSalary").asText());
                    }
                    if (m.has("salaryFit") && !m.get("salaryFit").asText().isEmpty()) {
                        item.setSalaryFitAnalysis(m.get("salaryFit").asText());
                    } else if (m.has("salaryFitAnalysis") && !m.get("salaryFitAnalysis").asText().isEmpty()) {
                        item.setSalaryFitAnalysis(m.get("salaryFitAnalysis").asText());
                    }

                    if ((item.getCandidateSalary() == null || item.getCandidateSalary().trim().isEmpty())
                            && report != null && report.getCandidateSummary() != null
                            && report.getCandidateSummary().getCandidateSalaryExpectations() != null
                            && !report.getCandidateSummary().getCandidateSalaryExpectations().trim().isEmpty()) {
                        item.setCandidateSalary(report.getCandidateSummary().getCandidateSalaryExpectations().trim());
                    }

                    seenVacancyIds.add(vid);
                    result.add(item);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to parse JSON response for candidate-vacancy match: errorType={}", e.getClass().getSimpleName());
        }

        return result;
    }

    private List<CandidateVacancyMatchItem> buildHonestFallbackItems(JobCandidate candidate,
                                                                   List<CandidateSkill> candidateSkills,
                                                                   String resumeText,
                                                                   List<OpenPosition> openPositions) {
        List<CandidateVacancyMatchItem> list = new ArrayList<>();
        Set<String> candidateSkillNames = candidateSkills.stream()
                .filter(cs -> cs.getSkill() != null && cs.getSkill().getSkillName() != null)
                .map(cs -> cs.getSkill().getSkillName().toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toSet());

        String candidatePosLower = candidate.getPersonPosition() != null && candidate.getPersonPosition().getPositionRuName() != null ?
                candidate.getPersonPosition().getPositionRuName().toLowerCase(Locale.ROOT) : "";

        for (OpenPosition op : openPositions) {
            if (Boolean.TRUE.equals(op.getOpenClose())) {
                continue;
            }
            CandidateVacancyMatchItem item = new CandidateVacancyMatchItem();
            item.setOpenPositionId(op.getId());
            item.setOpenPosition(op);
            item.setVacancyName(op.getVacansyName());
            item.setProjectName(op.getProjectName() != null ? op.getProjectName().getProjectName() : "");
            item.setPositionName(op.getPositionType() != null ? op.getPositionType().getPositionRuName() : "");
            item.setPriority(op.getPriority());

            List<String> matchedSkills = new ArrayList<>();
            List<String> missingRequirements = new ArrayList<>();

            if (op.getSkillsList() != null && !op.getSkillsList().isEmpty()) {
                List<SkillTree> matchedFromResume = SkillNameMatcher.matchText(op.getSkillsList(), resumeText);
                Set<UUID> matchedSkillIds = matchedFromResume.stream().map(SkillTree::getId).collect(Collectors.toSet());

                for (SkillTree st : op.getSkillsList()) {
                    if (st.getSkillName() != null) {
                        String sName = st.getSkillName().trim();
                        String sLower = sName.toLowerCase(Locale.ROOT);
                        if (candidateSkillNames.contains(sLower) || matchedSkillIds.contains(st.getId())) {
                            matchedSkills.add(sName);
                        } else {
                            missingRequirements.add(sName);
                        }
                    }
                }
            }

            int roleFit = 0;
            if (!candidatePosLower.isEmpty() && op.getPositionType() != null && op.getPositionType().getPositionRuName() != null) {
                String vacPosLower = op.getPositionType().getPositionRuName().toLowerCase(Locale.ROOT);
                if (candidatePosLower.equals(vacPosLower)) {
                    roleFit = 20;
                } else if (candidatePosLower.contains(vacPosLower) || vacPosLower.contains(candidatePosLower)) {
                    roleFit = 14;
                }
            }

            int skillsFit = 0;
            if (!matchedSkills.isEmpty()) {
                int totalRequired = matchedSkills.size() + missingRequirements.size();
                skillsFit = (int) Math.round(((double) matchedSkills.size() / Math.max(1, totalRequired)) * 30.0);
            }

            int score = clamp(roleFit + skillsFit + FALLBACK_BASE_EXPERIENCE_FIT, 0, 100);

            item.setRoleFit(roleFit);
            item.setSkillsFit(skillsFit);
            item.setExperienceFit(FALLBACK_BASE_EXPERIENCE_FIT);
            item.setPreferencesFit(0);
            item.setDomainFit(0);
            item.setScore(score);
            item.setVerdict("Предварительная оценка без AI");
            item.setMatchedSkills(matchedSkills);
            item.setMissingCriticalRequirements(missingRequirements);
            item.setReasonsToOffer(Collections.singletonList("Оценка рассчитана на основе совпадения ключевых навыков и должности"));
            item.setSummary("Внимание: расчет выполнен по базовым правилам без использования нейросети (AI недоступен).");

            list.add(item);
        }

        return list;
    }

    private String resolveVerdict(int score) {
        if (score >= 80) return "Рекомендуется предложить";
        if (score >= 65) return "Имеет смысл рассмотреть";
        if (score >= 45) return "Слабое соответствие";
        return "Не рекомендуется";
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private List<String> extractStringList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode n : arrayNode) {
                if (n.isTextual() && !n.asText().trim().isEmpty()) {
                    list.add(n.asText().trim());
                }
            }
        }
        return list;
    }

    private String cleanJsonText(String text) {
        if (text == null) return "";
        String s = text.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        s = s.trim();

        int firstObj = s.indexOf('{');
        int firstArr = s.indexOf('[');
        int start = -1;
        if (firstObj >= 0 && firstArr >= 0) {
            start = Math.min(firstObj, firstArr);
        } else if (firstObj >= 0) {
            start = firstObj;
        } else {
            start = firstArr;
        }

        int lastObj = s.lastIndexOf('}');
        int lastArr = s.lastIndexOf(']');
        int end = Math.max(lastObj, lastArr);

        if (start >= 0 && end > start) {
            return s.substring(start, end + 1).trim();
        }
        return s;
    }

    /**
     * Удаляет завершённые и забытые операции, чтобы polling-контракт не создавал
     * неограниченный in-memory кэш. Снимки нужны только во время работы экрана и
     * ещё один час после её завершения.
     */
    private void cleanupExpiredProgress() {
        long now = System.currentTimeMillis();
        progressStates.entrySet().removeIf(entry -> now - entry.getValue().getLastUpdatedAt() > PROGRESS_TTL_MILLIS);
    }

    /**
     * Синхронизированное состояние одной операции. Все наружные чтения получают
     * отдельный DTO, поэтому web-поток не наблюдает частично обновлённые счётчики.
     */
    static final class MatchProgressState {
        private final UUID operationId;
        private final long startedAt;
        private long analysisStartedAt;
        private long lastUpdatedAt;
        private String phase = "PREPARING";
        private String statusMessage = "Подготовка данных кандидата и открытых вакансий.";
        private int totalVacancies;
        private int processedVacancies;
        private int totalChunks;
        private int completedChunks;
        private int failedChunks;
        private boolean completed;
        private boolean success;

        MatchProgressState(UUID operationId, long startedAt) {
            this.operationId = operationId;
            this.startedAt = startedAt;
            this.lastUpdatedAt = startedAt;
        }

        synchronized void updateStage(String phase, String statusMessage) {
            this.phase = phase;
            this.statusMessage = statusMessage;
            this.lastUpdatedAt = System.currentTimeMillis();
        }

        synchronized void beginCandidateAnalysis(int totalCandidates) {
            this.totalVacancies = Math.max(0, totalCandidates);
            this.totalChunks = Math.max(0, totalCandidates);
            this.processedVacancies = 0;
            this.completedChunks = 0;
            this.analysisStartedAt = System.currentTimeMillis();
            this.lastUpdatedAt = analysisStartedAt;
            this.phase = "AI-анализ кандидатов";
            this.statusMessage = "Отобрано кандидатов для анализа: " + this.totalVacancies + ". Запуск сопоставления...";
        }

        synchronized void startCandidate(int candidateIndex, String candidateFullName, String position) {
            this.lastUpdatedAt = System.currentTimeMillis();
            this.phase = "Анализ: " + (candidateFullName != null ? candidateFullName : "Кандидат " + candidateIndex);
            this.statusMessage = "Кандидат " + candidateIndex + " из " + totalVacancies + ": "
                    + (candidateFullName != null ? candidateFullName : "")
                    + (position != null && !position.isEmpty() ? " (" + position + ")" : "")
                    + " — сопоставление с вакансией...";
        }

        synchronized void completeCandidate(int candidateIndex, String candidateFullName, Integer score, boolean candidateSucceeded) {
            this.processedVacancies = Math.min(totalVacancies, candidateIndex);
            this.completedChunks = Math.min(totalChunks, candidateIndex);
            if (!candidateSucceeded) {
                this.failedChunks++;
            }
            this.lastUpdatedAt = System.currentTimeMillis();
            this.phase = completedChunks < totalChunks ? "AI-анализ кандидатов" : "Финализация";
            String scoreStr = score != null ? " (рейтинг: " + score + "%)" : "";
            this.statusMessage = "Обработано " + processedVacancies + " из " + totalVacancies + " кандидатов. "
                    + (candidateFullName != null ? "Последний: " + candidateFullName + scoreStr : "");
        }

        synchronized void beginAnalysis(int totalVacancies, int totalChunks) {
            this.totalVacancies = Math.max(0, totalVacancies);
            this.totalChunks = Math.max(0, totalChunks);
            this.analysisStartedAt = System.currentTimeMillis();
            this.lastUpdatedAt = analysisStartedAt;
            this.phase = "ANALYZING";
            this.statusMessage = "Подготовлено вакансий: " + this.totalVacancies
                    + ". Аналитических пакетов: " + this.totalChunks + ".";
        }

        synchronized void startChunk(int chunkNumber, int chunkSize) {
            lastUpdatedAt = System.currentTimeMillis();
            phase = "ANALYZING";
            statusMessage = "AI анализирует пакет " + chunkNumber + " из " + totalChunks
                    + " (вакансий в пакете: " + Math.max(0, chunkSize) + ").";
        }

        synchronized void completeChunk(int chunkSize, boolean chunkSucceeded) {
            completedChunks = Math.min(totalChunks, completedChunks + 1);
            processedVacancies = Math.min(totalVacancies, processedVacancies + Math.max(0, chunkSize));
            if (!chunkSucceeded) {
                failedChunks++;
            }
            lastUpdatedAt = System.currentTimeMillis();
            phase = completedChunks < totalChunks ? "ANALYZING" : "FINALIZING";
            statusMessage = "Обработано вакансий: " + processedVacancies + " из " + totalVacancies
                    + "; завершено пакетов: " + completedChunks + " из " + totalChunks
                    + (failedChunks > 0 ? "; пакетов с ошибкой: " + failedChunks : "") + ".";
        }

        synchronized void finish(boolean success, String finalStatusMessage) {
            this.success = success;
            this.completed = true;
            this.lastUpdatedAt = System.currentTimeMillis();
            this.phase = success ? (failedChunks > 0 ? "COMPLETED_WITH_WARNINGS" : "COMPLETED") : "FAILED";
            if (finalStatusMessage != null && !finalStatusMessage.trim().isEmpty()) {
                this.statusMessage = finalStatusMessage;
            } else if (success && failedChunks > 0) {
                this.statusMessage = "Подбор завершён. Часть аналитических пакетов обработана с ошибкой: " + failedChunks + ".";
            } else if (success) {
                this.statusMessage = "Подбор вакансий завершён.";
            }
        }

        synchronized CandidateVacancyMatchProgress snapshot(long now) {
            CandidateVacancyMatchProgress snapshot = new CandidateVacancyMatchProgress();
            snapshot.setOperationId(operationId);
            snapshot.setPhase(phase);
            snapshot.setStatusMessage(statusMessage);
            snapshot.setTotalVacancies(totalVacancies);
            snapshot.setProcessedVacancies(processedVacancies);
            snapshot.setTotalChunks(totalChunks);
            snapshot.setCompletedChunks(completedChunks);
            snapshot.setFailedChunks(failedChunks);
            snapshot.setElapsedMillis(Math.max(0L, now - startedAt));
            snapshot.setEstimatedRemainingMillis(estimateRemainingMillis(now));
            snapshot.setProgressPercent(calculateProgressPercent());
            snapshot.setCompleted(completed);
            snapshot.setSuccess(success);
            return snapshot;
        }

        synchronized long getLastUpdatedAt() {
            return lastUpdatedAt;
        }

        UUID getOperationId() {
            return operationId;
        }

        private int calculateProgressPercent() {
            if (completed) {
                return 100;
            }
            if (totalVacancies <= 0 || processedVacancies <= 0) {
                return 0;
            }
            return Math.min(99, (int) (((long) processedVacancies * 100L) / totalVacancies));
        }

        private Long estimateRemainingMillis(long now) {
            if (completed || completedChunks <= 0 || totalChunks <= completedChunks || analysisStartedAt <= 0L) {
                return completed ? 0L : null;
            }
            long analysisElapsed = Math.max(0L, now - analysisStartedAt);
            long averageChunkMillis = analysisElapsed / completedChunks;
            return averageChunkMillis * (totalChunks - completedChunks);
        }
    }
}
