package com.company.hunttech.service;

import com.company.hunttech.dto.integration.*;

/**
 * Единый сервис внешней интеграции HRM HuntTech (BL-2026-036).
 * <p>
 * Предоставляет безопасный API для создания и синхронизации компаний, проектов,
 * вакансий, кандидатов, резюме и взаимодействий внешними системами.
 */
public interface ExternalIntegrationService {

    String NAME = "hunttech_ExternalIntegrationService";

    /**
     * Создает новую компанию или находит существующую по ИНН/наименованию с защитой от дублей (BL-2026-027).
     *
     * @param request запрос с реквизитами компании, idempotencyKey и externalId
     * @return структурированный ответ с ID компании и статусом (CREATED / EXISTING_FOUND)
     */
    CompanyResponseDto createCompany(CompanyCreateRequestDto request);

    /**
     * Транзакционно создает проект и связанную вакансию (BL-2026-025).
     *
     * @param request запрос с данными проекта и вакансии
     * @return структурированный ответ с идентификаторами созданного проекта и вакансии
     */
    ProjectVacancyResponseDto createProjectAndVacancy(ProjectVacancyCreateRequestDto request);

    /**
     * Создает резюме кандидата CandidateCV с привязкой к кандидату и опционально вакансии/должности (BL-2026-028).
     *
     * @param request запрос с данными резюме
     * @return структурированный ответ с идентификатором созданного резюме
     */
    CandidateCvResponseDto createCandidateCV(CandidateCvCreateRequestDto request);

    /**
     * Создает запись взаимодействия IteractionList с кандидатом по вакансии (BL-2026-028).
     *
     * @param request запрос с данными взаимодействия
     * @return структурированный ответ с идентификатором и номером созданного взаимодействия
     */
    InteractionResponseDto createInteraction(InteractionCreateRequestDto request);

    /**
     * Комплексно создает кандидата, резюме и первое взаимодействие в рамках одной транзакции (BL-2026-026).
     * Поддерживает дедупликацию по телефону, email и ФИО с обогащением существующей карточки.
     *
     * @param request комплексный запрос создания кандидата
     * @return структурированный ответ с идентификаторами созданных/привязанных сущностей
     */
    CandidateCompositeResponseDto createCandidateWithDetails(CandidateCompositeCreateRequestDto request);
}


