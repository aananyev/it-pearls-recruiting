package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_STAFF_CURRENT")
@Entity(name = "itpearls_StaffCurrent")
@NamePattern("%s|staffingTable")
public class StaffCurrent extends StandardEntity {
    private static final long serialVersionUID = -7802125117033864204L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "EMPLOYEE_ID")
    private JobCandidate employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STAFFING_TABLE_ID")
    private StaffingTable staffingTable;

    @NotNull
    @Column(name = "SALARY", nullable = false)
    private Integer salary;

    @NotNull
    @Column(name = "FORM_EMPLOYMENT", nullable = false)
    private String formEmployment;

    public FormEmployment getFormEmployment() {
        return formEmployment == null ? null : FormEmployment.fromId(formEmployment);
    }

    public void setFormEmployment(FormEmployment formEmployment) {
        this.formEmployment = formEmployment == null ? null : formEmployment.getId();
    }

    public Integer getSalary() {
        return salary;
    }

    public void setSalary(Integer salary) {
        this.salary = salary;
    }

    public JobCandidate getEmployee() {
        return employee;
    }

    public void setEmployee(JobCandidate employee) {
        this.employee = employee;
    }

    public StaffingTable getStaffingTable() {
        return staffingTable;
    }

    public void setStaffingTable(StaffingTable staffingTable) {
        this.staffingTable = staffingTable;
    }
}