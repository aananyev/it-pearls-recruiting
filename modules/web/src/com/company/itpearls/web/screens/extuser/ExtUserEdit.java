package com.company.itpearls.web.screens.extuser;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.StdPictures;
import com.company.itpearls.entity.UserAiConfiguration;
import com.company.itpearls.entity.UserSettings;
import com.company.itpearls.web.screens.useraiconfiguration.UserAiConfigurationEdit;
import com.company.itpearls.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.UUID;

@UiController("itpearls_ExtUserEdit")
@UiDescriptor("ext-user-edit.xml")
public class ExtUserEdit extends Screen {

    private static final String USER_SETTINGS_QUERY =
            "select e from itpearls_UserSettings e where e.user.id = :userId";

    @Inject
    private Field smtpPassword;
    @Inject
    private Image defaultPic;
    @Inject
    private FileUploadField fileImageFaceUpload;
    @Inject
    private Image userPic;
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

    @Subscribe
    public void onInit(InitEvent event) {
        userDs.addItemChangeListener(e -> {
            refreshProfileLabels();
            refreshAiConfigs();
            refreshAvatar();
        });
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        refreshProfileLabels();
        refreshAiConfigs();
        refreshAvatar();
        User user = userDs.getItem();
        if (user != null && PersistenceHelper.isNew(user)) {
            passwordBox.setVisible(true);
        }
    }

    @Install(to = "emailFieldPasswordRequired.smtpPasswordRequired", subject = "validator")
    private void emailFieldPasswordRequiredSmtpPasswordRequiredValidator(Boolean value) {
        smtpPassword.setRequired(Boolean.TRUE.equals(value));
    }

    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        refreshAvatar();
    }

    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadBeforeValueClear(FileUploadField.BeforeValueClearEvent event) {
        refreshAvatar();
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

    private void refreshAvatar() {
        FileDescriptor avatar = resolveAvatarFileDescriptor(userDs.getItem());
        userPic.setValueSource(null);
        if (FileDescriptorImageHelper.fileExists(fileLoader, avatar)) {
            defaultPic.setVisible(false);
            userPic.setVisible(true);
            FileDescriptorImageHelper.setImageSource(userPic, fileLoader, avatar,
                    StdPictures.NO_CANDIDATE.getId());
        } else {
            userPic.setVisible(false);
            defaultPic.setVisible(true);
        }
    }

    /**
     * Priority: personal photo from «About me» ({@link UserSettings#fileImageFace}),
     * then admin/system photo on {@link ExtUser#fileImageFace}, then placeholder.
     */
    private FileDescriptor resolveAvatarFileDescriptor(User user) {
        if (user == null) {
            return null;
        }
        FileDescriptor personalImage = loadPersonalImage(user);
        if (FileDescriptorImageHelper.fileExists(fileLoader, personalImage)) {
            return personalImage;
        }
        if (user instanceof ExtUser) {
            FileDescriptor adminImage = ((ExtUser) user).getFileImageFace();
            if (FileDescriptorImageHelper.fileExists(fileLoader, adminImage)) {
                return adminImage;
            }
        }
        return null;
    }

    private FileDescriptor loadPersonalImage(User user) {
        return dataManager.load(UserSettings.class)
                .query(USER_SETTINGS_QUERY)
                .parameter("userId", user.getId())
                .view("userSettings-view")
                .optional()
                .map(UserSettings::getFileImageFace)
                .orElse(null);
    }
}
