package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Table(name = "ITPEARLS_APPLICATION_RECRUITMENT_LIST")
@Entity(name = "itpearls_ApplicationRecruitmentList")
@NamePattern("%s|code")
public class ApplicationRecruitmentList extends StandardEntity {
    private static final long serialVersionUID = -792797817950444495L;

    @Column(name = "ACTIVE")
    private Boolean active;

    @Temporal(TemporalType.DATE)
    @Column(name = "OPEN_DATE")
    private Date openDate;

    @NotNull
    @Column(name = "QUICK_DESCRIPTION", nullable = false, length = 80)
    private String code;

    @Temporal(TemporalType.DATE)
    @Column(name = "CLOSE_DATE")
    private Date closeDate;

    private @OnDelete(DeletePolicy.CASCADE)
    @Composition
    @OneToMany(mappedBy = "applicationRecruitmentList")
    List<ApplicationRecruitment> applicationRecruitment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECRUITER_ID")
    private ExtUser recruiter;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(Date closeDate) {
        this.closeDate = closeDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setApplicationRecruitment(List<ApplicationRecruitment> applicationRecruitment) {
        this.applicationRecruitment = applicationRecruitment;
    }

    public List<ApplicationRecruitment> getApplicationRecruitment() {
        return applicationRecruitment;
    }

    public ExtUser getRecruiter() {
        return recruiter;
    }

    public void setRecruiter(ExtUser recruiter) {
        this.recruiter = recruiter;
    }

}