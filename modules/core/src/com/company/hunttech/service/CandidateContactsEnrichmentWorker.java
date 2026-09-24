package com.company.hunttech.service;

import com.company.hunttech.config.HunttechContactEnrichmentConfig;
import com.company.hunttech.entity.*;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.security.app.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Фоновый исполнитель обогащения контактов и фото кандидатов (Candidate Contacts Enrichment Worker).
 * <p>
 * Строгая однопоточная обработка (concurrency = 1, AI concurrency = 1),
 * безопасный троттлинг с настраиваемыми паузами, приоритетная выборка свежих резюме
 * и защита от зависания задач.
 */
@Component("hunttech_CandidateContactsEnrichmentWorker")
public class CandidateContactsEnrichmentWorker {

    private static final Logger log = LoggerFactory.getLogger(CandidateContactsEnrichmentWorker.class);

    @Inject
    private Configuration configuration;
    @Inject
    private Authentication authentication;
    @Inject
    private Persistence persistence;
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private CandidateContactEnrichmentService enrichmentService;

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private volatile long lastRequestTimestamp = 0;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hrm-contacts-enrichment-worker");
            t.setDaemon(true);
            return t;
        });

        // Запуск периодического тика каждые 10 секунд
        scheduler.scheduleWithFixedDelay(this::runWorkerTickSafe, 25, 10, TimeUnit.SECONDS);
        log.info("Фоновый воркер CandidateContactsEnrichmentWorker успешно инициализирован (период проверки 10 сек)");
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        log.info("Фоновый воркер CandidateContactsEnrichmentWorker остановлен");
    }

    /**
     * Немедленно инициирует шаг обработки очереди воркером в пуле потоков.
     */
    public void triggerImmediateProcessing() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.execute(this::runWorkerTickSafe);
        }
    }

    /**
     * Безопасная обертка для периодического тика воркера.
     */
    public void runWorkerTickSafe() {
        try {
            authentication.begin();
            processNextCandidateCv();
        } catch (Throwable t) {
            log.warn("Непредвиденная ошибка в тике CandidateContactsEnrichmentWorker: {}", t.getMessage());
        } finally {
            authentication.end();
        }
    }

    /**
     * Основной метод обработки одного CV из очереди.
     */
    public void processNextCandidateCv() {
        HunttechContactEnrichmentConfig cfg = configuration.getConfig(HunttechContactEnrichmentConfig.class);
        if (!cfg.getEnabled()) {
            return; // Служба выключена администратором
        }

        // 1. Проверка паузы между вызовами (delayBetweenRequestsSec)
        long now = System.currentTimeMillis();
        long minIntervalMs = Math.max(1, cfg.getDelayBetweenRequestsSec()) * 1000L;
        if (now - lastRequestTimestamp < minIntervalMs) {
            return; // Пауза троттлинга еще не истекла
        }

        // 2. Проверка атомарного флага выполнения (concurrency = 1)
        if (!isProcessing.compareAndSet(false, true)) {
            return; // Предыдущая обработка еще выполняется
        }

        try {
            // 3. Восстановление зависших задач (stuck in PROCESSING)
            recoverStuckProcessingTasks(Math.max(1, cfg.getStuckTimeoutMinutes()));

            // 4. Проверка лимита кандидатов в час
            int maxPerHour = Math.max(1, cfg.getMaxCandidatesPerHour());
            if (isHourlyLimitExceeded(maxPerHour)) {
                log.info("Достигнут почасовой лимит контактов ({}/час), воркер ожидает", maxPerHour);
                return;
            }

            // 5. Выборка следующего CV и перевод в статус PROCESSING в короткой транзакции
            NextTaskToProcess task = selectAndLockNextCandidateCv();
            if (task == null) {
                return; // Очередь пуста
            }

            log.info("CandidateContactsEnrichmentWorker взял в обработку кандидата {} (CV ID: {}, статус: {})",
                    task.candidateFullName, task.candidateCvId, task.initialStatus);

            lastRequestTimestamp = System.currentTimeMillis();

            // 6. Вызов AI и обогащение контактов (ВНЕ транзакции БД!)
            CandidateCV cv = dataManager.load(CandidateCV.class)
                    .id(task.candidateCvId)
                    .view("candidateCV-contact-enrichment-view")
                    .optional()
                    .orElse(null);

            JobCandidate candidate = dataManager.load(JobCandidate.class)
                    .id(task.candidateId)
                    .view("jobCandidate-contact-enrichment-view")
                    .optional()
                    .orElse(null);

            if (cv != null && candidate != null) {
                enrichmentService.scanAndEnrich(candidate, cv, cfg.getAiFunctionCode(), true);
            }

        } catch (Throwable t) {
            log.error("Ошибка при выполнении цикла CandidateContactsEnrichmentWorker: {}", t.getMessage(), t);
        } finally {
            isProcessing.set(false);
        }
    }

    private static class NextTaskToProcess {
        UUID candidateId;
        UUID candidateCvId;
        String candidateFullName;
        CandidateCvAnalysisStatus initialStatus;
    }

    /**
     * Короткая транзакция: выборка ровно одного наивысшеприоритетного резюме и перевод в PROCESSING.
     */
    private NextTaskToProcess selectAndLockNextCandidateCv() {
        HunttechContactEnrichmentConfig cfg = configuration.getConfig(HunttechContactEnrichmentConfig.class);
        int batchSize = Math.max(1, cfg.getBatchFetchSize());

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();

            // 0. Срочные задачи от рекрутеров (priority >= PRIORITY_HIGH)
            List<Object[]> priorityCandidates = em.createQuery(
                    "select a.id, a.candidate.id, a.candidateCv.id, c.fullName " +
                            "from hunttech_CandidateCvContactAnalysis a " +
                            "join a.candidate c " +
                            "where a.status = :naStatus and coalesce(a.priority, 0) >= :highPriority " +
                            "order by a.priority desc, a.createTs asc")
                    .setParameter("naStatus", CandidateCvAnalysisStatus.NOT_ANALYZED.getId())
                    .setParameter("highPriority", CandidateContactEnrichmentService.PRIORITY_HIGH)
                    .setMaxResults(batchSize)
                    .getResultList();

            for (Object[] row : priorityCandidates) {
                UUID analysisId = (UUID) row[0];
                UUID candidateId = (UUID) row[1];
                UUID cvId = (UUID) row[2];
                String name = (String) row[3];

                int updated = em.createQuery(
                        "update hunttech_CandidateCvContactAnalysis a " +
                                "set a.status = :procStatus, a.processingStartedAt = :now " +
                                "where a.id = :id and a.status = :expectedStatus")
                        .setParameter("procStatus", CandidateCvAnalysisStatus.PROCESSING.getId())
                        .setParameter("now", new Date())
                        .setParameter("id", analysisId)
                        .setParameter("expectedStatus", CandidateCvAnalysisStatus.NOT_ANALYZED.getId())
                        .executeUpdate();

                if (updated > 0) {
                    tx.commit();
                    NextTaskToProcess task = new NextTaskToProcess();
                    task.candidateId = candidateId;
                    task.candidateCvId = cvId;
                    task.candidateFullName = name;
                    task.initialStatus = CandidateCvAnalysisStatus.NOT_ANALYZED;
                    log.info("CandidateContactsEnrichmentWorker захватил СРОЧНУЮ задачу кандидата {} (CV ID: {})", name, cvId);
                    return task;
                }
            }

            // 1. Задачи в статусе RETRY с наступившим временем повтора
            List<Object[]> retryCandidates = em.createQuery(
                    "select a.id, a.candidate.id, a.candidateCv.id, c.fullName " +
                            "from hunttech_CandidateCvContactAnalysis a " +
                            "join a.candidate c " +
                            "where a.status = :retryStatus and a.nextRetryAt <= :now " +
                            "order by a.nextRetryAt asc")
                    .setParameter("retryStatus", CandidateCvAnalysisStatus.RETRY.getId())
                    .setParameter("now", new Date())
                    .setMaxResults(batchSize)
                    .getResultList();

            for (Object[] row : retryCandidates) {
                UUID analysisId = (UUID) row[0];
                UUID candidateId = (UUID) row[1];
                UUID cvId = (UUID) row[2];
                String name = (String) row[3];

                int updated = em.createQuery(
                        "update hunttech_CandidateCvContactAnalysis a " +
                                "set a.status = :procStatus, a.processingStartedAt = :now " +
                                "where a.id = :id and a.status = :expectedStatus")
                        .setParameter("procStatus", CandidateCvAnalysisStatus.PROCESSING.getId())
                        .setParameter("now", new Date())
                        .setParameter("id", analysisId)
                        .setParameter("expectedStatus", CandidateCvAnalysisStatus.RETRY.getId())
                        .executeUpdate();

                if (updated > 0) {
                    tx.commit();
                    NextTaskToProcess task = new NextTaskToProcess();
                    task.candidateId = candidateId;
                    task.candidateCvId = cvId;
                    task.candidateFullName = name;
                    task.initialStatus = CandidateCvAnalysisStatus.RETRY;
                    return task;
                }
            }

            // 2. Резюме в статусе STALE (по datePost desc)
            List<Object[]> staleCandidates = em.createQuery(
                    "select a.id, a.candidate.id, a.candidateCv.id, c.fullName " +
                            "from hunttech_CandidateCvContactAnalysis a " +
                            "join a.candidate c " +
                            "join a.candidateCv cv " +
                            "where a.status = :staleStatus " +
                            "order by cv.datePost desc")
                    .setParameter("staleStatus", CandidateCvAnalysisStatus.STALE.getId())
                    .setMaxResults(batchSize)
                    .getResultList();

            for (Object[] row : staleCandidates) {
                UUID analysisId = (UUID) row[0];
                UUID candidateId = (UUID) row[1];
                UUID cvId = (UUID) row[2];
                String name = (String) row[3];

                int updated = em.createQuery(
                        "update hunttech_CandidateCvContactAnalysis a " +
                                "set a.status = :procStatus, a.processingStartedAt = :now " +
                                "where a.id = :id and a.status = :expectedStatus")
                        .setParameter("procStatus", CandidateCvAnalysisStatus.PROCESSING.getId())
                        .setParameter("now", new Date())
                        .setParameter("id", analysisId)
                        .setParameter("expectedStatus", CandidateCvAnalysisStatus.STALE.getId())
                        .executeUpdate();

                if (updated > 0) {
                    tx.commit();
                    NextTaskToProcess task = new NextTaskToProcess();
                    task.candidateId = candidateId;
                    task.candidateCvId = cvId;
                    task.candidateFullName = name;
                    task.initialStatus = CandidateCvAnalysisStatus.STALE;
                    return task;
                }
            }

            // 3. Резюме в статусе NOT_ANALYZED (обычный приоритет, от свежих к старым: cv.datePost desc)
            List<Object[]> notAnalyzedList = em.createQuery(
                    "select a.id, a.candidate.id, a.candidateCv.id, c.fullName " +
                            "from hunttech_CandidateCvContactAnalysis a " +
                            "join a.candidate c " +
                            "join a.candidateCv cv " +
                            "where a.status = :naStatus and coalesce(a.priority, 0) < :highPriority " +
                            "order by cv.datePost desc")
                    .setParameter("naStatus", CandidateCvAnalysisStatus.NOT_ANALYZED.getId())
                    .setParameter("highPriority", CandidateContactEnrichmentService.PRIORITY_HIGH)
                    .setMaxResults(batchSize)
                    .getResultList();

            for (Object[] row : notAnalyzedList) {
                UUID analysisId = (UUID) row[0];
                UUID candidateId = (UUID) row[1];
                UUID cvId = (UUID) row[2];
                String name = (String) row[3];

                int updated = em.createQuery(
                        "update hunttech_CandidateCvContactAnalysis a " +
                                "set a.status = :procStatus, a.processingStartedAt = :now " +
                                "where a.id = :id and a.status = :expectedStatus")
                        .setParameter("procStatus", CandidateCvAnalysisStatus.PROCESSING.getId())
                        .setParameter("now", new Date())
                        .setParameter("id", analysisId)
                        .setParameter("expectedStatus", CandidateCvAnalysisStatus.NOT_ANALYZED.getId())
                        .executeUpdate();

                if (updated > 0) {
                    tx.commit();
                    NextTaskToProcess task = new NextTaskToProcess();
                    task.candidateId = candidateId;
                    task.candidateCvId = cvId;
                    task.candidateFullName = name;
                    task.initialStatus = CandidateCvAnalysisStatus.NOT_ANALYZED;
                    return task;
                }
            }

            // 4. Новые CV, для которых запись CandidateCvContactAnalysis еще не создавалась (по cv.datePost desc)
            List<Object[]> newCvs = em.createQuery(
                    "select cv.id, c.id, c.fullName " +
                            "from hunttech_CandidateCV cv " +
                            "join cv.candidate c " +
                            "where (cv.textCV is not null and length(trim(cv.textCV)) > 0 or cv.originalFileCV is not null or cv.fileCV is not null) " +
                            "and not exists (" +
                            "    select 1 from hunttech_CandidateCvContactAnalysis a where a.candidateCv.id = cv.id" +
                            ") " +
                            "order by cv.datePost desc")
                    .setMaxResults(batchSize)
                    .getResultList();

            for (Object[] row : newCvs) {
                UUID cvId = (UUID) row[0];
                UUID candidateId = (UUID) row[1];
                String name = (String) row[2];

                JobCandidate candidate = em.find(JobCandidate.class, candidateId);
                CandidateCV cv = em.find(CandidateCV.class, cvId);

                if (candidate != null && cv != null) {
                    try {
                        CandidateCvContactAnalysis a = metadata.create(CandidateCvContactAnalysis.class);
                        a.setCandidate(candidate);
                        a.setCandidateCv(cv);
                        a.setStatus(CandidateCvAnalysisStatus.PROCESSING);
                        a.setProcessingStartedAt(new Date());
                        em.persist(a);
                        tx.commit();

                        NextTaskToProcess task = new NextTaskToProcess();
                        task.candidateId = candidateId;
                        task.candidateCvId = cvId;
                        task.candidateFullName = name;
                        task.initialStatus = CandidateCvAnalysisStatus.NOT_ANALYZED;
                        return task;
                    } catch (Exception ex) {
                        log.debug("Конфликт захвата нового CV для контактов {}: {}", cvId, ex.getMessage());
                    }
                }
            }

            return null; // Очередь пуста
        } catch (Exception e) {
            log.warn("Ошибка при захвате задачи в CandidateContactsEnrichmentWorker: {}", e.getMessage());
            return null;
        }
    }

    private void recoverStuckProcessingTasks(int timeoutMinutes) {
        if (timeoutMinutes <= 0) timeoutMinutes = 15;
        Date threshold = new Date(System.currentTimeMillis() - timeoutMinutes * 60_000L);

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();
            List<CandidateCvContactAnalysis> stuck = em.createQuery(
                    "select a from hunttech_CandidateCvContactAnalysis a " +
                            "where a.status = :processingStatus and a.processingStartedAt <= :threshold",
                    CandidateCvContactAnalysis.class)
                    .setParameter("processingStatus", CandidateCvAnalysisStatus.PROCESSING.getId())
                    .setParameter("threshold", threshold)
                    .getResultList();

            for (CandidateCvContactAnalysis a : stuck) {
                log.warn("Восстановление зависшей задачи CandidateCvContactAnalysis ID: {} (CV ID: {}), возврат в RETRY",
                        a.getId(), a.getCandidateCv() != null ? a.getCandidateCv().getId() : null);
                a.setStatus(CandidateCvAnalysisStatus.RETRY);
                a.setLastError("Зависание обработки контактов более " + timeoutMinutes + " мин (сервер перезагружен или таймаут)");
                a.setNextRetryAt(new Date(System.currentTimeMillis() + 60_000L));
            }
            tx.commit();
        } catch (Exception e) {
            log.warn("Ошибка восстановления зависших задач контактов: {}", e.getMessage());
        }
    }

    private boolean isHourlyLimitExceeded(int maxPerHour) {
        if (maxPerHour <= 0) return false;
        Date oneHourAgo = new Date(System.currentTimeMillis() - 3600_000L);

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();
            Number count = (Number) em.createQuery(
                    "select count(a) from hunttech_CandidateCvContactAnalysis a " +
                            "where a.processingFinishedAt >= :hourAgo")
                    .setParameter("hourAgo", oneHourAgo)
                    .getSingleResult();
            return count != null && count.intValue() >= maxPerHour;
        } catch (Exception e) {
            return false;
        }
    }
}
