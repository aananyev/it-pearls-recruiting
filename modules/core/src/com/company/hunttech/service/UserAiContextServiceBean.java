package com.company.hunttech.service;

import com.company.hunttech.config.HunttechAiPersonalizationConfig;
import com.company.hunttech.entity.UserAiProfile;
import com.company.hunttech.service.dto.AiUserContext;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(UserAiContextService.NAME)
public class UserAiContextServiceBean implements UserAiContextService {

    private static final String QUERY_CURRENT_PROFILE =
            "select e from hunttech_UserAiProfile e where e.user = :user";

    @Inject
    private DataManager dataManager;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Configuration configuration;

    @Override
    public AiUserContext buildCurrentUserContext() {
        return UserAiContextBuilder.buildContext(loadCurrentUserProfile(), resolveConfiguredLimit());
    }

    @Override
    public AiUserContext buildContext(UserAiProfile profile) {
        return UserAiContextBuilder.buildContext(profile);
    }

    @Override
    public String buildCurrentUserContextPreview() {
        return UserAiContextBuilder.buildPreview(buildCurrentUserContext(), null);
    }

    @Override
    public String buildContextPreview(UserAiProfile profile) {
        // Тот же бюджет, что и фактическое исполнение: «факт = preview» (план §6.2).
        return UserAiContextBuilder.buildPreview(profile, resolveConfiguredLimit());
    }

    /**
     * Резолвит {@code hunttech.ai.userContextLimit} через CUBA Config
     * ({@link HunttechAiPersonalizationConfig#getUserContextLimitOrDefault()}):
     * единая точка резолва — и фактическое исполнение, и UI-предпросмотр обязаны
     * использовать этот метод, чтобы «факт = preview» (план персонализации §6.2).
     * Весь блок дополнительно ограничен сверху жёстким лимитом builder'а (16000).
     */
    public int resolveConfiguredLimit() {
        if (configuration == null) {
            // Юнит-тесты создают бин без контейнера: CUBA Config недоступен — дефолт 4000.
            return 4000;
        }
        return configuration.getConfig(HunttechAiPersonalizationConfig.class).getUserContextLimitOrDefault();
    }

    /*
     * Загрузка профиля остаётся в middleware: фактические AI-вызовы должны работать
     * с сохранённым профилем пользователя, а UI-предпросмотр использует текущий datasource.
     */
    private UserAiProfile loadCurrentUserProfile() {
        return dataManager.load(UserAiProfile.class)
                .query(QUERY_CURRENT_PROFILE)
                .parameter("user", userSessionSource.getUserSession().getUser())
                .view("userAiProfile-view")
                .optional()
                .orElse(null);
    }
}

