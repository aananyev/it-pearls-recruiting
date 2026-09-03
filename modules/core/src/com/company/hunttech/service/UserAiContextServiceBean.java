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
        return UserAiContextBuilder.buildPreview(profile);
    }

    /**
     * Резолвит {@code hunttech.ai.userContextLimit} через CUBA Config
     * ({@link HunttechAiPersonalizationConfig}); некорректное (≤ 0) значение даёт дефолт 4000.
     * Весь блок ограничен сверху жёстким лимитом builder'а (16000), поэтому
     * чрезмерно большое значение безопасно клампится. Единая точка резолва:
     * и фактическое исполнение, и UI-предпросмотр обязаны использовать этот метод,
     * чтобы «факт = preview» (план персонализации §6.2).
     */
    public int resolveConfiguredLimit() {
        int configured = configuration.getConfig(HunttechAiPersonalizationConfig.class).getUserContextLimit();
        return configured > 0 ? configured : 4000;
    }

    /*
     * Загрузка профиля остаётся в middleware: фактические AI-вызовы должны работать
     * с сохранённым профилем пользователя, а UI-предпросмотр использует текущий datasource.
     */
    private UserAiProfile loadCurrentUserProfile() {
        return dataManager.load(UserAiProfile.class)
                .query(QUERY_CURRENT_PROFILE)
                .parameter("user", userSessionSource.getUserSession().getUser())
                .view("_local")
                .optional()
                .orElse(null);
    }
}


