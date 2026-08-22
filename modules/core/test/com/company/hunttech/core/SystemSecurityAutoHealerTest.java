package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.security.entity.User;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Автотест гарантии самоисцеления и целостности системных учетных записей (anonymous, admin).
 * Проверяет, что при искусственной симуляции повреждения (soft-delete, active=false)
 * компонент SystemSecurityAutoHealerBean мгновенно восстанавливает пользователей и их роли,
 * не изменяя пароль администратора.
 */
public class SystemSecurityAutoHealerTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    @Test
    public void testAutoHealerRestoresAnonymousAndAdminPreservingPassword() throws Exception {
        DataSource dataSource = AppBeans.get(DataSource.class);
        assertNotNull("DataSource должен быть доступен в Spring контексте", dataSource);

        SystemSecurityAutoHealerBean healer = AppBeans.get(SystemSecurityAutoHealerBean.class);
        assertNotNull("SystemSecurityAutoHealerBean должен быть зарегистрирован", healer);

        DataManager dataManager = AppBeans.get(DataManager.class);

        String originalAdminPassword = null;
        try (Connection conn = dataSource.getConnection()) {
            // 1. Получаем оригинальный пароль администратора
            try (PreparedStatement ps = conn.prepareStatement("SELECT password FROM sec_user WHERE login_lc = 'admin'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        originalAdminPassword = rs.getString("password");
                    }
                }
            }

            // 2. Симулируем повреждение/soft-delete системных пользователей
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("UPDATE sec_user SET delete_ts = CURRENT_TIMESTAMP, deleted_by = 'simulated_test', active = false " +
                        "WHERE login_lc IN ('anonymous', 'admin')");
                st.executeUpdate("UPDATE sec_user_role SET delete_ts = CURRENT_TIMESTAMP, deleted_by = 'simulated_test' " +
                        "WHERE user_id IN (SELECT id FROM sec_user WHERE login_lc IN ('anonymous', 'admin'))");
            }

            // Проверяем, что пользователи действительно помечены как удаленные
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT count(*) FROM sec_user WHERE login_lc IN ('anonymous', 'admin') AND delete_ts IS NOT NULL")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(2, rs.getInt(1));
                }
            }

            // 3. Вызываем модуль самоисцеления
            int healedCount = healer.healSystemUsers();
            assertTrue("AutoHealer должен восстановить минимум 2 пользователей", healedCount >= 2);

            // 4. Проверяем, что пользователи anonymous и admin полностью активны (delete_ts IS NULL, active = true)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT login_lc, active, delete_ts, password FROM sec_user WHERE login_lc IN ('anonymous', 'admin')")) {
                try (ResultSet rs = ps.executeQuery()) {
                    int verified = 0;
                    while (rs.next()) {
                        verified++;
                        String login = rs.getString("login_lc");
                        assertTrue("Пользователь " + login + " должен быть active=true", rs.getBoolean("active"));
                        assertNull("Пользователь " + login + " не должен иметь delete_ts", rs.getTimestamp("delete_ts"));
                        if ("admin".equals(login)) {
                            assertEquals("Пароль администратора должен быть неизменным", originalAdminPassword, rs.getString("password"));
                        }
                    }
                    assertEquals(2, verified);
                }
            }

            // 5. Проверяем, что роли системных пользователей также активны
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT count(*) FROM sec_user_role ur JOIN sec_user u ON u.id = ur.user_id " +
                    "WHERE u.login_lc IN ('anonymous', 'admin') AND ur.delete_ts IS NOT NULL")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("Не должно остаться soft-deleted ролей у системных пользователей", 0, rs.getInt(1));
                }
            }
        }

        // 6. Проверяем через CUBA DataManager (проверка уровня ORM / сущностей)
        List<User> anonUsers = dataManager.load(User.class)
                .query("select u from sec$User u where u.loginLowerCase = 'anonymous'")
                .list();
        assertFalse("Пользователь anonymous должен загружаться через DataManager", anonUsers.isEmpty());
        assertTrue("Пользователь anonymous должен быть активен", Boolean.TRUE.equals(anonUsers.get(0).getActive()));

        List<User> adminUsers = dataManager.load(User.class)
                .query("select u from sec$User u where u.loginLowerCase = 'admin'")
                .list();
        assertFalse("Пользователь admin должен загружаться через DataManager", adminUsers.isEmpty());
        assertTrue("Пользователь admin должен быть активен", Boolean.TRUE.equals(adminUsers.get(0).getActive()));
    }
}
