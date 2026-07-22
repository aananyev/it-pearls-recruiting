package com.company.hunttech.web.screens.useraiconfiguration;

import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.service.HrmAiService;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.model.CollectionLoader;
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
    private Button makeCurrentBtn;
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;
    @Inject
    private HrmAiService hrmAiService;
    @Inject
    private CollectionLoader<UserAiConfiguration> userAiConfigurationsDl;
    @Inject
    private MessageBundle messageBundle;

    @Subscribe
    public void onInit(InitEvent event) {
        updateActionState();
        userAiConfigurationsTable.addSelectionListener(selectionEvent -> updateActionState());
    }

    /**
     * Делает выбранную конфигурацию единственной текущей для пользователя.
     * Переключение выполняется в core-сервисе одной транзакцией, чтобы кнопки
     * AI-анализа не могли получить неоднозначный выбор провайдера.
     */
    public void onMakeCurrentBtnClick() {
        UserAiConfiguration selected = userAiConfigurationsTable.getSingleSelected();
        if (selected == null) {
            return;
        }

        try {
            hrmAiService.setCurrentConfiguration(selected.getId());
            userAiConfigurationsDl.load();
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption(String.format(messageBundle.getMessage("currentProviderChanged"),
                            selected.getProviderCode()))
                    .show();
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("connectionError"))
                    .withDescription(e.getMessage())
                    .show();
        }
    }

    public void onTestBtnClick() {
        UserAiConfiguration selected = userAiConfigurationsTable.getSingleSelected();
        if (selected == null) {
            return;
        }

        // Browse-view намеренно не содержит секретный apiKey; для теста перезагружаем выбранную строку edit-view.
        UserAiConfiguration full = dataManager.load(UserAiConfiguration.class)
                .id(selected.getId())
                .view("userAiConfiguration-edit-view")
                .one();

        try {
            hrmAiService.testConnection(full);
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption(messageBundle.getMessage("connectionSuccess"))
                    .withDescription(String.format(messageBundle.getMessage("connectionSuccessDescription"),
                            full.getProviderCode()))
                    .show();
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("connectionError"))
                    .withDescription(e.getMessage())
                    .show();
        }
    }

    private void updateActionState() {
        UserAiConfiguration selected = userAiConfigurationsTable.getSingleSelected();
        testBtn.setEnabled(selected != null);
        makeCurrentBtn.setEnabled(selected != null && !Boolean.TRUE.equals(selected.getIsActive()));
    }
}
