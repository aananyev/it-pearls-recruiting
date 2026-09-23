package com.company.hunttech.dto.integration;

import java.io.Serializable;

/**
 * Фильтр для постраничного чтения и поиска по справочникам (BL-2026-029).
 */
public class ReferenceFilterDto implements Serializable {
    private static final long serialVersionUID = 8273645192837465L;

    private String search;
    private Integer limit = 50;
    private Integer offset = 0;
    private String parentId;
    private String correlationId;

    public ReferenceFilterDto() {
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
