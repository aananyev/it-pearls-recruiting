package com.company.hunttech.web.screens.adminaiconfiguration;

import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.service.AiCredentialService;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.PasswordField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("hunttech_AdminAiConfiguration.edit")
@UiDescriptor("admin-ai-configuration-edit.xml")
@EditedEntityContainer("adminConfigurationDc")
@LoadDataBeforeShow
public class AdminAiConfigurationEdit extends StandardEditor<AdminAiConfiguration> {
    private static final Map<String, String> DEFAULT_MODELS = new LinkedHashMap<>();

    static {
        DEFAULT_MODELS.put("yandex", "yandexgpt/latest");
        DEFAULT_MODELS.put("gigachat", "GigaChat");
        DEFAULT_MODELS.put("openai", "gpt-4o");
        DEFAULT_MODELS.put("anthropic", "claude-sonnet-4-6");
        DEFAULT_MODELS.put("gemini", "gemini-3.5-flash");
        DEFAULT_MODELS.put("grok", "grok-4.3");
        DEFAULT_MODELS.put("deepseek", "deepseek-v4-flash");
        DEFAULT_MODELS.put("qwen", "qwen-plus");
        DEFAULT_MODELS.put("kimi", "kimi-k2.5");
        DEFAULT_MODELS.put("glm", "glm-5.1");
    }

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
        Map<String, String> providers = new LinkedHashMap<>();
        providers.put("YandexGPT", "yandex");
        providers.put("GigaChat", "gigachat");
        providers.put("OpenAI", "openai");
        providers.put("Anthropic Claude", "anthropic");
        providers.put("Google Gemini", "gemini");
        providers.put("xAI Grok", "grok");
        providers.put("DeepSeek", "deepseek");
        providers.put("Alibaba Qwen", "qwen");
        providers.put("Moonshot Kimi", "kimi");
        providers.put("Z.AI GLM", "glm");
        providerCodeField.setOptionsMap(providers);
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
            String defaultModel = DEFAULT_MODELS.get(providerCode);
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
