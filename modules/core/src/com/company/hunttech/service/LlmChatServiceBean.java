package com.company.hunttech.service;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.ai.LlmChatConversation;
import com.company.hunttech.entity.ai.LlmChatMessage;
import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.company.hunttech.entity.ai.LlmChatQuotaPeriod;
import com.company.hunttech.entity.ai.LlmChatQuotaReservation;
import com.company.hunttech.entity.ai.LlmUserQuotaOverride;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Calendar;

/**
 * Synchronous MVP facade for the floating chat. Provider routing, profile
 * context and admin fallback policy remain centralized in AiExecutionService.
 */
@Service(LlmChatService.NAME)
public class LlmChatServiceBean implements LlmChatService {
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

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private AiExecutionService aiExecutionService;
    @Inject
    private Security security;

    @Override
    public UUID startConversation() {
        ExtUser user = currentUser();
        LlmChatConversation conversation = metadata.create(LlmChatConversation.class);
        conversation.setUser(user);
        conversation.setStatus("ACTIVE");
        conversation.setTitle("Новый диалог");
        return dataManager.commit(conversation).getId();
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

        Map<String, Object> context = new HashMap<>();
        // Only the chat message is supplied. Candidate/CV entities are never
        // resolved or attached to this contract.
        context.put("message", message.trim());
        context.put("callerSource", "LlmChatService");
        AiExecutionResult result;
        try {
            result = aiExecutionService.executeText(FUNCTION_CODE, context);
        } catch (RuntimeException failure) {
            markQuotaPending(quota);
            throw failure;
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
        assistantMessage.setCredentialOwner(result.getCredentialOwner() == null
                ? null : result.getCredentialOwner().name());
        conversation.setLastMessageAt(new Date());
        dataManager.commit(new CommitContext(conversation, assistantMessage));

        return new LlmChatResponse(conversation.getId(), result.getText(), result.getProviderCode(),
                result.getModelName(), result.getCredentialOwner());
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
        }
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
        if (quotaTokens <= 0) {
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
        int estimatedTokens = Math.max(1, (message.codePointCount(0, message.length()) + 3) / 4
                + Math.max(1, function.getMaxTokens() == null ? 1200 : function.getMaxTokens()));
        int used = safeInt(period.getConsumedTokens()) + safeInt(period.getReservedTokens())
                + safeInt(period.getPendingTokens());
        if (used + estimatedTokens > safeInt(period.getQuotaTokens())) {
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
        LlmChatQuotaPeriod period = dataManager.load(LlmChatQuotaPeriod.class)
                .id(context.periodId).view("llm-chat-quota-period-view").one();
        LlmChatQuotaReservation reservation = dataManager.load(LlmChatQuotaReservation.class)
                .id(context.reservationId).view("llm-chat-quota-reservation-view").one();
        period.setReservedTokens(Math.max(0, safeInt(period.getReservedTokens()) - context.reservedTokens));
        period.setPendingTokens(safeInt(period.getPendingTokens()) + context.reservedTokens);
        reservation.setStatus("UNKNOWN_PENDING");
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
}
