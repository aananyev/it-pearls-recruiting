package com.company.hunttech.service;

import com.company.hunttech.entity.*;
import com.company.hunttech.service.dto.BulkTakeIntoWorkResult;
import com.company.hunttech.service.dto.TakeIntoWorkResult;
import com.company.hunttech.service.dto.VacancyMatchMonitoringDto;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.TimeSource;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.*;

@Service(CandidateVacancyWorkflowService.NAME)
public class CandidateVacancyWorkflowServiceBean implements CandidateVacancyWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(CandidateVacancyWorkflowServiceBean.class);

    @Inject
    private DataManager dataManager;

    @Inject
    private Metadata metadata;

    @Inject
    private UserSessionSource userSessionSource;

    @Inject
    private AiExecutionService aiExecutionService;

    @Inject
    private TimeSource timeSource;

    @Override
    @Transactional
    public TakeIntoWorkResult takeIntoWork(UUID candidateId, UUID vacancyId, Integer aiScore, UUID matchRunId) {
        TakeIntoWorkResult result = new TakeIntoWorkResult();

        if (candidateId == null || vacancyId == null) {
            result.setSuccess(false);
            result.setMessage("Идентификатор кандидата и вакансии обязательны.");
            return result;
        }

        User currentUser = userSessionSource.getUserSession().getUser();

        JobCandidate candidate = dataManager.load(JobCandidate.class)
                .id(candidateId)
                .view("jobCandidate-full-view")
                .optional()
                .orElse(null);

        OpenPosition vacancy = dataManager.load(OpenPosition.class)
                .id(vacancyId)
                .view("openPosition-full-view")
                .optional()
                .orElse(null);

        if (candidate == null || vacancy == null) {
            result.setSuccess(false);
            result.setMessage("Кандидат или вакансия не найдены в системе.");
            return result;
        }

        if (Boolean.TRUE.equals(vacancy.getOpenClose())) {
            result.setSuccess(false);
            result.setMessage("Вакансия закрыта (openClose = true). Добавление кандидатов возможно только на открытые позиции.");
            return result;
        }

        result.setCandidateFullName(candidate.getFullName());
        result.setVacancyTitle(vacancy.getVacansyName());

        // 1. Проверка идемпотентности: находится ли кандидат уже в работе по этой вакансии в IteractionList
        List<IteractionList> existingList = dataManager.load(IteractionList.class)
                .query("select e from hunttech_IteractionList e where e.candidate.id = :candId and e.vacancy.id = :vacId order by e.dateIteraction desc")
                .parameter("candId", candidateId)
                .parameter("vacId", vacancyId)
                .view("iteractionList-view")
                .list();

        if (!existingList.isEmpty()) {
            IteractionList existing = existingList.get(0);
            result.setSuccess(true);
            result.setAlreadyInWork(true);
            result.setInteractionId(existing.getId());
            result.setMessage("Кандидат уже находится в работе по этой вакансии (взаимодействие от "
                    + (existing.getDateIteraction() != null ? existing.getDateIteraction().toString() : "—") + ").");

            // Фиксируем provenance feedback в статусе IN_WORK
            saveOrUpdateFeedback(matchRunId, vacancy, candidate, currentUser, aiScore, RecruiterDecisionType.IN_WORK, null, null, existing, existing.getIteractionType());
            return result;
        }

        // 2. Определение стартового типа взаимодействия (sign_start_case = true или дефолтный)
        Iteraction startType = dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e where e.signStartCase = true order by e.number_ asc")
                .optional()
                .orElse(null);

        if (startType == null) {
            startType = dataManager.load(Iteraction.class)
                    .query("select e from hunttech_Iteraction e where e.iterationName like '%Предложение работы%' or e.iterationName like '%Ресерчинг%' order by e.iterationName asc")
                    .optional()
                    .orElse(null);
        }

        if (startType == null) {
            startType = dataManager.load(Iteraction.class)
                    .query("select e from hunttech_Iteraction e order by e.number_ asc")
                    .list().stream().findFirst().orElse(null);
        }

        // 3. Создание взаимодействия IteractionList
        IteractionList newInteraction = metadata.create(IteractionList.class);
        newInteraction.setCandidate(candidate);
        newInteraction.setVacancy(vacancy);
        newInteraction.setDateIteraction(timeSource.currentTimestamp());
        if (currentUser instanceof ExtUser) {
            newInteraction.setRecrutier((ExtUser) currentUser);
        }
        newInteraction.setRecrutierName(currentUser.getName() != null ? currentUser.getName() : currentUser.getLogin());
        newInteraction.setIteractionType(startType);
        newInteraction.setComment("Кандидат взят в работу по результатам AI-подбора на вакансию \""
                + vacancy.getVacansyName() + "\" (AI score: " + (aiScore != null ? aiScore : "—") + ").");
        newInteraction.setAddString(matchRunId != null ? "AI_MATCH:" + matchRunId : "AI_MATCH");

        dataManager.commit(newInteraction);

        // 4. Фиксация в аудите решений рекрутера
        saveOrUpdateFeedback(matchRunId, vacancy, candidate, currentUser, aiScore, RecruiterDecisionType.IN_WORK, null, null, newInteraction, startType);

        // 5. Обновление счетчиков в matchRun
        if (matchRunId != null) {
            updateMatchRunStats(matchRunId, 1, 0, 0, 0);
        }

        result.setSuccess(true);
        result.setAlreadyInWork(false);
        result.setInteractionId(newInteraction.getId());
        result.setMessage("Кандидат успешно взят в работу по вакансии \"" + vacancy.getVacansyName() + "\".");
        return result;
    }

    @Override
    public IteractionList getExistingRelation(UUID candidateId, UUID vacancyId) {
        if (candidateId == null || vacancyId == null) {
            return null;
        }
        return dataManager.load(IteractionList.class)
                .query("select e from hunttech_IteractionList e where e.candidate.id = :candId and e.vacancy.id = :vacId order by e.dateIteraction desc")
                .parameter("candId", candidateId)
                .parameter("vacId", vacancyId)
                .view("iteractionList-view")
                .optional()
                .orElse(null);
    }

    @Override
    public BulkTakeIntoWorkResult bulkTakeIntoWork(List<UUID> candidateIds, UUID vacancyId, UUID matchRunId) {
        BulkTakeIntoWorkResult bulkResult = new BulkTakeIntoWorkResult();
        if (candidateIds == null || candidateIds.isEmpty() || vacancyId == null) {
            return bulkResult;
        }

        bulkResult.setTotalSelected(candidateIds.size());

        for (UUID candId : candidateIds) {
            try {
                TakeIntoWorkResult single = takeIntoWork(candId, vacancyId, null, matchRunId);
                if (single.isSuccess()) {
                    if (single.isAlreadyInWork()) {
                        bulkResult.setAlreadyInWorkCount(bulkResult.getAlreadyInWorkCount() + 1);
                    } else {
                        bulkResult.setAddedCount(bulkResult.getAddedCount() + 1);
                    }
                } else {
                    bulkResult.setErrorCount(bulkResult.getErrorCount() + 1);
                    bulkResult.getErrorDetails().add(single.getMessage());
                }
            } catch (Exception e) {
                log.error("Error adding candidate {} to vacancy {}: {}", candId, vacancyId, e.getMessage(), e);
                bulkResult.setErrorCount(bulkResult.getErrorCount() + 1);
                bulkResult.getErrorDetails().add("Ошибка для ID " + candId + ": " + e.getMessage());
            }
        }

        return bulkResult;
    }

    @Override
    @Transactional
    public void recordFeedback(UUID candidateId, UUID vacancyId, String decision, String rejectionReason, String comment, Integer aiScore, UUID matchRunId) {
        if (candidateId == null || vacancyId == null) {
            return;
        }

        User currentUser = userSessionSource.getUserSession().getUser();
        JobCandidate candidate = dataManager.load(JobCandidate.class).id(candidateId).optional().orElse(null);
        OpenPosition vacancy = dataManager.load(OpenPosition.class).id(vacancyId).optional().orElse(null);

        if (candidate == null || vacancy == null) {
            return;
        }

        RecruiterDecisionType decType = RecruiterDecisionType.NEW;
        if ("IN_WORK".equalsIgnoreCase(decision)) decType = RecruiterDecisionType.IN_WORK;
        else if ("POSTPONED".equalsIgnoreCase(decision)) decType = RecruiterDecisionType.POSTPONED;
        else if ("REJECTED".equalsIgnoreCase(decision)) decType = RecruiterDecisionType.REJECTED;

        Iteraction reasonIteraction = null;
        if (rejectionReason != null && !rejectionReason.trim().isEmpty()) {
            reasonIteraction = dataManager.load(Iteraction.class)
                    .query("select e from hunttech_Iteraction e where e.iterationName = :name")
                    .parameter("name", rejectionReason.trim())
                    .optional()
                    .orElse(null);
        }

        saveOrUpdateFeedback(matchRunId, vacancy, candidate, currentUser, aiScore, decType, rejectionReason, comment, null, reasonIteraction);

        if (matchRunId != null) {
            if (decType == RecruiterDecisionType.REJECTED) {
                updateMatchRunStats(matchRunId, 0, 1, 0, 0);
            } else if (decType == RecruiterDecisionType.POSTPONED) {
                updateMatchRunStats(matchRunId, 0, 0, 1, 0);
            }
        }
    }

    @Override
    public IteractionList prepareInteractionDraft(UUID candidateId, UUID vacancyId, Integer aiScore, List<String> matchedSkills, List<String> missingRequirements) {
        JobCandidate candidate = dataManager.load(JobCandidate.class).id(candidateId).view("jobCandidate-full-view").optional().orElse(null);
        OpenPosition vacancy = dataManager.load(OpenPosition.class).id(vacancyId).view("openPosition-full-view").optional().orElse(null);

        IteractionList draft = metadata.create(IteractionList.class);
        draft.setCandidate(candidate);
        draft.setVacancy(vacancy);
        draft.setDateIteraction(timeSource.currentTimestamp());

        User currentUser = userSessionSource.getUserSession().getUser();
        if (currentUser instanceof ExtUser) {
            draft.setRecrutier((ExtUser) currentUser);
        }
        draft.setRecrutierName(currentUser.getName() != null ? currentUser.getName() : currentUser.getLogin());

        Iteraction startType = dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e where e.signStartCase = true order by e.number_ asc")
                .optional()
                .orElse(null);
        if (startType == null) {
            startType = dataManager.load(Iteraction.class)
                    .query("select e from hunttech_Iteraction e order by e.number_ asc")
                    .list().stream().findFirst().orElse(null);
        }
        draft.setIteractionType(startType);

        StringBuilder comment = new StringBuilder();
        comment.append("Кандидат выбран по результатам AI-подбора на вакансию:\n");
        if (vacancy != null) {
            comment.append(vacancy.getVacansyName()).append("\n\n");
        }
        if (aiScore != null) {
            comment.append("Оценка соответствия AI: ").append(aiScore).append("%\n\n");
        }
        if (matchedSkills != null && !matchedSkills.isEmpty()) {
            comment.append("Совпадающие навыки:\n").append(String.join(", ", matchedSkills)).append("\n\n");
        }
        if (missingRequirements != null && !missingRequirements.isEmpty()) {
            comment.append("Требует уточнения:\n").append(String.join(", ", missingRequirements)).append("\n");
        }

        draft.setComment(comment.toString().trim());
        return draft;
    }

    @Override
    public String generateOutreachDraft(UUID candidateId, UUID vacancyId, List<String> reasonsToOffer, List<String> matchedSkills) {
        JobCandidate candidate = dataManager.load(JobCandidate.class).id(candidateId).view("jobCandidate-full-view").optional().orElse(null);
        OpenPosition vacancy = dataManager.load(OpenPosition.class).id(vacancyId).view("openPosition-full-view").optional().orElse(null);

        if (candidate == null || vacancy == null) {
            return "Не удалось сформировать черновик: кандидат или вакансия не найдены.";
        }

        // Подготовка компактного контекста
        String vacancyTitle = vacancy.getVacansyName() != null ? vacancy.getVacansyName() : "Вакансия";
        String projectName = vacancy.getProjectName() != null ? vacancy.getProjectName().getProjectName() : "IT-проект";
        String workFormat = Boolean.TRUE.equals(vacancy.getRemoteWork()) ? "Удаленная работа"
                : (vacancy.getCityPosition() != null ? vacancy.getCityPosition().getCityRuName() : "Локальный офис / гибрид");

        String rawComment = vacancy.getComment() != null ? vacancy.getComment() : "";
        String cleanComment = Jsoup.parse(rawComment).text();
        String vacancySummary = cleanComment.length() > 600 ? cleanComment.substring(0, 600) + "..." : cleanComment;

        String keyRequirements = matchedSkills != null && !matchedSkills.isEmpty()
                ? String.join(", ", matchedSkills) : "В соответствии с профилем роли";

        String candidateName = candidate.getFirstName() != null ? candidate.getFirstName() : candidate.getFullName();
        String candidateRole = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
        String relevantSkills = matchedSkills != null && !matchedSkills.isEmpty() ? String.join(", ", matchedSkills) : "Профильные навыки";
        String reasons = reasonsToOffer != null && !reasonsToOffer.isEmpty() ? String.join("; ", reasonsToOffer) : "Релевантный опыт и стек технологий";

        Map<String, Object> params = new HashMap<>();
        params.put("vacancyTitle", vacancyTitle);
        params.put("projectName", projectName);
        params.put("workFormat", workFormat);
        params.put("vacancySummary", vacancySummary);
        params.put("keyRequirements", keyRequirements);
        params.put("candidateName", candidateName);
        params.put("candidateRole", candidateRole);
        params.put("relevantSkills", relevantSkills);
        params.put("relevantExperience", "Опыт работы в релевантной роли");
        params.put("reasonsToOffer", reasons);

        try {
            AiExecutionResult aiResult = aiExecutionService.executeText(FUNCTION_OUTREACH_DRAFT, params);
            if (aiResult != null && aiResult.getText() != null && !aiResult.getText().trim().isEmpty()) {
                return aiResult.getText().trim();
            }
        } catch (Exception e) {
            log.error("Failed to generate outreach draft: {}", e.getMessage(), e);
        }

        User currentUser = userSessionSource.getUserSession().getUser();

        // Детерминированный fallback при ошибке LLM
        return String.format(
                "Здравствуйте, %s!\n\n" +
                "Меня зовут %s, компания HuntTech.\n" +
                "Обратил внимание на ваш профессиональный опыт в качестве %s. Сейчас у нас открыта позиция «%s» (%s, проект: %s).\n\n" +
                "Ваш стек (%s) отлично соотносится с задачами проекта. Буду рад обсудить подробности и ответить на ваши вопросы, если вы сейчас открыты к новым предложениям.\n\n" +
                "С уважением,\n%s",
                candidateName,
                currentUser != null && currentUser.getName() != null ? currentUser.getName() : "рекрутер",
                candidateRole,
                vacancyTitle,
                workFormat,
                projectName,
                relevantSkills,
                currentUser != null && currentUser.getName() != null ? currentUser.getName() : "команда HuntTech"
        );
    }

    @Override
    @Transactional
    public VacancyCandidateMatchRun createMatchRun(UUID vacancyId, UUID candidateId, String aiFunctionCode, int candidatesCount) {
        VacancyCandidateMatchRun run = metadata.create(VacancyCandidateMatchRun.class);
        if (vacancyId != null) {
            run.setVacancy(dataManager.load(OpenPosition.class).id(vacancyId).optional().orElse(null));
        }
        if (candidateId != null) {
            run.setCandidate(dataManager.load(JobCandidate.class).id(candidateId).optional().orElse(null));
        }
        run.setRecruiter(userSessionSource.getUserSession().getUser());
        run.setRunTime(timeSource.currentTimestamp());
        run.setAiFunctionCode(aiFunctionCode != null ? aiFunctionCode : "CANDIDATE_VACANCY_MATCH_ANALYZE");
        run.setCandidatesReviewedCount(candidatesCount);
        run.setTakenIntoWorkCount(0);
        run.setRejectedCount(0);
        run.setPostponedCount(0);
        run.setOutreachDraftsGeneratedCount(0);
        dataManager.commit(run);
        return run;
    }

    @Override
    public VacancyMatchMonitoringDto getMatchMonitoring(Date from, Date to) {
        VacancyMatchMonitoringDto dto = new VacancyMatchMonitoringDto();
        Date fromDate = from != null ? from : new Date(0);
        Date toDate = to != null ? to : new Date();

        List<VacancyCandidateMatchRun> runs = dataManager.load(VacancyCandidateMatchRun.class)
                .query("select e from hunttech_VacancyCandidateMatchRun e where e.runTime >= :from and e.runTime <= :to")
                .parameter("from", fromDate)
                .parameter("to", toDate)
                .list();

        dto.setTotalRuns(runs.size());
        int totalReviewed = 0;
        int totalTaken = 0;
        int totalRejected = 0;
        int totalPostponed = 0;
        int totalOutreach = 0;

        for (VacancyCandidateMatchRun r : runs) {
            if (r.getCandidatesReviewedCount() != null) totalReviewed += r.getCandidatesReviewedCount();
            if (r.getTakenIntoWorkCount() != null) totalTaken += r.getTakenIntoWorkCount();
            if (r.getRejectedCount() != null) totalRejected += r.getRejectedCount();
            if (r.getPostponedCount() != null) totalPostponed += r.getPostponedCount();
            if (r.getOutreachDraftsGeneratedCount() != null) totalOutreach += r.getOutreachDraftsGeneratedCount();
        }

        dto.setCandidatesReviewed(totalReviewed);
        dto.setTakenIntoWorkCount(totalTaken);
        dto.setRejectedCount(totalRejected);
        dto.setPostponedCount(totalPostponed);
        dto.setOutreachDraftsCount(totalOutreach);

        if (totalReviewed > 0) {
            dto.setAcceptanceRate((totalTaken * 100.0) / totalReviewed);
        } else {
            dto.setAcceptanceRate(0.0);
        }

        // Анализ по диапазонам баллов AI: "80-100", "60-79", "<60"
        List<VacancyCandidateMatchFeedback> feedbacks = dataManager.load(VacancyCandidateMatchFeedback.class)
                .query("select e from hunttech_VacancyCandidateMatchFeedback e where e.decisionTime >= :from and e.decisionTime <= :to")
                .parameter("from", fromDate)
                .parameter("to", toDate)
                .list();

        Map<String, int[]> bands = new LinkedHashMap<>();
        bands.put("80-100", new int[]{0, 0, 0, 0}); // total, taken, rejected, postponed
        bands.put("60-79", new int[]{0, 0, 0, 0});
        bands.put("<60", new int[]{0, 0, 0, 0});

        for (VacancyCandidateMatchFeedback fb : feedbacks) {
            int score = fb.getAiScore() != null ? fb.getAiScore() : 0;
            String bandKey = score >= 80 ? "80-100" : (score >= 60 ? "60-79" : "<60");
            int[] st = bands.get(bandKey);
            st[0]++; // total
            if (fb.getDecision() == RecruiterDecisionType.IN_WORK) st[1]++;
            else if (fb.getDecision() == RecruiterDecisionType.REJECTED) st[2]++;
            else if (fb.getDecision() == RecruiterDecisionType.POSTPONED) st[3]++;
        }

        dto.setScoreBandBreakdown(bands);
        return dto;
    }

    private void saveOrUpdateFeedback(UUID matchRunId, OpenPosition vacancy, JobCandidate candidate, User recruiter,
                                      Integer aiScore, RecruiterDecisionType decision, String rejectionReason,
                                      String comment, IteractionList iteractionList, Iteraction iteractionType) {
        VacancyCandidateMatchFeedback fb = dataManager.load(VacancyCandidateMatchFeedback.class)
                .query("select e from hunttech_VacancyCandidateMatchFeedback e where e.vacancy.id = :vId and e.candidate.id = :cId")
                .parameter("vId", vacancy.getId())
                .parameter("cId", candidate.getId())
                .optional()
                .orElse(null);

        if (fb == null) {
            fb = metadata.create(VacancyCandidateMatchFeedback.class);
            fb.setVacancy(vacancy);
            fb.setCandidate(candidate);
        }

        fb.setRecruiter(recruiter);
        fb.setDecision(decision);
        if (aiScore != null) fb.setAiScore(aiScore);
        if (rejectionReason != null) fb.setRejectionReason(rejectionReason);
        if (comment != null) fb.setComment(comment);
        if (iteractionList != null) fb.setIteractionList(iteractionList);
        if (iteractionType != null) fb.setIteractionType(iteractionType);
        fb.setDecisionTime(timeSource.currentTimestamp());

        if (matchRunId != null) {
            VacancyCandidateMatchRun run = dataManager.load(VacancyCandidateMatchRun.class).id(matchRunId).optional().orElse(null);
            if (run != null) fb.setMatchRun(run);
        }

        dataManager.commit(fb);
    }

    private void updateMatchRunStats(UUID matchRunId, int takenDelta, int rejectedDelta, int postponedDelta, int outreachDelta) {
        try {
            VacancyCandidateMatchRun run = dataManager.load(VacancyCandidateMatchRun.class).id(matchRunId).optional().orElse(null);
            if (run != null) {
                if (takenDelta > 0) run.setTakenIntoWorkCount((run.getTakenIntoWorkCount() != null ? run.getTakenIntoWorkCount() : 0) + takenDelta);
                if (rejectedDelta > 0) run.setRejectedCount((run.getRejectedCount() != null ? run.getRejectedCount() : 0) + rejectedDelta);
                if (postponedDelta > 0) run.setPostponedCount((run.getPostponedCount() != null ? run.getPostponedCount() : 0) + postponedDelta);
                if (outreachDelta > 0) run.setOutreachDraftsGeneratedCount((run.getOutreachDraftsGeneratedCount() != null ? run.getOutreachDraftsGeneratedCount() : 0) + outreachDelta);
                dataManager.commit(run);
            }
        } catch (Exception e) {
            log.warn("Failed to update match run stats: {}", e.getMessage());
        }
    }
}
