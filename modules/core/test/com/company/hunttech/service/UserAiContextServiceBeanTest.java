package com.company.hunttech.service;

import com.company.hunttech.entity.AiCommunicationStyle;
import com.company.hunttech.entity.AiFunctionalRole;
import com.company.hunttech.entity.AiPreferredLanguage;
import com.company.hunttech.entity.AiResponseDetailLevel;
import com.company.hunttech.entity.UserAiProfile;
import com.company.hunttech.service.dto.AiUserContext;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UserAiContextServiceBeanTest {

    @Test
    public void disabledProfile_returnsEmptyContext() {
        UserAiContextServiceBean service = new UserAiContextServiceBean();
        UserAiProfile profile = new UserAiProfile();
        profile.setProfileEnabled(false);
        profile.setExternalProcessingAllowed(true);
        profile.setAboutMe("Руководитель");

        AiUserContext context = service.buildContext(profile);

        assertTrue(context.isEmpty());
        assertFalse(context.isActive());
    }

    @Test
    public void profileWithoutExternalConsent_returnsEmptyContext() {
        UserAiContextServiceBean service = new UserAiContextServiceBean();
        UserAiProfile profile = new UserAiProfile();
        profile.setProfileEnabled(true);
        profile.setExternalProcessingAllowed(false);
        profile.setAboutMe("Руководитель");

        assertTrue(service.buildContext(profile).isEmpty());
    }

    @Test
    public void activeProfile_separatesDataAndInstructions() {
        UserAiContextServiceBean service = new UserAiContextServiceBean();
        UserAiProfile profile = activeProfile();
        profile.setCurrentPosition("Руководитель");
        profile.setFunctionalRole(AiFunctionalRole.EXECUTIVE);
        profile.setPreferredLanguage(AiPreferredLanguage.RUSSIAN);
        profile.setCustomAiInstructions("Разделяй факты и рекомендации");

        AiUserContext context = service.buildContext(profile);

        assertTrue(context.isActive());
        assertEquals("Руководитель", context.getProfileData().get("currentPosition"));
        assertEquals("EXECUTIVE", context.getProfileData().get("functionalRole"));
        assertEquals("RUSSIAN", context.getProfileData().get("preferredLanguage"));
        assertEquals(1, context.getCustomInstructions().size());
        assertEquals("Разделяй факты и рекомендации", context.getCustomInstructions().get(0));
        assertFalse(context.getProfileData().containsKey("customAiInstructions"));
    }

    @Test
    public void sanitizer_removesControlCharactersAndNormalizesWhitespace() {
        String result = UserAiContextBuilder.sanitize(
                "  Руководитель\u0000\t  HRM  \r\n\n\nархитектор  ", 100);
        assertEquals("Руководитель HRM \n\nархитектор", result);
    }

    @Test
    public void sanitizer_respectsUnicodeCodePointLimit() {
        String result = UserAiContextBuilder.sanitize("АБВГД", 3);
        assertEquals("АБВ", result);
    }

    @Test
    public void preview_neverContainsSecretFields() {
        UserAiContextServiceBean service = new UserAiContextServiceBean();
        UserAiProfile profile = activeProfile();
        profile.setAboutMe("Профессиональный профиль");
        String preview = service.buildContextPreview(profile);
        assertTrue(preview.contains("Профессиональный профиль"));
        assertTrue(preview.contains("API-ключи"));
        assertFalse(preview.contains("SMTP"));
        assertFalse(preview.contains("apiKey"));
    }

    @Test
    public void localBuilder_previewUsesCurrentUnsavedProfileValues() {
        UserAiProfile profile = activeProfile();
        profile.setCurrentPosition("Несохранённая должность из datasource");

        String preview = UserAiContextBuilder.buildPreview(profile);

        assertTrue(preview.contains("Несохранённая должность из datasource"));
        assertEquals(preview, new UserAiContextServiceBean().buildContextPreview(profile));
    }

    @Test
    public void blankValue_isNotAddedToContext() {
        UserAiContextServiceBean service = new UserAiContextServiceBean();
        UserAiProfile profile = activeProfile();
        profile.setAboutMe(" \t ");
        AiUserContext context = service.buildContext(profile);
        assertNull(context.getProfileData().get("aboutMe"));
    }

    @Test
    public void stylePreferencesAndInstructions_keepBudgetBeforeLargeLobFields() {
        // При ограниченном бюджете стилевые предпочтения и пользовательские
        // инструкции добавляются первыми и не вытесняются объёмными LOB-полями.
        UserAiContextServiceBean service = new UserAiContextServiceBean();
        UserAiProfile profile = activeProfile();
        profile.setPreferredLanguage(AiPreferredLanguage.RUSSIAN);
        profile.setCommunicationStyle(AiCommunicationStyle.DIRECT);
        profile.setResponseDetailLevel(AiResponseDetailLevel.BRIEF);
        profile.setCustomAiInstructions("Стиль — кратко и по делу");
        // Объёмные LOB-поля: суммарно заметно больше общего лимита контекста.
        profile.setCurrentResponsibilities(repeat("обязанности ", 700));
        profile.setEducation(repeat("образование ", 700));
        profile.setCertifications(repeat("сертификаты ", 700));
        profile.setDomainExpertise(repeat("экспертиза ", 700));

        AiUserContext context = service.buildContext(profile);

        assertEquals("RUSSIAN", context.getProfileData().get("preferredLanguage"));
        assertEquals("DIRECT", context.getProfileData().get("communicationStyle"));
        assertEquals("BRIEF", context.getProfileData().get("responseDetailLevel"));
        assertEquals(1, context.getCustomInstructions().size());
        assertEquals("Стиль — кратко и по делу", context.getCustomInstructions().get(0));
        // Объёмные поля могли усечься/выпасть — это ожидаемо при лимите,
        // но ядро персонализации обязано выжить.
        assertTrue(context.getProfileData().containsKey("currentResponsibilities"));
    }

    @Test
    public void limitedContext_truncatesByRequestedBudget() {
        UserAiContextServiceBean service = new UserAiContextServiceBean();
        UserAiProfile profile = activeProfile();
        profile.setAboutMe(repeat("слово ", 1000));

        AiUserContext context = UserAiContextBuilder.buildContext(profile, 100);

        // Общий бюджет соблюдён: суммарный размер данных ≤ 100 code points.
        int total = context.getProfileData().values().stream()
                .mapToInt(v -> v.codePointCount(0, v.length()))
                .sum();
        assertTrue(total <= 100);
    }

    @Test
    public void previewWithLimit_matchesContextWithSameLimit() {
        // Консистентность «факт = preview» (план §6.2): одинаковый бюджет —
        // одинаковое содержимое данных.
        UserAiContextServiceBean service = new UserAiContextServiceBean();
        UserAiProfile profile = activeProfile();
        profile.setCurrentPosition("Руководитель");
        profile.setAboutMe(repeat("слово ", 100));

        AiUserContext context = UserAiContextBuilder.buildContext(profile, 200);
        String preview = UserAiContextBuilder.buildPreview(profile, 200);

        for (Map.Entry<String, String> entry : context.getProfileData().entrySet()) {
            assertTrue(preview.contains("- " + entry.getKey() + ": " + entry.getValue()));
        }
    }

    @Test
    public void limitAboveHardCap_isClamped() {
        UserAiContextServiceBean service = new UserAiContextServiceBean();
        UserAiProfile profile = activeProfile();
        profile.setAboutMe(repeat("слово ", 5000));

        AiUserContext context = UserAiContextBuilder.buildContext(profile, 999999);

        int total = context.getProfileData().values().stream()
                .mapToInt(v -> v.codePointCount(0, v.length()))
                .sum();
        // Жёсткий верхний предел builder'а (16000) не превышается.
        assertTrue(total <= 16000);
    }

    @Test
    public void defaultOverload_usesHardLimitOf16000() {
        UserAiProfile profile = activeProfile();
        profile.setAboutMe(repeat("слово ", 5000));

        // Однопараметрические методы builder'а сохраняют исторический дефолт 16000
        // (совместимость с существующими потребителями).
        profile.setCurrentResponsibilities(repeat("обязанности ", 400));
        profile.setDecisionPriorities(repeat("приоритеты ", 400));
        AiUserContext context = UserAiContextBuilder.buildContext(profile);
        int total = context.getProfileData().values().stream()
                .mapToInt(v -> v.codePointCount(0, v.length()))
                .sum();
        assertTrue(total <= 16000);
        assertTrue(total > 4000);
    }

    private static String repeat(String value, int times) {
        return String.join("", java.util.Collections.nCopies(times, value));
    }

    private UserAiProfile activeProfile() {
        UserAiProfile profile = new UserAiProfile();
        profile.setProfileEnabled(true);
        profile.setExternalProcessingAllowed(true);
        return profile;
    }
}
