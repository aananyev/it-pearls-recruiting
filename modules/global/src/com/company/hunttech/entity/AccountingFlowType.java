package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AccountingFlowType implements EnumClass<Integer> {

    PRIMARY(10),
    ADVANCE_REPORT(20),
    ALL(90);

    private final Integer id;

    AccountingFlowType(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AccountingFlowType fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AccountingFlowType value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
