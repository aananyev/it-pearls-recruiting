package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.City;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Project;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.security.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Хелпер для формирования глобальных уведомлений (UiNotificationEvent)
 * об открытии, закрытии и создании вакансий с краткими характеристиками.
 */
public final class OpenPositionNotificationHelper {
    private static final Logger log = LoggerFactory.getLogger(OpenPositionNotificationHelper.class);

    private static final ThreadLocal<DecimalFormat> SALARY_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        return new DecimalFormat("#,###", symbols);
    });

    private OpenPositionNotificationHelper() {
    }

    /**
     * Формирует сообщение об открытии вакансии.
     */
    public static String buildOpenMessage(OpenPosition position, User user) {
        return buildNotificationHtml("Открыта вакансия: ", position, user);
    }

    /**
     * Формирует сообщение о закрытии вакансии.
     */
    public static String buildCloseMessage(OpenPosition position, User user) {
        return buildNotificationHtml("Закрыта вакансия: ", position, user);
    }

    /**
     * Формирует сообщение о создании новой вакансии.
     */
    public static String buildNewVacancyMessage(OpenPosition position, User user) {
        return buildNotificationHtml("Открыта новая вакансия: ", position, user);
    }

    public static String buildNotificationHtml(String prefix, OpenPosition position, User user) {
        StringBuilder sb = new StringBuilder();
        String title = position != null && position.getVacansyName() != null ? position.getVacansyName() : "Без названия";
        sb.append(prefix).append("<b>").append(escapeHtml(title)).append("</b>");

        String characteristics = formatShortCharacteristics(position);
        if (!characteristics.isEmpty()) {
            sb.append("<br><span style='font-size: 12px; color: #4b5563;'>")
              .append(characteristics)
              .append("</span>");
        }

        if (user != null && user.getName() != null && !user.getName().trim().isEmpty()) {
            sb.append("<br><span style='font-size: 11px; color: #9ca3af; float: right;'><i>")
              .append(escapeHtml(user.getName()))
              .append("</i></span>");
        }

        return sb.toString();
    }

    public static String formatShortCharacteristics(OpenPosition position) {
        if (position == null) return "";
        List<String> parts = new ArrayList<>();

        // 1. Проект / Компания
        try {
            if (isLoaded(position, "projectName") && position.getProjectName() != null) {
                Project prj = position.getProjectName();
                String prjName = prj.getProjectName();
                if (prjName != null && !prjName.trim().isEmpty()) {
                    parts.add("Проект: " + escapeHtml(prjName.trim()));
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Город / Локация
        try {
            if (isLoaded(position, "cityPosition") && position.getCityPosition() != null) {
                String cityName = position.getCityPosition().getCityRuName();
                if (cityName != null && !cityName.trim().isEmpty()) {
                    parts.add(escapeHtml(cityName.trim()));
                }
            } else if (isLoaded(position, "cities") && position.getCities() != null && !position.getCities().isEmpty()) {
                City city = position.getCities().iterator().next();
                if (city != null && city.getCityRuName() != null) {
                    parts.add(escapeHtml(city.getCityRuName().trim()));
                }
            }
        } catch (Exception ignored) {
        }

        // 3. Формат работы (удаленка / комментарий)
        try {
            if (isLoaded(position, "remoteWork") && position.getRemoteWork() != null) {
                Integer rw = position.getRemoteWork();
                if (rw == 0) {
                    parts.add("В офисе");
                } else if (rw == 1) {
                    parts.add("Удалённо");
                } else if (rw == 2) {
                    parts.add("Гибрид");
                }
            } else if (isLoaded(position, "remoteComment") && position.getRemoteComment() != null && !position.getRemoteComment().trim().isEmpty()) {
                parts.add(escapeHtml(position.getRemoteComment().trim()));
            }
        } catch (Exception ignored) {
        }

        // 4. Зарплата
        try {
            BigDecimal min = isLoaded(position, "salaryMin") ? position.getSalaryMin() : null;
            BigDecimal max = isLoaded(position, "salaryMax") ? position.getSalaryMax() : null;
            DecimalFormat df = SALARY_FORMAT.get();
            if (min != null && max != null) {
                parts.add(df.format(min) + " – " + df.format(max) + " ₽");
            } else if (min != null) {
                parts.add("от " + df.format(min) + " ₽");
            } else if (max != null) {
                parts.add("до " + df.format(max) + " ₽");
            }
        } catch (Exception ignored) {
        }

        return String.join(" · ", parts);
    }

    private static boolean isLoaded(Object entity, String property) {
        if (entity == null || property == null) return false;
        try {
            if (PersistenceHelper.isNew(entity)) {
                return true;
            }
            return PersistenceHelper.isLoaded(entity, property);
        } catch (Exception e) {
            log.debug("Не удалось проверить состояние загрузки свойства {}: {}", property, e.getMessage());
            return true;
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
