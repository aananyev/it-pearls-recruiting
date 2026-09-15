package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.service.SmartCvParsedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилитный класс для анализа и сопоставления контактных данных кандидата с данными из резюме.
 */
public class SmartCvScanHelper {

    /**
     * Очистка телефонного номера до цифр и извлечение значимой части (последние 10 цифр).
     */
    public static String normalizePhoneDigits(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 11 && (digits.startsWith("7") || digits.startsWith("8"))) {
            return digits.substring(1);
        }
        return digits;
    }

    /**
     * Проверка совпадения двух телефонных номеров с учетом региональных форматов.
     */
    public static boolean phonesMatch(String p1, String p2) {
        if (p1 == null && p2 == null) return true;
        if (p1 == null || p2 == null) return false;
        String d1 = normalizePhoneDigits(p1);
        String d2 = normalizePhoneDigits(p2);
        if (d1.isEmpty() && d2.isEmpty()) {
            return p1.trim().equalsIgnoreCase(p2.trim());
        }
        return d1.equals(d2);
    }

    /**
     * Очистка Telegram никнейма от url, @ и слешей.
     */
    public static String normalizeTelegram(String tg) {
        if (tg == null) return null;
        String clean = tg.trim();
        clean = clean.replaceFirst("^(https?://)?(www\\.)?t\\.me/", "");
        clean = clean.replaceFirst("^@", "");
        clean = clean.replaceAll("[/\\s]", "");
        return clean.toLowerCase();
    }

    /**
     * Проверка совпадения Telegram с учетом никнейма и ссылок.
     */
    public static boolean telegramsMatch(String t1, String t2) {
        if (t1 == null && t2 == null) return true;
        if (t1 == null || t2 == null) return false;
        String n1 = normalizeTelegram(t1);
        String n2 = normalizeTelegram(t2);
        if (n1.isEmpty() && n2.isEmpty()) {
            return t1.trim().equalsIgnoreCase(t2.trim());
        }
        return n1.equalsIgnoreCase(n2);
    }

    /**
     * Проверка совпадения Email без учета регистра.
     */
    public static boolean emailsMatch(String e1, String e2) {
        if (e1 == null && e2 == null) return true;
        if (e1 == null || e2 == null) return false;
        return e1.trim().equalsIgnoreCase(e2.trim());
    }

    /**
     * Построение списка сопоставлений полей кандидата и данных из резюме.
     */
    public static List<CvScanFieldComparison> buildComparisons(JobCandidate candidate, SmartCvParsedData data) {
        List<CvScanFieldComparison> result = new ArrayList<>();
        if (data == null) {
            return result;
        }

        // 1. Фамилия (secondName)
        result.add(createGeneralComparison(
                "secondName",
                "Фамилия",
                candidate != null ? candidate.getSecondName() : null,
                data.getLastName()
        ));

        // 2. Имя (firstName)
        result.add(createGeneralComparison(
                "firstName",
                "Имя",
                candidate != null ? candidate.getFirstName() : null,
                data.getFirstName()
        ));

        // 3. Отчество (middleName)
        result.add(createGeneralComparison(
                "middleName",
                "Отчество",
                candidate != null ? candidate.getMiddleName() : null,
                data.getMiddleName()
        ));

        // 4. Телефон (phone)
        result.add(createPhoneComparison(
                "phone",
                "Телефон",
                candidate != null ? candidate.getPhone() : null,
                data.getPhone()
        ));

        // 5. Мобильный телефон (mobilePhone)
        result.add(createPhoneComparison(
                "mobilePhone",
                "Мобильный телефон",
                candidate != null ? candidate.getMobilePhone() : null,
                data.getMobilePhone()
        ));

        // 6. Email
        result.add(createEmailComparison(
                "email",
                "Email",
                candidate != null ? candidate.getEmail() : null,
                data.getEmail()
        ));

        // 7. Telegram
        result.add(createTelegramComparison(
                "telegramName",
                "Telegram",
                candidate != null ? candidate.getTelegramName() : null,
                data.getTelegram()
        ));

        // 8. WhatsApp
        result.add(createGeneralComparison(
                "whatsupName",
                "WhatsApp",
                candidate != null ? candidate.getWhatsupName() : null,
                data.getWhatsapp()
        ));

        // 9. Skype
        result.add(createGeneralComparison(
                "skypeName",
                "Skype",
                candidate != null ? candidate.getSkypeName() : null,
                data.getSkype()
        ));

        // 10. Город проживания
        String currentCity = (candidate != null && candidate.getCityOfResidence() != null)
                ? candidate.getCityOfResidence().getCityRuName() : null;
        result.add(createGeneralComparison(
                "cityOfResidence",
                "Город проживания",
                currentCity,
                data.getCity()
        ));

        return result;
    }

    private static CvScanFieldComparison createGeneralComparison(String fieldId, String caption, String curr, String found) {
        String c = (curr != null && !curr.trim().isEmpty()) ? curr.trim() : null;
        String f = (found != null && !found.trim().isEmpty()) ? found.trim() : null;

        CvScanFieldComparison.Status status;
        if (f == null) {
            status = CvScanFieldComparison.Status.EMPTY;
        } else if (c == null) {
            status = CvScanFieldComparison.Status.NEW;
        } else if (c.equalsIgnoreCase(f)) {
            status = CvScanFieldComparison.Status.MATCH;
        } else {
            status = CvScanFieldComparison.Status.REPLACE;
        }
        return new CvScanFieldComparison(fieldId, caption, c != null ? c : "—", f != null ? f : "—", status);
    }

    private static CvScanFieldComparison createPhoneComparison(String fieldId, String caption, String curr, String found) {
        String c = (curr != null && !curr.trim().isEmpty()) ? curr.trim() : null;
        String f = (found != null && !found.trim().isEmpty()) ? found.trim() : null;

        CvScanFieldComparison.Status status;
        if (f == null) {
            status = CvScanFieldComparison.Status.EMPTY;
        } else if (c == null) {
            status = CvScanFieldComparison.Status.NEW;
        } else if (phonesMatch(c, f)) {
            status = CvScanFieldComparison.Status.MATCH;
        } else {
            status = CvScanFieldComparison.Status.REPLACE;
        }
        return new CvScanFieldComparison(fieldId, caption, c != null ? c : "—", f != null ? f : "—", status);
    }

    private static CvScanFieldComparison createEmailComparison(String fieldId, String caption, String curr, String found) {
        String c = (curr != null && !curr.trim().isEmpty()) ? curr.trim() : null;
        String f = (found != null && !found.trim().isEmpty()) ? found.trim() : null;

        CvScanFieldComparison.Status status;
        if (f == null) {
            status = CvScanFieldComparison.Status.EMPTY;
        } else if (c == null) {
            status = CvScanFieldComparison.Status.NEW;
        } else if (emailsMatch(c, f)) {
            status = CvScanFieldComparison.Status.MATCH;
        } else {
            status = CvScanFieldComparison.Status.REPLACE;
        }
        return new CvScanFieldComparison(fieldId, caption, c != null ? c : "—", f != null ? f : "—", status);
    }

    private static CvScanFieldComparison createTelegramComparison(String fieldId, String caption, String curr, String found) {
        String c = (curr != null && !curr.trim().isEmpty()) ? curr.trim() : null;
        String f = (found != null && !found.trim().isEmpty()) ? found.trim() : null;

        CvScanFieldComparison.Status status;
        if (f == null) {
            status = CvScanFieldComparison.Status.EMPTY;
        } else if (c == null) {
            status = CvScanFieldComparison.Status.NEW;
        } else if (telegramsMatch(c, f)) {
            status = CvScanFieldComparison.Status.MATCH;
        } else {
            status = CvScanFieldComparison.Status.REPLACE;
        }
        return new CvScanFieldComparison(fieldId, caption, c != null ? c : "—", f != null ? f : "—", status);
    }
}
