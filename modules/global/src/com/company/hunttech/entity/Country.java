package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|countryRuName")
@Table(name = "HUNTTECH_COUNTRY", indexes = {
        @Index(name = "IDX_COUNTRY_COUNTRY_RU_NAME", columnList = "COUNTRY_RU_NAME")
})
@Entity(name = "hunttech_Country")
public class Country extends StandardEntity {
    private static final long serialVersionUID = 7930626317945396969L;

    @NotNull
    @Column(name = "COUNTRY_RU_NAME", nullable = false, unique = true, length = 50)
    protected String countryRuName;

    @Column(name = "COUNTRY_ENG_NAME", length = 100)
    protected String countryEngName;

    @Column(name = "COUNTRY_SHORT_NAME", length = 2)
    protected String countryShortName;

    @Column(name = "ALPHA3_CODE", length = 3)
    protected String alpha3Code;

    @Column(name = "NUMERIC_CODE", length = 3)
    protected String numericCode;

    @Column(name = "CURRENCY_CODE", length = 3)
    protected String currencyCode;

    @Column(name = "CAPITAL", length = 100)
    protected String capital;

    @Column(name = "PHONE_CODE")
    protected Integer phoneCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_FLAG_ID")
    protected FileDescriptor fileFlag;

    @Lob
    @Column(name = "FLAG_IMAGE")
    protected byte[] flagImage;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "regionCountry")
    protected List<Region> countryOfRegion;

    public String getCountryEngName() {
        return countryEngName;
    }

    public void setCountryEngName(String countryEngName) {
        this.countryEngName = countryEngName;
    }

    public String getAlpha3Code() {
        return alpha3Code;
    }

    public void setAlpha3Code(String alpha3Code) {
        this.alpha3Code = alpha3Code;
    }

    public String getNumericCode() {
        return numericCode;
    }

    public void setNumericCode(String numericCode) {
        this.numericCode = numericCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCapital() {
        return capital;
    }

    public void setCapital(String capital) {
        this.capital = capital;
    }

    public FileDescriptor getFileFlag() {
        return fileFlag;
    }

    public void setFileFlag(FileDescriptor fileFlag) {
        this.fileFlag = fileFlag;
    }

    public byte[] getFlagImage() {
        return flagImage;
    }

    public void setFlagImage(byte[] flagImage) {
        this.flagImage = flagImage;
    }

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