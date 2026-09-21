package com.company.hunttech.dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * Снимок фактического прогресса AI-подбора вакансий для кандидата.
 *
 * <p>DTO передаётся через CUBA remote service. Счётчики меняются только после
 * завершения аналитического чанка, поэтому экран может показывать реальное,
 * а не таймерное продвижение операции.</p>
 */
public class CandidateVacancyMatchProgress implements Serializable {
    private static final long serialVersionUID = 238746192837461923L;

    private UUID operationId;
    private String phase;
    private String statusMessage;
    private int totalVacancies;
    private int processedVacancies;
    private int totalChunks;
    private int completedChunks;
    private int failedChunks;
    private int progressPercent;
    private long elapsedMillis;
    private Long estimatedRemainingMillis;
    private boolean completed;
    private boolean success;

    public UUID getOperationId() {
        return operationId;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public int getTotalVacancies() {
        return totalVacancies;
    }

    public void setTotalVacancies(int totalVacancies) {
        this.totalVacancies = totalVacancies;
    }

    public int getProcessedVacancies() {
        return processedVacancies;
    }

    public void setProcessedVacancies(int processedVacancies) {
        this.processedVacancies = processedVacancies;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public int getCompletedChunks() {
        return completedChunks;
    }

    public void setCompletedChunks(int completedChunks) {
        this.completedChunks = completedChunks;
    }

    public int getFailedChunks() {
        return failedChunks;
    }

    public void setFailedChunks(int failedChunks) {
        this.failedChunks = failedChunks;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    public Long getEstimatedRemainingMillis() {
        return estimatedRemainingMillis;
    }

    public void setEstimatedRemainingMillis(Long estimatedRemainingMillis) {
        this.estimatedRemainingMillis = estimatedRemainingMillis;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
