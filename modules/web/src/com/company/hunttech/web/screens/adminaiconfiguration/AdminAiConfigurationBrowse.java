package com.company.hunttech.web.screens.adminaiconfiguration;

import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.service.AiCredentialService;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_AdminAiConfiguration.browse")
@UiDescriptor("admin-ai-configuration-browse.xml")
@LookupComponent("adminConfigurationsTable")
@LoadDataBeforeShow
public class AdminAiConfigurationBrowse extends StandardLookup<AdminAiConfiguration> {
    @Inject
    private DataGrid<AdminAiConfiguration> adminConfigurationsTable;
    @Inject
    private Button testBtn;
    @Inject
    private AiCredentialService aiCredentialService;
    @Inject
    private CollectionLoader<AdminAiConfiguration> adminConfigurationsDl;
    @Inject
    private Notifications notifications;

    @Subscribe
    public void onInit(InitEvent event) {
        testBtn.setEnabled(false);
        adminConfigurationsTable.addSelectionListener(selectionEvent ->
                testBtn.setEnabled(adminConfigurationsTable.getSingleSelected() != null));
    }

    /**
     * Тест выполняется целиком на middleware по id записи: Web Client не загружает
     * даже шифротекст API key, а после проверки перечитывает только safe browse-view.
     */
    public void testSelectedConnection() {
        AdminAiConfiguration selected = adminConfigurationsTable.getSingleSelected();
        if (selected == null) {
            return;
        }
        try {
            aiCredentialService.testAdminConnection(selected.getId());
            adminConfigurationsDl.load();
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Корпоративное AI-подключение работает")
                    .show();
        } catch (RuntimeException e) {
            adminConfigurationsDl.load();
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка корпоративного AI-подключения")
                    .withDescription(e.getMessage())
                    .show();
        }
    }
}
