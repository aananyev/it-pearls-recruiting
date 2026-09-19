package com.company.hunttech.service.dto;

import com.company.hunttech.entity.CandidateSkillPriority;
import com.company.hunttech.service.AiExecutionResult;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Структурированный результат анализа навыков кандидата с детализацией изменений (дельта).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidateSkillsScanResult implements Serializable {
    private static final long serialVersionUID = 1928472910293847561L;

    private boolean success;
    private String rawError;
    private long durationMs;
    private int totalDetected;
    @JsonIgnore
    private transient AiExecutionResult aiExecution;

    private List<SkillDeltaItem> addedSkills = new ArrayList<>();
    private List<SkillDeltaItem> updatedSkills = new ArrayList<>();
    private List<SkillDeltaItem> unchangedSkills = new ArrayList<>();
    private List<SkillDeltaItem> skippedSkills = new ArrayList<>();

    public static class SkillDeltaItem implements Serializable {
        private static final long serialVersionUID = 819284729102938475L;

        private String skillName;
        private CandidateSkillPriority priority;
        private CandidateSkillPriority oldPriority;
        private String reason;

        public SkillDeltaItem() {
        }

        public SkillDeltaItem(String skillName, CandidateSkillPriority priority) {
            this.skillName = skillName;
            this.priority = priority;
        }

        public SkillDeltaItem(String skillName, CandidateSkillPriority priority, CandidateSkillPriority oldPriority, String reason) {
            this.skillName = skillName;
            this.priority = priority;
            this.oldPriority = oldPriority;
            this.reason = reason;
        }

        public String getSkillName() {
            return skillName;
        }

        public void setSkillName(String skillName) {
            this.skillName = skillName;
        }

        public CandidateSkillPriority getPriority() {
            return priority;
        }

        public void setPriority(CandidateSkillPriority priority) {
            this.priority = priority;
        }

        public CandidateSkillPriority getOldPriority() {
            return oldPriority;
        }

        public void setOldPriority(CandidateSkillPriority oldPriority) {
            this.oldPriority = oldPriority;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getRawError() {
        return rawError;
    }

    public void setRawError(String rawError) {
        this.rawError = rawError;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getTotalDetected() {
        return totalDetected;
    }

    public void setTotalDetected(int totalDetected) {
        this.totalDetected = totalDetected;
    }

    @JsonIgnore
    public AiExecutionResult getAiExecution() {
        return aiExecution;
    }

    @JsonIgnore
    public void setAiExecution(AiExecutionResult aiExecution) {
        this.aiExecution = aiExecution;
    }

    public List<SkillDeltaItem> getAddedSkills() {
        return addedSkills;
    }

    public void setAddedSkills(List<SkillDeltaItem> addedSkills) {
        this.addedSkills = addedSkills != null ? addedSkills : new ArrayList<>();
    }

    public List<SkillDeltaItem> getUpdatedSkills() {
        return updatedSkills;
    }

    public void setUpdatedSkills(List<SkillDeltaItem> updatedSkills) {
        this.updatedSkills = updatedSkills != null ? updatedSkills : new ArrayList<>();
    }

    public List<SkillDeltaItem> getUnchangedSkills() {
        return unchangedSkills;
    }

    public void setUnchangedSkills(List<SkillDeltaItem> unchangedSkills) {
        this.unchangedSkills = unchangedSkills != null ? unchangedSkills : new ArrayList<>();
    }

    public List<SkillDeltaItem> getSkippedSkills() {
        return skippedSkills;
    }

    public void setSkippedSkills(List<SkillDeltaItem> skippedSkills) {
        this.skippedSkills = skippedSkills != null ? skippedSkills : new ArrayList<>();
    }
}
