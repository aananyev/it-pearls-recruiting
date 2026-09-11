package com.company.hunttech.web.screens.llmchat;

import com.company.hunttech.service.HermesChatService;
import com.company.hunttech.service.dto.HermesChatMessage;
import com.company.hunttech.service.dto.HermesChatResponse;
import com.company.hunttech.service.dto.HermesConnectionStatus;
import com.company.hunttech.config.HunttechHermesConfig;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Автотест контракта интеграции Hermes Agent (профиль hrm-viewer):
 * 1. Проверка сигнатур HermesChatService, HermesChatMessage, HermesChatResponse, HermesConnectionStatus.
 * 2. Проверка регистрации remoteProxy в web-spring.xml.
 * 3. Проверка интерфейса конфигурации HunttechHermesConfig.
 * 4. Проверка целостности XML-дескриптора llm-chat-screen.xml (наличие вкладки hermesChatTab и ее компонентов).
 * 5. Проверка контроллера LlmChatScreen.java (инъекция HermesChatService, обработчики hermesSendBtn, JS-мост).
 * 6. Проверка рендеринга истории диалога через MarkdownRenderer.renderHermesChatHistory.
 */
class HermesChatIntegrationContractTest {

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
    void testHermesChatServiceContract() throws Exception {
        Method startMethod = HermesChatService.class.getMethod("startHermesConversation");
        assertNotNull(startMethod);
        assertEquals(UUID.class, startMethod.getReturnType());

        Method sendMethod = HermesChatService.class.getMethod("sendHermesMessage", UUID.class, String.class);
        assertNotNull(sendMethod);
        assertEquals(HermesChatResponse.class, sendMethod.getReturnType());

        Method historyMethod = HermesChatService.class.getMethod("loadHermesHistory", UUID.class);
        assertNotNull(historyMethod);
        assertEquals(List.class, historyMethod.getReturnType());

        Method checkMethod = HermesChatService.class.getMethod("checkHermesConnection");
        assertNotNull(checkMethod);
        assertEquals(HermesConnectionStatus.class, checkMethod.getReturnType());
    }

    @Test
    void testHunttechHermesConfigContract() {
        assertTrue(HunttechHermesConfig.class.isInterface());
        assertDoesNotThrow(() -> HunttechHermesConfig.class.getMethod("getSshHost"));
        assertDoesNotThrow(() -> HunttechHermesConfig.class.getMethod("getContainerName"));
        assertDoesNotThrow(() -> HunttechHermesConfig.class.getMethod("getProfile"));
        assertDoesNotThrow(() -> HunttechHermesConfig.class.getMethod("getSshEnabled"));
    }

    @Test
    void testWebSpringRemoteProxyRegistered() throws Exception {
        File springFile = resolveFile("modules/web/src/com/company/hunttech/web-spring.xml");
        assertTrue(springFile.exists(), "web-spring.xml должен существовать");

        String content = new String(Files.readAllBytes(springFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("hunttech_HermesChatService"),
                "web-spring.xml должен содержать регистрацию remoteProxy для hunttech_HermesChatService");
        assertTrue(content.contains("com.company.hunttech.service.HermesChatService"),
                "web-spring.xml должен связывать ключ hunttech_HermesChatService с интерфейсом HermesChatService");
    }

    @Test
    void testXmlDescriptorContainsHermesComponents() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/llmchat/llm-chat-screen.xml");
        assertTrue(xmlFile.exists(), "XML-дескриптор должен существовать");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        NodeList tabs = doc.getElementsByTagName("tab");
        Element hermesTab = null;
        Element localTab = null;
        for (int i = 0; i < tabs.getLength(); i++) {
            Element el = (Element) tabs.item(i);
            if ("hermesChatTab".equals(el.getAttribute("id"))) {
                hermesTab = el;
            } else if ("localChatTab".equals(el.getAttribute("id"))) {
                localTab = el;
            }
        }
        assertNotNull(localTab, "Вкладка localChatTab должна быть сохранена");
        assertNotNull(hermesTab, "Вкладка hermesChatTab должна присутствовать в дескрипторе");

        NodeList textAreas = doc.getElementsByTagName("textArea");
        boolean hasHermesInputArea = false;
        for (int i = 0; i < textAreas.getLength(); i++) {
            Element el = (Element) textAreas.item(i);
            if ("hermesInputArea".equals(el.getAttribute("id"))) {
                hasHermesInputArea = true;
            }
        }
        assertTrue(hasHermesInputArea, "Поле ввода hermesInputArea должно присутствовать");

        NodeList buttons = doc.getElementsByTagName("button");
        boolean hasHermesSendBtn = false;
        for (int i = 0; i < buttons.getLength(); i++) {
            Element el = (Element) buttons.item(i);
            if ("hermesSendBtn".equals(el.getAttribute("id"))) {
                hasHermesSendBtn = true;
            }
        }
        assertTrue(hasHermesSendBtn, "Кнопка отправки hermesSendBtn должна присутствовать");
    }

    @Test
    void testControllerIntegrationIntegrity() throws Exception {
        File controllerFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");
        assertTrue(controllerFile.exists(), "Контроллер LlmChatScreen.java должен существовать");

        String code = new String(Files.readAllBytes(controllerFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(code.contains("HermesChatService hermesChatService"),
                "Контроллер должен инжектировать HermesChatService");
        assertTrue(code.contains("executeHermesSend"),
                "Контроллер должен содержать метод executeHermesSend");
        assertTrue(code.contains("hunttechSendHermesChatMessage"),
                "Контроллер должен регистрировать JS-функцию hunttechSendHermesChatMessage");
        assertTrue(code.contains("@Subscribe(\"hermesSendBtn\")"),
                "Контроллер должен содержать подписчик на нажатие кнопки hermesSendBtn");
    }

    @Test
    void testMarkdownRendererHermesHistory() {
        List<HermesChatMessage> messages = new ArrayList<>();
        messages.add(new HermesChatMessage("user", "Привет, Hermes!"));
        messages.add(new HermesChatMessage("assistant", "Привет! Я **Hermes Agent** (`hrm-viewer`). Чем помочь?"));

        String html = MarkdownRenderer.renderHermesChatHistory(messages, null, null);
        assertNotNull(html);
        assertTrue(html.contains("llm-chat-msg-user"), "Должен содержать блок пользователя");
        assertTrue(html.contains("llm-chat-msg-ai"), "Должен содержать блок ассистента");
        assertTrue(html.contains("Hermes Agent (hrm-viewer)"), "Должен содержать имя Hermes Agent (hrm-viewer)");
        assertTrue(html.contains("class=\"llm-md-strong\">Hermes Agent</strong>"), "Markdown жирный текст должен рендериться как strong");
        assertTrue(html.contains("class=\"llm-md-code-inline\">hrm-viewer</code>"), "Инлайн-код должен рендериться как code");
    }
}
