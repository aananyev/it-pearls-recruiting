package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|regionRuName")
@Table(name = "HUNTTECH_REGION", indexes = {
        @Index(name = "IDX_HUNTTECH_REGION", columnList = "ID")
})
@Entity(name = "hunttech_Region")
public class Region extends StandardEntity {
    private static final long serialVersionUID = 6717889040534438099L;

    @NotNull
    @Column(name = "REGION_RU_NAME", nullable = false, unique = true, length = 50)
    protected String regionRuName;

    @Column(name = "REGION_ENG_NAME", length = 100)
    protected String regionEngName;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REGION_COUNTRY_ID")
    protected Country regionCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_REGION_EMBLEM_ID")
    protected FileDescriptor fileRegionEmblem;

    @Lob
    @Column(name = "EMBLEM_IMAGE")
    protected byte[] emblemImage;

    @Composition
    @OneToMany(mappedBy = "cityRegion")
    protected List<City> regionOfCity;

    @Column(name = "REGION_CODE", unique = true)
    protected Integer regionCode;

    @Column(name = "ISO_CODE", length = 10)
    protected String isoCode;

    @Column(name = "FIAS_ID", length = 50)
    protected String fiasId;

    @Column(name = "REGION_TYPE", length = 50)
    protected String regionType;

    @Column(name = "CAPITAL", length = 100)
    protected String capital;

    @Column(name = "TIME_ZONE", length = 50)
    protected String timeZone;

    public String getRegionEngName() {
        return regionEngName;
    }

    public void setRegionEngName(String regionEngName) {
        this.regionEngName = regionEngName;
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

    public FileDescriptor getFileRegionEmblem() {
        return fileRegionEmblem;
    }

    public void setFileRegionEmblem(FileDescriptor fileRegionEmblem) {
        this.fileRegionEmblem = fileRegionEmblem;
    }

    public byte[] getEmblemImage() {
        return emblemImage;
    }

    public void setEmblemImage(byte[] emblemImage) {
        this.emblemImage = emblemImage;
    }

    public List<City> getRegionOfCity() {
        return regionOfCity;
    }

    public void setRegionOfCity(List<City> regionOfCity) {
        this.regionOfCity = regionOfCity;
    }

    public Country getRegionCountry() {
        return regionCountry;
    }

    public void setRegionCountry(Country regionCountry) {
        this.regionCountry = regionCountry;
    }

    public Integer getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(Integer regionCode) {
        this.regionCode = regionCode;
    }

    public String getRegionRuName() {
        return regionRuName;
    }

    public void setRegionRuName(String regionRuName) {
        this.regionRuName = regionRuName;
    }
}