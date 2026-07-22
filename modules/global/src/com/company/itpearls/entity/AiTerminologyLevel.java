package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AiTerminologyLevel implements EnumClass<Integer> {

    PLAIN(10),
    PROFESSIONAL(20),
    EXPERT(30);

    private final Integer id;

    AiTerminologyLevel(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AiTerminologyLevel fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AiTerminologyLevel value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
