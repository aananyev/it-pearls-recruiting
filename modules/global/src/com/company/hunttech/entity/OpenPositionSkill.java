package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Сущность «Требуемые навыки вакансии / позиции».
 * <p>
 * Связывает сущность вакансии ({@link OpenPosition}) со справочником навыков ({@link SkillTree})
 * с указанием признака критичности требования: главное, вспомогательное или прочее ({@link CandidateSkillPriority}).
 * <p>
 * Для конкретной позиции навыки не должны повторяться (уникальный композитный индекс).
 */
@NamePattern("%s (%s)|skill,priority")
@Table(name = "HUNTTECH_OPEN_POSITION_SKILL", indexes = {
        @Index(name = "IDX_HUNTTECH_OPEN_POSITION_SKILL_POSITION", columnList = "OPEN_POSITION_ID"),
        @Index(name = "IDX_HUNTTECH_OPEN_POSITION_SKILL_SKILL", columnList = "SKILL_ID")
}, uniqueConstraints = {
        @UniqueConstraint(name = "IDX_HUNTTECH_OPEN_POSITION_SKILL_UNQ", columnNames = {"OPEN_POSITION_ID", "SKILL_ID", "DELETE_TS"})
})
@Entity(name = "hunttech_OpenPositionSkill")
public class OpenPositionSkill extends StandardEntity {
    private static final long serialVersionUID = 6129384712093847124L;

    /** Вакансия / позиция, к которой относится требуемый навык */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OPEN_POSITION_ID")
    protected OpenPosition openPosition;

    /** Навык из централизованного справочника skilltree */
    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SKILL_ID")
    protected SkillTree skill;

    /** Признак критичности / уровень навыка (главное, вспомогательное, прочее) */
    @Column(name = "PRIORITY")
    protected Integer priority;

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
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
