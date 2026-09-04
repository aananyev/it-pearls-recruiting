package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контрактный тест компоновки и архитектуры ExtSettingsWindow (Split-View, полновысотный сайдбар 270px, TabSheet в рабочей области).
 */
public class ExtSettingsWindowLayoutContractTest {

    @Test
    public void extSettingsWindowLayoutStructureContract() throws IOException {
        String descriptor = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml");

        // 1. Корень окна использует двухпанельную компоновку edit-screen-layout
        assertTrue("Корень должен содержать stylename ext-settings-window edit-screen-layout",
                descriptor.contains("stylename=\"ext-settings-window edit-screen-layout\""));
        assertTrue("Корень должен содержать контейнер settingsMainLayout",
                descriptor.contains("<hbox id=\"settingsMainLayout\""));

        // 2. Сайдбар на всю высоту родительского экрана слева (270px)
        assertTrue("Сайдбар должен быть объявлен как прямой дочерний элемент settingsMainLayout",
                descriptor.contains("<vbox id=\"userAiProfileSidebar\""));
        assertTrue("Ширина сайдбара 270px и высота 100%",
                descriptor.contains("width=\"270px\"") && descriptor.contains("height=\"100%\""));
        assertTrue("Сайдбар содержит класс edit-sidebar",
                descriptor.contains("stylename=\"user-ai-profile-sidebar edit-sidebar\""));

        // 3. Правая рабочая область содержит tabSheet
        assertTrue("Правая рабочая область должна содержать settingsWorkspaceBox",
                descriptor.contains("<vbox id=\"settingsWorkspaceBox\""));
        // 2026-09-04: height="100%" убран — высоту tabSheet отдаёт expand родителя
        // (фикс D1 "поехавшей" компоновки, отчёт UI/UX-дизайнера ExtSettingsWindow).
        assertTrue("settingsWorkspaceBox должен содержать tabSheet id=\"settingsTabSheet\"",
                descriptor.contains("<tabSheet id=\"settingsTabSheet\" width=\"100%\""));

        // 4. Все 5 вкладок присутствуют в TabSheet
        assertTrue("Вкладка Мой профиль и ИИ", descriptor.contains("<tab id=\"msgMyInfo\""));
        assertTrue("Вкладка Интерфейс", descriptor.contains("<tab id=\"msgInterface\""));
        assertTrue("Вкладка Почта", descriptor.contains("<tab id=\"mailAccessTab\""));
        assertTrue("Вкладка ИИ Настройки", descriptor.contains("<tab id=\"aiAccessTab\""));
        assertTrue("Вкладка Geo API", descriptor.contains("<tab id=\"geoApiAccessTab\""));

        // 5. Вкладки используют scrollBox для вертикальной прокрутки без наложения элементов
        assertTrue(descriptor.contains("id=\"userAiProfileContentScrollBox\""));
        assertTrue(descriptor.contains("id=\"interfaceSettingsContentScrollBox\""));
        assertTrue(descriptor.contains("id=\"emailSettingsContentScrollBox\""));
        assertTrue(descriptor.contains("id=\"aiSettingsContentScrollBox\""));
        assertTrue(descriptor.contains("id=\"geoSettingsContentScrollBox\""));

        // 6. Подвал с кнопками OK и Cancel
        assertTrue("Подвал действий должен присутствовать", descriptor.contains("<hbox id=\"buttons\""));
        assertTrue("Кнопка OK", descriptor.contains("<button id=\"okBtn\""));
        assertTrue("Кнопка Cancel", descriptor.contains("<button id=\"cancelBtn\""));
    }

    @Test
    public void extSettingsWindowControllerNavigationSyncContract() throws IOException {
        String controller = source("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowEmailNavigation.java");

        assertTrue("Контроллер должен инжектировать settingsTabSheet",
                controller.contains("private TabSheet settingsTabSheet;"));
        assertTrue("Контроллер должен инициализировать синхронизацию вкладок",
                controller.contains("initTabSheetSync()"));
        assertTrue("Переключение на вкладку msgMyInfo при навигации профиля",
                controller.contains("settingsTabSheet.setSelectedTab(\"msgMyInfo\")"));
        assertTrue("Переключение на вкладку msgInterface при навигации интерфейса",
                controller.contains("settingsTabSheet.setSelectedTab(\"msgInterface\")"));
        assertTrue("Переключение на вкладку mailAccessTab при навигации почты",
                controller.contains("settingsTabSheet.setSelectedTab(\"mailAccessTab\")"));
        assertTrue("Переключение на вкладку aiAccessTab при навигации ИИ",
                controller.contains("settingsTabSheet.setSelectedTab(\"aiAccessTab\")"));
    }

    private String source(String relativePath) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }
}
