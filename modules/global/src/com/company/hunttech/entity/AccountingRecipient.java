package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "HUNTTECH_ACCOUNTING_RECIPIENT", indexes = {
        @Index(name = "IDX_HUNTTECH_ACC_RECIPIENT_EMAIL", columnList = "EMAIL"),
        @Index(name = "IDX_HUNTTECH_ACC_RECIPIENT_FLOW", columnList = "FLOW_TYPE")
})
@Entity(name = "hunttech_AccountingRecipient")
@NamePattern("%s <%s>|name,email")
public class AccountingRecipient extends StandardEntity {
    private static final long serialVersionUID = 1188043852155198561L;

    @NotNull
    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @NotNull
    @Column(name = "EMAIL", nullable = false, length = 255)
    private String email;

    @Column(name = "ACTIVE")
    private Boolean active = true;

    @NotNull
    @Column(name = "RECIPIENT_ROLE", nullable = false)
    private Integer recipientRole;

    @NotNull
    @Column(name = "FLOW_TYPE", nullable = false)
    private Integer flowType;

    @Column(name = "SEND_AS_TO")
    private Boolean sendAsTo = true;

    @Column(name = "SEND_AS_CC")
    private Boolean sendAsCc = false;

    @Column(name = "SEND_AS_BCC")
    private Boolean sendAsBcc = false;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public AccountingRecipientRole getRecipientRole() {
        return AccountingRecipientRole.fromId(recipientRole);
    }

    public void setRecipientRole(AccountingRecipientRole recipientRole) {
        this.recipientRole = recipientRole == null ? null : recipientRole.getId();
    }

    public AccountingFlowType getFlowType() {
        return AccountingFlowType.fromId(flowType);
    }

    public void setFlowType(AccountingFlowType flowType) {
        this.flowType = flowType == null ? null : flowType.getId();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getSendAsTo() {
        return sendAsTo;
    }

    public void setSendAsTo(Boolean sendAsTo) {
        this.sendAsTo = sendAsTo;
    }

    public Boolean getSendAsCc() {
        return sendAsCc;
    }

    public void setSendAsCc(Boolean sendAsCc) {
        this.sendAsCc = sendAsCc;
    }

    public Boolean getSendAsBcc() {
        return sendAsBcc;
    }

    public void setSendAsBcc(Boolean sendAsBcc) {
        this.sendAsBcc = sendAsBcc;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
