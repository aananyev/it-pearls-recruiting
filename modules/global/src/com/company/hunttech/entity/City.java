package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@NamePattern("%s|cityRuName")
@Table(name = "HUNTTECH_CITY", indexes = {
        @Index(name = "IDX_HUNTTECH_CITY_RU_NAME", columnList = "CITY_RU_NAME")
})
@Entity(name = "hunttech_City")
public class City extends StandardEntity {
    private static final long serialVersionUID = -8881735664013253888L;

    @NotNull
    @Column(name = "CITY_RU_NAME", nullable = false, unique = true, length = 50)
    protected String cityRuName;

    @Column(name = "CITY_ENG_NAME", length = 100)
    protected String cityEngName;

    @Column(name = "CITY_PHONE_CODE", length = 10)
    protected String cityPhoneCode;

    @Column(name = "POSTAL_CODE", length = 20)
    protected String postalCode;

    @Column(name = "FIAS_ID", length = 50)
    protected String fiasId;

    @Column(name = "POPULATION")
    protected Long population;

    @Column(name = "LATITUDE")
    protected Double latitude;

    @Column(name = "LONGITUDE")
    protected Double longitude;

    @Column(name = "TIME_ZONE", length = 50)
    protected String timeZone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_CITY_EMBLEM_ID")
    protected FileDescriptor fileCityEmblem;

    @Lob
    @Column(name = "EMBLEM_IMAGE")
    protected byte[] emblemImage;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_REGION_ID")
    protected Region cityRegion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    protected OpenPosition openPosition;

    public String getCityEngName() {
        return cityEngName;
    }

    public void setCityEngName(String cityEngName) {
        this.cityEngName = cityEngName;
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

    public FileDescriptor getFileCityEmblem() {
        return fileCityEmblem;
    }

    public void setFileCityEmblem(FileDescriptor fileCityEmblem) {
        this.fileCityEmblem = fileCityEmblem;
    }

    public byte[] getEmblemImage() {
        return emblemImage;
    }

    public void setEmblemImage(byte[] emblemImage) {
        this.emblemImage = emblemImage;
    }

    public Region getCityRegion() {
        return cityRegion;
    }

    public void setCityRegion(Region cityRegion) {
        this.cityRegion = cityRegion;
    }

    public String getCityPhoneCode() {
        return cityPhoneCode;
    }

    public void setCityPhoneCode(String cityPhoneCode) {
        this.cityPhoneCode = cityPhoneCode;
    }

    public String getCityRuName() {
        return cityRuName;
    }

    public void setCityRuName(String cityRuName) {
        this.cityRuName = cityRuName;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }
}