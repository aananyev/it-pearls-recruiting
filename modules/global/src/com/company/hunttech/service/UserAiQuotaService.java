package com.company.hunttech.service;

import com.company.hunttech.service.dto.ai.ActiveUserQuotaSummary;
import com.company.hunttech.service.dto.ai.UserAiQuotaInfo;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Middleware-сервис управления лимитами и статистикой квот AI-токенов пользователей.
 */
public interface UserAiQuotaService {
    String NAME = "hunttech_UserAiQuotaService";
    String MSG_QUOTA_EXHAUSTED = "Закончились доступные токены ИИ. Пожалуйста, обратитесь к администратору системы для пополнения квоты.";

    /**
     * Возвращает сводную информацию о квоте пользователя на текущий месяц.
     *
     * @param userId идентификатор пользователя
     * @return DTO с данными о выделенных, израсходованных, зарезервированных и оставшихся токенах
     */
    UserAiQuotaInfo getUserQuota(UUID userId);

    /**
     * Устанавливает или обновляет персональный лимит (override) токенов на текущий месяц.
     *
     * @param userId идентификатор пользователя
     * @param monthlyQuotaTokens лимит токенов (-1 для безлимита, null для сброса на системный дефолт)
     * @param reason обоснование установки лимита
     */
    void setMonthlyQuota(UUID userId, Integer monthlyQuotaTokens, String reason);

    /**
     * Разово добавляет бонусные токены пользователю исключительно на текущий календарный месяц.
     * Постоянный месячный лимит пользователя не изменяется.
     * При наступлении следующего календарного месяца неиспользованные бонусные токены не переносятся.
     * Повторные вызовы в рамках текущего месяца суммируются.
     *
     * @param userId идентификатор пользователя
     * @param amount количество добавляемых токенов (строго > 0)
     * @param reason комментарий / основание начисления
     */
    void addMonthlyBonusTokens(UUID userId, int amount, String reason);

    /**
     * Пакетно возвращает квоты для переданного списка идентификаторов пользователей.
     *
     * @param userIds список ID пользователей
     * @return карта userId -> UserAiQuotaInfo
     */
    Map<UUID, UserAiQuotaInfo> getUsersQuotaMap(List<UUID> userIds);

    /**
     * Возвращает сводную таблицу квот и фактического расхода по ВСЕМ активным пользователям системы.
     *
     * @param from дата начала периода расхода
     * @param to дата окончания периода расхода
     * @return список DTO по всем активным пользователям
     */
    List<ActiveUserQuotaSummary> getActiveUsersQuotaSummary(Date from, Date to);

    /**
     * Возвращает системную квоту по умолчанию (из AiFunctionConfiguration для LLM_CHAT).
     *
     * @return квота по умолчанию
     */
    int loadDefaultMonthlyQuota();

    /**
     * Проверяет доступность квоты токенов для пользователя.
     * Если квота исчерпана (remainingTokens <= 0 и нет безлимита), выбрасывает DevelopmentException с каноническим сообщением:
     * "Закончились доступные токены ИИ. Пожалуйста, обратитесь к администратору системы для пополнения квоты."
     *
     * @param userId идентификатор пользователя
     * @param estimatedTokens предварительная оценка требуемых токенов
     */
    void checkQuotaAvailable(UUID userId, int estimatedTokens);

    /**
     * Проверяет, доступна ли квота для пользователя (true - доступна или безлимит, false - исчерпана).
     *
     * @param userId идентификатор пользователя
     * @param estimatedTokens предварительная оценка требуемых токенов
     * @return true если квоты достаточно
     */
    boolean isQuotaAvailable(UUID userId, int estimatedTokens);

    /**
     * Списывает фактически израсходованные токены пользователя в счет квоты текущего календарного месяца.
     * Увеличивает consumedTokens в LlmChatQuotaPeriod текущего месяца.
     *
     * @param userId идентификатор пользователя
     * @param tokensUsed количество фактически израсходованных токенов (prompt + completion)
     */
    void recordTokenConsumption(UUID userId, int tokensUsed);

    /**
     * Проверяет, является ли модель административной (ADMIN / CONTAINER_DEFAULT).
     * Если true - модель корпоративная/административная и её вызовы тарифицируются в квоте пользователя.
     * Если false (USER) - модель личная, вызовы регистрируются в статистике (AiCallLog), но не списываются из квоты.
     *
     * @param credentialOwner владелец учетных данных ("ADMIN", "USER", etc.)
     * @return true если модель административная
     */
    boolean isAdminModel(String credentialOwner);

    /**
     * Списывает фактически израсходованные токены пользователя в счет квоты текущего месяца,
     * ТОЛЬКО если использовалась административная модель (credentialOwner != "USER").
     * Если использовалась личная модель пользователя (credentialOwner == "USER"), списание пропускается.
     *
     * @param userId идентификатор пользователя
     * @param tokensUsed количество фактически израсходованных токенов (prompt + completion)
     * @param credentialOwner владелец модели ("ADMIN", "USER", etc.)
     */
    void recordTokenConsumption(UUID userId, int tokensUsed, String credentialOwner);

    /**
     * Проверяет, настроены ли у пользователя активные личные модели ИИ (UserAiConfiguration).
     *
     * @param userId идентификатор пользователя
     * @return true если у пользователя есть активная конфигурация личной модели
     */
    boolean hasActivePersonalModel(UUID userId);

    /**
     * Проверяет, настроен ли у пользователя активный личный оверрайд (UserAiFunctionOverride) для конкретной AI-функции.
     *
     * @param userId идентификатор пользователя
     * @param functionCode код AI-функции
     * @return true если для функции настроено и включено личное замещение
     */
    boolean hasActivePersonalOverride(UUID userId, String functionCode);
}
