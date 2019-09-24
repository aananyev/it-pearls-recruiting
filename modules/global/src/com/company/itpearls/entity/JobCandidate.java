package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.Date;

@NamePattern("%s|firstName")
@Table(name = "ITPEARLS_JOB_CANDIDATE")
@Entity(name = "itpearls_JobCandidate")
public class JobCandidate extends StandardEntity {
    private static final long serialVersionUID = 4893767839653404761L;

    @NotNull
    @Column(name = "FIRST_NAME", nullable = false, length = 80)
    protected String firstName;

    @NotNull
    @Column(name = "MIDDLE_NAME", nullable = false, length = 80)
    protected String middleName;

    @NotNull
    @Column(name = "SECOND_NAME", nullable = false, length = 80)
    protected String secondName;

    @Temporal(TemporalType.DATE)
    @NotNull
    @Column(name = "BIRDH_DATE", nullable = false)
    protected Date birdhDate;

    @Email
    @Column(name = "EMAIL", length = 30)
    protected String email;

    @Column(name = "PHONE", length = 10)
    protected String phone;

    @Column(name = "SKYPE_NAME", length = 30)
    protected String skypeName;

    @Column(name = "TELEGRAM_NAME", length = 30)
    protected String telegramName;

    @Column(name = "WIBER_NAME", length = 30)
    protected String wiberName;

    @Column(name = "WHATSUP_NAME", length = 30)
    protected String whatsupName;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POSITION_COUNTRY_ID")
    protected Country positionCountry;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSON_POSITION_ID")
    protected Position personPosition;

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

    public String getWhatsupName() {
        return whatsupName;
    }

    public void setWhatsupName(String whatsupName) {
        this.whatsupName = whatsupName;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}