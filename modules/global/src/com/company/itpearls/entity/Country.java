package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@NamePattern("%s %s|countryEnName,countryRuName")
@Table(name = "ITPEARLS_COUNTRY")
@Entity(name = "itpearls_Country")
public class Country extends StandardEntity {
    private static final long serialVersionUID = 7930626317945396969L;

    @NotNull
    @Column(name = "COUNTRY_RU_NAME", nullable = false, unique = true, length = 50)
    protected String countryRuName;

    @NotNull
    @Column(name = "COUNTRY_EN_NAME", nullable = false, unique = true, length = 50)
    protected String countryEnName;

    @Column(name = "COUNTRY_SHORT_NAME", unique = true, length = 2)
    protected String countryShortName;

    @Column(name = "PHONE_CODE", unique = true)
    protected Integer phoneCode;

    public Integer getPhoneCode() {
        return phoneCode;
    }

    public void setPhoneCode(Integer phoneCode) {
        this.phoneCode = phoneCode;
    }

    public String getCountryShortName() {
        return countryShortName;
    }

    public void setCountryShortName(String countryShortName) {
        this.countryShortName = countryShortName;
    }

    public String getCountryEnName() {
        return countryEnName;
    }

    public void setCountryEnName(String countryEnName) {
        this.countryEnName = countryEnName;
    }

    public String getCountryRuName() {
        return countryRuName;
    }

    public void setCountryRuName(String countryRuName) {
        this.countryRuName = countryRuName;
    }
}