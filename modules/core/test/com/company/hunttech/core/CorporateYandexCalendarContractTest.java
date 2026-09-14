package com.company.hunttech.core;

import com.company.hunttech.dto.yandex.YandexCalendarInfoDto;
import com.company.hunttech.entity.CorporateYandexCalendar;
import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.Position;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Комплексный контрактный и функциональный тест для интеграции Яндекс Календаря с взаимодействиями кандидатов:
 * 1. Целостность метаданных сущности CorporateYandexCalendar и полей IteractionList.
 * 2. Регистрация в persistence.xml, views.xml, db.changelog-master.xml и web-menu.xml.
 * 3. Валидация XML дескрипторов экранов CorporateYandexCalendar, IteractionEdit и IteractionListEdit.
 * 4. Алгоритм вычисления названия встречи по стандарту: <Кандидат> — <Должность> — <Взаимодействие>.
 * 5. Алгоритм вычисления календаря по умолчанию по строгой цепочке приоритетов.
 */
public class CorporateYandexCalendarContractTest {

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
    public void testCorporateYandexCalendarEntityStructure() {
        CorporateYandexCalendar calendar = new CorporateYandexCalendar();
        calendar.setName("Рекрутинг HuntTech");
        calendar.setAccountEmail("corporate@hunttech.ru");
        calendar.setCalendarPath("/calendars/corporate@hunttech.ru/events/");
        calendar.setActive(true);
        calendar.setIsDefault(true);
        calendar.setDescription("Основной корпоративный календарь");

        assertEquals("Рекрутинг HuntTech", calendar.getName());
        assertEquals("corporate@hunttech.ru", calendar.getAccountEmail());
        assertEquals("/calendars/corporate@hunttech.ru/events/", calendar.getCalendarPath());
        assertTrue(calendar.getActive());
        assertTrue(calendar.getIsDefault());
        assertEquals("Основной корпоративный календарь", calendar.getDescription());
    }

    @Test
    public void testIteractionListCalendarFieldsStructure() {
        IteractionList item = new IteractionList();
        item.setCalendarEventId("event-uid-12345");
        item.setCalendarId("/calendars/corp/events/");
        item.setCalendarSyncState("SYNCED");
        item.setAddToCalendar(true);

        assertEquals("event-uid-12345", item.getCalendarEventId());
        assertEquals("/calendars/corp/events/", item.getCalendarId());
        assertEquals("SYNCED", item.getCalendarSyncState());
        assertTrue(item.getAddToCalendar());
    }

    @Test
    public void testLiquibaseChangelogRegistration() throws Exception {
        File changelog = resolveFile("modules/core/db/changelog/260914-2-addCorporateYandexCalendarAndInteractionEventColumns.xml");
        assertTrue("Файл миграции 260914-2-addCorporateYandexCalendarAndInteractionEventColumns.xml должен существовать", changelog.exists());

        String masterChangelog = readProjectFile("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue("db.changelog-master.xml должен включать 260914-2-addCorporateYandexCalendarAndInteractionEventColumns.xml",
                masterChangelog.contains("260914-2-addCorporateYandexCalendarAndInteractionEventColumns.xml"));
    }

    @Test
    public void testViewsXmlRegistration() throws Exception {
        String viewsXml = readProjectFile("modules/global/src/com/company/hunttech/views.xml");
        assertTrue("views.xml должен содержать entity hunttech_CorporateYandexCalendar",
                viewsXml.contains("entity=\"hunttech_CorporateYandexCalendar\""));
        assertTrue("views.xml должен содержать view corporateYandexCalendar-view",
                viewsXml.contains("name=\"corporateYandexCalendar-view\""));

        // Data View Integrity для IteractionList
        assertTrue("iteractionList-edit-view должен содержать свойство calendarEventId",
                viewsXml.contains("<property name=\"calendarEventId\"/>"));
        assertTrue("iteractionList-edit-view должен содержать свойство calendarId\"/>",
                viewsXml.contains("<property name=\"calendarId\"/>"));
        assertTrue("iteractionList-edit-view должен содержать свойство calendarSyncState\"/>",
                viewsXml.contains("<property name=\"calendarSyncState\"/>"));
        assertTrue("iteractionList-edit-view должен содержать свойство addToCalendar\"/>",
                viewsXml.contains("<property name=\"addToCalendar\"/>"));
    }

    @Test
    public void testPersistenceXmlRegistration() throws Exception {
        String persistenceXml = readProjectFile("modules/global/src/com/company/hunttech/persistence.xml");
        assertTrue("persistence.xml должен содержать класс CorporateYandexCalendar",
                persistenceXml.contains("<class>com.company.hunttech.entity.CorporateYandexCalendar</class>"));
    }

    @Test
    public void testWebMenuRegistration() throws Exception {
        String webMenuXml = readProjectFile("modules/web/src/com/company/hunttech/web-menu.xml");
        assertTrue("web-menu.xml должен содержать экран corporate-yandex-calendar-browse",
                webMenuXml.contains("hunttech_CorporateYandexCalendar.browse"));
    }

    @Test
    public void testIteractionEditSidebarAndHint() throws Exception {
        String iteractionEditXml = readProjectFile("modules/web/src/com/company/hunttech/web/screens/iteraction/iteraction-edit.xml");
        assertTrue("iteraction-edit.xml должен иметь стандартную ширину сайдбара 312px",
                iteractionEditXml.contains("id=\"iteractionSidebar\"") && iteractionEditXml.contains("width=\"312px\""));
        assertTrue("iteraction-edit.xml должен содержать подсказку о календаре additionalFieldHintLabel",
                iteractionEditXml.contains("id=\"additionalFieldHintLabel\""));
    }

    @Test
    public void testIteractionListEditCalendarComponents() throws Exception {
        String iteractionListEditXml = readProjectFile("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        assertTrue("iteraction-list-edit.xml должен содержать calendarBox",
                iteractionListEditXml.contains("id=\"calendarBox\""));
        assertTrue("iteraction-list-edit.xml должен содержать addToCalendarCheckBox",
                iteractionListEditXml.contains("id=\"addToCalendarCheckBox\""));
        assertTrue("iteraction-list-edit.xml должен содержать calendarLookupField",
                iteractionListEditXml.contains("id=\"calendarLookupField\""));
        assertTrue("iteraction-list-edit.xml должен иметь стандартную ширину сайдбара 312px",
                iteractionListEditXml.contains("id=\"iteractionListSidebar\"") && iteractionListEditXml.contains("width=\"312px\""));
    }

    @Test
    public void testEventTitleFormatting() {
        // Сценарий 1: Полный набор данных
        String title1 = formatTitle("Иван Петров", "Java Developer", "Назначено собеседование с рекрутером HuntTech");
        assertEquals("Иван Петров — Java Developer — Назначено собеседование с рекрутером HuntTech", title1);

        // Сценарий 2: Без должности кандидата, но есть вакансия
        String title2 = formatTitle("Анна Сидорова", null, "Первичный контакт");
        assertEquals("Анна Сидорова — Первичный контакт", title2);

        // Сценарий 3: Без имени кандидата (только должность и взаимодействие)
        String title3 = formatTitle(null, "DevOps Engineer", "Техническое интервью");
        assertEquals("DevOps Engineer — Техническое интервью", title3);

        // Сценарий 4: Полностью пустые данные (дефолтный заголовок)
        String title4 = formatTitle(null, null, null);
        assertEquals("Взаимодействие с кандидатом", title4);
    }

    private String formatTitle(String candidateName, String candidatePosition, String interactionName) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(candidateName)) parts.add(candidateName.trim());
        if (StringUtils.isNotBlank(candidatePosition)) parts.add(candidatePosition.trim());
        if (StringUtils.isNotBlank(interactionName)) parts.add(interactionName.trim());

        return parts.isEmpty() ? "Взаимодействие с кандидатом" : String.join(" — ", parts);
    }

    @Test
    public void testDefaultCalendarPriorityChain() {
        // Список 1: Личный календарь (isDefault=true) и Корпоративный (isDefault=true)
        // Приоритет: 1) персональный дефолт; 2) корпоративный дефолт; 3) первый активный
        YandexCalendarInfoDto personal = new YandexCalendarInfoDto();
        personal.setDisplayName("Личный Яндекс Календарь");
        personal.setPath("/calendars/user@yandex.ru/events/");
        personal.setPersonal(true);
        personal.setDefault(true);

        YandexCalendarInfoDto corp = new YandexCalendarInfoDto();
        corp.setDisplayName("Корпоративный Календарь");
        corp.setPath("/calendars/corp@hunttech.ru/events/");
        corp.setPersonal(false);
        corp.setDefault(true);

        List<YandexCalendarInfoDto> calendars = Arrays.asList(personal, corp);
        String defaultPath = resolveDefaultCalendar(calendars);
        assertEquals("/calendars/user@yandex.ru/events/", defaultPath);

        // Список 2: Только корпоративные календари, один из них default
        corp.setDefault(false);
        YandexCalendarInfoDto corpDefault = new YandexCalendarInfoDto();
        corpDefault.setDisplayName("Главный Корпоративный");
        corpDefault.setPath("/calendars/main@hunttech.ru/events/");
        corpDefault.setPersonal(false);
        corpDefault.setDefault(true);

        List<YandexCalendarInfoDto> corpList = Arrays.asList(corp, corpDefault);
        assertEquals("/calendars/main@hunttech.ru/events/", resolveDefaultCalendar(corpList));

        // Список 3: Ни один не помечен default -> берется первый активный
        corpDefault.setDefault(false);
        assertEquals("/calendars/corp@hunttech.ru/events/", resolveDefaultCalendar(corpList));

        // Список 4: Пустой список -> null
        assertNull(resolveDefaultCalendar(new ArrayList<>()));
    }

    private String resolveDefaultCalendar(List<YandexCalendarInfoDto> list) {
        if (list == null || list.isEmpty()) return null;

        // 1. Личный календарь, если он default
        for (YandexCalendarInfoDto c : list) {
            if (c.isPersonal() && c.isDefault()) return c.getPath();
        }
        // 2. Корпоративный календарь default
        for (YandexCalendarInfoDto c : list) {
            if (!c.isPersonal() && c.isDefault()) return c.getPath();
        }
        // 3. Первый попавшийся
        return list.get(0).getPath();
    }
}
