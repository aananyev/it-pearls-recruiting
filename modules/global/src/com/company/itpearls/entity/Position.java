package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@NamePattern("%s %s|positionEnName,positionRuName")
@Table(name = "ITPEARLS_POSITION")
@Entity(name = "itpearls_Position")
public class Position extends StandardEntity {
    private static final long serialVersionUID = 8755535357083277650L;

    @NotNull
    @Column(name = "POSITION_RU_NAME", nullable = false, unique = true, length = 80)
    protected String positionRuName;

    @NotNull
    @Column(name = "POSITION_EN_NAME", nullable = false, unique = true, length = 80)
    protected String positionEnName;

    public String getPositionEnName() {
        return positionEnName;
    }

    public void setPositionEnName(String positionEnName) {
        this.positionEnName = positionEnName;
    }

    public String getPositionRuName() {
        return positionRuName;
    }

    public void setPositionRuName(String positionRuName) {
        this.positionRuName = positionRuName;
    }
}