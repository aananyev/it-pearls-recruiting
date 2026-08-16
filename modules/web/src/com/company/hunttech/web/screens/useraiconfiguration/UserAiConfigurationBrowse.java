package com.company.hunttech.web.screens.useraiconfiguration;

import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.service.HrmAiService;
import com.company.hunttech.web.util.AiOperationNotifier;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_UserAiConfiguration.browse")
@UiDescriptor("user-ai-configuration-browse.xml")
@LoadDataBeforeShow
public class UserAiConfigurationBrowse extends StandardLookup<UserAiConfiguration> {

    @Inject
    private Table<UserAiConfiguration> userAiConfigurationsTable;
    @Inject
    private Button testBtn;
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;
    @Inject
    private BackgroundWorker backgroundWorker;

    @Subscribe
    public void onInit(InitEvent event) {
        testBtn.setEnabled(false);
        userAiConfigurationsTable.addSelectionListener(e ->
                testBtn.setEnabled(!userAiConfigurationsTable.getSelected().isEmpty()));
    }

    public void onTestBtnClick() {
        UserAiConfiguration selected = userAiConfigurationsTable.getSingleSelected();
        if (selected == null) {
            return;
        }

        // Контракт пользовательской нотификации («AI-нотификации 2 раза»): старт
        // операции — исчезающая TRAY-нотификация, показывается СРАЗУ (до «крутилки»);
        // по завершении — итоговая с моделью и собственником API (личный ключ).
        AiOperationNotifier.showStarted(notifications, "Проверка AI-подключения…", null);

        // Проверка выполняется в фоне (BackgroundTask) — эталонный паттерн
        // AI-нотификаций (CandidateCVEdit «Сканировать навыки»): нотификация о старте
        // → «крутилка» → итоговая нотификация. При синхронном вызове на UI-потоке
        // обе нотификации (старт и итог) пришли бы одной пачкой в конце запроса.
        final UserAiConfiguration configuration = selected;
        testBtn.setEnabled(false);
        final Screen progressDialog = AiOperationNotifier.showProgress(this, "Проверка AI-подключения…");

        BackgroundTask<Integer, AiExecutionResult> task =
                new BackgroundTask<Integer, AiExecutionResult>(60, this) {
                    @Override
                    public AiExecutionResult run(TaskLifeCycle<Integer> taskLifeCycle) {
                        // Перезагружаем с edit-view: browse-view не содержит apiKey (секретное поле)
                        UserAiConfiguration full = dataManager.load(LoadContext.create(UserAiConfiguration.class)
                                .setId(configuration.getId())
                                .setView(View.LOCAL));
                        HrmAiService aiService = (HrmAiService) AppBeans.get("hunttech_HrmAiService");
                        return aiService.testConnection(full);
                    }

                    @Override
                    public void done(AiExecutionResult result) {
                        AiOperationNotifier.closeProgress(progressDialog);
                        testBtn.setEnabled(true);
                        // Контракт пользовательской нотификации: реальный AI-вызов несёт метаданные
                        // (модель, провайдер, собственник API = личный ключ пользователя) и завершается
                        // исчезающей TRAY-нотификацией с указанием «какая модель что делала».
                        AiOperationNotifier.show(notifications, result,
                                "AI-подключение успешно",
                                "Провайдер «" + result.getProviderCode() + "» отвечает корректно.");
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        AiOperationNotifier.closeProgress(progressDialog);
                        testBtn.setEnabled(true);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Ошибка AI-подключения")
                                .withDescription(ex.getMessage())
                                .show();
                        return true;
                    }

                    @Override
                    public boolean handleTimeoutException() {
                        AiOperationNotifier.closeProgress(progressDialog);
                        testBtn.setEnabled(true);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Ошибка AI-подключения")
                                .withDescription("Проверка подключения превысила допустимое время выполнения.")
                                .show();
                        return true;
                    }
                };
        backgroundWorker.handle(task).execute();
    }
}
