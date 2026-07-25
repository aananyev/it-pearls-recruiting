package com.company.hunttech.web.screens.extsettingswindow;

import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.GroupBoxLayout;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.VBoxLayout;

import javax.inject.Inject;
import java.util.Map;

/**
 * Дополняет вкладку email связью между навигацией слева и аккордеоном справа.
 * Загрузка, редактирование и сохранение почтовых настроек остаются в базовом контроллере.
 */
public class ExtSettingsWindowEmailNavigation extends ExtSettingsWindow {

    private static final String NAVIGATION_STYLE = "borderless settings-section-nav-item";
    private static final String ACTIVE_NAVIGATION_STYLE =
            "borderless settings-section-nav-item settings-section-nav-item-active";

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

    private Button emailSettingsSmtpNav;
    private Button emailSettingsPop3Nav;
    private Button emailSettingsImapNav;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        initEmailSettingsNavigation();
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
                "emailSettingsSmtpNav", "emailSettingsSmtpSection", this::selectSmtpSettings);
        emailSettingsPop3Nav = createNavigationButton(
                "emailSettingsPop3Nav", "emailSettingsPop3Section", this::selectPop3Settings);
        emailSettingsImapNav = createNavigationButton(
                "emailSettingsImapNav", "emailSettingsImapSection", this::selectImapSettings);

        emailSettingsNavigation.add(emailSettingsSmtpNav);
        emailSettingsNavigation.add(emailSettingsPop3Nav);
        emailSettingsNavigation.add(emailSettingsImapNav);
        selectEmailSettingsSection(smtpSettingsSection, smtpServer, emailSettingsSmtpNav);
    }

    private Button createNavigationButton(String id, String messageKey, Runnable handler) {
        Button button = uiComponents.create(Button.class);
        button.setId(id);
        button.setCaption(getMessage(messageKey));
        button.setWidth("100%");
        button.setStyleName(NAVIGATION_STYLE);
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
}
