package com.company.hunttech.service;

import com.company.hunttech.service.dto.OpenPositionExplanationResult;

import java.util.UUID;

/**
 * Сервис интеллектуального анализа и объяснения требований вакансий для рекрутеров.
 */
public interface OpenPositionExplanationService {
    String NAME = "hunttech_OpenPositionExplanationService";

    /**
     * Стандартное объяснение требований вакансии через стандартный Java-сервис AI (AiExecutionService).
     *
     * @param openPositionId ID вакансии
     * @return результат генерации с метаданными и идентификатором записи журнала
     */
    OpenPositionExplanationResult explainRequirements(UUID openPositionId);

    /**
     * Сверхпонятное объяснение требований вакансии на житейских примерах с поиском в интернете через Hermes hrm-viewer.
     *
     * @param openPositionId       ID вакансии
     * @param previousExplanation предыдущий сгенерированный текст (если есть) для контекстной связи
     * @return результат генерации с метаданными и идентификатором записи журнала
     */
    OpenPositionExplanationResult explainSimplifiedWithWebSearch(UUID openPositionId, String previousExplanation);
}
