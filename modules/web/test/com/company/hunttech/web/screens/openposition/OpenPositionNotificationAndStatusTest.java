package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Project;
import com.haulmont.cuba.security.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class OpenPositionNotificationAndStatusTest {

    @Test
    public void testPriorityAndClosedBadgeWhenOpen() {
        OpenPositionPriorityUiHelper.BadgeData badge =
                OpenPositionPriorityUiHelper.getPriorityAndStatusBadge(3, false, 20, null);
        assertNotNull(badge);
        assertNotNull(badge.getHtml());
        assertTrue(badge.getHtml().contains("svg"));
        assertFalse(badge.getHtml().contains("Вакансия закрыта"));
        assertTrue(badge.getDescription().contains("Высокий приоритет"));
    }

    @Test
    public void testPriorityAndClosedBadgeWhenClosed() {
        OpenPositionPriorityUiHelper.BadgeData badge =
                OpenPositionPriorityUiHelper.getPriorityAndStatusBadge(2, true, 20, null);
        assertNotNull(badge);
        assertNotNull(badge.getHtml());
        // Должен содержать признак закрытой вакансии (красный замочек) и приоритет
        assertTrue(badge.getHtml().contains("title='Вакансия закрыта'"));
        assertTrue(badge.getHtml().contains("#ef4444"));
        assertTrue(badge.getDescription().contains("Вакансия закрыта"));
        assertTrue(badge.getDescription().contains("Обычный приоритет"));
    }

    @Test
    public void testClosedBadgeStandalone() {
        OpenPositionPriorityUiHelper.BadgeData badge =
                OpenPositionPriorityUiHelper.getClosedBadge(22, null);
        assertNotNull(badge);
        assertTrue(badge.getHtml().contains("width: 22px"));
        assertTrue(badge.getDescription().contains("Вакансия закрыта"));
    }

    @Test
    public void testNotificationOpenMessage() {
        OpenPosition pos = new OpenPosition();
        pos.setVacansyName("Senior Java Developer");
        pos.setSalaryMin(new BigDecimal("200000"));
        pos.setSalaryMax(new BigDecimal("300000"));

        User user = new User();
        user.setName("Алексей Ананьев");

        String msg = OpenPositionNotificationHelper.buildOpenMessage(pos, user);
        assertNotNull(msg);
        assertTrue(msg.startsWith("Открыта вакансия: "));
        assertTrue(msg.contains("Senior Java Developer"));
        assertTrue(msg.contains("200 000 – 300 000 ₽"));
        assertTrue(msg.contains("Алексей Ананьев"));
    }

    @Test
    public void testNotificationCloseMessage() {
        OpenPosition pos = new OpenPosition();
        pos.setVacansyName("QA Lead");

        User user = new User();
        user.setName("Елена Смирнова");

        String msg = OpenPositionNotificationHelper.buildCloseMessage(pos, user);
        assertNotNull(msg);
        assertTrue(msg.startsWith("Закрыта вакансия: "));
        assertTrue(msg.contains("QA Lead"));
        assertTrue(msg.contains("Елена Смирнова"));
    }

    @Test
    public void testNotificationNewVacancyMessage() {
        OpenPosition pos = new OpenPosition();
        pos.setVacansyName("DevOps Инженер");

        User user = new User();
        user.setName("Системный администратор");

        String msg = OpenPositionNotificationHelper.buildNewVacancyMessage(pos, user);
        assertNotNull(msg);
        assertTrue(msg.startsWith("Открыта новая вакансия: "));
        assertTrue(msg.contains("DevOps Инженер"));
    }

    @Test
    public void testShortCharacteristicsWithProject() {
        OpenPosition pos = new OpenPosition();
        pos.setVacansyName("Frontend Разработчик");
        Project prj = new Project();
        prj.setProjectName("HRM HuntTech");
        pos.setProjectName(prj);
        pos.setSalaryMin(new BigDecimal("150000"));

        String chars = OpenPositionNotificationHelper.formatShortCharacteristics(pos);
        assertNotNull(chars);
        assertTrue(chars.contains("Проект: HRM HuntTech"));
        assertTrue(chars.contains("от 150 000 ₽"));
    }
}
