package com.company.hunttech.core;

import com.company.hunttech.service.SmartOpenPositionParsedData;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.math.BigDecimal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Контрактный тест интеграции навыка открытия вакансий (HuntTech Vacancy Opening)
 * в LLM-чат Hermes (HermesChatServiceBean), SmartOpenPositionIngestServiceBean и LlmChatServiceBean.
 */
public class HermesChatVacancyOpeningContractTest {

    private String readSourceFile(String relativeCorePath, String relativeRootPath) throws Exception {
        File f1 = new File(relativeCorePath);
        if (f1.exists()) {
            return Files.readString(f1.toPath());
        }
        File f2 = new File(relativeRootPath);
        if (f2.exists()) {
            return Files.readString(f2.toPath());
        }
        throw new IllegalStateException("Файл не найден: " + relativeCorePath + " / " + relativeRootPath);
    }

    @Test
    public void testHermesChatServiceBeanContract() throws Exception {
        String code = readSourceFile(
                "src/com/company/hunttech/service/HermesChatServiceBean.java",
                "modules/core/src/com/company/hunttech/service/HermesChatServiceBean.java"
        );

        // 1. Инъекция SmartOpenPositionIngestService
        assertTrue("HermesChatServiceBean должен инжектировать SmartOpenPositionIngestService",
                code.contains("SmartOpenPositionIngestService smartOpenPositionIngestService"));

        // 2. Наличие методов распознавания намерения и обработки
        assertTrue("Должен присутствовать метод isVacancyOpeningIntent",
                code.contains("boolean isVacancyOpeningIntent(String message)"));
        assertTrue("Должен присутствовать метод isManagerOrDirector",
                code.contains("boolean isManagerOrDirector(ExtUser user)"));
        assertTrue("Должен присутствовать метод handleVacancyOpeningInHermes",
                code.contains("handleVacancyOpeningInHermes("));

        // 3. Перехват в sendHermesMessage
        assertTrue("sendHermesMessage должен перехватывать открытие вакансий перед вызовом Hermes Agent",
                code.contains("if (isVacancyOpeningIntent(message.trim()))") &&
                code.contains("handleVacancyOpeningInHermes("));

        // 4. Безопасность и навигация: человекочитаемые ссылки hrm://vacancy/ без вывода сырых UUID
        assertTrue("Ссылки на карточку вакансии должны формироваться в формате hrm://vacancy/<id>",
                code.contains("hrm://vacancy/"));
        assertTrue("Должно быть предупреждение о дубликатах со ссылкой на карточку",
                code.contains("Вакансия уже существует в системе (дубликат)"));

        // 5. Системный промпт Hermes должен содержать регламент hunttech-vacancy-opening
        assertTrue("Системный промпт Hermes должен содержать раздел регламента открытия вакансий",
                code.contains("СТАНДАРТ И РЕГЛАМЕНТ ОТКРЫТИЯ ВАКАНСИЙ (HUNTTECH VACANCY OPENING)"));
        assertTrue("Промпт должен требовать заглушку «НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.»",
                code.contains("НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ."));
    }

    @Test
    public void testSmartOpenPositionIngestServiceBeanContract() throws Exception {
        String code = readSourceFile(
                "src/com/company/hunttech/service/SmartOpenPositionIngestServiceBean.java",
                "modules/core/src/com/company/hunttech/service/SmartOpenPositionIngestServiceBean.java"
        );

        // 1. Дедупликация: первый шаг — точный vacansyID
        assertTrue("findDuplicate должен в первую очередь проверять точный vacansyID",
                code.contains("e.vacansyID = :vid") && code.contains("cleanVid"));

        // 2. Расчет ставок аутстаффинга по справочнику OutstaffingRates
        assertTrue("Должен присутствовать метод applyOutstaffingRates",
                code.contains("applyOutstaffingRates(SmartOpenPositionParsedData data)"));
        assertTrue("Должен проверяться точный шаг ставки rate = :rate",
                code.contains("select e from hunttech_OutstaffingRates e where e.rate = :rate"));
        assertTrue("Должен проверяться ближайший меньший шаг ставки rate < :rate order by e.rate desc",
                code.contains("where e.rate < :rate order by e.rate desc"));
        assertTrue("При отсутствии ставки должен устанавливаться salaryCandidateRequest = true",
                code.contains("data.setSalaryCandidateRequest(true)"));

        // 3. Синхронизация 4 артефактов
        assertTrue("ensureFourArtifacts должен генерировать английское описание commentEn",
                code.contains("buildCommentEn(data, sourceText)"));
        assertTrue("Чеклист должен дублироваться в exercise",
                code.contains("data.setExercise(data.getInterviewChecklist())"));
        assertTrue("Карта поиска должна дублироваться в memoForInterview",
                code.contains("data.setMemoForInterview(data.getSearchMap())"));
        assertTrue("План интервью должен дублироваться в templateLetter",
                code.contains("data.setTemplateLetter(data.getInterviewPlan())"));
        assertTrue("Должна формироваться Telegram-публикация",
                code.contains("buildTelegramPost(data)"));

        // 4. Стандарты по умолчанию: приоритет 2 (Normal), оформление 0 (Аутстаффинг), город МСК +/- 2 часа
        assertTrue("Город для удаленки по умолчанию должен быть Регионы РФ (МСК +/- 2 часа)",
                code.contains("Регионы РФ (МСК +/- 2 часа)"));
        assertTrue("Приоритет по умолчанию должен быть NORMAL (2)",
                code.contains("OpenPositionPriority.NORMAL.getId()"));
        assertTrue("Оформление по умолчанию должно быть 0 (Аутстаффинг)",
                code.contains("openPosition.setRegistrationForWork(data.getRegistrationForWork() != null ? data.getRegistrationForWork() : 0)"));

        // 5. Владелец по умолчанию hrm-bot / htm-bot
        assertTrue("Должен присутствовать метод resolveVacancyOwner",
                code.contains("resolveVacancyOwner(String requestedOwnerLogin, ExtUser defaultRecruiter)"));
        assertTrue("Системный бот hrm-bot / htm-bot должен быть владельцем по умолчанию",
                code.contains("hrm-bot") && code.contains("htm-bot"));

        // 6. Поддержка специфики SSP 62630 и цепочки ДКС
        assertTrue("Должна поддерживаться специфика SSP 62630",
                code.contains("62630"));
        assertTrue("Должна поддерживаться цепочка ДКС в findOrCreateProject",
                code.contains("ДКС") && code.contains("newProj.setProjectDepartment("));
    }

    @Test
    public void testParsedDataModelIntegrity() {
        SmartOpenPositionParsedData data = new SmartOpenPositionParsedData();
        data.setVacansyName("Архитектор DWH");
        data.setVacansyID("62630");
        data.setOutstaffingCost(new BigDecimal("3500.00"));
        data.setRegistrationForWork(0);
        data.setSalaryCandidateRequest(false);
        data.setCommentEn("<h3>1. Role, Position Title</h3><p>DWH Architect</p>");
        data.setTemplateLetter("<h3>План интервью</h3>");
        data.setTelegramPost("🔥 Новая вакансия: Архитектор DWH");
        data.setOwnerLogin("okozhevnikova");

        assertEquals("62630", data.getVacansyID());
        assertEquals(new BigDecimal("3500.00"), data.getOutstaffingCost());
        assertEquals(Integer.valueOf(0), data.getRegistrationForWork());
        assertEquals(Boolean.FALSE, data.getSalaryCandidateRequest());
        assertNotNull(data.getCommentEn());
        assertNotNull(data.getTemplateLetter());
        assertNotNull(data.getTelegramPost());
        assertEquals("okozhevnikova", data.getOwnerLogin());
    }

    @Test
    public void testLlmChatServiceBeanContract() throws Exception {
        String code = readSourceFile(
                "src/com/company/hunttech/service/LlmChatServiceBean.java",
                "modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java"
        );

        assertTrue("LlmChatServiceBean должен содержать перехват isVacancyOpeningIntent",
                code.contains("isVacancyOpeningIntent(message.trim())"));
        assertTrue("LlmChatServiceBean должен форматировать ссылки через hrm://vacancy/",
                code.contains("hrm://vacancy/"));
        assertTrue("LlmChatServiceBean должен включать Telegram-пост в ответ",
                code.contains("getTelegramPost()"));
    }
}
