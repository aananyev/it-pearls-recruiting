package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@NamePattern("%s %s|number,iterationName")
@Table(name = "ITPEARLS_ITERACTION", indexes = {
        @Index(name = "IDX_ITPEARLS_ITERACTION", columnList = "ITERATION_NAME")
})
@Entity(name = "itpearls_Iteraction")
public class Iteraction extends StandardEntity {
    private static final long serialVersionUID = -3287484760093673466L;

    @Column(name = "NUMBER_")
    protected String number;

    @Column(name = "MANDATORY_ITERACTION")
    protected Boolean mandatoryIteraction;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITERACTION_TREE_ID")
    protected Iteraction iteractionTree;

    @NotNull
    @Column(name = "ITERATION_NAME", nullable = false, unique = true, length = 80)
    protected String iterationName;

    @Column(name = "CALL_BUTTON_TEXT", length = 30)
    protected String callButtonText;

    @Column(name = "CALL_CLASS", length = 30)
    protected String callClass;

    @Column(name = "CALL_FORM")
    protected Boolean callForm;

    public Boolean getCallForm() {
        return callForm;
    }

    public void setCallForm(Boolean callForm) {
        this.callForm = callForm;
    }

    public String getCallClass() {
        return callClass;
    }

    public void setCallClass(String callClass) {
        this.callClass = callClass;
    }

    public String getCallButtonText() {
        return callButtonText;
    }

    public void setCallButtonText(String callButtonText) {
        this.callButtonText = callButtonText;
    }

    public Boolean getMandatoryIteraction() {
        return mandatoryIteraction;
    }

    public void setMandatoryIteraction(Boolean mandatoryIteraction) {
        this.mandatoryIteraction = mandatoryIteraction;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    public Iteraction getIteractionTree() {
        return iteractionTree;
    }

    public void setIteractionTree(Iteraction iteractionTree) {
        this.iteractionTree = iteractionTree;
    }

    public String getIterationName() {
        return iterationName;
    }

    public void setIterationName(String iterationName) {
        this.iterationName = iterationName;
    }
}