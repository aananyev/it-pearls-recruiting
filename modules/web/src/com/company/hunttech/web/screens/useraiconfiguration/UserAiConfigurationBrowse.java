package com.company.hunttech.web.screens.useraiconfiguration;

import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.service.HrmAiService;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Table;
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

        // Перезагружаем с edit-view: browse-view не содержит apiKey (секретное поле)
        UserAiConfiguration full = dataManager.load(LoadContext.create(UserAiConfiguration.class)
                .setId(selected.getId())
                .setView(View.LOCAL));

        HrmAiService aiService = (HrmAiService) AppBeans.get("hunttech_HrmAiService");
        try {
            aiService.testConnection(full);
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("AI-подключение успешно")
                    .withDescription("Провайдер «" + full.getProviderCode() + "» отвечает корректно.")
                    .show();
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка AI-подключения")
                    .withDescription(e.getMessage())
                    .show();
        }
    }
}
