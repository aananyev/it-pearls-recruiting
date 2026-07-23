package com.company.hunttech.service;

import com.company.hunttech.entity.UserAiProfile;
import com.company.hunttech.service.dto.AiUserContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Service(UserAiContextService.NAME)
public class UserAiContextServiceBean implements UserAiContextService {

    private static final String QUERY_CURRENT_PROFILE =
            "select e from hunttech_UserAiProfile e where e.user = :user";

    private static final int DEFAULT_FIELD_LIMIT = 4000;
    private static final int SHORT_FIELD_LIMIT = 255;
    private static final int TOTAL_CONTEXT_LIMIT = 16000;

    @Inject
    private DataManager dataManager;
    @Inject
    private UserSessionSource userSessionSource;

    @Override
    public AiUserContext buildCurrentUserContext() {
        UserAiProfile profile = dataManager.load(UserAiProfile.class)
                .query(QUERY_CURRENT_PROFILE)
                .parameter("user", userSessionSource.getUserSession().getUser())
                .view("_local")
                .optional()
                .orElse(null);
        return buildContext(profile);
    }

    @Override
    public AiUserContext buildContext(UserAiProfile profile) {
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
        addData(context, remaining, "functionalRole", profile.getFunctionalRole() == null ? null : profile.getFunctionalRole().name());
        addData(context, remaining, "seniorityLevel", profile.getSeniorityLevel() == null ? null : profile.getSeniorityLevel().name());
        addData(context, remaining, "professionalExperienceYears", profile.getProfessionalExperienceYears() == null ? null : String.valueOf(profile.getProfessionalExperienceYears()));
        addData(context, remaining, "recruitingExperienceYears", profile.getRecruitingExperienceYears() == null ? null : String.valueOf(profile.getRecruitingExperienceYears()));
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
        addData(context, remaining, "preferredLanguage", profile.getPreferredLanguage() == null ? null : profile.getPreferredLanguage().name());
        addData(context, remaining, "responseDetailLevel", profile.getResponseDetailLevel() == null ? null : profile.getResponseDetailLevel().name());
        addData(context, remaining, "communicationStyle", profile.getCommunicationStyle() == null ? null : profile.getCommunicationStyle().name());
        addData(context, remaining, "terminologyLevel", profile.getTerminologyLevel() == null ? null : profile.getTerminologyLevel().name());
        addData(context, remaining, "preferredAnswerStructure", profile.getPreferredAnswerStructure() == null ? null : profile.getPreferredAnswerStructure().name());
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

    @Override
    public String buildCurrentUserContextPreview() {
        return buildPreview(buildCurrentUserContext(), null);
    }

    @Override
    public String buildContextPreview(UserAiProfile profile) {
        return buildPreview(buildContext(profile), profile);
    }

    private String buildPreview(AiUserContext context, UserAiProfile profile) {
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

    private void addData(AiUserContext context, int[] remaining, String key, String value) {
        if (value == null || value.isEmpty() || remaining[0] <= 0) {
            return;
        }
        String limited = truncateByCodePoints(value, remaining[0]);
        if (!limited.isEmpty()) {
            context.getProfileData().put(key, limited);
            remaining[0] -= limited.codePointCount(0, limited.length());
        }
    }

    // Очищает пользовательский текст от управляющих символов и нормализует пробелы.
    String sanitize(String value, int maxLength) {
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

    private String truncateByCodePoints(String value, int maxCodePoints) {
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

    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }
}
