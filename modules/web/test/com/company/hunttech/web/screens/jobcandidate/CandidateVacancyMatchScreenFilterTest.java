package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.CandidateVacancyMatchItem;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Тестирование логики фильтрации нерелевантных кандидатов и вакансий в CandidateVacancyMatchScreen.
 */
public class CandidateVacancyMatchScreenFilterTest {

    @Test
    public void testIsNotRecommendedWithStandardVerdicts() {
        CandidateVacancyMatchItem good = new CandidateVacancyMatchItem();
        good.setScore(85);
        good.setVerdict("Рекомендуется предложить");
        assertFalse("Высокий балл и вердикт 'Рекомендуется предложить' должны быть релевантны",
                CandidateVacancyMatchScreen.isNotRecommended(good));

        CandidateVacancyMatchItem consider = new CandidateVacancyMatchItem();
        consider.setScore(70);
        consider.setVerdict("Имеет смысл рассмотреть");
        assertFalse("Вердикт 'Имеет смысл рассмотреть' должен быть релевантен",
                CandidateVacancyMatchScreen.isNotRecommended(consider));

        CandidateVacancyMatchItem weak = new CandidateVacancyMatchItem();
        weak.setScore(50);
        weak.setVerdict("Слабое соответствие");
        assertFalse("Вердикт 'Слабое соответствие' должен оставаться в таблице",
                CandidateVacancyMatchScreen.isNotRecommended(weak));
    }

    @Test
    public void testIsNotRecommendedWithNegativeVerdicts() {
        CandidateVacancyMatchItem notRecommendedRu = new CandidateVacancyMatchItem();
        notRecommendedRu.setScore(40);
        notRecommendedRu.setVerdict("Не рекомендуется");
        assertTrue("Вердикт 'Не рекомендуется' должен определяться как нерелевантный",
                CandidateVacancyMatchScreen.isNotRecommended(notRecommendedRu));

        CandidateVacancyMatchItem notRecommendedVerb = new CandidateVacancyMatchItem();
        notRecommendedVerb.setScore(35);
        notRecommendedVerb.setVerdict("Не рекомендовать");
        assertTrue("Вердикт 'Не рекомендовать' должен определяться как нерелевантный",
                CandidateVacancyMatchScreen.isNotRecommended(notRecommendedVerb));

        CandidateVacancyMatchItem notRecommendedEn = new CandidateVacancyMatchItem();
        notRecommendedEn.setScore(25);
        notRecommendedEn.setVerdict("NOT_RECOMMENDED");
        assertTrue("Вердикт 'NOT_RECOMMENDED' должен определяться как нерелевантный",
                CandidateVacancyMatchScreen.isNotRecommended(notRecommendedEn));

        CandidateVacancyMatchItem notRecommendedSpace = new CandidateVacancyMatchItem();
        notRecommendedSpace.setScore(20);
        notRecommendedSpace.setVerdict("not recommended");
        assertTrue("Вердикт 'not recommended' должен определяться как нерелевантный",
                CandidateVacancyMatchScreen.isNotRecommended(notRecommendedSpace));
    }

    @Test
    public void testIsNotRecommendedByLowScoreOrUnrated() {
        CandidateVacancyMatchItem lowScore = new CandidateVacancyMatchItem();
        lowScore.setScore(30);
        lowScore.setVerdict(null);
        assertTrue("Балл ниже 45 без вердикта должен считаться нерелевантным",
                CandidateVacancyMatchScreen.isNotRecommended(lowScore));

        CandidateVacancyMatchItem zeroScore = new CandidateVacancyMatchItem();
        zeroScore.setScore(0);
        zeroScore.setVerdict("Не оценен AI");
        assertTrue("Не оцененный AI с нулевым баллом должен считаться нерелевантным",
                CandidateVacancyMatchScreen.isNotRecommended(zeroScore));
    }

    @Test
    public void testIsNotRecommendedNullSafety() {
        assertFalse("Null item должен обрабатываться безопасно",
                CandidateVacancyMatchScreen.isNotRecommended(null));
    }
}
