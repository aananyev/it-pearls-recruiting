package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

/**
 * Статусы решения рекрутера по кандидату из аналитического списка AI-подбора.
 */
public enum RecruiterDecisionType implements EnumClass<Integer> {

    NEW(10, "Не обработан"),
    IN_WORK(20, "В работе"),
    POSTPONED(30, "Отложен"),
    REJECTED(40, "Не подходит");

    private final Integer id;
    private final String caption;

    RecruiterDecisionType(Integer id, String caption) {
        this.id = id;
        this.caption = caption;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public String getCaption() {
        return caption;
    }

    @Nullable
    public static RecruiterDecisionType fromId(Integer id) {
        for (RecruiterDecisionType at : RecruiterDecisionType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}
