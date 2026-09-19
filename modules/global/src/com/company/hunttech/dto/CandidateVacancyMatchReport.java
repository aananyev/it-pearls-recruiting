package com.company.hunttech.dto;

import com.company.hunttech.entity.CandidateVacancyMatchItem;
import com.company.hunttech.service.AiExecutionResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Итоговый аналитический отчёт сопоставления кандидата с открытыми вакансиями.
 */
public class CandidateVacancyMatchReport implements Serializable {
    private static final long serialVersionUID = 891237192837192831L;

    private UUID candidateId;
    private String candidateFullName;
    private String candidatePosition;
    private String candidateCity;
    private String candidateCurrentCompany;

    private CandidateAiSummary candidateSummary = new CandidateAiSummary();
    private int totalVacanciesAnalyzed;
    private int matchedVacanciesCount;
    private List<CandidateVacancyMatchItem> items = new ArrayList<>();
    private String generalConclusion;

    private AiExecutionResult aiExecutionResult;
    private boolean fallbackUsed;
    private String statusMessage;
    private boolean success = true;

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateFullName() {
        return candidateFullName;
    }

    public void setCandidateFullName(String candidateFullName) {
        this.candidateFullName = candidateFullName;
    }

    public String getCandidatePosition() {
        return candidatePosition;
    }

    public void setCandidatePosition(String candidatePosition) {
        this.candidatePosition = candidatePosition;
    }

    public String getCandidateCity() {
        return candidateCity;
    }

    public void setCandidateCity(String candidateCity) {
        this.candidateCity = candidateCity;
    }

    public String getCandidateCurrentCompany() {
        return candidateCurrentCompany;
    }

    public void setCandidateCurrentCompany(String candidateCurrentCompany) {
        this.candidateCurrentCompany = candidateCurrentCompany;
    }

    public CandidateAiSummary getCandidateSummary() {
        return candidateSummary;
    }

    public void setCandidateSummary(CandidateAiSummary candidateSummary) {
        this.candidateSummary = candidateSummary != null ? candidateSummary : new CandidateAiSummary();
    }

    public int getTotalVacanciesAnalyzed() {
        return totalVacanciesAnalyzed;
    }

    public void setTotalVacanciesAnalyzed(int totalVacanciesAnalyzed) {
        this.totalVacanciesAnalyzed = totalVacanciesAnalyzed;
    }

    public int getMatchedVacanciesCount() {
        return matchedVacanciesCount;
    }

    public void setMatchedVacanciesCount(int matchedVacanciesCount) {
        this.matchedVacanciesCount = matchedVacanciesCount;
    }

    public List<CandidateVacancyMatchItem> getItems() {
        return items;
    }

    public void setItems(List<CandidateVacancyMatchItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getGeneralConclusion() {
        return generalConclusion;
    }

    public void setGeneralConclusion(String generalConclusion) {
        this.generalConclusion = generalConclusion;
    }

    public AiExecutionResult getAiExecutionResult() {
        return aiExecutionResult;
    }

    public void setAiExecutionResult(AiExecutionResult aiExecutionResult) {
        this.aiExecutionResult = aiExecutionResult;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
