package com.company.hunttech.service.dto.cv;

import java.io.Serializable;

/**
 * DTO отдельной записи об образовании из резюме кандидата.
 */
public class SmartCvEducationDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String institution;
    private String faculty;
    private String specialty;
    private Integer graduationYear;
    private String degree;

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    /**
     * Формирует читабельную строку описания образования.
     */
    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        if (institution != null && !institution.trim().isEmpty()) {
            sb.append(institution.trim());
        }
        if (specialty != null && !specialty.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(specialty.trim());
        }
        if (degree != null && !degree.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" (").append(degree.trim()).append(")");
            } else {
                sb.append(degree.trim());
            }
        }
        if (graduationYear != null) {
            if (sb.length() > 0) sb.append(" — ");
            sb.append(graduationYear).append(" г.");
        }
        return sb.toString();
    }
}
