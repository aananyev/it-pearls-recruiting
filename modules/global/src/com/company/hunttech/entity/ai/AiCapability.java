package com.company.hunttech.entity.ai;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

/**
 * Тип технической возможности, необходимой AI-функции.
 *
 * На первом этапе execution layer выполняет текстовые capability. Остальные значения
 * зарезервированы в модели, чтобы не связывать бизнес-функции только с generateText().
 */
public enum AiCapability implements EnumClass<String> {
    TEXT_GENERATION("TEXT_GENERATION"),
    TEXT_ANALYSIS("TEXT_ANALYSIS"),
    TEXT_TRANSFORMATION("TEXT_TRANSFORMATION"),
    VISION("VISION"),
    IMAGE_GENERATION("IMAGE_GENERATION"),
    EMBEDDING("EMBEDDING"),
    DOCUMENT_ANALYSIS("DOCUMENT_ANALYSIS"),
    AUDIO_TRANSCRIPTION("AUDIO_TRANSCRIPTION");

    private final String id;

    AiCapability(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public static AiCapability fromId(String id) {
        if (id == null) {
            return null;
        }
        for (AiCapability value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return null;
    }
}
