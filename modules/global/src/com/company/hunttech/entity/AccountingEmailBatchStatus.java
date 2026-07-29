package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AccountingEmailBatchStatus implements EnumClass<Integer> {

    DRAFT(10),
    READY_TO_SEND(20),
    SENT(30),
    ERROR(40),
    CANCELLED(50);

    private final Integer id;

    AccountingEmailBatchStatus(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AccountingEmailBatchStatus fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AccountingEmailBatchStatus value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
