package com.company.hunttech.service.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class VacancyMatchMonitoringDto implements Serializable {
    private static final long serialVersionUID = 718293718923719L;

    private int totalRuns;
    private int candidatesReviewed;
    private int takenIntoWorkCount;
    private int rejectedCount;
    private int postponedCount;
    private int outreachDraftsCount;
    private double acceptanceRate;

    // Распределение решений по диапазонам баллов AI: "80-100", "60-79", "<60"
    private Map<String, int[]> scoreBandBreakdown = new LinkedHashMap<>(); // key -> [total, taken, rejected, postponed]

    public int getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(int totalRuns) {
        this.totalRuns = totalRuns;
    }

    public int getCandidatesReviewed() {
        return candidatesReviewed;
    }

    public void setCandidatesReviewed(int candidatesReviewed) {
        this.candidatesReviewed = candidatesReviewed;
    }

    public int getTakenIntoWorkCount() {
        return takenIntoWorkCount;
    }

    public void setTakenIntoWorkCount(int takenIntoWorkCount) {
        this.takenIntoWorkCount = takenIntoWorkCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public int getPostponedCount() {
        return postponedCount;
    }

    public void setPostponedCount(int postponedCount) {
        this.postponedCount = postponedCount;
    }

    public int getOutreachDraftsCount() {
        return outreachDraftsCount;
    }

    public void setOutreachDraftsCount(int outreachDraftsCount) {
        this.outreachDraftsCount = outreachDraftsCount;
    }

    public double getAcceptanceRate() {
        return acceptanceRate;
    }

    public void setAcceptanceRate(double acceptanceRate) {
        this.acceptanceRate = acceptanceRate;
    }

    public Map<String, int[]> getScoreBandBreakdown() {
        return scoreBandBreakdown;
    }

    public void setScoreBandBreakdown(Map<String, int[]> scoreBandBreakdown) {
        this.scoreBandBreakdown = scoreBandBreakdown;
    }
}
