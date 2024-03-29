package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.annotation.Extends;
import com.haulmont.cuba.core.entity.annotation.PublishEntityChangedEvents;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;

@PublishEntityChangedEvents
@Entity(name = "itpearls_ExtUser")
@Extends(User.class)
public class ExtUser extends User {
    private static final long serialVersionUID = 6173000981123148225L;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IMAGE_ID")
    private FileDescriptor fileImageFace;

    @Column(name = "SMTP_SERVER", length = 128)
    private String smtpServer;

    @Column(name = "SMTP_PORT")
    private Integer smtpPort;

    @Column(name = "SMTP_PASSWORD_REQUIRED")
    private Boolean smtpPasswordRequired;

    @Column(name = "SMTP_USER", length = 64)
    private String smtpUser;

    @Column(name = "SMTP_PASSWORD", length = 128)
    private String smtpPassword;

    @Column(name = "POP3_SERVER", length = 128)
    private String pop3Server;

    @Column(name = "POP3_PORT")
    private Integer pop3Port;

    @Column(name = "POP3_PASSWORD_REQUIRED")
    private Boolean pop3PasswordRequired;

    @Column(name = "POP3_USER", length = 64)
    private String pop3User;

    @Column(name = "POP3PASSWORD", length = 128)
    private String pop3Password;

    @Column(name = "IMAP_SERVER", length = 128)
    private String imapServer;

    @Column(name = "IMAP_PORT")
    private Integer imapPort;

    @Column(name = "IMAP_PASSWORD_REQUIRED")
    private Boolean imapPasswordRequired;

    @Column(name = "IMAP_USER", length = 64)
    private String imapUser;

    @Column(name = "IMAP_PASSWORD", length = 128)
    private String imapPassword;

    @Column(name = "STATISTICS_")
    private Boolean statistics;

    @Column(name = "DASHBOARDS")
    private Boolean dashboards;

    public Boolean getDashboards() {
        return dashboards;
    }

    public void setDashboards(Boolean dashboards) {
        this.dashboards = dashboards;
    }

    public Boolean getStatistics() {
        return statistics;
    }

    public void setStatistics(Boolean statistics) {
        this.statistics = statistics;
    }

    public String getImapUser() {
        return imapUser;
    }

    public void setImapUser(String imapUser) {
        this.imapUser = imapUser;
    }

    public String getPop3User() {
        return pop3User;
    }

    public void setPop3User(String pop3User) {
        this.pop3User = pop3User;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public FileDescriptor getFileImageFace() {
        return fileImageFace;
    }

    public void setFileImageFace(FileDescriptor fileImageFace) {
        this.fileImageFace = fileImageFace;
    }

    public Boolean getImapPasswordRequired() {
        return imapPasswordRequired;
    }

    public void setImapPasswordRequired(Boolean imapPasswordRequired) {
        this.imapPasswordRequired = imapPasswordRequired;
    }

    public Boolean getPop3PasswordRequired() {
        return pop3PasswordRequired;
    }

    public void setPop3PasswordRequired(Boolean pop3PasswordRequired) {
        this.pop3PasswordRequired = pop3PasswordRequired;
    }

    public Boolean getSmtpPasswordRequired() {
        return smtpPasswordRequired;
    }

    public void setSmtpPasswordRequired(Boolean smtpPasswordRequired) {
        this.smtpPasswordRequired = smtpPasswordRequired;
    }

    public String getImapPassword() {
        return imapPassword;
    }

    public void setImapPassword(String imapPassword) {
        this.imapPassword = imapPassword;
    }

    public String getPop3Password() {
        return pop3Password;
    }

    public void setPop3Password(String pop3Password) {
        this.pop3Password = pop3Password;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public void setPop3Port(Integer pop3Port) {
        this.pop3Port = pop3Port;
    }

    public Integer getPop3Port() {
        return pop3Port;
    }

    public Integer getImapPort() {
        return imapPort;
    }

    public void setImapPort(Integer imapPort) {
        this.imapPort = imapPort;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getImapServer() {
        return imapServer;
    }

    public void setImapServer(String imapServer) {
        this.imapServer = imapServer;
    }

    public String getPop3Server() {
        return pop3Server;
    }

    public void setPop3Server(String pop3Server) {
        this.pop3Server = pop3Server;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }
}