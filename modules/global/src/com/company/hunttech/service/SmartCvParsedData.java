package com.company.hunttech.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Структурированные данные резюме кандидата, извлеченные нейросетью.
 */
public class SmartCvParsedData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String lastName;
    private String firstName;
    private String middleName;
    private String birthDate;
    private String phone;
    private String mobilePhone;
    private String email;
    private String telegram;
    private String skype;
    private String whatsapp;
    private String position;
    private String city;
    private String currentCompany;
    private String salary;
    private List<String> skills = new ArrayList<>();
    private List<com.company.hunttech.service.dto.cv.SmartCvWorkExperienceDto> workExperience = new ArrayList<>();
    private List<com.company.hunttech.service.dto.cv.SmartCvEducationDto> education = new ArrayList<>();
    private List<String> missingPositions = new ArrayList<>();
    private Integer experienceYears;
    private String summary;
    private String rawText;

    public List<com.company.hunttech.service.dto.cv.SmartCvWorkExperienceDto> getWorkExperience() {
        return workExperience != null ? workExperience : Collections.emptyList();
    }

    public void setWorkExperience(List<com.company.hunttech.service.dto.cv.SmartCvWorkExperienceDto> workExperience) {
        this.workExperience = workExperience != null ? workExperience : new ArrayList<>();
    }

    public List<com.company.hunttech.service.dto.cv.SmartCvEducationDto> getEducation() {
        return education != null ? education : Collections.emptyList();
    }

    public void setEducation(List<com.company.hunttech.service.dto.cv.SmartCvEducationDto> education) {
        this.education = education != null ? education : new ArrayList<>();
    }

    public List<String> getMissingPositions() {
        return missingPositions != null ? missingPositions : Collections.emptyList();
    }

    public void setMissingPositions(List<String> missingPositions) {
        this.missingPositions = missingPositions != null ? missingPositions : new ArrayList<>();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelegram() {
        return telegram;
    }

    public void setTelegram(String telegram) {
        this.telegram = telegram;
    }

    public String getSkype() {
        return skype;
    }

    public void setSkype(String skype) {
        this.skype = skype;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public List<String> getSkills() {
        return skills == null ? Collections.emptyList() : skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (lastName != null && !lastName.trim().isEmpty()) {
            sb.append(lastName.trim());
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(firstName.trim());
        }
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName.trim());
        }
        return sb.toString();
    }
}
