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

    @Test
    void testPaginationAndNavigationContracts() throws IOException {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");
        assertTrue(javaFile.exists(), "Контроллер LlmChatScreen.java должен существовать");
        String content = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);

        // 1. Проверка лимита в 20 сообщений
        assertTrue(content.contains("PAGE_SIZE = 20"), "Контроллер обязан задавать PAGE_SIZE = 20 для истории");

        // 2. Проверка RPC-мостов динамической подгрузки более ранних сообщений
        assertTrue(content.contains("hunttechLoadEarlierMessages"),
                "Контроллер обязан регистрировать RPC-функцию hunttechLoadEarlierMessages");
        assertTrue(content.contains("hunttechLoadEarlierHermesMessages"),
                "Контроллер обязан регистрировать RPC-функцию hunttechLoadEarlierHermesMessages");

        // 3. Проверка RPC-моста выполнения быстрых действий
        assertTrue(content.contains("hunttechExecuteChatAction"),
                "Контроллер обязан регистрировать RPC-функцию hunttechExecuteChatAction");

        // 4. Проверка обработчиков открытия OpenPositionEdit, CandidateCVEdit, IteractionListEdit
        assertTrue(content.contains("OpenPositionEdit.class"),
                "Контроллер обязан поддерживать открытие OpenPositionEdit");
        assertTrue(content.contains("CandidateCVEdit.class"),
                "Контроллер обязан поддерживать открытие CandidateCVEdit");
        assertTrue(content.contains("IteractionListEdit.class"),
                "Контроллер обязан поддерживать создание и открытие IteractionListEdit");

        // 5. Проверка открытия реестра OpenPositionReestrBrowse с параметризованным фильтром
        assertTrue(content.contains("OpenPositionReestrBrowse.class"),
                "Контроллер обязан поддерживать открытие реестра вакансий OpenPositionReestrBrowse");
        assertTrue(content.contains("setPositionTypeFilter"),
                "Контроллер обязан передавать фильтр по должности в OpenPositionReestrBrowse");

        // 6. Проверка поддержки фильтрации в самом OpenPositionReestrBrowse
        File reestrFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionReestrBrowse.java");
        assertTrue(reestrFile.exists(), "OpenPositionReestrBrowse.java должен существовать");
        String reestrContent = new String(Files.readAllBytes(reestrFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(reestrContent.contains("initialPositionTypeFilter"),
                "OpenPositionReestrBrowse обязан содержать initialPositionTypeFilter");

        File reestrXml = resolveFile("modules/web/src/com/company/hunttech/web/screens/openposition/open-position-reestr-browse.xml");
        assertTrue(reestrXml.exists(), "open-position-reestr-browse.xml должен существовать");
        String reestrXmlContent = new String(Files.readAllBytes(reestrXml.toPath()), StandardCharsets.UTF_8);
        assertTrue(reestrXmlContent.contains("like :positionTypeName"),
                "open-position-reestr-browse.xml обязан содержать условие фильтрации по должности");
    }

    @Test
    void testStandardCubaSecurityAndPermissionContracts() throws IOException {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");
        assertTrue(javaFile.exists(), "Контроллер LlmChatScreen.java должен существовать");
        String content = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);

        // 1. Проверка стандартных средств безопасности CUBA Platform
        assertTrue(content.contains("security.isScreenPermitted"),
                "Контроллер обязан использовать стандартный метод security.isScreenPermitted для проверки прав на экраны");
        assertTrue(content.contains("security.isEntityOpPermitted"),
                "Контроллер обязан использовать стандартный метод security.isEntityOpPermitted для проверки прав на операции с сущностями");

        // 2. Проверка операций EntityOp (READ, CREATE, UPDATE)
        assertTrue(content.contains("EntityOp.READ"),
                "Контроллер обязан проверять право EntityOp.READ перед чтением данных");
        assertTrue(content.contains("EntityOp.CREATE"),
                "Контроллер обязан проверять право EntityOp.CREATE перед созданием экземпляров сущностей");
        assertTrue(content.contains("EntityOp.UPDATE"),
                "Контроллер обязан проверять право EntityOp.UPDATE для выявления режима 'только для чтения'");

        // 3. Проверка предупреждения и блокировки для пользователей с доступом 'только для чтения'
        assertTrue(content.contains("Ограничение доступа (только чтение)"),
                "Чат обязан выводить предупреждение о доступе 'только для чтения' и блокировать вызов формы/создание");

        // 4. Проверка проверки прав на экран настроек ExUserSettingEdit / settings
        assertTrue(content.contains("ExUserSettingEdit") && content.contains("\"settings\""),
                "Чат обязан поддерживать и проверять права на экран настроек ExUserSettingEdit (settings)");

        // 5. Проверка наличия обработчика открытия экранов через WindowConfig и Security
        assertTrue(content.contains("looksLikeScreenOpenRequest"),
                "Контроллер обязан распознавать запросы на открытие экранных форм");
        assertTrue(content.contains("processOpenScreenCommand"),
                "Контроллер обязан обрабатывать команды открытия экранов с проверкой прав доступа");
        assertTrue(content.contains("windowConfig.hasWindow"),
                "Контроллер обязан проверять регистрацию экранов через WindowConfig");
    }
}

