package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Контрактный тест макета и вкладок CompanyReestrEdit.
 * Проверяет строгое соблюдение канонического стандарта Edit-форм HRM HuntTech:
 * 1. Неизменяемый сайдбар 270px (1-в-1 CompanyEdit: аватар 176x176, загрузка, умная обработка,
 *    идентификация, реквизиты, label-навигация «Разделы», подсказка).
 * 2. Адаптивный Workspace: edit-workspace, единый edit-toolbar, общий скроллер контента,
 *    система вкладок edit-tabs с обязательным margin="false" на всех табах,
 *    отсутствие двойного управления высотой (expand + height 100%).
 * 3. Data View Integrity: view="company-edit-view", все необходимые справочные коллекции.
 */
public class CompanyReestrEditLayoutContractTest {

    private static final String SCREEN =
            "modules/web/src/com/company/hunttech/web/screens/company/company-reestr-edit.xml";

    private static final String[] TAB_SCROLL_IDS = {
            "companyDetailsScroll", "companyRequisitesScroll",
            "companyDescriptionScroll", "companyDepartmentsScroll"
    };

    private static final String[] TAB_IDS = {
            "tabConpanyDetails", "companyRequisitesTab",
            "companyDescriptionTab", "tabCompanyDepartament"
    };

    @Test
    public void testSidebarIsStrictlyIdenticalToContract() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Сайдбар 270px
        assertTrue("отсутствует класс edit-sidebar", xml.contains("stylename=\"edit-sidebar\""));
        assertTrue("ширина сайдбара не 270px", xml.contains("width=\"270px\""));

        // Элементы визуального блока сайдбара
        assertTrue("отсутствует контейнер логотипа companyLogoPicBox", xml.contains("id=\"companyLogoPicBox\""));
        assertTrue("отсутствует круглый аватар companyLogoFileImage", xml.contains("id=\"companyLogoFileImage\""));
        assertTrue("размер аватара не 176px", xml.contains("width=\"176px\"") && xml.contains("ovalWidth=\"176px\""));
        assertTrue("отсутствует загрузчик логотипа companyLogoFileUpload", xml.contains("id=\"companyLogoFileUpload\""));
        assertTrue("отсутствует кнопка умной обработки логотипа enhanceCompanyLogoBtn", xml.contains("id=\"enhanceCompanyLogoBtn\""));

        // Идентификация и сводка реквизитов
        assertTrue("отсутствует заголовок сайдбара companySidebarTitle", xml.contains("id=\"companySidebarTitle\""));
        assertTrue("отсутствует подзаголовок компании companySidebarName", xml.contains("id=\"companySidebarName\""));
        assertTrue("отсутствует ИНН в сайдбаре companySidebarInn", xml.contains("id=\"companySidebarInn\""));
        assertTrue("отсутствует Город в сайдбаре companySidebarCity", xml.contains("id=\"companySidebarCity\""));

        // Label-навигация «Разделы»
        assertTrue("отсутствует блок навигации label-navigation", xml.contains("stylename=\"label-navigation\""));
        assertTrue("отсутствует заголовок навигации Разделы", xml.contains("company-editor-navigation-title"));
        assertTrue("отсутствует кнопка навигации Информация", xml.contains("id=\"companyEditorNavMain\""));
        assertTrue("отсутствует кнопка навигации Реквизиты", xml.contains("id=\"companyEditorNavRequisites\""));
        assertTrue("отсутствует кнопка навигации Описание", xml.contains("id=\"companyEditorNavDescription\""));
        assertTrue("отсутствует кнопка навигации Подразделения", xml.contains("id=\"companyEditorNavDepartments\""));

        // Спейсер и подсказка
        assertTrue("отсутствует спейсер companySidebarSpacer", xml.contains("id=\"companySidebarSpacer\""));
        assertTrue("отсутствует подсказка companySidebarHint", xml.contains("id=\"companySidebarHint\""));
    }

    @Test
    public void testWorkspaceStructureAndTabMarginFalse() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Тулбар и скроллер контента
        assertTrue("отсутствует companyEditorToolbar", xml.contains("id=\"companyEditorToolbar\""));
        assertTrue("отсутствует общий scrollBox companyEditorContentScrollBox",
                xml.contains("id=\"companyEditorContentScrollBox\""));
        assertTrue("общий scrollBox без stylename edit-workspace-scroll",
                xml.contains("stylename=\"edit-workspace edit-workspace-scroll company-editor-content-scroll\""));

        // TabSheet
        assertTrue("mainTab без stylename edit-tabs", xml.contains("<tabSheet id=\"mainTab\""));
        assertTrue("mainTab должен иметь height 100%", xml.contains("height=\"100%\""));

        // Инвариант margin="false" и отсутствие expand на всех вкладках
        for (String tabId : TAB_IDS) {
            int tabIdx = xml.indexOf("<tab id=\"" + tabId + "\"");
            assertTrue("вкладка " + tabId + " не найдена", tabIdx >= 0);
            int declEnd = xml.indexOf('>', tabIdx);
            String tabDecl = xml.substring(tabIdx, declEnd);

            assertTrue("вкладка " + tabId + " обязана иметь margin=\"false\"",
                    tabDecl.contains("margin=\"false\""));
            assertFalse("вкладка " + tabId + " не должна иметь expand (конфликт двойной высоты)",
                    tabDecl.contains("expand="));
        }

        // Внутренние скроллеры вкладок: ТОЛЬКО company-tab-scroll, без дублирования edit-workspace
        for (String scrollId : TAB_SCROLL_IDS) {
            int idx = xml.indexOf("id=\"" + scrollId + "\"");
            assertTrue("scrollBox " + scrollId + " не найден", idx >= 0);
            int declEnd = xml.indexOf('>', idx);
            String decl = xml.substring(idx, declEnd);

            assertTrue(scrollId + " обязан иметь stylename company-tab-scroll",
                    decl.contains("company-tab-scroll"));
            assertFalse(scrollId + " не должен нести класс edit-workspace",
                    decl.contains("edit-workspace"));
        }

        // Подвал действий (footer)
        assertTrue("отсутствует подвал действий editActions", xml.contains("id=\"editActions\""));
        assertTrue("подвал действий без stylename edit-footer-actions",
                xml.contains("stylename=\"edit-footer-actions\""));
        assertTrue("кнопка сохранения без стиля company-editor-primary-action",
                xml.contains("stylename=\"company-editor-primary-action\""));
        assertTrue("кнопка отмены без стиля company-editor-secondary-action",
                xml.contains("stylename=\"company-editor-secondary-action\""));
    }

    @Test
    public void testDataViewIntegrity() throws IOException {
        String xml = readProjectFile(SCREEN);

        assertTrue("companyDc обязан использовать company-edit-view",
                xml.contains("view=\"company-edit-view\""));
        assertTrue("отсутствует вложенная коллекция departmentOfCompanyDc",
                xml.contains("id=\"departmentOfCompanyDc\""));
        assertTrue("отсутствует коллекция companyOwnershipsDc", xml.contains("id=\"companyOwnershipsDc\""));
        assertTrue("отсутствует коллекция companyDirectorsDc", xml.contains("id=\"companyDirectorsDc\""));
        assertTrue("отсутствует коллекция companyGroupDc", xml.contains("id=\"companyGroupDc\""));
        assertTrue("отсутствует коллекция cityOfCompaniesDc", xml.contains("id=\"cityOfCompaniesDc\""));
        assertTrue("отсутствует коллекция regionOfCompaniesDc", xml.contains("id=\"regionOfCompaniesDc\""));
        assertTrue("отсутствует коллекция countryOfCompaniesDc", xml.contains("id=\"countryOfCompaniesDc\""));
    }

    @Test
    public void testSmartUploadButtonsAndBusinessLogicWiring() throws IOException {
        String xml = readProjectFile(SCREEN);

        // Кнопка быстрого умного заполнения в тулбаре
        assertTrue("отсутствует кнопка smartFillCompanyBtn в тулбаре", xml.contains("id=\"smartFillCompanyBtn\""));
        // Кнопка умной загрузки реквизитов во вкладке реквизитов
        assertTrue("отсутствует кнопка smartUploadRequisitesBtn во вкладке реквизитов",
                xml.contains("id=\"smartUploadRequisitesBtn\""));

        // Проверка наличия бизнес-логики в Java-контроллере
        String javaCode = readProjectFile("modules/web/src/com/company/hunttech/web/screens/company/CompanyReestrEdit.java");
        assertTrue("контроллер обязан содержать openSmartCompanyWizard",
                javaCode.contains("openSmartCompanyWizard()"));
        assertTrue("контроллер обязан содержать downloadAndApplyLogo",
                javaCode.contains("downloadAndApplyLogo("));
        assertTrue("контроллер обязан содержать обработку smartFillCompanyBtn",
                javaCode.contains("@Subscribe(\"smartFillCompanyBtn\")"));
        assertTrue("контроллер обязан содержать обработку smartUploadRequisitesBtn",
                javaCode.contains("@Subscribe(\"smartUploadRequisitesBtn\")"));
        assertTrue("контроллер обязан содержать каскадную логику handleCityChange",
                javaCode.contains("handleCityChange("));
        assertTrue("контроллер обязан содержать каскадную логику handleRegionChange",
                javaCode.contains("handleRegionChange("));
        assertTrue("контроллер обязан содержать ленивую загрузку loadAddress",
                javaCode.contains("loadAddress()"));
        assertTrue("контроллер обязан содержать ленивую загрузку loadCompanyDescriptions",
                javaCode.contains("loadCompanyDescriptions()"));
        assertTrue("контроллер обязан содержать ленивую загрузку loadDepartments",
                javaCode.contains("loadDepartments()"));
    }

    @Test
    public void testCompanyReestrBrowseWiringAndPrimaryEditorScreen() throws IOException {
        String editJavaCode = readProjectFile("modules/web/src/com/company/hunttech/web/screens/company/CompanyReestrEdit.java");
        assertTrue("CompanyReestrEdit обязан иметь аннотацию @PrimaryEditorScreen(Company.class)",
                editJavaCode.contains("@PrimaryEditorScreen(Company.class)"));

        String browseXml = readProjectFile("modules/web/src/com/company/hunttech/web/screens/company/company-reestr-browse.xml");
        assertTrue("действия create в browse XML должны содержать property screenClass",
                browseXml.contains("<property name=\"screenClass\" value=\"com.company.hunttech.web.screens.company.CompanyReestrEdit\"/>"));
        assertTrue("действия create/edit в browse XML должны содержать property screenId",
                browseXml.contains("<property name=\"screenId\" value=\"hunttech_CompanyReestr.edit\"/>"));

        String browseJavaCode = readProjectFile("modules/web/src/com/company/hunttech/web/screens/company/CompanyReestrBrowse.java");
        assertTrue("browse-контроллер обязан вызывать setupTableActions",
                browseJavaCode.contains("setupTableActions();"));
        assertTrue("browse-контроллер обязан настраивать EditAction со screenClass CompanyReestrEdit",
                browseJavaCode.contains("((EditAction<Company>) editAction).setScreenClass(CompanyReestrEdit.class);"));
        assertTrue("browse-контроллер обязан настраивать CreateAction со screenClass CompanyReestrEdit",
                browseJavaCode.contains("((CreateAction<Company>) createAction).setScreenClass(CompanyReestrEdit.class);"));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
    }

    private static Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertTrue("Не найден корень проекта HRM HuntTech", root != null);
        return root;
    }
}
