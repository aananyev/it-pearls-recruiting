package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.annotation.Extends;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "itpearls_ExtUser")
@Extends(User.class)
public class ExtUser extends User {
    private static final long serialVersionUID = 6173000981123148225L;

    @Column(name = "SMTP_SERVER", length = 128)
    private String smtpServer;

    @Column(name = "SMTP_PORT")
    private Integer smtpPort;

    @Column(name = "POP3_SERVER", length = 128)
    private String pop3Server;

    @Column(name = "POP3_PORT")
    private Integer pop3Port;

    @Column(name = "IMAP_SERVER", length = 128)
    private String imapServer;

    @Column(name = "IMAP_PORT")
    private Integer imapPort;

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