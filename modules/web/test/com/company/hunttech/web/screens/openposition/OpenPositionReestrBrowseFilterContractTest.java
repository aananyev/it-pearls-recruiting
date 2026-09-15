package com.company.hunttech.web.screens.openposition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Контрактный тест фильтрации вакансий в статусе «драфт / на проверку»
 * в реестре открытых позиций (OpenPositionReestrBrowse).
 */
public class OpenPositionReestrBrowseFilterContractTest {

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
    @DisplayName("Проверка JPQL-условий и компонентов в open-position-reestr-browse.xml")
    void testXmlDescriptorFilterConditions() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/openposition/open-position-reestr-browse.xml");
        assertTrue(xmlFile.exists(), "Файл open-position-reestr-browse.xml должен существовать");

        String content = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);

        // 1. Условие подбора вакансий в статусе драфт/на проверку
        assertTrue(content.contains(":underReviewOrDraft = true and (e.priority in (-2, -1) or e.signDraft = true)"),
                "XML обязан содержать условие :underReviewOrDraft для отбора вакансий со статусом драфт/на проверку (-2, -1, signDraft=true)");

        // 2. Условие исключения черновиков из общего рабочего списка
        assertTrue(content.contains(":excludeDrafts = true and (e.signDraft is null or e.signDraft = false) and (e.priority is null or e.priority >= 0)"),
                "XML обязан содержать условие :excludeDrafts для исключения черновиков из общего списка");

        // 3. Наличие пункта «На проверку» в выпадающем меню кнопки «Приоритет»
        assertTrue(content.contains("id=\"priorityFilterPopupButton\""),
                "XML обязан содержать popupButton priorityFilterPopupButton");
        assertTrue(content.contains("id=\"priorityUnderReview\""),
                "priorityFilterPopupButton обязан содержать action priorityUnderReview");
    }

    @Test
    @DisplayName("Проверка Java-контроллера OpenPositionReestrBrowse на поддержку фильтра «На проверку»")
    void testJavaControllerFilterLogic() throws Exception {
        File javaFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionReestrBrowse.java");
        assertTrue(javaFile.exists(), "Файл OpenPositionReestrBrowse.java должен существовать");

        String content = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);

        // 1. Обработка действия priorityUnderReview
        assertTrue(content.contains("priorityUnderReview"),
                "Контроллер обязан регистрировать действие priorityUnderReview");
        assertTrue(content.contains("openPositionsDl.setParameter(\"underReviewOrDraft\", true)"),
                "Действие priorityUnderReview обязано устанавливать underReviewOrDraft = true");

        // 2. Инициализация excludeDrafts в onBeforeShow
        assertTrue(content.contains("openPositionsDl.setParameter(\"excludeDrafts\", true)"),
                "onBeforeShow обязан устанавливать excludeDrafts = true по умолчанию");

        // 3. Автопереключение на «На проверку» после умной загрузки
        assertTrue(content.contains("[SMART_VACANCY_OPENING_UI] Переключение фильтра на «На проверку»"),
                "Контроллер обязан логировать и переключать фильтр на «На проверку» после завершения SmartOpenPositionUploadScreen");

        // 4. Отображение статуса в сайдбаре
        assertTrue(content.contains("На проверку"),
                "Сайдбар обязан отображать статус «На проверку» для вакансий с priority = -2");
    }
}
