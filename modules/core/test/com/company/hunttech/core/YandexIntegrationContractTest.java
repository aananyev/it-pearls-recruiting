package com.company.hunttech.core;

import com.company.hunttech.dto.yandex.*;
import com.company.hunttech.entity.UserYandexConfiguration;
import com.company.hunttech.service.AiYandexOrchestrationService;
import com.company.hunttech.service.AiYandexOrchestrationServiceBean;
import com.company.hunttech.service.YandexIntegrationService;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Контрактные тесты для интеграции с сервисами Yandex 360 (Календарь, Телемост, Wiki, Почта):
 * 1. Целостность сущности UserYandexConfiguration и Liquibase миграций.
 * 2. Регистрация удаленных сервисов в web-spring.xml.
 * 3. Точность распознавания намерений AI: различение личного и корпоративного календаря («Hunttech у заказчика»).
 */
public class YandexIntegrationContractTest {

    private File resolveFile(String relativePath) {
        File file = new File(relativePath);
        if (file.exists()) {
            return file;
        }
        File subFile = new File("../../" + relativePath);
        if (subFile.exists()) {
            return subFile;
        }
        File coreFile = new File("../" + relativePath);
        if (coreFile.exists()) {
            return coreFile;
        }
        return file;
    }

    private String readProjectFile(String relativePath) throws Exception {
        File file = resolveFile(relativePath);
        assertTrue("Файл должен существовать: " + relativePath, file.exists());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void testUserYandexConfigurationStructure() {
        assertEquals("https://caldav.yandex.ru", UserYandexConfiguration.DEFAULT_CALENDAR_BASE_URL);
        assertEquals("Hunttech у заказчика", UserYandexConfiguration.DEFAULT_CLIENT_CALENDAR_NAME);
        assertEquals("Europe/Saratov", UserYandexConfiguration.DEFAULT_TIME_ZONE);
        assertEquals("https://cloud-api.yandex.net/v1/telemost-api", UserYandexConfiguration.DEFAULT_TELEMOST_BASE_URL);

        UserYandexConfiguration config = new UserYandexConfiguration();
        config.setAccountEmail("alan@hunttech.ru");
        config.setCalendarConnected(true);
        config.setTelemostConnected(true);

        assertTrue(config.getCalendarConnected());
        assertTrue(config.getTelemostConnected());
        assertEquals("alan@hunttech.ru", config.getAccountEmail());
    }

    @Test
    public void testLiquibaseChangelogAndMasterRegistration() throws Exception {
        File changelog = resolveFile("modules/core/db/changelog/260913-3-add-user-yandex-configuration.xml");
        assertTrue("Файл миграции 260913-3-add-user-yandex-configuration.xml должен существовать", changelog.exists());

        String masterChangelog = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue("db.changelog-master.xml должен включать 260913-3-add-user-yandex-configuration.xml",
                masterChangelog.contains("260913-3-add-user-yandex-configuration.xml"));
    }

    @Test
    public void testViewsXmlRegistration() throws Exception {
        String viewsXml = readProjectFile("modules/global/src/com/company/hunttech/views.xml");
        assertTrue("views.xml должен содержать объявление сущности hunttech_UserYandexConfiguration",
                viewsXml.contains("entity=\"hunttech_UserYandexConfiguration\""));
        assertTrue("views.xml должен содержать view userYandexConfiguration-view",
                viewsXml.contains("name=\"userYandexConfiguration-view\""));
    }

    @Test
    public void testWebSpringServiceRegistration() throws Exception {
        String webSpring = readProjectFile("modules/web/src/com/company/hunttech/web-spring.xml");
        assertTrue("web-spring.xml должен регистрировать remoteProxy hunttech_YandexIntegrationService",
                webSpring.contains("hunttech_YandexIntegrationService"));
        assertTrue("web-spring.xml должен регистрировать remoteProxy hunttech_AiYandexOrchestrationService",
                webSpring.contains("hunttech_AiYandexOrchestrationService"));
    }

    @Test
    public void testAiYandexIntentClassification() {
        AiYandexOrchestrationServiceBean orchestrationBean = new AiYandexOrchestrationServiceBean();

        // 1. Проверка намерения "в моем календаре"
        String personalMsg = "создай в моем календаре встречу с кандидатом Ивановым завтра в 15:00";
        assertTrue(orchestrationBean.isMeetingBookingIntent(personalMsg));
        AiMeetingParseResult personalResult = orchestrationBean.parseMeetingIntent(personalMsg, null);
        assertTrue(personalResult.isIntentDetected());
        assertEquals(YandexCalendarType.PERSONAL, personalResult.getCalendarType());
        assertTrue(personalResult.isTelemostRequired());
        assertNotNull(personalResult.getStartTime());

        // 2. Проверка намерения "в календаре собеседования с заказчиком"
        String clientMsg = "создай в календаре собеседования с заказчиком звонок с Ивановым Иваном послезавтра в 16:30";
        assertTrue(orchestrationBean.isMeetingBookingIntent(clientMsg));
        AiMeetingParseResult clientResult = orchestrationBean.parseMeetingIntent(clientMsg, null);
        assertTrue(clientResult.isIntentDetected());
        assertEquals(YandexCalendarType.CLIENT_INTERVIEW, clientResult.getCalendarType());
        assertEquals("Hunttech у заказчика", clientResult.getCalendarName());
        assertTrue(clientResult.isTelemostRequired());
        assertNotNull(clientResult.getStartTime());

        // 3. Нерелевантное сообщение
        String irrelevantMsg = "покажи список открытых вакансий";
        assertFalse(orchestrationBean.isMeetingBookingIntent(irrelevantMsg));
    }
}
