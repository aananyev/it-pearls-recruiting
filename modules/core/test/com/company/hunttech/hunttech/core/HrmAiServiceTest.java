package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.TestEntityTracker;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.service.HrmAiService;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.security.entity.User;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Проверяет доступность и базовую целостность AI-сервиса.
 * <p>
 * Полноценные тесты testConnection с реальными API-ключами выполняются
 * вручную через UI (кнопка «Тест AI» в окне Мониторинга ключей).
 */
public class HrmAiServiceTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private HrmAiService aiService;
    private DataManager dataManager;
    private TestEntityTracker tracker;

    @Before
    public void setUp() {
        aiService = AppBeans.get(HrmAiService.class);
        dataManager = AppBeans.get(DataManager.class);
        tracker = new TestEntityTracker(dataManager);
    }

    @After
    public void tearDown() {
        tracker.cleanup();
    }

    @Test
    public void serviceBeanIsResolvable() {
        assertNotNull("HrmAiService должен быть доступен через AppBeans", aiService);
    }

    @Test
    public void newConfigurationCanBeCreated() {
        User user = dataManager.load(User.class)
                .query("select u from sec$User u").maxResults(1).one();

        UserAiConfiguration c = new UserAiConfiguration();
        c.setUser(user);
        c.setProviderCode("openai");
        c.setApiKey("sk-test-placeholder");
        c.setDefaultModelName("gpt-4o");
        c.setIsActive(true);

        assertEquals("openai", c.getProviderCode());
        assertNotNull(c.getUser());
        assertNotNull(c.getApiKey());
    }

    @Test
    public void aiAnalysisServiceIsResolvable() {
        Object svc = AppBeans.get("hunttech_AiAnalysisService");
        assertNotNull("AiAnalysisService должен быть доступен", svc);
    }

    @Test
    public void entityDataExtractorsIsResolvable() {
        Object extractors = AppBeans.get("hunttech_EntityDataExtractors");
        assertNotNull("EntityDataExtractors должен быть доступен", extractors);
    }
}
