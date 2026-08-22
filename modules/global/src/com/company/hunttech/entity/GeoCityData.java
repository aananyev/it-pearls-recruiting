package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;

import java.io.Serializable;

@MetaClass(name = "hunttech_GeoCityData")
public class GeoCityData extends BaseUuidEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @MetaProperty
    private String cityRuName;

    @MetaProperty
    private String cityEngName;

    @MetaProperty
    private String cityPhoneCode;

    @MetaProperty
    private String postalCode;

    @MetaProperty
    private String fiasId;

    @MetaProperty
    private Long population;

    @MetaProperty
    private Double latitude;

    @MetaProperty
    private Double longitude;

    @MetaProperty
    private String timeZone;

    @MetaProperty
    private String emblemUrl;

    @MetaProperty
    private String regionName;

    @MetaProperty
    private String countryName;

    @MetaProperty
    private String rawSnippet;

    public String getCityRuName() {
        return cityRuName;
    }

    public void setCityRuName(String cityRuName) {
        this.cityRuName = cityRuName;
    }

    public String getCityEngName() {
        return cityEngName;
    }

    public void setCityEngName(String cityEngName) {
        this.cityEngName = cityEngName;
    }

    public String getCityPhoneCode() {
        return cityPhoneCode;
    }

    public void setCityPhoneCode(String cityPhoneCode) {
        this.cityPhoneCode = cityPhoneCode;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getFiasId() {
        return fiasId;
    }

    public void setFiasId(String fiasId) {
        this.fiasId = fiasId;
    }

    public Long getPopulation() {
        return population;
    }

    public void setPopulation(Long population) {
        this.population = population;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
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

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
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
