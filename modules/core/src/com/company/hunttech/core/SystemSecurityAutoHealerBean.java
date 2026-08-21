package com.company.hunttech.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Автоматический модуль целостности и самоисцеления системных учетных записей (anonymous, admin).
 * Гарантирует, что при любом старте приложения или обновлении Spring-контекста системные пользователи
 * и их роли будут активны и доступны (delete_ts IS NULL, active = true).
 * Пароли и персональные данные пользователя admin не затрагиваются.
 */
@Component("hunttech_SystemSecurityAutoHealer")
public class SystemSecurityAutoHealerBean {

    private static final Logger log = LoggerFactory.getLogger(SystemSecurityAutoHealerBean.class);

    @Inject
    private DataSource dataSource;

    @PostConstruct
    public void onInit() {
        healSystemUsers();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        healSystemUsers();
    }

    /**
     * Выполняет проверку и восстановление системных пользователей anonymous и admin.
     *
     * @return количество восстановленных записей
     */
    public int healSystemUsers() {
        if (dataSource == null) {
            return 0;
        }
        int totalHealed = 0;
        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // 1. Восстановление пользователей anonymous и admin при soft-delete или неактивности (пароли сохраняются)
                String healUsersSql = "UPDATE sec_user " +
                        "SET delete_ts = NULL, deleted_by = NULL, active = true " +
                        "WHERE login_lc IN ('anonymous', 'admin') AND (delete_ts IS NOT NULL OR active = false)";
                try (PreparedStatement ps = conn.prepareStatement(healUsersSql)) {
                    int healedUsers = ps.executeUpdate();
                    if (healedUsers > 0) {
                        log.info("[SystemSecurityAutoHealer] Восстановлена активность системных пользователей: {}", healedUsers);
                        totalHealed += healedUsers;
                    }
                }

                // 2. Восстановление ролей пользователей anonymous и admin
                String healRolesSql = "UPDATE sec_user_role " +
                        "SET delete_ts = NULL, deleted_by = NULL " +
                        "WHERE user_id IN (SELECT id FROM sec_user WHERE login_lc IN ('anonymous', 'admin')) " +
                        "AND delete_ts IS NOT NULL";
                try (PreparedStatement ps = conn.prepareStatement(healRolesSql)) {
                    int healedRoles = ps.executeUpdate();
                    if (healedRoles > 0) {
                        log.info("[SystemSecurityAutoHealer] Восстановлены роли системных пользователей: {}", healedRoles);
                        totalHealed += healedRoles;
                    }
                }

                conn.commit();
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (Exception rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.warn("[SystemSecurityAutoHealer] Ошибка при проверке и восстановлении системных пользователей: {}", e.getMessage(), e);
        }
        return totalHealed;
    }
}
