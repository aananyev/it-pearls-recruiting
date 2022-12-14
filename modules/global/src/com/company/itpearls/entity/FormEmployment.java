package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum FormEmployment implements EnumClass<String> {

    FULLTIME("Полная занятость"),
    PARTTIME("Частичная занятость");

    private String id;

    FormEmployment(String value) {
        this.id = value;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static FormEmployment fromId(String id) {
        for (FormEmployment at : FormEmployment.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}