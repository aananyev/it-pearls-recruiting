package com.company.hunttech.ai;

import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Извлекает обычный текст из файлов описания проекта без сохранения документа
 * как бизнес-сущности. Поддерживаемый контракт upload: PDF, DOCX и TXT.
 */
public final class ProjectDescriptionTextExtractor {
    private static final Charset WINDOWS_1251 = Charset.forName("windows-1251");

    private ProjectDescriptionTextExtractor() {
    }

    public static String extract(InputStream inputStream, String extension) throws IOException {
        if (inputStream == null) {
            throw new IOException("Поток загруженного файла отсутствует.");
        }
        String normalizedExtension = normalizeExtension(extension);
        String text;
        switch (normalizedExtension) {
            case "pdf":
                text = extractPdf(inputStream);
                break;
            case "docx":
                text = extractDocx(inputStream);
                break;
            case "txt":
                text = extractTxt(inputStream);
                break;
            default:
                throw new IOException("Неподдерживаемый формат описания проекта: " + normalizedExtension);
        }
        String normalizedText = normalizeText(text);
        if (normalizedText.isEmpty()) {
            throw new IOException("В загруженном файле не найден текст описания проекта.");
        }
        return normalizedText;
    }

    private static String extractPdf(InputStream inputStream) throws IOException {
        // PDFBox 3.0-alpha в проекте использует RandomAccessReadBuffer; сохраняем
        // тот же совместимый API-паттерн, что и существующие HRM HuntTech parser'ы.
        RandomAccessRead randomAccessRead = new RandomAccessReadBuffer(inputStream);
        PDFParser parser = new PDFParser(randomAccessRead);
        try (PDDocument document = parser.parse()) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String extractTxt(InputStream inputStream) throws IOException {
        byte[] bytes = readAllBytes(inputStream);
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        // Старые описания HRM HuntTech встречаются в Windows-1251. Если UTF-8
        // декодирование дало replacement char, используем совместимый fallback.
        return utf8.indexOf('\uFFFD') >= 0 ? new String(bytes, WINDOWS_1251) : utf8;
    }

    private static String extractDocx(InputStream inputStream) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return extractWordDocumentXml(zip);
                }
            }
        }
        throw new IOException("DOCX не содержит word/document.xml.");
    }

    /**
     * DOCX читается стандартным StAX: так не требуется отдельный тяжёлый parser,
     * а DTD/external entities отключены, чтобы upload не открывал XML entity path.
     */
    private static String extractWordDocumentXml(InputStream inputStream) throws IOException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        trySetProperty(factory, XMLInputFactory.SUPPORT_DTD, false);
        trySetProperty(factory, "javax.xml.stream.isSupportingExternalEntities", false);

        StringBuilder result = new StringBuilder();
        XMLStreamReader reader = null;
        try {
            reader = factory.createXMLStreamReader(inputStream, StandardCharsets.UTF_8.name());
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    if ("t".equals(localName)) {
                        result.append(reader.getElementText());
                    } else if ("tab".equals(localName)) {
                        result.append('\t');
                    } else if ("br".equals(localName) || "cr".equals(localName)) {
                        result.append('\n');
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT
                        && "p".equals(reader.getLocalName())) {
                    result.append('\n');
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException("Не удалось прочитать структуру DOCX.", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException ignored) {
                    // Поток закрывает ZipInputStream; ошибка close reader не меняет результат парсинга.
                }
            }
        }
        return result.toString();
    }

    private static void trySetProperty(XMLInputFactory factory, String name, Object value) {
        try {
            factory.setProperty(name, value);
        } catch (IllegalArgumentException ignored) {
            // Реализации StAX могут не поддерживать опциональные свойства безопасности.
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static String normalizeExtension(String extension) {
        String value = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
        return value.startsWith(".") ? value.substring(1) : value;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u0000', ' ')
                .trim();
    }
}
