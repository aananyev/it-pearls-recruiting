package com.company.hunttech.web.screens.aidashboard;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Контрактные тесты для экранов «Дашборд аналитики AI» (AdminAiDashboard) и «Моя статистика AI» (UserAiDashboard):
 * 1. Проверка структуры XML дескрипторов (пропорции KPI карточек, таблицы, фильтры).
 * 2. Проверка поддержки провайдера Hermes в фильтрах и таблицах.
 * 3. Проверка генераторов колонок в контроллерах.
 */
class AiDashboardContractTest {

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        if (path.startsWith("modules/web/")) {
            File subFile = new File(path.substring("modules/web/".length()));
            if (subFile.exists()) {
                return subFile;
            }
        }
        return file;
    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    @Test
    void testAdminDashboardXmlContract() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/aidashboard/admin-ai-dashboard.xml");
        assertTrue(xmlFile.exists(), "admin-ai-dashboard.xml должен существовать");

        Document doc = parseXml(xmlFile);

        // Проверка kpiPanel
        NodeList hboxes = doc.getElementsByTagName("hbox");
        Element kpiPanel = null;
        for (int i = 0; i < hboxes.getLength(); i++) {
            Element el = (Element) hboxes.item(i);
            if ("kpiPanel".equals(el.getAttribute("id"))) {
                kpiPanel = el;
                break;
            }
        }
        assertNotNull(kpiPanel, "kpiPanel должен присутствовать в XML");
        assertEquals("true", kpiPanel.getAttribute("spacing"), "kpiPanel должен иметь spacing=true");
        assertTrue(kpiPanel.getAttribute("expand").contains("spendCard"), "kpiPanel должен равномерно расширять карточки");
        assertTrue(kpiPanel.getAttribute("expand").contains("activeUsersCard"), "kpiPanel должен расширять activeUsersCard");
        assertTrue(kpiPanel.getAttribute("expand").contains("totalVolumeCard"), "kpiPanel должен расширять totalVolumeCard");
        assertTrue(kpiPanel.getAttribute("expand").contains("reliabilityCard"), "kpiPanel должен расширять reliabilityCard");

        // Проверка userSummaryTable
        NodeList tables = doc.getElementsByTagName("table");
        Element userSummaryTable = null;
        for (int i = 0; i < tables.getLength(); i++) {
            Element el = (Element) tables.item(i);
            if ("userSummaryTable".equals(el.getAttribute("id"))) {
                userSummaryTable = el;
                break;
            }
        }
        assertNotNull(userSummaryTable, "userSummaryTable должен присутствовать");
    }

    @Test
    void testAdminDashboardControllerIntegrity() throws Exception {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/aidashboard/AdminAiDashboard.java");
        assertTrue(javaFile.exists(), "AdminAiDashboard.java должен существовать");

        String code = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(code.contains("@UiController(\"hunttech_AdminAiDashboard\")"), "Контроллер должен иметь ID hunttech_AdminAiDashboard");
        assertTrue(code.contains("providerOptions.put(\"Hermes\", \"hermes\")"), "Фильтр провайдеров должен содержать Hermes");
        assertTrue(code.contains("userSummaryTable.addGeneratedColumn(\"promptTokens\""), "userSummaryTable должен иметь генератор promptTokens");
        assertTrue(code.contains("userSummaryTable.addGeneratedColumn(\"completionTokens\""), "userSummaryTable должен иметь генератор completionTokens");
        assertTrue(code.contains("userSummaryTable.addGeneratedColumn(\"totalTokens\""), "userSummaryTable должен иметь генератор totalTokens");
    }

    @Test
    void testUserDashboardXmlContract() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/aidashboard/user-ai-dashboard.xml");
        assertTrue(xmlFile.exists(), "user-ai-dashboard.xml должен существовать");

        Document doc = parseXml(xmlFile);

        // Проверка kpiPanel
        NodeList hboxes = doc.getElementsByTagName("hbox");
        Element kpiPanel = null;
        for (int i = 0; i < hboxes.getLength(); i++) {
            Element el = (Element) hboxes.item(i);
            if ("kpiPanel".equals(el.getAttribute("id"))) {
                kpiPanel = el;
                break;
            }
        }
        assertNotNull(kpiPanel, "kpiPanel должен присутствовать в user-ai-dashboard.xml");
        assertEquals("true", kpiPanel.getAttribute("spacing"), "kpiPanel должен иметь spacing=true");
        assertTrue(kpiPanel.getAttribute("expand").contains("totalCallsCard"), "kpiPanel должен расширять totalCallsCard");
        assertTrue(kpiPanel.getAttribute("expand").contains("tokensCard"), "kpiPanel должен расширять tokensCard");
        assertTrue(kpiPanel.getAttribute("expand").contains("costCard"), "kpiPanel должен расширять costCard");
        assertTrue(kpiPanel.getAttribute("expand").contains("speedCard"), "kpiPanel должен расширять speedCard");

        // Проверка наличия колонки providerCode в recentCallsTable
        NodeList columns = doc.getElementsByTagName("column");
        boolean hasProviderColumn = false;
        for (int i = 0; i < columns.getLength(); i++) {
            Element col = (Element) columns.item(i);
            if ("providerCode".equals(col.getAttribute("id"))) {
                hasProviderColumn = true;
                break;
            }
        }
        assertTrue(hasProviderColumn, "recentCallsTable должна содержать колонку providerCode");
    }

    @Test
    void testUserDashboardControllerIntegrity() throws Exception {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/aidashboard/UserAiDashboard.java");
        assertTrue(javaFile.exists(), "UserAiDashboard.java должен существовать");

        String code = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(code.contains("@UiController(\"hunttech_UserAiDashboard\")"), "Контроллер должен иметь ID hunttech_UserAiDashboard");
        assertTrue(code.contains("recentCallsTable.addGeneratedColumn(\"providerCode\""), "Таблица должна иметь генератор колонки providerCode");
        assertTrue(code.contains("Hermes"), "Генератор providerCode должен поддерживать отображение бейджа Hermes");
    }
}
