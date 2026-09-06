package com.company.hunttech.service.dto.avatar;

import com.haulmont.cuba.core.entity.FileDescriptor;

import java.io.Serializable;
import java.util.Objects;

/**
 * DTO с результатом разрешения эффективного аватара пользователя.
 */
public class ResolvedAvatarInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_THEME_FALLBACK_PATH = "icons/no-programmer.jpeg";

    private final FileDescriptor fileDescriptor;
    private final AvatarSourceType sourceType;
    private final boolean fallbackUsed;
    private final String fallbackThemePath;

    public ResolvedAvatarInfo(FileDescriptor fileDescriptor, AvatarSourceType sourceType,
                              boolean fallbackUsed, String fallbackThemePath) {
        this.fileDescriptor = fileDescriptor;
        this.sourceType = sourceType;
        this.fallbackUsed = fallbackUsed;
        this.fallbackThemePath = fallbackThemePath != null ? fallbackThemePath : DEFAULT_THEME_FALLBACK_PATH;
    }

    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    public AvatarSourceType getSourceType() {
        return sourceType;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public String getFallbackThemePath() {
        return fallbackThemePath;
    }

    public boolean hasFileDescriptor() {
        return fileDescriptor != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedAvatarInfo that = (ResolvedAvatarInfo) o;
        return fallbackUsed == that.fallbackUsed &&
                Objects.equals(fileDescriptor, that.fileDescriptor) &&
                sourceType == that.sourceType &&
                Objects.equals(fallbackThemePath, that.fallbackThemePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileDescriptor, sourceType, fallbackUsed, fallbackThemePath);
    }

    @Override
    public String toString() {
        return "ResolvedAvatarInfo{" +
                "sourceType=" + sourceType +
                ", fileDescriptor=" + (fileDescriptor != null ? fileDescriptor.getId() : "null") +
                ", fallbackUsed=" + fallbackUsed +
                ", fallbackThemePath='" + fallbackThemePath + '\'' +
                '}';
    }
}
