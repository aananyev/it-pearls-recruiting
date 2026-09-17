package com.company.hunttech.service;

import com.company.hunttech.service.dto.HermesChatMessage;
import com.company.hunttech.service.dto.HermesChatResponse;
import com.company.hunttech.service.dto.HermesConnectionStatus;

import java.util.List;
import java.util.UUID;

/**
 * Сервис взаимодействия со вторым Hermes Agent для менеджеров и директоров
 * (профиль hrm-operator в контейнере hermes-hrm-operator).
 * Обеспечивает безопасное выполнение разрешенных операций изменения в HRM
 * под контролем CUBA Security, Entity Permissions и Attribute Permissions.
 */
public interface HermesManagerChatService {
    String NAME = "hunttech_HermesManagerChatService";

    /**
     * Создать или получить активный диалог Manager Hermes для текущего пользователя.
     */
    UUID startManagerHermesConversation();

    /**
     * Отправить сообщение агенту Manager Hermes и получить ответ с исполнением намерения.
     */
    HermesChatResponse sendManagerHermesMessage(UUID conversationId, String message);

    /**
     * Загрузить историю сообщений диалога Manager Hermes.
     */
    List<HermesChatMessage> loadManagerHermesHistory(UUID conversationId);

    /**
     * Проверить статус подключения к контейнеру hermes-hrm-operator.
     */
    HermesConnectionStatus checkManagerHermesConnection();

    /**
     * Очистить историю диалога Manager Hermes.
     */
    void clearManagerHermesHistory(UUID conversationId);
}
