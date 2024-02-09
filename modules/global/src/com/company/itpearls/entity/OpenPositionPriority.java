package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum OpenPositionPriority implements EnumClass<Integer> {

    DRAFT(-1),
    PAUSED(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4);

    private Integer id;

    OpenPositionPriority(Integer value) {
        this.id = value;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static OpenPositionPriority fromId(Integer id) {
        for (OpenPositionPriority at : OpenPositionPriority.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }

    public static OpenPositionPriority fromName(String strName) {
        for (OpenPositionPriority at : OpenPositionPriority.values()) {
            if (at.name().equalsIgnoreCase(strName)) {
                return at;
            }
        }
        return null;
    }
}