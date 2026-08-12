package com.company.hunttech.web.screens.useraifunctionoverride;

import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AiExecutionPolicy;
import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.company.hunttech.entity.ai.UserAiFunctionOverride;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;

@UiController("hunttech_UserAiFunctionOverride.edit")
@UiDescriptor("user-ai-function-override-edit.xml")
@EditedEntityContainer("overrideDc")
@LoadDataBeforeShow
public class UserAiFunctionOverrideEdit extends StandardEditor<UserAiFunctionOverride> {
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private CollectionLoader<UserAiConfiguration> userConfigurationsDl;
    @Inject
    private LookupPickerField<AiFunctionConfiguration> aiFunctionField;
    @Inject
    private LookupPickerField<UserAiConfiguration> userAiConfigurationField;
    @Inject
    private TextField<String> modelNameField;
    @Inject
    private Notifications notifications;
    @Inject
    private Button mainNav;
    @Inject
    private Button modelNav;

    @Subscribe
    public void onInitEntity(InitEntityEvent<UserAiFunctionOverride> event) {
        event.getEntity().setUser(userSessionSource.getUserSession().getUser());
        event.getEntity().setEnabled(true);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        // @LoadDataBeforeShow загружает данные после BeforeShow, поэтому параметр безопасно задаётся здесь.
        User currentUser = userSessionSource.getUserSession().getUser();
        userConfigurationsDl.setParameter("user", currentUser);
        updateModelFieldState(getEditedEntity().getAiFunction());
    }

    @Subscribe("aiFunctionField")
    public void onAiFunctionFieldValueChange(com.haulmont.cuba.gui.components.HasValue.ValueChangeEvent<AiFunctionConfiguration> event) {
        updateModelFieldState(event.getValue());
    }

    /**
     * Защищает per-function invariant поверх DB unique constraint: только текущий пользователь,
     * его активный credential и функция с разрешённым override могут быть сохранены.
     */
    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        User currentUser = userSessionSource.getUserSession().getUser();
        UserAiFunctionOverride override = getEditedEntity();
        AiFunctionConfiguration function = override.getAiFunction();
        UserAiConfiguration configuration = override.getUserAiConfiguration();

        if (override.getUser() == null || !currentUser.getId().equals(override.getUser().getId())) {
            reject(event, "Замещение можно создавать только для текущего пользователя.");
            return;
        }
        if (function == null || !Boolean.TRUE.equals(function.getActive())
                || AiExecutionPolicy.ADMIN_ONLY == function.getExecutionPolicy()) {
            reject(event, "Выбранная AI-функция не разрешает персональное замещение.");
            return;
        }
        if (configuration == null || configuration.getUser() == null
                || !currentUser.getId().equals(configuration.getUser().getId())
                || !Boolean.TRUE.equals(configuration.getIsActive())) {
            reject(event, "Выберите своё активное AI-подключение.");
            return;
        }
        if (isConfigured(override.getModelName()) && !Boolean.TRUE.equals(function.getAllowModelOverride())) {
            reject(event, "Для этой AI-функции администратор запретил замену модели.");
        }
    }

    public void focusMainSection() {
        aiFunctionField.focus();
        mainNav.addStyleName("label-nav-item-active");
        modelNav.removeStyleName("label-nav-item-active");
    }

    public void focusModelSection() {
        userAiConfigurationField.focus();
        modelNav.addStyleName("label-nav-item-active");
        mainNav.removeStyleName("label-nav-item-active");
    }

    private void updateModelFieldState(AiFunctionConfiguration function) {
        modelNameField.setEditable(function != null && Boolean.TRUE.equals(function.getAllowModelOverride()));
    }

    private void reject(BeforeCommitChangesEvent event, String message) {
        notifications.create(Notifications.NotificationType.WARNING)
                .withCaption(message)
                .show();
        event.preventCommit();
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
