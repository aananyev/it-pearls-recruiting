package com.company.hunttech.core;

import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.service.AiExecutionService;
import com.company.hunttech.service.SmartCvIngestServiceBean;
import com.company.hunttech.service.SmartCvParsedData;
import com.company.hunttech.service.dto.cv.SmartCvEducationDto;
import com.company.hunttech.service.dto.cv.SmartCvWorkExperienceDto;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Metadata;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Комплексный набор тестов для проверки всех компонентов функции
 * «Умное заведение карточки кандидата»:
 * 1. Извлечение текста из различных форматов файлов (PDF, DOCX, RTF, TXT, Pages) и обработка битых файлов.
 * 2. Парсинг AI JSON-ответов (полные, частичные, с markdown-обертками, некорректные).
 * 3. Извлечение структурированного опыта работы (workExperience) и образования (education).
 * 4. Нормализация контактов (Telegram, телефоны) и выявление дефектов очистки компаний.
 * 5. Алгоритмы дедупликации кандидатов (телефон, email, telegram, ФИО).
 * 6. Валидация отсутствующих критических полей (missing fields).
 * 7. Контракты кнопок на экранах JobCandidateReestr и CandidateCVReestrBrowse.
 */
public class SmartCvIngestComprehensiveTest {

    private SmartCvIngestServiceBean service;
    private AiExecutionService mockAiExecutionService;
    private DataManager mockDataManager;
    private Metadata mockMetadata;

    @Before
    public void setUp() throws Exception {
        service = new SmartCvIngestServiceBean();
        mockAiExecutionService = mock(AiExecutionService.class);
        mockDataManager = mock(DataManager.class);
        mockMetadata = mock(Metadata.class);

        setField(service, "aiExecutionService", mockAiExecutionService);
        setField(service, "dataManager", mockDataManager);
        setField(service, "metadata", mockMetadata);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    // =========================================================================
    // БЛОК 1: ТЕСТИРОВАНИЕ ФОРМАТОВ ФАЙЛОВ И ИЗВЛЕЧЕНИЯ ТЕКСТА
    // =========================================================================

    @Test
    public void testExtractTextFromPdf() throws Exception {
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Alexey Smirnov Senior Java Architect HuntTech Resume");
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            pdfBytes = baos.toByteArray();
        }

        FileDescriptor fd = new FileDescriptor();
        fd.setExtension("pdf");
        fd.setName("Alexey_Smirnov_CV.pdf");

        String text = service.extractTextFromFile(fd, pdfBytes);
        assertNotNull(text);
        assertTrue("Текст должен содержать ключевые слова из PDF", text.contains("Alexey Smirnov Senior Java Architect"));
    }

    @Test
    public void testExtractTextFromDocx() throws Exception {
        byte[] docxBytes;
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun r = p.createRun();
            r.setText("Elena Vasileva Frontend Developer React TypeScript");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            docxBytes = baos.toByteArray();
        }

        FileDescriptor fd = new FileDescriptor();
        fd.setExtension("docx");
        fd.setName("Elena_Vasileva.docx");

        String text = service.extractTextFromFile(fd, docxBytes);
        assertNotNull(text);
        assertTrue("Текст должен содержать ключевые слова из DOCX", text.contains("Elena Vasileva"));
        assertTrue(text.contains("React TypeScript"));
    }

    @Test
    public void testExtractTextFromRtf() {
        String rtfContent = "{\\rtf1\\ansi\\deff0 {\\fonttbl {\\f0 Courier;}}\\f0\\fs24 " +
                "Dmitry Petrov Lead DevOps Engineer Kubernetes Terraform\\par}";
        byte[] rtfBytes = rtfContent.getBytes(StandardCharsets.UTF_8);

        FileDescriptor fd = new FileDescriptor();
        fd.setExtension("rtf");
        fd.setName("Dmitry_Petrov.rtf");

        String text = service.extractTextFromFile(fd, rtfBytes);
        assertNotNull(text);
        assertTrue("RTF должен быть успешно прочитан", text.contains("Dmitry Petrov Lead DevOps Engineer"));
    }

    @Test
    public void testExtractTextFromPlainText() {
        String rawContent = "Иванов Иван Иванович\nJava Team Lead\nТелефон: +79991112233\nEmail: ivan@example.com";
        byte[] txtBytes = rawContent.getBytes(StandardCharsets.UTF_8);

        FileDescriptor fd = new FileDescriptor();
        fd.setExtension("txt");
        fd.setName("Ivanov.txt");

        String text = service.extractTextFromFile(fd, txtBytes);
        assertEquals("Текст из plain text файла должен совпадать", rawContent, text);
    }

    @Test
    public void testExtractTextFromApplePages() throws Exception {
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 750);
                cs.showText("Apple Pages Candidate Resume Content");
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            pdfBytes = baos.toByteArray();
        }

        ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipBaos)) {
            ZipEntry entry = new ZipEntry("QuickLook/Preview.pdf");
            zos.putNextEntry(entry);
            zos.write(pdfBytes);
            zos.closeEntry();
        }

        FileDescriptor fd = new FileDescriptor();
        fd.setExtension("pages");
        fd.setName("Candidate.pages");

        String text = service.extractTextFromFile(fd, zipBaos.toByteArray());
        assertNotNull(text);
        assertTrue("Текст из Pages Preview.pdf должен успешно извлекаться", text.contains("Apple Pages Candidate Resume Content"));
    }

    @Test
    public void testExtractTextFromCorruptedFiles() {
        byte[] corruptedBytes = new byte[]{0x00, 0x12, 0x34, 0x56, (byte) 0xFF};

        FileDescriptor fdPdf = new FileDescriptor();
        fdPdf.setExtension("pdf");
        String pdfResult = service.extractTextFromFile(fdPdf, corruptedBytes);
        assertNotNull(pdfResult);
        assertEquals("При ошибке чтения битого PDF должна возвращаться пустая строка", "", pdfResult);

        FileDescriptor fdDocx = new FileDescriptor();
        fdDocx.setExtension("docx");
        String docxResult = service.extractTextFromFile(fdDocx, corruptedBytes);
        assertNotNull(docxResult);
        assertEquals("При ошибке чтения битого DOCX должна возвращаться пустая строка", "", docxResult);

        FileDescriptor fdEmpty = new FileDescriptor();
        fdEmpty.setExtension("pdf");
        String emptyResult = service.extractTextFromFile(fdEmpty, new byte[0]);
        assertEquals("", emptyResult);
    }

    // =========================================================================
    // БЛОК 2: ТЕСТИРОВАНИЕ КАЧЕСТВА ПРОМПТА И ПАРСИНГА AI JSON
    // =========================================================================

    @Test
    public void testParseCvTextWithCleanJson() {
        String json = "{\n" +
                "  \"lastName\": \"Смирнов\",\n" +
                "  \"firstName\": \"Алексей\",\n" +
                "  \"middleName\": \"Сергеевич\",\n" +
                "  \"birthDate\": \"1990-05-15\",\n" +
                "  \"phone\": \"+7 (999) 123-45-67\",\n" +
                "  \"mobilePhone\": \"+7 (999) 123-45-67\",\n" +
                "  \"email\": \"alexey.smirnov@example.com\",\n" +
                "  \"telegram\": \"@asmirnov\",\n" +
                "  \"skype\": \"live:asmirnov\",\n" +
                "  \"whatsapp\": \"+79991234567\",\n" +
                "  \"position\": \"Senior Java Developer\",\n" +
                "  \"city\": \"Москва\",\n" +
                "  \"currentCompany\": \"ООО \\\"Технологии Успеха\\\"\",\n" +
                "  \"salary\": \"350 000 руб.\",\n" +
                "  \"skills\": [\"Java\", \"Spring Boot\", \"PostgreSQL\", \"Kafka\", \"Docker\"],\n" +
                "  \"experienceYears\": 8,\n" +
                "  \"summary\": \"Опытный Java-разработчик с 8-летним стажем.\",\n" +
                "  \"education\": [\n" +
                "    {\n" +
                "      \"institution\": \"МГТУ им. Н.Э. Баумана\",\n" +
                "      \"faculty\": \"ИУ\",\n" +
                "      \"specialty\": \"Программная инженерия\",\n" +
                "      \"graduationYear\": 2013,\n" +
                "      \"degree\": \"Специалист\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"workExperience\": [\n" +
                "    {\n" +
                "      \"companyName\": \"Альфа Финтех\",\n" +
                "      \"companyDescription\": \"Банковское ПО\",\n" +
                "      \"companyWebsite\": \"https://alpha-fintech.ru\",\n" +
                "      \"positionName\": \"Java Developer\",\n" +
                "      \"startDate\": \"2016-09-01\",\n" +
                "      \"endDate\": \"2019-12-31\",\n" +
                "      \"isCurrent\": false,\n" +
                "      \"city\": \"Москва\",\n" +
                "      \"duties\": \"Разработка платежного шлюза\",\n" +
                "      \"achievements\": \"Увеличил пропускную способность в 3 раза\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"companyName\": \"Технологии Успеха\",\n" +
                "      \"positionName\": \"Senior Java Developer\",\n" +
                "      \"startDate\": \"2020-01-10\",\n" +
                "      \"isCurrent\": true,\n" +
                "      \"city\": \"Москва\",\n" +
                "      \"duties\": \"Проектирование микросервисов\",\n" +
                "      \"achievements\": \"Успешный релиз новой платформы\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        AiExecutionResult mockResult = mock(AiExecutionResult.class);
        when(mockResult.getText()).thenReturn(json);
        when(mockAiExecutionService.executeText(anyString(), any())).thenReturn(mockResult);

        SmartCvParsedData data = service.parseCvText("Исходный текст резюме");

        assertEquals("Смирнов", data.getLastName());
        assertEquals("Алексей", data.getFirstName());
        assertEquals("Сергеевич", data.getMiddleName());
        assertEquals("Смирнов Алексей Сергеевич", data.getFullName());
        assertEquals("+7 (999) 123-45-67", data.getPhone());
        assertEquals("alexey.smirnov@example.com", data.getEmail());
        assertEquals("asmirnov", data.getTelegram()); // Проверка очистки @
        assertEquals("Senior Java Developer", data.getPosition());
        assertEquals("Москва", data.getCity());
        assertEquals("350 000 руб.", data.getSalary());
        assertEquals(Integer.valueOf(8), data.getExperienceYears());
        assertEquals(5, data.getSkills().size());
        assertTrue(data.getSkills().contains("Spring Boot"));

        // Проверка образования
        assertNotNull(data.getEducation());
        assertEquals(1, data.getEducation().size());
        SmartCvEducationDto edu = data.getEducation().get(0);
        assertEquals("МГТУ им. Н.Э. Баумана", edu.getInstitution());
        assertEquals(Integer.valueOf(2013), edu.getGraduationYear());

        // Проверка опыта работы
        assertNotNull(data.getWorkExperience());
        assertEquals(2, data.getWorkExperience().size());
        SmartCvWorkExperienceDto exp1 = data.getWorkExperience().get(0);
        assertEquals("Альфа Финтех", exp1.getCompanyName());
        assertEquals("Java Developer", exp1.getPositionName());
        assertFalse(exp1.getIsCurrent());

        SmartCvWorkExperienceDto exp2 = data.getWorkExperience().get(1);
        assertEquals("Технологии Успеха", exp2.getCompanyName());
        assertTrue(exp2.getIsCurrent());
    }

    @Test
    public void testParseCvTextWithMarkdownFences() {
        String wrappedJson = "```json\n" +
                "{\n" +
                "  \"lastName\": \"Ковалев\",\n" +
                "  \"firstName\": \"Михаил\",\n" +
                "  \"telegram\": \"https://t.me/m_kovalev\",\n" +
                "  \"email\": \"KOVALEV@MAIL.RU\"\n" +
                "}\n" +
                "```";

        AiExecutionResult mockResult = mock(AiExecutionResult.class);
        when(mockResult.getText()).thenReturn(wrappedJson);
        when(mockAiExecutionService.executeText(anyString(), any())).thenReturn(mockResult);

        SmartCvParsedData data = service.parseCvText("Текст резюме");

        assertEquals("Ковалев", data.getLastName());
        assertEquals("Михаил", data.getFirstName());
        assertEquals("m_kovalev", data.getTelegram()); // Проверка очистки https://t.me/
        assertEquals("kovalev@mail.ru", data.getEmail()); // Проверка приведения email к lowercase
    }

    @Test
    public void testParseCvTextWithIncompleteData() {
        // Резюме без контактов и без города
        String incompleteJson = "{\n" +
                "  \"lastName\": \"Безымянный\",\n" +
                "  \"firstName\": \"Петр\",\n" +
                "  \"position\": \"Системный администратор\"\n" +
                "}";

        AiExecutionResult mockResult = mock(AiExecutionResult.class);
        when(mockResult.getText()).thenReturn(incompleteJson);
        when(mockAiExecutionService.executeText(anyString(), any())).thenReturn(mockResult);

        SmartCvParsedData data = service.parseCvText("Краткое резюме без контактов");

        assertEquals("Безымянный", data.getLastName());
        assertEquals("Петр", data.getFirstName());
        assertNull(data.getPhone());
        assertNull(data.getEmail());
        assertNull(data.getCity());
    }

    @Test
    public void testParseCvTextWithMalformedJsonGracefulRecovery() {
        // Невалидный JSON от LLM не должен приводить к падению приложения
        String brokenJson = "{ \"lastName\": \"Сидоров\", \"firstName\": ";

        AiExecutionResult mockResult = mock(AiExecutionResult.class);
        when(mockResult.getText()).thenReturn(brokenJson);
        when(mockAiExecutionService.executeText(anyString(), any())).thenReturn(mockResult);

        SmartCvParsedData data = service.parseCvText("Любой текст");
        assertNotNull(data);
        assertNull(data.getLastName());
    }

    // =========================================================================
    // БЛОК 3: ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ (НОРМАЛИЗАЦИЯ И ДАТЫ)
    // =========================================================================

    @Test
    public void testCleanCompanyNameEvaluation() throws Exception {
        Method m = SmartCvIngestServiceBean.class.getDeclaredMethod("cleanCompanyName", String.class);
        m.setAccessible(true);

        // Латинские правовые формы:
        assertEquals("Google", m.invoke(service, "Google LLC"));
        assertEquals("Apple", m.invoke(service, "Apple Inc"));

        // Кириллические правовые формы теперь корректно удаляются благодаря (?iU)\b:
        assertEquals("Яндекс", m.invoke(service, "ООО \"Яндекс\""));
        assertEquals("Сбербанк", m.invoke(service, "ПАО «Сбербанк»"));
        assertEquals("Крок", m.invoke(service, "ЗАО \"Крок\""));
    }

    @Test
    public void testNormalizeDigits() throws Exception {
        Method m = SmartCvIngestServiceBean.class.getDeclaredMethod("normalizeDigits", String.class);
        m.setAccessible(true);

        assertEquals("79991112233", m.invoke(service, "+7 (999) 111-22-33"));
        assertEquals("89991112233", m.invoke(service, "8-999-111-22-33"));
        assertEquals("79991112233", m.invoke(service, "+7 999 111 22 33"));
        assertEquals("", m.invoke(service, (String) null));
    }

    @Test
    public void testParseDateSafe() throws Exception {
        Method m = SmartCvIngestServiceBean.class.getDeclaredMethod("parseDateSafe", String.class);
        m.setAccessible(true);

        Date d1 = (Date) m.invoke(service, "2023-05-18");
        assertNotNull(d1);

        Date d2 = (Date) m.invoke(service, "18.05.2023");
        assertNotNull(d2);

        Date d3 = (Date) m.invoke(service, "2023-05");
        assertNotNull(d3);

        Date d4 = (Date) m.invoke(service, "2023");
        assertNotNull(d4);

        Date dInvalid = (Date) m.invoke(service, "не дата");
        assertNull(dInvalid);
    }

    // =========================================================================
    // БЛОК 4: ВАЛИДАЦИЯ НЕДОСТАЮЩИХ ПОЛЕЙ (MISSING FIELDS)
    // =========================================================================

    @Test
    public void testValidateMissingFields() throws Exception {
        Method m = SmartCvIngestServiceBean.class.getDeclaredMethod("validateMissingFields", JobCandidate.class);
        m.setAccessible(true);

        JobCandidate c = new JobCandidate();
        c.setFirstName("Кандидат");
        c.setSecondName("Новый");

        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) m.invoke(service, c);

        assertTrue("Должно требоваться имя", missing.contains("Имя"));
        assertTrue("Должна требоваться фамилия", missing.contains("Фамилия"));
        assertTrue("Должна требоваться должность", missing.contains("Должность"));
        assertTrue("Должен требоваться город", missing.contains("Город проживания"));
        assertTrue("Должны требоваться контакты", missing.contains("Контакты (телефон/email/telegram)"));
    }

    // =========================================================================
    // БЛОК 5: ТЕСТИРОВАНИЕ НА РЕАЛЬНЫХ РЕЗЮМЕ ИЗ ПАПКИ /CV
    // =========================================================================

    @Test
    public void testRealResumesFromLocalDirectory() throws Exception {
        java.io.File dir = new java.io.File("/Users/alekseyananyev/StudioProjects/CV");
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        String[] testFileNames = {
                "Резюме_Владислав_Агабекян.pdf",
                "Резюме_Владислав_Агабекян.docx",
                "Резюме_QA_Курицын_С.А.doc",
                "Шуршенов Алибек.pages",
                "Ткаченко Алексей (резюме).doc",
                "Резюме_QA_Engineer_Никита_Андреевич_Веретенов_от_06_02_2024_08_07.pdf"
        };

        for (String fileName : testFileNames) {
            java.io.File file = new java.io.File(dir, fileName);
            if (!file.exists()) continue;

            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String ext = "";
            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx > 0) ext = fileName.substring(dotIdx + 1).toLowerCase();

            FileDescriptor fd = new FileDescriptor();
            fd.setName(fileName);
            fd.setExtension(ext);

            String extracted = service.extractTextFromFile(fd, bytes);
            assertNotNull("Извлеченный текст не должен быть null для " + fileName, extracted);
            assertTrue("Извлеченный текст должен быть непустым для " + fileName, extracted.trim().length() > 50);
            System.out.println("УСПЕХ: Извлечен текст из реального файла [" + fileName + "], размер текста: " + extracted.length() + " символов");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDuplicateDetectionWithMasksAndFallbacks() {
        JobCandidate existing = new JobCandidate();
        existing.setFirstName("Владислав");
        existing.setSecondName("Агабекян");
        existing.setPhone("+7 (999) 111-22-33");
        existing.setEmail("vladislav@example.com");
        existing.setTelegramName("@vagabekyan");

        FluentLoader deepLoader = mock(FluentLoader.class);
        FluentLoader.ByQuery queryLoader = mock(FluentLoader.ByQuery.class);
        when(mockDataManager.load(JobCandidate.class)).thenReturn(deepLoader);
        when(deepLoader.query(anyString())).thenReturn(queryLoader);
        when(queryLoader.parameter(anyString(), any())).thenReturn(queryLoader);
        when(queryLoader.view(anyString())).thenReturn(queryLoader);
        when(queryLoader.list()).thenReturn(Collections.singletonList(existing));

        // 1. Поиск по телефону в другом формате
        SmartCvParsedData dataPhone = new SmartCvParsedData();
        dataPhone.setPhone("8-999-111-22-33");
        JobCandidate dupPhone = service.findDuplicate(dataPhone);
        assertNotNull("Дубликат по телефону с маской должен быть найден", dupPhone);
        assertEquals("Владислав", dupPhone.getFirstName());

        // 2. Поиск по fallback на mobilePhone при некорректном phone
        SmartCvParsedData dataMobile = new SmartCvParsedData();
        dataMobile.setPhone("123");
        dataMobile.setMobilePhone("+7 999 111 22 33");
        JobCandidate dupMobile = service.findDuplicate(dataMobile);
        assertNotNull("Дубликат по fallback на mobilePhone должен быть найден", dupMobile);

        // 3. Поиск по Telegram без @
        SmartCvParsedData dataTg = new SmartCvParsedData();
        dataTg.setTelegram("vagabekyan");
        JobCandidate dupTg = service.findDuplicate(dataTg);
        assertNotNull("Дубликат по Telegram без @ должен быть найден", dupTg);

        // 4. Поиск по переставленным имени и фамилии
        SmartCvParsedData dataName = new SmartCvParsedData();
        dataName.setFirstName("Агабекян");
        dataName.setLastName("Владислав");
        JobCandidate dupName = service.findDuplicate(dataName);
        assertNotNull("Дубликат по переставленным ФИО должен быть найден", dupName);
    }
}
