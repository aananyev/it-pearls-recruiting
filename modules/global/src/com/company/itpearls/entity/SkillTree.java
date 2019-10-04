package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

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