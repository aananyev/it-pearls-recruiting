package com.company.hunttech.listeners;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.service.CandidateContactEnrichmentService;
import com.company.hunttech.service.CandidateSkillEnrichmentService;
import com.haulmont.cuba.core.app.events.EntityChangedEvent;
import com.haulmont.cuba.security.app.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Слушатель жизненного цикла резюме (CandidateCV).
 * <p>
 * При создании нового резюме или обновлении текста/файла резюме (в карточке кандидата,
 * форме резюме, парсерах или умном импорте) автоматически ставит кандидата
 * в начало очередей сервисов «Фоновое определение навыков» и «Фоновое определение контактов»
 * с наивысшим приоритетом (PRIORITY_HIGH = 100) и немедленно инициирует шаги воркеров.
 */
@Component("hunttech_CandidateCvChangedListener")
public class CandidateCvChangedListener {

    private static final Logger log = LoggerFactory.getLogger(CandidateCvChangedListener.class);

    @Inject
    private CandidateSkillEnrichmentService enrichmentService;
    @Inject
    private CandidateContactEnrichmentService contactEnrichmentService;
    @Inject
    private Authentication authentication;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCandidateCvAfterCommit(EntityChangedEvent<CandidateCV, UUID> event) {
        if (event.getType() == EntityChangedEvent.Type.DELETED) {
            return;
        }

        boolean isNew = (event.getType() == EntityChangedEvent.Type.CREATED);
        boolean contentChanged = (event.getType() == EntityChangedEvent.Type.UPDATED
                && (event.getChanges().isChanged("textCV")
                || event.getChanges().isChanged("originalFileCV")
                || event.getChanges().isChanged("fileCV")));

        if (!isNew && !contentChanged) {
            return;
        }

        UUID cvId = event.getEntityId().getValue();
        log.info("Перехвачено событие {} для CandidateCV ID: {} -> постановка в приоритетную очередь анализа навыков и контактов",
                event.getType(), cvId);

        try {
            authentication.begin();
            enrichmentService.enqueueCandidateCvPriority(cvId);
            contactEnrichmentService.enqueueCandidateCvPriority(cvId);
        } catch (Exception e) {
            log.warn("Ошибка при автоматической постановке CandidateCV {} в очередь анализа: {}",
                    cvId, e.getMessage());
        } finally {
            authentication.end();
        }
    }
}
