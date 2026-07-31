package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AccountingDocumentEventType implements EnumClass<Integer> {

    RECEIVED(10),
    OCR_PROCESSED(20),
    CONFIRMATION_REQUESTED(30),
    CONFIRMED(40),
    RENAMED(50),
    MOVED_TO_FINAL_FOLDER(60),
    ADDED_TO_EMAIL_BATCH(70),
    SENT(80),
    MOVED_TO_SENT_FOLDER(90),
    REJECTED(100),
    ERROR(110);

    private final Integer id;

    AccountingDocumentEventType(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AccountingDocumentEventType fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AccountingDocumentEventType value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
