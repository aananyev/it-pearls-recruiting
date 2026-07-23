package com.company.hunttech.entity;

import com.company.hunttech.entity.ExtUser;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "HUNTTECH_USER_AI_PROFILE", uniqueConstraints = {
        @UniqueConstraint(name = "IDX_HUNTTECH_USER_AI_PROFILE_UNQ_USER", columnNames = "USER_ID")
})
@Entity(name = "hunttech_UserAiProfile")
@NamePattern("%s|user")
public class UserAiProfile extends StandardEntity {
    private static final long serialVersionUID = 5119245747892793319L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDeleteInverse(DeletePolicy.DENY)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    @NotNull
    private ExtUser user;

    @NotNull
    @Column(name = "PROFILE_ENABLED", nullable = false)
    private Boolean profileEnabled = false;

    @NotNull
    @Column(name = "EXTERNAL_PROCESSING_ALLOWED", nullable = false)
    private Boolean externalProcessingAllowed = false;

    @Column(name = "CONSENT_VERSION", length = 32)
    private String consentVersion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CONSENT_ACCEPTED_AT")
    private Date consentAcceptedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PROFILE_CONFIRMED_AT")
    private Date profileConfirmedAt;

    @Lob
    @Column(name = "ABOUT_ME")
    private String aboutMe;

    @Column(name = "CURRENT_POSITION", length = 255)
    private String currentPosition;

    @Column(name = "FUNCTIONAL_ROLE")
    private Integer functionalRole;

    @Column(name = "SENIORITY_LEVEL")
    private Integer seniorityLevel;

    @Column(name = "PROFESSIONAL_EXPERIENCE_YEARS")
    private Integer professionalExperienceYears;

    @Column(name = "RECRUITING_EXPERIENCE_YEARS")
    private Integer recruitingExperienceYears;

    @Lob
    @Column(name = "CURRENT_RESPONSIBILITIES")
    private String currentResponsibilities;

    @Lob
    @Column(name = "EDUCATION")
    private String education;

    @Lob
    @Column(name = "CERTIFICATIONS")
    private String certifications;

    @Lob
    @Column(name = "DOMAIN_EXPERTISE")
    private String domainExpertise;

    @Lob
    @Column(name = "INDUSTRIES")
    private String industries;

    @Lob
    @Column(name = "RECRUITING_SPECIALIZATIONS")
    private String recruitingSpecializations;

    @Lob
    @Column(name = "TARGET_ROLES")
    private String targetRoles;

    @Column(name = "CANDIDATE_LEVELS", length = 255)
    private String candidateLevels;

    @Lob
    @Column(name = "HIRING_GEOGRAPHIES")
    private String hiringGeographies;

    @Lob
    @Column(name = "DECISION_PRIORITIES")
    private String decisionPriorities;

    @Lob
    @Column(name = "CLIENT_PROJECT_CONTEXT")
    private String clientAndProjectContext;

    @Lob
    @Column(name = "PROFESSIONAL_GOALS")
    private String professionalGoals;

    @Lob
    @Column(name = "PROFESSIONAL_INTERESTS")
    private String professionalInterests;

    @Lob
    @Column(name = "DEVELOPMENT_AREAS")
    private String developmentAreas;

    @Lob
    @Column(name = "CURRENT_PRIORITIES")
    private String currentPriorities;

    @Column(name = "PREFERRED_LANGUAGE")
    private Integer preferredLanguage;

    @Column(name = "RESPONSE_DETAIL_LEVEL")
    private Integer responseDetailLevel;

    @Column(name = "COMMUNICATION_STYLE")
    private Integer communicationStyle;

    @Column(name = "TERMINOLOGY_LEVEL")
    private Integer terminologyLevel;

    @Column(name = "PREFERRED_ANSWER_STRUCTURE")
    private Integer preferredAnswerStructure;

    @Lob
    @Column(name = "CUSTOM_AI_INSTRUCTIONS")
    private String customAiInstructions;

    @Lob
    @Column(name = "COMMUNICATION_CONSTRAINTS")
    private String communicationConstraints;

    public ExtUser getUser() { return user; }
    public void setUser(ExtUser user) { this.user = user; }
    public Boolean getProfileEnabled() { return profileEnabled; }
    public void setProfileEnabled(Boolean profileEnabled) { this.profileEnabled = profileEnabled; }
    public Boolean getExternalProcessingAllowed() { return externalProcessingAllowed; }
    public void setExternalProcessingAllowed(Boolean externalProcessingAllowed) { this.externalProcessingAllowed = externalProcessingAllowed; }
    public String getConsentVersion() { return consentVersion; }
    public void setConsentVersion(String consentVersion) { this.consentVersion = consentVersion; }
    public Date getConsentAcceptedAt() { return consentAcceptedAt; }
    public void setConsentAcceptedAt(Date consentAcceptedAt) { this.consentAcceptedAt = consentAcceptedAt; }
    public Date getProfileConfirmedAt() { return profileConfirmedAt; }
    public void setProfileConfirmedAt(Date profileConfirmedAt) { this.profileConfirmedAt = profileConfirmedAt; }
    public String getAboutMe() { return aboutMe; }
    public void setAboutMe(String aboutMe) { this.aboutMe = aboutMe; }
    public String getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }
    public AiFunctionalRole getFunctionalRole() { return functionalRole == null ? null : AiFunctionalRole.fromId(functionalRole); }
    public void setFunctionalRole(AiFunctionalRole functionalRole) { this.functionalRole = functionalRole == null ? null : functionalRole.getId(); }
    public AiSeniorityLevel getSeniorityLevel() { return seniorityLevel == null ? null : AiSeniorityLevel.fromId(seniorityLevel); }
    public void setSeniorityLevel(AiSeniorityLevel seniorityLevel) { this.seniorityLevel = seniorityLevel == null ? null : seniorityLevel.getId(); }
    public Integer getProfessionalExperienceYears() { return professionalExperienceYears; }
    public void setProfessionalExperienceYears(Integer professionalExperienceYears) { this.professionalExperienceYears = professionalExperienceYears; }
    public Integer getRecruitingExperienceYears() { return recruitingExperienceYears; }
    public void setRecruitingExperienceYears(Integer recruitingExperienceYears) { this.recruitingExperienceYears = recruitingExperienceYears; }
    public String getCurrentResponsibilities() { return currentResponsibilities; }
    public void setCurrentResponsibilities(String currentResponsibilities) { this.currentResponsibilities = currentResponsibilities; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }
    public String getDomainExpertise() { return domainExpertise; }
    public void setDomainExpertise(String domainExpertise) { this.domainExpertise = domainExpertise; }
    public String getIndustries() { return industries; }
    public void setIndustries(String industries) { this.industries = industries; }
    public String getRecruitingSpecializations() { return recruitingSpecializations; }
    public void setRecruitingSpecializations(String recruitingSpecializations) { this.recruitingSpecializations = recruitingSpecializations; }
    public String getTargetRoles() { return targetRoles; }
    public void setTargetRoles(String targetRoles) { this.targetRoles = targetRoles; }
    public String getCandidateLevels() { return candidateLevels; }
    public void setCandidateLevels(String candidateLevels) { this.candidateLevels = candidateLevels; }
    public String getHiringGeographies() { return hiringGeographies; }
    public void setHiringGeographies(String hiringGeographies) { this.hiringGeographies = hiringGeographies; }
    public String getDecisionPriorities() { return decisionPriorities; }
    public void setDecisionPriorities(String decisionPriorities) { this.decisionPriorities = decisionPriorities; }
    public String getClientAndProjectContext() { return clientAndProjectContext; }
    public void setClientAndProjectContext(String clientAndProjectContext) { this.clientAndProjectContext = clientAndProjectContext; }
    public String getProfessionalGoals() { return professionalGoals; }
    public void setProfessionalGoals(String professionalGoals) { this.professionalGoals = professionalGoals; }
    public String getProfessionalInterests() { return professionalInterests; }
    public void setProfessionalInterests(String professionalInterests) { this.professionalInterests = professionalInterests; }
    public String getDevelopmentAreas() { return developmentAreas; }
    public void setDevelopmentAreas(String developmentAreas) { this.developmentAreas = developmentAreas; }
    public String getCurrentPriorities() { return currentPriorities; }
    public void setCurrentPriorities(String currentPriorities) { this.currentPriorities = currentPriorities; }
    public AiPreferredLanguage getPreferredLanguage() { return preferredLanguage == null ? null : AiPreferredLanguage.fromId(preferredLanguage); }
    public void setPreferredLanguage(AiPreferredLanguage preferredLanguage) { this.preferredLanguage = preferredLanguage == null ? null : preferredLanguage.getId(); }
    public AiResponseDetailLevel getResponseDetailLevel() { return responseDetailLevel == null ? null : AiResponseDetailLevel.fromId(responseDetailLevel); }
    public void setResponseDetailLevel(AiResponseDetailLevel responseDetailLevel) { this.responseDetailLevel = responseDetailLevel == null ? null : responseDetailLevel.getId(); }
    public AiCommunicationStyle getCommunicationStyle() { return communicationStyle == null ? null : AiCommunicationStyle.fromId(communicationStyle); }
    public void setCommunicationStyle(AiCommunicationStyle communicationStyle) { this.communicationStyle = communicationStyle == null ? null : communicationStyle.getId(); }
    public AiTerminologyLevel getTerminologyLevel() { return terminologyLevel == null ? null : AiTerminologyLevel.fromId(terminologyLevel); }
    public void setTerminologyLevel(AiTerminologyLevel terminologyLevel) { this.terminologyLevel = terminologyLevel == null ? null : terminologyLevel.getId(); }
    public AiAnswerStructure getPreferredAnswerStructure() { return preferredAnswerStructure == null ? null : AiAnswerStructure.fromId(preferredAnswerStructure); }
    public void setPreferredAnswerStructure(AiAnswerStructure preferredAnswerStructure) { this.preferredAnswerStructure = preferredAnswerStructure == null ? null : preferredAnswerStructure.getId(); }
    public String getCustomAiInstructions() { return customAiInstructions; }
    public void setCustomAiInstructions(String customAiInstructions) { this.customAiInstructions = customAiInstructions; }
    public String getCommunicationConstraints() { return communicationConstraints; }
    public void setCommunicationConstraints(String communicationConstraints) { this.communicationConstraints = communicationConstraints; }
}
