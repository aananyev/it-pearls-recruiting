package com.company.hunttech.web.screens.extsettingswindow;

import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.UserAiProfile;
import com.company.hunttech.service.UserAiContextBuilder;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.GroupBoxLayout;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.OptionsGroup;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.data.Datasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * Связывает навигационные индексы всех вкладок ExtSettingsWindow
 * с существующими рабочими блоками справа.
 *
 * Загрузка, редактирование, валидация и сохранение настроек остаются
 * в базовом контроллере; расширение управляет presentation state и локальным preview.
 */
public class ExtSettingsWindowEmailNavigation extends ExtSettingsWindow {

    private static final Logger log = LoggerFactory.getLogger(ExtSettingsWindowEmailNavigation.class);
    private static final String NAVIGATION_STYLE = "borderless settings-section-nav-item label-nav-item";
    private static final String ACTIVE_NAVIGATION_STYLE =
            "borderless settings-section-nav-item settings-section-nav-item-active label-nav-item label-nav-item-active";
    private static final String AI_NAVIGATION_STYLE = "borderless ai-settings-nav-item label-nav-item";
    private static final String ACTIVE_AI_NAVIGATION_STYLE =
            "borderless ai-settings-nav-item ai-settings-nav-item-active label-nav-item label-nav-item-active";
    private static final String PROFILE_NAVIGATION_STYLE = "borderless user-ai-profile-nav-item label-nav-item";
    private static final String ACTIVE_PROFILE_NAVIGATION_STYLE =
            "borderless user-ai-profile-nav-item user-ai-profile-nav-item-active label-nav-item label-nav-item-active";

    @Inject
    private UiComponents uiComponents;
    @Inject
    private Notifications notifications;

    @Inject
    private VBoxLayout emailSettingsNavigation;
    @Inject
    private GroupBoxLayout smtpSettingsSection;
    @Inject
    private GroupBoxLayout pop3SettingsSection;
    @Inject
    private GroupBoxLayout imapSettingsSection;
    @Inject
    private TextField<String> smtpServer;
    @Inject
    private TextField<String> pop3Server;
    @Inject
    private TextField<String> imapServer;

    @Inject
    private VBoxLayout aiSettingsNavigation;
    @Inject
    private CheckBox preferPersonalAiApiSettingsField;
    @Inject
    private Table<UserAiConfiguration> aiConfigsTable;

    @Inject
    private VBoxLayout userAiProfileSectionNavigation;
    @Inject
    private GroupBoxLayout professionalProfileGroup;
    @Inject
    private GroupBoxLayout recruitingProfileGroup;
    @Inject
    private GroupBoxLayout responsePreferencesGroup;
    @Inject
    private GroupBoxLayout goalsGroup;
    @Inject
    private GroupBoxLayout privacyGroup;
    @Inject
    private GroupBoxLayout previewGroup;
    @Inject
    private TextField<String> currentPositionField;
    @Inject
    private TextArea<String> recruitingSpecializationsField;
    @Inject
    private LookupField preferredLanguageField;
    @Inject
    private TextArea<String> professionalGoalsField;
    @Inject
    private CheckBox profileEnabledField;
    @Inject
    private TextArea<String> aiContextPreviewArea;
    @Inject
    private Datasource<UserAiProfile> userAiProfileDs;

    @Inject
    private VBoxLayout interfaceSettingsNavigation;
    @Inject
    private OptionsGroup modeOptions;
    @Inject
    private LookupField appThemeField;
    @Inject
    private LookupField appLangField;
    @Inject
    private LookupField defaultScreenField;

    @Inject
    private TabSheet settingsTabSheet;

    private boolean initialized;
    private Button emailSettingsSmtpNav;
    private Button emailSettingsPop3Nav;
    private Button emailSettingsImapNav;
    private Button aiSettingsSourceNav;
    private Button aiSettingsConnectionsNav;
    private Button userAiProfileProfessionalNav;
    private Button userAiProfileRecruitingNav;
    private Button userAiProfileResponseNav;
    private Button userAiProfileGoalsNav;
    private Button userAiProfilePrivacyNav;
    private Button userAiProfilePreviewNav;
    private Button interfaceSettingsWindowNav;
    private Button interfaceSettingsAppearanceNav;
    private Button interfaceSettingsRegionalNav;
    private Button interfaceSettingsStartupNav;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        initEmailSettingsNavigation();
        initAiSettingsNavigation();
        initUserAiProfileNavigation();
        initInterfaceSettingsNavigation();
        initTabSheetSync();
        this.initialized = true;
    }

    private void initTabSheetSync() {
        if (settingsTabSheet != null) {
            settingsTabSheet.addSelectedTabChangeListener(event -> {
                TabSheet.Tab selectedTab = event.getSelectedTab();
                if (selectedTab == null || selectedTab.getName() == null) {
                    return;
                }
                String tabName = selectedTab.getName();
                if ("msgMyInfo".equals(tabName)) {
                    updateUserAiProfileNavigationStyles(userAiProfileProfessionalNav);
                } else if ("msgInterface".equals(tabName)) {
                    updateInterfaceNavigationStyles(interfaceSettingsWindowNav);
                } else if ("mailAccessTab".equals(tabName)) {
                    if (emailSettingsSmtpNav != null) emailSettingsSmtpNav.setStyleName(ACTIVE_NAVIGATION_STYLE);
                    if (emailSettingsPop3Nav != null) emailSettingsPop3Nav.setStyleName(NAVIGATION_STYLE);
                    if (emailSettingsImapNav != null) emailSettingsImapNav.setStyleName(NAVIGATION_STYLE);
                } else if ("aiAccessTab".equals(tabName)) {
                    updateAiNavigationStyles(aiSettingsSourceNav);
                }
            });
        }
    }

    /**
     * Формирует предпросмотр из текущего состояния datasource, включая несохранённые изменения.
     * Удалённый вызов здесь не используется: передача редактируемой CUBA entity в middleware
     * не нужна для UI-сценария и делала кнопку зависимой от remoting/serialization.
     */
    @Override
    public void previewAiContext() {
        UserAiProfile profile = userAiProfileDs == null ? null : userAiProfileDs.getItem();
        if (profile == null) {
            showAiContextPreviewError("aiContextPreviewUnavailable");
            return;
        }
        if (aiContextPreviewArea == null || previewGroup == null) {
            log.error("AI context preview components are not injected: area={}, group={}",
                    aiContextPreviewArea != null, previewGroup != null);
            showAiContextPreviewError("aiContextPreviewError");
            return;
        }

        try {
            aiContextPreviewArea.setValue(UserAiContextBuilder.buildPreview(profile));
            previewGroup.setExpanded(true);
            updateUserAiProfileNavigationStyles(userAiProfilePreviewNav);
            // Фокус прокручивает длинную форму к раскрытому результату и делает действие видимым пользователю.
            aiContextPreviewArea.focus();
        } catch (RuntimeException e) {
            log.error("Cannot build AI context preview from current datasource", e);
            showAiContextPreviewError("aiContextPreviewError");
        }
    }

    private void showAiContextPreviewError(String messageKey) {
        notifications.create(Notifications.NotificationType.WARNING)
                .withCaption(getMessage(messageKey))
                .withPosition(Notifications.Position.BOTTOM_RIGHT)
                .show();
    }

    /**
     * Сохраняет заголовок существующей навигации и заменяет только три некликабельных
     * пункта протоколов на визуально идентичные кнопки CUBA.
     */
    private void initEmailSettingsNavigation() {
        Component navigationTitle = emailSettingsNavigation.getComponent(0);
        emailSettingsNavigation.removeAll();
        emailSettingsNavigation.add(navigationTitle);

        emailSettingsSmtpNav = createNavigationButton(
                "emailSettingsSmtpNav", "emailSettingsSmtpSection",
                this::selectSmtpSettings, NAVIGATION_STYLE);
        emailSettingsPop3Nav = createNavigationButton(
                "emailSettingsPop3Nav", "emailSettingsPop3Section",
                this::selectPop3Settings, NAVIGATION_STYLE);
        emailSettingsImapNav = createNavigationButton(
                "emailSettingsImapNav", "emailSettingsImapSection",
                this::selectImapSettings, NAVIGATION_STYLE);

        emailSettingsNavigation.add(emailSettingsSmtpNav);
        emailSettingsNavigation.add(emailSettingsPop3Nav);
        emailSettingsNavigation.add(emailSettingsImapNav);
        selectEmailSettingsSection(smtpSettingsSection, smtpServer, emailSettingsSmtpNav);
    }

    /**
     * Превращает существующий AI-индекс в навигацию, не меняя карточки предпочтений,
     * таблицу подключений и операции управления конфигурациями.
     */
    private void initAiSettingsNavigation() {
        Component navigationTitle = aiSettingsNavigation.getComponent(0);
        aiSettingsNavigation.removeAll();
        aiSettingsNavigation.add(navigationTitle);

        aiSettingsSourceNav = createNavigationButton(
                "aiSettingsSourceNav", "aiSettingsSourceSection",
                this::selectAiSourceSettings, AI_NAVIGATION_STYLE);
        aiSettingsConnectionsNav = createNavigationButton(
                "aiSettingsConnectionsNav", "aiSettingsConnectionsSection",
                this::selectAiConnectionsSettings, AI_NAVIGATION_STYLE);

        aiSettingsNavigation.add(aiSettingsSourceNav);
        aiSettingsNavigation.add(aiSettingsConnectionsNav);
        // Скрытая при открытии вкладка получает только активный стиль; фокус устанавливается после клика пользователя.
        updateAiNavigationStyles(aiSettingsSourceNav);
    }

    /**
     * Связывает шесть пунктов профиля пользователя с шестью существующими
     * секциями-аккордеонами без изменения datasource-binding и consent-логики.
     */
    private void initUserAiProfileNavigation() {
        Component navigationTitle = userAiProfileSectionNavigation.getComponent(0);
        userAiProfileSectionNavigation.removeAll();
        userAiProfileSectionNavigation.add(navigationTitle);

        userAiProfileProfessionalNav = createNavigationButton(
                "userAiProfileProfessionalNav", "professionalProfile",
                this::selectProfessionalProfile, PROFILE_NAVIGATION_STYLE);
        userAiProfileRecruitingNav = createNavigationButton(
                "userAiProfileRecruitingNav", "recruitingProfile",
                this::selectRecruitingProfile, PROFILE_NAVIGATION_STYLE);
        userAiProfileResponseNav = createNavigationButton(
                "userAiProfileResponseNav", "responsePreferences",
                this::selectResponsePreferences, PROFILE_NAVIGATION_STYLE);
        userAiProfileGoalsNav = createNavigationButton(
                "userAiProfileGoalsNav", "goalsAndInterests",
                this::selectGoalsAndInterests, PROFILE_NAVIGATION_STYLE);
        userAiProfilePrivacyNav = createNavigationButton(
                "userAiProfilePrivacyNav", "privacyAndBoundaries",
                this::selectPrivacyAndBoundaries, PROFILE_NAVIGATION_STYLE);
        userAiProfilePreviewNav = createNavigationButton(
                "userAiProfilePreviewNav", "aiContextPreview",
                this::selectAiContextPreview, PROFILE_NAVIGATION_STYLE);

        userAiProfileSectionNavigation.add(userAiProfileProfessionalNav);
        userAiProfileSectionNavigation.add(userAiProfileRecruitingNav);
        userAiProfileSectionNavigation.add(userAiProfileResponseNav);
        userAiProfileSectionNavigation.add(userAiProfileGoalsNav);
        userAiProfileSectionNavigation.add(userAiProfilePrivacyNav);
        userAiProfileSectionNavigation.add(userAiProfilePreviewNav);
        // Вкладка «Обо мне» уже содержит раскрытый первый аккордеон; синхронизируем только подсветку.
        updateUserAiProfileNavigationStyles(userAiProfileProfessionalNav);
    }

    /**
     * Делает индекс интерфейсных настроек кликабельным. Вкладка содержит одну
     * штатную карточку, поэтому выбор переводит фокус на первое поле нужной группы,
     * не скрывая и не перестраивая legacy-компоненты SettingsWindow.
     */
    private void initInterfaceSettingsNavigation() {
        Component navigationTitle = interfaceSettingsNavigation.getComponent(0);
        interfaceSettingsNavigation.removeAll();
        interfaceSettingsNavigation.add(navigationTitle);

        interfaceSettingsWindowNav = createNavigationButton(
                "interfaceSettingsWindowNav", "interfaceSettingsWindowSection",
                this::selectInterfaceWindowSettings, NAVIGATION_STYLE);
        interfaceSettingsAppearanceNav = createNavigationButton(
                "interfaceSettingsAppearanceNav", "interfaceSettingsAppearanceSection",
                this::selectInterfaceAppearanceSettings, NAVIGATION_STYLE);
        interfaceSettingsRegionalNav = createNavigationButton(
                "interfaceSettingsRegionalNav", "interfaceSettingsRegionalSection",
                this::selectInterfaceRegionalSettings, NAVIGATION_STYLE);
        interfaceSettingsStartupNav = createNavigationButton(
                "interfaceSettingsStartupNav", "interfaceSettingsStartupSection",
                this::selectInterfaceStartupSettings, NAVIGATION_STYLE);

        interfaceSettingsNavigation.add(interfaceSettingsWindowNav);
        interfaceSettingsNavigation.add(interfaceSettingsAppearanceNav);
        interfaceSettingsNavigation.add(interfaceSettingsRegionalNav);
        interfaceSettingsNavigation.add(interfaceSettingsStartupNav);
        // Скрытая при открытии вкладка не получает фокус до явного выбора пользователя.
        updateInterfaceNavigationStyles(interfaceSettingsWindowNav);
    }

    private Button createNavigationButton(String id,
                                          String messageKey,
                                          Runnable handler,
                                          String styleName) {
        Button button = uiComponents.create(Button.class);
        button.setId(id);
        button.setCaption(getMessage(messageKey));
        button.setWidth("100%");
        button.setStyleName(styleName);
        button.addClickListener(event -> handler.run());
        return button;
    }

    public void selectSmtpSettings() {
        selectEmailSettingsSection(smtpSettingsSection, smtpServer, emailSettingsSmtpNav);
    }

    public void selectPop3Settings() {
        selectEmailSettingsSection(pop3SettingsSection, pop3Server, emailSettingsPop3Nav);
    }

    public void selectImapSettings() {
        selectEmailSettingsSection(imapSettingsSection, imapServer, emailSettingsImapNav);
    }

    /**
     * Активирует карточку персонального источника через первое доступное предпочтение.
     * Фокус является только UI-навигацией и не изменяет значение checkbox.
     */
    public void selectAiSourceSettings() {
        if (initialized && settingsTabSheet != null) {
            settingsTabSheet.setSelectedTab("aiAccessTab");
        }
        updateAiNavigationStyles(aiSettingsSourceNav);
        preferPersonalAiApiSettingsField.focus();
    }

    /**
     * Активирует блок подключений и переводит фокус на существующую таблицу.
     * Выбранная строка и состояние кнопок редактирования при этом не меняются.
     */
    public void selectAiConnectionsSettings() {
        if (initialized && settingsTabSheet != null) {
            settingsTabSheet.setSelectedTab("aiAccessTab");
        }
        updateAiNavigationStyles(aiSettingsConnectionsNav);
        aiConfigsTable.focus();
    }

    public void selectProfessionalProfile() {
        selectUserAiProfileSection(
                professionalProfileGroup, userAiProfileProfessionalNav, currentPositionField::focus);
    }

    public void selectRecruitingProfile() {
        selectUserAiProfileSection(
                recruitingProfileGroup, userAiProfileRecruitingNav, recruitingSpecializationsField::focus);
    }

    public void selectResponsePreferences() {
        selectUserAiProfileSection(
                responsePreferencesGroup, userAiProfileResponseNav, preferredLanguageField::focus);
    }

    public void selectGoalsAndInterests() {
        selectUserAiProfileSection(
                goalsGroup, userAiProfileGoalsNav, professionalGoalsField::focus);
    }

    public void selectPrivacyAndBoundaries() {
        selectUserAiProfileSection(
                privacyGroup, userAiProfilePrivacyNav, profileEnabledField::focus);
    }

    public void selectAiContextPreview() {
        selectUserAiProfileSection(
                previewGroup, userAiProfilePreviewNav, aiContextPreviewArea::focus);
    }

    public void selectInterfaceWindowSettings() {
        if (initialized && settingsTabSheet != null) {
            settingsTabSheet.setSelectedTab("msgInterface");
        }
        updateInterfaceNavigationStyles(interfaceSettingsWindowNav);
        modeOptions.focus();
    }

    public void selectInterfaceAppearanceSettings() {
        if (initialized && settingsTabSheet != null) {
            settingsTabSheet.setSelectedTab("msgInterface");
        }
        updateInterfaceNavigationStyles(interfaceSettingsAppearanceNav);
        appThemeField.focus();
    }

    public void selectInterfaceRegionalSettings() {
        if (initialized && settingsTabSheet != null) {
            settingsTabSheet.setSelectedTab("msgInterface");
        }
        updateInterfaceNavigationStyles(interfaceSettingsRegionalNav);
        appLangField.focus();
    }

    public void selectInterfaceStartupSettings() {
        if (initialized && settingsTabSheet != null) {
            settingsTabSheet.setSelectedTab("msgInterface");
        }
        updateInterfaceNavigationStyles(interfaceSettingsStartupNav);
        defaultScreenField.focus();
    }

    /**
     * Реализует взаимоисключающий выбор протокола: раскрывает выбранную секцию,
     * сворачивает остальные, синхронизирует активный пункт слева и переводит
     * фокус в первое поле выбранного блока. Фокус одновременно прокручивает
     * вертикальный ScrollBox до раскрытой секции без изменения данных формы.
     */
    private void selectEmailSettingsSection(GroupBoxLayout selectedSection,
                                            TextField<String> selectedFirstField,
                                            Button selectedNavigationButton) {
        if (initialized && settingsTabSheet != null) {
            settingsTabSheet.setSelectedTab("mailAccessTab");
        }
        smtpSettingsSection.setExpanded(smtpSettingsSection == selectedSection);
        pop3SettingsSection.setExpanded(pop3SettingsSection == selectedSection);
        imapSettingsSection.setExpanded(imapSettingsSection == selectedSection);

        emailSettingsSmtpNav.setStyleName(
                emailSettingsSmtpNav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        emailSettingsPop3Nav.setStyleName(
                emailSettingsPop3Nav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        emailSettingsImapNav.setStyleName(
                emailSettingsImapNav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);

        if (initialized) {
            selectedFirstField.focus();
        }
    }

    /**
     * Раскрывает выбранный раздел профиля, сворачивает остальные и переводит
     * фокус на первое штатное поле. Значения сущности и состояние consent не меняются.
     */
    private void selectUserAiProfileSection(GroupBoxLayout selectedSection,
                                            Button selectedNavigationButton,
                                            Runnable focusHandler) {
        if (initialized && settingsTabSheet != null) {
            settingsTabSheet.setSelectedTab("msgMyInfo");
        }
        professionalProfileGroup.setExpanded(professionalProfileGroup == selectedSection);
        recruitingProfileGroup.setExpanded(recruitingProfileGroup == selectedSection);
        responsePreferencesGroup.setExpanded(responsePreferencesGroup == selectedSection);
        goalsGroup.setExpanded(goalsGroup == selectedSection);
        privacyGroup.setExpanded(privacyGroup == selectedSection);
        previewGroup.setExpanded(previewGroup == selectedSection);

        updateUserAiProfileNavigationStyles(selectedNavigationButton);
        if (initialized) {
            focusHandler.run();
        }
    }

    private void updateAiNavigationStyles(Button selectedNavigationButton) {
        aiSettingsSourceNav.setStyleName(
                aiSettingsSourceNav == selectedNavigationButton
                        ? ACTIVE_AI_NAVIGATION_STYLE : AI_NAVIGATION_STYLE);
        aiSettingsConnectionsNav.setStyleName(
                aiSettingsConnectionsNav == selectedNavigationButton
                        ? ACTIVE_AI_NAVIGATION_STYLE : AI_NAVIGATION_STYLE);
    }

    private void updateUserAiProfileNavigationStyles(Button selectedNavigationButton) {
        userAiProfileProfessionalNav.setStyleName(
                userAiProfileProfessionalNav == selectedNavigationButton
                        ? ACTIVE_PROFILE_NAVIGATION_STYLE : PROFILE_NAVIGATION_STYLE);
        userAiProfileRecruitingNav.setStyleName(
                userAiProfileRecruitingNav == selectedNavigationButton
                        ? ACTIVE_PROFILE_NAVIGATION_STYLE : PROFILE_NAVIGATION_STYLE);
        userAiProfileResponseNav.setStyleName(
                userAiProfileResponseNav == selectedNavigationButton
                        ? ACTIVE_PROFILE_NAVIGATION_STYLE : PROFILE_NAVIGATION_STYLE);
        userAiProfileGoalsNav.setStyleName(
                userAiProfileGoalsNav == selectedNavigationButton
                        ? ACTIVE_PROFILE_NAVIGATION_STYLE : PROFILE_NAVIGATION_STYLE);
        userAiProfilePrivacyNav.setStyleName(
                userAiProfilePrivacyNav == selectedNavigationButton
                        ? ACTIVE_PROFILE_NAVIGATION_STYLE : PROFILE_NAVIGATION_STYLE);
        userAiProfilePreviewNav.setStyleName(
                userAiProfilePreviewNav == selectedNavigationButton
                        ? ACTIVE_PROFILE_NAVIGATION_STYLE : PROFILE_NAVIGATION_STYLE);
    }

    private void updateInterfaceNavigationStyles(Button selectedNavigationButton) {
        interfaceSettingsWindowNav.setStyleName(
                interfaceSettingsWindowNav == selectedNavigationButton
                        ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        interfaceSettingsAppearanceNav.setStyleName(
                interfaceSettingsAppearanceNav == selectedNavigationButton
                        ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        interfaceSettingsRegionalNav.setStyleName(
                interfaceSettingsRegionalNav == selectedNavigationButton
                        ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        interfaceSettingsStartupNav.setStyleName(
                interfaceSettingsStartupNav == selectedNavigationButton
                        ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
    }
}
