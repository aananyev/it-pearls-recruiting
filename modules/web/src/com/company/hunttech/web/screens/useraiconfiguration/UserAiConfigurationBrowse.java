package com.company.hunttech.web.screens.useraiconfiguration;

import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.service.HrmAiService;
import com.company.hunttech.web.util.AiOperationNotifier;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Label;
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
    private Button openEditCardBtn;
    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Label<String> detailPrimaryBadge;
    @Inject
    private Label<String> detailActive;
    @Inject
    private Label<String> detailUser;
    @Inject
    private Label<String> detailPriority;
    @Inject
    private Label<String> detailContext;
    @Inject
    private Label<String> detailRetries;
    @Inject
    private Label<String> detailProvider;
    @Inject
    private Label<String> detailModel;
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;
    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private Messages messages;
    @Inject
    private ScreenBuilders screenBuilders;

    @Subscribe
    public void onInit(InitEvent event) {
        testBtn.setEnabled(false);
        openEditCardBtn.setEnabled(false);

        userAiConfigurationsTable.addSelectionListener(e -> {
            UserAiConfiguration selected = userAiConfigurationsTable.getSingleSelected();
            boolean hasSelection = selected != null;
            testBtn.setEnabled(hasSelection);
            openEditCardBtn.setEnabled(hasSelection);
            updateSidebar(selected);
        });

        openEditCardBtn.addClickListener(clickEvent -> openSelectedEditor());
    }

    private void updateSidebar(UserAiConfiguration selected) {
        if (selected != null) {
            String login = selected.getUser() != null ? selected.getUser().getLogin() : "";
            detailTitle.setValue(!login.isEmpty() ? login : messages.getMessage(getClass(), "sidebarSubtitle"));
            detailSubtitle.setValue(selected.getProviderCode() != null ? selected.getProviderCode() : "-");

            boolean isPrimary = Boolean.TRUE.equals(selected.getIsPrimary());
            detailPrimaryBadge.setValue(isPrimary ? ("★ " + messages.getMessage(getClass(), "primaryBadge")) : messages.getMessage(getClass(), "nonPrimaryBadge"));

            boolean isActive = Boolean.TRUE.equals(selected.getIsActive());
            detailActive.setValue(isActive ? ("🟢 " + messages.getMessage(getClass(), "statusActive")) : ("⚪ " + messages.getMessage(getClass(), "statusInactive")));
            detailUser.setValue(login.isEmpty() ? "-" : login);
            detailPriority.setValue(selected.getPriority() != null ? String.valueOf(selected.getPriority()) : "0");
            detailContext.setValue(selected.getMaxContextTokens() != null ? String.valueOf(selected.getMaxContextTokens()) : "-");
            detailRetries.setValue(selected.getMaxRetries() != null ? String.valueOf(selected.getMaxRetries()) : "3");

            detailProvider.setValue(selected.getProviderCode() != null ? selected.getProviderCode() : "-");
            detailModel.setValue(selected.getDefaultModelName() != null ? selected.getDefaultModelName() : "-");
        } else {
            detailTitle.setValue(messages.getMessage(getClass(), "sidebarDefaultTitle"));
            detailSubtitle.setValue(messages.getMessage(getClass(), "sidebarSubtitle"));
            detailPrimaryBadge.setValue("");
            detailActive.setValue("-");
            detailUser.setValue("-");
            detailPriority.setValue("-");
            detailContext.setValue("-");
            detailRetries.setValue("-");
            detailProvider.setValue("-");
            detailModel.setValue("-");
        }
    }

    private void openSelectedEditor() {
        UserAiConfiguration selected = userAiConfigurationsTable.getSingleSelected();
        if (selected != null) {
            screenBuilders.editor(userAiConfigurationsTable)
                    .editEntity(selected)
                    .withScreenClass(UserAiConfigurationEdit.class)
                    .show();
        }
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
