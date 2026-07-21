package com.company.hunttech.web.screens.candidatecv;

import com.haulmont.cuba.core.global.PersistenceHelper;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Регрессионный тест: защита от unfetched fileImageFace в CandidateCVEdit.
 *
 * Тест проверяет контракт безопасности, не запуская UI:
 * — исходный browse-view не включает fileImageFace;
 * — метод инициализации фото проверяет PersistenceHelper.isLoaded.
 */
public class CandidateCVEditRegressionTest {

    @Test
    public void setCandidatePicImageHasPersistenceGuard() throws Exception {
        // Читаем исходный код метода setCandidatePicImage
        String source = readSource("modules/web/src/com/company/hunttech/web/screens/candidatecv/CandidateCVEdit.java");

        // 1. Должен быть вызов PersistenceHelper.isLoaded
        assertTrue("setCandidatePicImage должен проверять PersistenceHelper.isLoaded",
                source.contains("PersistenceHelper.isLoaded"));

        // 2. fileImageFace не должен вызываться без предварительной проверки
        String picMethod = extractMethod(source, "setCandidatePicImage");
        int checkIdx = picMethod.indexOf("PersistenceHelper.isLoaded");
        int getterIdx = picMethod.indexOf(".getFileImageFace()");
        assertTrue("Проверка isLoaded должна быть ДО вызова getFileImageFace()",
                checkIdx < getterIdx);
    }

    @Test
    public void onAfterShow2CallsSetCandidatePicImage() throws Exception {
        String source = readSource("modules/web/src/com/company/hunttech/web/screens/candidatecv/CandidateCVEdit.java");
        String afterShow = extractMethod(source, "onAfterShow2");

        assertTrue("onAfterShow2 должен вызывать setCandidatePicImage",
                afterShow.contains("setCandidatePicImage()"));
    }

    @Test
    public void browseViewDoesNotIncludeFileImageFace() throws Exception {
        String viewsXml = readSource("modules/global/src/com/company/hunttech/views.xml");
        String browseView = extractView(viewsXml, "candidateCV-browse-view");

        assertFalse("candidateCV-browse-view НЕ должен содержать fileImageFace",
                browseView.contains("fileImageFace"));
    }

    @Test
    public void editorViewIncludesFileImageFace() throws Exception {
        String viewsXml = readSource("modules/global/src/com/company/hunttech/views.xml");
        String editView = extractView(viewsXml, "candidateCV-view");

        assertTrue("candidateCV-view должен содержать fileImageFace",
                editView.contains("fileImageFace"));
    }

    // ── helpers ──

    private static String readSource(String relativePath) throws Exception {
        String base = System.getProperty("user.dir");
        // в Gradle test user.dir = project root
        if (!new File(base, relativePath).exists()) {
            base = new File(base).getParent(); // на случай вложенного модуля
        }
        File f = new File(base, relativePath);
        if (!f.exists()) {
            f = new File("../../" + relativePath); // от modules/web
        }
        return new String(Files.readAllBytes(f.toPath()));
    }

    private static String extractMethod(String source, String methodName) {
        int start = source.indexOf("void " + methodName + "(");
        if (start < 0) {
            start = source.indexOf("public " + methodName + "(");
        }
        if (start < 0) {
            return "";
        }
        // От начала метода до следующего метода или конца класса
        int brace = source.indexOf("{", start);
        int depth = 0;
        int i = brace;
        for (; i < source.length(); i++) {
            if (source.charAt(i) == '{') depth++;
            if (source.charAt(i) == '}') {
                depth--;
                if (depth == 0) break;
            }
        }
        return source.substring(start, i + 1);
    }

    private static String extractView(String xml, String viewName) {
        int start = xml.indexOf("name=\"" + viewName + "\"");
        if (start < 0) return "";
        // Найти открывающий <view
        int tagStart = xml.lastIndexOf("<view", start);
        int depth = 0;
        int i = tagStart;
        for (; i < xml.length(); i++) {
            if (xml.startsWith("<view", i)) depth++;
            if (xml.startsWith("</view>", i)) {
                depth--;
                if (depth == 0) break;
            }
        }
        return xml.substring(tagStart, i + 8);
    }
}
