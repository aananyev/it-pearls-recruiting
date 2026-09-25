package com.company.hunttech.web.screens.iteractionlist;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Регрессионный тест для экрана hunttech_IteractionList.edit (IteractionListEdit).
 * Проверяет:
 * 1. Сохранение обратной совместимости для всех стандартных вызовов формы (без restrictedRootIteractionId).
 * 2. Корректность XML-дескриптора и параметров JPQL condition (:number и :rootTreeId).
 * 3. Наличие и поведение нового API ограничения типов взаимодействия (setRestrictedRootIteractionId).
 * 4. Неизменность контейнеров данных, лоадеров и компонентов для внешних экранов.
 */
class IteractionListEditRegressionTest {

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
        Path root = Paths.get("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        if (root != null) {
            File resolved = root.resolve(path).toFile();
            if (resolved.exists()) {
                return resolved;
            }
        }
        return file;
    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    @Test
    void testXmlDescriptorDataLoadersAndConditionsIntegrity() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        assertTrue(xmlFile.exists(), "Файл iteraction-list-edit.xml должен существовать");

        Document doc = parseXml(xmlFile);

        // 1. Проверяем наличие основного контейнера iteractionListDc
        NodeList instanceNodes = doc.getElementsByTagName("instance");
        boolean hasIteractionListDc = false;
        for (int i = 0; i < instanceNodes.getLength(); i++) {
            Element el = (Element) instanceNodes.item(i);
            if ("iteractionListDc".equals(el.getAttribute("id"))) {
                hasIteractionListDc = true;
                assertEquals("com.company.hunttech.entity.IteractionList", el.getAttribute("class"));
                assertEquals("iteractionList-edit-view", el.getAttribute("view"));
            }
        }
        assertTrue(hasIteractionListDc, "iteractionListDc instance container обязан присутствовать");

        // 2. Проверяем коллекцию iteractionTypesDc и её лоадер iteractionTypesLc
        NodeList collectionNodes = doc.getElementsByTagName("collection");
        Element typesCollection = null;
        for (int i = 0; i < collectionNodes.getLength(); i++) {
            Element el = (Element) collectionNodes.item(i);
            if ("iteractionTypesDc".equals(el.getAttribute("id"))) {
                typesCollection = el;
                break;
            }
        }
        assertNotNull(typesCollection, "iteractionTypesDc collection container обязан присутствовать");
        assertEquals("com.company.hunttech.entity.Iteraction", typesCollection.getAttribute("class"));
        assertEquals("iteraction-list-type-view", typesCollection.getAttribute("view"));

        NodeList loaders = typesCollection.getElementsByTagName("loader");
        assertTrue(loaders.getLength() > 0, "Лоадер iteractionTypesLc должен присутствовать");
        Element loader = (Element) loaders.item(0);
        assertEquals("iteractionTypesLc", loader.getAttribute("id"));

        String xmlContent = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);

        // 3. Проверяем, что исходное условие :number сохранено без изменений
        assertTrue(xmlContent.contains("e.iteractionTree.number like :number"),
                "Исходное условие 'e.iteractionTree.number like :number' должно быть сохранено для стандартных вызовов");

        // 4. Проверяем, что добавлено условие :rootTreeId внутри <and>
        assertTrue(xmlContent.contains("e.iteractionTree.id = :rootTreeId"),
                "Условие 'e.iteractionTree.id = :rootTreeId' должно присутствовать для ограничения по группе");
    }

    @Test
    void testControllerClassAndFieldsIntegrity() throws Exception {
        Class<?> clazz = IteractionListEdit.class;

        // 1. Проверяем наличие поля restrictedRootIteractionId и его setter
        Field restrictedField = clazz.getDeclaredField("restrictedRootIteractionId");
        assertNotNull(restrictedField, "Поле restrictedRootIteractionId должно присутствовать в IteractionListEdit");
        assertEquals(UUID.class, restrictedField.getType());

        Method setter = clazz.getDeclaredMethod("setRestrictedRootIteractionId", UUID.class);
        assertNotNull(setter, "Метод setRestrictedRootIteractionId(UUID) должен присутствовать в IteractionListEdit");

        // 2. Проверяем наличие injected-полей лоадера и контейнера типов
        Field lcField = clazz.getDeclaredField("iteractionTypesLc");
        assertNotNull(lcField, "Лоадер iteractionTypesLc должен быть объявлен");

        Field dcField = clazz.getDeclaredField("iteractionTypesDc");
        assertNotNull(dcField, "Контейнер iteractionTypesDc должен быть объявлен");
    }

    @Test
    void testDefaultStatePreservesStandardBehaviorForOtherCallers() throws Exception {
        IteractionListEdit editor = new IteractionListEdit();

        // 1. По умолчанию restrictedRootIteractionId равен null (для всех внешних экранов)
        Field field = IteractionListEdit.class.getDeclaredField("restrictedRootIteractionId");
        field.setAccessible(true);
        assertNull(field.get(editor), "По умолчанию restrictedRootIteractionId обязан быть null");

        // 2. Проверяем работу setter
        UUID targetRootId = UUID.randomUUID();
        editor.setRestrictedRootIteractionId(targetRootId);
        assertEquals(targetRootId, field.get(editor), "После вызова setter restrictedRootIteractionId должен быть установлен");

        // 3. Проверяем сброс обратно в null
        editor.setRestrictedRootIteractionId(null);
        assertNull(field.get(editor), "Должна быть возможность сбросить restrictedRootIteractionId обратно в null");
    }

    @Test
    void testExternalCallersRemainUntouched() throws Exception {
        // Проверяем, что другие экраны, открывающие IteractionListEdit, не были повреждены
        String[] callerPaths = {
                "modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java",
                "modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateBrowse.java",
                "modules/web/src/com/company/hunttech/web/screens/jobcandidate/CandidateVacancyMatchScreen.java",
                "modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java",
                "modules/web/src/com/company/hunttech/web/screens/personelreserve/PersonelReserveBrowse.java",
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteractionlistbrowse/IteractionListSimpleBrowse.java",
                "modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListBrowse.java"
        };

        for (String path : callerPaths) {
            File f = resolveFile(path);
            assertTrue(f.exists(), "Файл вызывающего экрана должен существовать: " + path);
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            assertTrue(content.contains("IteractionListEdit.class"),
                    "Вызывающий экран должен по-прежнему ссылаться на IteractionListEdit.class: " + path);
            assertFalse(content.contains("setRestrictedRootIteractionId"),
                    "Вызывающие экраны не должны использовать restrictedRootIteractionId (стандартное поведение): " + path);
        }
    }
}
