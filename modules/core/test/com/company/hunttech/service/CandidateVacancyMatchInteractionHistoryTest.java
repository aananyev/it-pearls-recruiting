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

        item.setPastInterviews(Arrays.asList("[15.08.2026] Сторона: На стороне заказчика | Рекрутер: Смирнова А. | Вакансия: Java Lead | Итоги: Пройдено успешно"));
        assertTrue(item.getPastInterviewsDisplay().contains("На стороне заказчика"));
        assertTrue(item.getPastInterviewsDisplay().contains("Смирнова А."));
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

    @Test
    public void testCandidateVacancyMatchItemSalaryFields() {
        CandidateVacancyMatchItem item = new CandidateVacancyMatchItem();
        item.setCandidateSalary("250 000 ₽");
        item.setVacancySalary("от 200 000 до 300 000 ₽");
        item.setSalaryFitAnalysis("Полное соответствие: ожидания укладываются в вилку заказчика");

        assertEquals("250 000 ₽", item.getCandidateSalary());
        assertEquals("от 200 000 до 300 000 ₽", item.getVacancySalary());
        assertEquals("Полное соответствие: ожидания укладываются в вилку заказчика", item.getSalaryFitAnalysis());
        assertEquals("Полное соответствие: ожидания укладываются в вилку заказчика", item.getSalaryFitDisplay());
    }

    @Test
    public void testSalaryOfferFormatting() {
        OpenPosition op = new OpenPosition();
        op.setSalaryMin(new java.math.BigDecimal("200000"));
        op.setSalaryMax(new java.math.BigDecimal("300000"));
        String offer = CandidateVacancyMatchAiServiceBean.formatVacancySalaryOffer(op);
        assertTrue(offer.contains("от 200 000 до 300 000 ₽"));

        op.setSalaryCandidateRequest(true);
        assertTrue(CandidateVacancyMatchAiServiceBean.formatVacancySalaryOffer(op).contains("по запросу кандидата"));

        OpenPosition op2 = new OpenPosition();
        op2.setSalaryCandidateRequest(true);
        assertEquals("По договоренности (по запросу кандидата)", CandidateVacancyMatchAiServiceBean.formatVacancySalaryOffer(op2));

        OpenPosition op3 = new OpenPosition();
        assertNull(CandidateVacancyMatchAiServiceBean.formatVacancySalaryOffer(op3));
    }

    @Test
    public void testCandidateSalaryExtractionFromResume() {
        String resume1 = "Иван Иванов. Желаемый доход: 250 000 руб. Опыт 5 лет.";
        String sal1 = CandidateVacancyMatchAiServiceBean.extractCandidateSalaryFromResume(resume1);
        assertNotNull(sal1);
        assertTrue(sal1.contains("250 000"));

        String resume2 = "З/п от 180 тыс. рублей. Senior Java Developer.";
        String sal2 = CandidateVacancyMatchAiServiceBean.extractCandidateSalaryFromResume(resume2);
        assertNotNull(sal2);
        assertTrue(sal2.contains("180 000"));
    }

    @Test
    public void testSalaryEvaluationAndFitting() {
        OpenPosition vacancy = new OpenPosition();
        vacancy.setSalaryMin(new java.math.BigDecimal("200000"));
        vacancy.setSalaryMax(new java.math.BigDecimal("300000"));

        // Case 1: In range
        String fitInRange = CandidateVacancyMatchAiServiceBean.evaluateSalaryFit("250 000 ₽", vacancy);
        assertTrue(fitInRange.contains("Полное соответствие"));

        // Case 2: Above max
        String fitAbove = CandidateVacancyMatchAiServiceBean.evaluateSalaryFit("350 000 ₽", vacancy);
        assertTrue(fitAbove.contains("превышают"));
        assertTrue(fitAbove.contains("16%"));

        // Case 3: Below min
        String fitBelow = CandidateVacancyMatchAiServiceBean.evaluateSalaryFit("150 000 ₽", vacancy);
        assertTrue(fitBelow.contains("ниже начальной вилки"));

        // Case 4: Only min specified and candidate >= min
        OpenPosition minOnlyVacancy = new OpenPosition();
        minOnlyVacancy.setSalaryMin(new java.math.BigDecimal("200000"));
        String fitMinOnly = CandidateVacancyMatchAiServiceBean.evaluateSalaryFit("250 000 ₽", minOnlyVacancy);
        assertTrue(fitMinOnly.contains("не ниже начального предложения"));

        // Case 5: Millions formatting and standalone pattern
        String resumeMillions = "Руководитель разработки. Ожидания: 1 500 000 руб.";
        String salMillions = CandidateVacancyMatchAiServiceBean.extractCandidateSalaryFromResume(resumeMillions);
        assertNotNull(salMillions);
        assertTrue(salMillions.contains("1 500 000"));

        // Case 6: Currency awareness ($ / USD)
        String fitUsd = CandidateVacancyMatchAiServiceBean.evaluateSalaryFit("3 000 $", vacancy);
        assertTrue(fitUsd.contains("валюте"));
        assertEquals("BY_AGREEMENT", CandidateVacancyMatchAiServiceBean.classifySalaryFitStatus("3 000 $", vacancy));

        // Case 7: Status classification
        assertEquals("IN_RANGE", CandidateVacancyMatchAiServiceBean.classifySalaryFitStatus("250 000 ₽", vacancy));
        assertEquals("ABOVE", CandidateVacancyMatchAiServiceBean.classifySalaryFitStatus("350 000 ₽", vacancy));
        assertEquals("BELOW", CandidateVacancyMatchAiServiceBean.classifySalaryFitStatus("150 000 ₽", vacancy));

        // Case 8: Decimal thousands parsing
        assertEquals(Long.valueOf(90500L), CandidateVacancyMatchAiServiceBean.parseSalaryNumericValue("90.5 тыс"));
        assertEquals(Long.valueOf(90500L), CandidateVacancyMatchAiServiceBean.parseSalaryNumericValue("90,5 тыс. руб."));

        // Case 9: CandidateVacancyMatchItem status display
        CandidateVacancyMatchItem item = new CandidateVacancyMatchItem();
        item.setSalaryFitStatus("IN_RANGE");
        assertEquals("В рамках вилки", item.getSalaryFitStatusDisplay());
    }
}
