package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Контрактный тест для вкладки «Места работы» (Work Experience) в экранной форме CandidateCVEdit.
 */
public class CandidateCVEditJobHistoryContractTest {

    private static final String SCREEN_XML =
            "modules/web/src/com/company/hunttech/web/screens/candidatecv/candidate-cv-edit.xml";
    private static final String CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/candidatecv/CandidateCVEdit.java";
    private static final String MESSAGES_RU =
            "modules/web/src/com/company/hunttech/web/screens/candidatecv/messages_ru.properties";
    private static final String MESSAGES_EN =
            "modules/web/src/com/company/hunttech/web/screens/candidatecv/messages.properties";

    private static final List<String> THEMES = Arrays.asList(
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark"
    );

    @Test
    public void screenXmlContainsJobHistoryTabAndContainers() throws IOException {
        String xml = readProjectFile(SCREEN_XML);

        assertTrue("Контейнер jobHistoriesDc должен быть объявлен", xml.contains("id=\"jobHistoriesDc\""));
        assertTrue("Загрузчик jobHistoriesDl должен быть объявлен", xml.contains("id=\"jobHistoriesDl\""));
        assertTrue("Вкладка tabJobHistory должна быть объявлена", xml.contains("<tab id=\"tabJobHistory\""));
        assertTrue("Тулбар jobHistoryToolbar должен быть объявлен", xml.contains("id=\"jobHistoryToolbar\""));
        assertTrue("Кнопка умного распознавания мест работы должна присутствовать", xml.contains("id=\"smartParseWorkExperienceBtn\""));
        assertTrue("Таблица jobHistoriesTable должна присутствовать", xml.contains("id=\"jobHistoriesTable\""));
        assertTrue("Кнопка добавления места работы должна быть", xml.contains("id=\"createJobHistoryBtn\""));
        assertTrue("Кнопка редактирования места работы должна быть", xml.contains("id=\"editJobHistoryBtn\""));
        assertTrue("Кнопка удаления места работы должна быть", xml.contains("id=\"removeJobHistoryBtn\""));
        assertTrue("Сайдбар навигация candidateCvJobHistoryNavigation должна быть объявлена", xml.contains("id=\"candidateCvJobHistoryNavigation\""));
    }

    @Test
    public void controllerContainsJobHistoryHandlers() throws IOException {
        String java = readProjectFile(CONTROLLER);

        assertTrue("Метод smartExtractWorkExperience должен присутствовать в контроллере",
                java.contains("public void smartExtractWorkExperience()"));
        assertTrue("Метод refreshJobHistories должен присутствовать в контроллере",
                java.contains("refreshJobHistories()"));
        assertTrue("Колонка генератора периода работы должна быть объявлена",
                java.contains("jobHistoriesTablePeriodColumnGenerator"));
        assertTrue("Колонка генератора компании должна быть объявлена",
                java.contains("jobHistoriesTableCompanyNameColumnGenerator"));
        assertTrue("Колонка генератора должности должна быть объявлена",
                java.contains("jobHistoriesTablePositionNameColumnGenerator"));
        assertTrue("Навигационный метод navigateJobHistoryTable должен присутствовать",
                java.contains("public void navigateJobHistoryTable()"));
    }

    @Test
    public void messagesContainJobHistoryLocalizations() throws IOException {
        String ru = readProjectFile(MESSAGES_RU);
        String en = readProjectFile(MESSAGES_EN);

        assertTrue("В messages_ru должен быть msgTabJobHistory", ru.contains("msgTabJobHistory=Места работы"));
        assertTrue("В messages_ru должен быть msgSmartParseWorkExperience", ru.contains("msgSmartParseWorkExperience=Распознать места работы"));
        assertTrue("В messages_ru должен быть msgAddWorkExperience", ru.contains("msgAddWorkExperience=Добавить место работы"));

        assertTrue("В messages должен быть msgTabJobHistory", en.contains("msgTabJobHistory=Work Experience"));
        assertTrue("В messages должен быть msgSmartParseWorkExperience", en.contains("msgSmartParseWorkExperience="));
    }

    @Test
    public void allThemesContainJobHistoryTableStyles() throws IOException {
        for (String theme : THEMES) {
            String scss = readProjectFile("modules/web/themes/" + theme + "/com.company.hunttech/candidate-cv-editor.scss");
            assertTrue("Тема " + theme + " должна содержать стиль .candidate-cv-job-history-table",
                    scss.contains(".candidate-cv-job-history-table"));
        }
    }

    private String readProjectFile(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        if (root == null) {
            throw new IOException("Не найден корень проекта HRM HuntTech");
        }
        return new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
