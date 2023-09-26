package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import java.util.Date;

@Table(name = "ITPEARLS_PERSONEL_RESERVE")
@Entity(name = "itpearls_PersonelReserve")
public class PersonelReserve extends StandardEntity {
    private static final long serialVersionUID = 3132402819849167563L;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOB_CANDIDATE_ID")
    private JobCandidate jobCandidate;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REACTUTIER_ID")
    private ExtUser recruter;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSON_POSITION_ID")
    private Position personPosition;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup", "open", "clear"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    private OpenPosition openPosition;

    @Column(name = "IN_PROCESSED")
    private Boolean inProcess;

    @Temporal(TemporalType.DATE)
    @Column(name = "DATE_")
    private Date date;

    @Column(name = "TERM_OF_PLACEMENT")
    private Integer termOfPlacement;

    @Temporal(TemporalType.DATE)
    @Column(name = "END_DATE")
    private Date endDate;

    @Column(name = "SELECTED_FOR_ACTION")
    private Boolean selectedForAction;

    @Column(name = "SELECTION_SYMBOL_FOR_ACTIONS")
    private Integer selectionSymbolForActions;

    @Column(name = "REMOVED_FROM_RESERVE")
    private Boolean removedFromReserve;

    public Integer getSelectionSymbolForActions() {
        return selectionSymbolForActions;
    }

    public void setSelectionSymbolForActions(Integer selectionSymbolForActions) {
        this.selectionSymbolForActions = selectionSymbolForActions;
    }

    public void setRecruter(ExtUser recruter) {
        this.recruter = recruter;
    }

    public ExtUser getRecruter() {
        return recruter;
    }

    public Boolean getSelectedForAction() {
        return selectedForAction;
    }

    public void setSelectedForAction(Boolean selectedForAction) {
        this.selectedForAction = selectedForAction;
    }

    public Boolean getInProcess() {
        return inProcess;
    }

    public void setInProcess(Boolean inProcess) {
        this.inProcess = inProcess;
    }

    public Boolean getRemovedFromReserve() {
        return removedFromReserve;
    }

    public void setRemovedFromReserve(Boolean removedFromReserve) {
        this.removedFromReserve = removedFromReserve;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public Position getPersonPosition() {
        return personPosition;
    }

    public void setPersonPosition(Position personPosition) {
        this.personPosition = personPosition;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getTermOfPlacement() {
        return termOfPlacement;
    }

    public void setTermOfPlacement(Integer termOfPlacement) {
        this.termOfPlacement = termOfPlacement;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }
}