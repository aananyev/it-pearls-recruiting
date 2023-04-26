package com.company.itpearls.web.screens.internalemailer;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailer;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@UiController("itpearls_InternalEmailer.edit")
@UiDescriptor("internal-emailer-edit.xml")
@EditedEntityContainer("emailerDc")
@LoadDataBeforeShow
public class InternalEmailerEdit<I extends InternalEmailer> extends StandardEditor<InternalEmailer> {
    @Inject
    private UserSession userSession;
    @Inject
    private CheckBox draftEmailField;
    @Inject
    private DateField<Date> dateCreateEmailField;
    @Inject
    private Button sendAndCloseButton;
    @Inject
    private Dialogs dialogs;
    @Inject
    private EmailService emailService;
    @Inject
    private DateField<Date> dateSendEmailField;
    @Inject
    private Logger log;
    @Inject
    private Notifications notifications;
    @Inject
    private TextField<String> subjectEmailField;
    @Inject
    private RichTextArea bodyEmailField;
    @Inject
    private TextField<String> fromEmailTextAddressField;
    @Inject
    private PickerField<ExtUser> fromEmailField;
    @Inject
    private SuggestionPickerField<JobCandidate> toEmailField;
    @Inject
    private MessageBundle messageBundle;

    private static final String EMAIL_FROMADDRESS = "cuba.email.fromAddress";
    private static final String EMAIL_SMTPHOST = "cuba.email.smtpHost";
    private static final String EMAIL_SMTPPORT = "cuba.email.smtpPort";
    private static final String EMAIL_SMTPUSER = "cuba.email.smtpUser";
    private static final String EMAIL_SMTPPASSWORD = "cuba.email.smtpPassword";

    private String cuba_email_fromAddress = "";
    private String cuba_email_smtpHost = "";
    private String cuba_email_smtpPort = "";
    private String cuba_email_smtpUser = "";
    private String cuba_email_smtpPassword = "";

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {

        if (PersistenceHelper.isNew(getEditedEntity())) {
            draftEmailField.setValue(true);
            dateCreateEmailField.setValue(new Date());
        }

        setSender((ExtUser) userSession.getUser());
    }

    private void setSender(ExtUser user) {
        String userEmail = System.getProperty(EMAIL_SMTPUSER);
        fromEmailTextAddressField.setValue(user.getName() + " \"" + user.getEmail() + "\"");
        fromEmailTextAddressField.setDescription("SMTP server: " + user.getSmtpServer() + ":" + user.getSmtpPort() + "\n" +
                "POP3 server: " + user.getPop3Server() + ":" + user.getPop3Port() + "\n" +
                "IMAP server% " + user.getImapServer() + ":" + user.getImapPort());
    }

    @Install(to = "toEmailField", subject = "optionIconProvider")
    private String toEmailFieldOptionIconProvider(JobCandidate jobCandidate) {
        if (jobCandidate.getEmail() != null) {
            return CubaIcon.PLUS_CIRCLE.source();
        } else {
            return CubaIcon.MINUS_CIRCLE.source();
        }
    }


    @Subscribe("toEmailField")
    public void onToEmailFieldValueChange(HasValue.ValueChangeEvent<JobCandidate> event) {
        if (event.getValue().getEmail() != null) {
            sendAndCloseButton.setEnabled(true);
            sendAndCloseButton.setDescription(messageBundle.getMessage("msgSendMessageToCandidate")
                    + event.getValue().getEmail());
        } else {
            sendAndCloseButton.setEnabled(false);
            sendAndCloseButton.setDescription(messageBundle.getMessage("msgNotPossibleSendEmail"));
        }
    }

    public void sendAndCloseButtonInvoke() {
        fromEmailField.setValue((ExtUser) userSession.getUser());
        sendEmail();
        close(StandardOutcome.CLOSE);
    }

    private void sendEmail() {
        dialogs.createOptionDialog()
                .withCaption("Send EMAIL")
                .withMessage("Отправить Email адресату "
                        + toEmailField.getValue().getFullName()
                        + " ("
                        + toEmailField.getValue().getEmail()
                        + ") ?")
                .withType(Dialogs.MessageType.CONFIRMATION)
                .withActions(new DialogAction(DialogAction.Type.YES).withHandler(e -> {
                            sendByEmailDefault();
                        }),
                        new DialogAction(DialogAction.Type.NO)
                ).show();
    }

    private void sendByEmailDefault() {
        InternalEmailer newsItem = getEditedEntity();

        EmailInfo emailInfo = EmailInfoBuilder.create()
                .setAddresses(toEmailField.getValue().getEmail())
                .setCaption(subjectEmailField.getValue())
                .setBody(bodyEmailField.getValue())
                .setFrom(null)
                .setTemplateParameters(Collections.singletonMap("newsItem", newsItem))
                .build();

        try {
            emailService.sendEmail(emailInfo);
            notifications.create()
                    .withType(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgInfo"))
                    .withDescription("Письмо адресату " + toEmailField.getValue().getEmail() + " отослано успешно.")
                    .show();
        } catch (EmailException e) {
            draftEmailField.setValue(true);
            notifications.create()
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription("Письмо не отправлено: " + e.getCause())
                    .show();

            commitChanges();
            log.error("ERROR:", e);
        } finally {
            commitChanges();
        }

        dateSendEmailField.setValue(new Date());
        draftEmailField.setValue(false);
        commitChanges();
    }

    private void replaceSystemSmtpProperties(ExtUser user) {
        String pass = user.getSmtpPassword();

        AppContext.setProperty(EMAIL_FROMADDRESS, user.getEmail());
        AppContext.setProperty(EMAIL_SMTPHOST, user.getSmtpServer());
        AppContext.setProperty(EMAIL_SMTPPORT, user.getSmtpPort().toString());
        AppContext.setProperty(EMAIL_SMTPUSER, user.getEmail());
        AppContext.setProperty(EMAIL_SMTPPASSWORD, user.getSmtpPassword());
    }

    private void restoreSystemSmtpProperties() {
        AppContext.setProperty(EMAIL_FROMADDRESS, cuba_email_fromAddress);
        AppContext.setProperty(EMAIL_SMTPHOST, cuba_email_smtpHost);
        AppContext.setProperty(EMAIL_SMTPPORT, cuba_email_smtpPort);
        AppContext.setProperty(EMAIL_SMTPUSER, cuba_email_smtpUser);
        AppContext.setProperty(EMAIL_SMTPPASSWORD, cuba_email_smtpPassword);
    }

    private void getSystemSmtpProperties() {
        cuba_email_fromAddress = AppContext.getProperty(EMAIL_FROMADDRESS);
        cuba_email_smtpHost = AppContext.getProperty(EMAIL_SMTPHOST);
        cuba_email_smtpPort = AppContext.getProperty(EMAIL_SMTPPORT);
        cuba_email_smtpUser = AppContext.getProperty(EMAIL_SMTPUSER);
        cuba_email_smtpPassword = AppContext.getProperty(EMAIL_SMTPPASSWORD);
    }

    @Install(to = "toEmailField", subject = "optionCaptionProvider")
    private String toEmailFieldOptionCaptionProvider(JobCandidate jobCandidate) {
        if (jobCandidate.getEmail() != null) {
            return jobCandidate.getEmail();
        } else {
            return null;
        }
    }
}