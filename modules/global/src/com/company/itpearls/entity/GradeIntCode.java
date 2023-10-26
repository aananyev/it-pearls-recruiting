package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum GradeIntCode implements EnumClass<Integer> {

    JUNIOR(0),
    REGULAR(1),
    MASTER(2),
    GRAND_MASTER(3);

    private Integer id;

    GradeIntCode(Integer value) {
        this.id = value;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static GradeIntCode fromId(Integer id) {
        for (GradeIntCode at : GradeIntCode.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}