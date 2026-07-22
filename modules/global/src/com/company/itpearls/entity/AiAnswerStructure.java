package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AiAnswerStructure implements EnumClass<Integer> {

    AUTO(10),
    EXECUTIVE_SUMMARY(20),
    ACTION_PLAN(30),
    STEP_BY_STEP(40),
    CHECKLIST(50),
    TABLE(60);

    private final Integer id;

    AiAnswerStructure(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AiAnswerStructure fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AiAnswerStructure value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
