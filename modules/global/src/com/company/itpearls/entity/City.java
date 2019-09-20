package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_CITY")
@Entity(name = "itpearls_City")
public class City extends StandardEntity {
    private static final long serialVersionUID = -8881735664013253888L;

    @NotNull
    @Column(name = "CITY_RU_NAME", nullable = false, unique = true, length = 50)
    protected String cityRuName;

    @Column(name = "CITY_EN_NAME", unique = true, length = 50)
    protected String cityEnName;

    @Column(name = "CITY_PHONE_CODE", unique = true, length = 5)
    protected String cityPhoneCode;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_REGION_ID")
    protected Region cityRegion;

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

    public String getCityEnName() {
        return cityEnName;
    }

    public void setCityEnName(String cityEnName) {
        this.cityEnName = cityEnName;
    }

    public String getCityRuName() {
        return cityRuName;
    }

    public void setCityRuName(String cityRuName) {
        this.cityRuName = cityRuName;
    }
}