package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;

import java.io.Serializable;

@MetaClass(name = "hunttech_GeoRegionData")
public class GeoRegionData extends BaseUuidEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @MetaProperty
    private String regionRuName;

    @MetaProperty
    private String regionEngName;

    @MetaProperty
    private Integer regionCode;

    @MetaProperty
    private String isoCode;

    @MetaProperty
    private String fiasId;

    @MetaProperty
    private String regionType;

    @MetaProperty
    private String capital;

    @MetaProperty
    private String timeZone;

    @MetaProperty
    private String emblemUrl;

    @MetaProperty
    private String countryName;

    @MetaProperty
    private String rawSnippet;

    public String getRegionRuName() {
        return regionRuName;
    }

    public void setRegionRuName(String regionRuName) {
        this.regionRuName = regionRuName;
    }

    public String getRegionEngName() {
        return regionEngName;
    }

    public void setRegionEngName(String regionEngName) {
        this.regionEngName = regionEngName;
    }

    public Integer getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(Integer regionCode) {
        this.regionCode = regionCode;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    public String getFiasId() {
        return fiasId;
    }

    public void setFiasId(String fiasId) {
        this.fiasId = fiasId;
    }

    public String getRegionType() {
        return regionType;
    }

    public void setRegionType(String regionType) {
        this.regionType = regionType;
    }

    public String getCapital() {
        return capital;
    }

    public void setCapital(String capital) {
        this.capital = capital;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getEmblemUrl() {
        return emblemUrl;
    }

    public void setEmblemUrl(String emblemUrl) {
        this.emblemUrl = emblemUrl;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getRawSnippet() {
        return rawSnippet;
    }

    public void setRawSnippet(String rawSnippet) {
        this.rawSnippet = rawSnippet;
    }
}
