package com.company.hunttech.web.screens.extuser;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.app.security.user.edit.UserEditor;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.FieldGroup;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * Расширяет штатный редактор пользователя CUBA presentation-навигацией по вкладкам
 * правой части экрана и запуском стандартного диалога смены пароля HRM HuntTech.
 */
public class ExtUserEditor extends UserEditor {

    private static final String ACTIVE_NAV_STYLE = "label-nav-item-active";

    private static final String GENERAL_TAB = "generalSettingsTab";
    private static final String EMAIL_TAB = "emailSettingsTab";
    private static final String AI_TAB = "aiSettingsTab";

    @Inject
    private Button changePasswordBtn;
    @Inject
    private TabSheet settingsTabSheet;

    @Inject
    private Button generalTabNav;
    @Inject
    private Button emailTabNav;
    @Inject
    private Button aiTabNav;

    @Inject
    private FieldGroup contactsFieldGroup;
    @Inject
    private FieldGroup emailFieldGroupLeft;
    @Inject
    private FieldGroup emailFieldGroupRight;
    @Inject
    private FieldGroup emailFieldPasswordRequired;
    @Inject
    private FieldGroup emailFieldGroupUser;
    @Inject
    private FieldGroup emailFieldGroupPasswords;

    @Override
    protected void postInit() {
        super.postInit();

        // Для нового пользователя пароль задаётся штатными полями UserEditor при сохранении.
        // Отдельный диалог CUBA работает только с уже сохранённой учётной записью.
        changePasswordBtn.setVisible(!PersistenceHelper.isNew(getItem()));

        // UserEditor создаёт часть legacy-полей программно. После super.postInit()
        // общий edit-form-control назначается реальным компонентам без изменения binding.
        applySharedFieldStyles(
                fieldGroupLeft,
                contactsFieldGroup,
                fieldGroupRight,
                emailFieldGroupLeft,
                emailFieldGroupRight,
                emailFieldPasswordRequired,
                emailFieldGroupUser,
                emailFieldGroupPasswords
        );

        // Пункты label-навигации переключают вкладки правого tabsheet.
        generalTabNav.addClickListener(event -> switchToTab(GENERAL_TAB, generalTabNav));
        emailTabNav.addClickListener(event -> switchToTab(EMAIL_TAB, emailTabNav));
        aiTabNav.addClickListener(event -> switchToTab(AI_TAB, aiTabNav));

        // Активный пункт навигации повторяет выбранную вкладку; данные не изменяются.
        settingsTabSheet.addSelectedTabChangeListener(
                event -> updateActiveNavigation(event.getSelectedTab()));
        updateActiveNavigation(settingsTabSheet.getSelectedTab());
    }

    /**
     * Переключает вкладку правой части экрана по пункту навигации и обновляет active-state.
     * Это presentation-only действие: loaders, данные, selection и save lifecycle не затрагиваются.
     */
    private void switchToTab(String tabName, Button activeButton) {
        settingsTabSheet.setSelectedTab(tabName);
        activateNavigation(activeButton);
    }

    /**
     * Открывает штатный экран CUBA sec$User.changePassword для редактируемого пользователя.
     * Диалог использует UserManagementService платформы и не затрагивает сохранение остальных
     * полей, ролей и замещений текущего редактора.
     */
    public void changePassword() {
        User user = getItem();
        if (user == null || PersistenceHelper.isNew(user)) {
            return;
        }

        Window changePasswordDialog = openWindow(
                "sec$User.changePassword",
                WindowManager.OpenType.DIALOG,
                ParamsMap.of("user", user)
        );
        changePasswordDialog.addCloseListener(actionId -> changePasswordBtn.focus());
    }

    /**
     * Помечает пункт, соответствующий активной вкладке; остальные пункты сбрасываются.
     * Selected tab и данные остаются неизменными.
     */
    private void updateActiveNavigation(TabSheet.Tab selectedTab) {
        String selectedTabName = selectedTab != null ? selectedTab.getName() : "";
        if (EMAIL_TAB.equals(selectedTabName)) {
            activateNavigation(emailTabNav);
        } else if (AI_TAB.equals(selectedTabName)) {
            activateNavigation(aiTabNav);
        } else {
            activateNavigation(generalTabNav);
        }
    }

    /**
     * Назначает общий визуальный класс уже созданным компонентам FieldGroup.
     * Required, editable, validators, datasource и property при этом не меняются.
     */
    private void applySharedFieldStyles(FieldGroup... fieldGroups) {
        for (FieldGroup fieldGroup : fieldGroups) {
            for (FieldGroup.FieldConfig fieldConfig : fieldGroup.getFields()) {
                Component fieldComponent = fieldConfig.getComponent();
                if (fieldComponent != null) {
                    fieldComponent.addStyleName("edit-form-control");
                }
            }
        }
    }

    /**
     * Сохраняет базовый label-nav-item каждого пункта и меняет только общий state-class.
     */
    private void activateNavigation(Button activeButton) {
        for (Button navigationButton : navigationButtons()) {
            navigationButton.removeStyleName(ACTIVE_NAV_STYLE);
        }
        activeButton.addStyleName(ACTIVE_NAV_STYLE);
    }

    private List<Button> navigationButtons() {
        return Arrays.asList(generalTabNav, emailTabNav, aiTabNav);
    }
}
