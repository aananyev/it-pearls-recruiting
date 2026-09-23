package com.company.hunttech.core;

import com.company.hunttech.config.HunttechContactEnrichmentConfig;
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.service.CandidateContactEnrichmentService;
import com.company.hunttech.service.CandidateContactEnrichmentServiceBean;
import com.company.hunttech.service.CandidateContactsEnrichmentWorker;
import com.company.hunttech.service.dto.CandidateExtractedContactsDto;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Тесты сервиса «Фоновое определение контактов кандидата» (BL-2026-004):
 * приоритет очереди, хеширование, парсинг JSON и фильтрация логотипов.
 */
public class CandidateContactsPriorityQueueTest {

    private CandidateContactEnrichmentServiceBean service;
    private DataManager mockDataManager;
    private Metadata mockMetadata;
    private Configuration mockConfiguration;
    private HunttechContactEnrichmentConfig mockConfig;
    private CandidateContactsEnrichmentWorker mockWorker;

    @Before
    public void setUp() throws Exception {
        service = new CandidateContactEnrichmentServiceBean();
        mockDataManager = mock(DataManager.class, Mockito.RETURNS_DEEP_STUBS);
        mockMetadata = mock(Metadata.class);
        mockConfiguration = mock(Configuration.class);
        mockConfig = mock(HunttechContactEnrichmentConfig.class);
        mockWorker = mock(CandidateContactsEnrichmentWorker.class);

        when(mockConfiguration.getConfig(HunttechContactEnrichmentConfig.class)).thenReturn(mockConfig);
        when(mockConfig.getMaxRetries()).thenReturn(4);
        when(mockConfig.getDelayBetweenRequestsSec()).thenReturn(15);
        when(mockConfig.getEnabled()).thenReturn(true);
        when(mockConfig.getAiFunctionCode()).thenReturn("CONTACTS_EXTRACT_BACKGROUND");

        injectField(service, "dataManager", mockDataManager);
        injectField(service, "metadata", mockMetadata);
        injectField(service, "configuration", mockConfiguration);
        injectField(service, "enrichmentWorker", mockWorker);
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    public void testNormalizedCvHash() {
        String text1 = "Иван Иванов  \n\t Телефон: +7 999 123-45-67   Email: ivan@example.com  ";
        String text2 = "иван иванов телефон: +7 999 123-45-67 email: ivan@example.com";

        String hash1 = service.calculateNormalizedCvHash(text1);
        String hash2 = service.calculateNormalizedCvHash(text2);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals("Хеши нормализованного текста должны совпадать независимо от регистра и пробелов", hash1, hash2);
    }

    @Test
    public void testParseContactsJson() throws Exception {
        String json = "```json\n" +
                "{\n" +
                "  \"phone\": \"+7 999 111-22-33\",\n" +
                "  \"mobilePhone\": \"+7 999 111-22-33\",\n" +
                "  \"email\": \"test.candidate@gmail.com\",\n" +
                "  \"telegramName\": \"@candidate_dev\",\n" +
                "  \"skypeName\": \"live:candidate\",\n" +
                "  \"city\": \"Москва\"\n" +
                "}\n" +
                "```";

        Method parseMethod = CandidateContactEnrichmentServiceBean.class.getDeclaredMethod("parseContactsJson", String.class);
        parseMethod.setAccessible(true);

        CandidateExtractedContactsDto dto = (CandidateExtractedContactsDto) parseMethod.invoke(service, json);

        assertNotNull(dto);
        assertEquals("+7 999 111-22-33", dto.getPhone());
        assertEquals("test.candidate@gmail.com", dto.getEmail());
        assertEquals("candidate_dev", dto.getTelegramName()); // символ @ должен быть удален
        assertEquals("live:candidate", dto.getSkypeName());
        assertEquals("Москва", dto.getCity());
        assertTrue(dto.hasAnyContact());
    }

    @Test
    public void testDetectCandidatePhotoFiltersOutLogo() throws Exception {
        // Создаем логотип (вытянутый баннер 250x40 - типичный hh.ru логотип)
        BufferedImage logoImg = new BufferedImage(250, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = logoImg.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 250, 40);
        g.dispose();

        ByteArrayOutputStream baosLogo = new ByteArrayOutputStream();
        ImageIO.write(logoImg, "png", baosLogo);
        byte[] logoBytes = baosLogo.toByteArray();

        // Создаем портретную фотографию (200x250 - портретное фото)
        BufferedImage portraitImg = new BufferedImage(200, 250, BufferedImage.TYPE_INT_RGB);
        Graphics2D gp = portraitImg.createGraphics();
        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 250; y++) {
                portraitImg.setRGB(x, y, new Color((x * 13) % 255, (y * 7) % 255, ((x + y) * 5) % 255).getRGB());
            }
        }
        gp.dispose();

        ByteArrayOutputStream baosPortrait = new ByteArrayOutputStream();
        ImageIO.write(portraitImg, "png", baosPortrait);
        byte[] portraitBytes = baosPortrait.toByteArray();

        Method detectMethod = CandidateContactEnrichmentServiceBean.class.getDeclaredMethod("detectCandidatePhoto", List.class);
        detectMethod.setAccessible(true);

        // 1. Проверяем список только с логотипом -> должен вернуть null (отсечен)
        byte[] detectedFromLogo = (byte[]) detectMethod.invoke(service, Collections.singletonList(logoBytes));
        assertNull("Логотип с соотношением сторон > 1.35 или малой высотой должен быть отсечен", detectedFromLogo);

        // 2. Проверяем список с логотипом и портретным фото -> должен выбрать портретное фото
        byte[] detectedPhoto = (byte[]) detectMethod.invoke(service, java.util.Arrays.asList(logoBytes, portraitBytes));
        assertNotNull("Портретное фото должно быть успешно определено", detectedPhoto);
        assertArrayEquals("Должно быть выбрано именно портретное фото, а не логотип", portraitBytes, detectedPhoto);
    }
}
