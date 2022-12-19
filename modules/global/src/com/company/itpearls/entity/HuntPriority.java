package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum HuntPriority implements EnumClass<Integer> {

    DRAFT(-1),
    PAUSED(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4);

    private Integer id;

    HuntPriority(Integer value) {
        this.id = value;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static HuntPriority fromId(Integer id) {
        for (HuntPriority at : HuntPriority.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}