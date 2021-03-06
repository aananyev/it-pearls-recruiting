package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|iterationName")
@Table(name = "ITPEARLS_ITERACTION", indexes = {
        @Index(name = "IDX_ITPEARLS_ITERACTION_NAME", columnList = "ITERATION_NAME"),
        @Index(name = "IDX_ITPEARLS_ITERACTION_NUMBER", columnList = "NUMBER_")
})
@Entity(name = "itpearls_Iteraction")
public class Iteraction extends StandardEntity {
    private static final long serialVersionUID = -3287484760093673466L;

    @Column(name = "NUMBER_")
    protected String number;

    @Column(name = "MANDATORY_ITERACTION")
    protected Boolean mandatoryIteraction;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITERACTION_TREE_ID")
    protected Iteraction iteractionTree;

    @NotNull
    @Column(name = "ITERATION_NAME", nullable = false, unique = true, length = 80)
    protected String iterationName;

    @Column(name = "PIC", length = 80)
    protected String pic;

    @Column(name = "CALL_BUTTON_TEXT", length = 30)
    protected String callButtonText;

    @Column(name = "CALL_CLASS", length = 30)
    protected String callClass;

    @Column(name = "CALL_FORM")
    protected Boolean callForm;

    @Column(name = "ADD_FLAG")
    protected Boolean addFlag;

    @Column(name = "SET_DATE_TIME")
    protected Boolean setDateTime;

    @Column(name = "ADD_TYPE")
    protected Integer addType;

    @Column(name = "CHECK_TRACE")
    protected  Integer checkTrace = 1;

    @Column(name = "ADD_FIELD", length = 40)
    protected String addField;

    @Column(name = "ADD_CAPTION", length = 80)
    protected String addCaption;

    @Column(name = "NOTIFICATION_TYPE")
    protected Integer notificationType;

    @Column(name = "CALENDAR_ITEM")
    protected Boolean calendarItem;

    @Column(name = "CALENDAR_ITEM_STYLE")
    protected String calendarItemStyle;

    @Column(name = "CALENDAR_ITEM_DESCRIPTION", length = 80)
    protected String calendarItemDescription;

    @Column(name = "FIND_TO_DIC")
    protected Boolean findToDic;

    public Boolean getFindToDic() { return findToDic; }

    public void setFindToDic( Boolean findToDic ) { this.findToDic = findToDic; }

    public Integer getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(Integer notificationType) {
        this.notificationType = notificationType;
    }

    public String getAddCaption() {
        return addCaption;
    }

    public void setAddCaption(String addCaption) {
        this.addCaption = addCaption;
    }

    public String getAddField() {
        return addField;
    }

    public void setAddField(String addField) {
        this.addField = addField;
    }

    public Integer getAddType() {
        return addType;
    }

    public void setAddType(Integer addType) {
        this.addType = addType;
    }

    public Boolean getAddFlag() {
        return addFlag;
    }

    public Boolean getSetDateTime() {
        return setDateTime;
    }

    public void setSetDateTime(Boolean setDateTime) {
        this.setDateTime = setDateTime;
    }

    public void setAddFlag(Boolean addFlag) {
        this.addFlag = addFlag;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public Boolean getCallForm() {
        return callForm;
    }

    public void setCallForm(Boolean callForm) {
        this.callForm = callForm;
    }

    public String getCallClass() {
        return callClass;
    }

    public void setCallClass(String callClass) {
        this.callClass = callClass;
    }

    public String getCallButtonText() {
        return callButtonText;
    }

    public void setCallButtonText(String callButtonText) {
        this.callButtonText = callButtonText;
    }

    public Boolean getMandatoryIteraction() {
        return mandatoryIteraction;
    }

    public void setMandatoryIteraction(Boolean mandatoryIteraction) {
        this.mandatoryIteraction = mandatoryIteraction;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    public Iteraction getIteractionTree() {
        return iteractionTree;
    }

    public void setIteractionTree(Iteraction iteractionTree) {
        this.iteractionTree = iteractionTree;
    }

    public String getIterationName() {
        return iterationName;
    }

    public void setIterationName(String iterationName) {
        this.iterationName = iterationName;
    }

    public String getCalendarItemDescription() {
        return calendarItemDescription;
    }

    public void setCalendarItemDescription(String calendarItemDescription) {
        this.calendarItemDescription = calendarItemDescription;
    }

    public void setCalendarItem( Boolean calendarItem ) { this.calendarItem = calendarItem; }

    public Boolean getCalendarItem() { return calendarItem; }

    public void setCalendarItemStyle( String calendarItemStyle ) { this.calendarItemStyle = calendarItemStyle; }

    public String getCalendarItemStyle() { return calendarItemStyle; }

    public Integer getCheckTrace() {
        return checkTrace;
    }

    public void setCheckTrace(Integer checkTrace) {
        this.checkTrace = checkTrace;
    }
}