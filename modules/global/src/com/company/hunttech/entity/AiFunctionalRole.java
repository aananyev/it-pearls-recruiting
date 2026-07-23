package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

public enum AiFunctionalRole implements EnumClass<Integer> {

    RECRUITER(10),
    RESEARCHER(20),
    RECRUITMENT_LEAD(30),
    ACCOUNT_MANAGER(40),
    HR_MANAGER(50),
    TECHNICAL_EXPERT(60),
    PROJECT_MANAGER(70),
    EXECUTIVE(80),
    OTHER(90);

    private final Integer id;

    AiFunctionalRole(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static AiFunctionalRole fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (AiFunctionalRole value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
