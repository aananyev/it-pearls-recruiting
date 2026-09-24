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
                .view(viewBuilder -> viewBuilder.addAll("textCV", "datePost", "resumePosition", "toVacancy"))
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
                        "projectName.projectName", "priority", "openClose"
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

        // 5. Формирование контекста кандидата
        String candidateProfile = buildCandidateProfileString(candidate);
        String candidateSkillsText = buildCandidateSkillsString(candidateSkills);
        String candidateResumeText = buildCandidateResumeText(cvList);

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
            enrichItemsWithCandidateAndStatus(allItems, candidate, null);
            allItems.sort(getComparator());
            report.setItems(allItems);
            report.setMatchedVacanciesCount(allItems.size());
            report.setAiExecutionResult(lastAiResult);
            report.setFallbackUsed(false);
            report.setSuccess(true);
            return report;
        }

        // 7. Честный Fallback при недоступности AI: рассчитываем базовую эвристическую оценку без фальсификации
        log.warn("AI service unavailable for candidate-vacancy match. Using honest rule-based fallback.");
        List<CandidateVacancyMatchItem> fallbackItems = buildHonestFallbackItems(candidate, candidateSkills, candidateResumeText, openPositions);
        enrichItemsWithCandidateAndStatus(fallbackItems, candidate, null);
        fallbackItems.sort(getComparator());

        report.setItems(fallbackItems);
        report.setMatchedVacanciesCount(fallbackItems.size());
        report.setFallbackUsed(true);
        report.setGeneralConclusion("Внимание: экспертный AI-анализ временно недоступен. Отображена предварительная оценка по совпадению ключевых параметров.");
        report.setStatusMessage("Предварительная оценка без AI");
        report.setSuccess(true);
        return report;
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
                            "projectName.projectName", "priority", "openClose", "searchMap"
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
                    String recruiterActivity = resolveRecruiterActivityDescription(cand.getId());
                    String lastJobDomain = resolveLastJobDomainDescription(cand);

                    String candidateProfile = buildCandidateProfileString(cand)
                            + "\nАктуальность взаимодействия с рекрутером в HRM: " + recruiterActivity
                            + "\nПредметная область последнего места работы: " + lastJobDomain;
                    String candidateSkillsText = buildCandidateSkillsString(candidateSkills);
                    String candidateResumeText = buildCandidateResumeText(cvList);

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

    private void enrichItemsWithCandidateAndStatus(List<CandidateVacancyMatchItem> items, JobCandidate candidate, UUID vacancyId) {
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

            list.add(v);
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
        } catch (Exception e) {
            log.error("Failed to serialize vacancies to JSON", e);
            return "[]";
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
