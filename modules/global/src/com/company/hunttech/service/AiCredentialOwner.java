package com.company.hunttech.service;

/**
 * Собственник API-credentials, использованных при AI-выполнении.
 *
 * <p>Обязательная часть контракта пользовательской нотификации об AI-операциях
 * (docs/architecture/HRM_HuntTech_AI_User_Notification_Contract.md): каждый реальный
 * AI-вызов возвращает в {@link AiExecutionResult}, чьим API-подключением он выполнен —
 * корпоративным (административным) или личным подключением пользователя.</p>
 */
public enum AiCredentialOwner {

    /**
     * Корпоративное (административное) подключение: запись
     * {@code HUNTTECH_ADMIN_AI_CONFIGURATION}, привязанная к AI-функции
     * ({@code admin_configuration_id}) и управляемая администратором.
     */
    ADMIN,

    /**
     * Личное подключение пользователя: запись {@code HUNTTECH_USER_AI_CONFIGURATION}
     * + активное замещение {@code HUNTTECH_USER_AI_FUNCTION_OVERRIDE} для функции.
     */
    USER
}
