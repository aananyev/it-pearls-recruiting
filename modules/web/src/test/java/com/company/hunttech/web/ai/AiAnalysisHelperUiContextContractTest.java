package com.company.hunttech.web.ai;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Регрессионный контракт UI-контекста общего helper-а AI-анализа.
 *
 * Notifications и Dialogs не являются Spring beans CUBA и должны получаться
 * из ScreenContext текущего экрана. Тест защищает реальный сценарий нажатия
 * AI-кнопки от NoSuchBeanDefinitionException.
 */
public class AiAnalysisHelperUiContextContractTest {

    private final String source;

    public AiAnalysisHelperUiContextContractTest() throws Exception {
        source = readSource("modules/web/src/com/company/hunttech/web/ai/AiAnalysisHelper.java");
    }

    @Test
    public void notificationsAreLoadedFromScreenContext() {
        assertTrue(source.contains("UiControllerUtils.getScreenContext(screen).getNotifications()"));
    }

    @Test
    public void dialogsAreLoadedFromScreenContext() {
        assertTrue(source.contains("UiControllerUtils.getScreenContext(screen).getDialogs()"));
    }

    @Test
    public void uiFacadesAreNotRequestedFromSpring() {
        assertFalse(source.contains("AppBeans.get(Notifications.class)"));
        assertFalse(source.contains("AppBeans.get(Dialogs.class)"));
    }

    @Test
    public void helperDoesNotReloadEntityWithLocalView() {
        assertFalse(source.contains("View.LOCAL"));
        assertFalse(source.contains("LoadContext.create"));
    }

    @Test
    public void helperKeepsThreeArgumentScreenContract() {
        assertTrue(source.contains(
                "public static void analyze(Screen screen, Entity entity, String promptCode)"));
        assertTrue(source.contains("service.analyze(entity, promptCode)"));
    }

    private static String readSource(String relativePath) throws Exception {
        String base = System.getProperty("user.dir");
        if (!new File(base, relativePath).exists()) {
            base = new File(base).getParent();
        }
        File file = new File(base, relativePath);
        if (!file.exists()) {
            file = new File("../../" + relativePath);
        }
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
