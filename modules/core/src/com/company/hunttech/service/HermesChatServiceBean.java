package com.company.hunttech.service;

import com.company.hunttech.config.HunttechHermesConfig;
import com.company.hunttech.dto.action.InterviewSchedulingResult;
import com.company.hunttech.core.ai.AiCostCalculator;
import com.company.hunttech.core.ai.AiSecretService;
import java.math.BigDecimal;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.entity.ai.AiCallLog;
import com.company.hunttech.entity.ai.LlmChatConversation;
import com.company.hunttech.entity.ai.LlmChatMessage;
import com.company.hunttech.service.AiSecuritySanitizer;
import com.company.hunttech.service.dto.AiUserContext;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.OpenPositionPriority;
import com.company.hunttech.service.dto.HermesChatMessage;
import com.company.hunttech.service.dto.HermesChatResponse;
import com.company.hunttech.service.dto.HermesConnectionStatus;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.company.hunttech.dto.yandex.AiMeetingParseResult;
import com.company.hunttech.dto.yandex.YandexMeetingResult;

/**
 * Реализация сервиса взаимодействия с Hermes Agent (профиль hrm-operator в Docker).
 */
@Service(HermesChatService.NAME)
public class HermesChatServiceBean implements HermesChatService {
    private static final Logger log = LoggerFactory.getLogger(HermesChatServiceBean.class);

    private static final String HERMES_CONVERSATION_TITLE_PREFIX = "Hermes: ";
    private static final String PROVIDER_HERMES = "hermes";
    private static final String DEFAULT_PROD_HOST = "hr.hunttech.ru";
    private static final String DEFAULT_PROD_IP = "92.63.101.170";
    private static final String DOCKER_SOCKET_PATH = "/var/run/docker.sock";
    private static final String SSH_KNOWN_HOSTS_PATH = "/tmp/hermes_known_hosts";
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("session_id:\\s*(\\S+)");
    private static final Pattern TOKEN_LINE_PATTERN = Pattern.compile("(?i)^\\s*(?:tokens?:|token usage:|prompt[_-]?tokens?:|completion[_-]?tokens?:|total[_-]?tokens?:|tokens?\\s*used:).*");
    private static final Pattern PROMPT_TOKENS_PATTERN = Pattern.compile("(?i)(?:prompt[_-]?tokens?|input[_-]?tokens?)\\s*[:=]?\\s*([0-9]+)|([0-9]+)\\s*(?:prompt|input)(?:\\s*tokens?)?");
    private static final Pattern COMPLETION_TOKENS_PATTERN = Pattern.compile("(?i)(?:completion[_-]?tokens?|output[_-]?tokens?)\\s*[:=]?\\s*([0-9]+)|([0-9]+)\\s*(?:completion|output)(?:\\s*tokens?)?");
    private static final Pattern TOTAL_TOKENS_PATTERN = Pattern.compile("(?i)(?:total[_-]?tokens?|tokens?\\s*used)\\s*[:=]?\\s*([0-9]+)|([0-9]+)\\s*total(?:\\s*tokens?)?");

    private static final String USER_CONTEXT_HEADER = "=== Сведения пользователя (не подтверждены HRM) ===";
    private static final String USER_INSTRUCTIONS_HEADER = "=== Предпочтения и инструкции пользователя ===";
    private static final String USER_CONTEXT_PRIORITY_NOTE =
            "Приоритет: системный промпт функции имеет приоритет над сведениями пользователя.\n"
                    + "Инструкции пользователя — это предпочтения стиля и структуры; они не отменяют факты,\n"
                    + "требования, ограничения и политики, заданные системным промптом.";

    @Inject
    private Configuration configuration;
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private AiSecretService aiSecretService;
    @Inject
    private UserAiContextService userAiContextService;
    @Inject
    private AiYandexOrchestrationService aiYandexOrchestrationService;
    @Inject
    private InterviewSchedulingActionService interviewSchedulingActionService;
    @Inject
    private SmartOpenPositionIngestService smartOpenPositionIngestService;
    @Inject
    private UserAiQuotaService userAiQuotaService;

    @Override
    public UUID startHermesConversation() {
        ExtUser currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        // Ищем активный диалог Hermes для данного пользователя
        String title = HERMES_CONVERSATION_TITLE_PREFIX + getHermesConfig().getProfile();
        LlmChatConversation conversation = dataManager.load(LlmChatConversation.class)
                .query("select e from hunttech_LlmChatConversation e " +
                        "where e.user.id = :userId and e.title = :title and e.status = 'ACTIVE' and e.deleteTs is null " +
                        "order by e.createTs desc")
                .parameter("userId", currentUser.getId())
                .parameter("title", title)
                .optional()
                .orElse(null);

        if (conversation == null) {
            conversation = metadata.create(LlmChatConversation.class);
            conversation.setUser(currentUser);
            conversation.setTitle(title);
            conversation.setStatus("ACTIVE");
            conversation.setLastMessageAt(new Date());
            conversation = dataManager.commit(conversation);
        }

        return conversation.getId();
    }

    @Override
    public List<HermesChatMessage> loadHermesHistory(UUID conversationId) {
        if (conversationId == null) {
            return new ArrayList<>();
        }

        ExtUser currentUser = getCurrentUser();
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;

        log.debug("loadHermesHistory: convId={}, userId={}", conversationId, currentUserId);
        List<LlmChatMessage> messages = dataManager.load(LlmChatMessage.class)
                .query("select e from hunttech_LlmChatMessage e " +
                        "where e.conversation.id = :conversationId and e.conversation.user.id = :userId and e.deleteTs is null " +
                        "order by e.sequenceNo asc")
                .parameter("conversationId", conversationId)
                .parameter("userId", currentUserId)
                .view("llm-chat-message-view")
                .list();

        List<HermesChatMessage> dtoList = new ArrayList<>();
        for (LlmChatMessage msg : messages) {
            HermesChatMessage dto = new HermesChatMessage();
            dto.setId(msg.getId());
            dto.setRole("USER".equalsIgnoreCase(msg.getRole()) ? "user" : "assistant");
            dto.setContent(msg.getContent());
            dto.setCreateTs(msg.getCreateTs());
            dto.setSequenceNo(msg.getSequenceNo());
            dto.setHermesSessionId(msg.getProviderRequestId());
            dtoList.add(dto);
        }

        log.debug("loadHermesHistory: загружено {} сообщений для convId={}", dtoList.size(), conversationId);
        return dtoList;
    }

    @Override
    public HermesChatResponse sendHermesMessage(UUID conversationId, String message) {
        long startTime = System.currentTimeMillis();
        if (conversationId == null) {
            throw new IllegalArgumentException("Идентификатор диалога не указан");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }

        ExtUser currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        // Проверяем доступность квоты токенов перед запуском запроса к Hermes (если нет персональных моделей)
        int estimatedPromptTokens = estimateTokens(message);
        boolean hasPersonalModel = userAiQuotaService != null && userAiQuotaService.hasActivePersonalModel(currentUser.getId());
        if (!hasPersonalModel && userAiQuotaService != null) {
            try {
                userAiQuotaService.checkQuotaAvailable(currentUser.getId(), estimatedPromptTokens);
            } catch (com.haulmont.cuba.core.global.DevelopmentException de) {
                long duration = System.currentTimeMillis() - startTime;
                String quotaErrorMsg = de.getMessage();
                log.warn("Пользователь {} исчерпал лимит токенов ИИ при вызове Hermes-viewer: {}",
                        currentUser.getLogin(), quotaErrorMsg);
                logAiCall(currentUser, conversationId, duration, PROVIDER_HERMES,
                        getHermesConfig() != null ? getHermesConfig().getProfile() : "hermes",
                        "ADMIN", estimatedPromptTokens, 0, estimatedPromptTokens, BigDecimal.ZERO, "USD",
                        "ERROR", quotaErrorMsg);
                return HermesChatResponse.error(conversationId, quotaErrorMsg, duration);
            }
        }

        LlmChatConversation conversation = dataManager.load(LlmChatConversation.class)
                .id(conversationId)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Диалог не найден: " + conversationId));

        // Вычисляем следующий sequenceNo
        Integer maxSeq = dataManager.loadValue("select max(e.sequenceNo) from hunttech_LlmChatMessage e " +
                "where e.conversation.id = :convId and e.deleteTs is null", Integer.class)
                .parameter("convId", conversationId)
                .optional()
                .orElse(0);

        // Ищем последнюю сессию Hermes для непрерывности контекста
        String lastHermesSessionId = dataManager.loadValue(
                "select e.providerRequestId from hunttech_LlmChatMessage e " +
                        "where e.conversation.id = :convId and e.providerRequestId is not null and e.deleteTs is null " +
                        "order by e.sequenceNo desc", String.class)
                .parameter("convId", conversationId)
                .optional()
                .orElse(null);

        log.info("sendHermesMessage: convId={}, пользователь={}, длина сообщения={}",
                conversationId, currentUser.getLogin(), message.length());

        // 0. Назначение собеседования кандидату и многошаговый диалог
        if (interviewSchedulingActionService != null) {
            InterviewSchedulingResult schedRes = null;
            try {
                schedRes = interviewSchedulingActionService.handleSchedulingAction(conversationId, message.trim(), currentUser, UUID.randomUUID().toString());
            } catch (Exception ex) {
                log.error("Сбой сценария назначения собеседования в Hermes Chat: {}", ex.getMessage(), ex);
            }
            if (schedRes != null && schedRes.getMessage() != null) {
                String responseText = schedRes.getMessage();
                long duration = System.currentTimeMillis() - startTime;
                LlmChatMessage userMsg = metadata.create(LlmChatMessage.class);
                userMsg.setConversation(conversation);
                userMsg.setRole("USER");
                userMsg.setContent(message.trim());
                userMsg.setSequenceNo(maxSeq + 1);
                userMsg.setStatus("COMPLETED");

                LlmChatMessage assistantMsg = metadata.create(LlmChatMessage.class);
                assistantMsg.setConversation(conversation);
                assistantMsg.setRole("ASSISTANT");
                assistantMsg.setContent(responseText);
                assistantMsg.setSequenceNo(maxSeq + 2);
                assistantMsg.setStatus("COMPLETED");
                assistantMsg.setProviderCode("yandex");
                assistantMsg.setModelName("caldav-telemost");

                conversation.setLastMessageAt(new Date());
                dataManager.commit(new CommitContext(conversation, userMsg, assistantMsg));

                int promptToks = estimateTokens(message);
                int compToks = estimateTokens(responseText);
                int totalToks = promptToks + compToks;
                if (userAiQuotaService != null && totalToks > 0) {
                    userAiQuotaService.recordTokenConsumption(currentUser.getId(), totalToks, "ADMIN");
                }
                logAiCall(currentUser, conversationId, duration, "yandex", "caldav-telemost", "ADMIN",
                        promptToks, compToks, totalToks, BigDecimal.ZERO, "RUB", "SUCCESS", null);

                HermesChatResponse hermesResp = new HermesChatResponse(conversationId, responseText, null, duration);
                hermesResp.setProviderCode("yandex");
                hermesResp.setModelName("caldav-telemost");
                hermesResp.setPromptTokens(promptToks);
                hermesResp.setCompletionTokens(compToks);
                hermesResp.setTotalTokens(totalToks);
                return hermesResp;
            }
        }

        // Перехват прямого запроса на создание встречи в календаре / Телемосте
        if (aiYandexOrchestrationService != null && aiYandexOrchestrationService.isMeetingBookingIntent(message.trim())) {
            AiMeetingParseResult parseResult = aiYandexOrchestrationService.parseMeetingIntent(message.trim(), currentUser.getId());
            if (parseResult.isIntentDetected() && AiYandexOrchestrationService.containsBookingVerb(message)) {
                String responseText;
                try {
                    YandexMeetingResult booking = aiYandexOrchestrationService.executeMeetingBooking(currentUser.getId(), parseResult);
                    if (booking.isSuccess()) {
                        responseText = booking.getMessage();
                    } else {
                        responseText = AiYandexOrchestrationService.formatCalDavFailureMessage(booking.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Сбой бронирования встречи в Hermes Chat: {}", e.getMessage(), e);
                    responseText = AiYandexOrchestrationService.formatCalDavFailureMessage(e.getMessage());
                }
                long duration = System.currentTimeMillis() - startTime;
                LlmChatMessage userMsg = metadata.create(LlmChatMessage.class);
                userMsg.setConversation(conversation);
                userMsg.setRole("USER");
                userMsg.setContent(message.trim());
                userMsg.setSequenceNo(maxSeq + 1);
                userMsg.setStatus("COMPLETED");

                LlmChatMessage assistantMsg = metadata.create(LlmChatMessage.class);
                assistantMsg.setConversation(conversation);
                assistantMsg.setRole("ASSISTANT");
                assistantMsg.setContent(responseText);
                assistantMsg.setSequenceNo(maxSeq + 2);
                assistantMsg.setStatus("COMPLETED");
                assistantMsg.setProviderCode("yandex");
                assistantMsg.setModelName("caldav-telemost");

                conversation.setLastMessageAt(new Date());
                dataManager.commit(new CommitContext(conversation, userMsg, assistantMsg));

                int promptToks = estimateTokens(message);
                int compToks = estimateTokens(responseText);
                int totalToks = promptToks + compToks;
                if (userAiQuotaService != null && totalToks > 0) {
                    userAiQuotaService.recordTokenConsumption(currentUser.getId(), totalToks, "ADMIN");
                }
                logAiCall(currentUser, conversationId, duration, "yandex", "caldav-telemost", "ADMIN",
                        promptToks, compToks, totalToks, BigDecimal.ZERO, "RUB", "SUCCESS", null);

                HermesChatResponse hermesResp = new HermesChatResponse(conversationId, responseText, null, duration);
                hermesResp.setProviderCode("yandex");
                hermesResp.setModelName("caldav-telemost");
                hermesResp.setPromptTokens(promptToks);
                hermesResp.setCompletionTokens(compToks);
                hermesResp.setTotalTokens(totalToks);
                return hermesResp;
            }
        }

        // 0.2 Перехват запроса на создание / открытие вакансии по стандарту hunttech-vacancy-opening
        if (isVacancyOpeningIntent(message.trim())) {
            return handleVacancyOpeningInHermes(conversation, message.trim(), currentUser, maxSeq, startTime);
        }

        // 1. Выполняем запрос к Hermes Agent с перебором моделей по утвержденному сценарию:
        //    Сначала подключается модель из пользовательских настроек (UserAiConfiguration).
        //    Если ни одна пользовательская модель не настроена или не отвечает — административные модели (AdminAiConfiguration).
        List<HermesExecutionCandidate> candidates = resolveExecutionCandidates(currentUser);
        log.info("Определено {} кандидатов для выполнения запроса к Hermes: {}", candidates.size(), candidates);

        String effectivePrompt = buildHermesUserPrompt(message.trim(), currentUser);

        String assistantText = null;
        String newSessionId = lastHermesSessionId;
        HermesExecutionCandidate successfulCandidate = null;
        HermesExecutionResult successfulResult = null;
        Exception lastException = null;

        for (int i = 0; i < candidates.size(); i++) {
            HermesExecutionCandidate candidate = candidates.get(i);
            try {
                // Если кандидат административный (ADMIN или CONTAINER_DEFAULT), проверяем квоту перед вызовом
                if (userAiQuotaService != null && userAiQuotaService.isAdminModel(candidate.getSource())) {
                    userAiQuotaService.checkQuotaAvailable(currentUser.getId(), estimatedPromptTokens);
                }

                log.info("Попытка #{}/{} выполнения запроса к Hermes через [{}]: provider={}, model={}, baseUrl={}",
                        i + 1, candidates.size(), candidate.getSource(), candidate.getProviderCode(),
                        candidate.getModelName(), candidate.getBaseUrl());
                HermesExecutionResult execResult = executeHermesCli(effectivePrompt, lastHermesSessionId, candidate);
                assistantText = execResult.cleanedText;
                if (execResult.sessionId != null && !execResult.sessionId.isEmpty()) {
                    newSessionId = execResult.sessionId;
                }
                successfulCandidate = candidate;
                successfulResult = execResult;
                log.info("Hermes успешно ответил через [{}] (provider={}, model={}, sessionId={}, responseLength={})",
                        candidate.getSource(), candidate.getProviderCode(), candidate.getModelName(),
                        newSessionId, assistantText != null ? assistantText.length() : 0);
                break;
            } catch (com.haulmont.cuba.core.global.DevelopmentException ex) {
                lastException = ex;
                log.warn("Пользователь {} исчерпал лимит токенов при попытке вызова модели Hermes [{}]: {}",
                        currentUser.getLogin(), candidate.getSource(), ex.getMessage());
                break;
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Подключение к модели Hermes #{}/{} [{}] не ответило: {}. Пробуем следующий вариант...",
                        i + 1, candidates.size(), candidate.getSource(), ex.getMessage(), ex);
            }
        }

        if (assistantText == null) {
            long duration = System.currentTimeMillis() - startTime;
            String errorDetail = lastException != null ? lastException.getMessage() : "нет ответа от моделей";
            log.error("Все варианты подключения к модели Hermes завершились ошибкой (всего {} вариантов): {}",
                    candidates.size(), errorDetail, lastException);

            HermesExecutionCandidate lastCandidate = !candidates.isEmpty() ? candidates.get(candidates.size() - 1) : null;
            String failProvider = (lastCandidate != null && lastCandidate.getProviderCode() != null)
                    ? lastCandidate.getProviderCode() : PROVIDER_HERMES;
            String failModel = (lastCandidate != null && lastCandidate.getModelName() != null)
                    ? lastCandidate.getModelName() : getHermesConfig().getProfile();
            String failOwner = (lastCandidate != null && "USER".equalsIgnoreCase(lastCandidate.getSource()))
                    ? "USER" : "ADMIN";

            int fallbackPromptTokens = estimateTokens(message);
            AiCostCalculator.CostResult costResult = AiCostCalculator.calculateCost(
                    failProvider, failModel, fallbackPromptTokens, 0);
            logAiCall(currentUser, conversationId, duration, failProvider, failModel, failOwner,
                    fallbackPromptTokens, 0, fallbackPromptTokens, costResult.getCost(), costResult.getCurrency(),
                    "ERROR", errorDetail);

            boolean isQuotaErr = errorDetail.contains("Закончились доступные токены ИИ")
                    || errorDetail.contains(UserAiQuotaService.MSG_QUOTA_EXHAUSTED);
            return HermesChatResponse.error(conversationId, isQuotaErr
                    ? errorDetail : "Ошибка Hermes Agent: " + errorDetail, duration);
        }

        // 2. Сохраняем сообщение пользователя, ответ ассистента и состояние диалога в основном CommitContext
        String effectiveProvider = (successfulCandidate != null && successfulCandidate.getProviderCode() != null)
                ? successfulCandidate.getProviderCode() : PROVIDER_HERMES;
        String effectiveModel = (successfulCandidate != null && successfulCandidate.getModelName() != null)
                ? successfulCandidate.getModelName() : getHermesConfig().getProfile();
        String credentialOwner = (successfulCandidate != null && "USER".equalsIgnoreCase(successfulCandidate.getSource()))
                ? "USER" : "ADMIN";

        int promptTokens = (successfulResult != null && successfulResult.promptTokens != null)
                ? successfulResult.promptTokens : estimateTokens(message);
        int completionTokens = (successfulResult != null && successfulResult.completionTokens != null)
                ? successfulResult.completionTokens : estimateTokens(assistantText);
        int totalTokens = (successfulResult != null && successfulResult.totalTokens != null)
                ? successfulResult.totalTokens : (promptTokens + completionTokens);

        AiCostCalculator.CostResult costResult = AiCostCalculator.calculateCost(
                effectiveProvider, effectiveModel, promptTokens, completionTokens);

        LlmChatMessage userMsg = metadata.create(LlmChatMessage.class);
        userMsg.setConversation(conversation);
        userMsg.setRole("USER");
        userMsg.setContent(message.trim());
        userMsg.setSequenceNo(maxSeq + 1);
        userMsg.setStatus("COMPLETED");
        userMsg.setProviderCode(effectiveProvider);
        userMsg.setModelName(effectiveModel);

        LlmChatMessage assistantMsg = metadata.create(LlmChatMessage.class);
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setContent(assistantText);
        assistantMsg.setSequenceNo(maxSeq + 2);
        assistantMsg.setStatus("COMPLETED");
        assistantMsg.setProviderCode(effectiveProvider);
        assistantMsg.setModelName(effectiveModel);
        assistantMsg.setProviderRequestId(newSessionId);

        conversation.setLastMessageAt(new Date());

        CommitContext commitContext = new CommitContext();
        commitContext.addInstanceToCommit(userMsg);
        commitContext.addInstanceToCommit(assistantMsg);
        commitContext.addInstanceToCommit(conversation);
        dataManager.commit(commitContext);

        long duration = System.currentTimeMillis() - startTime;

        // Списываем фактически израсходованные токены в счет месячной квоты пользователя
        // (recordTokenConsumption с credentialOwner списывает только для административных моделей)
        if (userAiQuotaService != null && totalTokens > 0) {
            userAiQuotaService.recordTokenConsumption(currentUser.getId(), totalTokens, credentialOwner);
        }

        // Отдельно и безопасно фиксируем технический аудит вызовов AI (AiCallLog), чтобы сбой аудита не отменял диалог
        logAiCall(currentUser, conversationId, duration, effectiveProvider, effectiveModel, credentialOwner,
                promptTokens, completionTokens, totalTokens, costResult.getCost(), costResult.getCurrency(),
                "SUCCESS", null);

        HermesChatResponse response = new HermesChatResponse(conversationId, assistantText, newSessionId, duration);
        response.setModelName(effectiveModel);
        response.setProviderCode(effectiveProvider);
        response.setPromptTokens(promptTokens);
        response.setCompletionTokens(completionTokens);
        response.setTotalTokens(totalTokens);
        response.setEstimatedCost(costResult.getCost());
        response.setCurrency(costResult.getCurrency());
        return response;
    }

    private void logAiCall(ExtUser user, UUID conversationId, long durationMs,
                           String providerCode, String modelName, String credentialOwner,
                           Integer promptTokens, Integer completionTokens, Integer totalTokens,
                           java.math.BigDecimal estimatedCost, String currency,
                           String status, String errorMessage) {
        try {
            AiCallLog callLog = metadata.create(AiCallLog.class);
            callLog.setUser(user);
            if (user != null) {
                callLog.setUserLogin(user.getLogin());
                callLog.setUserName(user.getName());
            }
            callLog.setCallTime(new Date());
            callLog.setDurationMs(durationMs);
            callLog.setFunctionCode("HERMES_CHAT");
            callLog.setFunctionName("Hermes-viewer (ассистент)");
            callLog.setCapability("TEXT_GENERATION");
            callLog.setProviderCode(providerCode);
            callLog.setModelName(modelName);
            callLog.setCredentialOwner(credentialOwner);
            callLog.setPromptTokens(promptTokens);
            callLog.setCompletionTokens(completionTokens);
            callLog.setTotalTokens(totalTokens);
            callLog.setEstimatedCost(estimatedCost);
            callLog.setCurrency(currency);
            callLog.setCallerSource(conversationId != null ? "HermesChat:" + conversationId : "HermesChat");
            callLog.setStatus(status);
            callLog.setErrorMessage(AiSecuritySanitizer.sanitizeError(errorMessage));
            dataManager.commit(new CommitContext(callLog));
        } catch (Exception e) {
            log.error("Не удалось сохранить запись AiCallLog для Hermes: {}", e.getMessage(), e);
        }
    }

    static int estimateTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private List<HermesExecutionCandidate> resolveExecutionCandidates(ExtUser currentUser) {
        List<HermesExecutionCandidate> candidates = new ArrayList<>();

        // 1. Сначала пользовательские настройки (isPrimary DESC, priority DESC)
        if (currentUser != null) {
            try {
                List<UserAiConfiguration> userConfigs = dataManager.load(UserAiConfiguration.class)
                        .query("select c from hunttech_UserAiConfiguration c " +
                                "where c.user.id = :userId and (c.isActive is null or c.isActive = true) " +
                                "order by c.isPrimary desc, c.priority desc")
                        .parameter("userId", currentUser.getId())
                        .list();
                for (UserAiConfiguration uc : userConfigs) {
                    String apiKey = resolveUserApiKey(uc);
                    if (apiKey != null && !apiKey.trim().isEmpty() && uc.getProviderCode() != null) {
                        candidates.add(new HermesExecutionCandidate("USER", uc.getProviderCode().trim(),
                                uc.getDefaultModelName(), apiKey.trim(), null));
                    }
                }
            } catch (Exception e) {
                log.warn("Ошибка загрузки пользовательских AI-конфигураций: {}", e.getMessage());
            }
        }

        // 2. Если ни одна пользовательская модель не отвечает — административные модели (priority DESC)
        try {
            List<AdminAiConfiguration> adminConfigs = dataManager.load(AdminAiConfiguration.class)
                    .query("select c from hunttech_AdminAiConfiguration c " +
                            "where (c.active is null or c.active = true) " +
                            "order by c.priority desc")
                    .list();
            for (AdminAiConfiguration ac : adminConfigs) {
                String apiKey = null;
                if (ac.getApiKeyEncrypted() != null && !ac.getApiKeyEncrypted().trim().isEmpty()) {
                    try {
                        apiKey = aiSecretService.decrypt(ac.getApiKeyEncrypted());
                    } catch (Exception e) {
                        log.warn("Не удалось расшифровать ключ AdminAiConfiguration {}: {}", ac.getName(), e.getMessage());
                    }
                }
                if (apiKey != null && !apiKey.trim().isEmpty() && ac.getProviderCode() != null) {
                    candidates.add(new HermesExecutionCandidate("ADMIN", ac.getProviderCode().trim(),
                            ac.getDefaultModelName(), apiKey.trim(), ac.getBaseApiUrl()));
                }
            }
        } catch (Exception e) {
            log.warn("Ошибка загрузки административных AI-конфигураций: {}", e.getMessage());
        }

        // 3. Резервное подключение по умолчанию из профиля контейнера
        candidates.add(new HermesExecutionCandidate("CONTAINER_DEFAULT", null, null, null, null));

        return candidates;
    }

    private String resolveUserApiKey(UserAiConfiguration configuration) {
        if (configuration.getApiKeyEncrypted() != null && !configuration.getApiKeyEncrypted().trim().isEmpty()) {
            try {
                return aiSecretService.decrypt(configuration.getApiKeyEncrypted());
            } catch (Exception e) {
                log.warn("Не удалось расшифровать ключ UserAiConfiguration: {}", e.getMessage());
            }
        }
        return configuration.getApiKey();
    }

    @Override
    public HermesConnectionStatus checkHermesConnection() {
        long startTime = System.currentTimeMillis();
        HunttechHermesConfig config = getHermesConfig();
        try {
            // Быстрая проверка через docker ps / inspect контейнера
            List<String> cmd = new ArrayList<>();
            if (shouldUseSsh(config)) {
                cmd.add("ssh");
                cmd.add("-o");
                cmd.add("BatchMode=yes");
                cmd.add("-o");
                cmd.add("ConnectTimeout=5");
                cmd.add("-o");
                cmd.add("StrictHostKeyChecking=accept-new");
                cmd.add("-o");
                cmd.add("UserKnownHostsFile=" + SSH_KNOWN_HOSTS_PATH);
                if (config.getSshPort() != 22) {
                    cmd.add("-p");
                    cmd.add(String.valueOf(config.getSshPort()));
                }
                cmd.add(config.getSshUser() + "@" + config.getSshHost());
                cmd.add("docker inspect -f '{{.State.Status}}' " + shellEscape(config.getContainerName()));
            } else {
                cmd.add("docker");
                cmd.add("inspect");
                cmd.add("-f");
                cmd.add("{{.State.Status}}");
                cmd.add(config.getContainerName());
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new HermesConnectionStatus(false, config.getSshHost(), config.getContainerName(),
                        config.getProfile(), "Таймаут подключения (10 с)", System.currentTimeMillis() - startTime);
            }

            int exitCode = process.exitValue();
            String stdout = readStream(process.getInputStream()).trim();
            long duration = System.currentTimeMillis() - startTime;

            if (exitCode == 0 && "running".equalsIgnoreCase(stdout)) {
                return new HermesConnectionStatus(true, config.getSshHost(), config.getContainerName(),
                        config.getProfile(), "Контейнер активен (статус: running)", duration);
            } else {
                String stderr = readStream(process.getErrorStream()).trim();
                return new HermesConnectionStatus(false, config.getSshHost(), config.getContainerName(),
                        config.getProfile(), "Контейнер недоступен (код " + exitCode + "): " + stderr + " " + stdout, duration);
            }
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            return new HermesConnectionStatus(false, config.getSshHost(), config.getContainerName(),
                    config.getProfile(), "Ошибка соединения: " + ex.getMessage(), duration);
        }
    }

    private HermesExecutionResult executeHermesCli(String prompt, String resumeSessionId,
                                                   HermesExecutionCandidate candidate) throws Exception {
        HunttechHermesConfig config = getHermesConfig();
        List<String> command = new ArrayList<>();

        // Формируем переменные окружения для передачи в docker
        Map<String, String> envVars = new HashMap<>();
        if (candidate != null && candidate.getApiKey() != null && !candidate.getApiKey().isEmpty()) {
            String provider = candidate.getProviderCode() != null
                    ? candidate.getProviderCode().toLowerCase(Locale.ROOT) : "";
            switch (provider) {
                case "deepseek":
                    envVars.put("DEEPSEEK_API_KEY", candidate.getApiKey());
                    if (candidate.getBaseUrl() != null && !candidate.getBaseUrl().isEmpty()) {
                        envVars.put("DEEPSEEK_BASE_URL", candidate.getBaseUrl());
                    }
                    break;
                case "openai":
                    envVars.put("OPENAI_API_KEY", candidate.getApiKey());
                    if (candidate.getBaseUrl() != null && !candidate.getBaseUrl().isEmpty()) {
                        envVars.put("OPENAI_BASE_URL", candidate.getBaseUrl());
                    }
                    break;
                case "openrouter":
                    envVars.put("OPENROUTER_API_KEY", candidate.getApiKey());
                    break;
                case "anthropic":
                    envVars.put("ANTHROPIC_API_KEY", candidate.getApiKey());
                    break;
                default:
                    envVars.put(provider.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "") + "_API_KEY", candidate.getApiKey());
                    break;
            }
        }

        // Собираем аргументы вызова hermes chat
        List<String> hermesArgs = new ArrayList<>();
        hermesArgs.add("hermes");
        hermesArgs.add("-p");
        hermesArgs.add(config.getProfile());
        hermesArgs.add("chat");

        if (candidate != null && candidate.getProviderCode() != null && !candidate.getProviderCode().isEmpty()) {
            hermesArgs.add("--provider");
            hermesArgs.add(candidate.getProviderCode());
        }
        if (candidate != null && candidate.getModelName() != null && !candidate.getModelName().isEmpty()) {
            hermesArgs.add("-m");
            hermesArgs.add(candidate.getModelName());
        }
        if (resumeSessionId != null && !resumeSessionId.trim().isEmpty()) {
            hermesArgs.add("--resume");
            hermesArgs.add(resumeSessionId.trim());
        }
        hermesArgs.add("--query-file");
        hermesArgs.add("-");
        hermesArgs.add("--oneshot");
        hermesArgs.add("-Q");

        if (shouldUseSsh(config)) {
            command.add("ssh");
            command.add("-o");
            command.add("BatchMode=yes");
            command.add("-o");
            command.add("ConnectTimeout=15");
            command.add("-o");
            command.add("StrictHostKeyChecking=accept-new");
            command.add("-o");
            command.add("UserKnownHostsFile=" + SSH_KNOWN_HOSTS_PATH);
            if (config.getSshPort() != 22) {
                command.add("-p");
                command.add(String.valueOf(config.getSshPort()));
            }
            command.add(config.getSshUser() + "@" + config.getSshHost());

            StringBuilder remoteCmd = new StringBuilder();
            remoteCmd.append("docker exec -i");
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                remoteCmd.append(" -e ").append(entry.getKey()).append("=").append(shellEscape(entry.getValue()));
            }
            remoteCmd.append(" ").append(shellEscape(config.getContainerName()));
            for (String arg : hermesArgs) {
                remoteCmd.append(" ").append(shellEscape(arg));
            }
            command.add(remoteCmd.toString());
        } else {
            command.add("docker");
            command.add("exec");
            command.add("-i");
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                command.add("-e");
                command.add(entry.getKey() + "=" + entry.getValue());
            }
            command.add(config.getContainerName());
            command.addAll(hermesArgs);
        }

        List<String> maskedCommand = new ArrayList<>();
        for (String part : command) {
            maskedCommand.add(part.replaceAll("(?i)(key=)('[^']*'|[^'\\s]+)", "$1***"));
        }
        log.info("Запуск процесса Hermes CLI (timeout={}s): {}", config.getTimeoutSeconds(), String.join(" ", maskedCommand));

        long cliStart = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        // Асинхронное чтение stdout и stderr во избежание deadlock при переполнении буфера пайпа OS
        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return readStream(process.getInputStream());
            } catch (Exception e) {
                log.warn("Ошибка чтения stdout процесса Hermes: {}", e.getMessage());
                return "";
            }
        });
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return readStream(process.getErrorStream());
            } catch (Exception e) {
                log.warn("Ошибка чтения stderr процесса Hermes: {}", e.getMessage());
                return "";
            }
        });

        // Безопасная передача промпта через STDIN (исключает шелл-инъекции и поломку спецсимволов)
        try (OutputStream os = process.getOutputStream()) {
            os.write(prompt.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int timeoutSeconds = config.getTimeoutSeconds();
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - cliStart;

        if (!completed) {
            process.destroyForcibly();
            stdoutFuture.cancel(true);
            stderrFuture.cancel(true);
            log.error("Таймаут выполнения Hermes CLI после {} мс (лимит {} с)", duration, timeoutSeconds);
            throw new RuntimeException("Превышено время ожидания ответа от Hermes Agent (" + timeoutSeconds + " с)");
        }

        String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
        String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
        int exitCode = process.exitValue();

        log.info("Процесс Hermes CLI завершился: exitCode={}, время={} мс, длина stdout={}, длина stderr={}",
                exitCode, duration, stdout != null ? stdout.length() : 0, stderr != null ? stderr.length() : 0);
        if (stderr != null && !stderr.trim().isEmpty()) {
            log.warn("Hermes CLI stderr (первые 300 символов): {}",
                    stderr.length() > 300 ? stderr.substring(0, 300) + "..." : stderr);
        }

        if (exitCode != 0) {
            String combinedErr = (stderr != null && !stderr.trim().isEmpty())
                    ? stderr.trim()
                    : (stdout != null && !stdout.trim().isEmpty() ? stdout.trim() : "нет вывода");
            boolean sessionNotFound = (stderr != null && stderr.contains("Session not found"))
                    || (stdout != null && stdout.contains("Session not found"));
            // Если сессия не найдена на сервере, повторяем запрос без флага --resume
            if (sessionNotFound && resumeSessionId != null) {
                log.warn("Сессия Hermes {} не найдена на сервере, повторяем запрос без --resume", resumeSessionId);
                return executeHermesCli(prompt, null, candidate);
            }
            log.warn("Hermes CLI завершился с ошибкой (exitCode={}): {}", exitCode, combinedErr);
            throw new RuntimeException("Hermes CLI завершился с ошибкой (код " + exitCode + "): " + combinedErr);
        }

        HermesExecutionResult result = parseHermesOutput(stdout);
        if (stderr != null) {
            parseTokensFromText(stderr, result);
        }
        if (result.promptTokens == null || result.promptTokens <= 0) {
            result.promptTokens = estimateTokens(prompt);
        }
        if (result.completionTokens == null || result.completionTokens <= 0) {
            result.completionTokens = estimateTokens(result.cleanedText);
        }
        if (result.totalTokens == null || result.totalTokens <= 0) {
            result.totalTokens = result.promptTokens + result.completionTokens;
        }

        if (isErrorResponse(result.cleanedText)) {
            log.warn("Ответ Hermes CLI содержит ошибку провайдера или безопасности: {}", result.cleanedText);
            throw new RuntimeException("Провайдер модели вернул ошибку: " + result.cleanedText);
        }

        log.info("Результат парсинга ответа Hermes: длина ответа={}, sessionId={}, tokens={}/{}",
                result.cleanedText != null ? result.cleanedText.length() : 0, result.sessionId,
                result.promptTokens, result.completionTokens);
        return result;
    }

    private String shellEscape(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    boolean shouldUseSsh(HunttechHermesConfig config) {
        if (config == null || !config.getSshEnabled()) {
            return false;
        }
        String host = config.getSshHost();
        if (host == null || host.trim().isEmpty()) {
            return false;
        }
        String trimmedHost = host.trim().toLowerCase(Locale.ROOT);
        if ("localhost".equals(trimmedHost) || "127.0.0.1".equals(trimmedHost) || "::1".equals(trimmedHost)) {
            return false;
        }
        // Если хост совпадает с сервером hr.hunttech.ru и доступен локальный docker.sock
        if (DEFAULT_PROD_HOST.equalsIgnoreCase(trimmedHost) || DEFAULT_PROD_IP.equals(trimmedHost)) {
            File dockerSock = new File(DOCKER_SOCKET_PATH);
            if (dockerSock.exists()) {
                log.debug("Вызов Hermes: обнаружен локальный сервер {} с доступным docker.sock, прямой вызов без SSH", trimmedHost);
                return false;
            }
        }
        return true;
    }

    boolean isErrorResponse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        String lower = trimmed.toLowerCase();
        boolean hasJsonError = lower.startsWith("{")
                && lower.contains("\"error\"")
                && (lower.contains("\"success\":false") || lower.contains("\"success\": false") || lower.contains("\"status\":\"error\""));

        return trimmed.startsWith("HTTP 403:")
                || trimmed.startsWith("HTTP 401:")
                || trimmed.startsWith("HTTP 429:")
                || trimmed.startsWith("HTTP 500:")
                || trimmed.startsWith("HTTP 502:")
                || trimmed.startsWith("HTTP 503:")
                || hasJsonError
                || lower.startsWith("access denied by security policy")
                || trimmed.startsWith("AuthenticationError:")
                || trimmed.startsWith("Error: No API key found");
    }

    public static class HermesExecutionCandidate {
        private final String source;
        private final String providerCode;
        private final String modelName;
        private final String apiKey;
        private final String baseUrl;

        public HermesExecutionCandidate(String source, String providerCode, String modelName, String apiKey, String baseUrl) {
            this.source = source;
            this.providerCode = providerCode;
            this.modelName = modelName;
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
        }

        public String getSource() { return source; }
        public String getProviderCode() { return providerCode; }
        public String getModelName() { return modelName; }
        public String getApiKey() { return apiKey; }
        public String getBaseUrl() { return baseUrl; }

        @Override
        public String toString() {
            return "[" + source + "] " + (providerCode != null ? providerCode : "default") +
                    (modelName != null ? " (" + modelName + ")" : "");
        }
    }

    private HermesExecutionResult parseHermesOutput(String rawOutput) {
        HermesExecutionResult result = new HermesExecutionResult();
        if (rawOutput == null) {
            result.cleanedText = "";
            return result;
        }

        String[] lines = rawOutput.split("\r?\n");
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            Matcher m = SESSION_ID_PATTERN.matcher(line);
            if (m.find()) {
                result.sessionId = m.group(1).trim();
                continue;
            }

            // Фильтрация технических баннеров Hermes
            String trimmed = line.trim();
            if (trimmed.startsWith("⚠️") || trimmed.startsWith("⚠") || trimmed.startsWith("↻")) {
                continue;
            }
            if (trimmed.startsWith("Warning:") || trimmed.startsWith("Normalized model")) {
                continue;
            }

            // Парсинг информации о токенах и фильтрация строк служебной статистики токенов
            boolean isTokenLine = parseTokensFromLine(line, result);
            if (isTokenLine || trimmed.toLowerCase().startsWith("tokens:") || trimmed.toLowerCase().startsWith("token usage:")) {
                continue;
            }

            sb.append(line).append("\n");
        }

        result.cleanedText = cleanHermesOutput(sb.toString().trim());
        return result;
    }

    /**
     * Очистка вывода Hermes от технических идентификаторов и кракозябр (UUID видов "8a0f5f9c-6a32-4e6a-a2c9-672870000001",
     * префиксов ID:, конструкций в скобках (ID: ...)).
     */
    public static String cleanHermesOutput(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String cleaned = text;

        // 1. Удаление префиксов UUID в строках списков:
        // например: "- 8a0f5f9c-6a32-4e6a-a2c9-672870000001 — " или "• \"8a0f5f9c-6a32-4e6a-a2c9-672870000001\" — "
        cleaned = cleaned.replaceAll("(?m)^(\\s*[-*•]\\s*)(?:\"|'|«|“|<code>|`)*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?:\"|'|»|”|</code>|`)*\\s*[—–\\-:]\\s*", "$1");

        // 2. Удаление конструкций в скобках: (ID: "8a0f5f9c-6a32-4e6a-a2c9-672870000001"), (ID: 8a0f...), (UUID: 8a0f...)
        cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*(?:id|uuid)?\\s*[:=]?\\s*[\"«“'`]?[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}[\"»”'`]?\\s*\\)", "");

        // 3. Удаление префиксов ID: "8a0f...", UUID: 8a0f... (не трогая ?id= или &id= в URL)
        cleaned = cleaned.replaceAll("(?i)(?<![?&/])\\b(?:id|uuid)\\s*[:=]?\\s*[\"«“'`][0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}[\"»”'`]", "");
        cleaned = cleaned.replaceAll("(?i)(?<![?&/])\\b(?:id|uuid)\\s*[:=]\\s*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "");

        // 4. Удаление сырых UUID в двойных или одинарных кавычках: "8a0f5f9c-6a32-4e6a-a2c9-672870000001", ""8a0f...""
        cleaned = cleaned.replaceAll("(?i)(?<![a-zA-Z0-9_\\-/=?.])(?:\"\"|\"|«|“|')(?:<code>|`)?[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?:</code>|`)?(?:\"\"|\"|»|”|')(?![a-zA-Z0-9_\\-/=?.])", "");

        // 5. Очистка оставшихся пробельных артефактов
        cleaned = cleaned.replaceAll("(?m)[ \\t]+", " ");
        cleaned = cleaned.replaceAll("(?m)[ \\t]+$", "");
        cleaned = cleaned.replaceAll(" \\(\\)", "");

        return cleaned.trim();
    }

    private boolean parseTokensFromLine(String line, HermesExecutionResult result) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        String trimmed = line.trim();
        boolean isExplicitTokenLine = TOKEN_LINE_PATTERN.matcher(trimmed).matches();

        boolean matched = false;
        Matcher mPrompt = PROMPT_TOKENS_PATTERN.matcher(line);
        if (mPrompt.find()) {
            matched = true;
            if (result.promptTokens == null) {
                try {
                    String val = mPrompt.group(1) != null ? mPrompt.group(1) : mPrompt.group(2);
                    if (val != null) {
                        result.promptTokens = Integer.parseInt(val.trim());
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        Matcher mComp = COMPLETION_TOKENS_PATTERN.matcher(line);
        if (mComp.find()) {
            matched = true;
            if (result.completionTokens == null) {
                try {
                    String val = mComp.group(1) != null ? mComp.group(1) : mComp.group(2);
                    if (val != null) {
                        result.completionTokens = Integer.parseInt(val.trim());
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        Matcher mTotal = TOTAL_TOKENS_PATTERN.matcher(line);
        if (mTotal.find()) {
            matched = true;
            if (result.totalTokens == null) {
                try {
                    String val = mTotal.group(1) != null ? mTotal.group(1) : mTotal.group(2);
                    if (val != null) {
                        result.totalTokens = Integer.parseInt(val.trim());
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return isExplicitTokenLine || (matched && trimmed.toLowerCase().contains("token"));
    }

    private void parseTokensFromText(String text, HermesExecutionResult result) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        for (String line : text.split("\r?\n")) {
            parseTokensFromLine(line, result);
        }
    }

    private String readStream(java.io.InputStream is) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    private ExtUser getCurrentUser() {
        if (userSessionSource == null || userSessionSource.getUserSession() == null) {
            return null;
        }
        User user = userSessionSource.getUserSession().getUser();
        if (user instanceof ExtUser) {
            return (ExtUser) user;
        }
        return dataManager.load(ExtUser.class).id(user.getId()).optional().orElse(null);
    }

    private HunttechHermesConfig getHermesConfig() {
        return configuration.getConfig(HunttechHermesConfig.class);
    }

    /**
     * Формирует промпт для Hermes Agent с включением персонализированного контекста
     * текущего пользователя (ExtUser и UserAiProfile), чтобы Hermes знал с кем общается
     * и давал ответы исходя из персональных предпочтений пользователя.
     * Строго соблюдает политику согласий и бюджет токенов через единый UserAiContextService.
     */
    String buildHermesUserPrompt(String userMessage, ExtUser currentUser) {
        if (currentUser == null) {
            return userMessage;
        }

        AiUserContext userCtx = null;
        try {
            userCtx = userAiContextService.buildCurrentUserContext();
        } catch (Exception e) {
            log.warn("Не удалось получить AiUserContext через UserAiContextService: {}", e.getMessage(), e);
        }

        if (userCtx == null || userCtx.isEmpty()) {
            return userMessage;
        }

        StringBuilder contextBlock = new StringBuilder();
        contextBlock.append(USER_CONTEXT_HEADER).append("\n");
        String userName = userCtx.getProfileData().get("userName");
        if (userName != null && !userName.trim().isEmpty()) {
            contextBlock.append("- Имя пользователя: ").append(userName.trim()).append("\n");
        }
        for (Map.Entry<String, String> entry : userCtx.getProfileData().entrySet()) {
            if ("userName".equals(entry.getKey())) {
                continue;
            }
            contextBlock.append("- ").append(translateProfileKey(entry.getKey())).append(": ").append(entry.getValue()).append("\n");
        }
        contextBlock.append("\n").append(USER_INSTRUCTIONS_HEADER).append("\n");
        if (userCtx.getCustomInstructions().isEmpty()) {
            contextBlock.append("- не заданы\n");
        } else {
            for (String instruction : userCtx.getCustomInstructions()) {
                contextBlock.append("- ").append(instruction).append("\n");
            }
        }

        contextBlock.append("\n").append(USER_CONTEXT_PRIORITY_NOTE).append("\n\n");

        contextBlock.append("=== ПРАВИЛА БЕЗОПАСНОСТИ И НАВИГАЦИИ В HRM ===\n");
        contextBlock.append("1. СТРОГИЙ ЗАПРЕТ НА УДАЛЕНИЕ: Тебе категорически запрещено удалять любые данные из базы данных (любые операции DELETE, DROP, TRUNCATE, soft-delete, удаление записей) для любого пользователя системы. На любые запросы об удалении отвечай вежливым отказом и пояснением, что операции удаления в чате строго заблокированы политикой безопасности.\n");
        contextBlock.append("2. НАВИГАЦИЯ: Если требуется дать ссылку на карточку в системе HRM, используй формат ссылок: [Текст](hrm://vacancy/<UUID>), [Текст](hrm://candidate/<UUID>), [Текст](hrm://cv/<UUID>), [Текст](hrm://interaction/<UUID>).\n");
        contextBlock.append("3. СТРОГИЙ ЗАПРЕТ ВЫВОДА ТЕХНИЧЕСКИХ ID: Категорически запрещено выводить пользователю технические идентификаторы (UUID / ID вида \"8a0f5f9c-6a32-4e6a-a2c9-672870000001\"). Пользователь никогда не должен видеть сырые UUID! Вместо ID всегда используй только человекочитаемые наименования (название вакансии, ФИО кандидата, название компании, должность) и при необходимости оформляй их ссылками: [Название Вакансии](hrm://vacancy/<UUID>) или [ФИО Кандидата](hrm://candidate/<UUID>).\n");
        contextBlock.append("4. ИНТЕГРАЦИЯ С ЯНДЕКС.КАЛЕНДАРЕМ И ТЕЛЕМОСТОМ (YANDEX 360):\n");
        contextBlock.append("   - Четко различай личный календарь и календарь собеседований с заказчиком:\n");
        contextBlock.append("     * Фразы «в моем календаре», «в личном календаре», «мне» -> Личный календарь пользователя.\n");
        contextBlock.append("     * Фразы «в календаре собеседования с заказчиком», «собеседование у заказчика», «в календаре с заказчиком» -> Корпоративный календарь «Hunttech у заказчика».\n");
        contextBlock.append("   - Для любых видеовстреч создавай ссылку на Яндекс Телемост, информируй о записи и AI-конспекте встречи.\n");
        contextBlock.append("   - Часовой пояс по умолчанию: Europe/Saratov (UTC+4, на 1 час вперед относительно Москвы).\n\n");
        contextBlock.append("5. СТАНДАРТ И РЕГЛАМЕНТ ОТКРЫТИЯ ВАКАНСИЙ (HUNTTECH VACANCY OPENING):\n");
        contextBlock.append("   - При создании/открытии вакансий из текстов или ссылок (SSP Soft, hh.ru и др.):\n");
        contextBlock.append("     * Единственный источник истины — оригинальный текст вакансии заказчика.\n");
        contextBlock.append("     * Если данные отсутствуют: строго использовать «НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.»\n");
        contextBlock.append("     * Если данные противоречивы: строго «УКАЗАНЫ ПРОТИВОРЕЧИВЫЕ ДАННЫЕ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.»\n");
        contextBlock.append("     * Запрещено выдумывать зарплаты, ставки, бонусы, контакты или условия компании.\n");
        contextBlock.append("     * Обязательно формируются 4 ключевых артефакта: стандартизированное описание (14 разделов на русском и перевод на английский), чеклист скрининга (must-have), карта поиска сорсера, план продающего собеседования.\n");
        contextBlock.append("     * Дублирование полей: чеклист пишется в interview_checklist и exercise; карта поиска — в search_map и memo_for_interview; план интервью — в interview_plan и template_letter.\n");
        contextBlock.append("     * Оформление по умолчанию: аутстаффинг (0). Приоритет по умолчанию: NORMAL (2), при слове «пауза» — PAUSED (0).\n");
        contextBlock.append("     * Локация для удаленки по умолчанию: «Регионы РФ (МСК +/- 2 часа)».\n");
        contextBlock.append("     * Расчет ставок кандидатов: строго по сетке OutstaffingRates (точный шаг либо ближайший меньший). При отсутствии ставки клиента — salary_candidate_request=true.\n\n");

        contextBlock.append("=== Запрос пользователя ===\n");
        contextBlock.append(userMessage);

        return contextBlock.toString();
    }

    private String translateProfileKey(String key) {
        if (key == null) {
            return "";
        }
        switch (key) {
            case "preferredLanguage": return "Предпочитаемый язык ответов";
            case "responseDetailLevel": return "Уровень детализации ответов";
            case "communicationStyle": return "Стиль общения";
            case "terminologyLevel": return "Уровень терминологии";
            case "preferredAnswerStructure": return "Предпочитаемая структура ответов";
            case "communicationConstraints": return "Ограничения в общении";
            case "currentPosition": return "Должность";
            case "functionalRole": return "Функциональная роль";
            case "seniorityLevel": return "Уровень квалификации (Seniority)";
            case "professionalExperienceYears": return "Опыт работы (лет)";
            case "recruitingExperienceYears": return "Опыт в рекрутинге (лет)";
            case "candidateLevels": return "Уровни кандидатов";
            case "aboutMe": return "О себе";
            case "currentResponsibilities": return "Текущие обязанности";
            case "decisionPriorities": return "Приоритеты принятия решений";
            case "targetRoles": return "Целевые роли";
            case "hiringGeographies": return "Географии найма";
            case "clientAndProjectContext": return "Контекст клиентов и проектов";
            case "domainExpertise": return "Экспертиза";
            case "industries": return "Отрасли";
            case "recruitingSpecializations": return "Специализации рекрутинга";
            case "professionalGoals": return "Профессиональные цели";
            case "professionalInterests": return "Профессиональные интересы";
            case "developmentAreas": return "Зоны развития";
            case "currentPriorities": return "Текущие приоритеты";
            case "education": return "Образование";
            case "certifications": return "Сертификаты";
            default: return key;
        }
    }

    static class HermesExecutionResult {
        String cleanedText = "";
        String sessionId = null;
        Integer promptTokens;
        Integer completionTokens;
        Integer totalTokens;
    }

    private boolean isVacancyOpeningIntent(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        String lower = message.trim().toLowerCase(Locale.ROOT);
        boolean hasVacancyKeyword = lower.matches("(?s).*(ваканси[яеиюей]|позици[яеиюей]|openposition).*");
        boolean hasActionKeyword = lower.matches("(?s).*(открой[а-я]*|создай[а-я]*|добавь[а-я]*|загрузи[а-я]*|открыть|создать|добавить|загрузить).*");
        boolean hasVacancyUrl = (lower.contains("need.ssp-soft.com") || lower.contains("hh.ru/vacancy") || lower.contains("career.habr.com"))
                && (hasVacancyKeyword || hasActionKeyword || lower.contains("http"));
        return (hasVacancyKeyword && hasActionKeyword) || hasVacancyUrl;
    }

    private boolean isManagerOrDirector(ExtUser user) {
        if (user == null) return false;
        String login = user.getLogin() != null ? user.getLogin().toLowerCase() : "";
        if ("admin".equals(login) || "alan".equals(login)) {
            return true;
        }
        if (user.getGroup() != null && user.getGroup().getName() != null) {
            String grp = user.getGroup().getName().toLowerCase();
            if (grp.contains("менедж") || grp.contains("директор") || grp.contains("руковод") || grp.contains("управлен") || grp.contains("admin")) {
                return true;
            }
        }
        if (user.getUserRoles() != null) {
            for (com.haulmont.cuba.security.entity.UserRole ur : user.getUserRoles()) {
                if (ur.getRole() != null && ur.getRole().getName() != null) {
                    String r = ur.getRole().getName().toLowerCase();
                    if (r.contains("manager") || r.contains("менедж") || r.contains("director") || r.contains("директор") || r.contains("admin")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private HermesChatResponse handleVacancyOpeningInHermes(LlmChatConversation conversation, String message, ExtUser currentUser, int maxSeq, long startTime) {
        log.info("[HERMES_CHAT_VACANCY] Инициировано открытие вакансии в Hermes Chat пользователем '{}'", currentUser.getLogin());
        String responseText;

        if (!isManagerOrDirector(currentUser)) {
            log.warn("[HERMES_CHAT_VACANCY] Отказ в доступе: пользователь '{}' не входит в группу Менеджеры/Директор", currentUser.getLogin());
            responseText = "⚠️ **Ограничение доступа**\n\nФункция открытия вакансий через LLM-чат Hermes доступна исключительно пользователям из групп **«Менеджеры»** и **«Директор»**.\nУ вашей учетной записи недостаточно полномочий для выполнения этой операции.";
        } else if (smartOpenPositionIngestService == null) {
            responseText = "❌ Сервис умного открытия вакансий (SmartOpenPositionIngestService) недоступен.";
        } else {
            try {
                // Извлекаем URL если есть
                Pattern urlPattern = Pattern.compile("(?i)(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)");
                Matcher urlMatcher = urlPattern.matcher(message);
                String targetInput = message;
                if (urlMatcher.find()) {
                    targetInput = urlMatcher.group(1).trim();
                    log.info("[HERMES_CHAT_VACANCY] Извлечен URL вакансии из сообщения: {}", targetInput);
                } else {
                    targetInput = message.replaceAll("(?i)^(?:открой|создай|добавь|загрузи)(?:\\s+пожалуйста)?\\s+вакансию[:\\s]*", "").trim();
                }

                SmartOpenPositionParsedData parsedData = smartOpenPositionIngestService.parseVacancyText(targetInput);
                if (parsedData == null || parsedData.getVacansyName() == null || parsedData.getVacansyName().trim().isEmpty()) {
                    responseText = "⚠️ Не удалось распознать данные вакансии из предоставленного текста или ссылки. Пожалуйста, проверьте ссылку или описание.";
                } else {
                    // Проверка на статус паузы в запросе
                    String lowerMsg = message.toLowerCase(Locale.ROOT);
                    if (lowerMsg.matches("(?s).*(пауз|pause).*")) {
                        parsedData.setPriority(OpenPositionPriority.PAUSED.getId()); // 0
                    } else {
                        parsedData.setPriority(OpenPositionPriority.NORMAL.getId()); // 2
                    }
                    parsedData.setSignDraft(false); // В чате вакансия открывается сразу по стандарту регламента

                    // Проверка дубликатов (в первую очередь по точной vacansyID)
                    OpenPosition duplicate = smartOpenPositionIngestService.findDuplicate(parsedData);
                    if (duplicate != null) {
                        String dupTitle = duplicate.getVacansyName() != null ? duplicate.getVacansyName() : "Вакансия";
                        String dupLink = "[" + dupTitle + "](hrm://vacancy/" + duplicate.getId() + ")";
                        responseText = "⚠️ **Вакансия уже существует в системе (дубликат)**\n\n" +
                                "В HRM уже зарегистрирована позиция с идентичными реквизитами:\n" +
                                "• **Карточка вакансии:** " + dupLink + (duplicate.getVacansyID() != null ? " (ID заявки: " + duplicate.getVacansyID() + ")" : "") + "\n" +
                                "• **Проект:** " + (duplicate.getProjectName() != null ? duplicate.getProjectName().getProjectName() : "Не указан") + "\n" +
                                "• **Статус:** " + (Boolean.TRUE.equals(duplicate.getOpenClose()) ? "Закрыта" : "Открыта") + "\n\n" +
                                "Согласно регламенту **HuntTech Vacancy Opening**, создание повторного дубликата отменено.";
                    } else {
                        SmartOpenPositionIngestResult result = smartOpenPositionIngestService.createOpenPosition(parsedData, currentUser);
                        if (!result.isSuccess() || result.getOpenPosition() == null) {
                            responseText = "❌ Не удалось сохранить вакансию в HRM: " + result.getMessage();
                        } else {
                            OpenPosition op = result.getOpenPosition();
                            String vacTitle = op.getVacansyName() != null ? op.getVacansyName() : "Открытая вакансия";
                            String vacLink = "[" + vacTitle + "](hrm://vacancy/" + op.getId() + ")";

                            StringBuilder sb = new StringBuilder();
                            sb.append("✅ **Вакансия успешно открыта в HRM**\n\n");
                            sb.append("• **Карточка вакансии:** ").append(vacLink).append("\n");
                            if (op.getVacansyID() != null) {
                                sb.append("• **ID заявки:** `").append(op.getVacansyID()).append("`\n");
                            }
                            sb.append("• **Проект:** ").append(op.getProjectName() != null ? op.getProjectName().getProjectName() : "Не указан").append("\n");
                            sb.append("• **Специализация:** ").append(op.getPositionType() != null ? op.getPositionType().getPositionRuName() : "Не указана").append("\n");
                            sb.append("• **Грейд:** ").append(op.getGrade() != null ? op.getGrade().getGradeName() : "Не указан").append("\n");
                            sb.append("• **Формат:** ").append(op.getRemoteWork() != null && op.getRemoteWork() == 1 ? "Удаленно (РФ / МСК ±2ч)" : (op.getRemoteWork() != null && op.getRemoteWork() == 2 ? "Гибрид" : "В офисе")).append("\n");
                            sb.append("• **Оформление:** ").append(op.getRegistrationForWork() != null && op.getRegistrationForWork() == 0 ? "Аутстаффинг" : "ТК / ГПХ").append("\n");
                            sb.append("• **Приоритет:** ").append(op.getPriority() != null && op.getPriority() == 0 ? "На паузе (0)" : "Нормальный (2)").append("\n");
                            sb.append("• **Ответственный:** ").append(op.getOwner() != null ? op.getOwner().getName() : "HRM Bot").append("\n");

                            if (Boolean.TRUE.equals(op.getSalaryCandidateRequest())) {
                                sb.append("• **Доход / Ставка:** по запросу кандидата (обсуждается на собеседовании)\n");
                            } else if (op.getSalaryMin() != null || op.getSalaryMax() != null) {
                                sb.append("• **Зарплатное предложение:** ");
                                if (op.getSalaryMin() != null && op.getSalaryMax() != null) {
                                    sb.append(op.getSalaryMin()).append(" – ").append(op.getSalaryMax()).append(" руб.");
                                } else if (op.getSalaryMax() != null) {
                                    sb.append("до ").append(op.getSalaryMax()).append(" руб.");
                                } else {
                                    sb.append("от ").append(op.getSalaryMin()).append(" руб.");
                                }
                                if (op.getSalaryIE() != null) {
                                    sb.append(" (ИП до ").append(op.getSalaryIE()).append(" руб.)");
                                }
                                sb.append("\n");
                            }
                            if (op.getSalaryComment() != null && !op.getSalaryComment().trim().isEmpty()) {
                                sb.append("• **Сетка ставок:** ").append(op.getSalaryComment()).append("\n");
                            }

                            sb.append("\n📁 **Сформированные и синхронизированные артефакты:**\n");
                            sb.append("1. **Стандартизированное описание:** 14 обязательных разделов на русском (`comment`) + перевод на английский (`commentEn`).\n");
                            sb.append("2. **Must-Have чеклист:** записан в `interviewChecklist` и продублирован в `exercise`.\n");
                            sb.append("3. **Карта поиска сорсера:** записана в `searchMap` и продублирована в `memoForInterview`.\n");
                            sb.append("4. **План продающего интервью:** записан в `interviewPlan` и продублирован в `templateLetter`.\n");

                            if (parsedData.getTelegramPost() != null && !parsedData.getTelegramPost().trim().isEmpty()) {
                                sb.append("\n📢 **Готовая публикация для Telegram-канала рекрутеров:**\n```markdown\n");
                                sb.append(parsedData.getTelegramPost().trim()).append("\n```\n");
                            }

                            sb.append("\nВакансия полностью готова к работе рекрутеров и доступна в **Реестре открытых вакансий**.");
                            responseText = sb.toString();
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[HERMES_CHAT_VACANCY] Ошибка при открытии вакансии в Hermes Chat: {}", e.getMessage(), e);
                responseText = "❌ Произошла непредвиденная ошибка при открытии вакансии. Пожалуйста, проверьте текст запроса или обратитесь к администратору системы.";
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        LlmChatMessage userMsg = metadata.create(LlmChatMessage.class);
        userMsg.setConversation(conversation);
        userMsg.setRole("USER");
        userMsg.setContent(message.trim());
        userMsg.setSequenceNo(maxSeq + 1);
        userMsg.setStatus("COMPLETED");

        LlmChatMessage assistantMsg = metadata.create(LlmChatMessage.class);
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setContent(responseText);
        assistantMsg.setSequenceNo(maxSeq + 2);
        assistantMsg.setStatus("COMPLETED");
        assistantMsg.setProviderCode("hermes");
        assistantMsg.setModelName("vacancy-opening-skill");

        conversation.setLastMessageAt(new Date());
        dataManager.commit(new CommitContext(conversation, userMsg, assistantMsg));

        int promptToks = estimateTokens(message);
        int compToks = estimateTokens(responseText);
        int totalToks = promptToks + compToks;
        if (userAiQuotaService != null && totalToks > 0) {
            userAiQuotaService.recordTokenConsumption(currentUser.getId(), totalToks, "USER");
        }
        logAiCall(currentUser, conversation.getId(), duration, "hermes", "vacancy-opening-skill", "USER",
                promptToks, compToks, totalToks, BigDecimal.ZERO, "USD", "SUCCESS", null);

        HermesChatResponse hermesResp = new HermesChatResponse(conversation.getId(), responseText, null, duration);
        hermesResp.setProviderCode("hermes");
        hermesResp.setModelName("vacancy-opening-skill");
        hermesResp.setPromptTokens(promptToks);
        hermesResp.setCompletionTokens(compToks);
        hermesResp.setTotalTokens(totalToks);
        return hermesResp;
    }
}
