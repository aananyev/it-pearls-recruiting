package com.company.itpearls.web.screens.extsettingswindow;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.UserSettings;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.config.MenuItem;
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
    private TextField<String> smtpPort;
    @Inject
    private CheckBox smtpPasswordRequired;
    @Inject
    private TextField<String> smtpPassword;
    private ExtUser currentUser;
    @Inject
    private TextField<String> imapServer;
    @Inject
    private TextField<String> imapPort;
    @Inject
    private CheckBox imapPasswordRequired;
    @Inject
    private TextField<String> imapPassword;
    @Inject
    private TextField<String> pop3Server;
    @Inject
    private TextField<String> pop3Port;
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

    UserSettings userSettings;
    final String QUERY_GET_USER_SETTINGS = "select e from UserSettings e where e.user = :currentUser";

    @Override
    public void init(Map<String, Object> params) {
        currentUser = (ExtUser) userSessionSource.getUserSession().getUser();
        userSettings = getUserSettingsEntity(currentUser);
        setEmailSettings();
        super.init(params);
    }

    private UserSettings getUserSettingsEntity(ExtUser currentUser) {


        return dataManager.load(UserSettings.class)
                .query(QUERY_GET_USER_SETTINGS)
                .parameter("currentUser", currentUser)
                .view("userSettings-view")
                .one();
    }


    private void setEmailSettings() {
        if (smtpServer.getValue() == null )
            smtpServer.setValue(currentUser.getSmtpServer());

        if (smtpPort.getValue() == null)
            smtpPort.setValue(currentUser.getSmtpPort() != null ?
                    currentUser.getSmtpPort().toString() : null);

        if (smtpPasswordRequired.getValue() == null)
            smtpPasswordRequired.setValue(currentUser.getSmtpPasswordRequired());

        if (smtpPassword.getValue() == null)
            smtpPassword.setValue(currentUser.getSmtpPassword());

        if (imapServer.getValue() == null )
            imapServer.setValue(currentUser.getImapServer());

        if (imapPort.getValue() == null)
            imapPort.setValue(currentUser.getImapPort() != null ?
                    currentUser.getImapPort().toString() : null);

        if (imapPasswordRequired.getValue() == null)
            imapPasswordRequired.setValue(currentUser.getImapPasswordRequired());

        if (imapPassword.getValue() == null)
            imapPassword.setValue(currentUser.getImapPassword());

        if (pop3Server.getValue() == null )
            pop3Server.setValue(currentUser.getPop3Server());

        if (pop3Port.getValue() == null)
            pop3Port.setValue(currentUser.getImapPort() != null ?
                    currentUser.getPop3Port().toString() : null);

        if (pop3PasswordRequired.getValue() == null)
            pop3PasswordRequired.setValue(currentUser.getPop3PasswordRequired());

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

    @Override
    protected void commit() {

        UserSettings userSettingsNew = metadata.create(UserSettings.class);

        userSettingsNew.setUser((ExtUser) userSession.getUser());
        userSettingsNew.setSmtpServer(smtpServer.getValue());
        userSettingsNew.setSmtpPort(Integer.parseInt(smtpPort.getValue()));
        userSettingsNew.setSmtpPasswordRequired(smtpPasswordRequired.getValue());
        userSettingsNew.setSmtpPassword(smtpPassword.getValue());

        userSettingsNew.setPop3Server(pop3Server.getValue());
        userSettingsNew.setPop3Port(Integer.parseInt(pop3Port.getValue()));
        userSettingsNew.setPop3PasswordRequired(pop3PasswordRequired.getValue());
        userSettingsNew.setPop3Password(pop3Password.getValue());

        userSettingsNew.setImapServer(imapServer.getValue());
        userSettingsNew.setImapPort(Integer.parseInt(imapPort.getValue()));
        userSettingsNew.setImapPasswordRequired(imapPasswordRequired.getValue());
        userSettingsNew.setImapPassword(imapPassword.getValue());

        CommitContext commitContext = new CommitContext(userSettingsNew, userSettings);
        dataManager.commit(commitContext);

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