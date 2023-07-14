package com.company.itpearls.entity;

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
@Table(name = "ITPEARLS_COMPANY", indexes = {
        @Index(name = "IDX_COMPANY_NAME", columnList = "COMANY_NAME"),
        @Index(name = "IDX_ITPEARLS_COMPANY_SHORT_NAME", columnList = "COMPANY_SHORT_NAME")
})
@Entity(name = "itpearls_Company")
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