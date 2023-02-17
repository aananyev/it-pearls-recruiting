package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum StdUserGroup implements EnumClass<String> {

    ACCOUNTING("Аккаунтинг"),
    MANAGEMENT("Менеджмент"),
    RESEARCHING("Ресерчинг"),
    STAGER("Стажер"),
    HUNTING("Хантинг");

    private String id;

    StdUserGroup(String value) {
        this.id = value;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static StdUserGroup fromId(String id) {
        for (StdUserGroup at : StdUserGroup.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}