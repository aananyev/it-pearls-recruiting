package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "HUNTTECH_ACCOUNTING_EXPENSE_CATEGORY", indexes = {
        @Index(name = "IDX_HUNTTECH_ACC_EXPENSE_CATEGORY_CODE", columnList = "CODE", unique = true)
})
@Entity(name = "hunttech_AccountingExpenseCategory")
@NamePattern("%s|nameRu")
public class AccountingExpenseCategory extends StandardEntity {
    private static final long serialVersionUID = 8981424794591683981L;

    @NotNull
    @Column(name = "CODE", nullable = false, unique = true, length = 64)
    private String code;

    @NotNull
    @Column(name = "NAME_RU", nullable = false, length = 128)
    private String nameRu;

    @Column(name = "ACTIVE")
    private Boolean active = true;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    @Lob
    @Column(name = "MATCH_KEYWORDS")
    private String matchKeywords;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNameRu() {
        return nameRu;
    }

    public void setNameRu(String nameRu) {
        this.nameRu = nameRu;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getMatchKeywords() {
        return matchKeywords;
    }

    public void setMatchKeywords(String matchKeywords) {
        this.matchKeywords = matchKeywords;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
