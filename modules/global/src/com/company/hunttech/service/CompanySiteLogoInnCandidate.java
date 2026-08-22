package com.company.hunttech.service;

import java.io.Serializable;

/**
 * DTO-модель кандидата организации при ИИ-поиске пар (Официальный сайт + Логотип + ИНН).
 */
public class CompanySiteLogoInnCandidate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String companyName;
    private String legalEntityName;
    private String inn;
    private String ogrn;
    private String website;
    private String logoUrl;
    private String country;
    private String city;
    private String description;
    private String source;

    public CompanySiteLogoInnCandidate() {
    }

    public CompanySiteLogoInnCandidate(String companyName, String inn, String website, String logoUrl) {
        this.companyName = companyName;
        this.inn = inn;
        this.website = website;
        this.logoUrl = logoUrl;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public void setLegalEntityName(String legalEntityName) {
        this.legalEntityName = legalEntityName;
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public String getOgrn() {
        return ogrn;
    }

    public void setOgrn(String ogrn) {
        this.ogrn = ogrn;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "CompanySiteLogoInnCandidate{" +
                "companyName='" + companyName + '\'' +
                ", legalEntityName='" + legalEntityName + '\'' +
                ", inn='" + inn + '\'' +
                ", ogrn='" + ogrn + '\'' +
                ", website='" + website + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", description='" + description + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
