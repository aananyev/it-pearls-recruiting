package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

/**
 * Перечисление критичности / приоритета навыка кандидата.
 */
public enum CandidateSkillPriority implements EnumClass<Integer> {

    /** Основной / обязательный навык */
    MAIN(10),

    /** Второстепенный / желательный навык */
    SECONDARY(20),

    /** Третьестепенный / дополнительный навык */
    TERTIARY(30);

    private final Integer id;

    CandidateSkillPriority(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static CandidateSkillPriority fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (CandidateSkillPriority value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
