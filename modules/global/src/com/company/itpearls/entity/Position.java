package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;
import org.springframework.beans.factory.annotation.Lookup;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|positionRuName")
@Table(name = "ITPEARLS_POSITION")
@Entity(name = "itpearls_Position")
public class Position extends StandardEntity {
    private static final long serialVersionUID = 8755535357083277650L;

    @NotNull
    @Column(name = "POSITION_RU_NAME", nullable = false, unique = true, length = 80)
    protected String positionRuName;

    public String getPositionRuName() {
        return positionRuName;
    }

    public void setPositionRuName(String positionRuName) {
        this.positionRuName = positionRuName;
    }
}