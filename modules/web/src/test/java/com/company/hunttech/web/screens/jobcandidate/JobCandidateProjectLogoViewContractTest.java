package com.company.hunttech.web.screens.jobcandidate;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Регрессионный контракт безопасной lazy-загрузки логотипа проекта.
 * <p>
 * Глобальный overwrite openPosition-edit-view удалён. projectLogo загружается
 * только во вложенном view job-candidate-edit.xml. Исходный shared view
 * не усечён, Entity mapping остаётся FetchType.LAZY.
 */
public class JobCandidateProjectLogoViewContractTest {

    @Test
    public void screenGraphLocalLoadsProjectLogo() throws Exception {
        String screen = readSource(
                "modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml");

        assertTrue(screen.contains("candidateCv\" fetch=\"BATCH\""));
        assertTrue(screen.contains("view=\"openPosition-edit-view\""));
        assertTrue(screen.contains("projectName\" view=\"_local\""));
        assertTrue(screen.contains("projectLogo\" view=\"_local\""));
    }

    @Test
    public void noSeparateViewsFileRegistered() throws Exception {
        String component = readSource(
                "modules/global/src/com/company/hunttech/app-component.xml");

        assertTrue(component.contains("com/company/hunttech/views.xml"));
        assertFalse(component.contains("job-candidate-project-logo-views.xml"));
    }

    @Test
    public void noGlobalOverwriteForOpenPositionEditView() throws Exception {
        String component = readSource(
                "modules/global/src/com/company/hunttech/app-component.xml");
        String views = readSource("modules/global/src/com/company/hunttech/views.xml");

        assertFalse(component.contains("job-candidate-project-logo-views.xml"));

        // Исходный views.xml не должен содержать overwrite для openPosition-edit-view
        int start = views.indexOf("name=\"openPosition-edit-view\"");
        int end = views.indexOf("</view>", start);
        String block = views.substring(start, end);
        assertFalse("openPosition-edit-view не должен иметь overwrite=\"true\" в views.xml",
                block.contains("overwrite=\"true\""));
    }

    @Test
    public void originalEditViewRetainsAllCriticalProperties() throws Exception {
        String views = readSource("modules/global/src/com/company/hunttech/views.xml");

        // Извлекаем блок openPosition-edit-view
        int start = views.indexOf("name=\"openPosition-edit-view\"");
        int end = views.indexOf("</view>", start);
        String block = views.substring(start, end);

        assertTrue(block.contains("vacansyID"));
        assertTrue(block.contains("signDraft"));
        assertTrue(block.contains("rating"));
        assertTrue(block.contains("closingDate"));
        assertTrue(block.contains("salaryMin"));
        assertTrue(block.contains("salaryMax"));
        assertTrue(block.contains("rawDescription"));
        assertTrue(block.contains("interviewChecklist"));
        assertTrue(block.contains("searchMap"));
        assertTrue(block.contains("interviewPlan"));
        assertTrue(block.contains("grade"));
        assertTrue(block.contains("cityPosition"));
        assertTrue(block.contains("positionType"));
        assertTrue(block.contains("projectName"));
        assertTrue(block.contains("parentOpenPosition"));
        assertTrue(block.contains("owner"));
    }

    @Test
    public void projectLogoRemainsLazyInEntity() throws Exception {
        String project = readSource(
                "modules/global/src/com/company/hunttech/entity/Project.java");

        assertTrue(project.contains("@ManyToOne(fetch = FetchType.LAZY)"));
        assertTrue(project.contains("private FileDescriptor projectLogo"));
    }

    @Test
    public void columnGeneratorStillUsesExistingRenderingHelper() throws Exception {
        String controller = readSource(
                "modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java");

        assertTrue(controller.contains("jobCandidateCandidateCvTableProjectLogoColumnColumnGenerator"));
        assertTrue(controller.contains("FileDescriptorImageHelper.setCompanyLogo"));
        assertTrue(controller.contains("icons/no-company.png"));
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
}
