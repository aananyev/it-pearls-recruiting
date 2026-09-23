package com.company.hunttech.service.dto;

import com.company.hunttech.service.AiExecutionResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Результат сканирования резюме кандидата на наличие контактных данных и фото.
 */
public class CandidateContactsScanResult implements Serializable {
    private static final long serialVersionUID = 7291048201948201934L;

    private boolean success;
    private long durationMs;
    private String rawError;
    private AiExecutionResult aiExecution;

    private CandidateExtractedContactsDto extractedContacts;
    private List<String> updatedFields = new ArrayList<>();
    private boolean photoExtracted;
    private int totalContactsFound;
    private int totalContactsUpdated;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getRawError() {
        return rawError;
    }

    public void setRawError(String rawError) {
        this.rawError = rawError;
    }

    public AiExecutionResult getAiExecution() {
        return aiExecution;
    }

    public void setAiExecution(AiExecutionResult aiExecution) {
        this.aiExecution = aiExecution;
    }

    public CandidateExtractedContactsDto getExtractedContacts() {
        return extractedContacts;
    }

    public void setExtractedContacts(CandidateExtractedContactsDto extractedContacts) {
        this.extractedContacts = extractedContacts;
    }

    public List<String> getUpdatedFields() {
        return updatedFields;
    }

    public void setUpdatedFields(List<String> updatedFields) {
        this.updatedFields = updatedFields;
    }

    public boolean isPhotoExtracted() {
        return photoExtracted;
    }

    public void setPhotoExtracted(boolean photoExtracted) {
        this.photoExtracted = photoExtracted;
    }

    public int getTotalContactsFound() {
        return totalContactsFound;
    }

    public void setTotalContactsFound(int totalContactsFound) {
        this.totalContactsFound = totalContactsFound;
    }

    public int getTotalContactsUpdated() {
        return totalContactsUpdated;
    }

    public void setTotalContactsUpdated(int totalContactsUpdated) {
        this.totalContactsUpdated = totalContactsUpdated;
    }
}
