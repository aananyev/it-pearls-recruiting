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
    public void menuContainsRecruiterDashboardGroup() throws IOException {
        String menu = source("modules/web/src/com/company/hunttech/web-menu.xml");
        assertTrue(menu.contains("id=\"recruiter-dashboards\""));
        assertTrue(menu.contains("hunttech_RecruiterKanbanDashboard"));
        assertTrue(menu.contains("hunttech_RecruiterFunnelDashboard"));
        assertTrue(menu.contains("hunttech_RecruiterReserveDashboard"));
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
