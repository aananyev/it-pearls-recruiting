package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum StdSelections implements EnumClass<Integer> {

    STAR_RED(1),
    STAR_YELLOW(2),
    STAR_GREEN(3),
    FLAG_RED(4),
    FLAG_YELLOW(5),
    FLAG_GREEN(6);

    private Integer id;

    StdSelections(Integer value) {
        this.id = value;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static StdSelections fromId(Integer id) {
        for (StdSelections at : StdSelections.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}