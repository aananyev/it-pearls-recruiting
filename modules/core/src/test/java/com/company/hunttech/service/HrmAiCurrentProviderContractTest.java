package com.company.hunttech.service;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Регрессионный контракт выбора единственной текущей AI-конфигурации.
 * Тест не вызывает внешний AI API и проверяет связность service/UI/migration-контракта.
 */
public class HrmAiCurrentProviderContractTest {

    @Test
    public void systemAnalysisUsesCurrentConfigurationInsteadOfHardcodedOpenAi() throws Exception {
        String source = readSource("modules/core/src/com/company/hunttech/core/ai/AiAnalysisServiceBean.java");
        assertTrue(source.contains("sendPromptUsingCurrentConfiguration(filledPrompt)"));
        assertFalse(source.contains("sendPrompt(filledPrompt, \"openai\")"));
    }

    @Test
    public void explicitProviderLookupDoesNotDependOnCurrentFlag() throws Exception {
        String source = readSource("modules/core/src/com/company/hunttech/service/HrmAiServiceBean.java");
        String query = extractConstant(source, "QUERY_USER_AI_CONFIG");
        assertTrue(query.contains("providerCode"));
        assertFalse(query.contains("isActive"));
    }

    @Test
    public void currentProviderLookupRequiresSingleActiveConfiguration() throws Exception {
        String source = readSource("modules/core/src/com/company/hunttech/service/HrmAiServiceBean.java");
        String query = extractConstant(source, "QUERY_CURRENT_AI_CONFIG");
        assertTrue(query.contains("isActive = true"));
        assertTrue(source.contains("currentConfigurations.size() > 1"));
    }

    @Test
    public void switchRunsInCoreTransactionAndDeactivatesOtherRows() throws Exception {
        String source = readSource("modules/core/src/com/company/hunttech/service/HrmAiServiceBean.java");
        String method = extractMethod(source, "setCurrentConfiguration");
        assertTrue(method.contains("persistence.createTransaction()"));
        assertTrue(method.contains("set e.isActive = false"));
        assertTrue(method.contains("selected.setIsActive(true)"));
        assertTrue(method.contains("transaction.commit()"));
    }

    @Test
    public void databaseMigrationCreatesPartialUniqueIndex() throws Exception {
        String migration = readSource(
                "modules/core/db/update/postgres/27/270722-002-enforceCurrentAiConfiguration.sql");
        assertTrue(migration.contains("CREATE UNIQUE INDEX IF NOT EXISTS"));
        assertTrue(migration.contains("WHERE IS_ACTIVE = TRUE AND DELETE_TS IS NULL"));
    }

    @Test
    public void browseScreenOffersLocalizedCurrentProviderAction() throws Exception {
        String xml = readSource(
                "modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/user-ai-configuration-browse.xml");
        assertTrue(xml.contains("id=\"makeCurrentBtn\""));
        assertTrue(xml.contains("caption=\"msg://makeCurrentBtn.caption\""));
        assertTrue(xml.contains("caption=\"msg://UserAiConfiguration.currentForAnalysis\""));
    }

    private static String readSource(String relativePath) throws Exception {
        String base = System.getProperty("user.dir");
        if (!new File(base, relativePath).exists()) {
            base = new File(base).getParent();
        }
        File file = new File(base, relativePath);
        if (!file.exists()) {
            file = new File("../../" + relativePath);
        }
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static String extractConstant(String source, String constantName) {
        int start = source.indexOf(constantName);
        if (start < 0) {
            return "";
        }
        int end = source.indexOf(';', start);
        return end >= 0 ? source.substring(start, end + 1) : source.substring(start);
    }

    private static String extractMethod(String source, String methodName) {
        int start = source.indexOf("void " + methodName + "(");
        if (start < 0) {
            return "";
        }
        int brace = source.indexOf('{', start);
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            if (source.charAt(i) == '{') {
                depth++;
            } else if (source.charAt(i) == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(start, i + 1);
                }
            }
        }
        return source.substring(start);
    }
}
