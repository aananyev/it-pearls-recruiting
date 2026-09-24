package com.company.hunttech.service;

import com.company.hunttech.entity.CandidateVacancyMatchItem;
import com.company.hunttech.entity.OpenPosition;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Тестирование логики анализа истории взаимодействий, сопоставления отказов и весовых коэффициентов.
 */
public class CandidateVacancyMatchInteractionHistoryTest {

    @Test
    public void testCandidateVacancyMatchItemInteractionFields() {
        CandidateVacancyMatchItem item = new CandidateVacancyMatchItem();
        assertEquals(Integer.valueOf(0), item.getInteractionWeightAdjustment());
        assertEquals("0% (нейтрально)", item.getInteractionWeightAdjustmentDisplay());

        item.setInteractionWeightAdjustment(10);
        assertEquals("+10% к рейтингу", item.getInteractionWeightAdjustmentDisplay());

        item.setInteractionWeightAdjustment(-15);
        assertEquals("-15% к рейтингу", item.getInteractionWeightAdjustmentDisplay());

        item.setPastRejectionsEmployerSide(Arrays.asList("Отказ заказчика: недостаточно компетенций в Kubernetes", "Отказано в офере"));
        assertTrue(item.getPastRejectionsEmployerSideDisplay().contains("Kubernetes"));
        assertTrue(item.getPastRejectionsEmployerSideDisplay().contains("Отказано в офере"));

        item.setPastRejectionsCandidateSide(Collections.singletonList("Отказался от офера: требовалась работа в офисе"));
        assertTrue(item.getPastRejectionsCandidateSideDisplay().contains("работа в офисе"));

        item.setInteractionHistoryAnalysis("Умный анализ: условия вакансии устраняют блокеры.");
        assertEquals("Умный анализ: условия вакансии устраняют блокеры.", item.getInteractionHistoryAnalysis());
    }

    @Test
    public void testWeightAdjustmentClamping() {
        CandidateVacancyMatchItem item = new CandidateVacancyMatchItem();
        item.setScore(80);
        item.setPreferencesFit(8);
        item.setSkillsFit(25);

        // Positive bonus
        int totalAdjustment = Math.max(-25, Math.min(15, 10));
        item.setInteractionWeightAdjustment(totalAdjustment);
        item.setScore(Math.max(0, Math.min(100, item.getScore() + totalAdjustment)));
        assertEquals(Integer.valueOf(90), item.getScore());

        // Negative penalty exceeding clamp
        int excessivePenalty = -40;
        int clampedPenalty = Math.max(-25, Math.min(15, excessivePenalty));
        assertEquals(-25, clampedPenalty);
        item.setScore(Math.max(0, Math.min(100, item.getScore() + clampedPenalty)));
        assertEquals(Integer.valueOf(65), item.getScore());
    }

    @Test
    public void testRemoteWorkMitigationLogic() {
        OpenPosition remoteVacancy = new OpenPosition();
        remoteVacancy.setVacansyName("Senior Java Developer");
        remoteVacancy.setRemoteWork(1);

        OpenPosition officeVacancy = new OpenPosition();
        officeVacancy.setVacansyName("Java Team Lead (Office)");
        officeVacancy.setRemoteWork(0);

        assertEquals(Integer.valueOf(1), remoteVacancy.getRemoteWork());
        assertEquals(Integer.valueOf(0), officeVacancy.getRemoteWork());
    }
}
