package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_VACANCY_PROMPT_TEMPLATE", indexes = {
        @Index(name = "IDX_ITPEARLS_VACANCY_PROMPT_TEMPLATE_CODE", columnList = "CODE", unique = true)
})
@Entity(name = "itpearls_VacancyPromptTemplate")
@NamePattern("%s %s|code,name")
public class VacancyPromptTemplate extends StandardEntity {
    private static final long serialVersionUID = -6193847201563829471L;

    @NotNull
    @Column(name = "CODE", nullable = false, unique = true, length = 64)
    private String code;

    @NotNull
    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @Lob
    @Column(name = "PROMPT_TEXT")
    private String promptText;

    @Column(name = "SYSTEM_CONTEXT", length = 1000)
    private String systemContext;

    @Column(name = "TEMPERATURE")
    private Double temperature = 0.7;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getSystemContext() {
        return systemContext;
    }

    public void setSystemContext(String systemContext) {
        this.systemContext = systemContext;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
}
