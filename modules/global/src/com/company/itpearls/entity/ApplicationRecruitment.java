package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "ITPEARLS_APPLICATION_RECRUITMENT")
@Entity(name = "itpearls_ApplicationRecruitment")
public class ApplicationRecruitment extends StandardEntity {
    private static final long serialVersionUID = 8849531439698151090L;

    @Column(name = "ACTIVE")
    private Boolean active;

    @NotNull
    @OnDelete(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "STAFFING_TABLE_ID")
    private StaffingTable staffingTable;

    @Column(name = "APPROVAL")
    private Boolean approval;

    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    @Column(name = "APPLICATION_DATE", nullable = false)
    private Date applicationDate;

    @NotNull
    @Column(name = "AMOUNT", nullable = false)
    private Integer amount;

    @Temporal(TemporalType.DATE)
    @NotNull
    @Column(name = "EXIT_DATE", nullable = false)
    private Date exitDate;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APPLICATION_RECRUITMENT_LIST_ID")
    private ApplicationRecruitmentList applicationRecruitmentList;

    public ApplicationRecruitmentList getApplicationRecruitmentList() {
        return applicationRecruitmentList;
    }

    public void setApplicationRecruitmentList(ApplicationRecruitmentList applicationRecruitmentList) {
        this.applicationRecruitmentList = applicationRecruitmentList;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Date getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(Date applicationDate) {
        this.applicationDate = applicationDate;
    }

    public Boolean getApproval() {
        return approval;
    }

    public void setApproval(Boolean approval) {
        this.approval = approval;
    }

    public Date getExitDate() {
        return exitDate;
    }

    public void setExitDate(Date exitDate) {
        this.exitDate = exitDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public StaffingTable getStaffingTable() {
        return staffingTable;
    }

    public void setStaffingTable(StaffingTable staffingTable) {
        this.staffingTable = staffingTable;
    }
}