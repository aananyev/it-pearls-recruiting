package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_REQUIRED_PARAMETERS")
@Entity(name = "itpearls_RequiredParameters")
public class RequiredParameters extends StandardEntity {
    private static final long serialVersionUID = -6373273416700646431L;

    @NotNull
    @Column(name = "NUMBER_", nullable = false, unique = true)
    protected Integer number;

    @NotNull
    @Column(name = "PARAMETER_NAME", nullable = false, unique = true, length = 80)
    protected String parameterName;

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }
}