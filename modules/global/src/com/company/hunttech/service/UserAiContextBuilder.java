package com.company.hunttech.service;

import com.company.hunttech.entity.UserAiProfile;
import com.company.hunttech.service.dto.AiUserContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Формирует безопасный ИИ-контекст из переданного профиля без Spring- и middleware-зависимостей.
 * Общий global-контракт используется core-сервисом и web-предпросмотром, чтобы правила очистки
 * и ограничения данных не расходились между фактическим запросом и показом пользователю.
 */
public final class UserAiContextBuilder {

    private static final int DEFAULT_FIELD_LIMIT = 4000;
    private static final int SHORT_FIELD_LIMIT = 255;
    private static final int TOTAL_CONTEXT_LIMIT = 16000;

    private UserAiContextBuilder() {
    }

    public static AiUserContext buildContext(UserAiProfile profile) {
        AiUserContext context = new AiUserContext();
        if (profile == null
                || !Boolean.TRUE.equals(profile.getProfileEnabled())
                || !Boolean.TRUE.equals(profile.getExternalProcessingAllowed())) {
            return context;
        }

        context.setActive(true);
        int[] remaining = new int[]{TOTAL_CONTEXT_LIMIT};

        // Профессиональные сведения добавляются как данные, а не как инструкции для модели.
        addData(context, remaining, "currentPosition", sanitize(profile.getCurrentPosition(), SHORT_FIELD_LIMIT));
        addData(context, remaining, "functionalRole", enumName(profile.getFunctionalRole()));
        addData(context, remaining, "seniorityLevel", enumName(profile.getSeniorityLevel()));
        addData(context, remaining, "professionalExperienceYears", stringValue(profile.getProfessionalExperienceYears()));
        addData(context, remaining, "recruitingExperienceYears", stringValue(profile.getRecruitingExperienceYears()));
        addData(context, remaining, "aboutMe", sanitize(profile.getAboutMe(), 2000));
        addData(context, remaining, "currentResponsibilities", sanitize(profile.getCurrentResponsibilities(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "education", sanitize(profile.getEducation(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "certifications", sanitize(profile.getCertifications(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "domainExpertise", sanitize(profile.getDomainExpertise(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "industries", sanitize(profile.getIndustries(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "recruitingSpecializations", sanitize(profile.getRecruitingSpecializations(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "targetRoles", sanitize(profile.getTargetRoles(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "candidateLevels", sanitize(profile.getCandidateLevels(), SHORT_FIELD_LIMIT));
        addData(context, remaining, "hiringGeographies", sanitize(profile.getHiringGeographies(), 2000));
        addData(context, remaining, "decisionPriorities", sanitize(profile.getDecisionPriorities(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "clientAndProjectContext", sanitize(profile.getClientAndProjectContext(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "professionalGoals", sanitize(profile.getProfessionalGoals(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "professionalInterests", sanitize(profile.getProfessionalInterests(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "developmentAreas", sanitize(profile.getDevelopmentAreas(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "currentPriorities", sanitize(profile.getCurrentPriorities(), DEFAULT_FIELD_LIMIT));
        addData(context, remaining, "preferredLanguage", enumName(profile.getPreferredLanguage()));
        addData(context, remaining, "responseDetailLevel", enumName(profile.getResponseDetailLevel()));
        addData(context, remaining, "communicationStyle", enumName(profile.getCommunicationStyle()));
        addData(context, remaining, "terminologyLevel", enumName(profile.getTerminologyLevel()));
        addData(context, remaining, "preferredAnswerStructure", enumName(profile.getPreferredAnswerStructure()));
        addData(context, remaining, "communicationConstraints", sanitize(profile.getCommunicationConstraints(), 2000));

        // Только это поле имеет семантику пользовательской инструкции.
        String customInstructions = sanitize(profile.getCustomAiInstructions(), DEFAULT_FIELD_LIMIT);
        if (customInstructions != null && remaining[0] > 0) {
            String limited = truncateByCodePoints(customInstructions, remaining[0]);
            if (!limited.isEmpty()) {
                context.getCustomInstructions().add(limited);
                remaining[0] -= limited.codePointCount(0, limited.length());
            }
        }
        return context;
    }

    public static String buildPreview(UserAiProfile profile) {
        return buildPreview(buildContext(profile), profile);
    }

    static String buildPreview(AiUserContext context, UserAiProfile profile) {
        StringBuilder preview = new StringBuilder();
        preview.append("Источник: сведения, указанные пользователем\n");
        preview.append("Внешние вызовы: не выполняются\n");
        if (profile != null && profile.getProfileConfirmedAt() != null) {
            preview.append("Профиль подтверждён: ")
                    .append(formatDate(profile.getProfileConfirmedAt()))
                    .append('\n');
        }
        preview.append('\n');

        if (context.isEmpty()) {
            preview.append("Контекст не передаётся: профиль выключен или отсутствует согласие.\n");
            return preview.toString();
        }

        preview.append("Данные профиля:\n");
        for (Map.Entry<String, String> entry : context.getProfileData().entrySet()) {
            preview.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }

        preview.append("\nПользовательские инструкции:\n");
        if (context.getCustomInstructions().isEmpty()) {
            preview.append("- не заданы\n");
        } else {
            for (String instruction : context.getCustomInstructions()) {
                preview.append("- ").append(instruction).append('\n');
            }
        }

        preview.append("\nСекреты, почтовые пароли и API-ключи в контекст не включаются.\n");
        return preview.toString();
    }

    // Очищает пользовательский текст от управляющих символов и нормализует пробелы.
    static String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return truncateByCodePoints(normalized, maxLength);
    }

    private static void addData(AiUserContext context, int[] remaining, String key, String value) {
        if (value == null || value.isEmpty() || remaining[0] <= 0) {
            return;
        }
        String limited = truncateByCodePoints(value, remaining[0]);
        if (!limited.isEmpty()) {
            context.getProfileData().put(key, limited);
            remaining[0] -= limited.codePointCount(0, limited.length());
        }
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String truncateByCodePoints(String value, int maxCodePoints) {
        if (value == null || maxCodePoints <= 0) {
            return "";
        }
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxCodePoints) {
            return value;
        }
        int endIndex = value.offsetByCodePoints(0, maxCodePoints);
        return value.substring(0, endIndex);
    }

    private static String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }
}
