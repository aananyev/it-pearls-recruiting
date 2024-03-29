package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.CaseConversion;
import com.haulmont.cuba.core.entity.annotation.ConversionType;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.Email;
import java.util.Date;

@NamePattern("%s %s|firstName,secondName")
@Table(name = "ITPEARLS_PERSON", indexes = {
        @Index(name = "IDX_ITPEARLS_PERSON_FIRST_NAME", columnList = "FIRST_NAME"),
        @Index(name = "IDX_ITPEARLS_PERSON_MIDDLE_NAME", columnList = "MIDDLE_NAME"),
        @Index(name = "IDX_ITPEARLS_PERSON_SECOND_NAME", columnList = "SECOND_NAME")
})
@Entity(name = "itpearls_Person")
public class Person extends StandardEntity {
    private static final long serialVersionUID = 5184615102344376058L;

    @Column(name = "FIRST_NAME", length = 80)
    protected String firstName;

    @Column(name = "MIDDLE_NAME", length = 80)
    protected String middleName;

    @Column(name = "SECOND_NAME", length = 80)
    protected String secondName;

    @Temporal(TemporalType.DATE)
    @Column(name = "BIRDH_DATE")
    protected Date birdhDate;

    @Email
    @Column(name = "EMAIL", length = 40)
    protected String email;

    @Column(name = "PHONE", length = 20)
    protected String phone;

    @Column(name = "MOB_PHONE", unique = true, length = 20)
    protected String mobPhone;

    @CaseConversion(type = ConversionType.LOWER)
    @Column(name = "SKYPE_NAME", unique = true, length = 40)
    protected String skypeName;

    @Column(name = "TELEGRAM_NAME", unique = true, length = 40)
    protected String telegramName;

    @Column(name = "WIBER_NAME", unique = true, length = 40)
    protected String wiberName;

    @CaseConversion(type = ConversionType.LOWER)
    @Column(name = "WATSUP_NAME", unique = true, length = 40)
    protected String watsupName;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POSITION_COUNTRY_ID")
    protected Country positionCountry;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_OF_RESIDENCE_ID")
    protected City cityOfResidence;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSON_POSITION_ID")
    protected Position personPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_DEPARTMENT_ID")
    protected CompanyDepartament companyDepartment;

    @Column(name = "SEND_RESUME_TO_EMAIL")
    protected Boolean sendResumeToEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_IMAGE_FACE")
    protected FileDescriptor fileImageFace;

    public Boolean getSendResumeToEmail() {
        return sendResumeToEmail;
    }

    public void setSendResumeToEmail(Boolean sendResumeToEmail) {
        this.sendResumeToEmail = sendResumeToEmail;
    }

    public CompanyDepartament getCompanyDepartment() {
        return companyDepartment;
    }

    public void setCompanyDepartment(CompanyDepartament companyDepartment) {
        this.companyDepartment = companyDepartment;
    }

    public String getMobPhone() {
        return mobPhone;
    }

    public void setMobPhone(String mobPhone) {
        this.mobPhone = mobPhone;
    }

    public String getWatsupName() {
        return watsupName;
    }

    public void setWatsupName(String watsupName) {
        this.watsupName = watsupName;
    }

    public String getWiberName() {
        return wiberName;
    }

    public void setWiberName(String wiberName) {
        this.wiberName = wiberName;
    }

    public String getTelegramName() {
        return telegramName;
    }

    public void setTelegramName(String telegramName) {
        this.telegramName = telegramName;
    }

    public String getSkypeName() {
        return skypeName;
    }

    public void setSkypeName(String skypeName) {
        this.skypeName = skypeName;
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

    public Date getBirdhDate() {
        return birdhDate;
    }

    public void setBirdhDate(Date birdhDate) {
        this.birdhDate = birdhDate;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public Position getPersonPosition() {
        return personPosition;
    }

    public void setPersonPosition(Position personPosition) {
        this.personPosition = personPosition;
    }

    public Country getPositionCountry() {
        return positionCountry;
    }

    public void setPositionCountry(Country positionCountry) {
        this.positionCountry = positionCountry;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public FileDescriptor getFileImageFace() {
        return fileImageFace;
    }

    public void setFileImageFace(FileDescriptor fileImageFace) {
        this.fileImageFace = fileImageFace;
    }

    public void setCityOfResidence(City cityOfResidence) {
        this.cityOfResidence = cityOfResidence;
    }

    public City getCityOfResidence() {
        return cityOfResidence;
    }
}