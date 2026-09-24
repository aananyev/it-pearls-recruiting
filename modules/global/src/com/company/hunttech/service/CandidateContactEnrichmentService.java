package com.company.hunttech.service;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.JobHistory;
import com.company.hunttech.service.dto.CandidateContactsEnrichmentKpiDto;
import com.company.hunttech.service.dto.CandidateContactsScanResult;

import java.util.List;
import java.util.UUID;

/**
 * Единый оркестратор определения, актуализации и аудита контактных данных и фото кандидатов (Candidate Contact Enrichment).
 * <p>
 * Разделяет UI-action и бизнес-логику анализа контактов:
 * <ul>
 *     <li>Ручной сценарий: JobCandidateBrowse / JobCandidateEdit -> CandidateContactEnrichmentService</li>
 *     <li>Фоновый сценарий: CandidateContactsEnrichmentWorker -> CandidateContactEnrichmentService</li>
 * </ul>
 */
public interface CandidateContactEnrichmentService {

    String NAME = "hunttech_CandidateContactEnrichmentService";

    int PRIORITY_DEFAULT = 0;
    int PRIORITY_HIGH = 100;

    /**
     * Ставит резюме кандидата в приоритетную очередь фонового анализа с наивысшим приоритетом (PRIORITY_HIGH = 100)
     * и немедленно инициирует шаг воркера (сценарий: создание кандидата или загрузка нового резюме).
     *
     * @param candidateCvId идентификатор CandidateCV
     */
    void enqueueCandidateCvPriority(UUID candidateCvId);

    /**
     * Регистрирует резюме кандидата в очереди фонового анализа контактов с заданным приоритетом.
     *
     * @param candidateCvId идентификатор CandidateCV
     * @param priority      приоритет в очереди (0 = фоновый, >=100 = экспресс)
     */
    void enqueueCandidateCv(UUID candidateCvId, int priority);

    /**
     * Выполняет анализ контактов и фото резюме кандидата, сохраняет новые контактные данные
     * и фиксирует состояние CandidateCvContactAnalysis.
     *
     * @param candidate       кандидат (обязательно)
     * @param cv              резюме кандидата (обязательно)
     * @param aiFunctionCode  код AI-функции (если null, используется по умолчанию)
     * @param isBackground    признак фонового вызова (для политики FREE_ONLY и логирования)
     * @return результат сканирования контактов и фото
     */
    CandidateContactsScanResult scanAndEnrich(JobCandidate candidate, CandidateCV cv, String aiFunctionCode, boolean isBackground);

    /**
     * Выполняет стандартный AI-анализ контактов с возможностью принудительного повторного сканирования.
     */
    CandidateContactsScanResult scanAndEnrich(JobCandidate candidate, CandidateCV cv, String aiFunctionCode,
                                             boolean isBackground, boolean forceScan);

    /**
     * Выполняет AI-анализ контактов с возможностью принудительного повторного сканирования
     * и явного указания режима только бесплатных моделей.
     */
    CandidateContactsScanResult scanAndEnrich(JobCandidate candidate, CandidateCV cv, String aiFunctionCode,
                                             boolean isBackground, boolean forceScan, boolean freeOnly);

    /**
     * Вычисляет SHA-256 хеш нормализованного текста резюме для отслеживания изменений.
     *
     * @param rawCvText исходный текст
     * @return 64-символьная hex-строка SHA-256 или null, если текст пуст
     */
    String calculateNormalizedCvHash(String rawCvText);

    /**
     * Ставит резюме кандидата на повторный анализ контактов (ручной Retry из мониторинга для ERROR или STALE).
     *
     * @param candidateCvId идентификатор CandidateCV
     */
    void reprocessCv(UUID candidateCvId);

    /**
     * Рассчитывает сводные агрегированные метрики для дашборда мониторинга (KPI).
     *
     * @return DTO со всеми счетчиками, прогрессом, токенами и скоростью
     */
    CandidateContactsEnrichmentKpiDto getKpiMetrics();

    /**
     * Включает или выключает фоновый воркер (сохраняется в SYS_CONFIG через Config).
     *
     * @param enabled true для включения, false для остановки
     */
    void setWorkerEnabled(boolean enabled);

    /**
     * Проверяет, включен ли фоновый воркер.
     */
    boolean isWorkerEnabled();

    /**
     * Проверяет, включен ли режим вызова только бесплатных AI-моделей (freeModel = true).
     */
    boolean isFreeOnly();

    /**
     * Включает или выключает режим вызова только бесплатных AI-моделей (сохраняется в SYS_CONFIG).
     */
    void setFreeOnly(boolean freeOnly);

    /**
     * Запускает один цикл фоновой обработки немедленно (для тестирования или ручного триггера).
     */
    void runSingleCycleNow();

    /**
     * Выполняет распознавание мест работы кандидата по резюме и сохраняет их в сущность JobHistory.
     * В первую очередь проверяется оригинальный файл (PDF, DOCX, DOC, RTF, TXT), затем textCV.
     * Описания проектов, роли и достижений форматируются в читаемый HTML.
     * Должность сопоставляется с наиболее похожей из справочника hunttech_Position.
     *
     * @param candidate       кандидат
     * @param cv              резюме кандидата
     * @param forceReprocess  принудительное обновление, даже если места работы уже есть
     * @return список созданных или обновленных записей JobHistory
     */
    List<JobHistory> enrichWorkExperience(JobCandidate candidate, CandidateCV cv, boolean forceReprocess);

    /**
     * Выполняет распознавание мест работы с явным указанием режима только бесплатных моделей.
     *
     * @param candidate       кандидат
     * @param cv              резюме кандидата
     * @param forceReprocess  принудительное обновление
     * @param freeOnly        true - только бесплатные модели, false - любые по правилам fallback
     * @return список созданных или обновленных записей JobHistory
     */
    List<JobHistory> enrichWorkExperience(JobCandidate candidate, CandidateCV cv, boolean forceReprocess, boolean freeOnly);
}

