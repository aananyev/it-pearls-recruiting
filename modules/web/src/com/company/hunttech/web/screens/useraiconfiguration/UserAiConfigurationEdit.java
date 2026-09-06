package com.company.hunttech.web.screens.useraiconfiguration;

import com.company.hunttech.ai.AiProviderCatalog;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.service.AiCredentialService;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.PasswordField;
import com.haulmont.cuba.gui.screen.StandardEditor.BeforeCommitChangesEvent;
import com.haulmont.cuba.gui.screen.StandardEditor.InitEntityEvent;
import com.haulmont.cuba.gui.screen.Screen.InitEvent;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.Screen.BeforeShowEvent;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;

@UiController("hunttech_UserAiConfiguration.edit")
@UiDescriptor("user-ai-configuration-edit.xml")
@EditedEntityContainer("userAiConfigurationDc")
@LoadDataBeforeShow
public class UserAiConfigurationEdit extends StandardEditor<UserAiConfiguration> {

    @Inject
    private LookupField<String> providerCodeField;
    @Inject
    private TextField<String> defaultModelNameField;
    @Inject
    private PasswordField apiKeyField;
    @Inject
    private AiCredentialService aiCredentialService;

    private User parentUser;
    private String lastAutomaticallyAppliedModel;

    public void setParentUser(User parentUser) {
        this.parentUser = parentUser;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        /*
         * Каталог общий с корпоративной AI-формой. Это исключает расхождение
         * provider codes/default models между двумя путями настройки AI.
         */
        providerCodeField.setOptionsMap(AiProviderCatalog.getProviderOptions());
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
        apiKeyField.setValue(null);
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        String newSecret = apiKeyField.getValue();
        if (isConfigured(newSecret)) {
            try {
                getEditedEntity().setApiKeyEncrypted(aiCredentialService.encryptUserSecret(newSecret));
                apiKeyField.setValue(null);
            } catch (RuntimeException e) {
                event.preventCommit();
                return;
            }
        }
        // Never write plaintext, including a legacy value accidentally loaded
        // by an older view or supplied by a previous editor implementation.
        getEditedEntity().setApiKey(null);
    }

    private void setDefaultModelForProvider(String providerCode) {
        /*
         * Модель подставляется в пустое поле и заменяется при переключении
         * провайдера только тогда, когда предыдущее значение также было
         * подставлено автоматически. Введённое пользователем имя модели
         * сохраняется и никогда не затирается выбором в списке.
         */
        String defaultModel = AiProviderCatalog.getDefaultModel(providerCode);
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
