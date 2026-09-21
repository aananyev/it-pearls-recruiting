package com.company.hunttech.service;

import com.company.hunttech.dto.CandidateVacancyMatchProgress;
import com.company.hunttech.entity.CandidateVacancyMatchItem;
import com.company.hunttech.entity.OpenPosition;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Контракт фактического прогресса, фильтра открытых вакансий и итоговой сортировки.
 */
public class CandidateVacancyMatchProgressTest {

    @Test
    public void progressUsesCompletedVacanciesAndTracksPartialFailures() {
        UUID operationId = UUID.randomUUID();
        CandidateVacancyMatchAiServiceBean.MatchProgressState state =
                new CandidateVacancyMatchAiServiceBean.MatchProgressState(operationId, System.currentTimeMillis());

        state.beginAnalysis(25, 3);
        CandidateVacancyMatchProgress beforeFirstChunk = state.snapshot(System.currentTimeMillis());
        assertEquals(0, beforeFirstChunk.getProgressPercent());
        assertNull("ETA неизвестен до завершения первого пакета", beforeFirstChunk.getEstimatedRemainingMillis());

        state.completeChunk(12, true);
        CandidateVacancyMatchProgress afterFirstChunk = state.snapshot(System.currentTimeMillis());
        assertEquals(12, afterFirstChunk.getProcessedVacancies());
        assertEquals(1, afterFirstChunk.getCompletedChunks());
        assertEquals(48, afterFirstChunk.getProgressPercent());
        assertNotNull("После первого пакета ETA должен быть рассчитан", afterFirstChunk.getEstimatedRemainingMillis());

        state.completeChunk(12, false);
        CandidateVacancyMatchProgress afterPartialFailure = state.snapshot(System.currentTimeMillis());
        assertEquals(24, afterPartialFailure.getProcessedVacancies());
        assertEquals(2, afterPartialFailure.getCompletedChunks());
        assertEquals(1, afterPartialFailure.getFailedChunks());
        assertEquals(96, afterPartialFailure.getProgressPercent());

        state.completeChunk(1, true);
        CandidateVacancyMatchProgress finalizing = state.snapshot(System.currentTimeMillis());
        assertEquals("FINALIZING", finalizing.getPhase());
        assertEquals(99, finalizing.getProgressPercent());

        state.finish(true, null);
        CandidateVacancyMatchProgress completed = state.snapshot(System.currentTimeMillis());
        assertTrue(completed.isCompleted());
        assertTrue(completed.isSuccess());
        assertEquals("COMPLETED_WITH_WARNINGS", completed.getPhase());
        assertEquals(100, completed.getProgressPercent());
        assertEquals(Long.valueOf(0L), completed.getEstimatedRemainingMillis());
    }

    @Test
    public void openFilterAcceptsFalseAndNullOnly() {
        OpenPosition explicitlyOpen = new OpenPosition();
        explicitlyOpen.setOpenClose(false);
        OpenPosition legacyOpen = new OpenPosition();
        legacyOpen.setOpenClose(null);
        OpenPosition closed = new OpenPosition();
        closed.setOpenClose(true);

        assertTrue(CandidateVacancyMatchAiServiceBean.isOpenVacancy(explicitlyOpen));
        assertTrue(CandidateVacancyMatchAiServiceBean.isOpenVacancy(legacyOpen));
        assertFalse(CandidateVacancyMatchAiServiceBean.isOpenVacancy(closed));
        assertFalse(CandidateVacancyMatchAiServiceBean.isOpenVacancy(null));
    }

    @Test
    public void productionComparatorSortsByScorePriorityAndName() {
        CandidateVacancyMatchItem lowerScore = item("Java", 75, 9);
        CandidateVacancyMatchItem higherScore = item("Architect", 90, 1);
        CandidateVacancyMatchItem highPriority = item("DevOps", 75, 10);
        CandidateVacancyMatchItem alphabetical = item("Alpha", 75, 9);
        List<CandidateVacancyMatchItem> items = new ArrayList<>(
                Arrays.asList(lowerScore, higherScore, highPriority, alphabetical));

        items.sort(new CandidateVacancyMatchAiServiceBean().getComparator());

        assertEquals(Arrays.asList(higherScore, highPriority, alphabetical, lowerScore), items);
    }

    private CandidateVacancyMatchItem item(String vacancyName, int score, int priority) {
        CandidateVacancyMatchItem item = new CandidateVacancyMatchItem();
        item.setVacancyName(vacancyName);
        item.setScore(score);
        item.setPriority(priority);
        return item;
    }
}
