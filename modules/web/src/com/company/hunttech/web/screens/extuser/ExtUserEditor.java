package com.company.hunttech.web.screens.extuser;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.service.TelegramIntegrationService;
import com.company.hunttech.service.UserAvatarManagementService;
import com.company.hunttech.service.dto.avatar.AvatarApplyMode;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.app.security.user.edit.UserEditor;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.ContentMode;
import com.haulmont.cuba.gui.components.FieldGroup;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * Расширяет штатный редактор пользователя CUBA presentation-навигацией по вкладкам
 * правой части экрана, интеграцией с Telegram для загрузки аватарок и запуском диалога смены пароля.
 */
public class ExtUserEditor extends UserEditor {

    private static final Logger log = LoggerFactory.getLogger(ExtUserEditor.class);

    private static final String ACTIVE_NAV_STYLE = "label-nav-item-active";

    private static final String GENERAL_TAB = "generalSettingsTab";
    private static final String EMAIL_TAB = "emailSettingsTab";
    private static final String AI_TAB = "aiSettingsTab";

    @Inject
    private TelegramIntegrationService telegramIntegrationService;
    @Inject
    private UserAvatarManagementService userAvatarManagementService;
    @Inject
    private Dialogs dialogs;
    @Inject
    private FileUploadField officialPhotoUpload;
    @Inject
    private FileLoader fileLoader;

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
    private TextField<String> telegramField;
    @Inject
    private Button fetchTelegramPhotoBtn;

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
        if (telegramField != null) {
            telegramField.addValueChangeListener(e -> refreshProfileLabels());
        }
        refreshProfileLabels();

        if (officialPhotoUpload != null) {
            officialPhotoUpload.addFileUploadSucceedListener(event -> onOfficialPhotoUploadSucceed());
            officialPhotoUpload.addBeforeValueClearListener(event -> onOfficialPhotoUploadBeforeValueClear());
        }
    }

    private void onOfficialPhotoUploadSucceed() {
        User user = getItem();
        if (!(user instanceof ExtUser)) {
            return;
        }
        ExtUser extUser = (ExtUser) user;
        FileDescriptor uploaded = officialPhotoUpload.getFileDescriptor();
        if (uploaded == null) {
            Object value = officialPhotoUpload.getValue();
            if (value instanceof FileDescriptor) {
                uploaded = (FileDescriptor) value;
            }
        }
        if (uploaded == null) {
            return;
        }

        FileDescriptor personalAvatar = extUser.getUserAvatar();
        boolean hasPersonalAvatar = personalAvatar != null && FileDescriptorImageHelper.fileExists(fileLoader, personalAvatar);

        if (hasPersonalAvatar) {
            showAdminPhotoChoiceDialog(extUser, uploaded, personalAvatar);
        } else {
            if (userAvatarManagementService != null) {
                userAvatarManagementService.applyAdminOfficialPhoto(extUser, uploaded, AvatarApplyMode.SMART_DEFAULT);
            } else {
                extUser.setOfficialPhoto(uploaded);
                extUser.setUserAvatar(uploaded);
            }
            refreshProfileLabels();
        }
    }

    private void onOfficialPhotoUploadBeforeValueClear() {
        User user = getItem();
        if (user instanceof ExtUser) {
            ExtUser extUser = (ExtUser) user;
            if (userAvatarManagementService != null) {
                userAvatarManagementService.clearAdminOfficialPhoto(extUser);
            } else {
                extUser.setOfficialPhoto(null);
            }
            refreshProfileLabels();
        }
    }

    private void showAdminPhotoChoiceDialog(ExtUser extUser, FileDescriptor newPhoto, FileDescriptor personalAvatar) {
        String avatarHtml = FileDescriptorImageHelper.buildCandidateFacePreviewHtml(fileLoader, personalAvatar);
        String userName = buildFio(extUser);

        String message = String.format(
                getMessage("msgPhotoChangePrompt"),
                userName, avatarHtml);

        dialogs.createOptionDialog()
                .withCaption(getMessage("msgPhotoChangeCaption"))
                .withMessage(message)
                .withContentMode(ContentMode.HTML)
                .withWidth("480px")
                .withActions(
                        new BaseAction("officialOnlyAction")
                                .withCaption(getMessage("msgPhotoOfficialOnly"))
                                .withPrimary(true)
                                .withHandler(e -> {
                                    if (userAvatarManagementService != null) {
                                        userAvatarManagementService.applyAdminOfficialPhoto(extUser, newPhoto, AvatarApplyMode.OFFICIAL_ONLY);
                                    } else {
                                        extUser.setOfficialPhoto(newPhoto);
                                    }
                                    if (officialPhotoUpload != null) {
                                        officialPhotoUpload.setValue(newPhoto);
                                    }
                                    refreshProfileLabels();
                                }),
                        new BaseAction("overwriteAllAction")
                                .withCaption(getMessage("msgPhotoOverwriteAll"))
                                .withHandler(e -> {
                                    if (userAvatarManagementService != null) {
                                        userAvatarManagementService.applyAdminOfficialPhoto(extUser, newPhoto, AvatarApplyMode.OVERWRITE_ALL);
                                    } else {
                                        extUser.setOfficialPhoto(newPhoto);
                                        extUser.setUserAvatar(newPhoto);
                                    }
                                    if (officialPhotoUpload != null) {
                                        officialPhotoUpload.setValue(newPhoto);
                                    }
                                    refreshProfileLabels();
                                }),
                        new BaseAction("cancelAction")
                                .withCaption(getMessage("msgCancel"))
                                .withHandler(e -> {
                                    if (userAvatarManagementService != null) {
                                        userAvatarManagementService.cleanupUnreferencedFile(newPhoto, extUser.getOfficialPhoto(), extUser.getUserAvatar());
                                    }
                                    if (officialPhotoUpload != null) {
                                        officialPhotoUpload.setValue(extUser.getOfficialPhoto());
                                    }
                                })
                )
                .show();
    }

    /**
     * Загружает фотографию пользователя из Telegram по указанному Telegram ID или @username
     * и сохраняет её в профиль пользователя (officialPhoto / userAvatar).
     */
    public void fetchTelegramPhoto() {
        User user = getItem();
        String userLogin = user != null ? user.getLogin() : "unknown";
        log.info("fetchTelegramPhoto action triggered for user: '{}'", userLogin);

        if (!(user instanceof ExtUser)) {
            log.warn("fetchTelegramPhoto: user entity is not an instance of ExtUser (login='{}')", userLogin);
            showNotification(String.format(getMessage("msgTelegramPhotoError"), "пользователь не является ExtUser"), NotificationType.WARNING);
            return;
        }

        ExtUser extUser = (ExtUser) user;
        String rawTelegram = telegramField != null && StringUtils.isNotBlank(telegramField.getValue())
                ? telegramField.getValue()
                : extUser.getTelegram();

        if (StringUtils.isBlank(rawTelegram)) {
            log.warn("fetchTelegramPhoto: Telegram field is empty for user '{}'", userLogin);
            showNotification(getMessage("msgTelegramPhotoEmpty"), NotificationType.WARNING);
            return;
        }

        String telegramIdentifier = rawTelegram.trim();
        log.info("fetchTelegramPhoto: requesting photo for Telegram identifier '{}' (user '{}')",
                telegramIdentifier, userLogin);

        if (!telegramIntegrationService.isConfigured()) {
            log.warn("fetchTelegramPhoto: TelegramIntegrationService is not configured or bot disabled");
            showNotification(getMessage("msgTelegramNotConfigured"), NotificationType.ERROR);
            return;
        }

        try {
            String safeLogin = extUser.getLogin() != null
                    ? extUser.getLogin().replaceAll("[^a-zA-Z0-9_.-]", "_")
                    : "user";
            String fileName = "user_avatar_" + safeLogin + "_" + System.currentTimeMillis() + ".jpg";
            log.info("Calling TelegramIntegrationService.saveUserProfilePhotoToFileStorage('{}', '{}')",
                    telegramIdentifier, fileName);

            FileDescriptor photoFd = telegramIntegrationService.saveUserProfilePhotoToFileStorage(telegramIdentifier, fileName);

            if (photoFd != null) {
                log.info("Telegram profile photo successfully saved: FileDescriptor ID={}, name='{}', size={} bytes",
                        photoFd.getId(), photoFd.getName(), photoFd.getSize());

                FileDescriptor personalAvatar = extUser.getUserAvatar();
                boolean hasPersonalAvatar = personalAvatar != null && FileDescriptorImageHelper.fileExists(fileLoader, personalAvatar);

                if (hasPersonalAvatar) {
                    showAdminPhotoChoiceDialog(extUser, photoFd, personalAvatar);
                } else {
                    if (userAvatarManagementService != null) {
                        userAvatarManagementService.applyAdminOfficialPhoto(extUser, photoFd, AvatarApplyMode.SMART_DEFAULT);
                    } else {
                        extUser.setOfficialPhoto(photoFd);
                        extUser.setUserAvatar(photoFd);
                    }
                    if (officialPhotoUpload != null) {
                        officialPhotoUpload.setValue(photoFd);
                    }
                    refreshProfileLabels();
                    showNotification(getMessage("msgTelegramPhotoSuccess"), NotificationType.HUMANIZED);
                }
            } else {
                log.warn("Telegram photo not found or failed to download for identifier '{}'", telegramIdentifier);
                showNotification(getMessage("msgTelegramPhotoNotFound"), NotificationType.WARNING);
            }
        } catch (Exception e) {
            log.error("Exception during fetchTelegramPhoto for identifier '{}': {}", telegramIdentifier, e.getMessage(), e);
            showNotification(String.format(getMessage("msgTelegramPhotoError"), e.getMessage()), NotificationType.ERROR);
        }
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
            String tg = telegramField != null && StringUtils.isNotBlank(telegramField.getValue())
                    ? telegramField.getValue()
                    : (user instanceof ExtUser ? ((ExtUser) user).getTelegram() : null);
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

