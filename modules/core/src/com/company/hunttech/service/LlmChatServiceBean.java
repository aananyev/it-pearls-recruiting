package com.company.hunttech.service;

import com.company.hunttech.LlmChatStreamEvent;
import com.company.hunttech.dto.HrmDataContextSnapshot;
import com.company.hunttech.dto.action.InterviewSchedulingResult;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.OpenPositionPriority;
import com.company.hunttech.entity.ai.LlmChatConversation;
import com.company.hunttech.entity.ai.LlmChatMessage;
import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.company.hunttech.entity.ai.LlmChatQuotaPeriod;
import com.company.hunttech.entity.ai.LlmChatQuotaReservation;
import com.company.hunttech.entity.ai.LlmUserQuotaOverride;
import com.company.hunttech.core.ai.AIProviderRegistry;
import com.company.hunttech.service.AiStreamListener;
import com.company.hunttech.service.AiSecuritySanitizer;
import com.haulmont.cuba.core.sys.SecurityContextAwareRunnable;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.TaskScheduler;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.dto.yandex.AiMeetingParseResult;
import com.company.hunttech.dto.yandex.YandexMeetingResult;
import com.haulmont.cuba.core.global.FluentLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware facade for the floating chat. It keeps synchronous compatibility
 * while exposing an owner-scoped polling snapshot for streaming. Provider
 * routing, profile context and admin fallback policy remain centralized in
 * AiExecutionService; provider-level cancellation is routed by the core adapter registry.
 */
@Service(LlmChatService.NAME)
public class LlmChatServiceBean implements LlmChatService {
    private static final Logger log = LoggerFactory.getLogger(LlmChatServiceBean.class);
    private static final String FUNCTION_CODE = "LLM_CHAT";
    private static final String QUERY_OWN_CONVERSATION =
            "select e from hunttech_LlmChatConversation e "
                    + "where e.id = :id and e.user.id = :userId and e.deleteTs is null";
    private static final String QUERY_HISTORY =
            "select e from hunttech_LlmChatMessage e "
                    + "where e.conversation.id = :conversationId "
                    + "and e.conversation.user.id = :userId and e.deleteTs is null "
                    + "order by e.sequenceNo asc";
    private static final String QUERY_RESERVATION_BY_REQUEST =
            "select e from hunttech_LlmChatQuotaReservation e "
                    + "where e.requestId = :requestId and e.conversation.id = :conversationId "
                    + "and e.period.user.id = :userId and e.deleteTs is null";
    private static final String QUERY_RESERVATION_BY_REQUEST_ADMIN =
            "select e from hunttech_LlmChatQuotaReservation e "
                    + "where e.requestId = :requestId and e.deleteTs is null";
    private static final String QUERY_ASSISTANT_BY_REQUEST =
            "select e from hunttech_LlmChatMessage e "
                    + "where e.conversation.id = :conversationId and e.requestId = :requestId "
                    + "and e.role = 'ASSISTANT' and e.deleteTs is null";
    private static final String QUERY_FUNCTION =
            "select e from hunttech_AiFunctionConfiguration e "
                    + "where e.code = :code and e.active = true";
    private static final String QUERY_QUOTA_PERIOD =
            "select e from hunttech_LlmChatQuotaPeriod e "
                    + "where e.user.id = :userId and e.periodStart = :periodStart and e.deleteTs is null";
    private static final String QUERY_QUOTA_OVERRIDE =
            "select e from hunttech_LlmUserQuotaOverride e "
                    + "where e.user.id = :userId and e.effectiveFrom <= :today "
                    + "and (e.effectiveTo is null or e.effectiveTo >= :today) and e.deleteTs is null "
                    + "order by e.effectiveFrom desc";
    private static final long STREAM_SESSION_TTL_MS = 10 * 60 * 1000L;

    private final ConcurrentMap<String, StreamingSession> streamingSessions = new ConcurrentHashMap<>();

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private AiExecutionService aiExecutionService;
    @Inject
    private AIProviderRegistry aiProviderRegistry;
    @Inject
    private Events events;
    @Inject
    private Security security;
    @Inject
    private HrmChatDataRetrieverService hrmChatDataRetrieverService;
    @Inject
    private AiYandexOrchestrationService aiYandexOrchestrationService;
    @Inject
    private InterviewSchedulingActionService interviewSchedulingActionService;
    @Inject
    private SmartOpenPositionIngestService smartOpenPositionIngestService;
    @Resource(name = "scheduler")
    private TaskScheduler scheduler;

    @Override
    public UUID startConversation() {
        ExtUser user = currentUser();
        LlmChatConversation conversation = metadata.create(LlmChatConversation.class);
        conversation.setUser(user);
        conversation.setStatus("ACTIVE");
        conversation.setTitle("Новый диалог");
        UUID id = dataManager.commit(conversation).getId();
        log.info("startConversation: создан диалог id={} для пользователя {}", id, user != null ? user.getLogin() : null);
        return id;
    }

    @Override
    public LlmChatResponse sendMessage(UUID conversationId, String message) {
        return sendMessage(conversationId, message, UUID.randomUUID().toString());
    }

    @Override
    public LlmChatResponse sendMessage(UUID conversationId, String message, String requestId) {
        if (message == null || message.trim().isEmpty()) {
            throw new DevelopmentException("Введите сообщение.");
        }
        if (message.length() > 32000) {
            throw new DevelopmentException("Сообщение слишком длинное: максимум 32000 символов.");
        }
        if (requestId == null || requestId.trim().isEmpty() || requestId.length() > 64) {
            throw new DevelopmentException("Некорректный идентификатор запроса.");
        }

        ExtUser user = currentUser();
        LlmChatConversation conversation = resolveConversation(conversationId, user);
        LlmChatResponse existing = resolveExistingRequest(conversation, user, requestId.trim());
        if (existing != null) {
            return existing;
        }
        Date now = new Date();
        int nextSequence = nextSequence(conversation.getId(), user.getId());
        QuotaReservationContext quota;
        try {
            quota = reserveQuota(user, conversation, message.trim(), requestId.trim());
        } catch (RuntimeException reservationFailure) {
            // A concurrent retry may win the unique requestId constraint.
            LlmChatResponse concurrent = resolveExistingRequest(conversation, user, requestId.trim());
            if (concurrent != null) {
                return concurrent;
            }
            throw reservationFailure;
        }

        LlmChatMessage userMessage = metadata.create(LlmChatMessage.class);
        userMessage.setConversation(conversation);
        userMessage.setRole("USER");
        userMessage.setContent(message.trim());
        userMessage.setSequenceNo(nextSequence);
        userMessage.setRequestId(requestId.trim());
        userMessage.setStatus("COMPLETED");
        conversation.setLastMessageAt(now);
        dataManager.commit(new CommitContext(conversation, userMessage));

        if (isCancellationRequested(quota)) {
            settleCancelledBeforeProvider(quota, userMessage);
            throw new DevelopmentException("Запрос отменён до обращения к AI-провайдеру.");
        }

        // 0. Назначение собеседования кандидату и обработка многошагового диалога
        if (interviewSchedulingActionService != null) {
            InterviewSchedulingResult schedRes = null;
            try {
                schedRes = interviewSchedulingActionService.handleSchedulingAction(conversation.getId(), message.trim(), user, requestId.trim());
            } catch (Exception ex) {
                log.error("Сбой обработки сценария назначения собеседования: {}", ex.getMessage(), ex);
            }
            if (schedRes != null && schedRes.getMessage() != null) {
                String responseText = schedRes.getMessage();
                LlmChatMessage assistantMessage = metadata.create(LlmChatMessage.class);
                assistantMessage.setConversation(conversation);
                assistantMessage.setRole("ASSISTANT");
                assistantMessage.setContent(responseText);
                assistantMessage.setSequenceNo(nextSequence + 1);
                assistantMessage.setRequestId(requestId.trim());
                assistantMessage.setStatus("COMPLETED");
                assistantMessage.setProviderCode("yandex");
                assistantMessage.setModelName("caldav-telemost");
                conversation.setLastMessageAt(new Date());
                dataManager.commit(new CommitContext(conversation, assistantMessage));
                settleObservedUsage(quota, 50, "yandex-caldav");
                return new LlmChatResponse(conversation.getId(), responseText, "yandex", "caldav-telemost", null);
            }
        }

        // 1. Попытка бронирования встречи в календаре CalDAV (универсальный fallback для произвольных тем)
        if (aiYandexOrchestrationService != null && aiYandexOrchestrationService.isMeetingBookingIntent(message.trim())) {
            AiMeetingParseResult parseResult = aiYandexOrchestrationService.parseMeetingIntent(message.trim(), user.getId());
            if (parseResult.isIntentDetected() && AiYandexOrchestrationService.containsBookingVerb(message)) {
                String responseText;
                boolean success = false;
                try {
                    YandexMeetingResult booking = aiYandexOrchestrationService.executeMeetingBooking(user.getId(), parseResult);
                    if (booking.isSuccess()) {
                        responseText = booking.getMessage();
                        success = true;
                    } else {
                        responseText = AiYandexOrchestrationService.formatCalDavFailureMessage(booking.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Сбой бронирования встречи CalDAV: {}", e.getMessage(), e);
                    responseText = AiYandexOrchestrationService.formatCalDavFailureMessage(e.getMessage());
                }
                LlmChatMessage assistantMessage = metadata.create(LlmChatMessage.class);
                assistantMessage.setConversation(conversation);
                assistantMessage.setRole("ASSISTANT");
                assistantMessage.setContent(responseText);
                assistantMessage.setSequenceNo(nextSequence + 1);
                assistantMessage.setRequestId(requestId.trim());
                assistantMessage.setStatus("COMPLETED");
                assistantMessage.setProviderCode("yandex");
                assistantMessage.setModelName("caldav-telemost");
                conversation.setLastMessageAt(new Date());
                dataManager.commit(new CommitContext(conversation, assistantMessage));
                if (success) {
                    settleObservedUsage(quota, 50, "yandex-caldav");
                } else {
                    releaseFailedReservation(quota, "yandex-caldav");
                }
                return new LlmChatResponse(conversation.getId(), responseText, "yandex", "caldav-telemost", null);
            }
        }

        // 2. Чтение / поиск событий в Яндекс-Календаре через CalDAV
        if (aiYandexOrchestrationService != null && aiYandexOrchestrationService.isCalendarQueryIntent(message.trim())) {
            String responseText;
            boolean success = false;
            try {
                responseText = aiYandexOrchestrationService.getCalendarScheduleSummary(user != null ? user.getId() : null, message.trim());
                success = true;
            } catch (Exception e) {
                log.error("Сбой чтения событий Яндекс-Календаря: {}", e.getMessage(), e);
                responseText = "⚠️ **Не удалось прочитать события Яндекс-Календаря.**\n\n" +
                        "Пожалуйста, проверьте подключение и токен доступа в окне Настроек (вкладка **«Яндекс 360»**).";
            }
            LlmChatMessage assistantMessage = metadata.create(LlmChatMessage.class);
            assistantMessage.setConversation(conversation);
            assistantMessage.setRole("ASSISTANT");
            assistantMessage.setContent(responseText);
            assistantMessage.setSequenceNo(nextSequence + 1);
            assistantMessage.setRequestId(requestId.trim());
            assistantMessage.setStatus("COMPLETED");
            assistantMessage.setProviderCode("yandex");
            assistantMessage.setModelName("caldav-calendar");
            conversation.setLastMessageAt(new Date());
            dataManager.commit(new CommitContext(conversation, assistantMessage));
            if (success) {
                settleObservedUsage(quota, 25, "yandex-caldav-query");
            } else {
                releaseFailedReservation(quota, "yandex-caldav-query");
            }
            return new LlmChatResponse(conversation.getId(), responseText, "yandex", "caldav-calendar", null);
        }

        if (isVacancyOpeningIntent(message.trim())) {
            String vacancyResponse = handleVacancyOpeningIntent(user, message.trim());
            settleObservedUsage(quota, 100, "smart-vacancy-ingest");
            LlmChatMessage assistantMessage = metadata.create(LlmChatMessage.class);
            assistantMessage.setConversation(conversation);
            assistantMessage.setRole("ASSISTANT");
            assistantMessage.setContent(vacancyResponse);
            assistantMessage.setSequenceNo(nextSequence + 1);
            assistantMessage.setRequestId(requestId.trim());
            assistantMessage.setStatus("COMPLETED");
            assistantMessage.setProviderCode("hrm-system");
            assistantMessage.setModelName("smart-vacancy-ingest");
            conversation.setLastMessageAt(new Date());
            dataManager.commit(new CommitContext(conversation, assistantMessage));
            return new LlmChatResponse(conversation.getId(), vacancyResponse, "hrm-system", "smart-vacancy-ingest", null);
        }

        TimeZone timeZone = resolveEffectiveTimeZone(user);
        Locale locale = resolveEffectiveLocale(user);
        String runtimeContext = LlmChatContextBuilder.buildRuntimeContext(new Date(), timeZone, locale);

        String messageWithHistory = buildMessageWithHistory(conversation, user, nextSequence, message.trim(), runtimeContext);
        HrmDataContextSnapshot snapshot = hrmChatDataRetrieverService.retrieveContextForMessage(message.trim());

        Map<String, Object> context = new HashMap<>();
        context.put("runtimeContext", runtimeContext);
        context.put("message", messageWithHistory);
        if (!snapshot.isEmpty()) {
            context.put("message", messageWithHistory + "\n\n" + snapshot.getFormattedContext());
            context.put("hrmContext", snapshot.getFormattedContext());
        }
        context.put("callerSource", "LlmChatService");
        context.put("requestId", requestId.trim());
        AiExecutionResult result;
        try {
            result = aiExecutionService.executeText(FUNCTION_CODE, context);
        } catch (RuntimeException failure) {
            log.error("sendMessage: ошибка вызова AI для convId={}, requestId={}: {}",
                    conversationId, requestId, failure.getMessage(), failure);
            markQuotaPending(quota);
            String safeMessage = AiSecuritySanitizer.sanitizeError(failure);
            throw new DevelopmentException(safeMessage == null
                    ? "Ошибка выполнения запроса к AI."
                    : safeMessage);
        }
        if (result == null || result.getText() == null || result.getText().trim().isEmpty()) {
            markQuotaPending(quota);
            throw new DevelopmentException("AI-провайдер вернул пустой ответ.");
        }
        if (settleQuota(quota, result)) {
            userMessage.setStatus("CANCELLED");
            dataManager.commit(userMessage);
            throw new DevelopmentException("Запрос отменён. Фактическое usage учтено; ответ не добавлен в историю.");
        }

        LlmChatMessage assistantMessage = metadata.create(LlmChatMessage.class);
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent(result.getText());
        assistantMessage.setSequenceNo(nextSequence + 1);
        assistantMessage.setRequestId(requestId.trim());
        assistantMessage.setStatus("COMPLETED");
        assistantMessage.setProviderCode(result.getProviderCode());
        assistantMessage.setModelName(result.getModelName());
        assistantMessage.setProviderRequestId(result.getProviderRequestId());
        assistantMessage.setCredentialOwner(result.getCredentialOwner() == null
                ? null : result.getCredentialOwner().name());
        conversation.setLastMessageAt(new Date());
        dataManager.commit(new CommitContext(conversation, assistantMessage));

        return new LlmChatResponse(conversation.getId(), result.getText(), result.getProviderCode(),
                result.getModelName(), result.getCredentialOwner());
    }

    @Override
    public LlmChatStreamState startStreaming(UUID conversationId, String message, String requestId) {
        validateMessage(message);
        validateRequestId(requestId);
        String normalizedRequestId = requestId.trim();
        ExtUser user = currentUser();
        LlmChatConversation conversation = resolveConversation(conversationId, user);
        cleanupStreamingSessions();

        StreamingSession active = streamingSessions.get(normalizedRequestId);
        if (active != null) {
            active.assertOwner(user.getId(), conversation.getId());
            return active.snapshot();
        }

        LlmChatStreamState existingState = resolveExistingStreamingState(conversation, user, normalizedRequestId);
        if (existingState != null) {
            return existingState;
        }

        Date now = new Date();
        int nextSequence = nextSequence(conversation.getId(), user.getId());
        QuotaReservationContext quota = reserveQuota(user, conversation, message.trim(), normalizedRequestId);
        LlmChatMessage userMessage = metadata.create(LlmChatMessage.class);
        userMessage.setConversation(conversation);
        userMessage.setRole("USER");
        userMessage.setContent(message.trim());
        userMessage.setSequenceNo(nextSequence);
        userMessage.setRequestId(normalizedRequestId);
        userMessage.setStatus("COMPLETED");
        conversation.setLastMessageAt(now);
        dataManager.commit(new CommitContext(conversation, userMessage));

        StreamingSession session = new StreamingSession(user.getId(), conversation.getId(), normalizedRequestId,
                quota, userMessage, nextSequence);
        streamingSessions.put(normalizedRequestId, session);
        if (isCancellationRequested(quota)) {
            settleCancelledBeforeProvider(quota, userMessage);
            session.complete("CANCELLED", "Запрос отменён до обращения к AI.");
            publishStreamEvent(session, true);
            return session.snapshot();
        }

        log.info("startStreaming: запуск стриминга AI для convId={}, user={}, requestId={}, messageLen={}",
                conversationId, user != null ? user.getLogin() : null, normalizedRequestId, message.trim().length());
        scheduler.schedule(new SecurityContextAwareRunnable(() -> executeStreaming(session)), new Date());
        return session.snapshot();
    }

    @Override
    public LlmChatStreamState pollStreaming(UUID conversationId, String requestId) {
        validateRequestId(requestId);
        String normalizedRequestId = requestId.trim();
        ExtUser user = currentUser();
        LlmChatConversation conversation = resolveConversation(conversationId, user);
        cleanupStreamingSessions();

        StreamingSession session = streamingSessions.get(normalizedRequestId);
        if (session != null) {
            session.assertOwner(user.getId(), conversation.getId());
            return session.snapshot();
        }

        LlmChatMessage assistant = dataManager.load(LlmChatMessage.class)
                .query(QUERY_ASSISTANT_BY_REQUEST)
                .parameter("conversationId", conversation.getId())
                .parameter("requestId", normalizedRequestId)
                .view("llm-chat-message-view")
                .optional().orElse(null);
        if (assistant != null) {
            return new LlmChatStreamState(conversation.getId(), normalizedRequestId, assistant.getContent(),
                    "COMPLETED", null, true);
        }
        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .query(QUERY_RESERVATION_BY_REQUEST)
                .parameter("requestId", normalizedRequestId)
                .parameter("conversationId", conversation.getId())
                .parameter("userId", user.getId())
                .view("llm-chat-quota-reservation-view")
                .optional().orElseThrow(() -> new DevelopmentException("Потоковый запрос не найден."));
        String status = reservation.getStatus();
        if ("UNKNOWN_PENDING".equals(status)) {
            return new LlmChatStreamState(conversation.getId(), normalizedRequestId, "", status,
                    "Результат запроса не подтверждён; резерв квоты помечен как уточняющий.", true);
        }
        boolean completed = "CANCELLED".equals(status) || "RELEASED".equals(status);
        return new LlmChatStreamState(conversation.getId(), normalizedRequestId, "", status,
                completed ? "Запрос завершён без ответа." : null, completed);
    }

    private void executeStreaming(StreamingSession session) {
        LlmChatConversation conversation = dataManager.load(LlmChatConversation.class)
                .id(session.conversationId).view("llm-chat-conversation-view").optional().orElse(null);
        ExtUser user = dataManager.load(ExtUser.class)
                .id(session.userId).view("_minimal").optional().orElse(null);

        String rawContent = session.userMessage != null && session.userMessage.getContent() != null
                ? session.userMessage.getContent().trim() : "";

        // 0. Назначение собеседования кандидату и обработка многошагового диалога
        if (interviewSchedulingActionService != null) {
            InterviewSchedulingResult schedRes = null;
            try {
                schedRes = interviewSchedulingActionService.handleSchedulingAction(session.conversationId, rawContent, user, session.requestId);
            } catch (Exception ex) {
                log.error("Сбой стриминга сценария назначения собеседования: {}", ex.getMessage(), ex);
            }
            if (schedRes != null && schedRes.getMessage() != null) {
                String responseText = schedRes.getMessage();
                session.append(responseText);
                LlmChatMessage assistantMessage = metadata.create(LlmChatMessage.class);
                assistantMessage.setConversation(conversation);
                assistantMessage.setRole("ASSISTANT");
                assistantMessage.setContent(responseText);
                assistantMessage.setSequenceNo(session.nextSequence + 1);
                assistantMessage.setRequestId(session.requestId);
                assistantMessage.setStatus("COMPLETED");
                assistantMessage.setProviderCode("yandex");
                assistantMessage.setModelName("caldav-telemost");
                conversation.setLastMessageAt(new Date());
                dataManager.commit(new CommitContext(conversation, assistantMessage));
                settleObservedUsage(session.quota, 50, "yandex-caldav");
                session.complete("COMPLETED", null);
                publishStreamEvent(session, true);
                return;
            }
        }

        // 1. Попытка бронирования встречи в календаре CalDAV
        if (aiYandexOrchestrationService != null && aiYandexOrchestrationService.isMeetingBookingIntent(rawContent)) {
            AiMeetingParseResult parseResult = aiYandexOrchestrationService.parseMeetingIntent(rawContent, user != null ? user.getId() : null);
            if (parseResult.isIntentDetected() && AiYandexOrchestrationService.containsBookingVerb(rawContent)) {
                String responseText;
                boolean success = false;
                try {
                    YandexMeetingResult booking = aiYandexOrchestrationService.executeMeetingBooking(user != null ? user.getId() : null, parseResult);
                    if (booking.isSuccess()) {
                        responseText = booking.getMessage();
                        success = true;
                    } else {
                        responseText = AiYandexOrchestrationService.formatCalDavFailureMessage(booking.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Сбой стриминга бронирования встречи CalDAV: {}", e.getMessage(), e);
                    responseText = AiYandexOrchestrationService.formatCalDavFailureMessage(e.getMessage());
                }
                session.append(responseText);
                LlmChatMessage assistantMessage = metadata.create(LlmChatMessage.class);
                assistantMessage.setConversation(conversation);
                assistantMessage.setRole("ASSISTANT");
                assistantMessage.setContent(responseText);
                assistantMessage.setSequenceNo(session.nextSequence + 1);
                assistantMessage.setRequestId(session.requestId);
                assistantMessage.setStatus("COMPLETED");
                assistantMessage.setProviderCode("yandex");
                assistantMessage.setModelName("caldav-telemost");
                conversation.setLastMessageAt(new Date());
                dataManager.commit(new CommitContext(conversation, assistantMessage));
                if (success) {
                    settleObservedUsage(session.quota, 50, "yandex-caldav");
                } else {
                    releaseFailedReservation(session.quota, "yandex-caldav");
                }
                session.complete("COMPLETED", null);
                publishStreamEvent(session, true);
                return;
            }
        }

        // 2. Чтение / поиск событий в Яндекс-Календаре через CalDAV
        if (aiYandexOrchestrationService != null && aiYandexOrchestrationService.isCalendarQueryIntent(rawContent)) {
            String responseText;
            boolean success = false;
            try {
                responseText = aiYandexOrchestrationService.getCalendarScheduleSummary(user != null ? user.getId() : null, rawContent);
                success = true;
            } catch (Exception e) {
                log.error("Сбой стриминга чтения событий Яндекс-Календаря: {}", e.getMessage(), e);
                responseText = "⚠️ **Не удалось прочитать события Яндекс-Календаря.**\n\n" +
                        "Пожалуйста, проверьте подключение и токен доступа в окне Настроек (вкладка **«Яндекс 360»**).";
            }
            session.append(responseText);
            LlmChatMessage assistantMessage = metadata.create(LlmChatMessage.class);
            assistantMessage.setConversation(conversation);
            assistantMessage.setRole("ASSISTANT");
            assistantMessage.setContent(responseText);
            assistantMessage.setSequenceNo(session.nextSequence + 1);
            assistantMessage.setRequestId(session.requestId);
            assistantMessage.setStatus("COMPLETED");
            assistantMessage.setProviderCode("yandex");
            assistantMessage.setModelName("caldav-calendar");
            conversation.setLastMessageAt(new Date());
            dataManager.commit(new CommitContext(conversation, assistantMessage));
            if (success) {
                settleObservedUsage(session.quota, 25, "yandex-caldav-query");
            } else {
                releaseFailedReservation(session.quota, "yandex-caldav-query");
            }
            session.complete("COMPLETED", null);
            publishStreamEvent(session, true);
            return;
        }

        if (isVacancyOpeningIntent(session.userMessage.getContent())) {
            String vacancyResponse = handleVacancyOpeningIntent(user, session.userMessage.getContent());
            session.append(vacancyResponse);
            settleObservedUsage(session.quota, 100, "smart-vacancy-ingest");
            LlmChatMessage assistantMessage = metadata.create(LlmChatMessage.class);
            assistantMessage.setConversation(conversation);
            assistantMessage.setRole("ASSISTANT");
            assistantMessage.setContent(vacancyResponse);
            assistantMessage.setSequenceNo(session.nextSequence + 1);
            assistantMessage.setRequestId(session.requestId);
            assistantMessage.setStatus("COMPLETED");
            assistantMessage.setProviderCode("hrm-system");
            assistantMessage.setModelName("smart-vacancy-ingest");
            conversation.setLastMessageAt(new Date());
            dataManager.commit(new CommitContext(conversation, assistantMessage));
            session.complete("COMPLETED", null);
            publishStreamEvent(session, true);
            return;
        }

        TimeZone timeZone = resolveEffectiveTimeZone(user);
        Locale locale = resolveEffectiveLocale(user);
        String runtimeContext = LlmChatContextBuilder.buildRuntimeContext(new Date(), timeZone, locale);

        String messageWithHistory = buildMessageWithHistory(conversation, user, session.nextSequence, session.userMessage.getContent(), runtimeContext);
        HrmDataContextSnapshot snapshot = hrmChatDataRetrieverService.retrieveContextForMessage(session.userMessage.getContent());

        Map<String, Object> context = new HashMap<>();
        context.put("runtimeContext", runtimeContext);
        context.put("message", messageWithHistory);
        if (!snapshot.isEmpty()) {
            context.put("message", messageWithHistory + "\n\n" + snapshot.getFormattedContext());
            context.put("hrmContext", snapshot.getFormattedContext());
        }
        context.put("callerSource", "LlmChatService.streaming");
        context.put("requestId", session.requestId);
        boolean quotaSettled = false;
        try {
            AiExecutionResult result = aiExecutionService.executeTextStreaming(FUNCTION_CODE, context,
                    new AiStreamListener() {
                        @Override
                        public void onDelta(String text) {
                            session.append(text);
                            publishStreamEvent(session, false);
                        }

                        @Override
                        public void onProviderRequestId(String providerRequestId) {
                            session.setProviderRequestId(providerRequestId);
                        }

                        @Override
                        public void onUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
                            session.setObservedTotalTokens(totalTokens);
                        }
                    });
            if (result == null || result.getText() == null || result.getText().trim().isEmpty()) {
                settleFailedQuota(session);
                session.complete("ERROR", "AI-провайдер вернул пустой ответ.");
                publishStreamEvent(session, true);
                return;
            }
            if (settleQuota(session.quota, result)) {
                quotaSettled = true;
                session.userMessage.setStatus("CANCELLED");
                dataManager.commit(session.userMessage);
                session.complete("CANCELLED", "Запрос отменён. Фактическое usage учтено; ответ не добавлен в историю.");
                publishStreamEvent(session, true);
                return;
            }
            quotaSettled = true;

            LlmChatMessage assistantMessage = metadata.create(LlmChatMessage.class);
            assistantMessage.setConversation(dataManager.load(LlmChatConversation.class)
                    .id(session.conversationId).view("llm-chat-conversation-view").one());
            assistantMessage.setRole("ASSISTANT");
            assistantMessage.setContent(result.getText());
            assistantMessage.setSequenceNo(session.nextSequence + 1);
            assistantMessage.setRequestId(session.requestId);
            assistantMessage.setStatus("COMPLETED");
            assistantMessage.setProviderCode(result.getProviderCode());
            assistantMessage.setModelName(result.getModelName());
            assistantMessage.setProviderRequestId(result.getProviderRequestId());
            assistantMessage.setCredentialOwner(result.getCredentialOwner() == null
                    ? null : result.getCredentialOwner().name());
            dataManager.commit(assistantMessage);
            log.info("executeStreaming: успешно завершён ответ AI для convId={}, requestId={}, tokens={}",
                    session.conversationId, session.requestId, result.getTotalTokens());
            session.complete("COMPLETED", null);
            publishStreamEvent(session, true);
        } catch (RuntimeException failure) {
            log.error("executeStreaming: сбой выполнения AI стриминга для convId={}, requestId={}: {}",
                    session.conversationId, session.requestId, failure.getMessage(), failure);
            if (!quotaSettled) {
                try {
                    settleFailedQuota(session, failure);
                } catch (RuntimeException reconciliationFailure) {
                    // Preserve the original user-facing failure; the reservation is
                    // still visible to the administrator for manual reconciliation.
                }
            }
            String originalMessage = failure.getMessage();
            String safeMessage = AiSecuritySanitizer.sanitizeError(failure);
            session.complete(isCancellationMessage(originalMessage) ? "CANCELLED" : "ERROR",
                    safeMessage == null ? "Ошибка выполнения запроса к AI." : safeMessage);
            publishStreamEvent(session, true);
        }
    }

    private void publishStreamEvent(StreamingSession session, boolean completed) {
        try {
            events.publish(new LlmChatStreamEvent(this, session.userId, session.conversationId,
                    session.requestId, completed));
        } catch (RuntimeException ignored) {
            // Push is an optimization; polling remains the recovery transport.
        }
    }

    private void validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new DevelopmentException("Введите сообщение.");
        }
        if (message.length() > 32000) {
            throw new DevelopmentException("Сообщение слишком длинное: максимум 32000 символов.");
        }
    }

    private void validateRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty() || requestId.length() > 64) {
            throw new DevelopmentException("Некорректный идентификатор запроса.");
        }
    }

    private boolean isCancellationMessage(String message) {
        return message != null && (message.contains("Запрос отменён") || message.contains("AI-запрос отменён"));
    }

    private void cleanupStreamingSessions() {
        long threshold = System.currentTimeMillis() - STREAM_SESSION_TTL_MS;
        streamingSessions.entrySet().removeIf(entry -> entry.getValue().isFinishedBefore(threshold));
    }

    private void settleFailedQuota(StreamingSession session) {
        settleFailedQuota(session, null);
    }

    private void settleFailedQuota(StreamingSession session, Throwable failure) {
        if (session.getObservedTotalTokens() != null) {
            settleObservedUsage(session.quota, session.getObservedTotalTokens(), session.getProviderRequestId());
        } else if (isNonConsumingError(failure)) {
            releaseFailedReservation(session.quota, session.getProviderRequestId());
        } else {
            markQuotaPending(session.quota, session.getProviderRequestId());
        }
    }

    private boolean isNonConsumingError(Throwable failure) {
        if (failure == null) {
            return false;
        }
        String msg = failure.getMessage();
        if (msg == null) {
            return false;
        }
        return msg.contains("HTTP 401") || msg.contains("HTTP 402")
                || msg.contains("HTTP 400") || msg.contains("HTTP 403")
                || msg.contains("HTTP 404") || msg.contains("Insufficient Balance")
                || msg.contains("invalid_api_key") || msg.contains("Incorrect API key")
                || msg.contains("Персональный API-ключ не настроен")
                || msg.contains("не настроено активное корпоративное подключение");
    }

    private void releaseFailedReservation(QuotaReservationContext context, String providerRequestId) {
        LlmChatQuotaPeriod period = dataManager.load(LlmChatQuotaPeriod.class)
                .id(context.periodId).view("llm-chat-quota-period-view").one();
        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .id(context.reservationId).view("llm-chat-quota-reservation-view").one();
        period.setReservedTokens(Math.max(0, safeInt(period.getReservedTokens()) - context.reservedTokens));
        reservation.setSettledTokens(0);
        reservation.setStatus("RELEASED");
        reservation.setProviderRequestId(providerRequestId);
        dataManager.commit(new CommitContext(period, reservation));
    }

    private LlmChatStreamState resolveExistingStreamingState(LlmChatConversation conversation,
                                                              ExtUser user, String requestId) {
        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .query(QUERY_RESERVATION_BY_REQUEST)
                .parameter("requestId", requestId)
                .parameter("conversationId", conversation.getId())
                .parameter("userId", user.getId())
                .view("llm-chat-quota-reservation-view")
                .optional().orElse(null);
        if (reservation == null) {
            return null;
        }
        String status = reservation.getStatus();
        if ("RESERVED".equals(status) || "CANCEL_REQUESTED".equals(status)) {
            return new LlmChatStreamState(conversation.getId(), requestId, "", status, null, false);
        }
        if ("UNKNOWN_PENDING".equals(status)) {
            return new LlmChatStreamState(conversation.getId(), requestId, "", status,
                    "Результат запроса не подтверждён; резерв квоты помечен как уточняющий.", true);
        }
        LlmChatMessage assistant = dataManager.load(LlmChatMessage.class)
                .query(QUERY_ASSISTANT_BY_REQUEST)
                .parameter("conversationId", conversation.getId())
                .parameter("requestId", requestId)
                .view("llm-chat-message-view")
                .optional().orElse(null);
        if (assistant == null) {
            throw new DevelopmentException("Результат потокового запроса сохранён, но ещё не восстановлен в истории.");
        }
        return new LlmChatStreamState(conversation.getId(), requestId, assistant.getContent(),
                "COMPLETED", null, true);
    }

    @Override
    public void cancelMessage(UUID conversationId, String requestId) {
        if (requestId == null || requestId.trim().isEmpty() || requestId.length() > 64) {
            throw new DevelopmentException("Некорректный идентификатор запроса.");
        }
        ExtUser user = currentUser();
        LlmChatConversation conversation = resolveConversation(conversationId, user);
        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .query(QUERY_RESERVATION_BY_REQUEST)
                .parameter("requestId", requestId.trim())
                .parameter("conversationId", conversation.getId())
                .parameter("userId", user.getId())
                .view("llm-chat-quota-reservation-view")
                .optional()
                .orElseThrow(() -> new DevelopmentException("Запрос не найден или уже завершён."));
        if ("RESERVED".equals(reservation.getStatus())) {
            reservation.setStatus("CANCEL_REQUESTED");
            dataManager.commit(reservation);
            // The registry interrupts the active adapter connection; if it has
            // not been opened yet, the provider observes the cancelled marker
            // when it registers the request.
            aiProviderRegistry.cancelRequest(requestId.trim());
        }
    }

    @Override
    public void reconcileUnknown(String requestId, Integer actualTokens, boolean providerCharged) {
        requireQuotaReconciliationPermission();
        if (requestId == null || requestId.trim().isEmpty() || requestId.length() > 64) {
            throw new DevelopmentException("Некорректный идентификатор запроса.");
        }
        if (actualTokens == null || actualTokens < 0
                || (!providerCharged && actualTokens != 0)) {
            throw new DevelopmentException("Укажите корректное фактическое usage и признак списания провайдером.");
        }

        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .query(QUERY_RESERVATION_BY_REQUEST_ADMIN)
                .parameter("requestId", requestId.trim())
                .view("llm-chat-quota-reservation-view")
                .optional()
                .orElseThrow(() -> new DevelopmentException("Резерв по requestId не найден."));
        if (!"UNKNOWN_PENDING".equals(reservation.getStatus())) {
            throw new DevelopmentException("Резерв уже сверяется или закрыт: " + reservation.getStatus());
        }

        LlmChatQuotaPeriod period = dataManager.load(LlmChatQuotaPeriod.class)
                .id(reservation.getPeriod().getId())
                .view("llm-chat-quota-period-view")
                .one();
        int reservedTokens = safeInt(reservation.getReservedTokens());
        period.setPendingTokens(Math.max(0, safeInt(period.getPendingTokens()) - reservedTokens));
        reservation.setSettledTokens(providerCharged ? actualTokens : 0);
        reservation.setStatus(providerCharged ? "SETTLED" : "RELEASED");
        reservation.setReconciledBy(currentLogin());
        reservation.setReconciledAt(new Date());
        if (providerCharged) {
            period.setConsumedTokens(safeInt(period.getConsumedTokens()) + actualTokens);
        }
        dataManager.commit(new CommitContext(period, reservation));
    }

    private LlmChatResponse resolveExistingRequest(LlmChatConversation conversation,
                                                   ExtUser user,
                                                   String requestId) {
        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .query(QUERY_RESERVATION_BY_REQUEST)
                .parameter("requestId", requestId)
                .parameter("conversationId", conversation.getId())
                .parameter("userId", user.getId())
                .view("llm-chat-quota-reservation-view")
                .optional()
                .orElse(null);
        if (reservation == null) {
            return null;
        }
        String status = reservation.getStatus();
        if ("UNKNOWN_PENDING".equals(status)) {
            throw new DevelopmentException("Этот запрос уже отправлен, его результат уточняется. "
                    + "Повторный вызов не выполняется.");
        }
        if ("RESERVED".equals(status)) {
            throw new DevelopmentException("Этот запрос уже выполняется.");
        }
        if ("CANCEL_REQUESTED".equals(status)) {
            throw new DevelopmentException("Для этого запроса уже запрошена отмена.");
        }
        if ("CANCELLED".equals(status)) {
            throw new DevelopmentException("Этот запрос уже отменён. Для новой попытки используйте новый requestId.");
        }
        LlmChatMessage assistant = dataManager.load(LlmChatMessage.class)
                .query(QUERY_ASSISTANT_BY_REQUEST)
                .parameter("conversationId", conversation.getId())
                .parameter("requestId", requestId)
                .view("llm-chat-message-view")
                .optional()
                .orElseThrow(() -> new DevelopmentException("Результат запроса сохранён, но ещё не восстановлен в истории."));
        return new LlmChatResponse(conversation.getId(), assistant.getContent(), assistant.getProviderCode(),
                assistant.getModelName(), parseCredentialOwner(assistant.getCredentialOwner()));
    }

    private AiCredentialOwner parseCredentialOwner(String value) {
        if (value == null) {
            return null;
        }
        try {
            return AiCredentialOwner.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public List<LlmChatMessage> loadHistory(UUID conversationId) {
        if (conversationId == null) {
            return Collections.emptyList();
        }
        ExtUser user = currentUser();
        resolveConversation(conversationId, user);
        return dataManager.load(LlmChatMessage.class)
                .query(QUERY_HISTORY)
                .parameter("conversationId", conversationId)
                .parameter("userId", user.getId())
                .view("llm-chat-message-view")
                .list();
    }

    @Override
    public List<LlmChatMessage> loadHistoryAsAdmin(UUID conversationId) {
        requireHistoryAdminPermission();
        if (conversationId == null) {
            return Collections.emptyList();
        }
        // The admin path still resolves a real conversation, but deliberately
        // does not add a user predicate. No write/delete operation is exposed.
        dataManager.load(LlmChatConversation.class)
                .id(conversationId)
                .view("llm-chat-conversation-view")
                .one();
        return dataManager.load(LlmChatMessage.class)
                .query("select e from hunttech_LlmChatMessage e "
                        + "where e.conversation.id = :conversationId and e.deleteTs is null "
                        + "order by e.sequenceNo asc")
                .parameter("conversationId", conversationId)
                .view("llm-chat-message-view")
                .list();
    }

    private void requireHistoryAdminPermission() {
        if (!security.isSpecificPermitted(LlmChatService.VIEW_CHAT_HISTORY_ADMIN_PERMISSION)) {
            throw new DevelopmentException("Нет права просмотра истории LLM-чата.");
        }
    }

    private void requireQuotaReconciliationPermission() {
        if (!security.isSpecificPermitted(LlmChatService.RECONCILE_CHAT_QUOTA_PERMISSION)) {
            throw new DevelopmentException("Нет права сверки usage LLM-чата.");
        }
    }

    private LlmChatConversation resolveConversation(UUID conversationId, ExtUser user) {
        if (conversationId == null) {
            throw new DevelopmentException("Не указан диалог.");
        }
        return dataManager.load(LlmChatConversation.class)
                .query(QUERY_OWN_CONVERSATION)
                .parameter("id", conversationId)
                .parameter("userId", user.getId())
                .view("llm-chat-conversation-view")
                .optional()
                .orElseThrow(() -> new DevelopmentException("Диалог не найден или недоступен."));
    }

    private int nextSequence(UUID conversationId, UUID userId) {
        List<LlmChatMessage> history = dataManager.load(LlmChatMessage.class)
                .query(QUERY_HISTORY)
                .parameter("conversationId", conversationId)
                .parameter("userId", userId)
                .view("llm-chat-message-view")
                .list();
        return history.isEmpty() ? 1 : history.get(history.size() - 1).getSequenceNo() + 1;
    }

    TimeZone resolveEffectiveTimeZone(User user) {
        if (userSessionSource != null && userSessionSource.checkCurrentUserSession()) {
            try {
                TimeZone tz = userSessionSource.getUserSession().getTimeZone();
                if (tz != null) {
                    return tz;
                }
            } catch (Exception ignored) {
            }
        }
        if (user != null && user.getTimeZone() != null && !user.getTimeZone().trim().isEmpty()) {
            try {
                return TimeZone.getTimeZone(user.getTimeZone().trim());
            } catch (Exception ignored) {
            }
        }
        return TimeZone.getTimeZone("Europe/Moscow");
    }

    Locale resolveEffectiveLocale(User user) {
        if (userSessionSource != null && userSessionSource.checkCurrentUserSession()) {
            try {
                Locale loc = userSessionSource.getUserSession().getLocale();
                if (loc != null) {
                    return loc;
                }
            } catch (Exception ignored) {
            }
        }
        if (user != null && user.getLanguage() != null && !user.getLanguage().trim().isEmpty()) {
            try {
                return new Locale(user.getLanguage().trim());
            } catch (Exception ignored) {
            }
        }
        return new Locale("ru", "RU");
    }

    String buildMessageWithHistory(LlmChatConversation conversation, ExtUser user,
                                   int currentSequenceNo, String currentMessageText) {
        return buildMessageWithHistory(conversation, user, currentSequenceNo, currentMessageText, null);
    }

    String buildMessageWithHistory(LlmChatConversation conversation, ExtUser user,
                                   int currentSequenceNo, String currentMessageText, String runtimeContext) {
        if (conversation == null || currentMessageText == null) {
            return currentMessageText;
        }
        int maxContextLimit = resolveEffectiveMaxContextTokens(user);

        Integer resetSeq = conversation.getContextResetSequenceNo();
        String query = "select m from hunttech_LlmChatMessage m " +
                "where m.conversation.id = :convId " +
                "and m.status = 'COMPLETED' " +
                "and m.sequenceNo < :currentSeq " +
                (resetSeq != null ? "and m.sequenceNo >= :resetSeq " : "") +
                "order by m.sequenceNo desc";

        FluentLoader.ByQuery<LlmChatMessage, UUID> loader = dataManager.load(LlmChatMessage.class)
                .query(query)
                .parameter("convId", conversation.getId())
                .parameter("currentSeq", currentSequenceNo);
        if (resetSeq != null) {
            loader.parameter("resetSeq", resetSeq);
        }
        loader.maxResults(200);
        List<LlmChatMessage> historyMessages = loader.view("llm-chat-message-view").list();

        if (historyMessages.isEmpty()) {
            return currentMessageText;
        }

        // Возвращаем сообщения в хронологический порядок после выборки последних N сообщений
        java.util.Collections.reverse(historyMessages);

        // Формируем историю со скользящим окном (sliding window)
        String slidingHistory = LlmChatContextBuilder.buildSlidingHistory(
                historyMessages, maxContextLimit, currentMessageText, runtimeContext);

        if (slidingHistory == null || slidingHistory.isEmpty()) {
            log.info("Размер контекста диалога для convId={} достиг лимита ({} токенов). Контекст сокращен/обнулён до текущего сообщения.",
                    conversation.getId(), maxContextLimit);
            conversation.setContextResetSequenceNo(currentSequenceNo);
            dataManager.commit(conversation);
            return currentMessageText;
        }

        return LlmChatContextBuilder.combinePayload(slidingHistory, currentMessageText);
    }

    int resolveEffectiveMaxContextTokens(ExtUser user) {
        int limit = UserAiConfiguration.DEFAULT_MAX_CONTEXT_TOKENS;
        if (user != null && dataManager != null) {
            try {
                List<UserAiConfiguration> userConfigs = dataManager.load(UserAiConfiguration.class)
                        .query("select e from hunttech_UserAiConfiguration e where e.user.id = :userId and e.isActive = true order by e.isPrimary desc, e.priority desc")
                        .parameter("userId", user.getId())
                        .view("userAiConfiguration-browse-view")
                        .list();
                if (!userConfigs.isEmpty()) {
                    UserAiConfiguration userConfig = userConfigs.get(0);
                    if (userConfig.getMaxContextTokens() != null) {
                        return Math.min(UserAiConfiguration.MAX_CONTEXT_TOKENS_LIMIT,
                                Math.max(UserAiConfiguration.MIN_CONTEXT_TOKENS_LIMIT, userConfig.getMaxContextTokens()));
                    }
                }
            } catch (Exception e) {
                log.warn("Не удалось загрузить лимит контекста UserAiConfiguration: {}", e.getMessage());
            }
        }
        if (dataManager != null) {
            try {
                List<AdminAiConfiguration> adminConfigs = dataManager.load(AdminAiConfiguration.class)
                        .query("select c from hunttech_AdminAiConfiguration c where c.active = true order by c.priority desc")
                        .view("admin-ai-configuration-browse-view")
                        .list();
                if (!adminConfigs.isEmpty()) {
                    AdminAiConfiguration adminConfig = adminConfigs.get(0);
                    if (adminConfig.getMaxContextTokens() != null) {
                        return Math.min(UserAiConfiguration.MAX_CONTEXT_TOKENS_LIMIT,
                                Math.max(UserAiConfiguration.MIN_CONTEXT_TOKENS_LIMIT, adminConfig.getMaxContextTokens()));
                    }
                }
            } catch (Exception e) {
                log.warn("Не удалось загрузить лимит контекста AdminAiConfiguration: {}", e.getMessage());
            }
        }
        return limit;
    }

    static int estimateTokens(String text) {
        return LlmChatContextBuilder.estimateTokens(text);
    }

    private QuotaReservationContext reserveQuota(ExtUser user, LlmChatConversation conversation,
                                                 String message, String requestId) {
        AiFunctionConfiguration function = dataManager.load(AiFunctionConfiguration.class)
                .query(QUERY_FUNCTION)
                .parameter("code", FUNCTION_CODE)
                .view("ai-function-configuration-browse-view")
                .optional()
                .orElseThrow(() -> new DevelopmentException("Функция чата LLM_CHAT не настроена."));
        Date today = truncateToDate(new Date());
        Integer override = dataManager.load(LlmUserQuotaOverride.class)
                .query(QUERY_QUOTA_OVERRIDE)
                .parameter("userId", user.getId())
                .parameter("today", today)
                .view("llm-user-quota-override-view")
                .optional()
                .map(LlmUserQuotaOverride::getMonthlyQuotaTokens)
                .orElse(null);
        int quotaTokens = override != null ? override : safeQuota(function.getDefaultMonthlyTokenQuota());
        boolean isUnlimited = quotaTokens == -1 || quotaTokens == Integer.MAX_VALUE;
        if (!isUnlimited && quotaTokens <= 0) {
            throw new DevelopmentException("Месячная квота LLM-чата ещё не настроена администратором.");
        }

        Date periodStart = monthStart(today);
        LlmChatQuotaPeriod period = dataManager.load(LlmChatQuotaPeriod.class)
                .query(QUERY_QUOTA_PERIOD)
                .parameter("userId", user.getId())
                .parameter("periodStart", periodStart)
                .view("llm-chat-quota-period-view")
                .optional()
                .orElseGet(() -> createQuotaPeriod(user, periodStart, quotaTokens));
        if (period.getQuotaTokens() == null || period.getQuotaTokens() != quotaTokens) {
            period.setQuotaTokens(quotaTokens);
        }
        int estimatedTokens = Math.max(1, (message.codePointCount(0, message.length()) + 3) / 4
                + Math.max(1, function.getMaxTokens() == null ? 1200 : function.getMaxTokens()));
        int used = safeInt(period.getConsumedTokens()) + safeInt(period.getReservedTokens())
                + safeInt(period.getPendingTokens());
        int extraTokens = safeInt(period.getExtraTokens());
        long totalAllowed = isUnlimited ? -1L : ((long) safeInt(period.getQuotaTokens()) + extraTokens);
        if (!isUnlimited && totalAllowed != -1L
                && ((long) used + estimatedTokens > totalAllowed)) {
            throw new DevelopmentException("Месячная квота чата исчерпана или занята текущими запросами.");
        }
        period.setReservedTokens(safeInt(period.getReservedTokens()) + estimatedTokens);
        LlmChatQuotaReservation reservation = metadata.create(LlmChatQuotaReservation.class);
        reservation.setPeriod(period);
        reservation.setConversation(conversation);
        reservation.setRequestId(requestId);
        reservation.setReservedTokens(estimatedTokens);
        reservation.setStatus("RESERVED");
        dataManager.commit(new CommitContext(period, reservation));
        return new QuotaReservationContext(period.getId(), reservation.getId(), estimatedTokens);
    }

    private LlmChatQuotaPeriod createQuotaPeriod(ExtUser user, Date periodStart, int quotaTokens) {
        LlmChatQuotaPeriod period = metadata.create(LlmChatQuotaPeriod.class);
        period.setUser(user);
        period.setPeriodStart(periodStart);
        period.setQuotaTokens(quotaTokens);
        period.setReservedTokens(0);
        period.setConsumedTokens(0);
        period.setPendingTokens(0);
        period.setExtraTokens(0);
        return period;
    }

    private boolean settleQuota(QuotaReservationContext context, AiExecutionResult result) {
        int consumed = result.getTotalTokens() == null
                ? context.reservedTokens : Math.max(0, result.getTotalTokens());
        LlmChatQuotaPeriod period = dataManager.load(LlmChatQuotaPeriod.class)
                .id(context.periodId).view("llm-chat-quota-period-view").one();
        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .id(context.reservationId).view("llm-chat-quota-reservation-view").one();
        period.setReservedTokens(Math.max(0, safeInt(period.getReservedTokens()) - context.reservedTokens));
        period.setConsumedTokens(safeInt(period.getConsumedTokens()) + consumed);
        reservation.setSettledTokens(consumed);
        reservation.setProviderRequestId(result.getProviderRequestId());
        boolean cancelled = "CANCEL_REQUESTED".equals(reservation.getStatus());
        reservation.setStatus(cancelled ? "CANCELLED" : (result.getTotalTokens() == null ? "ESTIMATED" : "SETTLED"));
        dataManager.commit(new CommitContext(period, reservation));
        return cancelled;
    }

    private boolean isCancellationRequested(QuotaReservationContext context) {
        return "CANCEL_REQUESTED".equals(dataManager.load(LlmChatQuotaReservation.class)
                .id(context.reservationId).view("llm-chat-quota-reservation-view").one().getStatus());
    }

    private void settleCancelledBeforeProvider(QuotaReservationContext context, LlmChatMessage userMessage) {
        LlmChatQuotaPeriod period = dataManager.load(LlmChatQuotaPeriod.class)
                .id(context.periodId).view("llm-chat-quota-period-view").one();
        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .id(context.reservationId).view("llm-chat-quota-reservation-view").one();
        period.setReservedTokens(Math.max(0, safeInt(period.getReservedTokens()) - context.reservedTokens));
        reservation.setSettledTokens(0);
        reservation.setStatus("CANCELLED");
        userMessage.setStatus("CANCELLED");
        dataManager.commit(new CommitContext(period, reservation, userMessage));
    }

    private void markQuotaPending(QuotaReservationContext context) {
        markQuotaPending(context, null);
    }

    private void markQuotaPending(QuotaReservationContext context, String providerRequestId) {
        LlmChatQuotaPeriod period = dataManager.load(LlmChatQuotaPeriod.class)
                .id(context.periodId).view("llm-chat-quota-period-view").one();
        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .id(context.reservationId).view("llm-chat-quota-reservation-view").one();
        period.setReservedTokens(Math.max(0, safeInt(period.getReservedTokens()) - context.reservedTokens));
        period.setPendingTokens(safeInt(period.getPendingTokens()) + context.reservedTokens);
        reservation.setStatus("UNKNOWN_PENDING");
        reservation.setProviderRequestId(providerRequestId);
        dataManager.commit(new CommitContext(period, reservation));
    }

    private void settleObservedUsage(QuotaReservationContext context, int actualTokens, String providerRequestId) {
        int consumed = Math.max(0, actualTokens);
        LlmChatQuotaPeriod period = dataManager.load(LlmChatQuotaPeriod.class)
                .id(context.periodId).view("llm-chat-quota-period-view").one();
        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .id(context.reservationId).view("llm-chat-quota-reservation-view").one();
        period.setReservedTokens(Math.max(0, safeInt(period.getReservedTokens()) - context.reservedTokens));
        period.setConsumedTokens(safeInt(period.getConsumedTokens()) + consumed);
        reservation.setSettledTokens(consumed);
        reservation.setProviderRequestId(providerRequestId);
        reservation.setReconciledBy("SYSTEM_PROVIDER_USAGE");
        reservation.setReconciledAt(new Date());
        reservation.setStatus("CANCEL_REQUESTED".equals(reservation.getStatus())
                ? "CANCELLED" : (consumed == 0 ? "RELEASED" : "SETTLED"));
        dataManager.commit(new CommitContext(period, reservation));
    }

    private int safeQuota(Integer value) {
        return value == null ? 0 : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private Date truncateToDate(Date value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date monthStart(Date value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return truncateToDate(calendar.getTime());
    }

    private static final class StreamingSession {
        private final UUID userId;
        private final UUID conversationId;
        private final String requestId;
        private final QuotaReservationContext quota;
        private final LlmChatMessage userMessage;
        private final int nextSequence;
        private final StringBuilder text = new StringBuilder();
        private volatile String providerRequestId;
        private volatile Integer observedTotalTokens;
        private volatile String status = "STREAMING";
        private volatile String errorMessage;
        private volatile long finishedAt;

        private StreamingSession(UUID userId, UUID conversationId, String requestId,
                                 QuotaReservationContext quota, LlmChatMessage userMessage, int nextSequence) {
            this.userId = userId;
            this.conversationId = conversationId;
            this.requestId = requestId;
            this.quota = quota;
            this.userMessage = userMessage;
            this.nextSequence = nextSequence;
        }

        private synchronized void append(String delta) {
            if (delta != null && !delta.isEmpty() && !isCompleted()) {
                text.append(delta);
            }
        }

        private synchronized void complete(String status, String errorMessage) {
            this.status = status;
            this.errorMessage = errorMessage;
            this.finishedAt = System.currentTimeMillis();
        }

        private void setProviderRequestId(String providerRequestId) {
            if (providerRequestId != null && !providerRequestId.trim().isEmpty()) {
                this.providerRequestId = providerRequestId.trim();
            }
        }

        private String getProviderRequestId() {
            return providerRequestId;
        }

        private void setObservedTotalTokens(Integer observedTotalTokens) {
            if (observedTotalTokens != null && observedTotalTokens >= 0) {
                this.observedTotalTokens = observedTotalTokens;
            }
        }

        private Integer getObservedTotalTokens() {
            return observedTotalTokens;
        }

        private synchronized LlmChatStreamState snapshot() {
            return new LlmChatStreamState(conversationId, requestId, text.toString(), status,
                    errorMessage, isCompleted());
        }

        private boolean isCompleted() {
            return "COMPLETED".equals(status) || "ERROR".equals(status) || "CANCELLED".equals(status);
        }

        private boolean isFinishedBefore(long threshold) {
            return isCompleted() && finishedAt > 0 && finishedAt < threshold;
        }

        private void assertOwner(UUID expectedUserId, UUID expectedConversationId) {
            if (!userId.equals(expectedUserId) || !conversationId.equals(expectedConversationId)) {
                throw new DevelopmentException("Потоковый запрос недоступен.");
            }
        }
    }

    private static final class QuotaReservationContext {
        private final UUID periodId;
        private final UUID reservationId;
        private final int reservedTokens;

        private QuotaReservationContext(UUID periodId, UUID reservationId, int reservedTokens) {
            this.periodId = periodId;
            this.reservationId = reservationId;
            this.reservedTokens = reservedTokens;
        }
    }

    private ExtUser currentUser() {
        User sessionUser = userSessionSource.getUserSession().getUser();
        if (sessionUser == null || sessionUser.getId() == null) {
            throw new DevelopmentException("Требуется авторизация пользователя.");
        }
        return dataManager.load(ExtUser.class)
                .id(sessionUser.getId())
                .view("_minimal")
                .one();
    }

    private String currentLogin() {
        User sessionUser = userSessionSource.getUserSession().getUser();
        if (sessionUser == null || sessionUser.getLogin() == null) {
            throw new DevelopmentException("Требуется авторизация администратора.");
        }
        return sessionUser.getLogin();
    }

    private boolean isVacancyOpeningIntent(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        String lower = message.trim().toLowerCase();
        boolean hasVacancyKeyword = lower.contains("ваканси") || lower.contains("позици") || lower.contains("openposition");
        boolean hasActionKeyword = lower.contains("открой") || lower.contains("создай") || lower.contains("добавь")
                || lower.contains("загрузи") || lower.contains("открыть") || lower.contains("создать") || lower.contains("загрузить");
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
        // Проверка группы пользователя
        if (user.getGroup() != null && user.getGroup().getName() != null) {
            String grp = user.getGroup().getName().toLowerCase();
            if (grp.contains("менедж") || grp.contains("директор") || grp.contains("руковод") || grp.contains("управлен") || grp.contains("admin")) {
                return true;
            }
        }
        // Проверка ролей пользователя
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

    private String handleVacancyOpeningIntent(ExtUser user, String message) {
        if (!isManagerOrDirector(user)) {
            log.warn("[LLM_CHAT_VACANCY] Отказ в доступе: пользователь '{}' не входит в группу Менеджеры/Директор",
                    user != null ? user.getLogin() : "null");
            return "⚠️ **Ограничение доступа**\n\nФункция открытия вакансий через LLM-чат доступна исключительно пользователям из групп **«Менеджеры»** и **«Директор»**.\nУ вашей учетной записи недостаточно полномочий для выполнения этого действия.";
        }

        log.info("[LLM_CHAT_VACANCY] Инициировано открытие вакансии из чата пользователем '{}'", user != null ? user.getLogin() : "null");
        try {
            // Извлекаем URL, если есть
            Pattern urlPattern = Pattern.compile("(?i)(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)");
            Matcher urlMatcher = urlPattern.matcher(message);
            String targetInput = message;
            if (urlMatcher.find()) {
                targetInput = urlMatcher.group(1).trim();
                log.info("[LLM_CHAT_VACANCY] Извлечен URL вакансии из сообщения: {}", targetInput);
            } else {
                // Если ссылки нет, очищаем команду из текста сообщения
                targetInput = message.replaceAll("(?i)^(?:открой|создай|добавь|загрузи)(?:\\s+пожалуйста)?\\s+вакансию[:\\s]*", "").trim();
            }

            if (smartOpenPositionIngestService == null) {
                return "❌ Сервис умного открытия вакансий недоступен.";
            }

            SmartOpenPositionParsedData parsedData = smartOpenPositionIngestService.parseVacancyText(targetInput);
            if (parsedData == null || parsedData.getVacansyName() == null || parsedData.getVacansyName().trim().isEmpty()) {
                return "⚠️ Не удалось распознать данные вакансии из предоставленного текста или ссылки. Пожалуйста, проверьте ссылку или описание.";
            }

            // Проверка на статус паузы в запросе
            String lowerMsg = message.toLowerCase();
            if (lowerMsg.contains("пауз") || lowerMsg.contains("pause")) {
                parsedData.setPriority(OpenPositionPriority.PAUSED.getId()); // 0
            }

            // Проверка дубликатов (в первую очередь по точной vacansyID)
            OpenPosition duplicate = smartOpenPositionIngestService.findDuplicate(parsedData);
            if (duplicate != null) {
                String dupTitle = duplicate.getVacansyName() != null ? duplicate.getVacansyName() : "Вакансия";
                String dupLink = "[" + dupTitle + "](hrm://vacancy/" + duplicate.getId() + ")";
                return "⚠️ **Вакансия уже существует в системе (дубликат)**\n\n" +
                        "В HRM уже зарегистрирована позиция с идентичными реквизитами:\n" +
                        "• **Карточка вакансии:** " + dupLink + (duplicate.getVacansyID() != null ? " (ID заявки: " + duplicate.getVacansyID() + ")" : "") + "\n" +
                        "• **Проект:** " + (duplicate.getProjectName() != null ? duplicate.getProjectName().getProjectName() : "Не указан") + "\n" +
                        "• **Статус:** " + (Boolean.TRUE.equals(duplicate.getOpenClose()) ? "Закрыта" : "Открыта") + "\n\n" +
                        "Согласно регламенту **HuntTech Vacancy Opening**, создание повторного дубликата отменено.";
            }

            SmartOpenPositionIngestResult result = smartOpenPositionIngestService.createOpenPosition(parsedData, user);
            if (!result.isSuccess() || result.getOpenPosition() == null) {
                return "❌ Не удалось сохранить вакансию в HRM: " + result.getMessage();
            }

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
            return sb.toString();
        } catch (Exception e) {
            log.error("[LLM_CHAT_VACANCY] Ошибка при открытии вакансии из чата: " + e.getMessage(), e);
            return "❌ Произошла ошибка при открытии вакансии: " + e.getMessage();
        }
    }
}
