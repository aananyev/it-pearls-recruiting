package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AiPreferredLanguage implements EnumClass<Integer> {

    AUTO(10),
    RUSSIAN(20),
    ENGLISH(30);

    private final Integer id;

    AiPreferredLanguage(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AiPreferredLanguage fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AiPreferredLanguage value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
