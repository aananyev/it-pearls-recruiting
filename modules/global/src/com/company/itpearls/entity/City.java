package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@NamePattern("%s|cityRuName")
@Table(name = "ITPEARLS_CITY", indexes = {
        @Index(name = "IDX_CITY_ID", columnList = "ID"),
        @Index(name = "IDX_CITY_CITY_RU_NAME", columnList = "CITY_RU_NAME"),
        @Index(name = "IDX_ITPEARLS_CITY_REGION_ID", columnList = "CITY_REGION_ID"),
        @Index(name = "IDX_ITPEARLS_CITY_OPEN_POSITION_ID", columnList = "OPEN_POSITION_ID")
})
@Entity(name = "itpearls_City")
public class City extends StandardEntity {
    private static final long serialVersionUID = -8881735664013253888L;

    @NotNull
    @Column(name = "CITY_RU_NAME", nullable = false, unique = true, length = 50)
    protected String cityRuName;

    @Column(name = "CITY_PHONE_CODE", unique = true, length = 5)
    protected String cityPhoneCode;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_REGION_ID")
    protected Region cityRegion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    protected OpenPosition openPosition;

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