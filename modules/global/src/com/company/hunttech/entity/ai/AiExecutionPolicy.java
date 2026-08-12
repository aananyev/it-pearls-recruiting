package com.company.hunttech.entity.ai;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

/**
 * Политика выбора корпоративного или персонального подключения для AI-функции.
 */
public enum AiExecutionPolicy implements EnumClass<String> {
    ADMIN_ONLY("ADMIN_ONLY"),
    USER_OVERRIDE_ALLOWED("USER_OVERRIDE_ALLOWED"),
    USER_REQUIRED("USER_REQUIRED");

    private final String id;

    AiExecutionPolicy(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public static AiExecutionPolicy fromId(String id) {
        if (id == null) {
            return null;
        }
        for (AiExecutionPolicy value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return null;
    }
}
