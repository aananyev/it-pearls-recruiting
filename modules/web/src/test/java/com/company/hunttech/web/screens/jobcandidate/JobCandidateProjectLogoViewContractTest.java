package com.company.hunttech.web.screens.jobcandidate;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

/**
 * Регрессионный контракт графа загрузки логотипа проекта в таблице резюме.
 *
 * Project.projectLogo остаётся LAZY-ссылкой. Без явного вложенного view CUBA
 * передаёт в генератор колонки detached Project с unfetched-атрибутом.
 */
public class JobCandidateProjectLogoViewContractTest {

    @Test
    public void additionalViewConfigIsRegisteredForAllApplicationBlocks() throws Exception {
        String component = readSource("modules/global/src/com/company/hunttech/app-component.xml");

        assertTrue(component.contains("com/company/hunttech/views.xml"));
        assertTrue(component.contains("com/company/hunttech/job-candidate-project-logo-views.xml"));
    }

    @Test
    public void openPositionEditViewIsExplicitlyOverwritten() throws Exception {
        String views = readSource(
                "modules/global/src/com/company/hunttech/job-candidate-project-logo-views.xml");

        assertTrue(views.contains("name=\"openPosition-edit-view\""));
        assertTrue(views.contains("overwrite=\"true\""));
        assertTrue(views.contains("name=\"projectName\" view=\"_local\""));
    }

    @Test
    public void projectLogoReferenceHasNestedFileDescriptorView() throws Exception {
        String views = readSource(
                "modules/global/src/com/company/hunttech/job-candidate-project-logo-views.xml");

        assertTrue(views.contains("name=\"projectLogo\" view=\"_local\""));
    }

    @Test
    public void projectLogoRemainsLazyInEntity() throws Exception {
        String project = readSource("modules/global/src/com/company/hunttech/entity/Project.java");

        assertTrue(project.contains("@ManyToOne(fetch = FetchType.LAZY)"));
        assertTrue(project.contains("private FileDescriptor projectLogo"));
    }

    @Test
    public void columnGeneratorStillUsesExistingRenderingHelper() throws Exception {
        String controller = readSource(
                "modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java");

        assertTrue(controller.contains("jobCandidateCandidateCvTableProjectLogoColumnColumnGenerator"));
        assertTrue(controller.contains("FileDescriptorImageHelper.setCompanyLogo"));
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
