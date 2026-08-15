package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Сущность «Навыки кандидата».
 * <p>
 * Связывает сущность кандидата ({@link JobCandidate}) со справочником навыков ({@link SkillTree})
 * с указанием признака критичности навыка: основной, второстепенный или третьестепенный ({@link CandidateSkillPriority}).
 * <p>
 * Для конкретного кандидата навыки не должны повторяться (уникальный композитный индекс).
 */
@NamePattern("%s (%s)|skill,priority")
@Table(name = "HUNTTECH_CANDIDATE_SKILL", indexes = {
        @Index(name = "IDX_HUNTTECH_CANDIDATE_SKILL_CANDIDATE", columnList = "CANDIDATE_ID"),
        @Index(name = "IDX_HUNTTECH_CANDIDATE_SKILL_SKILL", columnList = "SKILL_ID")
}, uniqueConstraints = {
        @UniqueConstraint(name = "IDX_HUNTTECH_CANDIDATE_SKILL_UNQ", columnNames = {"CANDIDATE_ID", "SKILL_ID", "DELETE_TS"})
})
@Entity(name = "hunttech_CandidateSkill")
public class CandidateSkill extends StandardEntity {
    private static final long serialVersionUID = 5129384712093847123L;

    /** Кандидат, к которому относится навык */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_ID")
    protected JobCandidate candidate;

    /** Навык из централизованного справочника skilltree */
    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SKILL_ID")
    protected SkillTree skill;

    /** Признак критичности / уровень навыка (основной, второстепенный, третьестепенный) */
    @Column(name = "PRIORITY")
    protected Integer priority;

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }

    public SkillTree getSkill() {
        return skill;
    }

    public void setSkill(SkillTree skill) {
        this.skill = skill;
    }

    public CandidateSkillPriority getPriority() {
        return priority == null ? null : CandidateSkillPriority.fromId(priority);
    }

    public void setPriority(CandidateSkillPriority priority) {
        this.priority = priority == null ? null : priority.getId();
    }
}
