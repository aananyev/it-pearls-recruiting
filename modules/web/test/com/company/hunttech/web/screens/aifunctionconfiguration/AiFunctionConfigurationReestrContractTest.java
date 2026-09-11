package com.company.hunttech.web.screens.aifunctionconfiguration;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Автотест целостности контракта экрана «Реестр функций AI»:
 * 1. Проверка структуры XML-дескриптора (Split-View 312px сайдбар, DataGrid, ovaFallbackImage, rowsCount).
 * 2. Проверка Java-контроллера (аннотации, подписки, методы быстрых действий).
 * 3. Проверка регистрации в web-menu.xml и локализации (ru/en).
 * 4. Проверка Data View Integrity (отсутствие Unfetched Attribute Access).
 */
class AiFunctionConfigurationReestrContractTest {

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        if (path.startsWith("modules/web/")) {
            File sub = new File(path.substring("modules/web/".length()));
            if (sub.exists()) {
                return sub;
            }
        }
        return file;
    }

    @Test
    void testXmlDescriptorIntegrity() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/aifunctionconfiguration/ai-function-configuration-reestr.xml");
        assertTrue(xmlFile.exists(), "XML-дескриптор ai-function-configuration-reestr.xml обязан существовать");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        // 1. Проверка корневого окна
        Element window = doc.getDocumentElement();
        assertEquals("window", window.getTagName());
        assertEquals("msg://reestrCaption", window.getAttribute("caption"));
        assertEquals("com.company.hunttech.web.screens.aifunctionconfiguration", window.getAttribute("messagesPack"));

        // 2. Проверка контейнера данных и безопасного browse view
        NodeList collections = doc.getElementsByTagName("collection");
        Element aiDc = null;
        for (int i = 0; i < collections.getLength(); i++) {
            Element el = (Element) collections.item(i);
            if ("aiFunctionsDc".equals(el.getAttribute("id"))) {
                aiDc = el;
                break;
            }
        }
        assertNotNull(aiDc, "Контейнер aiFunctionsDc должен присутствовать");
        assertEquals("ai-function-configuration-browse-view", aiDc.getAttribute("view"),
                "Контейнер обязан использовать safe browse view без prompt LOB");

        // 3. Проверка сайдбара 312px
        NodeList vboxes = doc.getElementsByTagName("vbox");
        Element detailPane = null;
        for (int i = 0; i < vboxes.getLength(); i++) {
            Element el = (Element) vboxes.item(i);
            if ("detailPane".equals(el.getAttribute("id"))) {
                detailPane = el;
                break;
            }
        }
        assertNotNull(detailPane, "Сайдбар detailPane должен присутствовать");
        assertEquals("312px", detailPane.getAttribute("width"), "Сайдбар должен иметь ширину 312px по стандарту Split-View");
        assertTrue(detailPane.getAttribute("stylename").contains("edit-sidebar"), "Сайдбар должен содержать стиль edit-sidebar");

        // 4. Проверка ovaFallbackImage
        NodeList fallbacks = doc.getElementsByTagName("ovaFallbackImage");
        Element pic = null;
        for (int i = 0; i < fallbacks.getLength(); i++) {
            Element el = (Element) fallbacks.item(i);
            if ("detailPic".equals(el.getAttribute("id"))) {
                pic = el;
                break;
            }
        }
        assertNotNull(pic, "Компонент detailPic (ovaFallbackImage) обязан присутствовать");
        assertEquals("icons/ai/ai-function-configuration.png", pic.getAttribute("fallbackThemePath"));
        assertEquals("120px", pic.getAttribute("width"));
        assertEquals("120px", pic.getAttribute("height"));

        // 5. Проверка DataGrid и rowsCount
        NodeList dataGrids = doc.getElementsByTagName("dataGrid");
        Element table = null;
        for (int i = 0; i < dataGrids.getLength(); i++) {
            Element el = (Element) dataGrids.item(i);
            if ("aiFunctionsTable".equals(el.getAttribute("id"))) {
                table = el;
                break;
            }
        }
        assertNotNull(table, "Таблица aiFunctionsTable должна присутствовать в XML");

        NodeList rowsCounts = doc.getElementsByTagName("rowsCount");
        assertTrue(rowsCounts.getLength() > 0, "Компактный rowsCount должен присутствовать для таблицы реестра");
    }

    @Test
    void testControllerIntegrity() throws Exception {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/aifunctionconfiguration/AiFunctionConfigurationReestr.java");
        assertTrue(javaFile.exists(), "Контроллер AiFunctionConfigurationReestr.java обязан существовать");

        String code = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);

        assertTrue(code.contains("@UiController(\"hunttech_AiFunctionConfiguration.reestr\")"),
                "Контроллер обязан быть зарегистрирован с ID hunttech_AiFunctionConfiguration.reestr");
        assertTrue(code.contains("@UiDescriptor(\"ai-function-configuration-reestr.xml\")"),
                "Контроллер обязан связываться с ai-function-configuration-reestr.xml");
        assertTrue(code.contains("@LookupComponent(\"aiFunctionsTable\")"),
                "LookupComponent должен указывать на aiFunctionsTable");
        assertTrue(code.contains("extends StandardLookup<AiFunctionConfiguration>"),
                "Контроллер обязан наследовать StandardLookup");
        assertTrue(code.contains("onAiFunctionsDcItemChange"),
                "Контроллер обязан подписываться на изменение выбранного элемента для обновления сайдбара");
        assertTrue(code.contains("updateSidebar"),
                "Контроллер обязан содержать метод updateSidebar");
        assertTrue(code.contains("onEditBtnClick"),
                "Контроллер обязан обрабатывать кнопку открытия карточки");
        assertTrue(code.contains("onCopyCodeBtnClick"),
                "Контроллер обязан обрабатывать кнопку копирования кода");
        assertTrue(code.contains("onCallLogsBtnClick"),
                "Контроллер обязан поддерживать переход в журнал вызовов AI");
        assertTrue(code.contains("onToggleActiveBtnClick"),
                "Контроллер обязан поддерживать быстрое переключение активности функции");
    }

    @Test
    void testMenuAndLocalizationRegistration() throws Exception {
        // Проверка меню
        File menuFile = resolveFile("modules/web/src/com/company/hunttech/web-menu.xml");
        assertTrue(menuFile.exists());
        String menuXml = new String(Files.readAllBytes(menuFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(menuXml.contains("screen=\"hunttech_AiFunctionConfiguration.reestr\""),
                "Главное меню web-menu.xml обязано содержать пункт hunttech_AiFunctionConfiguration.reestr");

        // Проверка русской локализации
        File ruFile = resolveFile("modules/web/src/com/company/hunttech/web/messages_ru.properties");
        assertTrue(ruFile.exists());
        String ruContent = new String(Files.readAllBytes(ruFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(ruContent.contains("menu_config.hunttech_AiFunctionConfiguration.reestr=Реестр функций AI"),
                "Русская локализация главного меню должна содержать название пункта реестра");

        // Проверка локализации экрана
        File screenRuFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/aifunctionconfiguration/messages_ru.properties");
        assertTrue(screenRuFile.exists());
        String screenRuContent = new String(Files.readAllBytes(screenRuFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(screenRuContent.contains("reestrCaption=Реестр функций AI"),
                "Локализация экрана messages_ru.properties должна содержать reestrCaption");
        assertTrue(screenRuContent.contains("sectionRouting=МАРШРУТИЗАЦИЯ И ИСПОЛНЕНИЕ"),
                "Локализация экрана messages_ru.properties должна содержать секцию маршрутизации");
    }

    @Test
    void testDataViewIntegrity() throws Exception {
        File viewsFile = resolveFile("modules/global/src/com/company/hunttech/ai-control-plane-views.xml");
        assertTrue(viewsFile.exists());
        String viewsXml = new String(Files.readAllBytes(viewsFile.toPath()), StandardCharsets.UTF_8);

        assertTrue(viewsXml.contains("name=\"ai-function-configuration-browse-view\""),
                "View ai-function-configuration-browse-view обязана быть задекларирована");
        assertTrue(viewsXml.contains("<property name=\"code\"/>"));
        assertTrue(viewsXml.contains("<property name=\"name\"/>"));
        assertTrue(viewsXml.contains("<property name=\"capability\"/>"));
        assertTrue(viewsXml.contains("<property name=\"executionPolicy\"/>"));
        assertTrue(viewsXml.contains("<property name=\"fallbackPolicy\"/>"));
        assertTrue(viewsXml.contains("<property name=\"adminConfiguration\" view=\"admin-ai-configuration-browse-view\"/>"));
        assertTrue(viewsXml.contains("<property name=\"adminModelName\"/>"));
        assertTrue(viewsXml.contains("<property name=\"temperature\"/>"));
        assertTrue(viewsXml.contains("<property name=\"maxTokens\"/>"));
        assertTrue(viewsXml.contains("<property name=\"defaultMonthlyTokenQuota\"/>"));
        assertTrue(viewsXml.contains("<property name=\"active\"/>"));
    }
}
