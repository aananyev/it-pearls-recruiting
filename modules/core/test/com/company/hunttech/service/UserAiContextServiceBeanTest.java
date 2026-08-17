package com.company.hunttech.service;

import com.company.hunttech.entity.AiCommunicationStyle;
import com.company.hunttech.entity.AiFunctionalRole;
import com.company.hunttech.entity.AiPreferredLanguage;
import com.company.hunttech.entity.AiResponseDetailLevel;
import com.company.hunttech.entity.UserAiProfile;
import com.company.hunttech.service.dto.AiUserContext;
import org.junit.Test;

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
