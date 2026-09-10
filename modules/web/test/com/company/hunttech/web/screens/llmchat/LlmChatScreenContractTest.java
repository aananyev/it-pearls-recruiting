package com.company.hunttech.web.screens.llmchat;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Автотест контракта экрана LLM-chat:
 * 1. Проверка структуры XML-дескриптора (inputArea, sendBtn, фокус).
 * 2. Проверка исходного кода контроллера (отключение trimming, регистрация моста hunttechSendChatMessage).
 * 3. Проверка стилей всех 7 тем (масштабирование SVG пиктограммы 30px, стили input-area).
 */
class LlmChatScreenContractTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-dark", "hunttech-modern-light"
    );

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
    void testXmlDescriptorContract() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/llmchat/llm-chat-screen.xml");
        assertTrue(xmlFile.exists(), "XML-дескриптор экрана должен существовать: " + xmlFile.getPath());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        NodeList textAreas = doc.getElementsByTagName("textArea");
        Element inputArea = null;
        for (int i = 0; i < textAreas.getLength(); i++) {
            Element el = (Element) textAreas.item(i);
            if ("inputArea".equals(el.getAttribute("id"))) {
                inputArea = el;
                break;
            }
        }
        assertNotNull(inputArea, "Поле inputArea должно присутствовать в разметке");

        NodeList buttons = doc.getElementsByTagName("button");
        Element sendBtn = null;
        for (int i = 0; i < buttons.getLength(); i++) {
            Element el = (Element) buttons.item(i);
            if ("sendBtn".equals(el.getAttribute("id"))) {
                sendBtn = el;
                break;
            }
        }
        assertNotNull(sendBtn, "Кнопка sendBtn должна присутствовать в разметке");
        assertTrue("true".equalsIgnoreCase(sendBtn.getAttribute("captionAsHtml")),
                "Кнопка отправки должна поддерживать HTML caption для отображения SVG");
    }

    @Test
    void testControllerContractTrimmingAndBridge() throws IOException {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");
        assertTrue(javaFile.exists(), "Контроллер LlmChatScreen.java должен существовать: " + javaFile.getPath());

        String content = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);

        // 1. Проверка отключения обрезки пробелов
        assertTrue(content.contains("inputArea.setTrimming(false)"),
                "Контроллер обязан отключать trimming для inputArea во избежание съедания пробелов при вводе");

        // 2. Проверка регистрации JS-моста для надежной отправки сообщений
        assertTrue(content.contains("hunttechSendChatMessage"),
                "Контроллер обязан регистрировать RPC-функцию hunttechSendChatMessage");

        // 3. Проверка наличия перехвата Enter без модификаторов в JS
        assertTrue(content.contains("e.key === 'Enter' && !e.shiftKey"),
                "JS-скрипт обязан перехватывать нажатие Enter без модификаторов");

        // 4. Проверка масштабирования пиктограммы до 30px
        assertTrue(content.contains("width=\\\"30\\\"") && content.contains("height=\\\"30\\\""),
                "Пиктограмма на кнопке отправки в контроллере должна иметь размер 30x30");
    }

    @Test
    void testAllThemesHaveSynchronizedStyles() throws IOException {
        for (String theme : THEMES) {
            File scssFile = resolveFile("modules/web/themes/" + theme + "/com.company.hunttech/chat-style.scss");
            assertTrue(scssFile.exists(), "Файл chat-style.scss должен существовать для темы " + theme);
            String scss = new String(Files.readAllBytes(scssFile.toPath()), StandardCharsets.UTF_8);

            assertTrue(scss.contains("width: 30px !important"),
                    "В SCSS темы " + theme + " пиктограмма .llm-chat-send-svg должна быть 30px в ширину");
            assertTrue(scss.contains("height: 30px !important"),
                    "В SCSS темы " + theme + " пиктограмма .llm-chat-send-svg должна быть 30px в высоту");
            assertTrue(scss.contains("&.v-textarea") || scss.contains("textarea"),
                    "В SCSS темы " + theme + " селектор .llm-chat-input-area должен корректно таргетировать сам textarea");

            File cssFile = resolveFile("modules/web/themes/" + theme + "/com.company.hunttech/chat-style.css");
            assertTrue(cssFile.exists(), "Файл chat-style.css должен существовать для темы " + theme);
            String css = new String(Files.readAllBytes(cssFile.toPath()), StandardCharsets.UTF_8);

            assertTrue(css.contains("width: 30px !important"),
                    "В CSS темы " + theme + " пиктограмма .llm-chat-send-svg должна быть 30px в ширину");
            assertTrue(css.contains("height: 30px !important"),
                    "В CSS темы " + theme + " пиктограмма .llm-chat-send-svg должна быть 30px в высоту");
            assertTrue(css.contains(".llm-chat-input-area.v-textarea") || css.contains("textarea.llm-chat-input-area"),
                    "В CSS темы " + theme + " селектор .llm-chat-input-area должен таргетировать сам textarea");
        }
    }
}
