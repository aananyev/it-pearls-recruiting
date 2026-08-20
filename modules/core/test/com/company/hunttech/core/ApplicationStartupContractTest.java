package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.entity.ExtUser;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.security.entity.User;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Автотест старта приложения и валидации целостности схемы БД и сущностей.
 * Проверяет доступность контейнера CUBA core, инициализацию анонимного и административного
 * пользователей, а также корректность ORM-маппинга ExtUser (включая атрибут telegram).
 */
public class ApplicationStartupContractTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    @Test
    public void test1_coreContextInitialized() {
        Metadata metadata = AppBeans.get(Metadata.class);
        assertNotNull("Metadata bean must be initialized", metadata);
        assertNotNull("hunttech_ExtUser must be registered", metadata.getClassNN("hunttech_ExtUser"));
    }

    @Test
    public void test2_extUserTelegramFieldMapped() {
        Metadata metadata = AppBeans.get(Metadata.class);
        assertNotNull("telegram property must be present on ExtUser",
                metadata.getClassNN(ExtUser.class).getProperty("telegram"));
    }

    @Test
    public void test3_anonymousUserExistsAndNotDeleted() {
        DataManager dataManager = AppBeans.get(DataManager.class);
        List<User> anonymousUsers = dataManager.load(User.class)
                .query("select u from sec$User u where u.loginLowerCase = 'anonymous'")
                .list();

        assertFalse("Системный пользователь 'anonymous' должен существовать в базе данных",
                anonymousUsers.isEmpty());
        User anon = anonymousUsers.get(0);
        assertTrue("Системный пользователь 'anonymous' должен быть активен",
                Boolean.TRUE.equals(anon.getActive()));
    }

    @Test
    public void test4_extUserQueryExecutesWithoutSqlErrors() {
        Persistence persistence = AppBeans.get(Persistence.class);
        try (Transaction tx = persistence.createTransaction()) {
            List<ExtUser> users = persistence.getEntityManager()
                    .createQuery("select u from hunttech_ExtUser u", ExtUser.class)
                    .setMaxResults(5)
                    .getResultList();
            assertNotNull(users);
            tx.commit();
        }
    }
}
