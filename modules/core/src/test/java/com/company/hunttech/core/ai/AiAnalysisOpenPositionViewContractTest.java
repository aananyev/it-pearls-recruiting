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
 * ViewBuilder в CUBA 7.3 не имеет перегрузки addView(String, View).
 * Auto-resolution по типу сущности используется для связывания вложенных view.
 * projectDepartment и companyName разрешаются как атрибуты Project и CompanyDepartament.
 */
public class AiAnalysisOpenPositionViewContractTest {

    @Test
    public void openPositionViewIncludesProjectNameAttribute() throws Exception {
        String method = extractMethod(readAiAnalysisService(), "buildOpenPositionAnalysisView");

        assertTrue(method.contains("addAll(\"shortDescription\", \"comment\", \"projectName\")"));
        assertTrue(method.contains("addView(ViewBuilder.of(com.company.hunttech.entity.Project.class)"));
    }

    @Test
    public void projectViewIncludesDepartmentAndProjectName() throws Exception {
        String method = extractMethod(readAiAnalysisService(), "buildOpenPositionAnalysisView");

        assertTrue(method.contains("addAll(\"projectName\", \"projectDepartment\")"));
        assertTrue(method.contains("addView(ViewBuilder.of(com.company.hunttech.entity.CompanyDepartament.class)"));
    }

    @Test
    public void departmentViewIncludesCompanyNameAttribute() throws Exception {
        String method = extractMethod(readAiAnalysisService(), "buildOpenPositionAnalysisView");

        assertTrue(method.contains("addAll(\"companyName\")"));
        assertTrue(method.contains("addView(ViewBuilder.of(com.company.hunttech.entity.Company.class)"));
    }

    @Test
    public void companyViewKeepsLegacyComanyNameAttribute() throws Exception {
        String method = extractMethod(readAiAnalysisService(), "buildOpenPositionAnalysisView");
        String company = readSource("modules/global/src/com/company/hunttech/entity/Company.java");

        assertTrue(method.contains("addAll(\"comanyName\")"));
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
        int start = source.indexOf("private View " + methodName);
        if (start < 0) start = source.indexOf("private View buildOpenPositionAnalysisView");
        int brace = source.indexOf('{', start);
        int depth = 1;
        int end = brace + 1;
        while (depth > 0 && end < source.length()) {
            char c = source.charAt(end);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            end++;
        }
        return source.substring(start, end);
    }
}
