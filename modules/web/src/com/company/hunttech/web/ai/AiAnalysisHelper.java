package com.company.hunttech.web.ai;

import com.company.hunttech.service.AiAnalysisService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.screen.Screen;

/**
 * Статический helper для вызова AI-анализа из любой формы.
 *
 * Использование (3 строки на форму):
 * <pre>
 *   XML:  &lt;button id="aiBtn" caption="AI-анализ" invoke="onAiAnalysisClick"/&gt;
 *   Java: public void onAiAnalysisClick() {
 *             AiAnalysisHelper.analyze(this, getEditedEntity(), "RESUME_ANALYSIS");
 *         }
 * </pre>
 */
public final class AiAnalysisHelper {

    private AiAnalysisHelper() {
    }

    /**
     * @param screen     текущий экран (для показа диалогов/нотификаций)
     * @param entity     сущность для анализа (null → предупреждение)
     * @param promptCode код промпта из AiPromptTemplate
     */
    public static void analyze(Screen screen, Entity entity, String promptCode) {
        if (entity == null) {
            AppBeans.get(Notifications.class)
                    .create(Notifications.NotificationType.WARNING)
                    .withCaption("Нет данных для анализа")
                    .show();
            return;
        }

        // Перезагружаем с View.LOCAL — browse-view может не содержать всех полей
        DataManager dm = AppBeans.get(DataManager.class);
        Entity full = dm.load(LoadContext.create(entity.getClass())
                .setId(entity.getId())
                .setView(View.LOCAL));

        AiAnalysisService svc = (AiAnalysisService) AppBeans.get("hunttech_AiAnalysisService");

        try {
            String result = svc.analyze(full, promptCode);

            Dialogs dialogs = AppBeans.get(Dialogs.class);
            dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                    .withCaption("AI-анализ")
                    .withMessage(result)
                    .withWidth("700px")
                    .withHeight("500px")
                    .show();
        } catch (Exception e) {
            AppBeans.get(Notifications.class)
                    .create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка AI-анализа")
                    .withDescription(e.getMessage())
                    .show();
        }
    }
}
