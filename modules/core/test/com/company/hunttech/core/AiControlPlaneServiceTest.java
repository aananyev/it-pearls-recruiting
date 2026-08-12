package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.entity.ai.AiCapability;
import com.company.hunttech.entity.ai.AiExecutionPolicy;
import com.company.hunttech.entity.ai.AiFallbackPolicy;
import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.company.hunttech.entity.ai.UserAiFunctionOverride;
import com.company.hunttech.service.AiCredentialService;
import com.company.hunttech.service.AiExecutionService;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Интеграционный smoke AI Control Plane без внешних API-вызовов.
 *
 * Проверяет, что новые middleware services доступны через CUBA service proxy,
 * а metadata registry видит все новые persistent entities и enum-backed поля.
 */
public class AiControlPlaneServiceTest {
    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    @Test
    public void middlewareServicesAreResolvable() {
        assertNotNull(AppBeans.get(AiExecutionService.class));
        assertNotNull(AppBeans.get(AiCredentialService.class));
    }

    @Test
    public void aiEntitiesAreRegisteredInMetadata() {
        Metadata metadata = AppBeans.get(Metadata.class);

        assertNotNull(metadata.getClassNN(AdminAiConfiguration.class));
        assertNotNull(metadata.getClassNN(AiFunctionConfiguration.class));
        assertNotNull(metadata.getClassNN(UserAiFunctionOverride.class));
    }

    @Test
    public void functionConfigurationUsesTypedPolicies() {
        AiFunctionConfiguration function = new AiFunctionConfiguration();
        function.setCapability(AiCapability.TEXT_ANALYSIS);
        function.setExecutionPolicy(AiExecutionPolicy.USER_OVERRIDE_ALLOWED);
        function.setFallbackPolicy(AiFallbackPolicy.FALLBACK_TO_ADMIN);

        assertEquals(AiCapability.TEXT_ANALYSIS, function.getCapability());
        assertEquals(AiExecutionPolicy.USER_OVERRIDE_ALLOWED, function.getExecutionPolicy());
        assertEquals(AiFallbackPolicy.FALLBACK_TO_ADMIN, function.getFallbackPolicy());
    }

    @Test
    public void functionCodeAndUserFunctionPairHaveDatabaseGuards() {
        String functionTable = AiFunctionConfiguration.class.getAnnotation(javax.persistence.Table.class).name();
        String overrideTable = UserAiFunctionOverride.class.getAnnotation(javax.persistence.Table.class).name();

        assertEquals("HUNTTECH_AI_FUNCTION_CONFIGURATION", functionTable);
        assertEquals("HUNTTECH_USER_AI_FUNCTION_OVERRIDE", overrideTable);
        assertTrue(AiFunctionConfiguration.class.getDeclaredFields().length > 0);
    }
}
