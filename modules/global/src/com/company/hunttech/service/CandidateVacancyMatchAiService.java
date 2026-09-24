package com.company.hunttech.service;

import com.company.hunttech.dto.CandidateVacancyMatchReport;
import com.company.hunttech.dto.CandidateVacancyMatchProgress;

import java.util.UUID;

/**
 * Сервис интеллектуального сопоставления кандидата и открытых вакансий HRM HuntTech.
 *
 * <p>Анализирует профиль и текст резюме кандидата относительно всех открытых вакансий
 * (где {@code openClose != true}), ранжирует их по экспертной оценке соответствия (score 0..100)
 * и формирует структурированный аналитический отчёт с обоснованием, совпадающими навыками,
 * критическими пробелами и рисками.</p>
 */
public interface CandidateVacancyMatchAiService {

    String NAME = "hunttech_CandidateVacancyMatchAiService";

    /**
     * Стабильный код системной AI-функции анализа соответствия кандидата и вакансий.
     */
    String FUNCTION_CODE = "CANDIDATE_VACANCY_MATCH_ANALYZE";

    /**
     * Параметр контекста: структурированный профиль кандидата.
     */
    String PARAM_CANDIDATE_PROFILE = "candidateProfile";

    /**
     * Параметр контекста: навыки кандидата из базы данных.
     */
    String PARAM_CANDIDATE_SKILLS = "candidateSkills";

    /**
     * Параметр контекста: текст резюме кандидата.
     */
    String PARAM_CANDIDATE_RESUME_TEXT = "candidateResumeText";

    /**
     * Параметр контекста: JSON-массив вакансий для текущего аналитического батча.
     */
    String PARAM_VACANCIES_JSON = "vacanciesJson";

    /**
     * Пороговые значения оценки соответствия (0..100) по шкале AI-функции CANDIDATE_VACANCY_MATCH_ANALYZE.
     */
    int SCORE_THRESHOLD_RECOMMEND = 80;
    int SCORE_THRESHOLD_CONSIDER = 65;
    int SCORE_THRESHOLD_WEAK_MATCH = 45;

    /**
     * Канонические текстовые вердикты AI.
     */
    String VERDICT_RECOMMENDED = "Рекомендуется предложить";
    String VERDICT_CONSIDER = "Имеет смысл рассмотреть";
    String VERDICT_WEAK_MATCH = "Слабое соответствие";
    String VERDICT_NOT_RECOMMENDED = "Не рекомендуется";
    String VERDICT_NOT_RECOMMENDED_CODE = "NOT_RECOMMENDED";
    String VERDICT_NOT_EVALUATED = "Не оценен AI";

    /**
     * Выполняет глубокий анализ соответствия кандидата всем открытым вакансиям компании.
     *
     * @param candidateId уникальный идентификатор кандидата (JobCandidate)
     * @return структурированный отчёт с ранжированным списком подходящих вакансий
     */
    CandidateVacancyMatchReport matchVacanciesForCandidate(UUID candidateId);

    /**
     * Выполняет анализ с идентификатором операции, по которому web-клиент может
     * получать фактический прогресс завершения аналитических чанков.
     *
     * @param candidateId уникальный идентификатор кандидата
     * @param operationId заранее созданный идентификатор операции; при {@code null}
     *                    сервис создаёт внутренний идентификатор
     * @return итоговый структурированный отчёт
     */
    CandidateVacancyMatchReport matchVacanciesForCandidate(UUID candidateId, UUID operationId);

    /**
     * Возвращает последний потокобезопасный снимок прогресса операции.
     *
     * @param operationId идентификатор, переданный в метод запуска
     * @return снимок прогресса или {@code null}, если операция неизвестна либо её TTL истёк
     */
    CandidateVacancyMatchProgress getVacancyMatchProgress(UUID operationId);

    /**
     * Выполняет интеллектуальный подбор наиболее подходящих кандидатов для выбранной открытой вакансии (Этап 2 и Этап 3).
     *
     * @param openPositionId уникальный идентификатор вакансии (OpenPosition)
     * @return структурированный отчёт с ранжированным списком кандидатов
     */
    CandidateVacancyMatchReport matchCandidatesForVacancy(UUID openPositionId);

    /**
     * Выполняет подбор кандидатов с заданным идентификатором для сквозной диагностики.
     *
     * @param openPositionId уникальный идентификатор вакансии
     * @param operationId идентификатор попытки, создаваемый вызывающей стороной
     * @return структурированный отчёт с ранжированным списком кандидатов
     */
    CandidateVacancyMatchReport matchCandidatesForVacancy(UUID openPositionId, UUID operationId);
}
