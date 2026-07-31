package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AccountingDocumentType implements EnumClass<Integer> {

    CONTRACT(10),
    ACT(20),
    UPD(30),
    INVOICE(40),
    TASK(50),
    RECEIPT(60),
    OTHER(90);

    private final Integer id;

    AccountingDocumentType(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AccountingDocumentType fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AccountingDocumentType value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
