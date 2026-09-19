package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Элемент аналитического отчёта сопоставления кандидата с вакансией.
 */
@MetaClass(name = "hunttech_CandidateVacancyMatchItem")
public class CandidateVacancyMatchItem extends BaseUuidEntity implements Serializable {
    private static final long serialVersionUID = 712398412893719283L;

    @MetaProperty
    protected UUID openPositionId;

    @MetaProperty
    protected String vacancyName;

    @MetaProperty
    protected String projectName;

    @MetaProperty
    protected String positionName;

    @MetaProperty
    protected Integer score;

    @MetaProperty
    protected String verdict;

    @MetaProperty
    protected Integer roleFit;

    @MetaProperty
    protected Integer skillsFit;

    @MetaProperty
    protected Integer experienceFit;

    @MetaProperty
    protected Integer preferencesFit;

    @MetaProperty
    protected Integer domainFit;

    @MetaProperty
    protected List<String> matchedSkills = new ArrayList<>();

    @MetaProperty
    protected List<String> missingCriticalRequirements = new ArrayList<>();

    @MetaProperty
    protected List<String> risks = new ArrayList<>();

    @MetaProperty
    protected List<String> reasonsToOffer = new ArrayList<>();

    @MetaProperty
    protected List<String> candidateEvidence = new ArrayList<>();

    @MetaProperty
    protected List<String> vacancyEvidence = new ArrayList<>();

    @MetaProperty
    protected String summary;

    @MetaProperty
    protected Integer priority;

    @MetaProperty
    protected OpenPosition openPosition;

    public UUID getOpenPositionId() {
        return openPositionId;
    }

    public void setOpenPositionId(UUID openPositionId) {
        this.openPositionId = openPositionId;
    }

    public String getVacancyName() {
        return vacancyName;
    }

    public void setVacancyName(String vacancyName) {
        this.vacancyName = vacancyName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public Integer getRoleFit() {
        return roleFit;
    }

    public void setRoleFit(Integer roleFit) {
        this.roleFit = roleFit;
    }

    public Integer getSkillsFit() {
        return skillsFit;
    }

    public void setSkillsFit(Integer skillsFit) {
        this.skillsFit = skillsFit;
    }

    public Integer getExperienceFit() {
        return experienceFit;
    }

    public void setExperienceFit(Integer experienceFit) {
        this.experienceFit = experienceFit;
    }

    public Integer getPreferencesFit() {
        return preferencesFit;
    }

    public void setPreferencesFit(Integer preferencesFit) {
        this.preferencesFit = preferencesFit;
    }

    public Integer getDomainFit() {
        return domainFit;
    }

    public void setDomainFit(Integer domainFit) {
        this.domainFit = domainFit;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills != null ? matchedSkills : new ArrayList<>();
    }

    public List<String> getMissingCriticalRequirements() {
        return missingCriticalRequirements;
    }

    public void setMissingCriticalRequirements(List<String> missingCriticalRequirements) {
        this.missingCriticalRequirements = missingCriticalRequirements != null ? missingCriticalRequirements : new ArrayList<>();
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks != null ? risks : new ArrayList<>();
    }

    public List<String> getReasonsToOffer() {
        return reasonsToOffer;
    }

    public void setReasonsToOffer(List<String> reasonsToOffer) {
        this.reasonsToOffer = reasonsToOffer != null ? reasonsToOffer : new ArrayList<>();
    }

    public List<String> getCandidateEvidence() {
        return candidateEvidence;
    }

    public void setCandidateEvidence(List<String> candidateEvidence) {
        this.candidateEvidence = candidateEvidence != null ? candidateEvidence : new ArrayList<>();
    }

    public List<String> getVacancyEvidence() {
        return vacancyEvidence;
    }

    public void setVacancyEvidence(List<String> vacancyEvidence) {
        this.vacancyEvidence = vacancyEvidence != null ? vacancyEvidence : new ArrayList<>();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    /**
     * Возвращает форматированный список совпадающих навыков в виде одной строки.
     */
    @MetaProperty
    public String getMatchedSkillsDisplay() {
        if (matchedSkills == null || matchedSkills.isEmpty()) {
            return "—";
        }
        return String.join(", ", matchedSkills);
    }

    /**
     * Возвращает форматированный список критических пробелов в виде одной строки.
     */
    @MetaProperty
    public String getMissingCriticalRequirementsDisplay() {
        if (missingCriticalRequirements == null || missingCriticalRequirements.isEmpty()) {
            return "Нет явных пробелов";
        }
        return String.join(", ", missingCriticalRequirements);
    }
}
