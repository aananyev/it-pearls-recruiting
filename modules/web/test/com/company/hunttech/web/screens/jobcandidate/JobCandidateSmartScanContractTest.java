package com.company.hunttech.web.screens.jobcandidate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Контрактный тест интеграции функции «Умное сканирование» резюме в JobCandidateEdit.
 */
public class JobCandidateSmartScanContractTest {

    private File resolveFile(String relativePath) {
        File f = new File(relativePath);
        if (f.exists()) return f;
        f = new File("../" + relativePath);
        if (f.exists()) return f;
        f = new File("../../" + relativePath);
        if (f.exists()) return f;
        return new File(relativePath);
    }

    @Test
    @DisplayName("Проверка разметки job-candidate-edit.xml: кнопка smartScanCvBtn и Data View Integrity")
    void testJobCandidateEditXml() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml");
        assertTrue(xmlFile.exists(), "job-candidate-edit.xml должен существовать");

        String content = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);

        // 1. Кнопка smartScanCvBtn
        assertTrue(content.contains("id=\"smartScanCvBtn\""), "В тулбаре должна присутствовать кнопка smartScanCvBtn");
        assertTrue(content.contains("caption=\"msg://msgSmartScanCv\""), "Кнопка должна использовать caption msg://msgSmartScanCv");
        assertTrue(content.contains("icon=\"font-icon:MAGIC\""), "Кнопка должна использовать иконку font-icon:MAGIC");
        assertTrue(content.contains("enable=\"false\""), "По умолчанию кнопка должна быть неактивна (enable=\"false\")");
        assertTrue(content.contains("description=\"msg://msgSmartScanCvTooltip\""), "Кнопка должна содержать tooltip");

        // 2. Data View Integrity для candidateCv
        assertTrue(content.contains("<property name=\"originalFileCV\" view=\"_local\"/>"), "view candidateCv обязан декларировать originalFileCV");
        assertTrue(content.contains("<property name=\"fileCV\" view=\"_local\"/>"), "view candidateCv обязан декларировать fileCV");
        assertTrue(content.contains("<property name=\"textCV\"/>"), "view candidateCv обязан декларировать textCV");
    }

    @Test
    @DisplayName("Проверка Java-контроллера JobCandidateEdit: логика enabled/disabled и диалог")
    void testJobCandidateEditJava() throws Exception {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java");
        assertTrue(javaFile.exists(), "JobCandidateEdit.java должен существовать");

        String content = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);

        assertTrue(content.contains("private Button smartScanCvBtn;"), "Поле smartScanCvBtn должно быть объявлено в контроллере");
        assertTrue(content.contains("jobCandidateCandidateCvTable.addSelectionListener(e -> updateSmartScanButtonState());"), "Слушатель выбора строк должен обновлять статус кнопки");
        assertTrue(content.contains("smartScanCvBtn.addClickListener(e -> onSmartScanCvBtnClick());"), "Кнопка должна вызывать onSmartScanCvBtnClick");
        assertTrue(content.contains("singleSelected = selected != null && selected.size() == 1;"), "Кнопка активна строго при 1 выделенном резюме");
        assertTrue(content.contains("openSmartCvScanDialog(cv);"), "Должен вызываться метод открытия диалога сканирования");
        assertTrue(content.contains("applySmartScanResults("), "Должен присутствовать метод применения результатов сканирования");
    }

    @Test
    @DisplayName("Проверка XML-дескриптора и регистрации диалога SmartCvScanDialog")
    void testSmartCvScanDialogStructure() throws Exception {
        File dialogXml = resolveFile("modules/web/src/com/company/hunttech/web/screens/jobcandidate/smart-cv-scan-dialog.xml");
        assertTrue(dialogXml.exists(), "smart-cv-scan-dialog.xml должен существовать");

        String xmlContent = new String(Files.readAllBytes(dialogXml.toPath()), StandardCharsets.UTF_8);
        assertTrue(xmlContent.contains("id=\"progressBar\""), "Диалог должен содержать progressBar");
        assertTrue(xmlContent.contains("id=\"statusLabel\""), "Диалог должен содержать statusLabel");
        assertTrue(xmlContent.contains("id=\"comparisonRowsBox\""), "Диалог должен содержать контейнер строк сопоставления");
        assertTrue(xmlContent.contains("id=\"applyBtn\""), "Диалог должен содержать кнопку подтверждения applyBtn");
        assertTrue(xmlContent.contains("id=\"cancelBtn\""), "Диалог должен содержать кнопку отмены cancelBtn");

        File screensXml = resolveFile("modules/web/src/com/company/hunttech/web-screens.xml");
        String screensContent = new String(Files.readAllBytes(screensXml.toPath()), StandardCharsets.UTF_8);
        assertTrue(screensContent.contains("id=\"hunttech_SmartCvScanDialog\""), "Экран hunttech_SmartCvScanDialog должен быть зарегистрирован в web-screens.xml");
    }

    @Test
    @DisplayName("Проверка пакетов локализации (русский и английский)")
    void testLocalization() throws Exception {
        File ruFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/jobcandidate/messages_ru.properties");
        String ruContent = new String(Files.readAllBytes(ruFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(ruContent.contains("msgSmartScanCv=Умное сканирование"), "В messages_ru должно быть название кнопки");
        assertTrue(ruContent.contains("smartCvScanDialog.title="), "В messages_ru должен быть заголовок диалога");

        File enFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/jobcandidate/messages.properties");
        String enContent = new String(Files.readAllBytes(enFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(enContent.contains("msgSmartScanCv=Smart scan"), "В messages должно быть название кнопки на английском");
        assertTrue(enContent.contains("smartCvScanDialog.title="), "В messages должен быть заголовок диалога на английском");
    }
}
