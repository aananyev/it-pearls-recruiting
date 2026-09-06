package com.company.hunttech.web.screens.extuser;

import com.company.hunttech.app.ImageProcessingService;
import com.company.hunttech.config.HunttechImageConfig;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.UserSettings;
import com.company.hunttech.service.UserAvatarManagementService;
import com.company.hunttech.service.dto.avatar.AvatarApplyMode;
import com.company.hunttech.web.screens.useraiconfiguration.UserAiConfigurationEdit;
import com.company.hunttech.web.util.AvatarImageUploadHelper;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;

@UiController("hunttech_ExtUserEdit")
@UiDescriptor("ext-user-edit.xml")
public class ExtUserEdit extends Screen {

    private static final Logger log = LoggerFactory.getLogger(ExtUserEdit.class);

    @Inject
    private Field smtpPassword;
    @Inject
    private FileUploadField officialPhotoUpload;
    @Inject
    private Label<String> fioLabel;
    @Inject
    private Label<String> loginLabel;
    @Inject
    private Label<String> statusLabel;
    @Inject
    private VBoxLayout passwordBox;
    @Inject
    private CollectionDatasource<UserAiConfiguration, UUID> userAiConfigsDs;
    @Inject
    private Table<UserAiConfiguration> aiConfigsTable;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Datasource<User> userDs;
    @Inject
    private DataManager dataManager;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private FileStorageService fileStorageService;
    @Inject
    private Dialogs dialogs;
    @Inject
    private ImageProcessingService imageProcessingService;
    @Inject
    private HunttechImageConfig hunttechImageConfig;
    @Inject
    private UserAvatarManagementService userAvatarManagementService;

    @Subscribe
    public void onInit(InitEvent event) {
        userDs.addItemChangeListener(e -> {
            refreshProfileLabels();
            refreshAiConfigs();
        });
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        refreshProfileLabels();
        refreshAiConfigs();
        User user = userDs.getItem();
        if (user != null && PersistenceHelper.isNew(user)) {
            passwordBox.setVisible(true);
        }
    }

    @Install(to = "emailFieldPasswordRequired.smtpPasswordRequired", subject = "validator")
    private void emailFieldPasswordRequiredSmtpPasswordRequiredValidator(Boolean value) {
        smtpPassword.setRequired(Boolean.TRUE.equals(value));
    }

    @Subscribe("officialPhotoUpload")
    public void onOfficialPhotoUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        ExtUser user = getExtUser();
        if (user == null) {
            return;
        }

        FileDescriptor newPhoto = officialPhotoUpload.getFileDescriptor();
        newPhoto = processUploadedPhoto(newPhoto);
        if (newPhoto == null) {
            return;
        }

        FileDescriptor personalAvatar = user.getUserAvatar();

        if (personalAvatar != null && FileDescriptorImageHelper.fileExists(fileLoader, personalAvatar)) {
            showAdminPhotoChoiceDialog(user, newPhoto, personalAvatar);
        } else {
            applyOfficialPhotoUpdate(user, newPhoto, AvatarApplyMode.SMART_DEFAULT);
        }
    }

    @Subscribe("officialPhotoUpload")
    public void onOfficialPhotoUploadBeforeValueClear(FileUploadField.BeforeValueClearEvent event) {
        ExtUser user = getExtUser();
        if (user != null) {
            if (userAvatarManagementService != null) {
                userAvatarManagementService.clearAdminOfficialPhoto(user);
            } else {
                user.setOfficialPhoto(null);
            }
        }
    }

    @Subscribe("changePasswordBtn")
    public void onChangePasswordBtnClick(Button.ClickEvent event) {
        passwordBox.setVisible(!passwordBox.isVisible());
    }

    @Subscribe("aiConfigsCreateBtn")
    public void onAiConfigsCreateBtnClick(Button.ClickEvent event) {
        User user = userDs.getItem();
        if (user == null) {
            return;
        }
        screenBuilders.editor(UserAiConfiguration.class, this)
                .withScreenClass(UserAiConfigurationEdit.class)
                .newEntity()
                .withInitializer(entity -> {
                    entity.setUser(user);
                    entity.setIsActive(true);
                })
                .withAfterCloseListener(afterCloseEvent -> refreshAiConfigs())
                .build()
                .show();
    }

    @Subscribe("aiConfigsEditBtn")
    public void onAiConfigsEditBtnClick(Button.ClickEvent event) {
        UserAiConfiguration selected = aiConfigsTable.getSingleSelected();
        if (selected == null) {
            return;
        }
        screenBuilders.editor(UserAiConfiguration.class, this)
                .withScreenClass(UserAiConfigurationEdit.class)
                .editEntity(selected)
                .withAfterCloseListener(afterCloseEvent -> refreshAiConfigs())
                .build()
                .show();
    }

    @Subscribe("aiConfigsRemoveBtn")
    public void onAiConfigsRemoveBtnClick(Button.ClickEvent event) {
        UserAiConfiguration selected = aiConfigsTable.getSingleSelected();
        if (selected == null) {
            return;
        }
        dataManager.remove(selected);
        refreshAiConfigs();
    }

    /**
     * Показ модального диалога администратору при обнаружении конфликта изображений
     */
    private void showAdminPhotoChoiceDialog(ExtUser user, FileDescriptor newPhoto, FileDescriptor personalAvatar) {
        String avatarHtml = FileDescriptorImageHelper.buildCandidateFacePreviewHtml(fileLoader, personalAvatar);
        String userName = buildFio(user);

        String message = String.format(
                messageBundle.getMessage("msgPhotoChangePrompt"),
                userName, avatarHtml);

        dialogs.createOptionDialog()
                .withCaption(messageBundle.getMessage("msgPhotoChangeCaption"))
                .withMessage(message)
                .withContentMode(ContentMode.HTML)
                .withWidth("480px")
                .withActions(
                        new BaseAction("officialOnlyAction")
                                .withCaption(messageBundle.getMessage("msgPhotoOfficialOnly"))
                                .withPrimary(true)
                                .withHandler(e -> applyOfficialPhotoUpdate(user, newPhoto, AvatarApplyMode.OFFICIAL_ONLY)),
                        new BaseAction("overwriteAllAction")
                                .withCaption(messageBundle.getMessage("msgPhotoOverwriteAll"))
                                .withHandler(e -> applyOfficialPhotoUpdate(user, newPhoto, AvatarApplyMode.OVERWRITE_ALL)),
                        new BaseAction("cancelAction")
                                .withCaption(messageBundle.getMessage("msgCancel"))
                                .withHandler(e -> {
                                    if (userAvatarManagementService != null) {
                                        userAvatarManagementService.cleanupUnreferencedFile(newPhoto, user.getOfficialPhoto(), user.getUserAvatar());
                                    }
                                    officialPhotoUpload.setValue(user.getOfficialPhoto());
                                })
                )
                .show();
    }

    private FileDescriptor processUploadedPhoto(FileDescriptor descriptor) {
        log.debug("Processing avatar upload with limits targetImageSize={}, targetImageFormat={}",
                hunttechImageConfig.getTargetImageSize(), hunttechImageConfig.getTargetImageFormat());
        return AvatarImageUploadHelper.processUploadedImage(
                descriptor, fileLoader, fileStorageService, dataManager, imageProcessingService, log);
    }

    /**
     * Универсальный метод применения изменений изображений для ExtUser
     */
    private void applyOfficialPhotoUpdate(ExtUser user, FileDescriptor newPhoto, AvatarApplyMode mode) {
        if (userAvatarManagementService != null) {
            userAvatarManagementService.applyAdminOfficialPhoto(user, newPhoto, mode);
        } else {
            FileDescriptor oldOfficial = user.getOfficialPhoto();
            removeStoredFileIfUnreferenced(oldOfficial, user.getUserAvatar(), newPhoto);
            user.setOfficialPhoto(newPhoto);
            if (mode == AvatarApplyMode.OVERWRITE_ALL || (mode == AvatarApplyMode.SMART_DEFAULT && user.getUserAvatar() == null)) {
                user.setUserAvatar(newPhoto);
            }
        }
        userDs.setItem(user);
    }

    private void removeStoredFileIfUnreferenced(FileDescriptor oldFile,
                                                FileDescriptor stillReferenced,
                                                FileDescriptor replacement) {
        if (oldFile == null || Objects.equals(oldFile, replacement)) {
            return;
        }
        if (stillReferenced != null && Objects.equals(oldFile.getId(), stillReferenced.getId())) {
            return;
        }
        try {
            fileStorageService.removeFile(oldFile);
        } catch (FileStorageException e) {
            log.warn("Cannot remove old user photo id={}: {}", oldFile.getId(), e.getMessage());
        }
    }

    private ExtUser getExtUser() {
        User user = userDs.getItem();
        return user instanceof ExtUser ? (ExtUser) user : null;
    }

    /**
     * Безопасная загрузка личных настроек пользователя из БД с нужным представлением
     */
    private UserSettings loadUserSettings(ExtUser user) {
        try {
            return dataManager.load(UserSettings.class)
                    .query("select e from hunttech_UserSettings e where e.user = :user")
                    .parameter("user", user)
                    .view("userSettings-view")
                    .optional()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Ошибка при загрузке UserSettings для пользователя {}", user.getLogin(), e);
            return null;
        }
    }

    private void refreshAiConfigs() {
        if (userDs.getItem() != null) {
            userAiConfigsDs.refresh();
        }
    }

    private void refreshProfileLabels() {
        User user = userDs.getItem();
        if (user == null) {
            fioLabel.setValue("");
            loginLabel.setValue("");
            statusLabel.setValue("");
            return;
        }
        fioLabel.setValue(buildFio(user));
        loginLabel.setValue(user.getLogin() != null ? user.getLogin() : "");
        boolean active = Boolean.TRUE.equals(user.getActive());
        statusLabel.setValue(active ? messageBundle.getMessage("msgStatusActive")
                : messageBundle.getMessage("msgStatusBlocked"));
    }

    private String buildFio(User user) {
        String fio = StringUtils.trimToEmpty(user.getName());
        if (StringUtils.isNotBlank(fio)) {
            return fio;
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
