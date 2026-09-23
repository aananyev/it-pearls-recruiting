package com.company.hunttech.dto.integration;

import java.io.Serializable;

/**
 * Унифицированное представление элемента справочника для внешних систем.
 */
public class ReferenceItemDto implements Serializable {
    private static final long serialVersionUID = 1928374619283741L;

    private String id;
    private String code;
    private String name;
    private String parentId;
    private Boolean active;
    private String description;

    public ReferenceItemDto() {
    }

    public ReferenceItemDto(String id, String code, String name, String parentId, Boolean active, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.parentId = parentId;
        this.active = active;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
