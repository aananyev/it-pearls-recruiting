package com.company.hunttech.web.screens.extsettingswindow;

import com.company.hunttech.entity.UserAiConfiguration;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.GroupBoxLayout;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.VBoxLayout;

import javax.inject.Inject;
import java.util.Map;

/**
 * Дополняет вкладки email и AI связью между навигацией слева и рабочими блоками справа.
 * Загрузка, редактирование и сохранение настроек остаются в базовом контроллере.
 */
public class ExtSettingsWindowEmailNavigation extends ExtSettingsWindow {

    private static final String NAVIGATION_STYLE = "borderless settings-section-nav-item";
    private static final String ACTIVE_NAVIGATION_STYLE =
            "borderless settings-section-nav-item settings-section-nav-item-active";
    private static final String AI_NAVIGATION_STYLE = "borderless ai-settings-nav-item";
    private static final String ACTIVE_AI_NAVIGATION_STYLE =
            "borderless ai-settings-nav-item ai-settings-nav-item-active";

    @Inject
    private UiComponents uiComponents;
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

    private Button emailSettingsSmtpNav;
    private Button emailSettingsPop3Nav;
    private Button emailSettingsImapNav;
    private Button aiSettingsSourceNav;
    private Button aiSettingsConnectionsNav;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        initEmailSettingsNavigation();
        initAiSettingsNavigation();
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
        updateAiNavigationStyles(aiSettingsSourceNav);
        preferPersonalAiApiSettingsField.focus();
    }

    /**
     * Активирует блок подключений и переводит фокус на существующую таблицу.
     * Выбранная строка и состояние кнопок редактирования при этом не меняются.
     */
    public void selectAiConnectionsSettings() {
        updateAiNavigationStyles(aiSettingsConnectionsNav);
        aiConfigsTable.focus();
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
        smtpSettingsSection.setExpanded(smtpSettingsSection == selectedSection);
        pop3SettingsSection.setExpanded(pop3SettingsSection == selectedSection);
        imapSettingsSection.setExpanded(imapSettingsSection == selectedSection);

        emailSettingsSmtpNav.setStyleName(
                emailSettingsSmtpNav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        emailSettingsPop3Nav.setStyleName(
                emailSettingsPop3Nav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        emailSettingsImapNav.setStyleName(
                emailSettingsImapNav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);

        selectedFirstField.focus();
    }

    private void updateAiNavigationStyles(Button selectedNavigationButton) {
        aiSettingsSourceNav.setStyleName(
                aiSettingsSourceNav == selectedNavigationButton
                        ? ACTIVE_AI_NAVIGATION_STYLE : AI_NAVIGATION_STYLE);
        aiSettingsConnectionsNav.setStyleName(
                aiSettingsConnectionsNav == selectedNavigationButton
                        ? ACTIVE_AI_NAVIGATION_STYLE : AI_NAVIGATION_STYLE);
    }
}
