package com.company.hunttech.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO агрегированных KPI метрик для экрана мониторинга фонового определения контактов и фото.
 */
public class CandidateContactsEnrichmentKpiDto implements Serializable {
    private static final long serialVersionUID = 8192847561029384751L;

    private boolean workerEnabled;
    private String workerStatus; // РАБОТАЕТ, ОЖИДАНИЕ, ОСТАНОВЛЕН, ОШИБКА
    private String workerStatusDetail;

    // Счетчики базы резюме
    private long totalEligibleCvCount;
    private long freshCount;
    private long notAnalyzedCount;
    private long staleCount;
    private long retryCount;
    private long errorCount;
    private long skippedCount;
    private long processingCount;

    // Временные срезы
    private long processedToday;
    private long processed24h;
    private long processed7d;

    // Метрики контактов и фото
    private long totalContactsExtractedToday;
    private long totalPhotosExtractedToday;
    private long totalContactsExtractedAllTime;
    private long totalPhotosExtractedAllTime;

    // Телеметрия AI вызовов
    private long aiRequestsToday;
    private long promptTokensToday;
    private long completionTokensToday;
    private long totalTokensToday;
    private BigDecimal estimatedCostToday;
    private BigDecimal estimatedCostTotal;
    private boolean paidRequestDetected;

    // Скорость и ETA
    private double processingSpeedPerHour;
    private String estimatedEtaText;

    // Сводка по провайдерам / моделям
    private List<ProviderModelStatDto> providerModelStats = new ArrayList<>();

    public static class ProviderModelStatDto implements Serializable {
        private static final long serialVersionUID = 628192847561029384L;

        private String provider;
        private String model;
        private long totalRequests;
        private long successRequests;
        private long errorRequests;
        private long totalTokens;
        private BigDecimal estimatedCost;

        public ProviderModelStatDto() {
        }

        public ProviderModelStatDto(String provider, String model, long totalRequests, long successRequests, long errorRequests, long totalTokens, BigDecimal estimatedCost) {
            this.provider = provider;
            this.model = model;
            this.totalRequests = totalRequests;
            this.successRequests = successRequests;
            this.errorRequests = errorRequests;
            this.totalTokens = totalTokens;
            this.estimatedCost = estimatedCost;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public long getTotalRequests() {
            return totalRequests;
        }

        public void setTotalRequests(long totalRequests) {
            this.totalRequests = totalRequests;
        }

        public long getSuccessRequests() {
            return successRequests;
        }

        public void setSuccessRequests(long successRequests) {
            this.successRequests = successRequests;
        }

        public long getErrorRequests() {
            return errorRequests;
        }

        public void setErrorRequests(long errorRequests) {
            this.errorRequests = errorRequests;
        }

        public long getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(long totalTokens) {
            this.totalTokens = totalTokens;
        }

        public BigDecimal getEstimatedCost() {
            return estimatedCost;
        }

        public void setEstimatedCost(BigDecimal estimatedCost) {
            this.estimatedCost = estimatedCost;
        }
    }

    public boolean isWorkerEnabled() {
        return workerEnabled;
    }

    public void setWorkerEnabled(boolean workerEnabled) {
        this.workerEnabled = workerEnabled;
    }

    public String getWorkerStatus() {
        return workerStatus;
    }

    public void setWorkerStatus(String workerStatus) {
        this.workerStatus = workerStatus;
    }

    public String getWorkerStatusDetail() {
        return workerStatusDetail;
    }

    public void setWorkerStatusDetail(String workerStatusDetail) {
        this.workerStatusDetail = workerStatusDetail;
    }

    public long getTotalEligibleCvCount() {
        return totalEligibleCvCount;
    }

    public void setTotalEligibleCvCount(long totalEligibleCvCount) {
        this.totalEligibleCvCount = totalEligibleCvCount;
    }

    public long getFreshCount() {
        return freshCount;
    }

    public void setFreshCount(long freshCount) {
        this.freshCount = freshCount;
    }

    public long getNotAnalyzedCount() {
        return notAnalyzedCount;
    }

    public void setNotAnalyzedCount(long notAnalyzedCount) {
        this.notAnalyzedCount = notAnalyzedCount;
    }

    public long getStaleCount() {
        return staleCount;
    }

    public void setStaleCount(long staleCount) {
        this.staleCount = staleCount;
    }

    public long getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(long retryCount) {
        this.retryCount = retryCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(long skippedCount) {
        this.skippedCount = skippedCount;
    }

    public long getProcessingCount() {
        return processingCount;
    }

    public void setProcessingCount(long processingCount) {
        this.processingCount = processingCount;
    }

    public long getProcessedToday() {
        return processedToday;
    }

    public void setProcessedToday(long processedToday) {
        this.processedToday = processedToday;
    }

    public long getProcessed24h() {
        return processed24h;
    }

    public void setProcessed24h(long processed24h) {
        this.processed24h = processed24h;
    }

    public long getProcessed7d() {
        return processed7d;
    }

    public void setProcessed7d(long processed7d) {
        this.processed7d = processed7d;
    }

    public long getTotalContactsExtractedToday() {
        return totalContactsExtractedToday;
    }

    public void setTotalContactsExtractedToday(long totalContactsExtractedToday) {
        this.totalContactsExtractedToday = totalContactsExtractedToday;
    }

    public long getTotalPhotosExtractedToday() {
        return totalPhotosExtractedToday;
    }

    public void setTotalPhotosExtractedToday(long totalPhotosExtractedToday) {
        this.totalPhotosExtractedToday = totalPhotosExtractedToday;
    }

    public long getTotalContactsExtractedAllTime() {
        return totalContactsExtractedAllTime;
    }

    public void setTotalContactsExtractedAllTime(long totalContactsExtractedAllTime) {
        this.totalContactsExtractedAllTime = totalContactsExtractedAllTime;
    }

    public long getTotalPhotosExtractedAllTime() {
        return totalPhotosExtractedAllTime;
    }

    public void setTotalPhotosExtractedAllTime(long totalPhotosExtractedAllTime) {
        this.totalPhotosExtractedAllTime = totalPhotosExtractedAllTime;
    }

    public long getAiRequestsToday() {
        return aiRequestsToday;
    }

    public void setAiRequestsToday(long aiRequestsToday) {
        this.aiRequestsToday = aiRequestsToday;
    }

    public long getPromptTokensToday() {
        return promptTokensToday;
    }

    public void setPromptTokensToday(long promptTokensToday) {
        this.promptTokensToday = promptTokensToday;
    }

    public long getCompletionTokensToday() {
        return completionTokensToday;
    }

    public void setCompletionTokensToday(long completionTokensToday) {
        this.completionTokensToday = completionTokensToday;
    }

    public long getTotalTokensToday() {
        return totalTokensToday;
    }

    public void setTotalTokensToday(long totalTokensToday) {
        this.totalTokensToday = totalTokensToday;
    }

    public BigDecimal getEstimatedCostToday() {
        return estimatedCostToday;
    }

    public void setEstimatedCostToday(BigDecimal estimatedCostToday) {
        this.estimatedCostToday = estimatedCostToday;
    }

    public BigDecimal getEstimatedCostTotal() {
        return estimatedCostTotal;
    }

    public void setEstimatedCostTotal(BigDecimal estimatedCostTotal) {
        this.estimatedCostTotal = estimatedCostTotal;
    }

    public boolean isPaidRequestDetected() {
        return paidRequestDetected;
    }

    public void setPaidRequestDetected(boolean paidRequestDetected) {
        this.paidRequestDetected = paidRequestDetected;
    }

    public double getProcessingSpeedPerHour() {
        return processingSpeedPerHour;
    }

    public void setProcessingSpeedPerHour(double processingSpeedPerHour) {
        this.processingSpeedPerHour = processingSpeedPerHour;
    }

    public String getEstimatedEtaText() {
        return estimatedEtaText;
    }

    public void setEstimatedEtaText(String estimatedEtaText) {
        this.estimatedEtaText = estimatedEtaText;
    }

    public List<ProviderModelStatDto> getProviderModelStats() {
        return providerModelStats;
    }

    public void setProviderModelStats(List<ProviderModelStatDto> providerModelStats) {
        this.providerModelStats = providerModelStats;
    }
}
