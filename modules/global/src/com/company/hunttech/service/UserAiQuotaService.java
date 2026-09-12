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
}
