package com.company.itpearls.web.screens.extsettingswindow;

import com.company.hunttech.app.ImageProcessingService;
import com.company.hunttech.config.HunttechImageConfig;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.UserAiContextService;
import com.company.itpearls.web.util.AvatarImageUploadHelper;
import com.company.itpearls.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.config.MenuItem;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.web.app.ui.core.settings.SettingsWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExtSettingsWindow extends SettingsWindow {

    private static final Logger log = LoggerFactory.getLogger(ExtSettingsWindow.class);
    private static final String QUERY_GET_USER_SETTINGS =
            "select e from itpearls_UserSettings e where e.user = :currentUser";
    private static final String QUERY_GET_USER_AI_PROFILE =
            "select e from itpearls_UserAiProfile e where e.user = :currentUser";
    private static final String AI_PROFILE_CONSENT_VERSION = "2026-07-22-v1";

    @Inject private UserSessionSource userSessionSource;
    @Inject private Metadata metadata;
    @Inject private DataManager dataManager;
    @Inject private FileLoader fileLoader;
    @Inject private FileStorageService fileStorageService;
    @Inject private ImageProcessingService imageProcessingService;
    @Inject private HunttechImageConfig hunttechImageConfig;
    @Inject private UserAiContextService userAiContextService;
    @Inject private Notifications notifications;
    @Inject private Dialogs dialogs;

    @Inject private TextField<String> smtpServer;
    @Inject private CheckBox smtpPasswordRequired;
    @Inject private TextField<String> smtpPassword;
    @Inject private TextField<String> imapServer;
    @Inject private CheckBox imapPasswordRequired;
    @Inject private TextField<String> imapPassword;
    @Inject private TextField<String> pop3Server;
    @Inject private CheckBox pop3PasswordRequired;
    @Inject private TextField<String> pop3Password;
    @Inject private TextField<Integer> smtpPort;
    @Inject private TextField<Integer> imapPort;
    @Inject private TextField<Integer> pop3Port;

    @Inject private Image userPic;
    @Inject private Image defaultPic;
    @Inject private FileUploadField userAvatarUpload;
    @Inject private Datasource<ExtUser> extUserDs;
    @Inject private Datasource<UserAiProfile> userAiProfileDs;

    @Inject private Label<String> userProfileNameLabel;
    @Inject private Label<String> currentPositionSidebarLabel;
    @Inject private Label<String> profileStatusLabel;
    @Inject private Label<String> profileCompletionLabel;
    @Inject private Label<String> profileConfirmedAtLabel;
    @Inject private Label<String> consentAcceptedAtLabel;
    @Inject private TextArea<String> aiContextPreviewArea;
    @Inject private GroupBoxLayout previewGroup;

    @Inject private TextField<String> currentPositionField;
    @Inject private LookupField<AiFunctionalRole> functionalRoleField;
    @Inject private LookupField<AiSeniorityLevel> seniorityLevelField;
    @Inject private LookupField<AiPreferredLanguage> preferredLanguageField;
    @Inject private LookupField<AiResponseDetailLevel> responseDetailLevelField;
    @Inject private LookupField<AiCommunicationStyle> communicationStyleField;
    @Inject private LookupField<AiTerminologyLevel> terminologyLevelField;
    @Inject private LookupField<AiAnswerStructure> preferredAnswerStructureField;
    @Inject private CheckBox profileEnabledField;
    @Inject private CheckBox externalProcessingAllowedField;

    private ExtUser currentUser;
    private UserSettings userSettings;

    @Override
    public void init(Map<String, Object> params) {
        currentUser = (ExtUser) userSessionSource.getUserSession().getUser();
        loadExtUser();
        loadOrCreateUserSettings();
        setEmailSettings();
        loadOrCreateUserAiProfile();
        initAiProfileOptions();
        refreshProfilePhoto();
        refreshProfileSummary();
        super.init(params);

        userAvatarUpload.addFileUploadSucceedListener(event -> onUserAvatarUploaded());
        userAvatarUpload.addBeforeValueClearListener(event -> onUserAvatarCleared());
        // Синхронизирует левую карточку профиля с редактируемыми значениями без сохранения.
        currentPositionField.addValueChangeListener(event -> refreshProfileSummary());
        functionalRoleField.addValueChangeListener(event -> refreshProfileSummary());
        profileEnabledField.addValueChangeListener(event -> refreshProfileSummary());
        externalProcessingAllowedField.addValueChangeListener(event -> refreshProfileSummary());
    }

    private void loadExtUser() {
        extUserDs.setItem(dataManager.load(ExtUser.class)
                .id(currentUser.getId()).view("extUser-view").one());
        currentUser = extUserDs.getItem();
    }

    private void loadOrCreateUserSettings() {
        userSettings = dataManager.load(UserSettings.class)
                .query(QUERY_GET_USER_SETTINGS)
                .parameter("currentUser", currentUser)
                .view("userSettings-view")
                .optional()
                .orElseGet(this::createNewUserSetting);
    }

    // Загружает персональный ИИ-профиль текущего пользователя без автоматического сохранения.
    private void loadOrCreateUserAiProfile() {
        UserAiProfile profile = dataManager.load(UserAiProfile.class)
                .query(QUERY_GET_USER_AI_PROFILE)
                .parameter("currentUser", currentUser)
                .view("userAiProfile-view")
                .optional()
                .orElseGet(this::createUserAiProfile);
        applySafeProfileDefaults(profile);
        userAiProfileDs.setItem(profile);
    }

    private UserAiProfile createUserAiProfile() {
        UserAiProfile profile = metadata.create(UserAiProfile.class);
        profile.setUser(currentUser);
        profile.setProfileEnabled(false);
        profile.setExternalProcessingAllowed(false);
        return profile;
    }

    private void applySafeProfileDefaults(UserAiProfile profile) {
        if (profile.getProfileEnabled() == null) profile.setProfileEnabled(false);
        if (profile.getExternalProcessingAllowed() == null) profile.setExternalProcessingAllowed(false);
        if (profile.getPreferredLanguage() == null) profile.setPreferredLanguage(AiPreferredLanguage.AUTO);
        if (profile.getResponseDetailLevel() == null) profile.setResponseDetailLevel(AiResponseDetailLevel.BALANCED);
        if (profile.getCommunicationStyle() == null) profile.setCommunicationStyle(AiCommunicationStyle.NEUTRAL);
        if (profile.getTerminologyLevel() == null) profile.setTerminologyLevel(AiTerminologyLevel.PROFESSIONAL);
        if (profile.getPreferredAnswerStructure() == null) profile.setPreferredAnswerStructure(AiAnswerStructure.AUTO);
    }

    private void initAiProfileOptions() {
        functionalRoleField.setOptionsMap(options(
                "Рекрутер", AiFunctionalRole.RECRUITER,
                "Ресерчер", AiFunctionalRole.RESEARCHER,
                "Руководитель рекрутмента", AiFunctionalRole.RECRUITMENT_LEAD,
                "Аккаунт-менеджер", AiFunctionalRole.ACCOUNT_MANAGER,
                "HR-менеджер", AiFunctionalRole.HR_MANAGER,
                "Технический эксперт", AiFunctionalRole.TECHNICAL_EXPERT,
                "Руководитель проекта", AiFunctionalRole.PROJECT_MANAGER,
                "Руководитель", AiFunctionalRole.EXECUTIVE,
                "Другая роль", AiFunctionalRole.OTHER));
        seniorityLevelField.setOptionsMap(options(
                "Junior", AiSeniorityLevel.JUNIOR, "Middle", AiSeniorityLevel.MIDDLE,
                "Senior", AiSeniorityLevel.SENIOR, "Lead", AiSeniorityLevel.LEAD,
                "Head", AiSeniorityLevel.HEAD, "Executive", AiSeniorityLevel.EXECUTIVE));
        preferredLanguageField.setOptionsMap(options(
                "Автоматически", AiPreferredLanguage.AUTO,
                "Русский", AiPreferredLanguage.RUSSIAN,
                "Английский", AiPreferredLanguage.ENGLISH));
        responseDetailLevelField.setOptionsMap(options(
                "Кратко", AiResponseDetailLevel.BRIEF,
                "Сбалансированно", AiResponseDetailLevel.BALANCED,
                "Подробно", AiResponseDetailLevel.DETAILED));
        communicationStyleField.setOptionsMap(options(
                "Прямой", AiCommunicationStyle.DIRECT,
                "Нейтральный", AiCommunicationStyle.NEUTRAL,
                "Обучающий", AiCommunicationStyle.COACHING));
        terminologyLevelField.setOptionsMap(options(
                "Простой", AiTerminologyLevel.PLAIN,
                "Профессиональный", AiTerminologyLevel.PROFESSIONAL,
                "Экспертный", AiTerminologyLevel.EXPERT));
        preferredAnswerStructureField.setOptionsMap(options(
                "Автоматически", AiAnswerStructure.AUTO,
                "Управленческая сводка", AiAnswerStructure.EXECUTIVE_SUMMARY,
                "План действий", AiAnswerStructure.ACTION_PLAN,
                "Пошаговая инструкция", AiAnswerStructure.STEP_BY_STEP,
                "Чек-лист", AiAnswerStructure.CHECKLIST,
                "Таблица", AiAnswerStructure.TABLE));
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, T> options(Object... values) {
        Map<String, T> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put((String) values[i], (T) values[i + 1]);
        }
        return result;
    }

    private void onUserAvatarUploaded() {
        ExtUser user = extUserDs.getItem();
        if (user == null) return;
        FileDescriptor newAvatar = processUploadedAvatar(userAvatarUpload.getFileDescriptor());
        FileDescriptor oldAvatar = user.getUserAvatar();
        removeStoredFileIfUnreferenced(oldAvatar, user.getOfficialPhoto(), newAvatar);
        user.setUserAvatar(newAvatar);
        refreshProfilePhoto();
    }

    private void onUserAvatarCleared() {
        ExtUser user = extUserDs.getItem();
        if (user == null) return;
        removeStoredFileIfUnreferenced(user.getUserAvatar(), user.getOfficialPhoto(), null);
        user.setUserAvatar(null);
        refreshProfilePhoto();
    }

    private FileDescriptor processUploadedAvatar(FileDescriptor descriptor) {
        log.debug("Processing user avatar upload with limits targetImageSize={}, targetImageFormat={}",
                hunttechImageConfig.getTargetImageSize(), hunttechImageConfig.getTargetImageFormat());
        return AvatarImageUploadHelper.processUploadedImage(
                descriptor, fileLoader, fileStorageService, dataManager, imageProcessingService, log);
    }

    private void removeStoredFileIfUnreferenced(FileDescriptor oldFile,
                                                FileDescriptor stillReferenced,
                                                FileDescriptor replacement) {
        if (oldFile == null || Objects.equals(oldFile, replacement)) return;
        if (stillReferenced != null && Objects.equals(oldFile.getId(), stillReferenced.getId())) return;
        try {
            fileStorageService.removeFile(oldFile);
        } catch (FileStorageException e) {
            log.warn("Cannot remove old user avatar id={}: {}", oldFile.getId(), e.getMessage());
        }
    }

    private void refreshProfilePhoto() {
        ExtUser user = extUserDs.getItem();
        userPic.setValueSource(null);
        FileDescriptor photo = user != null ? user.resolveProfilePhoto() : null;
        if (FileDescriptorImageHelper.fileExists(fileLoader, photo)) {
            userPic.setVisible(true);
            defaultPic.setVisible(false);
            FileDescriptorImageHelper.setUserProfilePhoto(userPic, fileLoader, user);
        } else {
            userPic.setVisible(false);
            defaultPic.setVisible(true);
        }
    }

    private void refreshProfileSummary() {
        UserAiProfile profile = userAiProfileDs.getItem();
        String displayName = currentUser.getName() != null ? currentUser.getName() : currentUser.getLogin();
        userProfileNameLabel.setValue(displayName);
        currentPositionSidebarLabel.setValue(profile != null && !isBlank(profile.getCurrentPosition())
                ? profile.getCurrentPosition() : getMessage("positionNotSpecified"));
        boolean active = profile != null
                && Boolean.TRUE.equals(profile.getProfileEnabled())
                && Boolean.TRUE.equals(profile.getExternalProcessingAllowed());
        profileStatusLabel.setValue(getMessage(active ? "aiProfileActive" : "aiProfileInactive"));
        profileCompletionLabel.setValue(getMessage("profileCompletion") + ": "
                + calculateProfileCompletion(profile) + "%");
        profileConfirmedAtLabel.setValue(getMessage("profileConfirmedAt") + ": "
                + formatDate(profile == null ? null : profile.getProfileConfirmedAt()));
        consentAcceptedAtLabel.setValue(getMessage("consentAcceptedAt") + ": "
                + formatDate(profile == null ? null : profile.getConsentAcceptedAt()));
    }

    private int calculateProfileCompletion(UserAiProfile profile) {
        if (profile == null) return 0;
        int filled = 0;
        filled += isBlank(profile.getAboutMe()) ? 0 : 1;
        filled += isBlank(profile.getCurrentPosition()) ? 0 : 1;
        filled += profile.getFunctionalRole() == null ? 0 : 1;
        filled += profile.getSeniorityLevel() == null ? 0 : 1;
        filled += isBlank(profile.getCurrentResponsibilities()) ? 0 : 1;
        filled += isBlank(profile.getEducation()) ? 0 : 1;
        filled += isBlank(profile.getDomainExpertise()) ? 0 : 1;
        filled += isBlank(profile.getRecruitingSpecializations()) ? 0 : 1;
        filled += isBlank(profile.getProfessionalGoals()) ? 0 : 1;
        filled += profile.getResponseDetailLevel() == null ? 0 : 1;
        return filled * 10;
    }

    private void setEmailSettings() {
        userSettings.setUser((ExtUser) userSession.getUser());
        smtpServer.setValue(firstNotNull(userSettings.getSmtpServer(), currentUser.getSmtpServer()));
        smtpPort.setValue(firstNotNull(userSettings.getSmtpPort(), currentUser.getSmtpPort(), 0));
        smtpPasswordRequired.setValue(firstNotNull(
                userSettings.getSmtpPasswordRequired(), currentUser.getSmtpPasswordRequired()));
        smtpPassword.setValue(firstNotNull(userSettings.getSmtpPassword(), currentUser.getSmtpPassword()));
        imapServer.setValue(firstNotNull(userSettings.getImapServer(), currentUser.getImapServer()));
        imapPort.setValue(firstNotNull(userSettings.getImapPort(), currentUser.getImapPort(), 0));
        imapPasswordRequired.setValue(firstNotNull(
                userSettings.getImapPasswordRequired(), currentUser.getImapPasswordRequired()));
        imapPassword.setValue(firstNotNull(userSettings.getImapPassword(), currentUser.getImapPassword()));
        pop3Server.setValue(firstNotNull(userSettings.getPop3Server(), currentUser.getPop3Server()));
        pop3Port.setValue(firstNotNull(userSettings.getPop3Port(), currentUser.getPop3Port(), 0));
        pop3PasswordRequired.setValue(firstNotNull(
                userSettings.getPop3PasswordRequired(), currentUser.getPop3PasswordRequired()));
        pop3Password.setValue(firstNotNull(userSettings.getPop3Password(), currentUser.getPop3Password()));
    }

    @SafeVarargs
    private final <T> T firstNotNull(T... values) {
        for (T value : values) if (value != null) return value;
        return null;
    }

    private UserSettings createNewUserSetting() {
        UserSettings settings = metadata.create(UserSettings.class);
        settings.setUser((ExtUser) userSession.getUser());
        return settings;
    }

    public void previewAiContext() {
        // Формирует очищенный предпросмотр данных, разрешённых к передаче ИИ-провайдеру.
        aiContextPreviewArea.setValue(userAiContextService.buildContextPreview(userAiProfileDs.getItem()));
        previewGroup.setExpanded(true);
    }

    public void clearAiProfile() {
        dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                .withCaption(getMessage("clearAiProfile"))
                .withMessage(getMessage("clearAiProfileConfirmation"))
                .withActions(
                        new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                .withHandler(event -> {
                                    resetAiProfileFields(userAiProfileDs.getItem());
                                    aiContextPreviewArea.setValue("");
                                    refreshProfileSummary();
                                }),
                        new DialogAction(DialogAction.Type.NO))
                .show();
    }

    // Очищает только профессиональный ИИ-профиль и не затрагивает учётную запись и аватар.
    private void resetAiProfileFields(UserAiProfile profile) {
        if (profile == null) return;
        profile.setProfileEnabled(false);
        profile.setExternalProcessingAllowed(false);
        profile.setConsentVersion(null);
        profile.setConsentAcceptedAt(null);
        profile.setProfileConfirmedAt(null);
        profile.setAboutMe(null);
        profile.setCurrentPosition(null);
        profile.setFunctionalRole(null);
        profile.setSeniorityLevel(null);
        profile.setProfessionalExperienceYears(null);
        profile.setRecruitingExperienceYears(null);
        profile.setCurrentResponsibilities(null);
        profile.setEducation(null);
        profile.setCertifications(null);
        profile.setDomainExpertise(null);
        profile.setIndustries(null);
        profile.setRecruitingSpecializations(null);
        profile.setTargetRoles(null);
        profile.setCandidateLevels(null);
        profile.setHiringGeographies(null);
        profile.setDecisionPriorities(null);
        profile.setClientAndProjectContext(null);
        profile.setProfessionalGoals(null);
        profile.setProfessionalInterests(null);
        profile.setDevelopmentAreas(null);
        profile.setCurrentPriorities(null);
        profile.setPreferredLanguage(AiPreferredLanguage.AUTO);
        profile.setResponseDetailLevel(AiResponseDetailLevel.BALANCED);
        profile.setCommunicationStyle(AiCommunicationStyle.NEUTRAL);
        profile.setTerminologyLevel(AiTerminologyLevel.PROFESSIONAL);
        profile.setPreferredAnswerStructure(AiAnswerStructure.AUTO);
        profile.setCustomAiInstructions(null);
        profile.setCommunicationConstraints(null);
        userAiProfileDs.setItem(profile);
    }

    @Override
    protected void commit() {
        UserAiProfile profile = userAiProfileDs.getItem();
        if (!validateAiProfile(profile)) return;
        prepareProfileConsent(profile);
        collectEmailSettings();

        // Сохраняет связанные настройки одной транзакцией, чтобы исключить частично обновлённый профиль.
        CommitContext context = new CommitContext();
        context.addInstanceToCommit(userSettings);
        if (extUserDs.getItem() != null) context.addInstanceToCommit(extUserDs.getItem());
        if (profile != null) context.addInstanceToCommit(profile);
        dataManager.commit(context);
        super.commit();
    }

    private void collectEmailSettings() {
        userSettings.setSmtpServer(smtpServer.getValue());
        userSettings.setSmtpPassword(smtpPassword.getValue());
        userSettings.setSmtpPasswordRequired(smtpPasswordRequired.getValue());
        userSettings.setSmtpPort(firstNotNull(smtpPort.getValue(), 0));
        userSettings.setImapServer(imapServer.getValue());
        userSettings.setImapPassword(imapPassword.getValue());
        userSettings.setImapPasswordRequired(imapPasswordRequired.getValue());
        userSettings.setImapPort(firstNotNull(imapPort.getValue(), 0));
        userSettings.setPop3Server(pop3Server.getValue());
        userSettings.setPop3Password(pop3Password.getValue());
        userSettings.setPop3PasswordRequired(pop3PasswordRequired.getValue());
        userSettings.setPop3Port(firstNotNull(pop3Port.getValue(), 0));
    }

    private boolean validateAiProfile(UserAiProfile profile) {
        if (profile == null) return true;
        // Не позволяет активировать персонализацию без подтверждённого согласия пользователя.
        if (Boolean.TRUE.equals(profile.getProfileEnabled())
                && !Boolean.TRUE.equals(profile.getExternalProcessingAllowed())) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(getMessage("profileConsentRequired")).show();
            return false;
        }
        if (!isExperienceValid(profile.getProfessionalExperienceYears())
                || !isExperienceValid(profile.getRecruitingExperienceYears())) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(getMessage("experienceValidationMessage")).show();
            return false;
        }
        return true;
    }

    private boolean isExperienceValid(Integer value) {
        return value == null || value >= 0 && value <= 70;
    }

    private void prepareProfileConsent(UserAiProfile profile) {
        if (profile == null) return;
        Date now = new Date();
        if (Boolean.TRUE.equals(profile.getExternalProcessingAllowed())) {
            if (profile.getConsentAcceptedAt() == null) profile.setConsentAcceptedAt(now);
            profile.setConsentVersion(AI_PROFILE_CONSENT_VERSION);
        } else {
            profile.setProfileEnabled(false);
            profile.setConsentVersion(null);
            profile.setConsentAcceptedAt(null);
        }
        if (Boolean.TRUE.equals(profile.getProfileEnabled())) profile.setProfileConfirmedAt(now);
        refreshProfileSummary();
    }

    private String formatDate(Date date) {
        return date == null ? getMessage("notSpecified")
                : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override protected void initDefaultScreenField() { super.initDefaultScreenField(); }
    @Override protected Map<String, String> collectScreens() { return super.collectScreens(); }
    @Override protected List<MenuItem> collectPermittedScreens(List<MenuItem> menuItems) {
        return super.collectPermittedScreens(menuItems);
    }
    @Override protected void initTimeZoneFields() { super.initTimeZoneFields(); }
    @Override protected void saveTimeZoneSettings() { super.saveTimeZoneSettings(); }
    @Override protected void saveLocaleSettings() { super.saveLocaleSettings(); }
    @Override protected void resetScreenSettings() { super.resetScreenSettings(); }
    @Override protected Set<String> getAllWindowIds() { return super.getAllWindowIds(); }
}
