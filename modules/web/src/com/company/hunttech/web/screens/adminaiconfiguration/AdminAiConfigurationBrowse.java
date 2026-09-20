package com.company.hunttech.web.screens.adminaiconfiguration;

import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.service.AiCredentialService;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.StreamResource;
import com.haulmont.cuba.gui.components.ThemeResource;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;

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
    private Button openEditCardBtn;
    @Inject
    private Image logoPreview;
    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Label<String> detailModelBadge;
    @Inject
    private Label<String> detailActive;
    @Inject
    private Label<String> detailFree;
    @Inject
    private Label<String> detailPriority;
    @Inject
    private Label<String> detailContext;
    @Inject
    private Label<String> detailRetries;
    @Inject
    private Label<String> detailLastStatus;
    @Inject
    private Label<String> detailProvider;
    @Inject
    private Label<String> detailModel;
    @Inject
    private AiCredentialService aiCredentialService;
    @Inject
    private CollectionLoader<AdminAiConfiguration> adminConfigurationsDl;
    @Inject
    private Notifications notifications;
    @Inject
    private Messages messages;
    @Inject
    private ScreenBuilders screenBuilders;

    @Subscribe
    public void onInit(InitEvent event) {
        testBtn.setEnabled(false);
        openEditCardBtn.setEnabled(false);
        setDefaultLogo();

        adminConfigurationsTable.addSelectionListener(selectionEvent -> {
            AdminAiConfiguration selected = adminConfigurationsTable.getSingleSelected();
            testBtn.setEnabled(selected != null);
            openEditCardBtn.setEnabled(selected != null);
            updateSidebar(selected);
        });

        openEditCardBtn.addClickListener(clickEvent -> openSelectedEditor());
    }

    private void updateSidebar(AdminAiConfiguration selected) {
        if (selected != null) {
            String name = selected.getName() != null ? selected.getName() : "";
            detailTitle.setValue(!name.isEmpty() ? name : messages.getMessage(getClass(), "sidebarSubtitle"));
            detailSubtitle.setValue(selected.getProviderCode() != null ? selected.getProviderCode() : "-");

            boolean isFree = Boolean.TRUE.equals(selected.getFreeModel());
            detailModelBadge.setValue(isFree ? ("★ " + messages.getMessage(getClass(), "freeModelBadge")) : messages.getMessage(getClass(), "paidModelBadge"));

            boolean isActive = Boolean.TRUE.equals(selected.getActive());
            detailActive.setValue(isActive ? ("🟢 " + messages.getMessage(getClass(), "statusActive")) : ("⚪ " + messages.getMessage(getClass(), "statusInactive")));
            detailFree.setValue(isFree ? "Да (бесплатная)" : "Платная");
            detailPriority.setValue(selected.getPriority() != null ? String.valueOf(selected.getPriority()) : "0");
            detailContext.setValue(selected.getMaxContextTokens() != null ? String.valueOf(selected.getMaxContextTokens()) : "-");
            detailRetries.setValue(selected.getMaxRetries() != null ? String.valueOf(selected.getMaxRetries()) : "3");
            detailLastStatus.setValue(selected.getLastTestStatus() != null ? selected.getLastTestStatus() : "-");

            detailProvider.setValue(selected.getProviderCode() != null ? selected.getProviderCode() : "-");
            detailModel.setValue(selected.getDefaultModelName() != null ? selected.getDefaultModelName() : "-");

            updateLogoPreview(selected.getLogoImage());
        } else {
            detailTitle.setValue(messages.getMessage(getClass(), "sidebarDefaultTitle"));
            detailSubtitle.setValue(messages.getMessage(getClass(), "sidebarSubtitle"));
            detailModelBadge.setValue("");
            detailActive.setValue("-");
            detailFree.setValue("-");
            detailPriority.setValue("-");
            detailContext.setValue("-");
            detailRetries.setValue("-");
            detailLastStatus.setValue("-");
            detailProvider.setValue("-");
            detailModel.setValue("-");
            setDefaultLogo();
        }
    }

    private void updateLogoPreview(byte[] logoBytes) {
        if (logoPreview == null) {
            return;
        }
        if (logoBytes != null && logoBytes.length > 0) {
            logoPreview.setSource(StreamResource.class)
                    .setStreamSupplier(() -> new ByteArrayInputStream(logoBytes));
        } else {
            setDefaultLogo();
        }
    }

    private void setDefaultLogo() {
        if (logoPreview != null) {
            logoPreview.setSource(ThemeResource.class)
                    .setPath("icons/ai/admin-ai-configuration.png");
        }
    }

    private void openSelectedEditor() {
        AdminAiConfiguration selected = adminConfigurationsTable.getSingleSelected();
        if (selected != null) {
            screenBuilders.editor(adminConfigurationsTable)
                    .editEntity(selected)
                    .withScreenClass(AdminAiConfigurationEdit.class)
                    .show();
        }
    }

    public void testSelectedConnection() {
        AdminAiConfiguration selected = adminConfigurationsTable.getSingleSelected();
        if (selected == null) {
            return;
        }
        try {
            aiCredentialService.testAdminConnection(selected.getId());
            adminConfigurationsDl.load();
            updateSidebar(adminConfigurationsTable.getSingleSelected());
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Корпоративное AI-подключение работает")
                    .show();
        } catch (RuntimeException e) {
            adminConfigurationsDl.load();
            updateSidebar(adminConfigurationsTable.getSingleSelected());
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка корпоративного AI-подключения")
                    .withDescription(e.getMessage())
                    .show();
        }
    }
}

