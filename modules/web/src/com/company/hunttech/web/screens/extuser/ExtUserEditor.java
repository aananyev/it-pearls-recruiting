package com.company.hunttech.web.screens.extuser;

import com.company.hunttech.entity.ExtUser;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.app.security.user.edit.UserEditor;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.FieldGroup;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.lang3.StringUtils;

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
    private Label<String> fioLabel;
    @Inject
    private Label<String> loginLabel;
    @Inject
    private Label<String> statusLabel;
    @Inject
    private Label<String> emailLabel;
    @Inject
    private Label<String> positionLabel;
    @Inject
    private Label<String> telegramLabel;
    @Inject
    private Label<String> loginDetailLabel;
    @Inject
    private Label<String> positionDetailLabel;

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

        // Sidebar-лейблы профиля (ФИО, login, статус, Email, должность) заполняются из userDs;
        // при смене item (открытие другого пользователя, переcommit) значения обновляются повторно.
        userDs.addItemChangeListener(e -> refreshProfileLabels());
        refreshProfileLabels();
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

    /**
     * Заполняет sidebar-лейблы профиля из текущего пользователя userDs.
     * Presentation-only: значения копируются в Label, entity не модифицируется.
     */
    private void refreshProfileLabels() {
        User user = getItem();
        if (user == null) {
            if (fioLabel != null) fioLabel.setValue("");
            if (loginLabel != null) loginLabel.setValue("");
            if (loginDetailLabel != null) loginDetailLabel.setValue("-");
            if (statusLabel != null) statusLabel.setValue("-");
            if (emailLabel != null) emailLabel.setValue("-");
            if (positionLabel != null) positionLabel.setValue("");
            if (positionDetailLabel != null) positionDetailLabel.setValue("-");
            if (telegramLabel != null) telegramLabel.setValue("-");
            return;
        }
        if (fioLabel != null) {
            fioLabel.setValue(buildFio(user));
        }
        if (loginLabel != null) {
            loginLabel.setValue(user.getLogin() != null ? user.getLogin() : "");
        }
        if (loginDetailLabel != null) {
            loginDetailLabel.setValue(user.getLogin() != null ? user.getLogin() : "-");
        }
        boolean active = Boolean.TRUE.equals(user.getActive());
        if (statusLabel != null) {
            statusLabel.setValue(active
                    ? "<span style='color: #22c55e; font-weight: 600;'>● " + getMessage("msgStatusActive") + "</span>"
                    : "<span style='color: #ef4444; font-weight: 600;'>● " + getMessage("msgStatusBlocked") + "</span>");
        }
        if (emailLabel != null) {
            emailLabel.setValue(user.getEmail() != null && !user.getEmail().trim().isEmpty() ? user.getEmail() : "-");
        }
        if (positionLabel != null) {
            positionLabel.setValue(user.getPosition() != null ? user.getPosition() : "");
        }
        if (positionDetailLabel != null) {
            positionDetailLabel.setValue(user.getPosition() != null && !user.getPosition().trim().isEmpty() ? user.getPosition() : "-");
        }
        if (telegramLabel != null) {
            String tg = user instanceof ExtUser ? ((ExtUser) user).getTelegram() : null;
            if (StringUtils.isNotBlank(tg)) {
                String cleanTg = tg.trim();
                telegramLabel.setValue(cleanTg.startsWith("@") ? cleanTg : "@" + cleanTg);
            } else {
                telegramLabel.setValue("-");
            }
        }
    }

    /**
     * Собирает человекочитаемое ФИО: name, либо lastName + firstName + middleName,
     * либо login как запасной идентификатор (порядок как у legacy ExtUserEdit).
     */
    private String buildFio(User user) {
        String name = StringUtils.trimToEmpty(user.getName());
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(user.getLastName())) {
            sb.append(user.getLastName());
        }
        if (StringUtils.isNotBlank(user.getFirstName())) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(user.getFirstName());
        }
        if (StringUtils.isNotBlank(user.getMiddleName())) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(user.getMiddleName());
        }
        return sb.length() > 0 ? sb.toString() : user.getLogin();
    }
}
