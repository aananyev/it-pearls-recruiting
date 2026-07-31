package com.company.hunttech.web.screens.extuser;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.app.security.user.edit.UserEditor;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.FieldGroup;
import com.haulmont.cuba.gui.components.GroupBoxLayout;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;

/**
 * Расширяет штатный редактор пользователя CUBA только presentation-навигацией
 * и запуском стандартного диалога смены пароля HRM HuntTech.
 */
public class ExtUserEditor extends UserEditor {

    private static final String ACTIVE_NAV_STYLE = "label-nav-item-active";

    @Inject
    private Button changePasswordBtn;
    @Inject
    private TabSheet settingsTabSheet;
    @Inject
    private VBoxLayout generalUserNavigation;
    @Inject
    private VBoxLayout emailUserNavigation;
    @Inject
    private VBoxLayout aiUserNavigation;

    @Inject
    private Button generalContactsNav;
    @Inject
    private Button generalRegionalNav;
    @Inject
    private Button generalRolesNav;
    @Inject
    private Button generalSubstitutionsNav;
    @Inject
    private Button emailServersNav;
    @Inject
    private Button emailPortsNav;
    @Inject
    private Button emailPasswordRequiredNav;
    @Inject
    private Button emailUsersNav;
    @Inject
    private Button emailPasswordsNav;
    @Inject
    private Button aiConfigurationsNav;

    @Named("contactsFieldGroup.login")
    private Component.Focusable loginField;
    @Named("emailFieldGroupLeft.smtpServer")
    private Component.Focusable smtpServerField;
    @Named("emailFieldGroupRight.smtpPort")
    private Component.Focusable smtpPortField;
    @Named("emailFieldPasswordRequired.smtpPasswordRequired")
    private Component.Focusable smtpPasswordRequiredField;
    @Named("emailFieldGroupUser.smtpUser")
    private Component.Focusable smtpUserField;
    @Named("emailFieldGroupPasswords.smtpPassword")
    private Component.Focusable smtpPasswordField;
    @Inject
    private Table aiConfigsTable;

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

    @Inject
    private GroupBoxLayout propertiesEmailBox;
    @Inject
    private GroupBoxLayout emailPortsBox;
    @Inject
    private GroupBoxLayout emailAuthenticationBox;
    @Inject
    private GroupBoxLayout emailAccountsBox;
    @Inject
    private GroupBoxLayout emailPasswordsBox;

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

        // Navigation-набор зависит только от активной вкладки и не переключает вкладки,
        // не запускает loaders и не изменяет текущего пользователя.
        settingsTabSheet.addSelectedTabChangeListener(
                event -> refreshNavigationForTab(event.getSelectedTab()));
        refreshNavigationForTab(settingsTabSheet.getSelectedTab());
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
     * Переводит focus к контактным полям текущей общей вкладки.
     */
    public void focusGeneralContactsSection() {
        focusAndActivate(loginField, generalContactsNav, generalNavigationButtons());
    }

    /**
     * Переводит focus к штатному language lookup региональной карточки.
     */
    public void focusGeneralRegionalSection() {
        focusAndActivate(languageLookup, generalRegionalNav, generalNavigationButtons());
    }

    /**
     * Переводит focus к таблице ролей без изменения selection или данных.
     */
    public void focusRolesSection() {
        focusAndActivate(rolesTable, generalRolesNav, generalNavigationButtons());
    }

    /**
     * Переводит focus к таблице замещений без изменения selection или данных.
     */
    public void focusSubstitutionsSection() {
        focusAndActivate(substTable, generalSubstitutionsNav, generalNavigationButtons());
    }

    /**
     * Переводит focus к первому полю accordion-секции почтовых серверов.
     */
    public void focusMailServersSection() {
        revealFocusAndActivate(
                propertiesEmailBox,
                smtpServerField,
                emailServersNav,
                emailNavigationButtons()
        );
    }

    /**
     * Переводит focus к первому полю accordion-секции почтовых портов.
     */
    public void focusMailPortsSection() {
        revealFocusAndActivate(
                emailPortsBox,
                smtpPortField,
                emailPortsNav,
                emailNavigationButtons()
        );
    }

    /**
     * Переводит focus к первому признаку обязательности почтового пароля.
     */
    public void focusMailAuthenticationSection() {
        revealFocusAndActivate(
                emailAuthenticationBox,
                smtpPasswordRequiredField,
                emailPasswordRequiredNav,
                emailNavigationButtons()
        );
    }

    /**
     * Переводит focus к первому имени почтовой учётной записи.
     */
    public void focusMailAccountsSection() {
        revealFocusAndActivate(
                emailAccountsBox,
                smtpUserField,
                emailUsersNav,
                emailNavigationButtons()
        );
    }

    /**
     * Переводит focus к первому password-полю почтовых протоколов.
     */
    public void focusMailPasswordsSection() {
        revealFocusAndActivate(
                emailPasswordsBox,
                smtpPasswordField,
                emailPasswordsNav,
                emailNavigationButtons()
        );
    }

    /**
     * Переводит focus к таблице персональных AI-конфигураций.
     */
    public void focusAiConfigurationsSection() {
        focusAndActivate(aiConfigsTable, aiConfigurationsNav, aiNavigationButtons());
    }

    /**
     * Показывает navigation-набор активной вкладки и сбрасывает active-state
     * на первый раздел этой вкладки. Selected tab и данные остаются неизменными.
     */
    private void refreshNavigationForTab(TabSheet.Tab selectedTab) {
        String selectedTabName = selectedTab != null ? selectedTab.getName() : "";

        boolean generalSelected = "generalSettingsTab".equals(selectedTabName);
        boolean emailSelected = "emailSettingsTab".equals(selectedTabName);
        boolean aiSelected = "aiSettingsTab".equals(selectedTabName);

        generalUserNavigation.setVisible(generalSelected);
        emailUserNavigation.setVisible(emailSelected);
        aiUserNavigation.setVisible(aiSelected);

        if (generalSelected) {
            activateNavigation(generalContactsNav, generalNavigationButtons());
        } else if (emailSelected) {
            activateNavigation(emailServersNav, emailNavigationButtons());
        } else if (aiSelected) {
            activateNavigation(aiConfigurationsNav, aiNavigationButtons());
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
     * Раскрывает accordion-секцию перед focus. Это presentation-only действие:
     * значения, loaders, validation и save lifecycle остаются неизменными.
     */
    private void revealFocusAndActivate(GroupBoxLayout section,
                                        Component.Focusable target,
                                        Button activeButton,
                                        List<Button> navigationButtons) {
        section.setExpanded(true);
        focusAndActivate(target, activeButton, navigationButtons);
    }

    /**
     * Focus является единственным действием navigation-пункта; active-state
     * изменяется добавлением/удалением общего label-nav-item-active.
     */
    private void focusAndActivate(Component.Focusable target,
                                  Button activeButton,
                                  List<Button> navigationButtons) {
        if (target != null) {
            target.focus();
        }
        activateNavigation(activeButton, navigationButtons);
    }

    /**
     * Сохраняет базовый label-nav-item каждого пункта и меняет только общий state-class.
     */
    private void activateNavigation(Button activeButton, List<Button> navigationButtons) {
        for (Button navigationButton : navigationButtons) {
            navigationButton.removeStyleName(ACTIVE_NAV_STYLE);
        }
        activeButton.addStyleName(ACTIVE_NAV_STYLE);
    }

    private List<Button> generalNavigationButtons() {
        return Arrays.asList(
                generalContactsNav,
                generalRegionalNav,
                generalRolesNav,
                generalSubstitutionsNav
        );
    }

    private List<Button> emailNavigationButtons() {
        return Arrays.asList(
                emailServersNav,
                emailPortsNav,
                emailPasswordRequiredNav,
                emailUsersNav,
                emailPasswordsNav
        );
    }

    private List<Button> aiNavigationButtons() {
        return Arrays.asList(aiConfigurationsNav);
    }
}
