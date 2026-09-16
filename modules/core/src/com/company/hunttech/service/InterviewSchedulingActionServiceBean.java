package com.company.hunttech.service;

import com.company.hunttech.core.OpenPositionService;
import com.company.hunttech.dto.action.InterviewSchedulingIntent;
import com.company.hunttech.dto.action.InterviewSchedulingResult;
import com.company.hunttech.dto.action.PendingInterviewState;
import com.company.hunttech.dto.yandex.YandexCalendarInfoDto;
import com.company.hunttech.dto.yandex.YandexMeetingRequest;
import com.company.hunttech.dto.yandex.YandexMeetingResult;
import com.company.hunttech.entity.*;
import com.company.hunttech.entity.ai.LlmChatPendingAction;
import com.company.hunttech.service.AiExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(InterviewSchedulingActionService.NAME)
public class InterviewSchedulingActionServiceBean implements InterviewSchedulingActionService {

    private static final Logger log = LoggerFactory.getLogger(InterviewSchedulingActionServiceBean.class);

    private static final String FUNCTION_INTERVIEW_SCHEDULING_PARSE = "INTERVIEW_SCHEDULING_PARSE";
    private static final String ACTION_TYPE_INTERVIEW_SCHEDULING = "INTERVIEW_SCHEDULING";

    private static final Pattern INTERVIEW_INTENT_PATTERN = Pattern.compile(
            "(?iu)(?:\\b(?:назначь|запланируй|проведи|поставь|организуй|забронируй|сделай|добавь)\\b.*?(?:собеседован|интервью|созвон|встреч)|" +
            "\\b(?:собеседован|интервью|созвон|встреч[уа])\\b.*?(?:для|с|кандидат|на\\s+ваканси|в\\s+проект))"
    );

    private static final Pattern CANCEL_PATTERN = Pattern.compile(
            "(?iu)^(?:отмена|отмени|отменить|стоп|не\\s+надо|забудь|отбой|назад)$"
    );

    private static final Pattern OPTION_NUMBER_PATTERN = Pattern.compile(
            "(?iu)^\\s*(?:№|номер|вариант|пункт)?\\s*(\\d+)\\s*(?:-?[ыийео]?|й|го)?\\s*$"
    );

    private static final Pattern CONFIRMATION_PATTERN = Pattern.compile(
            "(?iu)^\\s*(?:да|подтверждаю|верно|согласен|давай|ок|хорошо|всё\\s+верно|да,\\s+верно|создавай)\\s*$"
    );

    private static final Pattern HOUR_MINUTE_PATTERN = Pattern.compile(
            "(?:в|на)?\\s*(\\d{1,2})[:\\.\\-](\\d{2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern HOUR_ONLY_PATTERN = Pattern.compile(
            "(?:в|на)\\s+(\\d{1,2})(?:\\s*(?:час(?:а|ов)?|ч|:00))?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern PATTERN_CANDIDATE = Pattern.compile(
            "(?iu)(?:для|с\\s+кандидатом|кандидат[а-я]*|с)\\s+([А-ЯЁ][а-яё]+(?:\\s+[А-ЯЁ][а-яё]+)?)(?=\\s+(?:на|в|по|к|до|с|сегодня|завтра|$))"
    );

    private static final Pattern PATTERN_CANDIDATE_FALLBACK = Pattern.compile(
            "(?iu)(?:для|с\\s+кандидатом|кандидат[а-я]*|с)\\s+([А-ЯЁ][а-яё]+(?:\\s+[А-ЯЁ][а-яё]+)?)"
    );

    private static final Pattern PATTERN_VACANCY = Pattern.compile(
            "(?iu)(?:на\\s+ваканси[юеи]|на\\s+позици[юеи]|должност[ьи])\\s+([^,\\n]+?)(?=\\s+(?:в|на|с|по|к|до|$))"
    );

    private static final Pattern PATTERN_PROJECT = Pattern.compile(
            "(?iu)(?:в\\s+проект[е]?|по\\s+проекту|проект[а]?)\\s+([^,\\n]+?)(?=\\s+(?:на|в|с|по|к|$))"
    );

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private Persistence persistence;
    @Inject
    private Security security;
    @Inject
    private AiExecutionService aiExecutionService;
    @Inject
    private YandexIntegrationService yandexIntegrationService;
    @Inject
    private OpenPositionService openPositionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean isInterviewSchedulingIntent(String userMessage) {
        if (StringUtils.isBlank(userMessage)) {
            return false;
        }
        return INTERVIEW_INTENT_PATTERN.matcher(userMessage.trim()).find();
    }

    @Override
    public InterviewSchedulingIntent parseSchedulingIntent(String userMessage, ExtUser currentUser) {
        InterviewSchedulingIntent intent = new InterviewSchedulingIntent();
        intent.setRawMessage(userMessage);

        if (StringUtils.isBlank(userMessage)) {
            intent.setIntentDetected(false);
            return intent;
        }

        // Быстрый Regex pre-check
        boolean regexMatch = isInterviewSchedulingIntent(userMessage);
        intent.setIntentDetected(regexMatch);

        // Попытка структурирования через AI Function
        try {
            Map<String, Object> ctx = Collections.singletonMap("sourceText", userMessage.trim());
            AiExecutionResult aiResult = aiExecutionService.executeText(FUNCTION_INTERVIEW_SCHEDULING_PARSE, ctx);
            if (aiResult != null && StringUtils.isNotBlank(aiResult.getText())) {
                String cleanJson = extractCleanJson(aiResult.getText());
                JsonNode json = objectMapper.readTree(cleanJson);
                if (json.isArray() && json.size() > 0) {
                    json = json.get(0);
                }

                if (json.hasNonNull("candidateQuery")) {
                    intent.setCandidateQuery(json.get("candidateQuery").asText().trim());
                }
                if (json.hasNonNull("vacancyQuery")) {
                    intent.setVacancyQuery(json.get("vacancyQuery").asText().trim());
                }
                if (json.hasNonNull("projectQuery")) {
                    intent.setProjectQuery(json.get("projectQuery").asText().trim());
                }
                if (json.hasNonNull("projectOwnerQuery")) {
                    intent.setProjectOwnerQuery(json.get("projectOwnerQuery").asText().trim());
                }
                if (json.hasNonNull("interactionTypeQuery")) {
                    intent.setInteractionTypeQuery(json.get("interactionTypeQuery").asText().trim());
                }
                if (json.hasNonNull("rawDateTime")) {
                    intent.setRawDateTime(json.get("rawDateTime").asText().trim());
                }
                if (json.hasNonNull("timeZone")) {
                    intent.setTimeZone(json.get("timeZone").asText().trim());
                }
                if (json.hasNonNull("online")) {
                    intent.setOnline(json.get("online").asBoolean(true));
                }
                if (json.hasNonNull("intentDetected")) {
                    intent.setIntentDetected(json.get("intentDetected").asBoolean(regexMatch));
                }
            }
        } catch (Exception ex) {
            log.warn("AI парсинг интента назначения интервью завершился с ошибкой, fallback на эвристику: {}", ex.getMessage());
        }

        // Эвристический fallback при отсутствии AI данных
        applyHeuristicFallback(userMessage, intent);

        // Парсинг целевой даты и времени
        Date parsedDate = resolveDateTime(intent.getRawDateTime() != null ? intent.getRawDateTime() : userMessage, intent.getTimeZone());
        intent.setTargetDateTime(parsedDate);

        return intent;
    }

    private void applyHeuristicFallback(String msg, InterviewSchedulingIntent intent) {
        String lower = msg.toLowerCase(Locale.ROOT);

        // Кандидат: "для Эльчина Аббасова", "с Эльчином Аббасовым"
        if (StringUtils.isBlank(intent.getCandidateQuery())) {
            Matcher m = PATTERN_CANDIDATE.matcher(msg);
            if (m.find()) {
                intent.setCandidateQuery(m.group(1).trim());
            } else {
                Matcher mFallback = PATTERN_CANDIDATE_FALLBACK.matcher(msg);
                if (mFallback.find()) {
                    intent.setCandidateQuery(mFallback.group(1).trim());
                }
            }
        }

        // Вакансия: "на вакансию Системного аналитика"
        if (StringUtils.isBlank(intent.getVacancyQuery())) {
            Matcher vm = PATTERN_VACANCY.matcher(msg);
            if (vm.find()) {
                intent.setVacancyQuery(vm.group(1).trim());
            }
        }

        // Проект по владельцу или названию: "в проект Дениса Гаркушина", "в проект ВТБ"
        if (StringUtils.isBlank(intent.getProjectQuery()) && StringUtils.isBlank(intent.getProjectOwnerQuery())) {
            Matcher pm = PATTERN_PROJECT.matcher(msg);
            if (pm.find()) {
                String pRaw = pm.group(1).trim();
                // Проверяем, не ФИО ли это владельца проекта (2 слова с заглавной)
                if (pRaw.matches("^[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+$")) {
                    intent.setProjectOwnerQuery(normalizeRussianFio(pRaw));
                } else {
                    intent.setProjectQuery(pRaw);
                }
            }
        }

        if (lower.contains("москв") || lower.contains("мск")) {
            intent.setTimeZone("Europe/Moscow");
        } else if (lower.contains("самар") || lower.contains("саратов")) {
            intent.setTimeZone("Europe/Saratov");
        }
    }

    @Override
    public InterviewSchedulingResult handleSchedulingAction(UUID conversationId, String userMessage, ExtUser currentUser, String requestId) {
        if (StringUtils.isBlank(userMessage) || currentUser == null || conversationId == null) {
            return null;
        }

        String trimmed = userMessage.trim();

        // 1. Проверяем, есть ли активный незавершенный Pending Action пользователя в данном диалоге
        LlmChatPendingAction pendingAction = loadActivePendingAction(conversationId, currentUser.getId());
        if (pendingAction != null) {
            // Обработка отмены
            if (CANCEL_PATTERN.matcher(trimmed).matches()) {
                cancelPendingAction(conversationId, currentUser.getId());
                return InterviewSchedulingResult.success("❌ Назначение собеседования отменено. Вы можете продолжить общение или задать новый вопрос.", null, null, null, null);
            }

            // Продолжение диалога с существующим состоянием
            PendingInterviewState state = deserializeState(pendingAction.getStateData());
            if (state != null) {
                return continuePendingAction(pendingAction, state, trimmed, currentUser);
            }
        }

        // 2. Если Pending Action нет, проверяем, является ли сообщение новым запросом на назначение собеседования
        if (!isInterviewSchedulingIntent(trimmed)) {
            return null;
        }

        // 3. Проверка прав доступа в CUBA Security
        if (!security.isEntityOpPermitted(IteractionList.class, EntityOp.CREATE)) {
            return InterviewSchedulingResult.error("⛔ **Ограничение доступа:** У вашей учётной записи нет прав на создание взаимодействий (`IteractionList`). Назначение собеседования заблокировано политикой безопасности CUBA Platform.");
        }

        // 4. Парсим интент
        InterviewSchedulingIntent intent = parseSchedulingIntent(trimmed, currentUser);
        if (!intent.isIntentDetected()) {
            return null;
        }

        PendingInterviewState state = new PendingInterviewState();
        state.setRawQuery(trimmed);
        state.setTimeZone(intent.getTimeZone());
        state.setTargetDateTime(intent.getTargetDateTime());
        state.setOnline(intent.isOnline());

        // 5. Пошаговый резолвинг
        return processSchedulingWorkflow(conversationId, currentUser, intent, state, requestId);
    }

    private InterviewSchedulingResult processSchedulingWorkflow(UUID conversationId, ExtUser currentUser, InterviewSchedulingIntent intent, PendingInterviewState state, String requestId) {
        // 1. Резолвинг кандидата
        if (state.getCandidateId() == null) {
            String candQuery = intent != null ? intent.getCandidateQuery() : null;
            List<JobCandidate> candidates = searchCandidates(candQuery);
            if (candidates.isEmpty()) {
                state.setStep("NEED_CANDIDATE");
                savePendingAction(conversationId, currentUser.getId(), state, requestId);
                return InterviewSchedulingResult.pending("NEED_CANDIDATE",
                        "⚠️ Кандидат" + (StringUtils.isNotBlank(candQuery) ? " по запросу «" + candQuery + "»" : "") +
                        " не найден в базе HRM. Пожалуйста, уточните ФИО кандидата.");
            } else if (candidates.size() > 1) {
                List<UUID> ids = new ArrayList<>();
                StringBuilder sb = new StringBuilder("Найдено несколько кандидатов. Выберите номер нужного кандидата (или введите ФИО):\n\n");
                int idx = 1;
                for (JobCandidate c : candidates) {
                    ids.add(c.getId());
                    String pos = (c.getPersonPosition() != null && c.getPersonPosition().getPositionRuName() != null)
                            ? c.getPersonPosition().getPositionRuName() : "специализация не указана";
                    String phone = c.getMobilePhone() != null ? c.getMobilePhone() : (c.getPhone() != null ? c.getPhone() : "-");
                    sb.append(idx++).append(". **[").append(c.getFullName()).append("](hrm://candidate/").append(c.getId())
                      .append(")** (").append(pos).append(", тел: ").append(phone).append(")\n");
                }
                state.setStep("NEED_CANDIDATE");
                state.setCandidateOptions(ids);
                savePendingAction(conversationId, currentUser.getId(), state, requestId);
                return InterviewSchedulingResult.pending("NEED_CANDIDATE", sb.toString().trim());
            } else {
                JobCandidate found = candidates.get(0);
                state.setCandidateId(found.getId());
                state.setCandidateFio(found.getFullName());
                state.setCandidateEmail(found.getEmail());
            }
        }

        // 2. Резолвинг вакансии (ТОЛЬКО ОТКРЫТЫЕ: openClose != true)
        if (state.getVacancyId() == null) {
            String vacQuery = intent != null ? intent.getVacancyQuery() : null;
            String projQuery = intent != null ? intent.getProjectQuery() : null;
            String ownerQuery = intent != null ? intent.getProjectOwnerQuery() : null;

            List<OpenPosition> openPositions = searchOpenPositions(vacQuery, projQuery, ownerQuery);
            if (openPositions.isEmpty()) {
                // Если не найдено по строгому фильтру, пробуем найти все открытые вакансии кандидата или общие открытые
                List<OpenPosition> allOpen = dataManager.load(OpenPosition.class)
                        .query("select e from hunttech_OpenPosition e where (e.openClose is null or e.openClose = false) order by e.priority desc, e.createTs desc")
                        .view("openPosition-iteraction-list-picker-view")
                        .maxResults(5)
                        .list();
                if (allOpen.isEmpty()) {
                    state.setStep("NEED_VACANCY");
                    savePendingAction(conversationId, currentUser.getId(), state, requestId);
                    return InterviewSchedulingResult.pending("NEED_VACANCY",
                            "⚠️ Не найдено ни одной открытой вакансии для назначения собеседования. Уточните название позиции.");
                }
                StringBuilder sb = new StringBuilder("Не удалось однозначно определить вакансию. Выберите номер открытой вакансии из списка:\n\n");
                List<UUID> vacIds = new ArrayList<>();
                int idx = 1;
                for (OpenPosition op : allOpen) {
                    vacIds.add(op.getId());
                    String pName = op.getProjectName() != null ? op.getProjectName().getProjectName() : "Без проекта";
                    sb.append(idx++).append(". **[").append(op.getVacansyName()).append("](hrm://vacancy/").append(op.getId())
                      .append(")** (Проект: ").append(pName).append(")\n");
                }
                state.setStep("NEED_VACANCY");
                state.setVacancyOptions(vacIds);
                savePendingAction(conversationId, currentUser.getId(), state, requestId);
                return InterviewSchedulingResult.pending("NEED_VACANCY", sb.toString().trim());
            } else if (openPositions.size() > 1) {
                List<UUID> vacIds = new ArrayList<>();
                StringBuilder sb = new StringBuilder("Найдено несколько подходящих открытых вакансий. Укажите номер нужной:\n\n");
                int idx = 1;
                for (OpenPosition op : openPositions) {
                    vacIds.add(op.getId());
                    String pName = op.getProjectName() != null ? op.getProjectName().getProjectName() : "Без проекта";
                    String pOwner = (op.getProjectName() != null && op.getProjectName().getProjectOwner() != null)
                            ? ("Руководитель: " + op.getProjectName().getProjectOwner().getSecondName() + " " + op.getProjectName().getProjectOwner().getFirstName()) : "";
                    sb.append(idx++).append(". **[").append(op.getVacansyName()).append("](hrm://vacancy/").append(op.getId())
                      .append(")** — проект **").append(pName).append("** ").append(pOwner).append("\n");
                }
                state.setStep("NEED_VACANCY");
                state.setVacancyOptions(vacIds);
                savePendingAction(conversationId, currentUser.getId(), state, requestId);
                return InterviewSchedulingResult.pending("NEED_VACANCY", sb.toString().trim());
            } else {
                OpenPosition op = openPositions.get(0);
                state.setVacancyId(op.getId());
                state.setVacancyName(op.getVacansyName());
                state.setProjectName(op.getProjectName() != null ? op.getProjectName().getProjectName() : null);
            }
        }

        // 3. Резолвинг типа взаимодействия (Iteraction)
        if (state.getInteractionTypeId() == null) {
            String itQuery = intent != null ? intent.getInteractionTypeQuery() : null;
            Iteraction itType = resolveInteractionType(itQuery, state.getProjectName());
            if (itType != null) {
                state.setInteractionTypeId(itType.getId());
                state.setInteractionTypeName(itType.getIterationName());
            } else {
                // Выбор из базовых типов собеседований
                List<Iteraction> types = loadInterviewInteractionTypes();
                if (types.size() > 1) {
                    List<UUID> tIds = new ArrayList<>();
                    StringBuilder sb = new StringBuilder("Уточните тип собеседования (выберите номер):\n\n");
                    int idx = 1;
                    for (Iteraction t : types) {
                        tIds.add(t.getId());
                        sb.append(idx++).append(". ").append(t.getIterationName()).append("\n");
                    }
                    state.setStep("NEED_INTERACTION_TYPE");
                    state.setInteractionTypeOptions(tIds);
                    savePendingAction(conversationId, currentUser.getId(), state, requestId);
                    return InterviewSchedulingResult.pending("NEED_INTERACTION_TYPE", sb.toString().trim());
                } else if (!types.isEmpty()) {
                    state.setInteractionTypeId(types.get(0).getId());
                    state.setInteractionTypeName(types.get(0).getIterationName());
                } else {
                    return InterviewSchedulingResult.error("⚠️ В справочнике системы не найдено ни одного типа взаимодействия для собеседований (с флагом Календарь или Собеседование). Обратитесь к администратору системы.");
                }
            }
        }

        // 4. Проверка даты и времени
        if (state.getTargetDateTime() == null) {
            state.setStep("CONFIRM_SCHEDULE");
            savePendingAction(conversationId, currentUser.getId(), state, requestId);
            return InterviewSchedulingResult.pending("CONFIRM_SCHEDULE",
                    "⚠️ Дата и время собеседования не указаны. Пожалуйста, напишите день и время (например «завтра в 17:00»).");
        }

        // 5. Все параметры успешно определены -> выполняем создание взаимодействия и бронирование в Yandex 360
        return executeFinalBooking(conversationId, currentUser, state);
    }

    private InterviewSchedulingResult continuePendingAction(LlmChatPendingAction pendingAction, PendingInterviewState state, String input, ExtUser currentUser) {
        String step = state.getStep();

        if ("NEED_CANDIDATE".equals(step)) {
            // Пользователь ввел число или новое ФИО
            Matcher numMatcher = OPTION_NUMBER_PATTERN.matcher(input);
            if (numMatcher.matches() && state.getCandidateOptions() != null) {
                try {
                    int optIdx = Integer.parseInt(numMatcher.group(1)) - 1;
                    if (optIdx >= 0 && optIdx < state.getCandidateOptions().size()) {
                        UUID candId = state.getCandidateOptions().get(optIdx);
                        JobCandidate c = dataManager.load(JobCandidate.class).id(candId).view("jobCandidate-view").one();
                        state.setCandidateId(c.getId());
                        state.setCandidateFio(c.getFullName());
                        state.setCandidateEmail(c.getEmail());
                        state.setCandidateOptions(null);
                        state.setStep("NEXT");
                        return processSchedulingWorkflow(pendingAction.getConversationId(), currentUser, null, state, pendingAction.getRequestId());
                    }
                } catch (NumberFormatException ignored) {}
            }
            // Если введено новое имя
            List<JobCandidate> candidates = searchCandidates(input);
            if (candidates.size() == 1) {
                JobCandidate c = candidates.get(0);
                state.setCandidateId(c.getId());
                state.setCandidateFio(c.getFullName());
                state.setCandidateEmail(c.getEmail());
                state.setCandidateOptions(null);
                state.setStep("NEXT");
                return processSchedulingWorkflow(pendingAction.getConversationId(), currentUser, null, state, pendingAction.getRequestId());
            } else {
                return InterviewSchedulingResult.pending("NEED_CANDIDATE", "Пожалуйста, введите номер кандидата из предложенного выше списка или точное ФИО.");
            }
        } else if ("NEED_VACANCY".equals(step)) {
            Matcher numMatcher = OPTION_NUMBER_PATTERN.matcher(input);
            if (numMatcher.matches() && state.getVacancyOptions() != null) {
                try {
                    int optIdx = Integer.parseInt(numMatcher.group(1)) - 1;
                    if (optIdx >= 0 && optIdx < state.getVacancyOptions().size()) {
                        UUID vacId = state.getVacancyOptions().get(optIdx);
                        OpenPosition op = dataManager.load(OpenPosition.class).id(vacId).view("openPosition-iteraction-list-picker-view").one();
                        state.setVacancyId(op.getId());
                        state.setVacancyName(op.getVacansyName());
                        state.setProjectName(op.getProjectName() != null ? op.getProjectName().getProjectName() : null);
                        state.setVacancyOptions(null);
                        state.setStep("NEXT");
                        return processSchedulingWorkflow(pendingAction.getConversationId(), currentUser, null, state, pendingAction.getRequestId());
                    }
                } catch (NumberFormatException ignored) {}
            }
            List<OpenPosition> searched = searchOpenPositions(input, null, null);
            if (searched.size() == 1) {
                OpenPosition op = searched.get(0);
                state.setVacancyId(op.getId());
                state.setVacancyName(op.getVacansyName());
                state.setProjectName(op.getProjectName() != null ? op.getProjectName().getProjectName() : null);
                state.setVacancyOptions(null);
                state.setStep("NEXT");
                return processSchedulingWorkflow(pendingAction.getConversationId(), currentUser, null, state, pendingAction.getRequestId());
            } else {
                return InterviewSchedulingResult.pending("NEED_VACANCY", "Пожалуйста, укажите номер вакансии из списка или точное название.");
            }
        } else if ("NEED_INTERACTION_TYPE".equals(step)) {
            Matcher numMatcher = OPTION_NUMBER_PATTERN.matcher(input);
            if (numMatcher.matches() && state.getInteractionTypeOptions() != null) {
                try {
                    int optIdx = Integer.parseInt(numMatcher.group(1)) - 1;
                    if (optIdx >= 0 && optIdx < state.getInteractionTypeOptions().size()) {
                        UUID tId = state.getInteractionTypeOptions().get(optIdx);
                        Iteraction it = dataManager.load(Iteraction.class).id(tId).view("iteraction-list-type-view").one();
                        state.setInteractionTypeId(it.getId());
                        state.setInteractionTypeName(it.getIterationName());
                        state.setInteractionTypeOptions(null);
                        state.setStep("NEXT");
                        return processSchedulingWorkflow(pendingAction.getConversationId(), currentUser, null, state, pendingAction.getRequestId());
                    }
                } catch (NumberFormatException ignored) {}
            }
            return InterviewSchedulingResult.pending("NEED_INTERACTION_TYPE", "Пожалуйста, укажите номер типа собеседования из списка.");
        } else if ("CONFIRM_SCHEDULE".equals(step)) {
            // Пользователь передал дату/время
            Date dt = resolveDateTime(input, state.getTimeZone());
            if (dt != null) {
                state.setTargetDateTime(dt);
                return executeFinalBooking(pendingAction.getConversationId(), currentUser, state);
            }
            if (CONFIRMATION_PATTERN.matcher(input).matches() && state.getTargetDateTime() != null) {
                return executeFinalBooking(pendingAction.getConversationId(), currentUser, state);
            }
            return InterviewSchedulingResult.pending("CONFIRM_SCHEDULE", "Укажите корректную дату и время (например «завтра в 17:00»).");
        }

        return null;
    }

    private InterviewSchedulingResult executeFinalBooking(UUID conversationId, ExtUser currentUser, PendingInterviewState state) {
        // Удаляем / завершаем Pending Action
        cancelPendingAction(conversationId, currentUser.getId());

        JobCandidate candidate = dataManager.load(JobCandidate.class)
                .id(state.getCandidateId())
                .view("jobCandidate-view")
                .one();

        OpenPosition vacancy = dataManager.load(OpenPosition.class)
                .id(state.getVacancyId())
                .view("openPosition-iteraction-list-picker-view")
                .one();

        Iteraction interactionType = dataManager.load(Iteraction.class)
                .id(state.getInteractionTypeId())
                .view("iteraction-list-type-view")
                .one();

        Date appointmentDate = state.getTargetDateTime() != null ? state.getTargetDateTime() : new Date();

        // 1. Создание IteractionList по domain lifecycle
        IteractionList newInteraction = metadata.create(IteractionList.class);
        newInteraction.setCandidate(candidate);
        newInteraction.setVacancy(vacancy);
        newInteraction.setIteractionType(interactionType);
        newInteraction.setRecrutier(currentUser);
        newInteraction.setDateIteraction(new Date());
        newInteraction.setAddDate(appointmentDate);
        newInteraction.setCommunicationMethod("Яндекс Телемост");
        newInteraction.setCurrentPriority(vacancy.getPriority());
        newInteraction.setCurrentOpenClose(vacancy.getOpenClose());

        // Расчет номера взаимодействия
        BigDecimal nextNumber = getNextInteractionNumber();
        newInteraction.setNumberIteraction(nextNumber);

        // Расчет chainInteraction
        IteractionList lastInteraction = findLastInteraction(candidate, vacancy);
        newInteraction.setChainInteraction(lastInteraction);

        // Формирование комментария
        StringBuilder comment = new StringBuilder();
        comment.append("Назначено собеседование из LLM-чата HRM.\n")
                .append("Вакансия: ").append(vacancy.getVacansyName()).append("\n")
                .append("Проект: ").append(state.getProjectName() != null ? state.getProjectName() : "—").append("\n");

        newInteraction.setComment(comment.toString());
        newInteraction.setAddToCalendar(true);

        // Выбираем целевой календарь
        List<YandexCalendarInfoDto> calendars = yandexIntegrationService.getAvailableCalendars(currentUser.getId());
        String targetCalendarPath = null;
        String targetCalendarName = "Яндекс Календарь";
        for (YandexCalendarInfoDto cal : calendars) {
            if (cal.isDefault()) {
                targetCalendarPath = cal.getPath();
                targetCalendarName = cal.getDisplayName();
                break;
            }
        }
        if (targetCalendarPath == null && !calendars.isEmpty()) {
            targetCalendarPath = calendars.get(0).getPath();
            targetCalendarName = calendars.get(0).getDisplayName();
        }
        newInteraction.setCalendarId(targetCalendarPath);

        // Сохраняем сущность перед вызовом CalDAV синхронизации
        newInteraction = dataManager.commit(newInteraction);

        // 2. Создание события в CalDAV и ссылки Телемост через YandexIntegrationService
        YandexMeetingResult bookingResult = null;
        try {
            bookingResult = yandexIntegrationService.syncInteractionCalendarEvent(
                    currentUser.getId(),
                    newInteraction.getId(),
                    true,
                    targetCalendarPath,
                    state.getTimeZone()
            );
        } catch (Exception ex) {
            log.error("Сбой синхронизации с Яндекс 360: {}", ex.getMessage(), ex);
        }

        // Перечитываем актуальные данные из сохраненного взаимодействия
        IteractionList savedInteraction = dataManager.load(IteractionList.class)
                .id(newInteraction.getId())
                .view("iteractionList-edit-view")
                .one();

        // 3. Создание новости по вакансии
        try {
            openPositionService.setOpenPositionNewsAutomatedMessage(
                    vacancy,
                    interactionType.getIterationName(),
                    savedInteraction.getComment(),
                    savedInteraction.getDateIteraction(),
                    candidate,
                    currentUser,
                    interactionType.getSignPriorityNews()
            );
        } catch (Exception ex) {
            log.warn("Не удалось создать OpenPositionNews: {}", ex.getMessage());
        }

        // 4. Формирование ответа пользователю с красивой разметкой
        SimpleDateFormat df = new SimpleDateFormat("EEEE, d MMMM yyyy г. в HH:mm", new Locale("ru", "RU"));
        df.setTimeZone(TimeZone.getTimeZone(state.getTimeZone()));
        String formattedDate = capitalize(df.format(appointmentDate));

        StringBuilder rep = new StringBuilder();
        rep.append("✅ **Собеседование успешно назначено!**\n\n");
        rep.append("- **Кандидат:** [").append(candidate.getFullName()).append("](hrm://candidate/").append(candidate.getId()).append(")");
        if (StringUtils.isNotBlank(candidate.getEmail())) {
            rep.append(" (").append(candidate.getEmail()).append(")");
        }
        rep.append("\n");

        rep.append("- **Вакансия:** [").append(vacancy.getVacansyName()).append("](hrm://vacancy/").append(vacancy.getId()).append(")\n");
        if (StringUtils.isNotBlank(state.getProjectName())) {
            rep.append("- **Проект:** ").append(state.getProjectName()).append("\n");
        }
        rep.append("- **Тип:** ").append(interactionType.getIterationName()).append("\n");
        rep.append("- **Дата и время:** 🗓️ **").append(formattedDate).append("** (").append(state.getTimeZone()).append(")\n");
        rep.append("- **Календарь:** ").append(targetCalendarName).append("\n");

        String telemostUrl = null;
        if (bookingResult != null && StringUtils.isNotBlank(bookingResult.getTelemostJoinUrl())) {
            telemostUrl = bookingResult.getTelemostJoinUrl();
            rep.append("- **Яндекс Телемост:** [📹 Присоединиться к видеовстрече](").append(telemostUrl).append(")\n");
        }

        if (StringUtils.isNotBlank(candidate.getEmail())) {
            rep.append("- **Приглашение:** Автоматически сформировано для кандидата (").append(candidate.getEmail()).append(").\n");
        } else {
            rep.append("- ⚠️ **Внимание:** У кандидата не указан Email в профиле HRM; приглашение в календарь не отправлено.\n");
        }

        rep.append("\nСоздано взаимодействие **IteractionList №").append(savedInteraction.getNumberIteraction()).append("**.");

        return InterviewSchedulingResult.success(
                rep.toString().trim(),
                savedInteraction.getId(),
                telemostUrl,
                savedInteraction.getCalendarEventId(),
                targetCalendarName
        );
    }

    private synchronized BigDecimal getNextInteractionNumber() {
        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();
            Number max = (Number) em.createQuery("select max(e.numberIteraction) from hunttech_IteractionList e").getSingleResult();
            if (max == null) {
                return BigDecimal.ONE;
            }
            return new BigDecimal(max.longValue() + 1);
        } catch (Exception ex) {
            log.error("Не удалось вычислить следующий номер взаимодействия IteractionList: {}", ex.getMessage(), ex);
            return BigDecimal.ONE;
        }
    }

    private IteractionList findLastInteraction(JobCandidate candidate, OpenPosition vacancy) {
        if (candidate == null || vacancy == null) {
            return null;
        }
        return dataManager.load(IteractionList.class)
                .query("select e from hunttech_IteractionList e " +
                        "where e.vacancy = :vacancy and e.candidate = :candidate and e.iteractionType is not null " +
                        "order by e.dateIteraction desc")
                .parameter("vacancy", vacancy)
                .parameter("candidate", candidate)
                .view("_minimal")
                .maxResults(1)
                .optional()
                .orElse(null);
    }

    private List<JobCandidate> searchCandidates(String query) {
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }
        String clean = query.replaceAll("[^a-zA-Zа-яА-ЯёЁ\\s]", " ").replaceAll("\\s+", " ").trim();
        String[] tokens = clean.split("\\s+");
        if (tokens.length == 0) {
            return Collections.emptyList();
        }

        if (tokens.length == 1) {
            String stem = stemRussian(tokens[0]);
            return dataManager.load(JobCandidate.class)
                    .query("select e from hunttech_JobCandidate e where " +
                            "(lower(e.secondName) like :stem or lower(e.firstName) like :stem or lower(e.fullName) like :stem) " +
                            "order by e.createTs desc")
                    .parameter("stem", "%" + stem.toLowerCase(Locale.ROOT) + "%")
                    .view("jobCandidate-view")
                    .maxResults(10)
                    .list();
        } else {
            String w1 = stemRussian(tokens[0]).toLowerCase(Locale.ROOT);
            String w2 = stemRussian(tokens[1]).toLowerCase(Locale.ROOT);
            String full = clean.toLowerCase(Locale.ROOT);

            return dataManager.load(JobCandidate.class)
                    .query("select e from hunttech_JobCandidate e where " +
                            "((lower(e.secondName) like :w1 and lower(e.firstName) like :w2) or " +
                            "(lower(e.secondName) like :w2 and lower(e.firstName) like :w1) or " +
                            "lower(e.fullName) like :full) " +
                            "order by e.createTs desc")
                    .parameter("w1", "%" + w1 + "%")
                    .parameter("w2", "%" + w2 + "%")
                    .parameter("full", "%" + full + "%")
                    .view("jobCandidate-view")
                    .maxResults(10)
                    .list();
        }
    }

    private List<OpenPosition> searchOpenPositions(String vacancyQuery, String projectQuery, String projectOwnerQuery) {
        StringBuilder q = new StringBuilder("select e from hunttech_OpenPosition e where (e.openClose is null or e.openClose = false) ");
        Map<String, Object> params = new HashMap<>();

        if (StringUtils.isNotBlank(vacancyQuery)) {
            String vStem = stemRussian(vacancyQuery.trim().split("\\s+")[0]).toLowerCase(Locale.ROOT);
            q.append("and lower(e.vacansyName) like :vStem ");
            params.put("vStem", "%" + vStem + "%");
        }

        if (StringUtils.isNotBlank(projectQuery)) {
            q.append("and lower(e.projectName.projectName) like :pQuery ");
            params.put("pQuery", "%" + projectQuery.trim().toLowerCase(Locale.ROOT) + "%");
        }

        if (StringUtils.isNotBlank(projectOwnerQuery)) {
            String oStem = stemRussian(projectOwnerQuery.trim().split("\\s+")[0]).toLowerCase(Locale.ROOT);
            q.append("and (lower(e.projectName.projectOwner.secondName) like :oStem or lower(e.projectName.projectOwner.firstName) like :oStem) ");
            params.put("oStem", "%" + oStem + "%");
        }

        q.append("order by e.priority desc, e.createTs desc");

        FluentLoader.ByQuery<OpenPosition, UUID> loader = dataManager.load(OpenPosition.class).query(q.toString()).view("openPosition-iteraction-list-picker-view").maxResults(10);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            loader.parameter(entry.getKey(), entry.getValue());
        }
        return loader.list();
    }

    private Iteraction resolveInteractionType(String userQuery, String projectName) {
        if (StringUtils.isNotBlank(userQuery)) {
            String clean = userQuery.toLowerCase(Locale.ROOT);
            List<Iteraction> list = dataManager.load(Iteraction.class)
                    .query("select e from hunttech_Iteraction e where lower(e.iterationName) like :q order by e.number asc")
                    .parameter("q", "%" + clean + "%")
                    .view("iteraction-list-type-view")
                    .list();
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }

        // По умолчанию ищем специализированные типы по флагам метаданных:
        // Если проект внешний / заказчик
        if (StringUtils.isNotBlank(projectName)) {
            Iteraction clientInterview = dataManager.load(Iteraction.class)
                    .query("select e from hunttech_Iteraction e where e.signClientInterview = true or lower(e.iterationName) like '%заказчик%'")
                    .view("iteraction-list-type-view")
                    .maxResults(1)
                    .optional()
                    .orElse(null);
            if (clientInterview != null) {
                return clientInterview;
            }
        }

        // Стандартное интервью
        return dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e where e.signOurInterviewAssigned = true or " +
                        "(e.calendarItem = true and (lower(e.iterationName) like '%собеседован%' or lower(e.iterationName) like '%интервью%')) " +
                        "order by e.number asc")
                .view("iteraction-list-type-view")
                .maxResults(1)
                .optional()
                .orElse(null);
    }

    private List<Iteraction> loadInterviewInteractionTypes() {
        return dataManager.load(Iteraction.class)
                .query("select e from hunttech_Iteraction e where e.calendarItem = true or " +
                        "lower(e.iterationName) like '%собеседован%' or lower(e.iterationName) like '%интервью%' " +
                        "order by e.number asc")
                .view("iteraction-list-type-view")
                .maxResults(5)
                .list();
    }

    private Date resolveDateTime(String text, String timeZoneStr) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        TimeZone tz = TimeZone.getTimeZone(StringUtils.defaultIfBlank(timeZoneStr, "Europe/Moscow"));
        Calendar cal = Calendar.getInstance(tz);

        String lower = text.toLowerCase(Locale.ROOT);
        boolean dayExplicitlySpecified = lower.contains("послезавтра") || lower.contains("завтра") || lower.contains("сегодня");

        boolean timeFound = false;
        int hour = 17;
        int minute = 0;

        Matcher hmMatcher = HOUR_MINUTE_PATTERN.matcher(text);
        if (hmMatcher.find()) {
            hour = Integer.parseInt(hmMatcher.group(1));
            minute = Integer.parseInt(hmMatcher.group(2));
            timeFound = true;
        } else {
            Matcher hMatcher = HOUR_ONLY_PATTERN.matcher(text);
            if (hMatcher.find()) {
                hour = Integer.parseInt(hMatcher.group(1));
                minute = 0;
                timeFound = true;
            }
        }

        if (!dayExplicitlySpecified && !timeFound) {
            return null;
        }

        if (lower.contains("послезавтра")) {
            cal.add(Calendar.DAY_OF_YEAR, 2);
        } else if (lower.contains("завтра")) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        } else if (lower.contains("сегодня")) {
            // сегодня
        }

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Если день явно не указан (например только «в 15:00») и указанное время сегодня уже прошло, переносим на завтра
        if (!dayExplicitlySpecified && cal.getTime().before(new Date())) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return cal.getTime();
    }

    private String stemRussian(String word) {
        if (word == null || word.length() <= 3) {
            return word != null ? word : "";
        }
        String w = word.toLowerCase(Locale.ROOT);
        if (w.endsWith("ова") || w.endsWith("ева") || w.endsWith("ина")) {
            return w.substring(0, w.length() - 1);
        }
        if (w.endsWith("ом") || w.endsWith("ем") || w.endsWith("ой") || w.endsWith("ей")) {
            return w.substring(0, w.length() - 2);
        }
        if (w.endsWith("а") || w.endsWith("я") || w.endsWith("у") || w.endsWith("ю") || w.endsWith("е") || w.endsWith("и")) {
            return w.substring(0, w.length() - 1);
        }
        return w;
    }

    private String normalizeRussianFio(String fio) {
        if (StringUtils.isBlank(fio)) {
            return fio;
        }
        String[] parts = fio.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            String norm = p;
            // Дениса -> Денис, Гаркушина -> Гаркушин, Иванова -> Иванов
            if (p.endsWith("а") && p.length() > 3) {
                norm = p.substring(0, p.length() - 1);
            } else if (p.endsWith("ом") && p.length() > 4) {
                norm = p.substring(0, p.length() - 2);
            } else if (p.endsWith("ем") && p.length() > 4) {
                norm = p.substring(0, p.length() - 2);
            }
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(norm);
        }
        return sb.toString();
    }

    private LlmChatPendingAction loadActivePendingAction(UUID conversationId, UUID userId) {
        Date now = new Date();
        return dataManager.load(LlmChatPendingAction.class)
                .query("select e from hunttech_LlmChatPendingAction e where e.conversationId = :cId and e.userId = :uId and e.status = 'PENDING' and e.expiresAt > :now")
                .parameter("cId", conversationId)
                .parameter("uId", userId)
                .parameter("now", now)
                .view("llmChatPendingAction-view")
                .optional()
                .orElse(null);
    }

    private void savePendingAction(UUID conversationId, UUID userId, PendingInterviewState state, String requestId) {
        cancelPendingAction(conversationId, userId);

        String serializedState = serializeState(state);
        if (serializedState == null) {
            log.error("Не удалось сериализовать состояние Pending Action, сохранение отменено");
            return;
        }

        LlmChatPendingAction action = metadata.create(LlmChatPendingAction.class);
        action.setConversationId(conversationId);
        action.setUserId(userId);
        action.setActionType(ACTION_TYPE_INTERVIEW_SCHEDULING);
        action.setStatus("PENDING");
        action.setRequestId(requestId);

        // TTL 15 минут
        Calendar exp = Calendar.getInstance();
        exp.add(Calendar.MINUTE, 15);
        action.setExpiresAt(exp.getTime());

        action.setStateData(serializeState(state));
        dataManager.commit(action);
    }

    @Override
    public boolean cancelPendingAction(UUID conversationId, UUID userId) {
        try {
            List<LlmChatPendingAction> active = dataManager.load(LlmChatPendingAction.class)
                    .query("select e from hunttech_LlmChatPendingAction e where e.conversationId = :cId and e.userId = :uId and e.status = 'PENDING'")
                    .parameter("cId", conversationId)
                    .parameter("uId", userId)
                    .view("llmChatPendingAction-view")
                    .list();
            for (LlmChatPendingAction a : active) {
                a.setStatus("CANCELLED");
            }
            if (!active.isEmpty()) {
                dataManager.commit(new CommitContext(active));
                return true;
            }
        } catch (Exception ex) {
            log.warn("Не удалось отменить Pending Action: {}", ex.getMessage());
        }
        return false;
    }

    private String serializeState(PendingInterviewState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception ex) {
            log.error("Не удалось сериализовать состояние Pending Action: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private PendingInterviewState deserializeState(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PendingInterviewState.class);
        } catch (Exception ex) {
            log.error("Не удалось десериализовать состояние Pending Action: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private String extractCleanJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
