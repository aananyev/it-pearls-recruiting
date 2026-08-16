package com.company.hunttech.web.screens;

import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

/**
 * Модальный диалог «крутилка» на время выполнения фоновой AI-операции:
 * сообщение + недетерминированный {@code ProgressBar} (indeterminate).
 *
 * <p>Показывается ПОСЛЕ стартовой нотификации {@code AiOperationNotifier.showStarted(...)}
 * и закрывается в завершающем обработчике {@code BackgroundTask}
 * ({@code done} / {@code handleException} / {@code handleTimeoutException}) —
 * порядок по контракту пользовательской нотификации: нотификация о начале →
 * «крутилка» → нотификация об итоге с кратким отчётом.</p>
 *
 * <p>Открытие/закрытие — через {@code AiOperationNotifier.showProgress(...)} /
 * {@code AiOperationNotifier.closeProgress(...)} (единая точка AI-фидбека).</p>
 */
@UiController("hunttech_AiProgressDialog")
@UiDescriptor("ai-progress-dialog.xml")
public class AiProgressDialog extends Screen {

    @Inject
    private Label<String> messageLabel;

    /**
     * Задаёт текст под «крутилкой», например «Анализ навыков резюме…».
     */
    public void setMessage(String message) {
        messageLabel.setValue(message);
    }
}
