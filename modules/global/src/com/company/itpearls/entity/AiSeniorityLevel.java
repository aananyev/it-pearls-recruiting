package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AiSeniorityLevel implements EnumClass<Integer> {

    JUNIOR(10),
    MIDDLE(20),
    SENIOR(30),
    LEAD(40),
    HEAD(50),
    EXECUTIVE(60);

    private final Integer id;

    AiSeniorityLevel(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AiSeniorityLevel fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AiSeniorityLevel value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
