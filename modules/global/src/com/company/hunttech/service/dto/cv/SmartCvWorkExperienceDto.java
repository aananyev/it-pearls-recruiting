package com.company.hunttech.service.dto.cv;

import java.io.Serializable;

/**
 * DTO отдельного места работы из резюме кандидата.
 */
public class SmartCvWorkExperienceDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String companyName;
    private String companyDescription;
    private String companyWebsite;
    private String positionName;
    private String startDate; // Формат "YYYY-MM" или "YYYY-MM-DD"
    private String endDate;   // Формат "YYYY-MM" или "YYYY-MM-DD" (null/пусто для текущего)
    private Boolean isCurrent;
    private String city;
    private String duties;
    private String achievements;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }

    public String getCompanyWebsite() {
        return companyWebsite;
    }

    public void setCompanyWebsite(String companyWebsite) {
        this.companyWebsite = companyWebsite;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean current) {
        isCurrent = current;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDuties() {
        return duties;
    }

    public void setDuties(String duties) {
        this.duties = duties;
    }

    public String getAchievements() {
        return achievements;
    }

    public void setAchievements(String achievements) {
        this.achievements = achievements;
    }

    /**
     * Формирует объединенный текст обязанностей и достижений.
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        if (duties != null && !duties.trim().isEmpty()) {
            sb.append(duties.trim());
        }
        if (achievements != null && !achievements.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n\nДостижения:\n");
            }
            sb.append(achievements.trim());
        }
        return sb.toString();
    }
}
