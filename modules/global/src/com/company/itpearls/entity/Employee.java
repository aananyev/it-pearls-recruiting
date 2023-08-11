package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Table(name = "ITPEARLS_EMPLOYEE")
@Entity(name = "itpearls_Employee")
@NamePattern("%s %s|jobCandidate,openPosition")
public class Employee extends StandardEntity {
    private static final long serialVersionUID = -6920073032090937565L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    private JobCandidate jobCandidate;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OPEN_POSITION_ID")
    private OpenPosition openPosition;

    @Temporal(TemporalType.DATE)
    @Column(name = "EMPLOYEE_DATE")
    private Date employeeDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "DISSMISAL_DATE")
    private Date dissmisalDate;

    @Column(name = "OUTSTAFFING_COST")
    private BigDecimal outstaffingCost;

    @Column(name = "SALARY")
    private BigDecimal salary;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LABOR_AGREEMENT_ID")
    private LaborAgreement laborAgreement;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WORK_STATUS_ID")
    private EmployeeWorkStatus workStatus;

    public void setLaborAgreement(LaborAgreement laborAgreement) {
        this.laborAgreement = laborAgreement;
    }

    public LaborAgreement getLaborAgreement() {
        return laborAgreement;
    }

    public void setWorkStatus(EmployeeWorkStatus workStatus) {
        this.workStatus = workStatus;
    }

    public EmployeeWorkStatus getWorkStatus() {
        return workStatus;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public BigDecimal getOutstaffingCost() {
        return outstaffingCost;
    }

    public void setOutstaffingCost(BigDecimal outstaffingCost) {
        this.outstaffingCost = outstaffingCost;
    }

    public Date getDissmisalDate() {
        return dissmisalDate;
    }

    public void setDissmisalDate(Date dissmisalDate) {
        this.dissmisalDate = dissmisalDate;
    }

    public Date getEmployeeDate() {
        return employeeDate;
    }

    public void setEmployeeDate(Date employeeDate) {
        this.employeeDate = employeeDate;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }
}
