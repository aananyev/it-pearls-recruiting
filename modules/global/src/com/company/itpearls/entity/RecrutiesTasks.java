package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@NamePattern("%s %s|reacrutier,openPosition")
@Table(name = "ITPEARLS_RECRUTIES_TASKS")
@Entity(name = "itpearls_RecrutiesTasks")
public class RecrutiesTasks extends StandardEntity {
    private static final long serialVersionUID = -6819071878196461497L;

    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "START_DATE", nullable = false)
    protected Date startDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "END_DATE")
    protected Date endDate;

    @Column(name = "CLOSED")
    @NotNull
    private Boolean closed;

    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "REACRUTIER_ID")
    protected User reacrutier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    protected OpenPosition openPosition;

    @Column(name = "SUBSCRIBE")
    protected Boolean subscribe;

    @Column(name = "RECRUTIER_NAME", length = 80)
    protected String recrutierName;

    @Column(name = "PLAN_FOR_PERIOD")
    private Integer planForPeriod;

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public Integer getPlanForPeriod() {
        return planForPeriod;
    }

    public void setPlanForPeriod(Integer planForPeriod) {
        this.planForPeriod = planForPeriod;
    }

    public String getRecrutierName() {
        return recrutierName;
    }

    public void setRecrutierName(String recrutierName) {
        this.recrutierName = recrutierName;
    }

    public Boolean getSubscribe() {
        return subscribe;
    }

    public void setSubscribe(Boolean subscribe) {
        this.subscribe = subscribe;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public void setReacrutier(User reacrutier) {
        this.reacrutier = reacrutier;
    }

    public User getReacrutier() {
        return reacrutier;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
}