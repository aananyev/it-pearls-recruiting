package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AiResponseDetailLevel implements EnumClass<Integer> {

    BRIEF(10),
    BALANCED(20),
    DETAILED(30);

    private final Integer id;

    AiResponseDetailLevel(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AiResponseDetailLevel fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AiResponseDetailLevel value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
