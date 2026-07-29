package com.company.hunttech.core;

import com.company.hunttech.entity.AccountingDocumentType;
import com.company.hunttech.entity.AccountingFlowType;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class AccountingDocumentIngestSupport {
    private static final int MAX_SAFE_FILE_NAME_LENGTH = 120;
    private static final DateTimeFormatter STORED_FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private AccountingDocumentIngestSupport() {
    }

    static boolean isSupportedTelegramFile(String originalFileName, String mimeType) {
        String normalizedMimeType = normalize(mimeType);
        if (normalizedMimeType.startsWith("image/") || "application/pdf".equals(normalizedMimeType)) {
            return true;
        }

        String extension = extensionOf(originalFileName);
        return "pdf".equals(extension)
                || "jpg".equals(extension)
                || "jpeg".equals(extension)
                || "png".equals(extension)
                || "heic".equals(extension)
                || "heif".equals(extension);
    }

    static AccountingFlowType resolveFlowType(String originalFileName, String caption) {
        String text = normalize(originalFileName + " " + caption);
        if (containsAny(text, "чек", "авансов", "топливо", "азс", "бензин", "дизель", "такси", "парковк")) {
            return AccountingFlowType.ADVANCE_REPORT;
        }
        return AccountingFlowType.PRIMARY;
    }

    static AccountingDocumentType resolveDocumentType(String originalFileName, String caption,
                                                      AccountingFlowType flowType) {
        if (AccountingFlowType.ADVANCE_REPORT.equals(flowType)) {
            return AccountingDocumentType.RECEIPT;
        }

        String text = normalize(originalFileName + " " + caption);
        if (text.contains("договор")) {
            return AccountingDocumentType.CONTRACT;
        }
        if (text.contains("упд")) {
            return AccountingDocumentType.UPD;
        }
        if (text.contains("акт")) {
            return AccountingDocumentType.ACT;
        }
        if (text.contains("счет") || text.contains("счёт")) {
            return AccountingDocumentType.INVOICE;
        }
        if (text.contains("задани")) {
            return AccountingDocumentType.TASK;
        }
        return AccountingDocumentType.OTHER;
    }

    static String buildStoredFileName(String originalFileName, String telegramChatId, Long telegramMessageId,
                                      LocalDateTime receivedAt) {
        String extension = extensionOf(originalFileName);
        if (extension.isEmpty()) {
            extension = "bin";
        }

        String baseName = originalFileName == null ? "telegram-document" : originalFileName;
        int extensionStart = baseName.lastIndexOf('.');
        if (extensionStart > 0) {
            baseName = baseName.substring(0, extensionStart);
        }
        String safeBaseName = sanitizeFileName(baseName);
        String safeChatId = sanitizeFileName(telegramChatId == null ? "chat" : telegramChatId);
        String messagePart = telegramMessageId == null ? "message" : telegramMessageId.toString();

        return STORED_FILE_TIME.format(receivedAt)
                + "-" + safeChatId
                + "-" + messagePart
                + "-" + safeBaseName
                + "." + extension;
    }

    static Path ensureUniquePath(Path targetPath) {
        if (!targetPath.toFile().exists()) {
            return targetPath;
        }

        String fileName = targetPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String name = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        Path parent = targetPath.getParent();
        int counter = 2;
        Path candidate;
        do {
            candidate = parent.resolve(name + "-" + counter + extension);
            counter++;
        } while (candidate.toFile().exists());
        return candidate;
    }

    static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitizeFileName(String value) {
        String safeValue = value == null ? "" : value.trim();
        safeValue = safeValue.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("\\s+", " ")
                .replaceAll("_+", "_");
        if (safeValue.isEmpty()) {
            safeValue = "document";
        }
        if (safeValue.length() > MAX_SAFE_FILE_NAME_LENGTH) {
            safeValue = safeValue.substring(0, MAX_SAFE_FILE_NAME_LENGTH).trim();
        }
        return safeValue;
    }

    private static boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
