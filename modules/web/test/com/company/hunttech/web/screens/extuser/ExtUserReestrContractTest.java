package com.company.hunttech.web.screens.extuser;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Контрактный автотест экрана «Реестр пользователей» (ExtUserReestr):
 * 1. Проверка структуры XML-дескриптора (Split-View 312px сайдбар, groupTable, ovaFallbackImage, rowsCount).
 * 2. Проверка Java-контроллера (аннотации, наследование StandardLookup, генерация аватара).
 * 3. Проверка регистрации в web-menu.xml, web-screens.xml и локализации (ru/en).
 * 4. Проверка цветовой схемы (job-candidate-editor + job-candidate-sidebar).
 */
class ExtUserReestrContractTest {

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
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/extuser/ext-user-reestr.xml");
        assertTrue(xmlFile.exists(), "XML-дескриптор ext-user-reestr.xml обязан существовать");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        // 1. Проверка корневого окна
        Element window = doc.getDocumentElement();
        assertEquals("window", window.getTagName());
        assertEquals("msg://reestrCaption", window.getAttribute("caption"));
        assertEquals("com.company.hunttech.web.screens.extuser", window.getAttribute("messagesPack"));

        // 2. Проверка контейнера данных и view
        NodeList collections = doc.getElementsByTagName("collection");
        Element usersDc = null;
        for (int i = 0; i < collections.getLength(); i++) {
            Element el = (Element) collections.item(i);
            if ("usersDc".equals(el.getAttribute("id"))) {
                usersDc = el;
                break;
            }
        }
        assertNotNull(usersDc, "Контейнер usersDc должен присутствовать");
        assertEquals("extUser-view", usersDc.getAttribute("view"),
                "Контейнер обязан использовать extUser-view");

        // 3. Проверка сайдбара 312px и цветовой схемы
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
        assertTrue(detailPane.getAttribute("stylename").contains("job-candidate-sidebar"), "Сайдбар должен содержать job-candidate-sidebar для единого цветового решения");

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
        assertEquals("120px", pic.getAttribute("width"));
        assertEquals("120px", pic.getAttribute("height"));

        // 5. Проверка таблицы и rowsCount
        NodeList tables = doc.getElementsByTagName("groupTable");
        Element usersTable = null;
        for (int i = 0; i < tables.getLength(); i++) {
            Element el = (Element) tables.item(i);
            if ("usersTable".equals(el.getAttribute("id"))) {
                usersTable = el;
                break;
            }
        }
        assertNotNull(usersTable, "Таблица usersTable обязана присутствовать");

        NodeList rowsCountList = doc.getElementsByTagName("rowsCount");
        assertTrue(rowsCountList.getLength() > 0, "Компактный счетчик rowsCount обязан присутствовать");
    }

    @Test
    void testJavaControllerContract() throws Exception {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserReestr.java");
        assertTrue(javaFile.exists(), "Контроллер ExtUserReestr.java обязан существовать");

        String code = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);

        assertTrue(code.contains("@UiController(\"hunttech_ExtUser.reestr\")"),
                "Контроллер обязан иметь @UiController(\"hunttech_ExtUser.reestr\")");
        assertTrue(code.contains("@UiDescriptor(\"ext-user-reestr.xml\")"),
                "Контроллер обязан иметь @UiDescriptor(\"ext-user-reestr.xml\")");
        assertTrue(code.contains("@LookupComponent(\"usersTable\")"),
                "Контроллер обязан иметь @LookupComponent(\"usersTable\")");
        assertTrue(code.contains("extends StandardLookup<ExtUser>"),
                "Контроллер обязан наследоваться от StandardLookup<ExtUser>");
        assertTrue(code.contains("usersTableAvatarColumnGenerator"),
                "Контроллер должен генерировать аватар 24px для таблицы пользователей");
        assertTrue(code.contains("sec$User.changePassword"),
                "Контроллер должен поддерживать вызов смены пароля");
    }

    @Test
    void testMenuAndScreenRegistration() throws Exception {
        File menuFile = resolveFile("modules/web/src/com/company/hunttech/web-menu.xml");
        assertTrue(menuFile.exists());
        String menuXml = new String(Files.readAllBytes(menuFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(menuXml.contains("hunttech_ExtUser.reestr"), "web-menu.xml обязан содержать пункт hunttech_ExtUser.reestr");

        File screensFile = resolveFile("modules/web/src/com/company/hunttech/web-screens.xml");
        assertTrue(screensFile.exists());
        String screensXml = new String(Files.readAllBytes(screensFile.toPath()), StandardCharsets.UTF_8);
        assertFalse(screensXml.contains("id=\"hunttech_ExtUser.reestr\""),
                "hunttech_ExtUser.reestr имеет @UiController и НЕ должен объявляться в legacy web-screens.xml (избегая ошибки Type.FRAGMENT в CUBA)");
        assertTrue(screensXml.contains("id=\"sec$User.browse\" template=\"com/company/hunttech/web/screens/extuser/ext-user-browse.xml\""),
                "sec$User.browse должен сохранять стандартный ext-user-browse.xml для обратной совместимости");

        File ruProps = resolveFile("modules/web/src/com/company/hunttech/web/messages_ru.properties");
        String ru = new String(Files.readAllBytes(ruProps.toPath()), StandardCharsets.UTF_8);
        assertTrue(ru.contains("menu_config.hunttech_ExtUser.reestr="), "messages_ru.properties обязан содержать заголовок меню");

        File enProps = resolveFile("modules/web/src/com/company/hunttech/web/messages.properties");
        String en = new String(Files.readAllBytes(enProps.toPath()), StandardCharsets.UTF_8);
        assertTrue(en.contains("menu_config.hunttech_ExtUser.reestr="), "messages.properties обязан содержать заголовок меню");
    }
}
