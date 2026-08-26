package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;

import java.io.Serializable;

@MetaClass(name = "hunttech_GeoCountryData")
public class GeoCountryData extends BaseUuidEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @MetaProperty
    private String countryRuName;

    @MetaProperty
    private String countryEngName;

    @MetaProperty
    private String countryShortName; // ISO-2: RU, US, CN, BY...

    @MetaProperty
    private String alpha3Code; // ISO-3: RUS, USA, CHN, BLR...

    @MetaProperty
    private String numericCode; // 643, 840...

    @MetaProperty
    private Integer phoneCode; // 7, 1, 375...

    @MetaProperty
    private String currencyCode; // RUB, USD, EUR, CNY...

    @MetaProperty
    private String capital; // Москва, Вашингтон, Пекин, Минск...

    @MetaProperty
    private String flagUrl; // SVG or PNG URL

    @MetaProperty
    private String rawSnippet;

    public String getCountryRuName() {
        return countryRuName;
    }

    public void setCountryRuName(String countryRuName) {
        this.countryRuName = countryRuName;
    }

    public String getCountryEngName() {
        return countryEngName;
    }

    public void setCountryEngName(String countryEngName) {
        this.countryEngName = countryEngName;
    }

    public String getCountryShortName() {
        return countryShortName;
    }

    public void setCountryShortName(String countryShortName) {
        this.countryShortName = countryShortName;
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

    public Integer getPhoneCode() {
        return phoneCode;
    }

    public void setPhoneCode(Integer phoneCode) {
        this.phoneCode = phoneCode;
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

    public String getFlagUrl() {
        return flagUrl;
    }

    public void setFlagUrl(String flagUrl) {
        this.flagUrl = flagUrl;
    }

    public String getRawSnippet() {
        return rawSnippet;
    }

    public void setRawSnippet(String rawSnippet) {
        this.rawSnippet = rawSnippet;
    }
}
