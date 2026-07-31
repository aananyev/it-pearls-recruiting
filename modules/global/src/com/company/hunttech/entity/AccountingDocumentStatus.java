package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AccountingDocumentStatus implements EnumClass<Integer> {

    NEW(10),
    WAITING_CONFIRMATION(20),
    WAITING_COMPANY_MATCH(30),
    CONFIRMED(40),
    READY_TO_SEND(50),
    SENT(60),
    REJECTED(70),
    BAD_SCAN(80),
    ERROR(90);

    private final Integer id;

    AccountingDocumentStatus(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AccountingDocumentStatus fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AccountingDocumentStatus value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
