package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.City;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.service.SmartCvParsedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты логики сопоставления контактных данных и нормализации для умного сканирования резюме.
 */
class SmartCvScanHelperTest {

    private JobCandidate candidate;
    private SmartCvParsedData parsedData;

    @BeforeEach
    void setUp() {
        candidate = new JobCandidate();
        candidate.setSecondName("Иванов");
        candidate.setFirstName("Иван");
        candidate.setMiddleName("Иванович");
        candidate.setPhone("+7 (900) 111-22-33");
        candidate.setMobilePhone("+7 (900) 111-22-34");
        candidate.setEmail("ivanov@example.com");
        candidate.setTelegramName("ivanov_tg");
        candidate.setWhatsupName("+79001112233");
        candidate.setSkypeName("ivanov_skype");

        City city = new City();
        city.setCityRuName("Москва");
        candidate.setCityOfResidence(city);

        parsedData = new SmartCvParsedData();
    }

    @Test
    @DisplayName("Сценарий 4: Все данные совпадают с учетом нормализации -> статус MATCH, чекбокс снят")
    void testAllFieldsMatch() {
        parsedData.setLastName("Иванов");
        parsedData.setFirstName("иван"); // регистронезависимо
        parsedData.setMiddleName("Иванович");
        parsedData.setPhone("8 (900) 111-22-33"); // другой формат номера
        parsedData.setMobilePhone("+7 900 111 22 34");
        parsedData.setEmail("IVANOV@EXAMPLE.COM"); // регистронезависимо
        parsedData.setTelegram("@ivanov_tg"); // с собачкой
        parsedData.setWhatsapp("+7 900 111-22-33");
        parsedData.setSkype("ivanov_skype");
        parsedData.setCity("москва");

        List<CvScanFieldComparison> comparisons = SmartCvScanHelper.buildComparisons(candidate, parsedData);
        Map<String, CvScanFieldComparison> map = comparisons.stream()
                .collect(Collectors.toMap(CvScanFieldComparison::getFieldId, c -> c));

        assertEquals(CvScanFieldComparison.Status.MATCH, map.get("secondName").getStatus());
        assertFalse(map.get("secondName").isSelected());

        assertEquals(CvScanFieldComparison.Status.MATCH, map.get("firstName").getStatus());
        assertFalse(map.get("firstName").isSelected());

        assertEquals(CvScanFieldComparison.Status.MATCH, map.get("phone").getStatus());
        assertFalse(map.get("phone").isSelected());

        assertEquals(CvScanFieldComparison.Status.MATCH, map.get("email").getStatus());
        assertFalse(map.get("email").isSelected());

        assertEquals(CvScanFieldComparison.Status.MATCH, map.get("telegramName").getStatus());
        assertFalse(map.get("telegramName").isSelected());

        assertEquals(CvScanFieldComparison.Status.MATCH, map.get("cityOfResidence").getStatus());
        assertFalse(map.get("cityOfResidence").isSelected());
    }

    @Test
    @DisplayName("Сценарий 3: В резюме найден новый телефон -> статус REPLACE, чекбокс выбран")
    void testDifferentPhoneFound() {
        parsedData.setPhone("+7 927 555-66-77"); // Новый телефон
        parsedData.setEmail("ivanov@example.com"); // Совпадает

        List<CvScanFieldComparison> comparisons = SmartCvScanHelper.buildComparisons(candidate, parsedData);
        Map<String, CvScanFieldComparison> map = comparisons.stream()
                .collect(Collectors.toMap(CvScanFieldComparison::getFieldId, c -> c));

        CvScanFieldComparison phoneComp = map.get("phone");
        assertEquals(CvScanFieldComparison.Status.REPLACE, phoneComp.getStatus());
        assertTrue(phoneComp.isSelected(), "Для измененного поля чекбокс должен быть выбран по умолчанию");
        assertTrue(phoneComp.isEnabled());
        assertEquals("+7 927 555-66-77", phoneComp.getFoundValue());

        CvScanFieldComparison emailComp = map.get("email");
        assertEquals(CvScanFieldComparison.Status.MATCH, emailComp.getStatus());
        assertFalse(emailComp.isSelected(), "Для совпадающего поля чекбокс не должен быть выбран");
    }

    @Test
    @DisplayName("Сценарий нового значения: в карточке поле пустое, в резюме найдено -> статус NEW, чекбокс выбран")
    void testNewFieldFound() {
        candidate.setTelegramName(null);
        candidate.setCityOfResidence(null);

        parsedData.setTelegram("https://t.me/new_candidate");
        parsedData.setCity("Санкт-Петербург");

        List<CvScanFieldComparison> comparisons = SmartCvScanHelper.buildComparisons(candidate, parsedData);
        Map<String, CvScanFieldComparison> map = comparisons.stream()
                .collect(Collectors.toMap(CvScanFieldComparison::getFieldId, c -> c));

        CvScanFieldComparison tgComp = map.get("telegramName");
        assertEquals(CvScanFieldComparison.Status.NEW, tgComp.getStatus());
        assertTrue(tgComp.isSelected());
        assertTrue(tgComp.isEnabled());
        assertEquals("—", tgComp.getCurrentValue());

        CvScanFieldComparison cityComp = map.get("cityOfResidence");
        assertEquals(CvScanFieldComparison.Status.NEW, cityComp.getStatus());
        assertTrue(cityComp.isSelected());
        assertEquals("Санкт-Петербург", cityComp.getFoundValue());
    }

    @Test
    @DisplayName("Сценарий 5: В резюме нет контактных данных -> статус EMPTY, чекбокс disabled")
    void testEmptyResumeData() {
        List<CvScanFieldComparison> comparisons = SmartCvScanHelper.buildComparisons(candidate, parsedData);
        for (CvScanFieldComparison comp : comparisons) {
            assertEquals(CvScanFieldComparison.Status.EMPTY, comp.getStatus());
            assertFalse(comp.isSelected(), "Пустые поля не должны выбираться");
            assertFalse(comp.isEnabled(), "Пустые поля должны быть disabled для защиты от перезаписи");
            assertEquals("—", comp.getFoundValue());
        }
    }

    @Test
    @DisplayName("Нормализация Telegram: ссылки t.me/, префикс @ и пробелы")
    void testTelegramNormalization() {
        assertEquals("candidate", SmartCvScanHelper.normalizeTelegram("@candidate"));
        assertEquals("candidate", SmartCvScanHelper.normalizeTelegram("https://t.me/candidate"));
        assertEquals("candidate", SmartCvScanHelper.normalizeTelegram("http://t.me/candidate/"));
        assertEquals("candidate", SmartCvScanHelper.normalizeTelegram("t.me/candidate"));
        assertEquals("candidate", SmartCvScanHelper.normalizeTelegram("  @Candidate  "));

        assertTrue(SmartCvScanHelper.telegramsMatch("candidate", "https://t.me/candidate"));
        assertTrue(SmartCvScanHelper.telegramsMatch("@candidate", "candidate"));
        assertFalse(SmartCvScanHelper.telegramsMatch("cand1", "cand2"));
    }

    @Test
    @DisplayName("Нормализация номеров телефонов: форматы +7, 8, скобки, тире")
    void testPhoneNormalization() {
        assertTrue(SmartCvScanHelper.phonesMatch("+7 (999) 123-45-67", "8 999 123 45 67"));
        assertTrue(SmartCvScanHelper.phonesMatch("79991234567", "+7(999)1234567"));
        assertFalse(SmartCvScanHelper.phonesMatch("+7 999 111-22-33", "+7 999 444-55-66"));
    }
}
