package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AccountingRecipientRole implements EnumClass<Integer> {

    PRIMARY_ACCOUNTANT(10),
    BACKUP_ACCOUNTANT(20),
    OWNER_COPY(30),
    OTHER(90);

    private final Integer id;

    AccountingRecipientRole(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AccountingRecipientRole fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AccountingRecipientRole value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
