package com.company.itpearls.web.screens.extsettingswindow;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.UserSettings;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.config.MenuItem;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.web.app.ui.core.settings.SettingsWindow;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExtSettingsWindow extends SettingsWindow {
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private TextField<String> smtpServer;
    @Inject
    private CheckBox smtpPasswordRequired;
    @Inject
    private TextField<String> smtpPassword;
    @Inject
    private TextField<String> imapServer;
    @Inject
    private CheckBox imapPasswordRequired;
    @Inject
    private TextField<String> imapPassword;
    @Inject
    private TextField<String> pop3Server;
    @Inject
    private CheckBox pop3PasswordRequired;
    @Inject
    private TextField<String> pop3Password;
    @Inject
    private Image userPic;
    @Inject
    private Metadata metadata;
    @Inject
    private DataManager dataManager;

    private ExtUser currentUser;
    private UserSettings userSettings;
    private final String QUERY_GET_USER_SETTINGS
            = "select e from itpearls_UserSettings e where e.user = :currentUser";
    @Inject
    private DataContext dataContext;
    @Inject
    private Image defaultPic;
    @Inject
    private TextField<Integer> smtpPort;
    @Inject
    private TextField<Integer> imapPort;
    @Inject
    private TextField<Integer> pop3Port;

    @Override
    public void init(Map<String, Object> params) {
        currentUser = (ExtUser) userSessionSource.getUserSession().getUser();
        getUserSettingsEntity(currentUser);

        setEmailSettings();

        if (userPic.getSource() == null) {
            userPic.setVisible(false);
            defaultPic.setVisible(true);
        } else {
            userPic.setVisible(true);
            defaultPic.setVisible(false);
        }

        super.init(params);
    }

    private void getUserSettingsEntity(ExtUser currentUser) {
        try {
            userSettings = dataManager.load(UserSettings.class)
                    .query(QUERY_GET_USER_SETTINGS)
                    .parameter("currentUser", currentUser)
                    .view("userSettings-view")
                    .one();
        } catch (IllegalStateException e) {
            userSettings = createNewUserSetting();
            setEmailSettings();

        }
    }

    private void setEmailSettings() {
        userSettings.setUser((ExtUser) userSession.getUser());

        if (userSettings.getSmtpServer() != null) {
            smtpServer.setValue(userSettings.getSmtpServer());
        }

        if (smtpServer.getValue() == null)
            smtpServer.setValue(currentUser.getSmtpServer());

        if (userSettings.getSmtpPort() != null) {
            smtpPort.setValue(userSettings.getSmtpPort());
        }

        if (smtpPort.getValue() == null) {
            smtpPort.setValue(currentUser.getSmtpPort() != null ?
                    currentUser.getSmtpPort() : 0);
        }

        if (userSettings.getSmtpPasswordRequired() != null) {
            smtpPasswordRequired.setValue(userSettings.getSmtpPasswordRequired());
        }

        if (smtpPasswordRequired.getValue() == null)
            smtpPasswordRequired.setValue(currentUser.getSmtpPasswordRequired());

        if (smtpPassword.getValue() == null)
            smtpPassword.setValue(userSettings.getSmtpPassword());


        if (userSettings.getImapServer() != null) {
            imapServer.setValue(userSettings.getImapServer());
        }

        if (imapServer.getValue() == null )
            imapServer.setValue(currentUser.getImapServer());

        if (userSettings.getImapPort() != null) {
            imapPort.setValue(userSettings.getImapPort());
        }

        if (imapPort.getValue() == null) {
            imapPort.setValue(currentUser.getImapPort());
        }

        if (imapPort.getValue() == null) {
            imapPort.setValue(currentUser.getImapPort() != null ?
                    currentUser.getImapPort() : 0);
        }

        if (userSettings.getImapPasswordRequired() != null) {
            imapPasswordRequired.setValue(userSettings.getImapPasswordRequired());
        }

        if (imapPasswordRequired.getValue() == null)
            imapPasswordRequired.setValue(currentUser.getImapPasswordRequired());


        if (imapPassword.getValue() == null)
            imapPassword.setValue(userSettings.getImapPassword());

        if (imapPassword.getValue() == null)
            imapPassword.setValue(currentUser.getImapPassword());


        if (userSettings.getPop3Server() != null) {
            pop3Server.setValue(userSettings.getPop3Server());
        }

        if (pop3Server.getValue() == null)
            pop3Server.setValue(currentUser.getPop3Server());

        if (userSettings.getPop3Port() != null) {
            pop3Port.setValue(userSettings.getPop3Port());
        }

        if (pop3Port.getValue() == null) {
            pop3Port.setValue(currentUser.getPop3Port());
        }

        if (pop3Port.getValue() == null) {
            pop3Port.setValue(currentUser.getPop3Port() != null ?
                    currentUser.getPop3Port() : 0);
        }

        if (userSettings.getPop3PasswordRequired() != null) {
            pop3PasswordRequired.setValue(userSettings.getPop3PasswordRequired());
        }

        if (pop3PasswordRequired.getValue() == null)
            pop3PasswordRequired.setValue(currentUser.getPop3PasswordRequired());

        if (pop3Password.getValue() == null)
            pop3Password.setValue(userSettings.getPop3Password());

        if (pop3Password.getValue() == null)
            pop3Password.setValue(currentUser.getPop3Password());

        if (userPic.getValueSource() == null) {
            if (currentUser.getFileImageFace() != null) {
                userPic.setSource(FileDescriptorResource.class)
                        .setFileDescriptor(currentUser.getFileImageFace());
            }
        }
    }

    @Override
    protected void initDefaultScreenField() {
        super.initDefaultScreenField();
    }

    @Override
    protected Map<String, String> collectScreens() {
        return super.collectScreens();
    }

    @Override
    protected List<MenuItem> collectPermittedScreens(List<MenuItem> menuItems) {
        return super.collectPermittedScreens(menuItems);
    }

    private UserSettings createNewUserSetting() {
        UserSettings userSettingsNew = metadata.create(UserSettings.class);

        userSettingsNew.setUser((ExtUser) userSession.getUser());
        userSettingsNew.setSmtpServer(smtpServer.getValue());
        userSettingsNew.setSmtpPort(smtpPort.getValue() != null ?
                smtpPort.getValue() : 0);
        userSettingsNew.setSmtpPasswordRequired(smtpPasswordRequired.getValue());
        userSettingsNew.setSmtpPassword(smtpPassword.getValue());

        userSettingsNew.setPop3Server(pop3Server.getValue());
        userSettingsNew.setPop3Port(pop3Port.getValue() != null ?
                pop3Port.getValue() : 0);
        userSettingsNew.setPop3PasswordRequired(pop3PasswordRequired.getValue());
        userSettingsNew.setPop3Password(pop3Password.getValue());

        userSettingsNew.setImapServer(imapServer.getValue());
        userSettingsNew.setImapPort(imapPort.getValue() != null ?
                imapPort.getValue() : 0);
        userSettingsNew.setImapPasswordRequired(imapPasswordRequired.getValue());
        userSettingsNew.setImapPassword(imapPassword.getValue());

        return userSettingsNew;
    }

    @Override
    protected void commit() {

        userSettings.setSmtpServer(smtpServer.getValue());
        userSettings.setSmtpPassword(smtpPassword.getValue());
        userSettings.setSmtpPasswordRequired(smtpPasswordRequired.getValue());

        userSettings.setSmtpPort(smtpPort.getValue() != null ?
                smtpPort.getValue() : 0);

        userSettings.setImapServer(imapServer.getValue());
        userSettings.setImapPassword(imapPassword.getValue());
        userSettings.setImapPasswordRequired(imapPasswordRequired.getValue());

        userSettings.setImapPort(imapPort.getValue() != null ?
                imapPort.getValue() : 0);

        userSettings.setPop3Server(pop3Server.getValue());
        userSettings.setPop3Password(pop3Password.getValue());
        userSettings.setPop3PasswordRequired(pop3PasswordRequired.getValue());

        userSettings.setPop3Port(pop3Port.getValue() != null ?
                pop3Port.getValue() : 0);

        dataManager.commit(userSettings);
        super.commit();
    }

    @Override
    protected void cancel() {
        super.cancel();
    }

    @Override
    protected void initTimeZoneFields() {
        super.initTimeZoneFields();
    }

    @Override
    protected void saveTimeZoneSettings() {
        super.saveTimeZoneSettings();
    }

    @Override
    protected void saveLocaleSettings() {
        super.saveLocaleSettings();
    }

    @Override
    protected void resetScreenSettings() {
        super.resetScreenSettings();
    }

    @Override
    protected Set<String> getAllWindowIds() {
        return super.getAllWindowIds();
    }
}