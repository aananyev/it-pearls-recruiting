package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;

/**
 * Шаблон системного промпта для AI-анализа сущностей.
 * Управляется администратором. Хранит текст промпта с {{placeholders}},
 * которые заполняются данными сущности во время анализа.
 *
 * Поле availablePlaceholders содержит JSON-схему доступных переменных,
 * например: {"resumeText":"текст резюме","candidateName":"ФИО кандидата"}.
 * Эта схема показывается администратору в форме редактирования как подсказка.
 */
@NamePattern("%s|name")
@Table(name = "HUNTTECH_AI_PROMPT_TEMPLATE")
@Entity(name = "hunttech_AiPromptTemplate")
public class AiPromptTemplate extends StandardEntity {

    private static final long serialVersionUID = -6599259759564102272L;

    @Column(name = "NAME", nullable = false)
    protected String name;

    @Column(name = "CODE", nullable = false, unique = true)
    protected String code;

    @Column(name = "ENTITY_CLASS", nullable = false)
    protected String entityClass;

    @Lob
    @Column(name = "PROMPT_TEXT", nullable = false)
    protected String promptText;

    @Lob
    @Column(name = "AVAILABLE_PLACEHOLDERS")
    protected String availablePlaceholders;

    @Column(name = "DESCRIPTION")
    protected String description;

    @Column(name = "ACTIVE", nullable = false)
    protected Boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getAvailablePlaceholders() {
        return availablePlaceholders;
    }

    public void setAvailablePlaceholders(String availablePlaceholders) {
        this.availablePlaceholders = availablePlaceholders;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
