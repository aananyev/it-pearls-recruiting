package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum GradeCode implements EnumClass<String> {

    JUNIOR("Junior"),
    REGULAR("Regular"),
    MASTER("Master"),
    GRAND_MASTER("Grand Master");

    private String id;

    GradeCode(String value) {
        this.id = value;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static GradeCode fromId(String id) {
        for (GradeCode at : GradeCode.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}