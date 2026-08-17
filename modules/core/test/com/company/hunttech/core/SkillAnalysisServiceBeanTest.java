package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.TestEntityTracker;
import com.company.hunttech.entity.SkillTree;
import com.company.hunttech.entity.ai.AiCapability;
import com.company.hunttech.service.AiCredentialOwner;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.service.AiExecutionService;
import com.company.hunttech.service.SkillAnalysisResult;
import com.company.hunttech.service.SkillAnalysisService;
import com.company.hunttech.service.SkillAnalysisServiceBean;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Контейнерный тест {@link SkillAnalysisServiceBean} со стабом {@link AiExecutionService}
 * (реальные провайдеры в тестах не вызываются — см. AI Control Plane, §5).
 *
 * <p>Проверяет и контракт пользовательской нотификации: результат несёт метаданные
 * AI-выполнения (модель, провайдер, собственник API) при AI-анализе и {@code null}
 * при классическом fallback (см. HRM_HuntTech_AI_User_Notification_Contract).</p>
 *
 * <p>Имена создаваемых навыков уникальны (SKILL_NAME — unique-constraint, тестовая БД
 * переиспользуется между прогонами).</p>
 */
public class SkillAnalysisServiceBeanTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private SkillAnalysisServiceBean bean;
    private StubAiExecutionService stub;
    private DataManager dataManager;
    private TestEntityTracker tracker;

    @Before
    public void setUp() throws Exception {
        dataManager = AppBeans.get(DataManager.class);
        tracker = new TestEntityTracker(dataManager);
        stub = new StubAiExecutionService();
        bean = new SkillAnalysisServiceBean();
        injectField("aiExecutionService", stub);
        injectField("dataManager", dataManager);
    }

    @After
    public void tearDown() {
        tracker.cleanup();
    }

    private void injectField(String name, Object value) throws Exception {
        Field field = SkillAnalysisServiceBean.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(bean, value);
    }

    private String uniqueName(String base) {
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private SkillTree createSkill(String name, Boolean notParsing) {
        SkillTree skill = dataManager.create(SkillTree.class);
        skill.setSkillName(name);
        skill.setNotParsing(notParsing);
        skill.setPrioritySkill(0);
        return tracker.track(dataManager.commit(skill, "skillTree-edit-view"));
    }

    @Test
    public void analyzeAllReturnsMatchedSkillsAndSkipsUnknown() {
        String javaName = uniqueName("Java");
        String springName = uniqueName("Spring");
        createSkill(javaName, false);
        createSkill(springName, false);
        String unknownSkill = "FakeSkill-" + UUID.randomUUID().toString().substring(0, 8);
        stub.result = "[\"" + javaName + "\", \"" + springName + "\", \"" + unknownSkill + "\"]";

        SkillAnalysisResult outcome = bean.analyzeAll("Текст резюме");
        List<SkillTree> skills = outcome.getSkills();

        assertEquals(2, skills.size());
        assertEquals(javaName, skills.get(0).getSkillName());
        assertEquals(springName, skills.get(1).getSkillName());
        assertEquals(SkillAnalysisService.LEVEL_ALL,
                stub.lastContext.get(SkillAnalysisService.PARAM_SKILL_LEVEL));
        assertEquals("Текст резюме",
                stub.lastContext.get(SkillAnalysisService.PARAM_SOURCE_TEXT));
    }

    @Test
    public void aiExecutionMetadataIsReturnedWhenAiUsed() {
        String javaName = uniqueName("Java");
        createSkill(javaName, false);
        stub.result = "[\"" + javaName + "\"]";

        SkillAnalysisResult outcome = bean.analyzeAll("Текст резюме");

        // Контракт пользовательской нотификации: модель, провайдер и собственник API
        // доходят до потребителя (экран показывает их в исчезающей нотификации).
        AiExecutionResult execution = outcome.getAiExecution();
        assertNotNull("AI-анализ выполнен — метаданные выполнения обязательны", execution);
        assertEquals("test-model", execution.getModelName());
        assertEquals("test-provider", execution.getProviderCode());
        assertEquals(AiCredentialOwner.ADMIN, execution.getCredentialOwner());
        assertEquals(SkillAnalysisService.FUNCTION_SKILLS_EXTRACT, execution.getFunctionCode());
        assertTrue(outcome.isAiUsed());
    }

    @Test
    public void analyzeMainPassesLevelMain() {
        String javaName = uniqueName("Java");
        createSkill(javaName, false);
        stub.result = "[\"" + javaName + "\"]";

        bean.analyzeMain("Текст вакансии");

        assertEquals(SkillAnalysisService.LEVEL_MAIN,
                stub.lastContext.get(SkillAnalysisService.PARAM_SKILL_LEVEL));
    }

    @Test
    public void analyzeSecondaryAndTertiaryPassLevels() {
        String javaName = uniqueName("Java");
        createSkill(javaName, false);
        stub.result = "[]";

        bean.analyzeSecondary("Текст");
        assertEquals(SkillAnalysisService.LEVEL_SECONDARY,
                stub.lastContext.get(SkillAnalysisService.PARAM_SKILL_LEVEL));

        bean.analyzeTertiary("Текст");
        assertEquals(SkillAnalysisService.LEVEL_TERTIARY,
                stub.lastContext.get(SkillAnalysisService.PARAM_SKILL_LEVEL));
    }

    @Test
    public void markdownFencedJsonResponseIsParsed() {
        String javaName = uniqueName("Java");
        createSkill(javaName, false);
        String unknownSkill = "FakeSkill-" + UUID.randomUUID().toString().substring(0, 8);
        stub.result = "```json\n[\"" + javaName + "\", \"" + unknownSkill + "\"]\n```";

        List<SkillTree> skills = bean.analyzeAll("Текст").getSkills();

        assertEquals(1, skills.size());
        assertEquals(javaName, skills.get(0).getSkillName());
    }

    @Test
    public void plainTextResponseIsParsed() {
        String javaName = uniqueName("Java");
        String sqlName = uniqueName("SQL");
        createSkill(javaName, false);
        createSkill(sqlName, false);
        stub.result = javaName + ", " + sqlName;

        List<SkillTree> skills = bean.analyzeAll("Текст").getSkills();

        assertEquals(2, skills.size());
    }

    @Test
    public void aiFailureFallsBackToDictionarySearchInText() {
        String javaName = uniqueName("Java");
        String springName = uniqueName("Spring");
        createSkill(javaName, false);
        createSkill(springName, false);
        stub.failure = new RuntimeException("AI недоступен: нет активного корпоративного подключения");

        SkillAnalysisResult outcome = bean.analyzeAll("Владею технологиями " + javaName + " и " + springName);

        assertEquals(2, outcome.getSkills().size());
        // Классический fallback: AI не выполнялся — метаданные отсутствуют, экран
        // не показывает нотификацию «обработано ИИ» (контракт пользовательской нотификации).
        assertNull("При классическом fallback метаданные AI-выполнения должны быть null",
                outcome.getAiExecution());
        assertTrue(!outcome.isAiUsed());
    }

    @Test
    public void notParsingSkillsAreExcludedFromDictionary() {
        String javaName = uniqueName("Java");
        String secretName = uniqueName("SecretSkill");
        createSkill(javaName, false);
        createSkill(secretName, true);
        stub.result = "[\"" + javaName + "\", \"" + secretName + "\"]";

        List<SkillTree> skills = bean.analyzeAll("Текст").getSkills();

        assertEquals(1, skills.size());
        assertEquals(javaName, skills.get(0).getSkillName());
    }

    @Test
    public void blankTextThrowsDevelopmentException() {
        try {
            bean.analyzeAll("   ");
            fail("Должно быть выброшено DevelopmentException для пустого текста");
        } catch (DevelopmentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void serviceIsResolvableThroughAppBeans() {
        assertNotNull(AppBeans.get(SkillAnalysisService.class));
    }

    @Test
    public void emptyAiResponseReturnsEmptyList() {
        createSkill(uniqueName("Java"), false);
        stub.result = "[]";
        assertTrue(bean.analyzeAll("Текст").getSkills().isEmpty());
    }

    @Test
    public void experienceYearsCollapseToSingleMaxSkill() {
        // Навыки опыта «1 год»…«20 лет» уже есть в справочнике dev-БД
        // (тест-контейнер использует локальную БД) — их не создаём.
        String javaName = uniqueName("Java");
        createSkill(javaName, false);
        stub.result = "[\"" + javaName + "\", \"1 год\", \"2 года\", \"5 лет\"]";

        List<SkillTree> skills = bean.analyzeAll("Резюме").getSkills();

        assertEquals(2, skills.size());
        assertEquals(javaName, skills.get(0).getSkillName());
        // Ровно один навык опыта — с максимальным (общим) стажем.
        assertEquals("5 лет", skills.get(1).getSkillName());
    }

    @Test
    public void experienceYearAbsentFromDictionaryIsSkippedWithWarning() {
        // «21 год» отсутствует в справочнике (диапазон 1–20 лет): навык не
        // возвращается, в лог пишется WARN для администратора (последующий анализ).
        String javaName = uniqueName("Java");
        createSkill(javaName, false);
        stub.result = "[\"" + javaName + "\", \"21 год\"]";

        List<SkillTree> skills = bean.analyzeAll("Резюме").getSkills();

        assertEquals(1, skills.size());
        assertEquals(javaName, skills.get(0).getSkillName());
    }

    @Test
    public void fallbackCollapsesExperienceYearsToSingleMaxSkill() {
        String javaName = uniqueName("Java");
        String springName = uniqueName("Spring");
        createSkill(javaName, false);
        createSkill(springName, false);
        stub.failure = new RuntimeException("AI недоступен");

        List<SkillTree> skills = bean.analyzeAll(
                "Владею технологиями " + javaName + " и " + springName
                        + ". Стаж: 5 лет, ранее 2 года.").getSkills();

        // Java + Spring + единственный навык опыта «5 лет» (2 года схлопнут).
        assertEquals(3, skills.size());
        assertTrue(skills.stream().anyMatch(s -> "5 лет".equals(s.getSkillName())));
        assertTrue(skills.stream().noneMatch(s -> "2 года".equals(s.getSkillName())));
    }

    /**
     * Стаб AI-шлюза: возвращает заданный ответ или бросает заданное исключение,
     * запоминая последний вызов. Метаданные результата фиксированы (test-model /
     * test-provider / ADMIN) — контракт нотификации проверяется по ним.
     */
    private static final class StubAiExecutionService implements AiExecutionService {
        private String result = "[]";
        private RuntimeException failure;
        private String lastFunctionCode;
        private Map<String, Object> lastContext;

        @Override
        public AiExecutionResult executeText(String functionCode, Map<String, Object> context) {
            lastFunctionCode = functionCode;
            lastContext = new LinkedHashMap<>(context);
            if (failure != null) {
                throw failure;
            }
            return AiExecutionResult.textResult(
                    SkillAnalysisService.FUNCTION_SKILLS_EXTRACT, "Извлечение навыков",
                    AiCapability.TEXT_GENERATION, "test-model", "test-provider",
                    AiCredentialOwner.ADMIN, result);
        }

        @Override
        public AiExecutionResult executeImage(String functionCode, Map<String, Object> context,
                                              byte[] sourceImage, String sourceMimeType) {
            throw new UnsupportedOperationException("Не используется в тестах SkillAnalysisService");
        }
    }
}
