package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "HUNTTECH_ACCOUNTING_COMPANY_ALIAS", indexes = {
        @Index(name = "IDX_HUNTTECH_ACC_COMPANY_ALIAS_COMPANY", columnList = "COMPANY_ID"),
        @Index(name = "IDX_HUNTTECH_ACC_COMPANY_ALIAS_ALIAS", columnList = "ALIAS")
})
@Entity(name = "hunttech_AccountingCompanyAlias")
@NamePattern("%s -> %s|alias,company")
public class AccountingCompanyAlias extends StandardEntity {
    private static final long serialVersionUID = -7962851521976888089L;

    @NotNull
    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMPANY_ID")
    private Company company;

    @NotNull
    @Column(name = "ALIAS", nullable = false, length = 255)
    private String alias;

    @Column(name = "INN", length = 16)
    private String inn;

    @Column(name = "YANDEX_DISK_FOLDER_PATH", length = 1000)
    private String yandexDiskFolderPath;

    @Column(name = "ACTIVE")
    private Boolean active = true;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public String getYandexDiskFolderPath() {
        return yandexDiskFolderPath;
    }

    public void setYandexDiskFolderPath(String yandexDiskFolderPath) {
        this.yandexDiskFolderPath = yandexDiskFolderPath;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
