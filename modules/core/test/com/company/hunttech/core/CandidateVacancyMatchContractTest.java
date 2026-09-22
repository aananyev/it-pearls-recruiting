package com.company.hunttech.core;

import com.company.hunttech.service.CandidateVacancyMatchAiService;
import com.company.hunttech.dto.CandidateVacancyMatchProgress;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Контрактный тест AI-функции «Подобрать вакансию кандидату» (CandidateVacancyMatchAiService).
 */
public class CandidateVacancyMatchContractTest {

    private Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        org.junit.Assert.assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }

    @Test
    public void testFunctionCodeConstant() {
        assertEquals("CANDIDATE_VACANCY_MATCH_ANALYZE", CandidateVacancyMatchAiService.FUNCTION_CODE);
        assertEquals("hunttech_CandidateVacancyMatchAiService", CandidateVacancyMatchAiService.NAME);
    }

    @Test
    public void testProgressPollingApiContract() throws NoSuchMethodException {
        Method matchingMethod = CandidateVacancyMatchAiService.class.getMethod(
                "matchVacanciesForCandidate", UUID.class, UUID.class);
        assertEquals(com.company.hunttech.dto.CandidateVacancyMatchReport.class, matchingMethod.getReturnType());

        Method progressMethod = CandidateVacancyMatchAiService.class.getMethod(
                "getVacancyMatchProgress", UUID.class);
        assertEquals(CandidateVacancyMatchProgress.class, progressMethod.getReturnType());
    }

    @Test
    public void testOpenVacancyQueryContract() throws IOException {
        Path beanPath = projectRoot().resolve(
                "modules/core/src/com/company/hunttech/service/CandidateVacancyMatchAiServiceBean.java");
        String bean = new String(Files.readAllBytes(beanPath), StandardCharsets.UTF_8);
        assertTrue("JPQL должен включать false и legacy null",
                bean.contains("e.openClose = false or e.openClose is null"));
        assertTrue("После загрузки должна сохраняться защитная фильтрация openClose != true",
                bean.contains("filter(CandidateVacancyMatchAiServiceBean::isOpenVacancy)"));
    }

    @Test
    public void testAiCallsCarrySafeCorrelationContext() throws IOException {
        Path beanPath = projectRoot().resolve(
                "modules/core/src/com/company/hunttech/service/CandidateVacancyMatchAiServiceBean.java");
        String bean = new String(Files.readAllBytes(beanPath), StandardCharsets.UTF_8);

        assertTrue("Прямой подбор должен передавать callerSource",
                bean.contains("CandidateVacancyMatch:candidate-to-vacancies"));
        assertTrue("Обратный подбор должен передавать callerSource",
                bean.contains("CandidateVacancyMatch:vacancy-to-candidates"));
        assertTrue("Каждый AI-вызов должен передавать requestId",
                bean.contains("context.put(\"requestId\""));
        assertTrue("Прямой подбор должен связывать requestId с operation id",
                bean.contains("progress.getOperationId()"));
        assertTrue("Обратный подбор должен использовать JPQL alias e для текста резюме",
                bean.contains("length(trim(e.textCV)) > 0"));
    }

    @Test
    public void testSeedMigrationPostgres() throws IOException {
        Path root = projectRoot();
        Path sqlPath = root.resolve("modules/core/db/update/postgres/26/260919-1-addCandidateVacancyMatchAiFunction.sql");
        assertTrue("SQL seed миграция postgres должна существовать", Files.exists(sqlPath));

        String sql = new String(Files.readAllBytes(sqlPath), StandardCharsets.UTF_8);
        assertTrue("Миграция должна содержать function code", sql.contains("'CANDIDATE_VACANCY_MATCH_ANALYZE'"));
        assertTrue("Миграция должна объявлять capability TEXT_ANALYSIS", sql.contains("'TEXT_ANALYSIS'"));
        assertTrue("Миграция должна содержать CONFIGURATION_VERSION = 1", sql.contains("CONFIGURATION_VERSION"));
        assertTrue("Миграция должна отключать user context для объективности", sql.contains("FALSE"));
        assertTrue("Миграция должна быть идемпотентной", sql.contains("WHERE NOT EXISTS"));
    }

    @Test
    public void testSeedMigrationHsql() throws IOException {
        Path root = projectRoot();
        Path sqlPath = root.resolve("modules/core/db/update/hsql/26/260919-1-addCandidateVacancyMatchAiFunction.sql");
        assertTrue("HSQL seed миграция должна существовать", Files.exists(sqlPath));

        String sql = new String(Files.readAllBytes(sqlPath), StandardCharsets.UTF_8);
        assertTrue("HSQL миграция должна содержать function code", sql.contains("'CANDIDATE_VACANCY_MATCH_ANALYZE'"));
    }

    @Test
    public void testChangelogRegisteredInMaster() throws IOException {
        Path root = projectRoot();
        Path masterPath = root.resolve("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue("db.changelog-master.xml должен существовать", Files.exists(masterPath));

        String master = new String(Files.readAllBytes(masterPath), StandardCharsets.UTF_8);
        assertTrue("Changelog 260919-1 должен быть подключен в master",
                master.contains("260919-1-addCandidateVacancyMatchAiFunction.xml"));
    }

    @Test
    public void testRegisteredInWebSpringXml() throws IOException {
        Path root = projectRoot();
        Path webSpringPath = root.resolve("modules/web/src/com/company/hunttech/web-spring.xml");
        assertTrue("web-spring.xml должен существовать", Files.exists(webSpringPath));

        String webSpring = new String(Files.readAllBytes(webSpringPath), StandardCharsets.UTF_8);
        assertTrue("hunttech_CandidateVacancyMatchAiService должен быть зарегистрирован в WebRemoteProxyBeanCreator",
                webSpring.contains("hunttech_CandidateVacancyMatchAiService") &&
                webSpring.contains("com.company.hunttech.service.CandidateVacancyMatchAiService"));
    }

    @Test
    public void testJobCandidateReestrSidebarButton() throws IOException {
        Path root = projectRoot();
        Path xmlPath = root.resolve("modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-reestr.xml");
        assertTrue("job-candidate-reestr.xml должен существовать", Files.exists(xmlPath));

        String xml = new String(Files.readAllBytes(xmlPath), StandardCharsets.UTF_8);
        assertTrue("Сайдбар должен содержать кнопку findSuitableVacancyBtn", xml.contains("id=\"findSuitableVacancyBtn\""));
        assertTrue("Кнопка должна использовать AI-иконку font-icon:MAGIC", xml.contains("icon=\"font-icon:MAGIC\""));
    }

    @Test
    public void testJobCandidateReestrControllerHandlers() throws IOException {
        Path root = projectRoot();
        Path javaPath = root.resolve("modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateReestr.java");
        assertTrue("JobCandidateReestr.java должен существовать", Files.exists(javaPath));

        String java = new String(Files.readAllBytes(javaPath), StandardCharsets.UTF_8);
        assertTrue("Контроллер должен обрабатывать findSuitableVacancyBtn",
                java.contains("@Subscribe(\"findSuitableVacancyBtn\")"));
        assertTrue("Контроллер должен перенаправлять findSuitableAction на новый экран",
                java.contains("CandidateVacancyMatchScreen"));
    }

    @Test
    public void testRegisteredInMetadataXml() throws IOException {
        Path root = projectRoot();
        Path metadataPath = root.resolve("modules/global/src/com/company/hunttech/metadata.xml");
        assertTrue("metadata.xml должен существовать", Files.exists(metadataPath));

        String metadata = new String(Files.readAllBytes(metadataPath), StandardCharsets.UTF_8);
        assertTrue("CandidateVacancyMatchItem должен быть зарегистрирован в metadata.xml",
                metadata.contains("com.company.hunttech.entity.CandidateVacancyMatchItem"));
    }

    @Test
    public void testScreenLayoutKeepsLocalStyleRootAndStableBindings() throws IOException {
        Path xmlPath = projectRoot().resolve(
                "modules/web/src/com/company/hunttech/web/screens/jobcandidate/candidate-vacancy-match-screen.xml");
        String xml = new String(Files.readAllBytes(xmlPath), StandardCharsets.UTF_8);

        assertTrue("Диалог CandidateVacancyMatch должен быть адаптивным",
                xml.contains("<dialogMode width=\"100%\" height=\"100%\""));
        assertTrue(xml.contains("stylename=\"candidate-vacancy-match-root\""));
        assertTrue(xml.contains("dataContainer=\"matchesDc\""));
        assertTrue(xml.contains("id=\"analysisProgressTimer\""));
        assertTrue(xml.contains("id=\"mainSplit\""));
        assertTrue(xml.contains("id=\"matchesTable\""));
        assertTrue(xml.contains("id=\"refreshAnalysisBtn\""));
        assertTrue(xml.contains("id=\"closeBtn\""));
    }

    @Test
    public void testAllThemePartialsUseSameScreenScopedLayout() throws IOException {
        List<String> themes = Arrays.asList(
                "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark",
                "helium", "halo", "hover", "havana");
        String reference = null;

        for (String theme : themes) {
            Path scssPath = projectRoot().resolve("modules/web/themes/" + theme
                    + "/com.company.hunttech/candidate-vacancy-match-screen.scss");
            String scss = new String(Files.readAllBytes(scssPath), StandardCharsets.UTF_8);
            assertTrue(theme + ": отсутствует локальный корень", scss.contains(".candidate-vacancy-match-root"));
            assertFalse(theme + ": запрещено менять геометрию всех HBox slot-обёрток",
                    scss.contains(".candidate-vacancy-match-header > .v-slot"));
            assertFalse(theme + ": запрещено подменять CUBA expand-layout через общий flex",
                    scss.contains(".candidate-vacancy-match-root > .v-expand {\n    flex:"));
            assertTrue(theme + ": details ScrollBox root должен оставаться прокручиваемым",
                    scss.contains(".candidate-vacancy-match-details-scroll {\n    overflow-x: hidden !important;\n    overflow-y: auto !important;"));
            assertFalse(theme + ": вложенный ScrollBox не должен перехватывать вертикальную прокрутку",
                    scss.contains(".candidate-vacancy-match-details-scroll > .c-scrollbox-content"));
            assertTrue(theme + ": кнопки действий должны занимать слот и переносить подпись на узком экране",
                    scss.contains(".candidate-vacancy-match-action-row .v-button {\n      min-width: 0 !important;\n      width: 100% !important;")
                            && scss.contains("white-space: normal !important;"));
            if (reference == null) {
                reference = scss;
            } else {
                assertEquals("SCSS partial тем должны быть идентичны", reference, scss);
            }
        }
    }
}
