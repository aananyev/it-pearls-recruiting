package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Table(name = "ITPEARLS_STAFFING_TABLE")
@Entity(name = "itpearls_StaffingTable")
@NamePattern("%s|openPosition")
public class StaffingTable extends StandardEntity {
    private static final long serialVersionUID = -6705890660852104185L;

    @Column(name = "ACTIVE")
    private Boolean active;

    @Column(name = "CODE", length = 32)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OPEN_POSITION_ID")
    @NotNull
    private OpenPosition openPosition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "GRADE_ID")
    @NotNull
    private Grade grade;

    @Column(name = "NUMBER_OF_STAFF", nullable = false)
    @NotNull
    private Integer numberOfStaff;

    @Column(name = "SALARY_MIN")
    private BigDecimal salaryMin;

    @Column(name = "SALARY_MAX")
    private String salaryMax;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    public String getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(String salaryMax) {
        this.salaryMax = salaryMax;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getNumberOfStaff() {
        return numberOfStaff;
    }

    public void setNumberOfStaff(Integer numberOfStaff) {
        this.numberOfStaff = numberOfStaff;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

}