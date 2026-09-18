package com.company.hunttech.web.screens.openposition;

import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Контрактный тест регистрации и дескриптора экрана OpenPositionRequirementExplanationDialog:
 * 1. Экран должен быть зарегистрирован через аннотации @UiController и @UiDescriptor.
 * 2. Экран НЕ должен быть объявлен в legacy web-screens.xml (во избежание ошибки CUBA DevelopmentException: Unable to create screen with type FRAGMENT).
 * 3. XML-дескриптор обязан иметь корневой элемент <window>.
 */
public class OpenPositionRequirementExplanationDialogContractTest {

    private File resolveFile(String relativePath) {
        File f = new File(relativePath);
        if (f.exists()) return f;
        f = new File("../" + relativePath);
        if (f.exists()) return f;
        f = new File("../../" + relativePath);
        if (f.exists()) return f;
        return new File(relativePath);
    }

    @Test
    @DisplayName("Проверка исключения из legacy web-screens.xml")
    void testNotPresentInLegacyWebScreensXml() throws Exception {
        File screensFile = resolveFile("modules/web/src/com/company/hunttech/web-screens.xml");
        assertTrue(screensFile.exists(), "web-screens.xml обязан существовать");
        String screensXml = new String(Files.readAllBytes(screensFile.toPath()), StandardCharsets.UTF_8);
        assertFalse(screensXml.contains("hunttech_OpenPositionRequirementExplanationDialog"),
                "hunttech_OpenPositionRequirementExplanationDialog имеет @UiController и НЕ должен объявляться в legacy web-screens.xml (избегая ошибки Type.FRAGMENT в CUBA)");
    }

    @Test
    @DisplayName("Проверка аннотаций класса контроллера")
    void testControllerAnnotations() {
        Class<?> clazz = OpenPositionRequirementExplanationDialog.class;
        assertTrue(Screen.class.isAssignableFrom(clazz), "Класс должен наследоваться от com.haulmont.cuba.gui.screen.Screen");

        UiController uiController = clazz.getAnnotation(UiController.class);
        assertNotNull(uiController, "Класс должен иметь аннотацию @UiController");
        assertEquals("hunttech_OpenPositionRequirementExplanationDialog", uiController.value(), "ID контроллера должен совпадать");

        UiDescriptor uiDescriptor = clazz.getAnnotation(UiDescriptor.class);
        assertNotNull(uiDescriptor, "Класс должен иметь аннотацию @UiDescriptor");
        assertEquals("open-position-requirement-explanation-dialog.xml", uiDescriptor.value(), "Дескриптор должен указывать на open-position-requirement-explanation-dialog.xml");
    }

    @Test
    @DisplayName("Проверка дескриптора open-position-requirement-explanation-dialog.xml")
    void testDescriptorXmlRootTag() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/openposition/open-position-requirement-explanation-dialog.xml");
        assertTrue(xmlFile.exists(), "Дескриптор open-position-requirement-explanation-dialog.xml обязан существовать");
        String content = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("<window"), "Корневой элемент дескриптора должен быть <window>");
        assertFalse(content.contains("<fragment"), "Дескриптор не должен быть фрагментом");
    }
}
