package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;

@Table(name = "ITPEARLS_SKILLS_FILTER_LAST_SELECTION")
@Entity(name = "itpearls_SkillsFilterLastSelection")
public class SkillsFilterLastSelection extends StandardEntity {
    private static final long serialVersionUID = -7242346621554790378L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POSITION_ID")
    private Position position;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_ID")
    private City city;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    private OpenPosition openPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOB_CANDIDATES_ID")
    private JobCandidate jobCandidates;

    @Column(name = "JOB_CANDIDATE_SELECTION")
    private Boolean jobCandidateSelection;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Boolean getJobCandidateSelection() {
        return jobCandidateSelection;
    }

    public void setJobCandidateSelection(Boolean jobCandidateSelection) {
        this.jobCandidateSelection = jobCandidateSelection;
    }

    public JobCandidate getJobCandidates() {
        return jobCandidates;
    }

    public void setJobCandidates(JobCandidate jobCandidates) {
        this.jobCandidates = jobCandidates;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}