package com.company.hunttech.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Саммари кандидата, извлеченное моделью при сопоставлении с вакансиями.
 */
public class CandidateAiSummary implements Serializable {
    private static final long serialVersionUID = 412893712893123891L;

    private List<String> targetRoles = new ArrayList<>();
    private List<String> keySkills = new ArrayList<>();
    private String experienceSummary;
    private List<String> explicitPreferences = new ArrayList<>();

    public List<String> getTargetRoles() {
        return targetRoles;
    }

    public void setTargetRoles(List<String> targetRoles) {
        this.targetRoles = targetRoles != null ? targetRoles : new ArrayList<>();
    }

    public List<String> getKeySkills() {
        return keySkills;
    }

    public void setKeySkills(List<String> keySkills) {
        this.keySkills = keySkills != null ? keySkills : new ArrayList<>();
    }

    public String getExperienceSummary() {
        return experienceSummary;
    }

    public void setExperienceSummary(String experienceSummary) {
        this.experienceSummary = experienceSummary;
    }

    private String candidateSalaryExpectations;

    public List<String> getExplicitPreferences() {
        return explicitPreferences;
    }

    public void setExplicitPreferences(List<String> explicitPreferences) {
        this.explicitPreferences = explicitPreferences != null ? explicitPreferences : new ArrayList<>();
    }

    public String getCandidateSalaryExpectations() {
        return candidateSalaryExpectations;
    }

    public void setCandidateSalaryExpectations(String candidateSalaryExpectations) {
        this.candidateSalaryExpectations = candidateSalaryExpectations;
    }
}
