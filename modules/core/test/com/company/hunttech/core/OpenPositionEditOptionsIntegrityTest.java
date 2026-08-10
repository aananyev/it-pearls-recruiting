package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.TestEntityTracker;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.CompanyDepartament;
import com.company.hunttech.entity.Project;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защита OpenPositionEdit от ошибок типа detached object / Unfetched Attribute Access
 * в слой OPTIONS формы: optionImageProvider'ы (companyNameField, companyDepartamentField,
 * projectNameField) читают логотипы через FK-цепочки (fileCompanyLogo / projectLogo).
 *
 * Если логотип не задекларирован во view списка опций — провайдер на каждую опцию при
 * каждом Vaadin-рендере делает lazy SELECT по ID (N+1). Замерено 2026-08-10: при открытии
 * формы ~5 660 SELECT Company JOIN SYS_FILE (7-9 с) из-за cacheable-загрузчика companyNamesLc:
 * CUBA entity cache при выборке списка выполняет точечный find() по ID для каждой строки.
 *
 * Тесты фиксируют:
 * 1) view опций содержат логотипы (нет UNFETCHED/lazy в провайдерах);
 * 2) список компаний загружается одним запросом со всеми полями провайдера;
 * 3) крупные справочники-опции НЕ помечены cacheable в XML формы (регрессия find-цикла).
 */
public class OpenPositionEditOptionsIntegrityTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private DataManager dataManager;
    private TestEntityTracker tracker;

    @Before
    public void setUp() {
        dataManager = AppBeans.get(DataManager.class);
        tracker = new TestEntityTracker(dataManager);
    }

    @After
    public void tearDown() {
        tracker.cleanup();
    }

    /** companyNameFieldImageProvider читает company.getFileCompanyLogo(). */
    @Test
    public void companyPickerViewDeclaresFileCompanyLogo() {
        Company company = dataManager.load(Company.class)
                .query("select e from hunttech_Company e")
                .view("company-picker-view")
                .maxResults(1)
                .list().get(0);

        assertTrue("company-picker-view должен декларировать fileCompanyLogo — иначе "
                        + "companyNameFieldImageProvider делает lazy SELECT по ID на каждую опцию",
                PersistenceHelper.isLoaded(company, "fileCompanyLogo"));
    }

    /** companyDepartamentFieldImageProvider читает department.getCompanyName().getFileCompanyLogo(). */
    @Test
    public void companyDepartamentPickerViewDeclaresCompanyLogoChain() {
        CompanyDepartament department = loadAnyDepartmentWithPickerView();
        assertNotNull("В БД должен найтись CompanyDepartament", department);

        assertTrue("companyDepartament-picker-view должен декларировать companyName",
                PersistenceHelper.isLoaded(department, "companyName"));
        assertTrue("companyDepartament-picker-view.companyName должен использовать view с "
                        + "fileCompanyLogo — иначе провайдер департамента делает lazy SELECT",
                PersistenceHelper.isLoaded(department.getCompanyName(), "fileCompanyLogo"));
    }

    /** projectFielsImageProvider читает project.getProjectLogo(). */
    @Test
    public void projectPickerViewDeclaresProjectLogo() {
        Project project = dataManager.load(Project.class)
                .query("select e from hunttech_Project e")
                .view("project-picker-view")
                .maxResults(1)
                .list().get(0);

        // Если логотипа у проекта нет (FK null) — getter вернёт null без SQL; наличие
        // не-null логотипа обязано быть загружено view, иначе lazy SELECT по ID.
        if (project.getProjectLogo() != null) {
            assertTrue("project-picker-view (или inline-view опций проекта) должен декларировать "
                            + "projectLogo для не-null логотипа",
                    PersistenceHelper.isLoaded(project, "projectLogo"));
        }
    }

    /** Список компаний (JPQL формы) с company-picker-view: все поля провайдера загружены. */
    @Test
    public void companyNamesListLoadsFileCompanyLogoWithoutLazy() {
        List<Company> companies = dataManager.load(Company.class)
                .query("select e from hunttech_Company e")
                .view("company-picker-view")
                .list();

        assertTrue("В БД должны быть компании для опций поля «Компания»", companies.size() > 0);
        for (Company company : companies) {
            assertTrue("Компания " + company.getId() + ": fileCompanyLogo не загружен в "
                            + "company-picker-view — провайдер иконок даст lazy SELECT по ID",
                    PersistenceHelper.isLoaded(company, "fileCompanyLogo"));
        }
    }

    /**
     * РЕГРЕССИЯ find-цикла: cacheable="true" на крупных справочниках-опциях заставляет
     * CUBA entity cache при выборке списка выполнять точечный find() по ID для каждой
     * строки (~5 660 SELECT компании + ~188 позиции + ~93 города при открытии формы).
     */
    @Test
    public void editXmlDoesNotCacheLargeDictionaryLoaders() throws IOException {
        String xml = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml");

        for (String loaderId : new String[]{"companyNamesLc", "positionTypesLc", "citiesDl"}) {
            Pattern tagPattern = Pattern.compile("<loader id=\"" + loaderId + "\"[^>]*>");
            Matcher matcher = tagPattern.matcher(xml);
            assertTrue("Не найден loader " + loaderId + " в open-position-edit.xml", matcher.find());

            assertFalse("loader " + loaderId + " не должен быть cacheable=\"true\": entity cache "
                            + "при выборке списка делает find() по ID на каждую строку (N+1-цикл, "
                            + "замерено при открытии формы)",
                    matcher.group().contains("cacheable"));
        }
    }

    /** Гарантированно берём существующий департамент (создаём, если БД пуста). */
    private CompanyDepartament loadAnyDepartmentWithPickerView() {
        List<CompanyDepartament> existing = dataManager.load(CompanyDepartament.class)
                .query("select e from hunttech_CompanyDepartament e where e.companyName is not null")
                .view("companyDepartament-picker-view")
                .maxResults(1)
                .list();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        Company company = dataManager.create(Company.class);
        company.setComanyName("Test-Company-" + UUID.randomUUID());
        tracker.track(dataManager.commit(company));

        CompanyDepartament department = dataManager.create(CompanyDepartament.class);
        department.setDepartamentRuName("Test-Department-" + UUID.randomUUID());
        department.setCompanyName(company);
        tracker.track(dataManager.commit(department));

        return dataManager.load(CompanyDepartament.class)
                .id(department.getId())
                .view("companyDepartament-picker-view")
                .one();
    }

    private String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
