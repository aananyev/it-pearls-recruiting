package com.company.itpearls.web.screens.internalemailer;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.*;

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

    protected JobCandidate jobCandidate = null;
    @Inject
    private Metadata metadata;
    @Inject
    private DataManager dataManager;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {

        if (PersistenceHelper.isNew(getEditedEntity())) {
            draftEmailField.setValue(true);
            dateCreateEmailField.setValue(new Date());
        }

        setSender((ExtUser) userSession.getUser());

        if (jobCandidate != null) {
            toEmailField.setValue(jobCandidate);
        }
    }

    private void setSender(ExtUser user) {
        String userEmail = System.getProperty(EMAIL_SMTPUSER);
        fromEmailTextAddressField.setValue(user.getName() + " \"" + user.getEmail() + "\"");
        fromEmailTextAddressField.setDescription("SMTP server: " + user.getSmtpServer() + ":" + user.getSmtpPort() + "\n" +
                "POP3 server: " + user.getPop3Server() + ":" + user.getPop3Port() + "\n" +
                "IMAP server: " + user.getImapServer() + ":" + user.getImapPort());
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
        close(StandardOutcome.COMMIT);
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
                .withActions(new DialogAction(DialogAction.Type.YES).withHandler(e ->
                        {
                            try {
                                sendByEmailDefaultMailx();
                            } catch (MessagingException messagingException) {
                                log.error("Error", messagingException);
                            }
                        }
                        ),
                        new DialogAction(DialogAction.Type.NO)
                ).show();
    }

    private void sendByEmailDefaultMailx() throws MessagingException {
        final String SMTP_SERVER = ((ExtUser) userSession.getUser()).getSmtpServer();
        final String SMTP_PORT = ((ExtUser) userSession.getUser()).getSmtpPort().toString();
        final String SMTP_USERNAME = ((ExtUser) userSession.getUser()).getSmtpUser();
        final String SMTP_PASSWORD = ((ExtUser) userSession.getUser()).getSmtpPassword();

        String to = toEmailField.getValue().getEmail();
        String subject = subjectEmailField.getValue();
        String body = bodyEmailField.getValue();

        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", SMTP_SERVER);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.sendpartial", "true");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_SERVER);
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(getMailHTMLHeader()
                    + body
                    + getMailHTMLFooter(), "text/html; charset=utf-8");

            message.setSentDate(new Date());

            Transport.send(message);

            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withType(Notifications.NotificationType.HUMANIZED)
                    .withHideDelayMs(5000)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withCaption(messageBundle.getMessage("msgInfo"))
                    .withDescription(messageBundle.getMessage("msgEmailSended"))
                    .show();

            getEditedEntity().setDateSendEmail(new Date());
            getEditedEntity().setDraftEmail(false);

            draftEmailField.setValue(false);
            dateSendEmailField.setValue(new Date());

            dataManager.commit(getEditedEntity());

            IteractionList emailIteraction = createIteraction();
            if (emailIteraction != null) {
                dataManager.commit(emailIteraction);
            } else {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withType(Notifications.NotificationType.WARNING)
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withDescription(messageBundle.getMessage("msgFailedCreateInteraction"))
                        .withHideDelayMs(15000)
                        .show();
            }
        } catch (MessagingException e) {
            log.error("Error", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgErrorSendEmail") + " " + e.getMessage())
                    .withHideDelayMs(15000)
                    .show();
        }
    }

    protected IteractionList createIteraction() {
        IteractionList iteractionList = metadata.create(IteractionList.class);

        iteractionList.setRecrutier((ExtUser) userSession.getUser());
        iteractionList.setRecrutierName(userSession.getUser().getName());
        iteractionList.setRating(5);
        iteractionList.setDateIteraction(new Date());
        iteractionList.setCandidate(toEmailField.getValue());
        iteractionList.setComment(subjectEmailField.getValue() != null ? subjectEmailField.getValue() : "");

        OpenPosition openPosition = getDefaultOpenPosition();

        if (openPosition != null) {
            iteractionList.setVacancy(openPosition);
        } else {
            return null;
        }

        BigDecimal maxNumberInteraction = getMaxNumberInteraction();
        if (maxNumberInteraction != null) {
            iteractionList.setNumberIteraction(maxNumberInteraction.add(BigDecimal.ONE));
        } else {
            return null;
        }

        Iteraction iteraction = getInteractionSignEmailSend();

        if (iteraction != null) {
            iteractionList.setIteractionType(getInteractionSignEmailSend());
        } else {
            return null;
        }

        return iteractionList;
    }

    private OpenPosition getDefaultOpenPosition() {
        String QUERY_GET_DEFAULT_OPEN_POSITION = "select e from itpearls_OpenPosition e where e.vacansyName like 'Default'";
        OpenPosition openPosition = null;

        try {
            openPosition = dataManager.load(OpenPosition.class)
                    .query(QUERY_GET_DEFAULT_OPEN_POSITION)
                    .one();
        } catch (NullPointerException e) {
            log.error("Error", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgErrorNotDefaultOpenPosition"))
                    .withHideDelayMs(15000)
                    .show();
        } finally {
            return openPosition;
        }
    }

    private Iteraction getInteractionSignEmailSend() {
        String QUERY_GET_INTERACTION_SIGN_EMAIL_SEND = "select e from itpearls_Iteraction e where e.signEmailSend = true";
        Iteraction retInteraction = null;

        try {
            retInteraction = dataManager.load(Iteraction.class)
                    .query(QUERY_GET_INTERACTION_SIGN_EMAIL_SEND)
                    .one();
        } catch (NullPointerException e) {
            log.error("Error", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgErrorNotFindSignEmailInteraction"))
                    .withHideDelayMs(15000)
                    .show();
        } finally {
            return retInteraction;
        }
    }

    private BigDecimal getMaxNumberInteraction() {
        String QUERY_GEM_MAX_NUMBER_INTERACTION = "select max(e.numberIteraction) from itpearls_IteractionList e";

        try {
            return dataManager.loadValue(QUERY_GEM_MAX_NUMBER_INTERACTION, BigDecimal.class)
                    .one();
        } catch (NullPointerException e) {
            log.error("Error", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgErrorGettingMaxNumberInteraction"))
                    .withHideDelayMs(15000)
                    .show();
            return null;
        }
    }


    private String getMailHTMLFooter() {
        return "</body>\n" +
                "</html>";
    }

    private String getMailHTMLHeader() {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" >\n" +
                "<title>...</title>\n" +
                "<style type=\"text/css\">\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>";
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
                    .withDescription(messageBundle.getMessage("msgLetterToRecipient")
                            + " "
                            + toEmailField.getValue().getEmail()
                            + " "
                            + messageBundle.getMessage("msgSendSucessfully"))
                    .withHideDelayMs(5000)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();
        } catch (EmailException e) {
            draftEmailField.setValue(true);
            notifications.create()
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgMailNotSend")
                            + e.getCause())
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

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public void setToEmailField(JobCandidate jobCandidate) {
        toEmailField.setValue(jobCandidate);
    }

    public void setReplyInternalEmailer(InternalEmailer internalEmailer) {
        getEditedEntity().setReplyInternalEmailer(internalEmailer);
    }

    public InternalEmailer getInternalEmailer() {
        return getEditedEntity();
    }
}