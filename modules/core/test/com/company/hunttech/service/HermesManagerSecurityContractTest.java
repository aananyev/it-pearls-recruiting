package com.company.hunttech.service;

import com.company.hunttech.config.HunttechHermesManagerConfig;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Project;
import com.company.hunttech.entity.ai.LlmChatConversation;
import com.company.hunttech.entity.ai.LlmChatMessage;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.global.UserSession;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Модульные автотесты безопасности и контрактов сервиса HermesManagerChatServiceBean:
 * 1. Запрос отклоняется при отсутствии specific permission hunttech.ai.useManagerHermesWrite.
 * 2. Запрет raw SQL / DELETE FROM / TRUNCATE / DROP / ALTER / DDL.
 * 3. Безусловный запрет операции DELETE (DENY BY DEFAULT).
 * 4. Запрет неизвестных сущностей (DENY BY DEFAULT).
 * 5. Запрет неизвестных полей (DENY BY DEFAULT).
 * 6. Отклонение CREATE при отсутствии EntityOp.CREATE.
 * 7. Отклонение UPDATE при отсутствии EntityOp.UPDATE.
 * 8. Отклонение изменения атрибута при отсутствии EntityAttrAccess.MODIFY.
 * 9. Успешный CREATE при наличии всех прав.
 * 10. Успешный UPDATE при наличии всех прав.
 */
public class HermesManagerSecurityContractTest {

    private HermesManagerChatServiceBean service;
    private DataManager dataManager;
    private Metadata metadata;
    private Security security;
    private UserSessionSource userSessionSource;
    private Configuration configuration;
    private HunttechHermesManagerConfig managerConfig;
    private MetaClass openPositionMetaClass;
    private ExtUser testUser;

    @Before
    public void setUp() {
        service = new HermesManagerChatServiceBean();
        dataManager = mock(DataManager.class);
        metadata = mock(Metadata.class);
        security = mock(Security.class);
        userSessionSource = mock(UserSessionSource.class);
        configuration = mock(Configuration.class);
        managerConfig = mock(HunttechHermesManagerConfig.class);
        openPositionMetaClass = mock(MetaClass.class);

        when(configuration.getConfig(HunttechHermesManagerConfig.class)).thenReturn(managerConfig);
        when(managerConfig.getProfile()).thenReturn("hrm-operator");
        when(managerConfig.getContainerName()).thenReturn("hermes-hrm-operator");
        when(metadata.getClassNN(OpenPosition.class)).thenReturn(openPositionMetaClass);

        testUser = new ExtUser();
        testUser.setId(UUID.randomUUID());
        testUser.setLogin("manager-user");

        UserSession userSession = mock(UserSession.class);
        when(userSession.getUser()).thenReturn(testUser);
        when(userSessionSource.getUserSession()).thenReturn(userSession);

        ReflectionTestUtils.setField(service, "dataManager", dataManager);
        ReflectionTestUtils.setField(service, "metadata", metadata);
        ReflectionTestUtils.setField(service, "security", security);
        ReflectionTestUtils.setField(service, "userSessionSource", userSessionSource);
        ReflectionTestUtils.setField(service, "configuration", configuration);
    }

    @Test
    public void testBackendRequestRejectedWithoutSpecificPermission() {
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(false);

        try {
            service.startManagerHermesConversation();
            fail("Ожидался SecurityException при отсутствии hunttech.ai.useManagerHermesWrite");
        } catch (SecurityException ex) {
            assertTrue("Сообщение должно указывать на недостаток прав", ex.getMessage().contains("Недостаточно прав"));
        }

        try {
            service.loadManagerHermesHistory(UUID.randomUUID());
            fail("Ожидался SecurityException при отсутствии hunttech.ai.useManagerHermesWrite");
        } catch (SecurityException ex) {
            assertTrue("Сообщение должно указывать на недостаток прав", ex.getMessage().contains("Недостаточно прав"));
        }

        try {
            service.sendManagerHermesMessage(UUID.randomUUID(), "Привет");
            fail("Ожидался SecurityException при отсутствии hunttech.ai.useManagerHermesWrite");
        } catch (SecurityException ex) {
            assertTrue("Сообщение должно указывать на недостаток прав", ex.getMessage().contains("Недостаточно прав"));
        }
    }

    @Test
    public void testRawSqlRejected() {
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(true);

        String[] dangerousSqlPrompts = {
                "SELECT * FROM sec_user",
                "DROP TABLE hunttech_open_position",
                "DELETE FROM sec_user WHERE login = 'admin'",
                "TRUNCATE TABLE hunttech_open_position",
                "ALTER TABLE hunttech_open_position DROP COLUMN vacansy_name",
                "INSERT INTO sec_user (id) VALUES ('123')",
                "UPDATE sec_user SET password = '123'",
                "UNION ALL SELECT id, password FROM sec_user"
        };

        for (String sql : dangerousSqlPrompts) {
            try {
                service.sendManagerHermesMessage(UUID.randomUUID(), sql);
                fail("Ожидался SecurityException для raw SQL: " + sql);
            } catch (SecurityException ex) {
                assertTrue("Сообщение должно запрещать сырые SQL: " + sql,
                        ex.getMessage().contains("SQL-запросы запрещены"));
            }
        }
    }

    @Test
    public void testDeleteOperationAlwaysRejected() {
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(true);
        when(security.isEntityOpPermitted(OpenPosition.class, EntityOp.DELETE)).thenReturn(true); // даже если роль теоретически имеет DELETE

        String rawResponse = "```json\n" +
                "{\n" +
                "  \"intent\": \"MUTATION\",\n" +
                "  \"entity\": \"OpenPosition\",\n" +
                "  \"operation\": \"DELETE\",\n" +
                "  \"entityId\": \"" + UUID.randomUUID() + "\",\n" +
                "  \"summary\": \"Удалить вакансию\"\n" +
                "}\n" +
                "```";

        try {
            service.processHermesResponse(rawResponse);
            fail("Ожидался SecurityException при попытке DELETE");
        } catch (SecurityException ex) {
            assertTrue("Сообщение должно гласить о безусловном запрете DELETE",
                    ex.getMessage().contains("Операция DELETE безусловно запрещена"));
        } catch (Exception e) {
            fail("Ожидался именно SecurityException, получен " + e.getClass().getName());
        }
    }

    @Test
    public void testUnsupportedEntityRejectedDefaultDeny() {
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(true);

        String rawResponse = "```json\n" +
                "{\n" +
                "  \"intent\": \"MUTATION\",\n" +
                "  \"entity\": \"SecUser\",\n" +
                "  \"operation\": \"CREATE\",\n" +
                "  \"attributes\": {\n" +
                "    \"login\": \"hacker\"\n" +
                "  }\n" +
                "}\n" +
                "```";

        try {
            service.processHermesResponse(rawResponse);
            fail("Ожидался отказ для неподдерживаемой сущности SecUser");
        } catch (IllegalArgumentException ex) {
            assertTrue("Должен сработать DENY BY DEFAULT для неизвестной сущности",
                    ex.getMessage().contains("DENY BY DEFAULT"));
        } catch (Exception e) {
            fail("Ожидался IllegalArgumentException, получен " + e.getClass().getName());
        }
    }

    @Test
    public void testUnknownFieldRejectedDefaultDeny() {
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(true);

        String rawResponse = "```json\n" +
                "{\n" +
                "  \"intent\": \"MUTATION\",\n" +
                "  \"entity\": \"OpenPosition\",\n" +
                "  \"operation\": \"CREATE\",\n" +
                "  \"attributes\": {\n" +
                "    \"vacansyName\": \"Разработчик\",\n" +
                "    \"unknownDangerousField\": \"payload\"\n" +
                "  }\n" +
                "}\n" +
                "```";

        try {
            service.processHermesResponse(rawResponse);
            fail("Ожидался отказ для неизвестного поля unknownDangerousField");
        } catch (IllegalArgumentException ex) {
            assertTrue("Должен сработать DENY BY DEFAULT для неизвестного поля",
                    ex.getMessage().contains("DENY BY DEFAULT"));
        } catch (Exception e) {
            fail("Ожидался IllegalArgumentException, получен " + e.getClass().getName());
        }
    }

    @Test
    public void testCreateRejectedWithoutEntityOpCreate() {
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(true);
        when(security.isEntityOpPermitted(OpenPosition.class, EntityOp.CREATE)).thenReturn(false);

        String rawResponse = "```json\n" +
                "{\n" +
                "  \"intent\": \"MUTATION\",\n" +
                "  \"entity\": \"OpenPosition\",\n" +
                "  \"operation\": \"CREATE\",\n" +
                "  \"attributes\": {\n" +
                "    \"vacansyName\": \"Java Developer\"\n" +
                "  }\n" +
                "}\n" +
                "```";

        try {
            service.processHermesResponse(rawResponse);
            fail("Ожидался SecurityException при отсутствии EntityOp.CREATE");
        } catch (SecurityException ex) {
            assertTrue("Сообщение должно указывать на недостаток прав",
                    ex.getMessage().contains("Недостаточно прав"));
        } catch (Exception e) {
            fail("Ожидался SecurityException, получен " + e.getClass().getName());
        }
    }

    @Test
    public void testUpdateRejectedWithoutEntityOpUpdate() {
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(true);
        when(security.isEntityOpPermitted(OpenPosition.class, EntityOp.UPDATE)).thenReturn(false);

        String rawResponse = "```json\n" +
                "{\n" +
                "  \"intent\": \"MUTATION\",\n" +
                "  \"entity\": \"OpenPosition\",\n" +
                "  \"operation\": \"UPDATE\",\n" +
                "  \"entityId\": \"" + UUID.randomUUID() + "\",\n" +
                "  \"attributes\": {\n" +
                "    \"salaryMin\": 200000\n" +
                "  }\n" +
                "}\n" +
                "```";

        try {
            service.processHermesResponse(rawResponse);
            fail("Ожидался SecurityException при отсутствии EntityOp.UPDATE");
        } catch (SecurityException ex) {
            assertTrue("Сообщение должно указывать на недостаток прав",
                    ex.getMessage().contains("Недостаточно прав"));
        } catch (Exception e) {
            fail("Ожидался SecurityException, получен " + e.getClass().getName());
        }
    }

    @Test
    public void testAttributeModifyRejectedWithoutModifyPermission() {
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(true);
        when(security.isEntityOpPermitted(OpenPosition.class, EntityOp.CREATE)).thenReturn(true);
        when(security.isEntityAttrPermitted(openPositionMetaClass, "salaryMin", EntityAttrAccess.MODIFY)).thenReturn(false);

        String rawResponse = "```json\n" +
                "{\n" +
                "  \"intent\": \"MUTATION\",\n" +
                "  \"entity\": \"OpenPosition\",\n" +
                "  \"operation\": \"CREATE\",\n" +
                "  \"attributes\": {\n" +
                "    \"salaryMin\": 200000\n" +
                "  }\n" +
                "}\n" +
                "```";

        try {
            service.processHermesResponse(rawResponse);
            fail("Ожидался SecurityException при отсутствии EntityAttrAccess.MODIFY для salaryMin");
        } catch (SecurityException ex) {
            assertTrue("Сообщение должно указывать на недостаток прав",
                    ex.getMessage().contains("Недостаточно прав"));
        } catch (Exception e) {
            fail("Ожидался SecurityException, получен " + e.getClass().getName());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateAllowedWithValidPermissions() throws Exception {
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(true);
        when(security.isEntityOpPermitted(OpenPosition.class, EntityOp.CREATE)).thenReturn(true);
        when(security.isEntityAttrPermitted(eq(openPositionMetaClass), anyString(), eq(EntityAttrAccess.MODIFY))).thenReturn(true);

        OpenPosition newPos = new OpenPosition();
        UUID generatedId = UUID.randomUUID();
        newPos.setId(generatedId);
        newPos.setVacansyName("Senior Java Engineer");

        when(metadata.create(OpenPosition.class)).thenReturn(newPos);
        when(dataManager.commit(any(OpenPosition.class))).thenReturn(newPos);

        FluentLoader projectLoader = mock(FluentLoader.class);
        FluentLoader.ByQuery byQuery = mock(FluentLoader.ByQuery.class);
        when(dataManager.load(Project.class)).thenReturn(projectLoader);
        when(projectLoader.query(anyString())).thenReturn(byQuery);
        when(byQuery.parameter(anyString(), any())).thenReturn(byQuery);
        when(byQuery.maxResults(anyInt())).thenReturn(byQuery);
        when(byQuery.optional()).thenReturn(Optional.empty());

        String rawResponse = "Хорошо, создаю вакансию.\n" +
                "```json\n" +
                "{\n" +
                "  \"intent\": \"MUTATION\",\n" +
                "  \"entity\": \"OpenPosition\",\n" +
                "  \"operation\": \"CREATE\",\n" +
                "  \"summary\": \"Создание вакансии Senior Java Engineer\",\n" +
                "  \"attributes\": {\n" +
                "    \"vacansyName\": \"Senior Java Engineer\",\n" +
                "    \"salaryMin\": 300000,\n" +
                "    \"salaryMax\": 450000\n" +
                "  }\n" +
                "}\n" +
                "```";

        String result = service.processHermesResponse(rawResponse);
        assertNotNull(result);
        assertTrue(result.contains("Senior Java Engineer"));
        assertTrue(result.contains("hrm://openPosition/" + generatedId));
        assertTrue(result.contains("✅ **Вакансия успешно создана:**"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateAllowedWithValidPermissions() throws Exception {
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(true);
        when(security.isEntityOpPermitted(OpenPosition.class, EntityOp.UPDATE)).thenReturn(true);
        when(security.isEntityAttrPermitted(eq(openPositionMetaClass), anyString(), eq(EntityAttrAccess.MODIFY))).thenReturn(true);

        UUID posId = UUID.randomUUID();
        OpenPosition existingPos = new OpenPosition();
        existingPos.setId(posId);
        existingPos.setVacansyName("Lead DevOps Engineer");

        FluentLoader posLoader = mock(FluentLoader.class);
        FluentLoader.ById byIdLoader = mock(FluentLoader.ById.class);
        when(dataManager.load(OpenPosition.class)).thenReturn(posLoader);
        when(posLoader.id(posId)).thenReturn(byIdLoader);
        when(byIdLoader.optional()).thenReturn(Optional.of(existingPos));
        when(dataManager.commit(existingPos)).thenReturn(existingPos);

        String rawResponse = "```json\n" +
                "{\n" +
                "  \"intent\": \"MUTATION\",\n" +
                "  \"entity\": \"OpenPosition\",\n" +
                "  \"operation\": \"UPDATE\",\n" +
                "  \"entityId\": \"" + posId + "\",\n" +
                "  \"summary\": \"Обновление зарплатной вилки\",\n" +
                "  \"attributes\": {\n" +
                "    \"salaryMin\": 350000\n" +
                "  }\n" +
                "}\n" +
                "```";

        String result = service.processHermesResponse(rawResponse);
        assertNotNull(result);
        assertTrue(result.contains("hrm://openPosition/" + posId));
        assertTrue(result.contains("✅ **Вакансия успешно обновлена:**"));
    }
}
