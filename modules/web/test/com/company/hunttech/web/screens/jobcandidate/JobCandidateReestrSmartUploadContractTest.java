package com.company.hunttech.web.screens.jobcandidate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Регрессионный контрактный тест BL-2026-024:
 * Позиционирование и фокус на созданном кандидате после завершения «Умной загрузки» в JobCandidateReestr.
 */
public class JobCandidateReestrSmartUploadContractTest {

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
    @DisplayName("Проверка позиционирования, скролла и синхронизации сайдбара после SmartCvUploadScreen")
    void testPostSmartUploadFocusAndSelection() throws Exception {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateReestr.java");
        assertTrue(javaFile.exists(), "JobCandidateReestr.java должен существовать");

        String content = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);

        // 1. Проверка снятия фильтра меток и сброса области
        assertTrue(content.contains("setCandidateScopeFilter(\"ALL\", \"Все кандидаты\", \"USERS\");"),
                "После умной загрузки область фильтра должна сбрасываться на ALL");
        assertTrue(content.contains("jobCandidatesDl.removeParameter(\"signIcon\");"),
                "После умной загрузки должен сниматься фильтр меток");

        // 2. Проверка гарантированного получения экземпляра сущности из контейнера или загрузки
        assertTrue(content.contains("jobCandidatesDc.getItemOrNull(candidateId)"),
                "Кандидат должен проверяться в контейнере реестра");
        assertTrue(content.contains("jobCandidatesDc.getMutableItems().add(0, toSelect);"),
                "Если кандидат не попал в первые 200 строк, он должен быть добавлен в контейнер");

        // 3. Проверка выделения, скролла и фокуса
        assertTrue(content.contains("candidatesTable.setSelected(toSelect);"),
                "Таблица обязана выделить созданного кандидата");
        assertTrue(content.contains("candidatesTable.scrollTo(toSelect);"),
                "Таблица обязана выполнить скролл к выбранной строке кандидата");
        assertTrue(content.contains("candidatesTable.focus();"),
                "Таблица должна получить фокус");

        // 4. Проверка синхронизации сайдбара деталей и кнопок действий
        assertTrue(content.contains("populateDetailPane(toSelect);"),
                "Левая панель деталей должна сразу заполниться данными созданного кандидата");
        assertTrue(content.contains("updateActionsState(toSelect);"),
                "Кнопки быстрых действий должны активироваться для созданного кандидата");
        assertTrue(content.contains("updateSignIconsState(toSelect);"),
                "Состояние меток кандидата должно быть обновлено");
    }
}
