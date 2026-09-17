package com.company.hunttech.web.screens.llmchat;

import com.company.hunttech.config.HunttechHermesManagerConfig;
import com.company.hunttech.service.HermesManagerChatService;
import com.company.hunttech.service.dto.HermesChatMessage;
import com.company.hunttech.service.dto.HermesChatResponse;
import com.company.hunttech.service.dto.HermesConnectionStatus;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Контрактный тест интеграции Manager Hermes в Web-модуле:
 * 1. Проверка методов HermesManagerChatService.
 * 2. Проверка регистрации remoteProxy hunttech_HermesManagerChatService в web-spring.xml.
 * 3. Проверка интерфейса HunttechHermesManagerConfig.
 * 4. Проверка XML-дескриптора llm-chat-screen.xml: вкладка hermesManagerChatTab,
 *    hermesManagerInputArea, hermesManagerSendBtn.
 * 5. Проверка контроллера LlmChatScreen.java: инъекция HermesManagerChatService,
 *    проверка прав hunttech.ai.useLocalChat и hunttech.ai.useManagerHermesWrite,
 *    переключение скрытых вкладок, обработчики hermesManagerSendBtn и JS-мост.
 */
class HermesManagerIntegrationContractTest {

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

    @Test
    void testHermesManagerChatServiceContract() throws Exception {
        Method startMethod = HermesManagerChatService.class.getMethod("startManagerHermesConversation");
        assertNotNull(startMethod);
        assertEquals(UUID.class, startMethod.getReturnType());

        Method sendMethod = HermesManagerChatService.class.getMethod("sendManagerHermesMessage", UUID.class, String.class);
        assertNotNull(sendMethod);
        assertEquals(HermesChatResponse.class, sendMethod.getReturnType());

        Method historyMethod = HermesManagerChatService.class.getMethod("loadManagerHermesHistory", UUID.class);
        assertNotNull(historyMethod);
        assertEquals(List.class, historyMethod.getReturnType());

        Method checkMethod = HermesManagerChatService.class.getMethod("checkManagerHermesConnection");
        assertNotNull(checkMethod);
        assertEquals(HermesConnectionStatus.class, checkMethod.getReturnType());
    }

    @Test
    void testHunttechHermesManagerConfigContract() {
        assertTrue(HunttechHermesManagerConfig.class.isInterface());
        assertDoesNotThrow(() -> HunttechHermesManagerConfig.class.getMethod("getSshHost"));
        assertDoesNotThrow(() -> HunttechHermesManagerConfig.class.getMethod("getContainerName"));
        assertDoesNotThrow(() -> HunttechHermesManagerConfig.class.getMethod("getProfile"));
        assertDoesNotThrow(() -> HunttechHermesManagerConfig.class.getMethod("getSshEnabled"));
        assertDoesNotThrow(() -> HunttechHermesManagerConfig.class.getMethod("getTimeoutSeconds"));
    }

    @Test
    void testWebSpringRemoteProxyRegistered() throws Exception {
        File springFile = resolveFile("modules/web/src/com/company/hunttech/web-spring.xml");
        assertTrue(springFile.exists(), "web-spring.xml должен существовать");

        String content = new String(Files.readAllBytes(springFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("hunttech_HermesManagerChatService"),
                "web-spring.xml должен содержать регистрацию remoteProxy для hunttech_HermesManagerChatService");
        assertTrue(content.contains("com.company.hunttech.service.HermesManagerChatService"),
                "web-spring.xml должен связывать ключ hunttech_HermesManagerChatService с интерфейсом HermesManagerChatService");
    }

    @Test
    void testXmlDescriptorContainsHermesManagerComponents() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/llmchat/llm-chat-screen.xml");
        assertTrue(xmlFile.exists(), "XML-дескриптор должен существовать");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        NodeList tabs = doc.getElementsByTagName("tab");
        Element localTab = null;
        Element hermesTab = null;
        Element hermesManagerTab = null;
        for (int i = 0; i < tabs.getLength(); i++) {
            Element el = (Element) tabs.item(i);
            if ("localChatTab".equals(el.getAttribute("id"))) {
                localTab = el;
            } else if ("hermesChatTab".equals(el.getAttribute("id"))) {
                hermesTab = el;
            } else if ("hermesManagerChatTab".equals(el.getAttribute("id"))) {
                hermesManagerTab = el;
            }
        }
        assertNotNull(localTab, "Вкладка localChatTab должна присутствовать");
        assertNotNull(hermesTab, "Вкладка hermesChatTab должна присутствовать");
        assertNotNull(hermesManagerTab, "Вкладка hermesManagerChatTab должна присутствовать");

        NodeList textAreas = doc.getElementsByTagName("textArea");
        boolean hasManagerInputArea = false;
        for (int i = 0; i < textAreas.getLength(); i++) {
            Element el = (Element) textAreas.item(i);
            if ("hermesManagerInputArea".equals(el.getAttribute("id"))) {
                hasManagerInputArea = true;
            }
        }
        assertTrue(hasManagerInputArea, "Поле ввода hermesManagerInputArea должно присутствовать");

        NodeList buttons = doc.getElementsByTagName("button");
        boolean hasManagerSendBtn = false;
        for (int i = 0; i < buttons.getLength(); i++) {
            Element el = (Element) buttons.item(i);
            if ("hermesManagerSendBtn".equals(el.getAttribute("id"))) {
                hasManagerSendBtn = true;
            }
        }
        assertTrue(hasManagerSendBtn, "Кнопка отправки hermesManagerSendBtn должна присутствовать");
    }

    @Test
    void testControllerIntegrityAndTabVisibilityLogic() throws Exception {
        File controllerFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");
        assertTrue(controllerFile.exists(), "Контроллер LlmChatScreen.java должен существовать");

        String code = new String(Files.readAllBytes(controllerFile.toPath()), StandardCharsets.UTF_8);

        // Инъекция сервиса
        assertTrue(code.contains("HermesManagerChatService hermesManagerChatService"),
                "Контроллер должен инжектировать HermesManagerChatService");

        // Проверка Specific Permissions
        assertTrue(code.contains("hunttech.ai.useLocalChat"),
                "Контроллер должен проверять специфическое право hunttech.ai.useLocalChat");
        assertTrue(code.contains("hunttech.ai.useManagerHermesWrite"),
                "Контроллер должен проверять специфическое право hunttech.ai.useManagerHermesWrite");

        // Логика переключения вкладки
        assertTrue(code.contains("applyTabPermissions"),
                "Контроллер должен содержать метод applyTabPermissions()");

        // Метод отправки сообщений Manager Hermes
        assertTrue(code.contains("executeHermesManagerSend"),
                "Контроллер должен содержать метод executeHermesManagerSend");

        // JS-функция
        assertTrue(code.contains("hunttechSendHermesManagerChatMessage"),
                "Контроллер должен регистрировать JS-функцию hunttechSendHermesManagerChatMessage");

        // Подписчик на кнопку
        assertTrue(code.contains("@Subscribe(\"hermesManagerSendBtn\")"),
                "Контроллер должен содержать @Subscribe(\"hermesManagerSendBtn\")");
    }
}
