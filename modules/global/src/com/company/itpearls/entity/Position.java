package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@NamePattern("%s / %s|positionRuName,positionEnName")
@Table(name = "ITPEARLS_POSITION", indexes = {
        @Index(name = "IDX_ITPEARLS_POSITION_RU_NAME", columnList = "POSITION_RU_NAME"),
        @Index(name = "IDX_ITPEARLS_POSITION_EN_NAME", columnList = "POSITION_EN_NAME")
})

@Entity(name = "itpearls_Position")
public class Position extends StandardEntity {
    private static final long serialVersionUID = 8755535357083277650L;

    @NotNull
    @Column(name = "POSITION_RU_NAME", nullable = false, unique = true, length = 80)
    protected String positionRuName;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOGO_ID")
    private FileDescriptor logo;

    @Column(name = "POSITION_EN_NAME", unique = true, length = 80)
    protected String positionEnName;

    @Lob
    @Column(name = "STANDART_DECRIPTION")
    private String standartDescription;

    @Lob
    @Column(name = "WHO_IS_THIS_GUY")
    private String whoIsThisGuy;

    @OnDeleteInverse(DeletePolicy.UNLINK)
    @OnDelete(DeletePolicy.UNLINK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    private JobCandidate jobCandidate;

    public FileDescriptor getLogo() {
        return logo;
    }

    public void setLogo(FileDescriptor logo) {
        this.logo = logo;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public String getWhoIsThisGuy() {
        return whoIsThisGuy;
    }

    public void setWhoIsThisGuy(String whoIsThisGuy) {
        this.whoIsThisGuy = whoIsThisGuy;
    }

    public String getStandartDescription() {
        return standartDescription;
    }

    public void setStandartDescription(String standartDescription) {
        this.standartDescription = standartDescription;
    }

    public String getPositionRuName() {
        return positionRuName;
    }

    public void setPositionRuName(String positionRuName) {
        this.positionRuName = positionRuName;
    }

    public void setPositionEnName(String positionEnName) {
        this.positionEnName = positionEnName;
    }

    public String getPositionEnName() {
        return positionEnName;
    }

}