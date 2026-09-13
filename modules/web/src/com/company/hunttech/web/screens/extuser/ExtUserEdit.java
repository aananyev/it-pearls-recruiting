package com.company.hunttech.web.screens.extuser;

import com.company.hunttech.app.ImageProcessingService;
import com.company.hunttech.config.HunttechImageConfig;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.UserSettings;
import com.company.hunttech.service.UserAiQuotaService;
import com.company.hunttech.service.dto.ai.UserAiQuotaInfo;
import com.company.hunttech.service.UserAvatarManagementService;
import com.company.hunttech.service.YandexIntegrationService;
import com.company.hunttech.dto.yandex.*;
import com.company.hunttech.entity.UserYandexConfiguration;
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
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.haulmont.cuba.gui.screen.StandardOutcome;

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
    @Inject
    private YandexIntegrationService yandexIntegrationService;
    @Inject
    private Datasource<UserYandexConfiguration> userYandexConfigDs;
    @Inject
    private TabSheet settingsTabSheet;
    @Inject
    private Button generalTabNav;
    @Inject
    private Button emailTabNav;
    @Inject
    private Button aiTabNav;
    @Inject
    private Button yandexTabNav;
    @Inject
    private PasswordField adminYandexTokenField;
    @Inject
    private PasswordField adminYandexRefreshField;
    @Inject
    private Label<String> adminYandexStatusLabel;
    @Inject
    private LookupField<String> adminTimeZoneField;
    @Inject
    private Notifications notifications;
    @Inject
    private FieldGroup adminAiQuotaFieldGroup;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private UserAiQuotaService userAiQuotaService;

    private TextField<String> monthlyQuotaTokensField;
    private TextField<String> consumedTokensField;
    private TextField<String> remainingTokensField;
    private Label<String> quotaStatusBadge;
    private Integer initialQuotaTokens;
    private Integer pendingQuotaToSave;
    private boolean pendingQuotaChanged;

    @Subscribe
    public void onInit(InitEvent event) {
        userDs.addItemChangeListener(e -> {
            refreshProfileLabels();
            refreshAiConfigs();
            refreshYandexConfig();
            if (!pendingQuotaChanged) {
                refreshAdminAiQuotaValues();
            }
        });
        initNavigationButtons();
        initAdminTimeZoneOptions();
        initAdminAiQuotaFields();
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        refreshProfileLabels();
        refreshAiConfigs();
        refreshYandexConfig();
        refreshAdminAiQuotaValues();
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
                .withOpenMode(OpenMode.DIALOG)
                .newEntity()
                .withInitializer(entity -> {
                    entity.setUser(user);
                    entity.setIsActive(true);
                    entity.setIsPrimary(false);
                    entity.setMaxRetries(2);
                    entity.setPriority(10);
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
        UserAiConfiguration toEdit = dataManager.load(UserAiConfiguration.class)
                .id(selected.getId())
                .view("userAiConfiguration-edit-view")
                .optional()
                .orElse(selected);

        screenBuilders.editor(UserAiConfiguration.class, this)
                .withScreenClass(UserAiConfigurationEdit.class)
                .withOpenMode(OpenMode.DIALOG)
                .editEntity(toEdit)
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

    private void initNavigationButtons() {
        if (generalTabNav != null) generalTabNav.addClickListener(e -> selectTab("generalSettingsTab", generalTabNav));
        if (emailTabNav != null) emailTabNav.addClickListener(e -> selectTab("emailSettingsTab", emailTabNav));
        if (aiTabNav != null) aiTabNav.addClickListener(e -> selectTab("aiSettingsTab", aiTabNav));
        if (yandexTabNav != null) yandexTabNav.addClickListener(e -> selectTab("yandexTab", yandexTabNav));
    }

    private void selectTab(String tabId, Button activeBtn) {
        if (settingsTabSheet != null) {
            settingsTabSheet.setSelectedTab(tabId);
        }
        String defaultStyle = "borderless label-nav-item";
        String activeStyle = "borderless label-nav-item label-nav-item-active";
        if (generalTabNav != null) generalTabNav.setStyleName(generalTabNav == activeBtn ? activeStyle : defaultStyle);
        if (emailTabNav != null) emailTabNav.setStyleName(emailTabNav == activeBtn ? activeStyle : defaultStyle);
        if (aiTabNav != null) aiTabNav.setStyleName(aiTabNav == activeBtn ? activeStyle : defaultStyle);
        if (yandexTabNav != null) yandexTabNav.setStyleName(yandexTabNav == activeBtn ? activeStyle : defaultStyle);
    }

    private void initAdminTimeZoneOptions() {
        if (adminTimeZoneField != null) {
            Map<String, String> timeZones = new LinkedHashMap<>();
            timeZones.put("Europe/Saratov (UTC+4)", "Europe/Saratov");
            timeZones.put("Europe/Moscow (UTC+3)", "Europe/Moscow");
            timeZones.put("UTC", "UTC");
            adminTimeZoneField.setOptionsMap(timeZones);
        }
    }

    private void refreshYandexConfig() {
        User user = userDs.getItem();
        if (user != null && yandexIntegrationService != null) {
            try {
                UserYandexConfiguration c = yandexIntegrationService.getOrCreateConfiguration(user.getId());
                userYandexConfigDs.setItem(c);
                refreshAdminYandexStatus();
            } catch (Exception e) {
                log.warn("Не удалось загрузить настройки Yandex для пользователя {}: {}", user.getLogin(), e.getMessage());
            }
        }
    }

    private void refreshAdminYandexStatus() {
        if (adminYandexStatusLabel == null || userYandexConfigDs == null) return;
        UserYandexConfiguration c = userYandexConfigDs.getItem();
        if (c == null) {
            adminYandexStatusLabel.setValue("Не настроено");
            return;
        }
        boolean cal = Boolean.TRUE.equals(c.getCalendarConnected());
        boolean tel = Boolean.TRUE.equals(c.getTelemostConnected());
        if (cal || tel) {
            adminYandexStatusLabel.setValue("Подключено: " + (cal ? "Календарь ✓ " : "") + (tel ? "Телемост ✓" : ""));
            adminYandexStatusLabel.setStyleName("bold friendly");
        } else {
            adminYandexStatusLabel.setValue("Требуется проверка доступа");
        }
    }

    public void onTestYandexAuthClick() {
        saveAdminYandexTokens();
        User u = userDs.getItem();
        if (u != null && yandexIntegrationService != null) {
            YandexDiagnosticResult res = yandexIntegrationService.testConnection(u.getId(), "AUTH");
            showDiagnosticNotification("Авторизация Яндекс 360", res);
            refreshAdminYandexStatus();
        }
    }

    public void onDiscoverCalendarsClick() {
        saveAdminYandexTokens();
        User u = userDs.getItem();
        if (u != null && yandexIntegrationService != null) {
            YandexDiagnosticResult res = yandexIntegrationService.testConnection(u.getId(), "CALENDAR");
            showDiagnosticNotification("Яндекс.Календарь", res);
            refreshAdminYandexStatus();
        }
    }

    public void onTestTelemostClick() {
        saveAdminYandexTokens();
        User u = userDs.getItem();
        if (u != null && yandexIntegrationService != null) {
            YandexDiagnosticResult res = yandexIntegrationService.testConnection(u.getId(), "TELEMOST");
            showDiagnosticNotification("Яндекс.Телемост", res);
            refreshAdminYandexStatus();
        }
    }

    private void saveAdminYandexTokens() {
        UserYandexConfiguration c = userYandexConfigDs.getItem();
        if (c != null && yandexIntegrationService != null) {
            String token = adminYandexTokenField != null ? adminYandexTokenField.getValue() : null;
            String refresh = adminYandexRefreshField != null ? adminYandexRefreshField.getValue() : null;
            yandexIntegrationService.saveConfiguration(c, token, refresh);
        }
    }

    private void showDiagnosticNotification(String caption, YandexDiagnosticResult res) {
        if (notifications == null) return;
        if (res.isSuccess()) {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption(caption + ": Успешно")
                    .withDescription(res.getMessage())
                    .show();
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(caption + ": Ошибка")
                    .withDescription(res.getMessage() + (res.getDetails() != null ? " (" + res.getDetails() + ")" : ""))
                    .show();
        }
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        if (event.closedWith(StandardOutcome.COMMIT)) {
            if (!readPendingQuotaInput()) {
                event.preventWindowClose();
                return;
            }
            saveAdminAiQuotaOverride();
            saveAdminYandexTokens();
        }
    }

    private void initAdminAiQuotaFields() {
        if (adminAiQuotaFieldGroup == null) {
            return;
        }

        adminAiQuotaFieldGroup.addCustomField("monthlyQuotaTokensField", (datasource, propertyId) -> {
            HBoxLayout container = uiComponents.create(HBoxLayout.class);
            container.setWidth("100%");
            container.setSpacing(true);

            monthlyQuotaTokensField = uiComponents.create(TextField.TYPE_STRING);
            monthlyQuotaTokensField.setWidth("100%");
            monthlyQuotaTokensField.setDescription(messageBundle.getMessage("msgTokensQuotaInputHelp"));

            Button addBonusTokensBtn = uiComponents.create(Button.class);
            addBonusTokensBtn.setId("addBonusTokensBtn");
            addBonusTokensBtn.setCaption(messageBundle.getMessage("btnAddBonusTokens"));
            addBonusTokensBtn.setIcon("font-icon:PLUS_CIRCLE");
            addBonusTokensBtn.setStyleName("friendly");
            addBonusTokensBtn.addClickListener(e -> openAddBonusTokensDialog());

            container.add(monthlyQuotaTokensField);
            container.add(addBonusTokensBtn);
            container.expand(monthlyQuotaTokensField);

            return container;
        });

        adminAiQuotaFieldGroup.addCustomField("consumedTokensField", (datasource, propertyId) -> {
            consumedTokensField = uiComponents.create(TextField.TYPE_STRING);
            consumedTokensField.setWidth("100%");
            consumedTokensField.setEditable(false);
            return consumedTokensField;
        });

        adminAiQuotaFieldGroup.addCustomField("remainingTokensField", (datasource, propertyId) -> {
            remainingTokensField = uiComponents.create(TextField.TYPE_STRING);
            remainingTokensField.setWidth("100%");
            remainingTokensField.setEditable(false);
            return remainingTokensField;
        });

        adminAiQuotaFieldGroup.addCustomField("quotaStatusBadge", (datasource, propertyId) -> {
            quotaStatusBadge = uiComponents.create(Label.TYPE_STRING);
            quotaStatusBadge.setHtmlEnabled(true);
            return quotaStatusBadge;
        });
    }

    private void openAddBonusTokensDialog() {
        User user = userDs.getItem();
        if (!(user instanceof ExtUser) || PersistenceHelper.isNew(user)) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgSaveUserFirstForBonus"))
                    .show();
            return;
        }

        ExtUser extUser = (ExtUser) user;

        dialogs.createInputDialog(this)
                .withCaption(messageBundle.getMessage("msgAddBonusTokensTitle"))
                .withParameter(
                        InputParameter.intParameter("tokenAmount")
                                .withCaption(messageBundle.getMessage("msgAddBonusTokensPrompt"))
                                .withRequired(true)
                )
                .withValidator(context -> {
                    Integer val = context.getValue("tokenAmount");
                    if (val == null || val <= 0) {
                        return ValidationErrors.of(messageBundle.getMessage("msgPositiveTokenAmountRequired"));
                    }
                    return ValidationErrors.none();
                })
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                        Integer amount = closeEvent.getValue("tokenAmount");
                        if (amount != null && amount > 0) {
                            try {
                                userAiQuotaService.addMonthlyBonusTokens(
                                        extUser.getId(),
                                        amount,
                                        messageBundle.getMessage("msgBonusTokensReasonManualAdd")
                                );
                                notifications.create(Notifications.NotificationType.HUMANIZED)
                                        .withCaption(messageBundle.formatMessage("msgBonusTokensAddedSuccess", amount))
                                        .show();
                                refreshAdminAiQuotaValues();
                            } catch (Exception ex) {
                                log.error("Ошибка при начислении бонусных токенов: {}", ex.getMessage(), ex);
                                notifications.create(Notifications.NotificationType.ERROR)
                                        .withCaption(messageBundle.getMessage("msgBonusTokensAddError") + ": " + ex.getMessage())
                                        .show();
                            }
                        }
                    }
                })
                .show();
    }

    private void refreshAdminAiQuotaValues() {
        User user = userDs.getItem();
        if (!(user instanceof ExtUser) || PersistenceHelper.isNew(user)) {
            if (monthlyQuotaTokensField != null) {
                int defQuota = userAiQuotaService != null ? userAiQuotaService.loadDefaultMonthlyQuota() : 1000000;
                monthlyQuotaTokensField.setValue(String.valueOf(defQuota));
                initialQuotaTokens = defQuota;
            }
            if (consumedTokensField != null) {
                consumedTokensField.setValue("0 " + messageBundle.getMessage("msgTokensCountSuffix"));
            }
            if (remainingTokensField != null) {
                remainingTokensField.setValue("0 " + messageBundle.getMessage("msgTokensCountSuffix"));
            }
            if (quotaStatusBadge != null) {
                quotaStatusBadge.setValue("<span style='color: #64748b; font-size: 11px;'>" + messageBundle.getMessage("msgTokensDefaultHint") + "</span>");
            }
            return;
        }

        ExtUser extUser = (ExtUser) user;
        if (userAiQuotaService == null) {
            return;
        }
        UserAiQuotaInfo quota = userAiQuotaService.getUserQuota(extUser.getId());

        initialQuotaTokens = quota.isUnlimited() ? Integer.valueOf(-1) : quota.getAllocatedTokens();
        if (monthlyQuotaTokensField != null) {
            if (quota.isUnlimited()) {
                monthlyQuotaTokensField.setValue("-1");
            } else if (quota.getAllocatedTokens() != null) {
                monthlyQuotaTokensField.setValue(String.valueOf(quota.getAllocatedTokens()));
            } else {
                monthlyQuotaTokensField.setValue("");
            }
        }

        if (consumedTokensField != null) {
            consumedTokensField.setValue(quota.formatConsumed() + " " + messageBundle.getMessage("msgTokensMonthConsumedNote"));
        }
        if (remainingTokensField != null) {
            String remFormatted = quota.formatRemaining() + " " + messageBundle.getMessage("msgTokensCountSuffix");
            if (quota.getExtraTokens() > 0 && !quota.isUnlimited()) {
                remFormatted += " " + messageBundle.formatMessage("msgExtraTokensActiveNote", quota.formatExtra());
            }
            remainingTokensField.setValue(remFormatted);
        }
        if (quotaStatusBadge != null) {
            String badge = quota.getStatusBadgeHtml();
            String note = quota.isCustomOverride() ? " " + messageBundle.getMessage("msgTokensCustomNote") : " " + messageBundle.getMessage("msgTokensDefaultNote");
            quotaStatusBadge.setValue(badge + " <span style='font-size: 11px; color: #64748b;'>" + note + "</span>");
        }
    }

    private boolean readPendingQuotaInput() {
        if (monthlyQuotaTokensField != null) {
            String val = monthlyQuotaTokensField.getValue();
            if (val != null && !val.trim().isEmpty()) {
                String clean = val.trim().replace(" ", "");
                if (!"-1".equals(clean)) {
                    try {
                        int parsed = Integer.parseInt(clean);
                        if (parsed < -1) {
                            notifications.create(Notifications.NotificationType.WARNING)
                                    .withCaption(messageBundle.getMessage("msgInvalidQuotaFormat"))
                                    .show();
                            return false;
                        }
                        pendingQuotaToSave = parsed;
                    } catch (NumberFormatException e) {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption(messageBundle.getMessage("msgInvalidQuotaFormat"))
                                .show();
                        return false;
                    }
                } else {
                    pendingQuotaToSave = -1;
                }
            } else {
                pendingQuotaToSave = null;
            }
            pendingQuotaChanged = true;
        }
        return true;
    }

    private void saveAdminAiQuotaOverride() {
        User user = userDs.getItem();
        if (!(user instanceof ExtUser) || !pendingQuotaChanged || userAiQuotaService == null) {
            pendingQuotaChanged = false;
            pendingQuotaToSave = null;
            return;
        }
        ExtUser extUser = (ExtUser) user;
        try {
            if (pendingQuotaToSave == null) {
                if (initialQuotaTokens != null) {
                    userAiQuotaService.setMonthlyQuota(extUser.getId(), null, messageBundle.getMessage("msgTokensResetToDefaultReason"));
                    initialQuotaTokens = null;
                }
            } else {
                if (initialQuotaTokens == null || !initialQuotaTokens.equals(pendingQuotaToSave)) {
                    userAiQuotaService.setMonthlyQuota(extUser.getId(), pendingQuotaToSave, messageBundle.getMessage("msgTokensAdminCardSetReason"));
                    initialQuotaTokens = pendingQuotaToSave;
                }
            }
        } catch (Exception ex) {
            log.error("Ошибка сохранения квоты токенов для пользователя {}: {}", extUser.getLogin(), ex.getMessage(), ex);
        } finally {
            pendingQuotaChanged = false;
            pendingQuotaToSave = null;
        }
    }
}

