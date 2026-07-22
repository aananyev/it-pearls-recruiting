package com.company.hunttech.core.ai;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Регрессионный контракт analysis-view вакансии.
 *
 * ViewBuilder должен связывать каждый вложенный view с конкретным property.
 * Иначе CUBA пытается разрешать projectDepartment и companyName относительно
 * OpenPosition и системная кнопка AI-анализа завершается DevelopmentException.
 */
public class AiAnalysisOpenPositionViewContractTest {

    @Test
    public void openPositionViewUsesExplicitProjectProperty() throws Exception {
        String method = extractMethod(readAiAnalysisService(), "buildOpenPositionAnalysisView");

        assertTrue(method.contains(".addView(\"projectName\","));
        assertFalse(method.contains(".addAll(\"shortDescription\", \"comment\", \"projectName\")"));
    }

    @Test
    public void projectViewUsesExplicitDepartmentProperty() throws Exception {
        String method = extractMethod(readAiAnalysisService(), "buildOpenPositionAnalysisView");

        assertTrue(method.contains(".addView(\"projectDepartment\","));
        assertFalse(method.contains(".addAll(\"projectName\", \"projectDepartment\")"));
    }

    @Test
    public void departmentViewUsesExplicitCompanyProperty() throws Exception {
        String method = extractMethod(readAiAnalysisService(), "buildOpenPositionAnalysisView");

        assertTrue(method.contains(".addView(\"companyName\","));
        assertFalse(method.contains(".addAll(\"companyName\")"));
    }

    @Test
    public void companyViewKeepsLegacyComanyNameAttribute() throws Exception {
        String method = extractMethod(readAiAnalysisService(), "buildOpenPositionAnalysisView");
        String company = readSource("modules/global/src/com/company/hunttech/entity/Company.java");

        assertTrue(method.contains(".addAll(\"comanyName\")"));
        assertTrue(company.contains("protected String comanyName;"));
        assertFalse(company.contains("protected String companyName;"));
    }

    @Test
    public void departmentCompanyNameIsACompanyReference() throws Exception {
        String department = readSource(
                "modules/global/src/com/company/hunttech/entity/CompanyDepartament.java");

        assertTrue(department.contains("protected Company companyName;"));
        assertTrue(department.contains("public Company getCompanyName()"));
    }

    private static String readAiAnalysisService() throws Exception {
        return readSource("modules/core/src/com/company/hunttech/core/ai/AiAnalysisServiceBean.java");
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

    private static String extractMethod(String source, String methodName) {
        int start = source.indexOf("private View " + methodName + "()");
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
