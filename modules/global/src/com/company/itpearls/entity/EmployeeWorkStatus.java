package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_EMPLOYEE_WORK_STATUS")
@Entity(name = "itpearls_EmployeeWorkStatus")
@NamePattern("%s|workStatusName")
public class EmployeeWorkStatus extends StandardEntity {
    private static final long serialVersionUID = -2117517646534436026L;

    @NotNull
    @Column(name = "WORK_STATUS_NAME", nullable = false, length = 60)
    private String workStatusName;

    @Column(name = "IN_STAFF")
    private Boolean inStaff;

    public Boolean getInStaff() {
        return inStaff;
    }

    public void setInStaff(Boolean inStaff) {
        this.inStaff = inStaff;
    }

    public String getWorkStatusName() {
        return workStatusName;
    }

    public void setWorkStatusName(String workStatusName) {
        this.workStatusName = workStatusName;
    }
}