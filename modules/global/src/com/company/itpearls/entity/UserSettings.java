package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_USER_SETTINGS")
@Entity(name = "itpearls_UserSettings")
@NamePattern("%s|user")
public class UserSettings extends StandardEntity {
    private static final long serialVersionUID = -294638674422970184L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDeleteInverse(DeletePolicy.DENY)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", unique = true)
    @NotNull
    private ExtUser user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IMAGE_ID")
    private FileDescriptor fileImageFace;

    @Column(name = "SMTP_SERVER", length = 128)
    private String smtpServer;

    @Column(name = "SMTP_PORT")
    private Integer smtpPort;

    @Column(name = "SMTP_PASSWORD_REQUIRED")
    private Boolean smtpPasswordRequired;

    @Column(name = "SMTP_PASSWORD", length = 128)
    private String smtpPassword;

    @Column(name = "POP3_SERVER", length = 128)
    private String pop3Server;

    @Column(name = "POP3_PORT")
    private Integer pop3Port;

    @Column(name = "POP3_PASSWORD_REQUIRED")
    private Boolean pop3PasswordRequired;

    @Column(name = "POP3PASSWORD", length = 128)
    private String pop3Password;

    @Column(name = "IMAP_SERVER", length = 128)
    private String imapServer;

    @Column(name = "IMAP_PORT")
    private Integer imapPort;

    @Column(name = "IMAP_PASSWORD_REQUIRED")
    private Boolean imapPasswordRequired;

    @Column(name = "IMAP_PASSWORD", length = 128)
    private String imapPassword;

    public void setUser(ExtUser user) {
        this.user = user;
    }

    public ExtUser getUser() {
        return user;
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