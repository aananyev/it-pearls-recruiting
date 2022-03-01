package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.*;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@PublishEntityChangedEvents
@NamePattern("%s %s %s %s|secondName,firstName,middleName,personPosition")
@Table(name = "ITPEARLS_JOB_CANDIDATE", indexes = {
        @Index(name = "IDX_ITPEARLS_JOB_CANDIDATE_FULL_NAME", columnList = "FULL_NAME"),
        @Index(name = "IDX_ITPEARLS_JOB_CANDIDATE_FIRST_NAME", columnList = "FIRST_NAME"),
        @Index(name = "IDX_ITPEARLS_JOB_CANDIDATE_SECOND_NAME", columnList = "SECOND_NAME")
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

    @Column(name = "BLOCK_CANDIDATE")
    private Boolean blockCandidate;

    @Column(name = "FULL_NAME", length = 160)
    protected String fullName;

    @Temporal(TemporalType.DATE)
    @Column(name = "BIRDH_DATE")
    protected Date birdhDate;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSON_POSITION_ID")
    protected Position personPosition;

    @OnDeleteInverse(DeletePolicy.UNLINK)
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "jobCandidate")
    @Composition
    private List<JobCandidatePositionLists> positionList;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CURRENT_COMPANY_ID")
    protected Company currentCompany;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_OF_RESIDENCE_ID")
    protected City cityOfResidence;

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

    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "candidate")
    @Composition
    protected List<IteractionList> iteractionList;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "jobCandidate")
    private List<LaborAgreement> laborAgreement;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "jobCandidate")
    protected List<SocialNetworkURLs> socialNetwork;

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
    @OneToMany(mappedBy = "candidate")
    protected List<CandidateCV> candidateCv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SKILL_TREE_ID")
    protected SkillTree skillTree;

    @Column(name = "STATUS")
    protected Integer status;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_IMAGE_FACE")
    protected FileDescriptor fileImageFace;

    @Column(name = "WORK_STATUS")
    protected Integer workStatus;

    @NotNull
    @Column(name = "PRIORITY_CONTACT", nullable = false)
    private Integer priorityContact;

    public List<LaborAgreement> getLaborAgreement() {
        return laborAgreement;
    }

    public void setLaborAgreement(List<LaborAgreement> laborAgreement) {
        this.laborAgreement = laborAgreement;
    }

    public void setWorkStatus(Integer workStatus) {
        this.workStatus = workStatus;
    }

    public Integer getWorkStatus() {
        return workStatus;
    }

    public void setIteractionList(List<IteractionList> iteractionList) {
        this.iteractionList = iteractionList;
    }

    public List<IteractionList> getIteractionList() {
        return iteractionList;
    }

    public Boolean getBlockCandidate() {
        return blockCandidate;
    }

    public void setBlockCandidate(Boolean blockCandidate) {
        this.blockCandidate = blockCandidate;
    }

    public void setPositionList(List<JobCandidatePositionLists> positionList) {
        this.positionList = positionList;
    }

    public List<JobCandidatePositionLists> getPositionList() {
        return positionList;
    }

    public Integer getPriorityContact() {
        return priorityContact;
    }

    public void setPriorityContact(Integer priorityContact) {
        this.priorityContact = priorityContact;
    }


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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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