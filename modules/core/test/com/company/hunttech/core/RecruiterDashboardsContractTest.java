package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecruiterDashboardsContractTest {

    private static final String[] THEMES = {
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark"
    };

    @Test
    public void threeDashboardsUseJsonModelsAndSharedRoot() throws IOException {
        checkScreen("recruiter-kanban-dashboard.xml", "recruiter-kanban-dashboard.json");
        checkScreen("recruiter-funnel-dashboard.xml", "recruiter-funnel-dashboard.json");
        checkScreen("recruiter-reserve-dashboard.xml", "recruiter-reserve-dashboard.json");
    }

    @Test
    public void appletsUseDashboardWidgetAndRefreshContract() throws IOException {
        checkWidget("RecruiterCandidateKanbanWidget.java", "hunttech_RecruiterCandidateKanbanWidget");
        checkWidget("RecruiterHiringFunnelWidget.java", "hunttech_RecruiterHiringFunnelWidget");
        checkWidget("RecruiterTalentReserveWidget.java", "hunttech_RecruiterTalentReserveWidget");
    }

    @Test
    public void kanbanAndFunnelLoadAllStageAttributesInDedicatedView() throws IOException {
        String views = source("modules/global/src/com/company/hunttech/views.xml");
        int viewStart = views.indexOf("name=\"recruiter-dashboard-iteraction-list-view\"");
        assertTrue("Отсутствует view дашборда рекрутера", viewStart >= 0);
        int viewEnd = views.indexOf("</view>", viewStart);
        String dashboardView = views.substring(viewStart, viewEnd);

        for (String attribute : new String[]{
                "iterationName", "signOurInterviewAssigned", "signOurInterview",
                "signClientInterview", "signSendToClient", "signPersonalReserve",
                "signPersonalReservePut", "signStartCase", "signEndCase"}) {
            assertTrue("В dashboard-view отсутствует " + attribute,
                    dashboardView.contains("name=\"" + attribute + "\""));
        }

        assertTrue(source("modules/web/src/com/company/hunttech/web/widgets/recruiterdashboard/"
                + "RecruiterCandidateKanbanWidget.java")
                .contains(".view(\"recruiter-dashboard-iteraction-list-view\")"));
        assertTrue(source("modules/web/src/com/company/hunttech/web/widgets/recruiterdashboard/"
                + "RecruiterHiringFunnelWidget.java")
                .contains(".view(\"recruiter-dashboard-iteraction-list-view\")"));
    }

    @Test
    public void menuContainsRecruiterDashboardsGroup() throws IOException {
        String menu = source("modules/web/src/com/company/hunttech/web-menu.xml");
        String dashboards = menuSection(menu, "recruiter-dashboards");

        assertEquals(1, occurrences(menu, "id=\"recruiter-dashboards\""));
        assertTrue(dashboards.contains("caption=\"Дашборды рекрутера\""));
        assertTrue(dashboards.contains("icon=\"DASHBOARD\""));
        assertEquals(3, occurrences(dashboards, "screen=\""));

        assertDashboardItem(menu, dashboards, "hunttech_RecruiterKanbanDashboard", "Kanban", "COLUMNS");
        assertDashboardItem(menu, dashboards, "hunttech_RecruiterFunnelDashboard", "Воронка найма", "FILTER");
        assertDashboardItem(menu, dashboards, "hunttech_RecruiterReserveDashboard", "Кадровый резерв", "SHIELD");

        assertFalse("Persistent recruiting-dashboard остаётся стартовым и не добавляется в меню",
                menu.contains("screen=\"recruiting-dashboard\""));
    }

    @Test
    public void aiAdministrationKeepsConfigurationLogsAndTechnicalScreens() throws IOException {
        String menu = source("modules/web/src/com/company/hunttech/web-menu.xml");
        String aiAdministration = menuSection(menu, "aiAdministration");

        for (String screenId : new String[]{
                "hunttech_LlmChatScreen",
                "hunttech_AdminAiDashboard",
                "hunttech_UserAiDashboard",
                "hunttech_AiFunctionConfiguration.reestr",
                "hunttech_AiFunctionConfiguration.browse",
                "hunttech_AdminAiConfiguration.browse",
                "hunttech_UserAiConfiguration.browse",
                "hunttech_UserAiFunctionOverride.browse",
                "hunttech_VacancyPromptTemplate.browse",
                "hunttech_AiCallLog.browse",
                "hunttech_CandidateSkillsEnrichmentMonitoring",
                "hunttech_LlmChatQuotaReconciliation.browse"}) {
            assertEquals("В Управлении AI должен ровно один раз сохраниться экран " + screenId,
                    1, occurrences(aiAdministration, "screen=\"" + screenId + "\""));
        }

        assertTrue(aiAdministration.contains("hunttech_AdminAiDashboard"));
        assertTrue(aiAdministration.contains("hunttech_UserAiDashboard"));
    }

    @Test
    public void dashboardStylesStayIdenticalAcrossThemes() throws IOException {
        String canonical = null;
        for (String theme : THEMES) {
            String current = source("modules/web/themes/" + theme
                    + "/com.company.hunttech/recruiter-dashboard-shared-styles.scss");
            assertTrue(current.contains(".recruiter-kanban-board"));
            assertTrue(current.contains(".recruiter-funnel-bar"));
            assertTrue(current.contains(".recruiter-reserve-row"));
            assertFalse(current.contains("\n  .v-label {"));
            if (canonical == null) {
                canonical = current;
            } else {
                assertEquals(canonical, current);
            }
        }
    }

    private void checkScreen(String xmlName, String jsonName) throws IOException {
        String base = "modules/web/src/com/company/hunttech/web/screens/recruiterdashboard/";
        String xml = source(base + xmlName);
        String json = source(base + jsonName);
        assertTrue(xml.contains("recruiter-dashboard-root"));
        assertTrue(xml.contains("jsonPath="));
        assertTrue(xml.contains("timerDelay=\"60\""));
        assertTrue(json.contains("\"visualModel\""));
        assertTrue(json.contains("\"frameId\""));
    }

    private void checkWidget(String fileName, String frameId) throws IOException {
        String java = source("modules/web/src/com/company/hunttech/web/widgets/recruiterdashboard/" + fileName);
        assertTrue(java.contains("@DashboardWidget"));
        assertTrue(java.contains("implements RefreshableWidget"));
        assertTrue(java.contains("@UiController(\"" + frameId + "\")"));
    }

    private void assertDashboardItem(String menu, String dashboards, String screenId,
                                     String caption, String icon) {
        assertEquals("Dashboard screen ID должен встречаться в меню ровно один раз: " + screenId,
                1, occurrences(menu, "screen=\"" + screenId + "\""));
        int itemStart = dashboards.indexOf("<item screen=\"" + screenId + "\"");
        assertTrue("В группе Дашборды отсутствует " + screenId, itemStart >= 0);
        int itemEnd = dashboards.indexOf("/>", itemStart);
        assertTrue("Не найден конец menu item для " + screenId, itemEnd > itemStart);
        String item = dashboards.substring(itemStart, itemEnd);
        assertTrue("Не сохранён caption для " + screenId, item.contains("caption=\"" + caption + "\""));
        assertTrue("Не сохранена icon для " + screenId, item.contains("icon=\"" + icon + "\""));
        assertFalse("Не должен появиться новый openType у " + screenId, item.contains("openType="));
    }

    private String menuSection(String menu, String menuId) {
        int start = menu.indexOf("<menu id=\"" + menuId + "\"");
        assertTrue("Не найден menu node " + menuId, start >= 0);
        int end = menu.indexOf("</menu>", start);
        assertTrue("Не найден конец menu node " + menuId, end > start);
        return menu.substring(start, end);
    }

    private int occurrences(String source, String value) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(value, from)) >= 0) {
            count++;
            from += value.length();
        }
        return count;
    }

    private String source(String relativePath) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Не найден корень проекта HRM HuntTech");
    }
}
