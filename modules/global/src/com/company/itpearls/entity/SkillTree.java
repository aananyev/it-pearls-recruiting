package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|skillName")
@Table(name = "ITPEARLS_SKILL_TREE")
@Entity(name = "itpearls_SkillTree")
public class SkillTree extends StandardEntity {
    private static final long serialVersionUID = -1280658717332773151L;

    @NotNull
    @Column(name = "SKILL_NAME", nullable = false, unique = true, length = 80)
    protected String skillName;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SKILL_TREE_ID")
    protected SkillTree skillTree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    protected JobCandidate jobCandidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    protected OpenPosition openPosition;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "skillTree")
    protected List<JobCandidate> candidates;

    public List<JobCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<JobCandidate> candidates) {
        this.candidates = candidates;
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

    public SkillTree getSkillTree() {
        return skillTree;
    }

    public void setSkillTree(SkillTree skillTree) {
        this.skillTree = skillTree;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }
}