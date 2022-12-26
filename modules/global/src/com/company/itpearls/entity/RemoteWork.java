package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum RemoteWork implements EnumClass<Integer> {

    NO(0),
    REMOTE(1),
    FIFTY_FIFTY(2);

    private Integer id;

    RemoteWork(Integer value) {
        this.id = value;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static RemoteWork fromId(Integer id) {
        for (RemoteWork at : RemoteWork.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}