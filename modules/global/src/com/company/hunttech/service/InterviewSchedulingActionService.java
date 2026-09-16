package com.company.hunttech.service;

import com.company.hunttech.dto.action.InterviewSchedulingIntent;
import com.company.hunttech.dto.action.InterviewSchedulingResult;
import com.company.hunttech.entity.ExtUser;

import java.util.UUID;

public interface InterviewSchedulingActionService {
    String NAME = "hunttech_InterviewSchedulingActionService";

    /**
     * Быстрая проверка, содержит ли пользовательский ввод намерение назначить собеседование или интервью кандидату.
     */
    boolean isInterviewSchedulingIntent(String userMessage);

    /**
     * NLU-парсинг параметров собеседования (кандидат, вакансия, проект, руководитель проекта, тип, дата и время).
     */
    InterviewSchedulingIntent parseSchedulingIntent(String userMessage, ExtUser currentUser);

    /**
     * Обработка запроса на назначение собеседования или продолжение многошагового диалога (уточнения, подтверждения, отмена).
     *
     * @param conversationId ID текущего диалога (вкладка «Локальный чат» или «Hermes»)
     * @param userMessage    Текст сообщения пользователя
     * @param currentUser    Текущий авторизованный пользователь
     * @param requestId      Уникальный ID запроса (для идемпотентности)
     * @return Результат обработки или null, если сообщение не относится к сценарию назначения собеседования
     */
    InterviewSchedulingResult handleSchedulingAction(UUID conversationId, String userMessage, ExtUser currentUser, String requestId);

    /**
     * Отмена текущего ожидающего действия пользователя в рамках диалога.
     */
    boolean cancelPendingAction(UUID conversationId, UUID userId);
}
