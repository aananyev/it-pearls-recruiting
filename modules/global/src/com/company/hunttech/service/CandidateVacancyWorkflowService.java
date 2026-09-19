package com.company.hunttech.service;

import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.VacancyCandidateMatchRun;
import com.company.hunttech.service.dto.BulkTakeIntoWorkResult;
import com.company.hunttech.service.dto.TakeIntoWorkResult;
import com.company.hunttech.service.dto.VacancyMatchMonitoringDto;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Оркестратор рабочего процесса рекрутера по результатам AI-подбора (Этап 3).
 *
 * <p>Обеспечивает строгое соблюдение принципа Human-in-the-loop:
 * фиксацию решений человека, защиту от создания параллельных сущностей (использование
 * существующей сущности {@link IteractionList}), идемпотентность добавления к вакансии,
 * выбор стандартизированных причин отказа из существующего справочника, AI-генерацию
 * персонализированных черновиков сообщений кандидатам без выдумывания условий.</p>
 */
public interface CandidateVacancyWorkflowService {

    String NAME = "hunttech_CandidateVacancyWorkflowService";

    /**
     * Стабильный код AI-функции генерации первого черновика предложения кандидату.
     */
    String FUNCTION_OUTREACH_DRAFT = "CANDIDATE_VACANCY_OUTREACH_DRAFT";

    /**
     * Берёт кандидата в работу по вакансии.
     * Если кандидат уже привязан к вакансии в IteractionList, дубликат не создаётся (идемпотентность).
     *
     * @param candidateId ID кандидата (JobCandidate)
     * @param vacancyId   ID вакансии (OpenPosition)
     * @param aiScore     оценка соответствия AI (0..100)
     * @param matchRunId  ID сессии подбора (VacancyCandidateMatchRun, опционально)
     * @return результат операции
     */
    TakeIntoWorkResult takeIntoWork(UUID candidateId, UUID vacancyId, Integer aiScore, UUID matchRunId);

    /**
     * Возвращает существующее взаимодействие (связь кандидата и вакансии в IteractionList), если оно есть.
     *
     * @param candidateId ID кандидата
     * @param vacancyId   ID вакансии
     * @return существующий IteractionList или null
     */
    IteractionList getExistingRelation(UUID candidateId, UUID vacancyId);

    /**
     * Пакетно добавляет кандидатов в работу по выбранной вакансии.
     * Пропускает кандидатов, которые уже находятся в работе.
     *
     * @param candidateIds список ID кандидатов
     * @param vacancyId    ID вакансии
     * @param matchRunId   ID сессии подбора (опционально)
     * @return сводный отчёт о пакетном добавлении
     */
    BulkTakeIntoWorkResult bulkTakeIntoWork(List<UUID> candidateIds, UUID vacancyId, UUID matchRunId);

    /**
     * Фиксирует отношение рекрутера к AI-рекомендации (POSTPONED, REJECTED)
     * с сохранением стандартизированной причины отказа и комментария.
     *
     * @param candidateId     ID кандидата
     * @param vacancyId       ID вакансии
     * @param decision        код решения (IN_WORK, POSTPONED, REJECTED)
     * @param rejectionReason причина отказа (из справочника hunttech_iteraction или комментарий)
     * @param comment         дополнительный комментарий рекрутера
     * @param aiScore         исходная оценка AI
     * @param matchRunId      ID сессии подбора
     */
    void recordFeedback(UUID candidateId, UUID vacancyId, String decision, String rejectionReason, String comment, Integer aiScore, UUID matchRunId);

    /**
     * Формирует предзаполненный объект взаимодействия (IteractionList)
     * для открытия стандартного экрана редактирования.
     *
     * @param candidateId         ID кандидата
     * @param vacancyId           ID вакансии
     * @param aiScore             оценка AI
     * @param matchedSkills       совпавшие ключевые навыки
     * @param missingRequirements требующие уточнения требования
     * @return подготовленный экземпляр IteractionList
     */
    IteractionList prepareInteractionDraft(UUID candidateId, UUID vacancyId, Integer aiScore, List<String> matchedSkills, List<String> missingRequirements);

    /**
     * Генерирует редактируемый персонализированный черновик предложения кандидату через AI Control Plane.
     * Не отправляет сообщение кандидату и не выдумывает условий/зарплат.
     *
     * @param candidateId    ID кандидата
     * @param vacancyId      ID вакансии
     * @param reasonsToOffer причины предложения из AI-отчёта
     * @param matchedSkills  совпадающие навыки
     * @return текст сообщения кандидату
     */
    String generateOutreachDraft(UUID candidateId, UUID vacancyId, List<String> reasonsToOffer, List<String> matchedSkills);

    /**
     * Создает или регистрирует сессию AI-подбора (VacancyCandidateMatchRun) для сохранения аудита.
     *
     * @param vacancyId       ID вакансии (или null, если подбор для кандидата)
     * @param candidateId     ID кандидата (или null, если подбор для вакансии)
     * @param aiFunctionCode  код функции AI
     * @param candidatesCount количество найденных кандидатов/вакансий
     * @return сохранённый объект VacancyCandidateMatchRun
     */
    VacancyCandidateMatchRun createMatchRun(UUID vacancyId, UUID candidateId, String aiFunctionCode, int candidatesCount);

    /**
     * Возвращает агрегированные метрики эффективности AI-подбора за указанный период.
     *
     * @param from начало периода
     * @param to   конец периода
     * @return статистика подбора
     */
    VacancyMatchMonitoringDto getMatchMonitoring(Date from, Date to);
}
