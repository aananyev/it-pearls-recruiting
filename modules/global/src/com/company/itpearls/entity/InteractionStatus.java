package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum InteractionStatus implements EnumClass<Integer> {

    DISPOSAL(-1),
    ADVENT(1),
    NOTHING(0);

    private Integer id;

    InteractionStatus(Integer value) {
        this.id = value;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static InteractionStatus fromId(Integer id) {
        for (InteractionStatus at : InteractionStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}