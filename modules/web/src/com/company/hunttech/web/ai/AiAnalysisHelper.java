package com.company.hunttech.web.ai;

import com.company.hunttech.service.AiAnalysisService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiControllerUtils;

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
        /*
         * Dialogs и Notifications создаются внутренней UI-инфраструктурой CUBA,
         * а не Spring-контейнером. Получаем их из ScreenContext текущего экрана,
         * иначе AppBeans.get(...) завершится NoSuchBeanDefinitionException.
         */
        Notifications notifications = UiControllerUtils.getScreenContext(screen).getNotifications();
        Dialogs dialogs = UiControllerUtils.getScreenContext(screen).getDialogs();

        if (entity == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Нет данных для анализа")
                    .show();
            return;
        }

        /*
         * Не выполняем повторную web-tier загрузку с View.LOCAL: core-сервис
         * самостоятельно перезагружает сущность специализированным analysis-view
         * со всеми связями, нужными placeholder-экстракторам.
         */
        AiAnalysisService service = (AiAnalysisService) AppBeans.get(AiAnalysisService.NAME);

        try {
            String result = service.analyze(entity, promptCode);

            dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                    .withCaption("AI-анализ")
                    .withMessage(result)
                    .withWidth("700px")
                    .withHeight("500px")
                    .show();
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка AI-анализа")
                    .withDescription(e.getMessage())
                    .show();
        }
    }
}
