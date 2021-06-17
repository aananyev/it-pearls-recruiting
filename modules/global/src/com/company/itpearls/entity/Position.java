package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|positionRuName")
@Table(name = "ITPEARLS_POSITION", indexes = {
        @Index(name = "IDX_ITPEARLS_POSITION_ID", columnList = "ID"),
        @Index(name = "IDX_ITPEARLS_POSITION_RU_NAME", columnList = "POSITION_RU_NAME"),
        @Index(name = "IDX_ITPEARLS_POSITION_EN_NAME", columnList = "POSITION_EN_NAME")
})

@Entity(name = "itpearls_Position")
public class Position extends StandardEntity {
    private static final long serialVersionUID = 8755535357083277650L;

    @NotNull
    @Column(name = "POSITION_RU_NAME", nullable = false, unique = true, length = 80)
    protected String positionRuName;

    @Column(name = "POSITION_EN_NAME", unique = true, length = 80)
    protected String positionEnName;

    @Lob
    @Column(name = "STANDART_DECRIPTION")
    private String standartDescription;

    @JoinTable(name = "ITPEARLS_JOB_CANDIDATE_POSITION_LINK",
            joinColumns = @JoinColumn(name = "POSITION_ID"),
            inverseJoinColumns = @JoinColumn(name = "JOB_CANDIDATE_ID"))
    @ManyToMany
    private List<JobCandidate> jobCandidates;

    public List<JobCandidate> getJobCandidates() {
        return jobCandidates;
    }

    public void setJobCandidates(List<JobCandidate> jobCandidates) {
        this.jobCandidates = jobCandidates;
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