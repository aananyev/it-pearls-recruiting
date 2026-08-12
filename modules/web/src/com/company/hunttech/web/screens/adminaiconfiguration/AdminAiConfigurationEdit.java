package com.company.hunttech.web.screens.adminaiconfiguration;

import com.company.hunttech.ai.AiProviderCatalog;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.service.AiCredentialService;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.PasswordField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.StandardEditor.BeforeCommitChangesEvent;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.StandardEditor.InitEntityEvent;
import com.haulmont.cuba.gui.screen.Screen.InitEvent;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_AdminAiConfiguration.edit")
@UiDescriptor("admin-ai-configuration-edit.xml")
@EditedEntityContainer("adminConfigurationDc")
@LoadDataBeforeShow
public class AdminAiConfigurationEdit extends StandardEditor<AdminAiConfiguration> {

    @Inject
    private LookupField<String> providerCodeField;
    @Inject
    private TextField<String> defaultModelNameField;
    @Inject
    private PasswordField apiKeyInput;
    @Inject
    private AiCredentialService aiCredentialService;
    @Inject
    private Notifications notifications;
    @Inject
    private Button mainNav;
    @Inject
    private Button connectionNav;
    @Inject
    private Button securityNav;

    @Subscribe
    public void onInit(InitEvent event) {
        // Единый каталог используется personal/admin AI Edit-формами.
        providerCodeField.setOptionsMap(AiProviderCatalog.getProviderOptions());
        providerCodeField.addValueChangeListener(event1 -> applyDefaultModel(event1.getValue()));
    }

    @Subscribe
    public void onInitEntity(InitEntityEvent<AdminAiConfiguration> event) {
        event.getEntity().setActive(true);
        event.getEntity().setPriority(0);
    }

    /**
     * Перед DataContext commit plaintext из unbound PasswordField шифруется middleware-сервисом.
     * Пустое поле при редактировании сохраняет прежний ciphertext и никогда его не показывает.
     */
    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        String newSecret = apiKeyInput.getValue();
        if (isConfigured(newSecret)) {
            try {
                getEditedEntity().setApiKeyEncrypted(aiCredentialService.encryptAdminSecret(newSecret));
                apiKeyInput.setValue(null);
            } catch (RuntimeException e) {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption("API-ключ не сохранён")
                        .withDescription(e.getMessage())
                        .show();
                event.preventCommit();
                return;
            }
        }
        if (!isConfigured(getEditedEntity().getApiKeyEncrypted())) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Введите API-ключ")
                    .show();
            event.preventCommit();
        }
    }

    public void focusMainSection() {
        providerCodeField.focus();
        setActiveNavigation(mainNav);
    }

    public void focusConnectionSection() {
        defaultModelNameField.focus();
        setActiveNavigation(connectionNav);
    }

    public void focusSecuritySection() {
        apiKeyInput.focus();
        setActiveNavigation(securityNav);
    }

    private void applyDefaultModel(String providerCode) {
        if (!isConfigured(defaultModelNameField.getValue())) {
            String defaultModel = AiProviderCatalog.getDefaultModel(providerCode);
            if (defaultModel != null) {
                defaultModelNameField.setValue(defaultModel);
            }
        }
    }

    private void setActiveNavigation(Button activeButton) {
        Button[] buttons = {mainNav, connectionNav, securityNav};
        for (Button button : buttons) {
            if (button == activeButton) {
                button.addStyleName("label-nav-item-active");
            } else {
                button.removeStyleName("label-nav-item-active");
            }
        }
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
