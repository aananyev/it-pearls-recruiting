package com.company.hunttech.service;

import com.company.hunttech.service.dto.HermesChatMessage;
import com.company.hunttech.service.dto.HermesChatResponse;
import com.company.hunttech.service.dto.HermesConnectionStatus;

import java.util.List;
import java.util.UUID;

/**
 * Сервис взаимодействия с Hermes Agent (профиль hrm-viewer в Docker).
 */
public interface HermesChatService {
    String NAME = "hunttech_HermesChatService";

    /**
     * Создать новый диалог Hermes для текущего пользователя.
     */
    UUID startHermesConversation();

    /**
     * Отправить сообщение агенту Hermes и получить структурированный ответ.
     */
    HermesChatResponse sendHermesMessage(UUID conversationId, String message);

    /**
     * Загрузить историю сообщений для указанного диалога Hermes.
     */
    List<HermesChatMessage> loadHermesHistory(UUID conversationId);

    /**
     * Проверить статус подключения к контейнеру Hermes и профилю hrm-viewer.
     */
    HermesConnectionStatus checkHermesConnection();
}
