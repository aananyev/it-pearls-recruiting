package com.company.hunttech.service;

import com.company.hunttech.entity.ai.LlmChatMessage;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Модульные тесты для LlmChatContextBuilder.
 * Проверяют математическую и фактологическую достоверность формирования
 * RuntimeContext, относительных календарных диапазонов и скользящего окна истории.
 */
public class LlmChatContextBuilderTest {

    private Date fixedNow;
    private TimeZone fixedTz;
    private Locale fixedLocale;

    @Before
    public void setUp() throws Exception {
        // Фиксированная дата теста: 2026-09-13 14:30:00 (Воскресенье)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        fixedTz = TimeZone.getTimeZone("Europe/Moscow");
        sdf.setTimeZone(fixedTz);
        fixedNow = sdf.parse("2026-09-13 14:30:00");
        fixedLocale = new Locale("ru", "RU");
    }

    /**
     * Базовый тест детерминированного Runtime Context:
     * Проверяет текущую дату, время, день недели и часовой пояс.
     */
    @Test
    public void testBuildRuntimeContext_DeterministicFixedDate() {
        String runtimeContext = LlmChatContextBuilder.buildRuntimeContext(fixedNow, fixedTz, fixedLocale);

        assertNotNull(runtimeContext);
        assertTrue(runtimeContext.contains("• Текущая дата (Current date): 2026-09-13"));
        assertTrue(runtimeContext.contains("• Текущее время (Current time): 14:30:00"));
        assertTrue(runtimeContext.contains("• Часовой пояс (Timezone): Europe/Moscow"));
        assertTrue(runtimeContext.contains("• День недели (Day of week): Воскресенье (Sunday)"));
        assertTrue(runtimeContext.contains("Понедельник (Monday) — 1-й день недели, Воскресенье (Sunday) — последний день."));
    }

    /**
     * Case 1: «Какая сегодня дата?»
     * В авторитетном Runtime Context должна быть строго дата 2026-09-13.
     */
    @Test
    public void testCase1_CurrentDate() {
        String runtimeContext = LlmChatContextBuilder.buildRuntimeContext(fixedNow, fixedTz, fixedLocale);

        assertTrue("RuntimeContext должен содержать Сегодня: 2026-09-13",
                runtimeContext.contains("- Сегодня (Today): 2026-09-13"));
    }

    /**
     * Case 2: «Что значит следующая неделя?»
     * Если сегодня воскресенье 2026-09-13, следующая неделя — строго 2026-09-14 — 2026-09-20.
     */
    @Test
    public void testCase2_NextWeekRange() {
        String runtimeContext = LlmChatContextBuilder.buildRuntimeContext(fixedNow, fixedTz, fixedLocale);

        assertTrue("Текущая неделя должна быть 2026-09-07 — 2026-09-13",
                runtimeContext.contains("- Текущая неделя (This week): 2026-09-07 — 2026-09-13"));
        assertTrue("Следующая неделя должна быть строго 2026-09-14 — 2026-09-20",
                runtimeContext.contains("- Следующая неделя (Next week): 2026-09-14 — 2026-09-20"));
        assertTrue("Обязательное правило должно содержать точный интервал следующей недели",
                runtimeContext.contains("Выражение «на следующей неделе» означает строго диапазон следующей недели: 2026-09-14 — 2026-09-20"));
    }

    /**
     * Case 3: «Запланируй это на следующую неделю»
     * Предыдущее сообщение должно сохраняться в истории для понимания слова «это»,
     * а диапазон следующей недели должен браться из runtime context.
     */
    @Test
    public void testCase3_ContextualHistoryWithNextWeek() {
        String runtimeContext = LlmChatContextBuilder.buildRuntimeContext(fixedNow, fixedTz, fixedLocale);

        LlmChatMessage prevUser = new LlmChatMessage();
        prevUser.setRole("USER");
        prevUser.setContent("Нужно провести техническое интервью с кандидатом Ивановым Алексеем");
        prevUser.setSequenceNo(1);

        LlmChatMessage prevAssistant = new LlmChatMessage();
        prevAssistant.setRole("ASSISTANT");
        prevAssistant.setContent("Кандидат Иванов Алексей рассмотрен на позицию Senior Java Developer.");
        prevAssistant.setSequenceNo(2);

        List<LlmChatMessage> history = Arrays.asList(prevUser, prevAssistant);
        String slidingHistory = LlmChatContextBuilder.buildSlidingHistory(history, 2000,
                "Запланируй это на следующую неделю", runtimeContext);

        String combined = LlmChatContextBuilder.combinePayload(slidingHistory, "Запланируй это на следующую неделю");

        assertTrue("История должна содержать реплику о кандидате Иванове для разрешения «это»",
                combined.contains("Ивановым Алексеем"));
        assertTrue("Payload должен содержать текущий запрос",
                combined.contains("Запланируй это на следующую неделю"));
        assertTrue("RuntimeContext содержит точные даты следующей недели 2026-09-14 — 2026-09-20",
                runtimeContext.contains("2026-09-14 — 2026-09-20"));
    }

    /**
     * Case 4: «Сделай это завтра»
     * При fixedNow = 2026-09-13 «завтра» должно быть строго 2026-09-14.
     */
    @Test
    public void testCase4_TomorrowDate() {
        String runtimeContext = LlmChatContextBuilder.buildRuntimeContext(fixedNow, fixedTz, fixedLocale);

        assertTrue("Завтра должно быть строго 2026-09-14",
                runtimeContext.contains("- Завтра (Tomorrow): 2026-09-14"));
        assertTrue("Правило завтра должно указывать дату 2026-09-14",
                runtimeContext.contains("Выражение «завтра» означает строго дату 2026-09-14"));
    }

    /**
     * Case 5: Использование истории
     * User: «Мы обсуждаем вакансию Java Developer»
     * Assistant: ...
     * User: «Какой у нее стек?»
     */
    @Test
    public void testCase5_HistoryStackReference() {
        LlmChatMessage msg1 = new LlmChatMessage();
        msg1.setRole("USER");
        msg1.setContent("Мы обсуждаем вакансию Java Developer в финтех-проект");
        msg1.setSequenceNo(1);

        LlmChatMessage msg2 = new LlmChatMessage();
        msg2.setRole("ASSISTANT");
        msg2.setContent("Принято, рассматриваем вакансию Java Developer.");
        msg2.setSequenceNo(2);

        String historyText = LlmChatContextBuilder.buildSlidingHistory(
                Arrays.asList(msg1, msg2), 2000, "Какой у нее стек?", "");

        String payload = LlmChatContextBuilder.combinePayload(historyText, "Какой у нее стек?");

        assertTrue(payload.contains("[История диалога]"));
        assertTrue(payload.contains("Пользователь: Мы обсуждаем вакансию Java Developer в финтех-проект"));
        assertTrue(payload.contains("Ассистент: Принято, рассматриваем вакансию Java Developer."));
        assertTrue(payload.contains("[Текущий запрос пользователя]\nКакой у нее стек?"));
    }

    /**
     * Case 6: Защита от галлюцинаций
     * Проверка наличия четких инструкций не выдумывать отсутствующие факты.
     */
    @Test
    public void testCase6_AntiHallucinationRulesInRuntimeContext() {
        String runtimeContext = LlmChatContextBuilder.buildRuntimeContext(fixedNow, fixedTz, fixedLocale);

        assertTrue(runtimeContext.contains("ОБЯЗАТЕЛЬНЫЕ ПРАВИЛА ИНТЕРПРЕТАЦИИ ВРЕМЕНИ:"));
        assertTrue(runtimeContext.contains("Если смысл временного выражения пользователя критически неоднозначен, задай краткий уточняющий вопрос."));
    }

    /**
     * Case 7: Различение даты события и текущей даты.
     * История содержит дату события (например 2026-08-20), а текущая дата — 2026-09-13.
     */
    @Test
    public void testCase7_DistinguishHistoricalDateFromCurrentDate() {
        String runtimeContext = LlmChatContextBuilder.buildRuntimeContext(fixedNow, fixedTz, fixedLocale);

        LlmChatMessage pastEventMsg = new LlmChatMessage();
        pastEventMsg.setRole("USER");
        pastEventMsg.setContent("20 августа 2026 года состоялось первое собеседование с кандидатом Сидоровым");
        pastEventMsg.setSequenceNo(1);

        String history = LlmChatContextBuilder.buildSlidingHistory(
                Collections.singletonList(pastEventMsg), 2000, "Когда было собеседование и сколько прошло времени?", runtimeContext);

        assertTrue("История сохраняет дату события", history.contains("20 августа 2026 года"));
        assertTrue("RuntimeContext содержит указание отличать дату события от текущей даты",
                runtimeContext.contains("четко отличай дату события от текущей даты выполнения запроса"));
        assertTrue("Текущая дата четко зафиксирована как 2026-09-13",
                runtimeContext.contains("(Current date): 2026-09-13"));
    }

    /**
     * Тест скользящего окна (Sliding Window):
     * При ограниченном лимите токенов старые сообщения отсекаются, а самые свежие сохраняются.
     */
    @Test
    public void testSlidingHistory_PreservesRecentMessagesOnTokenLimit() {
        List<LlmChatMessage> allMessages = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            LlmChatMessage msg = new LlmChatMessage();
            msg.setRole(i % 2 == 1 ? "USER" : "ASSISTANT");
            msg.setContent("Сообщение номер " + i + " с важным контекстом диалога.");
            msg.setSequenceNo(i);
            allMessages.add(msg);
        }

        // Бюджет токенов достаточен только для последних ~3-4 сообщений
        int limitedTokens = 250;
        String slidingHistory = LlmChatContextBuilder.buildSlidingHistory(
                allMessages, limitedTokens, "Финальный вопрос", "");

        assertNotNull(slidingHistory);
        assertFalse(slidingHistory.isEmpty());
        // Самое последнее сообщение обязательно должно войти
        assertTrue("Сообщение 10 должно сохраниться", slidingHistory.contains("Сообщение номер 10"));
        assertTrue("Сообщение 9 должно сохраниться", slidingHistory.contains("Сообщение номер 9"));
        // Старое сообщение 1 должно быть отсечено
        assertFalse("Сообщение 1 должно быть отсечено из-за лимита токенов", slidingHistory.contains("Сообщение номер 1 "));
    }

    /**
     * Тест скользящего окна: если бюджет токенов крайне мал (не помещается даже одно сообщение),
     * возвращается пустая строка, чтобы запрос пользователя не сломался.
     */
    @Test
    public void testSlidingHistory_ZeroBudgetReturnsEmpty() {
        LlmChatMessage massiveMsg = new LlmChatMessage();
        massiveMsg.setRole("ASSISTANT");
        massiveMsg.setContent("Огромный текст ответа, который не помещается в мизерный лимит...");
        massiveMsg.setSequenceNo(1);

        String result = LlmChatContextBuilder.buildSlidingHistory(
                Collections.singletonList(massiveMsg), 50, "Вопрос пользователя", "Большой runtime context");

        assertEquals("При нехватке бюджета история должна быть пустой", "", result);
    }

    /**
     * Проверка корректного вычисления знака для отрицательных часовых поясов (включая нецелые часы).
     */
    @Test
    public void testBuildRuntimeContext_NegativeTimezoneOffset() {
        TimeZone tzNewYork = TimeZone.getTimeZone("America/New_York");
        String ctxNewYork = LlmChatContextBuilder.buildRuntimeContext(fixedNow, tzNewYork, fixedLocale);
        assertTrue("Должен содержать отрицательное смещение UTC-04:00", ctxNewYork.contains("UTC-04:00"));

        TimeZone tzStJohns = TimeZone.getTimeZone("America/St_Johns");
        String ctxStJohns = LlmChatContextBuilder.buildRuntimeContext(fixedNow, tzStJohns, fixedLocale);
        assertTrue("Должен содержать отрицательное смещение UTC-02:30", ctxStJohns.contains("UTC-02:30"));
    }

    /**
     * Проверка консервативной оценки токенов (единый алгоритм с LlmChatServiceBean).
     */
    @Test
    public void testEstimateTokens_ConsistentAlgorithm() {
        assertEquals(0, LlmChatContextBuilder.estimateTokens(null));
        assertEquals(0, LlmChatContextBuilder.estimateTokens(""));
        assertEquals(1, LlmChatContextBuilder.estimateTokens("A"));
        // Слово из 10 символов кириллицы: (10*3+4)/5 = 34/5 = 6 токенов
        assertEquals(6, LlmChatContextBuilder.estimateTokens("Разработка"));
    }
}
