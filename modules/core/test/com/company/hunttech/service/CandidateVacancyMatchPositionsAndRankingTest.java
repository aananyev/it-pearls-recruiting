package com.company.hunttech.service;

import com.company.hunttech.dto.CandidateVacancyMatchReport;
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Position;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FluentLoader;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты алгоритма подбора кандидатов по вакансии (BL-2026-042):
 * - Определение должностей из карты поиска и сопоставление со справочником Position;
 * - Источники последнего резюме (файл -> textCV -> чек-лист -> сопроводительное письмо);
 * - Актуальность взаимодействий и приоритизация.
 */
public class CandidateVacancyMatchPositionsAndRankingTest {

    private CandidateVacancyMatchAiServiceBean service;
    private DataManager dataManager;
    private AiExecutionService aiExecutionService;
    private FileLoader fileLoader;

    @Before
    public void setUp() {
        service = new CandidateVacancyMatchAiServiceBean();
        dataManager = mock(DataManager.class);
        aiExecutionService = mock(AiExecutionService.class);
        fileLoader = mock(FileLoader.class);

        ReflectionTestUtils.setField(service, "dataManager", dataManager);
        ReflectionTestUtils.setField(service, "aiExecutionService", aiExecutionService);
        ReflectionTestUtils.setField(service, "fileLoader", fileLoader);
    }

    @Test
    public void testExtractPositionsFromSearchMap() throws Exception {
        Method method = CandidateVacancyMatchAiServiceBean.class
                .getDeclaredMethod("extractPositionsFromSearchMap", String.class, Set.class);
        method.setAccessible(true);

        String searchMap = "<p><strong>Карта поиска для рекрутера:</strong> Java Developer (УТОЧНИТЬ)</p>\n" +
                "<p><strong>Должность:</strong> Backend Инженер</p>\n" +
                "<p><strong>Альтернативные названия должностей:</strong></p>\n" +
                "<ul>\n" +
                "<li>Spring Boot Developer</li>\n" +
                "<li>Java Software Engineer</li>\n" +
                "</ul>\n" +
                "<p><strong>Компании-доноры:</strong> Сбер, Тинькофф</p>";

        Set<String> titles = new LinkedHashSet<>();
        method.invoke(service, searchMap, titles);

        assertTrue("Должна быть извлечена основная должность", titles.stream().anyMatch(t -> t.equalsIgnoreCase("Java Developer")));
        assertTrue("Должна быть извлечена должность из секции 'Должность'", titles.stream().anyMatch(t -> t.equalsIgnoreCase("Backend Инженер")));
        assertTrue("Должна быть извлечена альтернативная должность", titles.stream().anyMatch(t -> t.contains("Spring Boot Developer")));
        assertTrue("Должна быть извлечена альтернативная должность", titles.stream().anyMatch(t -> t.contains("Java Software Engineer")));
        assertFalse("Компании-доноры не должны попадать в должности", titles.stream().anyMatch(t -> t.toLowerCase().contains("сбер")));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testMatchExactPositionInDictionary() throws Exception {
        Method method = CandidateVacancyMatchAiServiceBean.class
                .getDeclaredMethod("matchExactPositionInDictionary", String.class);
        method.setAccessible(true);

        Position dictPos = new Position();
        dictPos.setPositionRuName("Java-разработчик");
        dictPos.setPositionEnName("Java Developer");

        FluentLoader loader = mock(FluentLoader.class);
        FluentLoader.ByQuery query = mock(FluentLoader.ByQuery.class);
        when(dataManager.load(Position.class)).thenReturn(loader);
        when(loader.query(anyString())).thenReturn(query);
        when(query.parameter(anyString(), any())).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(dictPos));

        Position result = (Position) method.invoke(service, "Senior Java Developer");
        assertNotNull("Должно быть найдено сопоставление со справочником после очистки грейда Senior", result);
        assertEquals("Java-разработчик", result.getPositionRuName());
    }

    @Test
    public void testResumeExtractionOrder() throws Exception {
        Method method = CandidateVacancyMatchAiServiceBean.class
                .getDeclaredMethod("buildCandidateResumeText", List.class);
        method.setAccessible(true);

        CandidateCV cv = new CandidateCV();
        cv.setTextCV("Текст из textCV");
        cv.setCommentLetter("Заметки и чек-лист рекрутера");
        cv.setLetter("Сопроводительное письмо кандидата");

        // 1. Проверяем textCV + чек-лист + сопроводительное письмо
        String textWithoutFile = (String) method.invoke(service, Collections.singletonList(cv));
        assertTrue(textWithoutFile.contains("Текст из textCV"));
        assertTrue(textWithoutFile.contains("ЧЕК-ЛИСТ И ЗАМЕТКИ РЕКРУТЕРА"));
        assertTrue(textWithoutFile.contains("Заметки и чек-лист рекрутера"));
        assertTrue(textWithoutFile.contains("СОПРОВОДИТЕЛЬНОЕ ПИСЬМО"));
        assertTrue(textWithoutFile.contains("Сопроводительное письмо кандидата"));

        // 2. Добавляем оригинальный файл - он должен иметь высший приоритет над textCV
        FileDescriptor fd = new FileDescriptor();
        fd.setExtension("txt");
        cv.setOriginalFileCV(fd);

        when(fileLoader.openStream(fd))
                .thenReturn(new ByteArrayInputStream("Текст из оригинального файла резюме".getBytes(StandardCharsets.UTF_8)));

        String textWithFile = (String) method.invoke(service, Collections.singletonList(cv));
        assertTrue(textWithFile.contains("Текст из оригинального файла резюме"));
        assertFalse("При наличии оригинального файла textCV не должен дублироваться в качестве основного текста",
                textWithFile.contains("Текст из textCV"));
        assertTrue("Чек-лист должен дополнять информацию", textWithFile.contains("Заметки и чек-лист рекрутера"));
    }

    @Test
    public void testRecruiterActivityRecencyDescription() throws Exception {
        Method method = CandidateVacancyMatchAiServiceBean.class
                .getDeclaredMethod("resolveRecruiterActivityDescription", UUID.class);
        method.setAccessible(true);

        UUID candidateId = UUID.randomUUID();
        com.haulmont.cuba.core.global.FluentValueLoader valueLoader = mock(com.haulmont.cuba.core.global.FluentValueLoader.class);
        when(dataManager.loadValue(anyString(), eq(Date.class))).thenReturn(valueLoader);
        when(valueLoader.parameter(eq("candId"), eq(candidateId))).thenReturn(valueLoader);

        // Дата контакта: 10 дней назад
        Date recentDate = new Date(System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000);
        when(valueLoader.list()).thenReturn(Collections.singletonList(recentDate));

        String desc = (String) method.invoke(service, candidateId);
        assertTrue("Должна быть зафиксирована наивысшая актуальность взаимодействия",
                desc.contains("наивысшая актуальность") && desc.contains("последнего месяца"));

        // Дата контакта: 2 года назад
        Date oldDate = new Date(System.currentTimeMillis() - 800L * 24 * 60 * 60 * 1000);
        when(valueLoader.list()).thenReturn(Collections.singletonList(oldDate));

        String oldDesc = (String) method.invoke(service, candidateId);
        assertTrue("Должен быть зафиксирован низкий приоритет взаимодействия для контакта 2+ лет назад",
                oldDesc.contains("низкий приоритет") || oldDesc.contains("лет назад"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testVacancyMatchHandlesClosedVacancySafely() {
        UUID vacancyId = UUID.randomUUID();
        OpenPosition vacancy = new OpenPosition();
        vacancy.setId(vacancyId);
        vacancy.setOpenClose(true); // Закрыта

        FluentLoader loader = mock(FluentLoader.class);
        FluentLoader.ById byId = mock(FluentLoader.ById.class);
        when(dataManager.load(OpenPosition.class)).thenReturn(loader);
        when(loader.id(vacancyId)).thenReturn(byId);
        when(byId.view(any(java.util.function.Consumer.class))).thenReturn(byId);
        when(byId.optional()).thenReturn(Optional.of(vacancy));

        CandidateVacancyMatchReport report = service.matchCandidatesForVacancy(vacancyId);
        assertNotNull(report);
        assertFalse(report.isSuccess());
        assertTrue(report.getStatusMessage().contains("закрыта") || report.getStatusMessage().contains("openClose"));
    }
}
