package com.company.hunttech.service;

import java.io.Serializable;

/**
 * DTO-модель распарсенных официальных реквизитов компании и генерального директора.
 */
public class CompanyRequisitesParsedData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String companyName;
    private String companyShortName;
    private String ownership;
    private String inn;
    private String kpp;
    private String ogrn;
    private String okpo;
    private String oktmo;
    private String okved;
    private String legalAddress;
    private String actualAddress;
    private String postalAddress;
    private String bik;
    private String bankName;
    private String settlementAccount;
    private String correspondentAccount;
    private String phone;
    private String email;
    private String website;

    // Данные генерального директора
    private String directorLastName;
    private String directorFirstName;
    private String directorMiddleName;
    private String directorPosition;
    private String directorPhone;
    private String directorEmail;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyShortName() {
        return companyShortName;
    }

    public void setCompanyShortName(String companyShortName) {
        this.companyShortName = companyShortName;
    }

    public String getOwnership() {
        return ownership;
    }

    public void setOwnership(String ownership) {
        this.ownership = ownership;
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public String getKpp() {
        return kpp;
    }

    public void setKpp(String kpp) {
        this.kpp = kpp;
    }

    public String getOgrn() {
        return ogrn;
    }

    public void setOgrn(String ogrn) {
        this.ogrn = ogrn;
    }

    public String getOkpo() {
        return okpo;
    }

    public void setOkpo(String okpo) {
        this.okpo = okpo;
    }

    public String getOktmo() {
        return oktmo;
    }

    public void setOktmo(String oktmo) {
        this.oktmo = oktmo;
    }

    public String getOkved() {
        return okved;
    }

    public void setOkved(String okved) {
        this.okved = okved;
    }

    public String getLegalAddress() {
        return legalAddress;
    }

    public void setLegalAddress(String legalAddress) {
        this.legalAddress = legalAddress;
    }

    public String getActualAddress() {
        return actualAddress;
    }

    public void setActualAddress(String actualAddress) {
        this.actualAddress = actualAddress;
    }

    public String getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(String postalAddress) {
        this.postalAddress = postalAddress;
    }

    public String getBik() {
        return bik;
    }

    public void setBik(String bik) {
        this.bik = bik;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getSettlementAccount() {
        return settlementAccount;
    }

    public void setSettlementAccount(String settlementAccount) {
        this.settlementAccount = settlementAccount;
    }

    public String getCorrespondentAccount() {
        return correspondentAccount;
    }

    public void setCorrespondentAccount(String correspondentAccount) {
        this.correspondentAccount = correspondentAccount;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDirectorLastName() {
        return directorLastName;
    }

    public void setDirectorLastName(String directorLastName) {
        this.directorLastName = directorLastName;
    }

    public String getDirectorFirstName() {
        return directorFirstName;
    }

    public void setDirectorFirstName(String directorFirstName) {
        this.directorFirstName = directorFirstName;
    }

    public String getDirectorMiddleName() {
        return directorMiddleName;
    }

    public void setDirectorMiddleName(String directorMiddleName) {
        this.directorMiddleName = directorMiddleName;
    }

    public String getDirectorPosition() {
        return directorPosition;
    }

    public void setDirectorPosition(String directorPosition) {
        this.directorPosition = directorPosition;
    }

    public String getDirectorPhone() {
        return directorPhone;
    }

    public void setDirectorPhone(String directorPhone) {
        this.directorPhone = directorPhone;
    }

    public String getDirectorEmail() {
        return directorEmail;
    }

    public void setDirectorEmail(String directorEmail) {
        this.directorEmail = directorEmail;
    }

    public String getDirectorFullName() {
        StringBuilder sb = new StringBuilder();
        if (directorLastName != null && !directorLastName.trim().isEmpty()) {
            sb.append(directorLastName.trim());
        }
        if (directorFirstName != null && !directorFirstName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(directorFirstName.trim());
        }
        if (directorMiddleName != null && !directorMiddleName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(directorMiddleName.trim());
        }
        return sb.toString();
    }
}
