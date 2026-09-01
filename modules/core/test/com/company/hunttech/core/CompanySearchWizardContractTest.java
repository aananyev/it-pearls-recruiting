package com.company.hunttech.core;

import com.company.hunttech.service.CompanyRequisitesParsedData;
import com.company.hunttech.service.CompanySearchAiService;
import com.company.hunttech.service.CompanySearchAiServiceBean;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Контрактный тест мастера умного поиска и заполнения реквизитов компании:
 * проверяет наличие AI-функции, сервиса поиска, компонентов экрана мастера
 * и кнопок вызова мастера в форме CompanyEdit.
 */
public class CompanySearchWizardContractTest {

    private static final String COMPANY_EDIT_XML =
            "modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml";
    private static final String COMPANY_EDIT_JAVA =
            "modules/web/src/com/company/hunttech/web/screens/company/CompanyEdit.java";
    private static final String WIZARD_XML =
            "modules/web/src/com/company/hunttech/web/screens/company/smart-company-requisites-upload-screen.xml";
    private static final String WIZARD_JAVA =
            "modules/web/src/com/company/hunttech/web/screens/company/SmartCompanyRequisitesUploadScreen.java";
    private static final String SERVICE_JAVA =
            "modules/core/src/com/company/hunttech/service/CompanySearchAiServiceBean.java";

    @Test
    public void testCompanySearchAiServiceConstants() {
        assertEquals("hunttech_CompanySearchAiService", CompanySearchAiService.NAME);
        assertEquals("COMPANY_WEB_SEARCH_PARSE_JSON", CompanySearchAiService.FUNCTION_COMPANY_WEB_SEARCH_PARSE_JSON);
    }

    @Test
    public void testParsedDataModelHasCompanyDescription() {
        CompanyRequisitesParsedData data = new CompanyRequisitesParsedData();
        data.setCompanyDescription("Ведущая IT-компания");
        data.setWorkingConditions("Гибридный график, ДМС");
        data.setRawFoundSnippet("Краткая выжимка");

        assertEquals("Ведущая IT-компания", data.getCompanyDescription());
        assertEquals("Гибридный график, ДМС", data.getWorkingConditions());
        assertEquals("Краткая выжимка", data.getRawFoundSnippet());
    }

    @Test
    public void testWizardXmlHasWebSearchAndCandidates() throws IOException {
        String xml = readProjectFile(WIZARD_XML);

        assertTrue("XML мастера должен содержать вкладку поиска в интернете",
                xml.contains("id=\"searchTab\""));
        assertTrue("XML мастера должен содержать поле наименования компании",
                xml.contains("id=\"searchCompanyNameField\""));
        assertTrue("XML мастера должен содержать поле ИНН компании",
                xml.contains("id=\"searchInnField\""));
        assertTrue("XML мастера должен содержать кнопку поиска в интернете",
                xml.contains("id=\"searchWebBtn\""));
        assertTrue("XML мастера должен содержать секцию вариантов организаций",
                xml.contains("id=\"candidatesCard\""));
        assertTrue("XML мастера должен содержать контейнер списка кандидатов",
                xml.contains("id=\"candidatesListBox\""));
        assertTrue("XML мастера должен содержать предпросмотр описания компании",
                xml.contains("id=\"previewDescription\""));
        assertTrue("XML мастера должен содержать предпросмотр условий работы",
                xml.contains("id=\"previewWorkingConditions\""));
    }

    @Test
    public void testCompanyEditHasSmartFillButtons() throws IOException {
        String xml = readProjectFile(COMPANY_EDIT_XML);

        assertTrue("В тулбаре CompanyEdit должна быть кнопка smartFillCompanyBtn над вкладками",
                xml.contains("id=\"smartFillCompanyBtn\""));
    }

    @Test
    public void testCompanyEditJavaHandlesSmartFill() throws IOException {
        String java = readProjectFile(COMPANY_EDIT_JAVA);

        assertTrue("CompanyEdit должен содержать обработчик smartFillCompanyBtn",
                java.contains("@Subscribe(\"smartFillCompanyBtn\")"));
        assertTrue("CompanyEdit должен передавать параметры поиска в мастер",
                java.contains("setInitialSearchParams"));
        assertTrue("CompanyEdit должен применять описание компании",
                java.contains("setCompanyDescription"));
        assertTrue("CompanyEdit должен содержать метод автоматической загрузки логотипа",
                java.contains("downloadAndApplyLogo"));
    }

    @Test
    public void testServiceBeanIsRegistered() throws IOException {
        String java = readProjectFile(SERVICE_JAVA);

        assertTrue("CompanySearchAiServiceBean должен быть Spring сервисом",
                java.contains("@Service(CompanySearchAiService.NAME)"));
        assertTrue("CompanySearchAiServiceBean должен реализовывать searchCompanyInWeb",
                java.contains("searchCompanyInWeb"));
        assertTrue("CompanySearchAiServiceBean должен обрабатывать fallback",
                java.contains("isValidCandidate"));

        String webSpring = readProjectFile("modules/web/src/com/company/hunttech/web-spring.xml");
        assertTrue("Сервис должен быть зарегистрирован в web-spring.xml как remoteProxy",
                webSpring.contains("hunttech_CompanySearchAiService"));

        String sqlMigration;
        Path changelogXml = projectRoot().resolve("modules/core/db/changelog/260822-2-addCompanyWebSearchAiFunction.xml");
        if (Files.exists(changelogXml)) {
            sqlMigration = new String(Files.readAllBytes(changelogXml), StandardCharsets.UTF_8);
        } else {
            sqlMigration = readProjectFile("modules/core/db/update/postgres/26/260822-2-addCompanyWebSearchAiFunction.sql");
        }
        assertTrue("Миграция должна сидировать AI-функцию COMPANY_WEB_SEARCH_PARSE_JSON",
                sqlMigration.contains("COMPANY_WEB_SEARCH_PARSE_JSON"));
    }

    @Test
    public void testEnrichedSearchOfflineGeneration() {
        CompanySearchAiServiceBean serviceBean = new CompanySearchAiServiceBean();
        List<CompanyRequisitesParsedData> results = serviceBean.searchCompanyInWeb("Яндекс", "7736207543");
        assertNotNull("Результаты поиска не должны быть null", results);
        assertFalse("Для запроса 'Яндекс' должен быть сформирован кандидат", results.isEmpty());

        CompanyRequisitesParsedData candidate = results.get(0);
        assertEquals("Яндекс", candidate.getCompanyName());
        assertEquals("7736207543", candidate.getInn());
        assertNotNull("Логотип должен быть сформирован для кандидата", candidate.getLogoUrl());
        assertNotNull("Официальный сайт должен быть сформирован для кандидата", candidate.getWebsite());
        assertNotNull("Сниппет о статусе данных должен присутствовать", candidate.getRawFoundSnippet());
    }

    @Test
    public void testWebsiteLogoBranchDiscovery() {
        CompanySearchAiServiceBean serviceBean = new CompanySearchAiServiceBean();
        // Проверка защиты от private host
        String privateResult = serviceBean.extractLogoFromWebsite("http://127.0.0.1:8080");
        assertNull("Запрос к приватному хосту должен блокироваться", privateResult);

        String invalidResult = serviceBean.extractLogoFromWebsite(null);
        assertNull("Null URL должен безопасно возвращать null", invalidResult);
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
