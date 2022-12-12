package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum StaffInteractionStatus implements EnumClass<Integer> {

    DISPOSAL(-1),
    ADVENT(1),
    NOTHING(0);

    private Integer id;

    StaffInteractionStatus(Integer value) {
        this.id = value;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static StaffInteractionStatus fromId(Integer id) {
        for (StaffInteractionStatus at : StaffInteractionStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}