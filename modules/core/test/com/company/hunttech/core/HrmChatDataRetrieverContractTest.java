package com.company.hunttech.core;

import com.company.hunttech.dto.HrmDataContextSnapshot;
import com.company.hunttech.service.HrmChatDataRetrieverService;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контрактные тесты для интеграции LLM-чата со срезом данных HRM HuntTech:
 * 1. DTO среза данных (HrmDataContextSnapshot)
 * 2. Data View Integrity в views.xml (openPosition-llm-view, jobCandidate-llm-view, etc.)
 * 3. Read-Only архитектура сервиса извлечения данных (HrmChatDataRetrieverService)
 * 4. Изоляция истории сообщений чата от технического контекста
 * 5. Миграции системного промпта и changelog-master
 * 6. Поддержка ссылок hrm:// в MarkdownRenderer (candidate, vacancy, interaction)
 * 7. Этап 2: Поиск по навыкам, агрегация воронки по вакансиям и история взаимодействий
 */
public class HrmChatDataRetrieverContractTest {

    @Test
    public void snapshotDtoIntegrityAndEmptiness() {
        HrmDataContextSnapshot emptySnapshot = HrmDataContextSnapshot.empty();
        assertTrue(emptySnapshot.isEmpty());
        assertEquals("", emptySnapshot.getFormattedContext());
        assertEquals(0, emptySnapshot.getTotalEntitiesFound());
        assertFalse(emptySnapshot.isHasVacancies());
        assertFalse(emptySnapshot.isHasCandidates());
        assertFalse(emptySnapshot.isHasInteractions());
        assertFalse(emptySnapshot.isHasResumes());

        UUID candId = UUID.randomUUID();
        UUID vacId = UUID.randomUUID();

        String contextText = "=== Срез данных HRM HuntTech (Режим чтения) ===\n\n"
                + "### Вакансии компании:\n"
                + "1. [Senior Java Engineer](hrm://vacancy/" + vacId + ") (Статус: OPEN, Заказчик: TechCorp)\n\n"
                + "### Кандидаты в базе:\n"
                + "1. [Алексей Смирнов](hrm://candidate/" + candId + ") | Позиция: Senior Java Developer\n\n"
                + "=== ВАЖНО ДЛЯ АССИСТЕНТА ===\n";

        HrmDataContextSnapshot snapshot = new HrmDataContextSnapshot(contextText, 2, true, true, false, false);

        assertFalse(snapshot.isEmpty());
        assertEquals(2, snapshot.getTotalEntitiesFound());
        assertTrue(snapshot.isHasVacancies());
        assertTrue(snapshot.isHasCandidates());
        assertFalse(snapshot.isHasInteractions());
        assertFalse(snapshot.isHasResumes());

        String formatted = snapshot.getFormattedContext();
        assertNotNull(formatted);
        assertTrue(formatted.contains("=== Срез данных HRM HuntTech (Режим чтения) ==="));
        assertTrue(formatted.contains("[Алексей Смирнов](hrm://candidate/" + candId + ")"));
        assertTrue(formatted.contains("[Senior Java Engineer](hrm://vacancy/" + vacId + ")"));
        assertTrue(formatted.contains("TechCorp"));
        assertTrue(formatted.contains("ВАЖНО ДЛЯ АССИСТЕНТА"));
    }

    @Test
    public void dataViewIntegrityForLlmExtractionViews() throws IOException {
        String viewsXml = source("modules/global/src/com/company/hunttech/views.xml");

        // openPosition-llm-view
        assertTrue("views.xml must declare openPosition-llm-view", viewsXml.contains("name=\"openPosition-llm-view\""));
        assertTrue(viewsXml.contains("<property name=\"positionRuName\"/>"));
        assertTrue(viewsXml.contains("<property name=\"salaryMin\"/>"));
        assertTrue(viewsXml.contains("<property name=\"salaryMax\"/>"));
        assertTrue(viewsXml.contains("<property name=\"shortDescription\"/>"));
        assertTrue("openPosition-llm-view must declare skillsList", viewsXml.contains("<property name=\"skillsList\" view=\"_minimal\"/>"));

        // jobCandidate-llm-view
        assertTrue("views.xml must declare jobCandidate-llm-view", viewsXml.contains("name=\"jobCandidate-llm-view\""));
        assertTrue(viewsXml.contains("<property name=\"fullName\"/>"));
        assertTrue(viewsXml.contains("<property name=\"telegramName\"/>"));
        assertTrue(viewsXml.contains("<property name=\"cityOfResidence\" view=\"_minimal\">"));
        assertTrue(viewsXml.contains("<property name=\"personPosition\" view=\"_minimal\">"));
        assertTrue(viewsXml.contains("<property name=\"candidateCv\" view=\"_minimal\">"));
        assertTrue("jobCandidate-llm-view must declare candidateSkills", viewsXml.contains("<property name=\"candidateSkills\" view=\"candidateSkill-view\"/>"));
        assertTrue("jobCandidate-llm-view must declare skillTree", viewsXml.contains("<property name=\"skillTree\" view=\"_minimal\"/>"));

        // candidateCV-llm-view
        assertTrue("views.xml must declare candidateCV-llm-view", viewsXml.contains("name=\"candidateCV-llm-view\""));
        assertTrue(viewsXml.contains("<property name=\"textCV\"/>"));

        // iteractionList-llm-view
        assertTrue("views.xml must declare iteractionList-llm-view", viewsXml.contains("name=\"iteractionList-llm-view\""));
        assertTrue(viewsXml.contains("<property name=\"dateIteraction\"/>"));
        assertTrue(viewsXml.contains("<property name=\"iteractionType\" view=\"_minimal\">"));
    }

    @Test
    public void serviceContractAndReadOnlyDesign() throws IOException {
        String serviceInterface = source("modules/global/src/com/company/hunttech/service/HrmChatDataRetrieverService.java");
        String serviceBean = source("modules/core/src/com/company/hunttech/service/HrmChatDataRetrieverServiceBean.java");

        assertEquals("hunttech_HrmChatDataRetrieverService", HrmChatDataRetrieverService.NAME);
        assertTrue(serviceInterface.contains("retrieveContextForMessage(String userMessage)"));

        assertTrue(serviceBean.contains("implements HrmChatDataRetrieverService"));
        assertTrue(serviceBean.contains("@Service(HrmChatDataRetrieverService.NAME)"));
        assertTrue(serviceBean.contains("dataManager.load(OpenPosition.class)"));
        assertTrue(serviceBean.contains("dataManager.load(JobCandidate.class)"));
        assertTrue(serviceBean.contains("dataManager.load(IteractionList.class)"));
        assertTrue(serviceBean.contains("dataManager.load(CandidateCV.class)"));

        // Read-only safety: сервис не должен изменять данные сущностей
        assertFalse(serviceBean.contains("dataManager.commit("));
        assertFalse(serviceBean.contains("em.persist("));
        assertFalse(serviceBean.contains("em.merge("));
        assertFalse(serviceBean.contains("em.remove("));
    }

    @Test
    public void stage2SkillsAndFunnelCapabilities() throws IOException {
        String serviceBean = source("modules/core/src/com/company/hunttech/service/HrmChatDataRetrieverServiceBean.java");

        // Поиск по навыкам в JPQL
        assertTrue("Must search vacancies by required skills", serviceBean.contains("exists (select s from e.skillsList s"));
        assertTrue("Must search candidates by candidateSkills", serviceBean.contains("exists (select cs from hunttech_CandidateSkill cs"));
        assertTrue("Must search candidates by primary skillTree", serviceBean.contains("lower(e.skillTree.skillName) like :"));

        // Расчет агрегатов воронки
        assertTrue("Must include batch funnel aggregator", serviceBean.contains("loadFunnelsForVacancies"));
        assertTrue("Must group interactions by stage", serviceBean.contains("group by e.vacancy.id, e.iteractionType.iterationName"));

        // Вывод ссылок на взаимодействия
        assertTrue("Must include interaction entity link", serviceBean.contains("hrm://interaction/"));
    }

    @Test
    public void chatServiceIsolatesHistoryFromTechnicalContext() throws IOException {
        String chatService = source("modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java");

        // Пользовательское сообщение сохраняется в БД без добавления среза
        assertTrue(chatService.contains("userMessage.setContent(message.trim());"));
        // Контекст для AI обогащается через HrmChatDataRetrieverService
        assertTrue(chatService.contains("hrmChatDataRetrieverService.retrieveContextForMessage"));
        assertTrue(chatService.contains("context.put(\"hrmContext\", snapshot.getFormattedContext());"));
    }

    @Test
    public void migrationsAndSystemPromptAreConfigured() throws IOException {
        String changelogMaster = source("modules/core/db/changelog/db.changelog-master.xml");
        String liquibase = source("modules/core/db/changelog/260910-1-enableLlmChatHrmDataGrounding.xml");
        String sql = source("modules/core/db/update/postgres/26/260910-1-enableLlmChatHrmDataGrounding.sql");

        assertTrue(changelogMaster.contains("260910-1-enableLlmChatHrmDataGrounding.xml"));
        assertTrue(liquibase.contains("260910-1-enableLlmChatHrmDataGrounding.sql"));
        assertTrue(sql.contains("UPDATE hunttech_ai_function_configuration"));
        assertTrue(sql.contains("hrm://candidate/"));
        assertTrue(sql.contains("hrm://vacancy/"));
        assertTrue(sql.contains("LLM_CHAT"));
    }

    @Test
    public void markdownRendererSupportsHrmEntityLinks() throws IOException {
        String rendererSource = source("modules/web/src/com/company/hunttech/web/screens/llmchat/MarkdownRenderer.java");

        assertTrue(rendererSource.contains("hrm://candidate/"));
        assertTrue(rendererSource.contains("hrm://vacancy/"));
        assertTrue(rendererSource.contains("hrm://interaction/"));
        assertTrue(rendererSource.contains("llm-hrm-entity-link"));
        assertTrue(rendererSource.contains("hunttech_JobCandidate.edit?id="));
        assertTrue(rendererSource.contains("hunttech_OpenPosition.edit?id="));
        assertTrue(rendererSource.contains("hunttech_IteractionList.edit?id="));
        assertTrue(rendererSource.contains("👤"));
        assertTrue(rendererSource.contains("💼"));
        assertTrue(rendererSource.contains("📋"));
    }

    @Test
    public void screenBuildersNavigationAndThemeIntegrity() throws IOException {
        String screenSource = source("modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java");

        // Проверка навигации через ScreenBuilders
        assertTrue(screenSource.contains("screenBuilders.editor(JobCandidate.class, this)"));
        assertTrue(screenSource.contains("screenBuilders.editor(OpenPosition.class, this)"));
        assertTrue(screenSource.contains("screenBuilders.editor(IteractionList.class, this)"));
        assertTrue(screenSource.contains("withOpenMode(OpenMode.NEW_TAB)"));

        // Data View Integrity при открытии редакторов
        assertTrue(screenSource.contains(".view(\"jobCandidate-view\")"));
        assertTrue(screenSource.contains(".view(\"openPosition-view\")"));
        assertTrue(screenSource.contains(".view(\"iteractionList-edit-view\")"));

        // Безопасность
        assertTrue(screenSource.contains("security.isEntityOpPermitted(JobCandidate.class, EntityOp.READ)"));
        assertTrue(screenSource.contains("security.isEntityOpPermitted(OpenPosition.class, EntityOp.READ)"));
        assertTrue(screenSource.contains("security.isEntityOpPermitted(IteractionList.class, EntityOp.READ)"));

        // JavaScript мост
        assertTrue(screenSource.contains("hunttechOpenHrmEntity"));
        assertTrue(screenSource.contains("hrmEntityBridgeRegistered"));

        // Синхронизация стилей по всем 7 темам
        String[] themes = {"hunttech-modern-light", "hunttech-modern-dark", "hunttech-modern", "helium", "halo", "havana", "hover"};
        for (String theme : themes) {
            String scss = source("modules/web/themes/" + theme + "/com.company.hunttech/chat-style.scss");
            String css = source("modules/web/themes/" + theme + "/com.company.hunttech/chat-style.css");

            assertTrue("SCSS for " + theme + " must include .llm-hrm-entity-link", scss.contains(".llm-hrm-entity-link"));
            assertTrue("CSS for " + theme + " must include .llm-hrm-entity-link", css.contains(".llm-hrm-entity-link"));
            assertTrue("SCSS for " + theme + " must include candidate entity styling", scss.contains("[data-entity=\"candidate\"]"));
            assertTrue("CSS for " + theme + " must include candidate entity styling", css.contains("[data-entity=\"candidate\"]"));
            assertTrue("SCSS for " + theme + " must include focus-visible", scss.contains(":focus-visible"));
            assertTrue("CSS for " + theme + " must include focus-visible", css.contains(":focus-visible"));
        }
    }

    private String source(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) root = root.getParent();
        assertNotNull("Не найден корень проекта", root);
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
