package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|countryRuName")
@Table(name = "ITPEARLS_COUNTRY")
@Entity(name = "itpearls_Country")
public class Country extends StandardEntity {
    private static final long serialVersionUID = 7930626317945396969L;

    @NotNull
    @Column(name = "COUNTRY_RU_NAME", nullable = false, unique = true, length = 50)
    protected String countryRuName;

    @Column(name = "COUNTRY_SHORT_NAME", unique = false, length = 2)
    protected String countryShortName;

    @Column(name = "PHONE_CODE")
    protected Integer phoneCode;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "regionCountry")
    protected List<Region> countryOfRegion;

    public List<Region> getCountryOfRegion() {
        return countryOfRegion;
    }

    public void setCountryOfRegion(List<Region> countryOfRegion) {
        this.countryOfRegion = countryOfRegion;
    }

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

    public String getCountryRuName() {
        return countryRuName;
    }

    public void setCountryRuName(String countryRuName) {
        this.countryRuName = countryRuName;
    }
}