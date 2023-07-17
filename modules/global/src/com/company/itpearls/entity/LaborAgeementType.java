package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_LABOR_AGEEMENT_TYPE", indexes = {
        @Index(name = "IDX_ITPEARLS_LABOR_AGEEMENT_TYPE_NAME", columnList = "NAME_AGREEMENT")
})
@Entity(name = "itpearls_LaborAgeementType")
@NamePattern("%s|nameAgreement")
public class LaborAgeementType extends StandardEntity {
    private static final long serialVersionUID = -9202105476505288640L;

    @Column(name = "EMPLOYEE_ORCOMPANY")
    private Integer employeeOrcompany;

    @Column(name = "NAME_AGREEMENT", nullable = false, length = 80)
    @NotNull
    private String nameAgreement;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public void setEmployeeOrcompany(Integer employeeOrcompany) {
        this.employeeOrcompany = employeeOrcompany;
    }

    public Integer getEmployeeOrcompany() {
        return employeeOrcompany;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getNameAgreement() {
        return nameAgreement;
    }

    public void setNameAgreement(String nameAgreement) {
        this.nameAgreement = nameAgreement;
    }
}