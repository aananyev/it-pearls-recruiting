package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_SKILL")
@Entity(name = "itpearls_Skill")
public class Skill extends StandardEntity {
    private static final long serialVersionUID = 6645117417531357013L;

    @NotNull
    @Column(name = "SKILL_NAME", nullable = false, unique = true, length = 80)
    protected String skillName;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SKILL_TYPE_ID")
    protected Specialisation skillType;

    public Specialisation getSkillType() {
        return skillType;
    }

    public void setSkillType(Specialisation skillType) {
        this.skillType = skillType;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }
}