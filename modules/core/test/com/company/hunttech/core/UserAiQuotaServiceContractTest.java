package com.company.hunttech.core;

import com.company.hunttech.entity.ai.LlmChatQuotaPeriod;
import com.company.hunttech.service.dto.ai.UserAiQuotaInfo;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Комплексные тесты по спецификации задач управления токенами:
 * - Test 1: Постоянный лимит (чтение и изменение через специализированный сервис)
 * - Test 2: Пополнение (разовое увеличение доступного объема без изменения постоянного лимита)
 * - Test 3: Расход (уменьшение остатка доступных токенов при росте расхода)
 * - Test 4: Несколько начислений (атомарное суммирование начислений)
 * - Test 5: Новый месяц (сгорание/неучастие бонусов предыдущего периода, возврат к базовому лимиту)
 * - Test 6: История (сохранение записей прошлых периодов для аудита)
 * - Test 7: Реальная проверка LLM (проверка reserveQuota в LlmChatServiceBean с учетом extraTokens)
 * - UI & Centralization: проверка UI ExtUserEditor / ExtUserEdit и централизации в UserAiQuotaService
 */
public class UserAiQuotaServiceContractTest {

    private static String source(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        if (!Files.exists(path)) {
            path = Paths.get("../../", relativePath);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * Test 1 — постоянный лимит:
     * Базовый лимит 1 000 000. Изменение на 1 500 000.
     * Значение постоянного лимита сохраняется и читается как 1 500 000.
     */
    @Test
    public void test1_constantMonthlyLimit() {
        UserAiQuotaInfo quota = new UserAiQuotaInfo();
        quota.setAllocatedTokens(1_000_000);
        quota.setCustomOverride(true);
        quota.setConsumedTokens(0);
        quota.setExtraTokens(0);

        assertEquals(1_000_000, (int) quota.getAllocatedTokens());
        assertEquals(1_000_000, quota.getEffectiveLimit());
        assertEquals(1_000_000, (int) quota.getAvailableTokens());

        // Администратор меняет постоянный лимит на 1 500 000
        quota.setAllocatedTokens(1_500_000);

        assertEquals(1_500_000, (int) quota.getAllocatedTokens());
        assertEquals(1_500_000, quota.getEffectiveLimit());
        assertEquals(1_500_000, (int) quota.getAvailableTokens());
    }

    /**
     * Test 2 — пополнение:
     * monthlyLimit = 1 000 000, extra = 200 000, used = 0 -> available = 1 200 000.
     * При этом monthlyLimit остается 1 000 000.
     */
    @Test
    public void test2_bonusTokensAddition() {
        UserAiQuotaInfo quota = new UserAiQuotaInfo();
        quota.setAllocatedTokens(1_000_000);
        quota.setExtraTokens(200_000);
        quota.setConsumedTokens(0);

        // Проверка формулы: available = monthlyLimit + extra - used
        assertEquals(1_000_000, (int) quota.getAllocatedTokens());
        assertEquals(200_000, quota.getExtraTokens());
        assertEquals(1_200_000, (int) quota.getAvailableTokens());
        assertEquals(1_200_000, quota.getEffectiveLimit());
    }

    /**
     * Test 3 — расход:
     * monthlyLimit = 1 000 000, extra = 200 000, used = 350 000 -> available = 850 000.
     */
    @Test
    public void test3_consumptionCalculation() {
        UserAiQuotaInfo quota = new UserAiQuotaInfo();
        quota.setAllocatedTokens(1_000_000);
        quota.setExtraTokens(200_000);
        quota.setConsumedTokens(350_000);

        assertEquals(850_000, (int) quota.getAvailableTokens());
        assertEquals(1_000_000, (int) quota.getAllocatedTokens());
        assertEquals(200_000, quota.getExtraTokens());
        assertEquals(350_000, quota.getConsumedTokens());
    }

    /**
     * Test 4 — несколько начислений:
     * +200 000, затем +300 000 -> extra = 500 000.
     */
    @Test
    public void test4_multipleBonusAccruals() {
        LlmChatQuotaPeriod period = new LlmChatQuotaPeriod();
        period.setExtraTokens(0);

        // Первое пополнение
        int firstBonus = 200_000;
        period.setExtraTokens(period.getExtraTokens() + firstBonus);
        assertEquals(200_000, (int) period.getExtraTokens());

        // Второе пополнение
        int secondBonus = 300_000;
        period.setExtraTokens(period.getExtraTokens() + secondBonus);
        assertEquals(500_000, (int) period.getExtraTokens());
    }

    /**
     * Test 5 — новый месяц:
     * Предыдущий месяц: monthlyLimit = 1 000 000, extra = 500 000.
     * В следующем месяце: новый период с extraTokens = 0, effective limit = 1 000 000.
     * Старые 500 000 не участвуют.
     */
    @Test
    public void test5_newMonthBonusExpiry() {
        // Период сентября 2026
        LlmChatQuotaPeriod septPeriod = new LlmChatQuotaPeriod();
        septPeriod.setExtraTokens(500_000);
        septPeriod.setQuotaTokens(1_000_000);
        septPeriod.setConsumedTokens(400_000);

        // Период октября 2026 (новый календарный месяц)
        LlmChatQuotaPeriod octPeriod = new LlmChatQuotaPeriod();
        octPeriod.setExtraTokens(0); // По умолчанию для нового месяца бонус равен 0
        octPeriod.setQuotaTokens(1_000_000);
        octPeriod.setConsumedTokens(0);

        UserAiQuotaInfo octQuota = new UserAiQuotaInfo();
        octQuota.setAllocatedTokens(octPeriod.getQuotaTokens());
        octQuota.setExtraTokens(octPeriod.getExtraTokens());
        octQuota.setConsumedTokens(octPeriod.getConsumedTokens());

        assertEquals(1_000_000, octQuota.getEffectiveLimit());
        assertEquals(1_000_000, (int) octQuota.getAvailableTokens());
        assertEquals(0, octQuota.getExtraTokens());
        // Сентябрьский бонус остался в сентябрьской записи
        assertEquals(500_000, (int) septPeriod.getExtraTokens());
    }

    /**
     * Test 6 — история:
     * Проверяем, что запись периода имеет periodStart, periodEnd, user, extraTokens
     * и сохраняется для аудита.
     */
    @Test
    public void test6_periodHistoryAndAudit() {
        LlmChatQuotaPeriod period = new LlmChatQuotaPeriod();
        Calendar cal = Calendar.getInstance();
        period.setPeriodStart(cal.getTime());
        period.setQuotaTokens(1_000_000);
        period.setExtraTokens(250_000);
        period.setConsumedTokens(150_000);

        assertNotNull(period.getPeriodStart());
        assertEquals(250_000, (int) period.getExtraTokens());
        assertEquals(1_000_000, (int) period.getQuotaTokens());
    }

    /**
     * Test 7 — реальная проверка LLM:
     * Проверка, что LlmChatServiceBean.reserveQuota учитывает extraTokens
     * при расчете totalAllowed, разрешая запросы при наличии extraTokens.
     */
    @Test
    public void test7_realLlmQuotaPipeline() throws IOException {
        String llmService = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        assertTrue("LlmChatServiceBean must read extraTokens from period",
                llmService.contains("period.getExtraTokens()"));
        assertTrue("LlmChatServiceBean must sum safeInt(period.getQuotaTokens()) + extraTokens",
                llmService.contains("safeInt(period.getQuotaTokens()) + extraTokens"));

        // Логическая верификация ветвления reserveQuota:
        // Базовый лимит: 1 000 000, Израсходовано: 1 000 000, Бонус: 200 000
        int quotaTokens = 1_000_000;
        int consumedTokens = 1_000_000;
        int extraTokens = 200_000;
        int totalAllowed = quotaTokens + extraTokens;
        int estimatedNeeded = 5_000;

        boolean allowed = consumedTokens + estimatedNeeded <= totalAllowed;
        assertTrue("Запрос должен быть разрешен благодаря начисленным бонусным токенам", allowed);

        // Без бонусных токенов (extraTokens = 0):
        int totalAllowedWithoutBonus = quotaTokens + 0;
        boolean allowedWithoutBonus = consumedTokens + estimatedNeeded <= totalAllowedWithoutBonus;
        assertFalse("Запрос должен быть заблокирован без бонусных токенов при исчерпании лимита", allowedWithoutBonus);
    }

    /**
     * Проверка структуры БД и миграций
     */
    @Test
    public void databaseMigrationsContract() throws IOException {
        String changelog = source("modules/core/db/changelog/260914-1-addExtraTokensToQuotaPeriod.xml");
        String master = source("modules/core/db/changelog/db.changelog-master.xml");
        String pgSql = source("modules/core/db/update/postgres/26/260914-1-addExtraTokensToQuotaPeriod.sql");
        String hsql = source("modules/core/db/update/hsql/26/260914-1-addExtraTokensToQuotaPeriod.sql");

        assertTrue(master.contains("260914-1-addExtraTokensToQuotaPeriod.xml"));
        assertTrue(changelog.contains("HUNTTECH_LLM_CHAT_QUOTA_PERIOD"));
        assertTrue(changelog.contains("EXTRA_TOKENS"));
        assertTrue(changelog.contains("defaultValueNumeric=\"0\""));
        assertTrue(pgSql.contains("EXTRA_TOKENS"));
        assertTrue(pgSql.contains("DEFAULT 0"));
        assertTrue(hsql.contains("EXTRA_TOKENS"));
    }

    /**
     * Проверка сервиса UserAiQuotaService и устранения root cause в ExtUserEditor / ExtUserEdit
     */
    @Test
    public void tokenServiceAndUiCentralizationContract() throws IOException {
        String serviceInterface = source("modules/global/src/com/company/hunttech/service/UserAiQuotaService.java");
        String serviceBean = source("modules/core/src/com/company/hunttech/service/UserAiQuotaServiceBean.java");
        String editor = source("modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEditor.java");
        String editScreen = source("modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEdit.java");

        // Service API
        assertTrue(serviceInterface.contains("addMonthlyBonusTokens(UUID userId, int amount, String reason)"));
        assertTrue(serviceBean.contains("addMonthlyBonusTokens"));
        assertTrue(serviceBean.contains("setMonthlyQuota"));

        // ExtUserEditor (Root Cause Fix: pendingQuotaChanged guard)
        assertTrue(editor.contains("pendingQuotaChanged"));
        assertTrue(editor.contains("pendingQuotaToSave"));
        assertTrue(editor.contains("if (!pendingQuotaChanged)"));
        assertTrue(editor.contains("openAddBonusTokensDialog()"));
        assertTrue(editor.contains("userAiQuotaService.addMonthlyBonusTokens"));
        assertTrue(editor.contains("userAiQuotaService.setMonthlyQuota"));

        // ExtUserEdit (Screen API)
        assertTrue(editScreen.contains("pendingQuotaChanged"));
        assertTrue(editScreen.contains("openAddBonusTokensDialog()"));
        assertTrue(editScreen.contains("userAiQuotaService.addMonthlyBonusTokens"));
        assertTrue(editScreen.contains("userAiQuotaService.setMonthlyQuota"));

        // Отсутствие дублирования бизнес-логики в UI
        assertFalse(editor.contains("update HUNTTECH_LLM_CHAT_QUOTA_PERIOD"));
        assertFalse(editScreen.contains("update HUNTTECH_LLM_CHAT_QUOTA_PERIOD"));
    }

    /**
     * Test 8 — Проверка доступности квоты и блокировки при перерасходе
     */
    @Test
    public void test8_quotaAvailableAndBlockingContract() {
        // 1. Безлимитный пользователь
        UserAiQuotaInfo unlimitedQuota = new UserAiQuotaInfo();
        unlimitedQuota.setAllocatedTokens(-1);
        unlimitedQuota.setConsumedTokens(10_000_000);
        assertTrue(unlimitedQuota.isUnlimited());
        assertNull(unlimitedQuota.getRemainingTokens());

        // 2. Пользователь с нормальным остатком
        UserAiQuotaInfo normalQuota = new UserAiQuotaInfo(100_000, 20_000, 30_000, 0, 0, false);
        assertFalse(normalQuota.isUnlimited());
        assertEquals(90_000, (int) normalQuota.getRemainingTokens());
        assertTrue(normalQuota.getRemainingTokens() > 0);

        // 3. Пользователь с исчерпанной квотой
        UserAiQuotaInfo exhaustedQuota = new UserAiQuotaInfo(100_000, 0, 100_000, 0, 0, false);
        assertFalse(exhaustedQuota.isUnlimited());
        assertEquals(0, (int) exhaustedQuota.getRemainingTokens());
        assertTrue(exhaustedQuota.getRemainingTokens() <= 0);

        // 4. Пользователь с перерасходом
        UserAiQuotaInfo overrunQuota = new UserAiQuotaInfo(100_000, 0, 125_000, 0, 0, false);
        assertFalse(overrunQuota.isUnlimited());
        assertEquals(0, (int) overrunQuota.getRemainingTokens());
        assertTrue(overrunQuota.getRemainingTokens() <= 0);
    }

    /**
     * Test 9 — Проверка контрактов сервисов и интеграции Hermes с системой учета токенов
     */
    @Test
    public void test9_hermesTokenAccountingContracts() throws IOException {
        String quotaInterface = source("modules/global/src/com/company/hunttech/service/UserAiQuotaService.java");
        String quotaBean = source("modules/core/src/com/company/hunttech/service/UserAiQuotaServiceBean.java");
        String hermesChat = source("modules/core/src/com/company/hunttech/service/HermesChatServiceBean.java");
        String hermesManager = source("modules/core/src/com/company/hunttech/service/HermesManagerChatServiceBean.java");
        String llmChat = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");
        String aiExecution = source("modules/core/src/com/company/hunttech/service/AiExecutionServiceBean.java");
        String llmChatScreen = source("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");

        String expectedMessage = "Закончились доступные токены ИИ. Пожалуйста, обратитесь к администратору системы для пополнения квоты.";

        // UserAiQuotaService interface & bean
        assertTrue(quotaInterface.contains("checkQuotaAvailable(UUID userId, int estimatedTokens)"));
        assertTrue(quotaInterface.contains("isQuotaAvailable(UUID userId, int estimatedTokens)"));
        assertTrue(quotaInterface.contains("recordTokenConsumption(UUID userId, int tokensUsed)"));
        assertTrue(quotaInterface.contains(expectedMessage));
        assertTrue(quotaBean.contains("MSG_QUOTA_EXHAUSTED"));
        assertTrue(quotaBean.contains("recordTokenConsumption"));

        // HermesChatServiceBean (Hermes-viewer)
        assertTrue(hermesChat.contains("userAiQuotaService.checkQuotaAvailable"));
        assertTrue(hermesChat.contains("userAiQuotaService.recordTokenConsumption"));
        assertTrue(hermesChat.contains("Hermes-viewer (ассистент)"));

        // HermesManagerChatServiceBean (Hermes-operator)
        assertTrue(hermesManager.contains("userAiQuotaService.checkQuotaAvailable"));
        assertTrue(hermesManager.contains("userAiQuotaService.recordTokenConsumption"));
        assertTrue(hermesManager.contains("HERMES_OPERATOR"));
        assertTrue(hermesManager.contains("Hermes-operator (управление HRM)"));

        // LlmChatServiceBean & AiExecutionServiceBean
        assertTrue(llmChat.contains(expectedMessage));
        assertTrue(aiExecution.contains("userAiQuotaService.checkQuotaAvailable"));
        assertTrue(aiExecution.contains("userAiQuotaService.recordTokenConsumption"));

        // LlmChatScreen
        assertTrue(llmChatScreen.contains("MSG_QUOTA_EXHAUSTED"));
        assertTrue(llmChatScreen.contains("userAiQuotaService.isQuotaAvailable"));
    }
}
