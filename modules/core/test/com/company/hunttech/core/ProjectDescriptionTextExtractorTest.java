package com.company.hunttech.core;

import com.company.hunttech.ai.ProjectDescriptionTextExtractor;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Unit contract извлечения Project description из upload без FileStorage. */
public class ProjectDescriptionTextExtractorTest {

    @Test
    public void utf8TxtIsExtracted() throws Exception {
        String value = "Проект платежной платформы\nJava, PostgreSQL";
        String result = ProjectDescriptionTextExtractor.extract(
                new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)), "txt");

        assertEquals(value, result);
    }

    @Test
    public void docxDocumentXmlIsExtracted() throws Exception {
        String documentXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body><w:p><w:r><w:t>Первый абзац</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t>Java PostgreSQL</w:t></w:r></w:p></w:body></w:document>";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        String result = ProjectDescriptionTextExtractor.extract(
                new ByteArrayInputStream(bytes.toByteArray()), ".docx");

        assertTrue(result.contains("Первый абзац"));
        assertTrue(result.contains("Java PostgreSQL"));
    }

    @Test(expected = IOException.class)
    public void legacyDocIsRejectedExplicitly() throws Exception {
        ProjectDescriptionTextExtractor.extract(
                new ByteArrayInputStream(new byte[]{1, 2, 3}), "doc");
    }
}
