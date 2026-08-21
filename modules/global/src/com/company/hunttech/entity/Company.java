package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s %s|comanyName,companyShortName")
@Table(name = "HUNTTECH_COMPANY", indexes = {
        @Index(name = "IDX_COMPANY_NAME", columnList = "COMANY_NAME"),
        @Index(name = "IDX_HUNTTECH_COMPANY_SHORT_NAME", columnList = "COMPANY_SHORT_NAME")
})
@Entity(name = "hunttech_Company")
public class Company extends StandardEntity {
    private static final long serialVersionUID = 7912366724901851184L;

    @Column(name = "OUR_CLIENT")
    protected Boolean ourClient;

    @Column(name = "OUR_LEGAL_ENTITY")
    private Boolean ourLegalEntity;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_OWNERSHIP_ID")
    protected Ownershup companyOwnership;

    @NotNull
    @Column(name = "COMANY_NAME", nullable = false, length = 80)
    protected String comanyName;

    @Column(name = "COMPANY_SHORT_NAME", length = 80)
    protected String companyShortName;

    @Column(name = "LEGAL_ENTITY_NAME", length = 255)
    protected String legalEntityName;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDeleteInverse(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_GROUP_ID")
    private CompanyGroup companyGroup;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_DIRECTOR_ID")
    protected Person companyDirector;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "companyName")
    protected List<CompanyDepartament> departmentOfCompany;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_OF_COMPANY_ID")
    protected City cityOfCompany;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REGION_OF_COMPANY_ID")
    protected Region regionOfCompany;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUNTRY_OF_COMPANY_ID")
    protected Country countryOfCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_COMPANY_LOGO")
    protected FileDescriptor fileCompanyLogo;

    @Lob
    @Column(name = "ADDRESS_OF_COMPANY")
    protected String addressOfCompany;

    @Lob
    @Column(name = "COMPANY_DESCRIPTION")
    private String companyDescription;

    @Lob
    @Column(name = "WORKING_CONDITIONS")
    private String workingConditions;

    @Column(name = "INN", length = 12)
    protected String inn;

    @Column(name = "KPP", length = 9)
    protected String kpp;

    @Column(name = "OGRN", length = 15)
    protected String ogrn;

    @Column(name = "OKPO", length = 10)
    protected String okpo;

    @Column(name = "OKTMO", length = 11)
    protected String oktmo;

    @Column(name = "OKVED", length = 100)
    protected String okved;

    @Lob
    @Column(name = "LEGAL_ADDRESS")
    protected String legalAddress;

    @Lob
    @Column(name = "ACTUAL_ADDRESS")
    protected String actualAddress;

    @Lob
    @Column(name = "POSTAL_ADDRESS")
    protected String postalAddress;

    @Column(name = "BIK", length = 9)
    protected String bik;

    @Column(name = "BANK_NAME", length = 255)
    protected String bankName;

    @Column(name = "SETTLEMENT_ACCOUNT", length = 20)
    protected String settlementAccount;

    @Column(name = "CORRESPONDENT_ACCOUNT", length = 20)
    protected String correspondentAccount;

    @Column(name = "PHONE", length = 50)
    protected String phone;

    @Column(name = "EMAIL", length = 100)
    protected String email;

    @Column(name = "WEBSITE", length = 255)
    protected String website;

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

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public void setLegalEntityName(String legalEntityName) {
        this.legalEntityName = legalEntityName;
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

    public CompanyGroup getCompanyGroup() {
        return companyGroup;
    }

    public void setCompanyGroup(CompanyGroup companyGroup) {
        this.companyGroup = companyGroup;
    }

    public Boolean getOurLegalEntity() {
        return ourLegalEntity;
    }

    public void setOurLegalEntity(Boolean ourLegalEntity) {
        this.ourLegalEntity = ourLegalEntity;
    }

    public String getWorkingConditions() {
        return workingConditions;
    }

    public void setWorkingConditions(String workingConditions) {
        this.workingConditions = workingConditions;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }

    public Boolean getOurClient() {
        return ourClient;
    }

    public void setOurClient(Boolean ourClient) {
        this.ourClient = ourClient;
    }

    public Region getRegionOfCompany() {
        return regionOfCompany;
    }

    public void setRegionOfCompany(Region regionOfCompany) {
        this.regionOfCompany = regionOfCompany;
    }

    public String getAddressOfCompany() {
        return addressOfCompany;
    }

    public void setAddressOfCompany(String addressOfCompany) {
        this.addressOfCompany = addressOfCompany;
    }

    public Country getCountryOfCompany() {
        return countryOfCompany;
    }

    public void setCountryOfCompany(Country countryOfCompany) {
        this.countryOfCompany = countryOfCompany;
    }

    public City getCityOfCompany() {
        return cityOfCompany;
    }

    public void setCityOfCompany(City cityOfCompany) {
        this.cityOfCompany = cityOfCompany;
    }

    public List<CompanyDepartament> getDepartmentOfCompany() {
        return departmentOfCompany;
    }

    public void setDepartmentOfCompany(List<CompanyDepartament> departmentOfCompany) {
        this.departmentOfCompany = departmentOfCompany;
    }

    public Person getCompanyDirector() {
        return companyDirector;
    }

    public void setCompanyDirector(Person companyDirector) {
        this.companyDirector = companyDirector;
    }

    public String getCompanyShortName() {
        return companyShortName;
    }

    public void setCompanyShortName(String companyShortName) {
        this.companyShortName = companyShortName;
    }

    public Ownershup getCompanyOwnership() {
        return companyOwnership;
    }

    public void setCompanyOwnership(Ownershup companyOwnership) {
        this.companyOwnership = companyOwnership;
    }

    public String getComanyName() {
        return comanyName;
    }

    public void setComanyName(String comanyName) {
        this.comanyName = comanyName;
    }

    public void setFileCompanyLogo(FileDescriptor fileCompanyLogo) {
        this.fileCompanyLogo = fileCompanyLogo;
    }

    public FileDescriptor getFileCompanyLogo() {
        return fileCompanyLogo;
    }
}