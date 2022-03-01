package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_POSSIBLE_NAMES", indexes = {
        @Index(name = "IDX_ITPEARLS_POSSIBLE_NAMES_EN_NAME_POSITION", columnList = "POSSIBLE_EN_NAME_POSIION"),
        @Index(name = "IDX_ITPEARLS_POSSIBLE_NAMES_POSSIBLE_NAME", columnList = "POSSIBLE_NAME")
})
@Entity(name = "itpearls_PossibleNames")
public class PossibleNames extends StandardEntity {
    private static final long serialVersionUID = 7412726477941831771L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @NotNull
    @OnDeleteInverse(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "POSITION_ID")
    private Position position;

    @NotNull
    @Column(name = "POSSIBLE_EN_NAME_POSIION", nullable = false, unique = true, length = 128)
    private String possibleEnNamePosiion;

    @NotNull
    @Column(name = "POSSIBLE_NAME", nullable = false, unique = true, length = 128)
    private String possibleRuNamePosition;

    public String getPossibleEnNamePosiion() {
        return possibleEnNamePosiion;
    }

    public void setPossibleEnNamePosiion(String possibleEnNamePosiion) {
        this.possibleEnNamePosiion = possibleEnNamePosiion;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getPossibleRuNamePosition() {
        return possibleRuNamePosition;
    }

    public void setPossibleRuNamePosition(String possibleRuNamePosition) {
        this.possibleRuNamePosition = possibleRuNamePosition;
    }
}