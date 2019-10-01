package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "ITPEARLS_MAIN_LIST")
@Entity(name = "itpearls_MainList")
public class MainList extends StandardEntity {
    private static final long serialVersionUID = 1827928532992138199L;

    @NotNull
    @Column(name = "NUMBER_LIST", nullable = false, unique = true)
    protected Integer numberList;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSON_CURRENT_POSITION_ID")
    protected Position personCurrentPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROJECT_NAME_ID")
    protected Project projectName;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    protected OpenPosition openPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ITERACTION_NAME_ID")
    protected Iteraction iteractionName;

    @NotNull
    @Column(name = "COMMUNICATION_METHOD", nullable = false, length = 20)
    protected String communicationMethod;

    @Lob
    @Column(name = "ITERACTION_COMMENT")
    protected String iteractionComment;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_FROM_COMPANY_ID")
    protected User userFromCompany;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATA_CONTACT")
    protected Date dataContact;

    public void setPersonCurrentPosition(Position personCurrentPosition) {
        this.personCurrentPosition = personCurrentPosition;
    }

    public void setNumberList(Integer numberList) {
        this.numberList = numberList;
    }

    public Position getPersonCurrentPosition() {
        return personCurrentPosition;
    }

    public String getCommunicationMethod() {
        return communicationMethod;
    }

    public void setCommunicationMethod(String communicationMethod) {
        this.communicationMethod = communicationMethod;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public Project getProjectName() {
        return projectName;
    }

    public void setProjectName(Project projectName) {
        this.projectName = projectName;
    }

    public String getIteractionComment() {
        return iteractionComment;
    }

    public void setIteractionComment(String iteractionComment) {
        this.iteractionComment = iteractionComment;
    }

    public Iteraction getIteractionName() {
        return iteractionName;
    }

    public void setIteractionName(Iteraction iteractionName) {
        this.iteractionName = iteractionName;
    }

    public Date getDataContact() {
        return dataContact;
    }

    public void setDataContact(Date dataContact) {
        this.dataContact = dataContact;
    }

    public User getUserFromCompany() {
        return userFromCompany;
    }

    public void setUserFromCompany(User userFromCompany) {
        this.userFromCompany = userFromCompany;
    }

    public Integer getNumberList() {
        return numberList;
    }
}