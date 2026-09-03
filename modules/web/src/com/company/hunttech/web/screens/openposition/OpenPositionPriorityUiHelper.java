package com.company.hunttech.web.screens.openposition;

import com.haulmont.cuba.gui.screen.MessageBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Единый UI-хэлпер для визуализации и стилизации бейджей приоритета вакансий
 * в реестрах (OpenPositionReestrBrowse, OpenPositionBrowse) и редакторе OpenPositionEdit.
 */
public final class OpenPositionPriorityUiHelper {

    private static final Logger log = LoggerFactory.getLogger(OpenPositionPriorityUiHelper.class);

    private OpenPositionPriorityUiHelper() {
    }

    public static class BadgeData {
        private final String html;
        private final String description;

        public BadgeData(String html, String description) {
            this.html = html;
            this.description = description;
        }

        public String getHtml() {
            return html;
        }

        public String getDescription() {
            return description;
        }
    }

    public static BadgeData getPriorityBadge(Integer priority, int sizePx) {
        return getPriorityBadge(priority, sizePx, null);
    }

    /**
     * Формирует HTML-разметку SVG-значка и всплывающую подсказку для числового приоритета
     * с поддержкой локализации через MessageBundle.
     *
     * @param priority      числовой приоритет (-2: На проверку, -1: Черновик, 0: Приостановлена, 1: Низкий, 2: Обычный, 3: Высокий, 4: Критический)
     * @param sizePx        размер значка в пикселях (22 для таблиц реестров, 24 для форм редактирования)
     * @param messageBundle опциональный бандл локализации для экранных подсказок
     * @return объект с HTML-значком и описанием
     */
    public static BadgeData getPriorityBadge(Integer priority, int sizePx, MessageBundle messageBundle) {
        int clampedSize = Math.max(14, Math.min(sizePx, 64));
        int p = priority != null ? priority : 2;
        int iconSize = clampedSize >= 24 ? 12 : (clampedSize >= 22 ? 11 : 9);
        String html;
        String desc;

        switch (p) {
            case 4: // Критический
                desc = resolveMessage(messageBundle, "msgPriorityTooltipCritical",
                        "<b>Критический приоритет</b><br>Требует немедленного внимания и первоочередного подбора кандидатов.");
                html = "<div style='display: flex; align-items: center; justify-content: center; width: " + clampedSize + "px; height: " + clampedSize + "px; border-radius: 50%; background: rgba(239, 68, 68, 0.18); border: 1.5px solid #ef4444; box-shadow: 0 0 6px rgba(239, 68, 68, 0.4); margin: 0 auto;'>"
                        + "<svg width='" + iconSize + "' height='" + iconSize + "' viewBox='0 0 24 24' fill='#ef4444'><path d='M12 2L1 21h22L12 2zm0 3.5L20.3 19H3.7L12 5.5zM11 10v4h2v-4h-2zm0 6v2h2v-2h-2z'/></svg>"
                        + "</div>";
                break;
            case 3: // Высокий
                desc = resolveMessage(messageBundle, "msgPriorityTooltipHigh",
                        "<b>Высокий приоритет</b><br>Повышенная срочность закрытия позиции.");
                html = "<div style='display: flex; align-items: center; justify-content: center; width: " + clampedSize + "px; height: " + clampedSize + "px; border-radius: 50%; background: rgba(245, 158, 11, 0.18); border: 1.5px solid #f59e0b; box-shadow: 0 0 5px rgba(245, 158, 11, 0.35); margin: 0 auto;'>"
                        + "<svg width='" + iconSize + "' height='" + iconSize + "' viewBox='0 0 24 24' fill='#f59e0b'><path d='M12 4l-7 7h4v8h6v-8h4z'/></svg>"
                        + "</div>";
                break;
            case 1: // Низкий
                desc = resolveMessage(messageBundle, "msgPriorityTooltipLow",
                        "<b>Низкий приоритет</b><br>Плановый фоновый подбор.");
                html = "<div style='display: flex; align-items: center; justify-content: center; width: " + clampedSize + "px; height: " + clampedSize + "px; border-radius: 50%; background: rgba(59, 130, 246, 0.15); border: 1.5px solid #3b82f6; margin: 0 auto;'>"
                        + "<svg width='" + iconSize + "' height='" + iconSize + "' viewBox='0 0 24 24' fill='#3b82f6'><path d='M12 20l7-7h-4V5H9v8H5z'/></svg>"
                        + "</div>";
                break;
            case 0: // Приостановлена
                desc = resolveMessage(messageBundle, "msgPriorityTooltipPaused",
                        "<b>Приостановлена</b><br>Работа по вакансии временно приостановлена заказчиком.");
                html = "<div style='display: flex; align-items: center; justify-content: center; width: " + clampedSize + "px; height: " + clampedSize + "px; border-radius: 50%; background: rgba(107, 114, 128, 0.15); border: 1.5px solid #6b7280; margin: 0 auto;'>"
                        + "<svg width='" + (iconSize - 2) + "' height='" + (iconSize - 2) + "' viewBox='0 0 24 24' fill='#6b7280'><path d='M6 19h4V5H6v14zm8-14v14h4V5h-4z'/></svg>"
                        + "</div>";
                break;
            case -1: // Черновик
                desc = resolveMessage(messageBundle, "msgPriorityTooltipDraft",
                        "<b>Черновик</b><br>Вакансия находится в стадии формирования и не опубликована.");
                html = "<div style='display: flex; align-items: center; justify-content: center; width: " + clampedSize + "px; height: " + clampedSize + "px; border-radius: 50%; background: rgba(148, 163, 184, 0.15); border: 1.5px dashed #94a3b8; margin: 0 auto;'>"
                        + "<svg width='" + (iconSize - 1) + "' height='" + (iconSize - 1) + "' viewBox='0 0 24 24' fill='#94a3b8'><path d='M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z'/></svg>"
                        + "</div>";
                break;
            case -2: // На проверку
                desc = resolveMessage(messageBundle, "msgPriorityTooltipUnderReview",
                        "<b>На проверку</b><br>Вакансия импортирована через умную загрузку и ожидает проверки перед публикацией.");
                html = "<div style='display: flex; align-items: center; justify-content: center; width: " + clampedSize + "px; height: " + clampedSize + "px; border-radius: 50%; background: rgba(139, 92, 246, 0.18); border: 1.5px solid #8b5cf6; box-shadow: 0 0 6px rgba(139, 92, 246, 0.35); margin: 0 auto;'>"
                        + "<svg width='" + iconSize + "' height='" + iconSize + "' viewBox='0 0 24 24' fill='#8b5cf6'><path d='M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z'/></svg>"
                        + "</div>";
                break;
            case 2: // Обычный
            default:
                desc = resolveMessage(messageBundle, "msgPriorityTooltipNormal",
                        "<b>Обычный приоритет</b><br>Стандартный рабочий приоритет вакансии.");
                html = "<div style='display: flex; align-items: center; justify-content: center; width: " + clampedSize + "px; height: " + clampedSize + "px; border-radius: 50%; background: rgba(16, 185, 129, 0.15); border: 1.5px solid #10b981; margin: 0 auto;'>"
                        + "<svg width='" + (iconSize - 3) + "' height='" + (iconSize - 3) + "' viewBox='0 0 24 24' fill='#10b981'><circle cx='12' cy='12' r='10'/></svg>"
                        + "</div>";
                break;
        }

        return new BadgeData(html, desc);
    }

    private static String resolveMessage(MessageBundle messageBundle, String key, String defaultVal) {
        if (messageBundle != null) {
            try {
                String msg = messageBundle.getMessage(key);
                if (msg != null && !msg.isEmpty() && !msg.equals(key)) {
                    return msg;
                }
            } catch (Exception e) {
                log.debug("Не удалось получить сообщение для ключа {}: {}", key, e.getMessage());
            }
        }
        return defaultVal;
    }
}
