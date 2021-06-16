package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.entity.annotation.PublishEntityChangedEvents;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@PublishEntityChangedEvents
@NamePattern("%s %s %s %s|secondName,firstName,middleName,personPosition")
@Table(name = "ITPEARLS_JOB_CANDIDATE", indexes = {
        @Index(name = "IDX_ITPEARLS_JOB_CANDIDATE_ID", columnList = "ID"),
        @Index(name = "IDX_ITPEARLS_JOB_CANDIDATE_CURRENT_COMPANY_ID", columnList = "CURRENT_COMPANY_ID"),
        @Index(name = "IDX_ITPEARLS_JOB_CANDIDATE_PERSON_POSITION_ID", columnList = "PERSON_POSITION_ID"),
        @Index(name = "IDX_ITPEARLS_JOB_CANDIDATE_CITY_OF_RESIDENCE_ID", columnList = "CITY_OF_RESIDENCE_ID"),
        @Index(name = "IDX_ITPEARLS_JOB_CANDIDATE_OPEN_POSITION_ID", columnList = "OPEN_POSITION_ID"),
        @Index(name = "IDX_ITPEARLS_JOB_CANDIDATE_FULL_NAME", columnList = "FULL_NAME")
})
@Entity(name = "itpearls_JobCandidate")
public class JobCandidate extends StandardEntity {
    private static final long serialVersionUID = 4893767839653404761L;

    @NotNull
    @Column(name = "FIRST_NAME", nullable = false, length = 80)
    protected String firstName;

    @Column(name = "MIDDLE_NAME", length = 80)
    protected String middleName;

    @NotNull
    @Column(name = "SECOND_NAME", nullable = false, length = 80)
    protected String secondName;

    @Column(name = "FULL_NAME", length = 160)
    protected String fullName;

    @Temporal(TemporalType.DATE)
    @Column(name = "BIRDH_DATE")
    protected Date birdhDate;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSON_POSITION_ID")
    protected Position personPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CURRENT_COMPANY_ID")
    protected Company currentCompany;

    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "jobCandidate")
    protected List<Position> positionList;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_OF_RESIDENCE_ID")
    protected City cityOfResidence;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POSITION_COUNTRY_ID")
    protected Country positionCountry;

    @Email
    @Column(name = "EMAIL", length = 50)
    protected String email;

    @Column(name = "PHONE", length = 18)
    protected String phone;

    @Column(name = "MOBILE_PHONE", length = 18)
    protected String mobilePhone;

    @Column(name = "SKYPE_NAME", length = 30)
    protected String skypeName;

    @Column(name = "TELEGRAM_NAME", length = 30)
    protected String telegramName;

    @Column(name = "TELEGRAM_GROUP", length = 50)
    protected String telegramGroup;

    @Column(name = "WIBER_NAME", length = 30)
    protected String wiberName;

    @Column(name = "WHATSUP_NAME", length = 30)
    protected String whatsupName;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "candidate")
    protected List<IteractionList> iteractionList;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SPECIALISATION_ID")
    protected Specialisation specialisation;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "candidate")
    protected List<JobHistory> jobHistory;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "jobCandidate")
    protected List<SocialNetworkURLs> socialNetwork;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "candidate")
    protected List<CandidateCV> candidateCv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SKILL_TREE_ID")
    protected SkillTree skillTree;

    @Composition
    @OnDelete(DeletePolicy.DENY)
    @OneToMany(mappedBy = "jobCandidate")
    protected List<SkillTree> skills;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    protected OpenPosition openPosition;

    @Column(name = "STATUS")
    protected Integer status;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_IMAGE_FACE")
    protected FileDescriptor fileImageFace;


    public FileDescriptor getFileImageFace() {
        return fileImageFace;
    }

    public void setFileImageFace(FileDescriptor fileImageFace) {
        this.fileImageFace = fileImageFace;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<SkillTree> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillTree> skills) {
        this.skills = skills;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public List<Position> getPositionList() {
        return positionList;
    }

    public void setPositionList(List<Position> positionList) {
        this.positionList = positionList;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public SkillTree getSkillTree() {
        return skillTree;
    }

    public void setSkillTree(SkillTree skillTree) {
        this.skillTree = skillTree;
    }

    public List<SocialNetworkURLs> getSocialNetwork() {
        return socialNetwork;
    }

    public void setSocialNetwork(List<SocialNetworkURLs> socialNetwork) {
        this.socialNetwork = socialNetwork;
    }

    public List<CandidateCV> getCandidateCv() {
        return candidateCv;
    }

    public void setCandidateCv(List<CandidateCV> candidateCv) {
        this.candidateCv = candidateCv;
    }

    public List<JobHistory> getJobHistory() {
        return jobHistory;
    }

    public void setJobHistory(List<JobHistory> jobHistory) {
        this.jobHistory = jobHistory;
    }

    public City getCityOfResidence() {
        return cityOfResidence;
    }

    public void setCityOfResidence(City cityOfResidence) {
        this.cityOfResidence = cityOfResidence;
    }

    public Specialisation getSpecialisation() {
        return specialisation;
    }

    public void setSpecialisation(Specialisation specialisation) {
        this.specialisation = specialisation;
    }

    public List<IteractionList> getIteractionList() {
        return iteractionList;
    }

    public void setIteractionList(List<IteractionList> iteractionList) {
        this.iteractionList = iteractionList;
    }

    public Company getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(Company currentCompany) {
        this.currentCompany = currentCompany;
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

    public void setTelegramGroup(String telegramGroup) {
        this.telegramGroup = telegramGroup;
    }

    public String getTelegramGroup() {
        return telegramGroup;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }
}