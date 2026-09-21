package com.company.hunttech.web.screens.openposition;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Контракт UI-генерации трёх материалов вакансии в OpenPositionEdit. */
public class OpenPositionAiMaterialContractTest {

    @Test
    void threeTabsExposeOneSharedMaterialGenerationFlow() throws Exception {
        String xml = read("modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml");
        String java = read("modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEdit.java");

        assertEquals(3, occurrences(xml, "caption=\"msg://msgGenerateVacancyMaterial\""));
        assertTrue(xml.contains("id=\"generateInterviewPlanBtn\""));
        assertTrue(xml.contains("id=\"generateSearchMapBtn\""));
        assertTrue(xml.contains("id=\"generateInterviewChecklistBtn\""));
        assertTrue(xml.contains("align=\"MIDDLE_RIGHT\""));

        assertTrue(java.contains("generateVacancyMaterial(VacancyMaterialType.INTERVIEW_PLAN"));
        assertTrue(java.contains("generateVacancyMaterial(VacancyMaterialType.SEARCH_MAP"));
        assertTrue(java.contains("generateVacancyMaterial(VacancyMaterialType.CHECKLIST"));
        assertEquals(1, occurrences(java, "private void generateVacancyMaterial(VacancyMaterialType materialType"));
        assertTrue(java.contains("hrmAiService.generateVacancyMaterial(materialType, context)"));
        assertFalse(java.contains("FUNCTION_VACANCY_CHECKLIST"), "Экран не должен выбирать functionCode");
    }

    @Test
    void generationProtectsExistingTextAndFollowsAiNotificationContract() throws Exception {
        String java = read("modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEdit.java");
        String materialFlow = java.substring(java.indexOf("private void startVacancyMaterialGeneration"),
                java.indexOf("private String resolveVacancyDescription"));

        assertTrue(java.contains("Заменить существующий текст?"));
        assertTrue(java.contains("Заменить и генерировать"));
        assertTrue(java.contains("new BackgroundTask<Integer, AiExecutionResult>(120, this)"));
        assertTrue(java.contains("AiOperationNotifier.showStarted(notifications"));
        assertTrue(java.contains("AiOperationNotifier.showProgress(this"));
        assertTrue(java.contains("AiOperationNotifier.show(notifications, result"));
        assertEquals(3, occurrences(materialFlow, "AiOperationNotifier.closeProgress(progressDialog)"));
        assertTrue(materialFlow.contains("targetField.setValue(result.getText())"));
        assertTrue(materialFlow.contains("sourceButton.setEnabled(false)"));
        assertTrue(materialFlow.contains("restoreMaterialButtonState(materialType, sourceButton, targetField)"));
        assertFalse(materialFlow.contains("ex.getMessage()"));
    }

    private String read(String relativePath) throws Exception {
        File file = new File(relativePath);
        if (!file.exists()) file = new File("../" + relativePath);
        if (!file.exists()) file = new File("../../" + relativePath);
        assertTrue(file.exists(), relativePath + " должен существовать");
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private int occurrences(String text, String needle) {
        int count = 0;
        for (int from = 0; (from = text.indexOf(needle, from)) >= 0; from += needle.length()) count++;
        return count;
    }
}
