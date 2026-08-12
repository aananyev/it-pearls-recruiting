package com.company.hunttech.entity.ai;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

/**
 * Политика повторного выполнения после ошибки персонального AI-подключения.
 */
public enum AiFallbackPolicy implements EnumClass<String> {
    NO_FALLBACK("NO_FALLBACK"),
    FALLBACK_TO_ADMIN("FALLBACK_TO_ADMIN");

    private final String id;

    AiFallbackPolicy(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public static AiFallbackPolicy fromId(String id) {
        if (id == null) {
            return null;
        }
        for (AiFallbackPolicy value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return null;
    }
}
