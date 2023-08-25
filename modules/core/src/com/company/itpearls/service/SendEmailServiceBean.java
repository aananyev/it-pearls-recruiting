package com.company.itpearls.service;

import org.springframework.stereotype.Service;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;


@Service(SendEmailService.NAME)
public class SendEmailServiceBean implements SendEmailService {

    private String smtpServer;
    private String pop3Server;
    private String userName;
    private String password;
    private String smtpPort;

    public void SendEmail(String smtpServer, String pop3Server, String userName, String password, String smtpPort) {
        this.smtpServer = smtpServer;
        this.pop3Server = pop3Server;
        this.userName = userName;
        this.password = password;
        this.smtpPort = smtpPort;
    }

/*    public void send(EmailServerParameters emailServerParameters, String to, String subject, String text) {

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", smtpPort);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(userName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(text);

            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    } */
}