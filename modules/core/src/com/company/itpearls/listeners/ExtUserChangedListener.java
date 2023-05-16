package com.company.itpearls.listeners;

import com.company.itpearls.entity.ExtUser;

import java.util.List;
import java.util.UUID;

import com.company.itpearls.entity.UserSettings;
import com.haulmont.cuba.core.TransactionalDataManager;
import com.haulmont.cuba.core.app.events.EntityChangedEvent;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.inject.Inject;

@Component("itpearls_ExtUserChangedListener")
public class ExtUserChangedListener {

    @Inject
    private TransactionalDataManager txDm;
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void onExtUserAfterCompletion(EntityChangedEvent<ExtUser, UUID> event) {
        setUserSettingsRecord(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onExtUserBeforeCommit(EntityChangedEvent<ExtUser, UUID> event) {
        setUserSettingsRecord(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExtUserAfterCommit(EntityChangedEvent<ExtUser, UUID> event) {
        setUserSettingsRecord(event);
    }

    @EventListener
    public void beforeCommit(EntityChangedEvent<ExtUser, UUID> event) {
        setUserSettingsRecord(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(EntityChangedEvent<ExtUser, UUID> event) {
        setUserSettingsRecord(event);
    }

    protected void setUserSettingsRecord(EntityChangedEvent<ExtUser, UUID> event) {
        ExtUser user;
        Boolean flag = false;

        if (event.getType() != EntityChangedEvent.Type.DELETED) {

            user = txDm.load(event.getEntityId())
                    .view("extUser-view")
                    .one();

            List<UserSettings> userSettings = dataManager.load(UserSettings.class)
                    .list();

            for (UserSettings usrSett : userSettings) {
                if (usrSett.getUser().equals(user)) {
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                UserSettings usrSet = metadata.create(UserSettings.class);
                usrSet.setUser(user);

                usrSet.setSmtpServer(user.getSmtpServer());
                usrSet.setSmtpPort(user.getSmtpPort());
                usrSet.setSmtpPasswordRequired(user.getSmtpPasswordRequired());
                usrSet.setSmtpPassword(user.getSmtpPassword());

                usrSet.setPop3Server(user.getPop3Server());
                usrSet.setPop3Port(user.getPop3Port());
                usrSet.setPop3PasswordRequired(user.getPop3PasswordRequired());
                usrSet.setPop3Password(user.getPop3Password());

                usrSet.setImapServer(user.getImapServer());
                usrSet.setImapPort(user.getImapPort());
                usrSet.setImapPasswordRequired(user.getImapPasswordRequired());
                usrSet.setImapPassword(user.getImapPassword());

                txDm.save(usrSet);
            }
        }
    }
}