package com.company.hunttech.web.screens.candidatecv;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Регрессионный тест: защита от unfetched fileImageFace и textCV в CandidateCVEdit.
 */
public class CandidateCVEditRegressionTest {

    private final String source;

    public CandidateCVEditRegressionTest() throws Exception {
        this.source = readSource("modules/web/src/com/company/hunttech/web/screens/candidatecv/CandidateCVEdit.java");
    }

    // ── fileImageFace ──

    @Test
    public void onBeforeShow_doesNotContain_setCandidatePicImage() {
        String beforeShow = extractMethod(source, "onBeforeShow");
        assertFalse("onBeforeShow НЕ должен вызывать setCandidatePicImage()",
                beforeShow.contains("setCandidatePicImage()"));
    }

    @Test
    public void onAfterShow2_contains_setCandidatePicImage() {
        String afterShow = extractMethod(source, "onAfterShow2");
        assertTrue("onAfterShow2 должен вызывать setCandidatePicImage()",
                afterShow.contains("setCandidatePicImage()"));
    }

    @Test
    public void setCandidatePicImage_hasPersistenceGuard() {
        String method = extractMethod(source, "setCandidatePicImage");
        assertTrue("setCandidatePicImage должен проверять PersistenceHelper.isLoaded",
                method.contains("PersistenceHelper.isLoaded"));
    }

    @Test
    public void persistenceGuard_isBefore_getFileImageFace() {
        String method = extractMethod(source, "setCandidatePicImage");
        int checkIdx = method.indexOf("PersistenceHelper.isLoaded");
        int getterIdx = method.indexOf(".getFileImageFace()");
        assertTrue("isLoaded ДО getFileImageFace()", checkIdx > 0 && checkIdx < getterIdx);
    }

    // ── textCV ──

    @Test
    public void onBeforeShow_doesNotContain_getTextCV_withoutGuard() {
        String beforeShow = extractMethod(source, "onBeforeShow");
        // Должна быть проверка isLoaded("textCV") перед getTextCV()
        assertTrue("onBeforeShow должен проверять PersistenceHelper.isLoaded для textCV",
                beforeShow.contains("PersistenceHelper.isLoaded") && beforeShow.contains("textCV"));
    }

    // ── views ──

    @Test
    public void browseView_doesNotContain_fileImageFace() throws Exception {
        String viewsXml = readSource("modules/global/src/com/company/hunttech/views.xml");
        String browseView = extractView(viewsXml, "candidateCV-browse-view");
        assertFalse("candidateCV-browse-view НЕ должен содержать fileImageFace",
                browseView.contains("fileImageFace"));
    }

    @Test
    public void browseView_doesNotContain_textCV() throws Exception {
        String viewsXml = readSource("modules/global/src/com/company/hunttech/views.xml");
        String browseView = extractView(viewsXml, "candidateCV-browse-view");
        assertFalse("candidateCV-browse-view НЕ должен содержать textCV",
                browseView.contains("textCV"));
    }

    @Test
    public void editorView_contains_fileImageFace() throws Exception {
        String viewsXml = readSource("modules/global/src/com/company/hunttech/views.xml");
        String editView = extractView(viewsXml, "candidateCV-view");
        assertTrue("candidateCV-view должен содержать fileImageFace",
                editView.contains("fileImageFace"));
    }

    // ── helpers ──

    private static String readSource(String relativePath) throws Exception {
        String base = System.getProperty("user.dir");
        if (!new File(base, relativePath).exists()) {
            base = new File(base).getParent();
        }
        File f = new File(base, relativePath);
        if (!f.exists()) {
            f = new File("../../" + relativePath);
        }
        return new String(Files.readAllBytes(f.toPath()));
    }

    private static String extractMethod(String source, String methodName) {
        int start = source.indexOf("void " + methodName + "(");
        if (start < 0) start = source.indexOf("public " + methodName + "(");
        if (start < 0) return "";
        int brace = source.indexOf("{", start);
        int depth = 0;
        int i = brace;
        for (; i < source.length(); i++) {
            if (source.charAt(i) == '{') depth++;
            if (source.charAt(i) == '}') { depth--; if (depth == 0) break; }
        }
        return source.substring(start, i + 1);
    }

    private static String extractView(String xml, String viewName) {
        int start = xml.indexOf("name=\"" + viewName + "\"");
        if (start < 0) return "";
        int tagStart = xml.lastIndexOf("<view", start);
        int depth = 0;
        int i = tagStart;
        for (; i < xml.length(); i++) {
            if (xml.startsWith("<view", i)) depth++;
            if (xml.startsWith("</view>", i)) { depth--; if (depth == 0) break; }
        }
        return xml.substring(tagStart, i + 8);
    }
}
