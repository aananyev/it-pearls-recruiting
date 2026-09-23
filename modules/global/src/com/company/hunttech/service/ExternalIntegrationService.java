package com.company.hunttech.service;

import com.company.hunttech.dto.integration.CompanyCreateRequestDto;
import com.company.hunttech.dto.integration.CompanyResponseDto;
import com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto;
import com.company.hunttech.dto.integration.ProjectVacancyResponseDto;

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
}

