package com.company.hunttech.service;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.service.dto.CandidateSkillsEnrichmentKpiDto;
import com.company.hunttech.service.dto.CandidateSkillsScanResult;

import java.util.UUID;

/**
 * Единый оркестратор определения, актуализации и аудита навыков кандидатов (Candidate Skills Enrichment).
 * <p>
 * Разделяет UI-action и бизнес-логику анализа навыков:
 * <ul>
 *     <li>Ручной сценарий: JobCandidateReestr / JobCandidateEdit -> CandidateSkillEnrichmentService</li>
 *     <li>Фоновый сценарий: CandidateSkillsEnrichmentWorker -> CandidateSkillEnrichmentService</li>
 * </ul>
 * Результаты обоих сценариев полностью совместимы и используют единый pipeline нормализации,
 * сопоставления со справочником SkillTree и защиты ручных данных.
 */
public interface CandidateSkillEnrichmentService {

    String NAME = "hunttech_CandidateSkillEnrichmentService";

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
     * Регистрирует резюме кандидата в очереди фонового анализа навыков с заданным приоритетом.
     *
     * @param candidateCvId идентификатор CandidateCV
     * @param priority      приоритет в очереди (0 = фоновый, >=100 = экспресс)
     */
    void enqueueCandidateCv(UUID candidateCvId, int priority);

    /**
     * Выполняет анализ навыков резюме кандидата, сохраняет новые/обновленные CandidateSkill
     * и фиксирует состояние CandidateCvSkillAnalysis.
     *
     * @param candidate       кандидат (обязательно)
     * @param cv              резюме кандидата (обязательно)
     * @param aiFunctionCode  код AI-функции (если null, используется по умолчанию)
     * @param isBackground    признак фонового вызова (для политики FREE_ONLY и логирования)
     * @return структурированный результат со списком добавленных/обновленных навыков и дельтой
     */
    CandidateSkillsScanResult scanAndEnrich(JobCandidate candidate, CandidateCV cv, String aiFunctionCode, boolean isBackground);

    /**
     * Выполняет стандартный AI-анализ навыков, при необходимости обходя внутреннюю
     * проверку хэша и версии функции. Применяется только когда внешний бизнес-критерий
     * явно признал навыки устаревшими.
     */
    CandidateSkillsScanResult scanAndEnrich(JobCandidate candidate, CandidateCV cv, String aiFunctionCode,
                                            boolean isBackground, boolean forceScan);

    /**
     * Вычисляет SHA-256 хеш нормализованного текста резюме для отслеживания изменений.
     *
     * @param rawCvText исходный текст (HTML или raw text)
     * @return 64-символьная hex-строка SHA-256 или null, если текст пуст
     */
    String calculateNormalizedCvHash(String rawCvText);

    /**
     * Ставит резюме кандидата на повторный анализ (ручной Retry из мониторинга для ERROR или STALE).
     *
     * @param candidateCvId идентификатор CandidateCV
     */
    void reprocessCv(UUID candidateCvId);

    /**
     * Рассчитывает сводные агрегированные метрики для дашборда мониторинга (KPI).
     *
     * @return DTO со всеми счетчиками, прогрессом, токенами и скоростью
     */
    CandidateSkillsEnrichmentKpiDto getKpiMetrics();

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
     * Запускает один цикл фоновой обработки немедленно (для тестирования или ручного триггера).
     */
    void runWorkerCycleNow();

    /**
     * Возвращает признак использования только бесплатных нейросетей.
     */
    boolean isFreeOnly();

    /**
     * Устанавливает признак использования только бесплатных нейросетей.
     */
    void setFreeOnly(boolean freeOnly);
}
