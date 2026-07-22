package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AiCommunicationStyle implements EnumClass<Integer> {

    DIRECT(10),
    NEUTRAL(20),
    COACHING(30);

    private final Integer id;

    AiCommunicationStyle(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AiCommunicationStyle fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AiCommunicationStyle value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
