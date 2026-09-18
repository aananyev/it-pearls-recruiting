package com.company.hunttech.web.screens.iteractionlist;

import org.junit.jupiter.api.DisplayName;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Контрактный тест проверяет адаптивную компоновку командного тулбара экрана
 * hunttech_IteractionListReestr.browse (IteractionListReestrBrowse):
 * - Преобразование tableFilterBar в cssLayout без конфликтов с Vaadin HBox/v-slot;
 * - Наличие селекторов iteraction-reestr-filter-bar, left-action-buttons, right-action-buttons;
 * - Удаление распорочного спейсера toolbarSpacer;
 * - Сохранность всех 5 кнопок и их действий;
 * - Синхронизацию SCSS-правил во всех 7 темах оформления.
 */
class IteractionListReestrLayoutContractTest {

    private static final List<String> THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

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

    private Element findElementById(Element root, String id) {
        if (id.equals(root.getAttribute("id"))) {
            return root;
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element found = findElementById((Element) children.item(i), id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    @DisplayName("Проверка XML-контракта tableFilterBar: cssLayout, классы left/right-action-buttons, отсутствие toolbarSpacer")
    void testXmlDescriptorToolbarContract() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-reestr-browse.xml");
        assertTrue(xmlFile.exists(), "XML-дескриптор iteraction-list-reestr-browse.xml должен существовать");

        Document doc = parseXml(xmlFile);
        Element root = doc.getDocumentElement();

        // 1. tableFilterBar должен быть cssLayout
        Element toolbar = findElementById(root, "tableFilterBar");
        assertNotNull(toolbar, "Контейнер tableFilterBar должен присутствовать в XML");
        assertEquals("cssLayout", toolbar.getTagName(), "tableFilterBar должен быть cssLayout, а не hbox");
        String toolbarStyle = toolbar.getAttribute("stylename");
        assertTrue(toolbarStyle.contains("candidate-filter-bar"), "tableFilterBar должен иметь класс candidate-filter-bar");
        assertTrue(toolbarStyle.contains("iteraction-reestr-filter-bar"), "tableFilterBar должен иметь класс iteraction-reestr-filter-bar");

        // 2. leftActionButtons должен быть cssLayout с классом left-action-buttons
        Element leftButtons = findElementById(root, "leftActionButtons");
        assertNotNull(leftButtons, "Контейнер leftActionButtons должен присутствовать");
        assertEquals("cssLayout", leftButtons.getTagName(), "leftActionButtons должен быть cssLayout");
        String leftStyle = leftButtons.getAttribute("stylename");
        assertTrue(leftStyle.contains("left-action-buttons"), "leftActionButtons должен содержать класс left-action-buttons");

        // 3. rightActionButtons должен быть cssLayout с классом right-action-buttons
        Element rightButtons = findElementById(root, "rightActionButtons");
        assertNotNull(rightButtons, "Контейнер rightActionButtons должен присутствовать");
        assertEquals("cssLayout", rightButtons.getTagName(), "rightActionButtons должен быть cssLayout");
        String rightStyle = rightButtons.getAttribute("stylename");
        assertTrue(rightStyle.contains("right-action-buttons"), "rightActionButtons должен содержать класс right-action-buttons");

        // 4. toolbarSpacer должен отсутствовать (распорка во flexbox недопустима)
        Element spacer = findElementById(root, "toolbarSpacer");
        assertNull(spacer, "Элемент toolbarSpacer должен отсутствовать в cssLayout тулбаре");

        // 5. Все кнопки и действия сохранены
        Element createBtn = findElementById(root, "createBtn");
        assertNotNull(createBtn, "Кнопка createBtn должна присутствовать");
        assertEquals("iteractionListsTable.create", createBtn.getAttribute("action"));

        Element editBtn = findElementById(root, "editBtn");
        assertNotNull(editBtn, "Кнопка editBtn должна присутствовать");
        assertEquals("iteractionListsTable.edit", editBtn.getAttribute("action"));

        Element removeBtn = findElementById(root, "removeBtn");
        assertNotNull(removeBtn, "Кнопка removeBtn должна присутствовать");
        assertEquals("iteractionListsTable.remove", removeBtn.getAttribute("action"));

        Element filterPopup = findElementById(root, "filterPopupButton");
        assertNotNull(filterPopup, "Кнопка filterPopupButton должна присутствовать");

        Element actionsPopup = findElementById(root, "actionsPopupButton");
        assertNotNull(actionsPopup, "Кнопка actionsPopupButton должна присутствовать");

        // Действия filterPopupButton
        assertNotNull(findElementById(root, "filterAll"), "Действие filterAll должно присутствовать");
        assertNotNull(findElementById(root, "filterMyOnly"), "Действие filterMyOnly должно присутствовать");
        assertNotNull(findElementById(root, "filterOutstaffingOnly"), "Действие filterOutstaffingOnly должно присутствовать");
        assertNotNull(findElementById(root, "filterLast30Days"), "Действие filterLast30Days должно присутствовать");
        assertNotNull(findElementById(root, "filterLast90Days"), "Действие filterLast90Days должно присутствовать");

        // Действия actionsPopupButton
        assertNotNull(findElementById(root, "refreshAction"), "Действие refreshAction должно присутствовать");
        assertNotNull(findElementById(root, "excelExportAction"), "Действие excelExportAction должно присутствовать");
    }

    @Test
    @DisplayName("Проверка SCSS-контракта во всех 7 темах: однострочное размещение и масштабирование iteraction-reestr-filter-bar")
    void testScssContractAcrossAllThemes() throws IOException {
        for (String theme : THEMES) {
            File scssFile = resolveFile(String.format(
                    "modules/web/themes/%s/com.company.hunttech/job-candidate-editor.scss", theme));
            assertTrue(scssFile.exists(), "SCSS-файл для темы " + theme + " должен существовать: " + scssFile.getPath());

            String scss = new String(Files.readAllBytes(scssFile.toPath()), StandardCharsets.UTF_8);

            assertTrue(scss.contains(".candidate-filter-bar.iteraction-reestr-filter-bar"),
                    "Тема " + theme + " должна содержать селектор .candidate-filter-bar.iteraction-reestr-filter-bar");
            assertTrue(scss.contains("flex-wrap: nowrap !important"),
                    "Тема " + theme + " должна удерживать тулбар строго в одну строку (flex-wrap: nowrap)");
            assertTrue(scss.contains("justify-content: space-between !important"),
                    "Тема " + theme + " должна выравнивать группы кнопок тулбара через space-between");
            assertTrue(scss.contains("margin-left: auto !important"),
                    "Тема " + theme + " должна прижимать правую группу кнопок через margin-left: auto");
            assertTrue(scss.contains("text-overflow: ellipsis !important"),
                    "Тема " + theme + " должна поддерживать эллипсис подписей кнопок при сжатии");
            assertTrue(scss.contains("overflow-x: auto !important"),
                    "Тема " + theme + " должна обеспечивать горизонтальную прокрутку при критическом сжатии");
            assertTrue(scss.contains("scrollbar-width: none !important"),
                    "Тема " + theme + " должна скрывать видимый скроллбар во избежание обрезки кнопок");
            assertTrue(scss.contains(".candidate-filter-bar:not(.iteraction-reestr-filter-bar)"),
                    "Тема " + theme + " должна исключать iteraction-reestr-filter-bar из медиа-запроса 1240px");
        }
    }
}
