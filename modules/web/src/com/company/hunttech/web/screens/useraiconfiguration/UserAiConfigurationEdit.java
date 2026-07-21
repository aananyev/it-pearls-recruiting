package com.company.hunttech.web.screens.useraiconfiguration;

import com.company.hunttech.entity.UserAiConfiguration;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("hunttech_UserAiConfiguration.edit")
@UiDescriptor("user-ai-configuration-edit.xml")
@EditedEntityContainer("userAiConfigurationDc")
@LoadDataBeforeShow
public class UserAiConfigurationEdit extends StandardEditor<UserAiConfiguration> {

    /**
     * Модели для быстрого первого подключения. Пользователь может заменить
     * значение вручную, если в его тарифе доступна другая модель провайдера.
     */
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

    private User parentUser;
    private String lastAutomaticallyAppliedModel;

    public void setParentUser(User parentUser) {
        this.parentUser = parentUser;
    }

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

        providerCodeField.addValueChangeListener(valueChangeEvent ->
                setDefaultModelForProvider(valueChangeEvent.getValue()));
    }

    @Subscribe
    public void onInitEntity(InitEntityEvent<UserAiConfiguration> event) {
        if (parentUser != null) {
            event.getEntity().setUser(parentUser);
        }
        if (event.getEntity().getIsActive() == null) {
            event.getEntity().setIsActive(true);
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (parentUser != null && PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setUser(parentUser);
        }
        setDefaultModelForProvider(getEditedEntity().getProviderCode());
    }

    private void setDefaultModelForProvider(String providerCode) {
        /*
         * Модель подставляется в пустое поле и заменяется при переключении
         * провайдера только тогда, когда предыдущее значение также было
         * подставлено автоматически. Введённое пользователем имя модели
         * сохраняется и никогда не затирается выбором в списке.
         */
        String defaultModel = DEFAULT_MODELS.get(providerCode);
        String currentModel = defaultModelNameField.getValue();
        boolean canApplyDefault = !isConfigured(currentModel)
                || currentModel.equals(lastAutomaticallyAppliedModel);
        if (defaultModel != null && canApplyDefault) {
            defaultModelNameField.setValue(defaultModel);
            lastAutomaticallyAppliedModel = defaultModel;
        } else {
            lastAutomaticallyAppliedModel = null;
        }
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
