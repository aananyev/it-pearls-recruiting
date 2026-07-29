package com.company.hunttech.core;

import java.io.Serializable;
import java.util.UUID;

public class AccountingDocumentIngestResult implements Serializable {
    private static final long serialVersionUID = 5807976889392712441L;

    private final boolean success;
    private final boolean duplicate;
    private final UUID documentId;
    private final String message;
    private final String storedPath;

    private AccountingDocumentIngestResult(boolean success, boolean duplicate, UUID documentId, String message,
                                           String storedPath) {
        this.success = success;
        this.duplicate = duplicate;
        this.documentId = documentId;
        this.message = message;
        this.storedPath = storedPath;
    }

    public static AccountingDocumentIngestResult success(UUID documentId, String message, String storedPath) {
        return new AccountingDocumentIngestResult(true, false, documentId, message, storedPath);
    }

    public static AccountingDocumentIngestResult failure(String message) {
        return new AccountingDocumentIngestResult(false, false, null, message, null);
    }

    public static AccountingDocumentIngestResult duplicate(String message) {
        return new AccountingDocumentIngestResult(false, true, null, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getMessage() {
        return message;
    }

    public String getStoredPath() {
        return storedPath;
    }
}
