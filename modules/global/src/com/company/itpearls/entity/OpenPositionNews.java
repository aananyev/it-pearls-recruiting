package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "ITPEARLS_OPEN_POSITION_NEWS")
@Entity(name = "itpearls_OpenPositionNews")
public class OpenPositionNews extends StandardEntity {
    private static final long serialVersionUID = 1736227904829751645L;

    @NotNull
    @Column(name = "SUBJECT", nullable = false)
    private String subject;

    @NotNull
    @OnDelete(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OPEN_POSITION_ID")
    private OpenPosition openPosition;

    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    @Column(name = "DATE_NEWS", nullable = false)
    private Date dateNews;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CANDIDATES_ID")
    private JobCandidate candidates;

    @Lookup(type = LookupType.DROPDOWN, actions = {"lookup", "open", "clear"})
    @OnDelete(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AUTHOR_ID")
    @NotNull
    private ExtUser author;

    @Column(name = "PRIORITY_NEWS")
    private Boolean priorityNews;

    public void setAuthor(ExtUser author) {
        this.author = author;
    }

    public ExtUser getAuthor() {
        return author;
    }

    public JobCandidate getCandidates() {
        return candidates;
    }

    public void setCandidates(JobCandidate candidates) {
        this.candidates = candidates;
    }

    public Boolean getPriorityNews() {
        return priorityNews;
    }

    public void setPriorityNews(Boolean priorityNews) {
        this.priorityNews = priorityNews;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getDateNews() {
        return dateNews;
    }

    public void setDateNews(Date dateNews) {
        this.dateNews = dateNews;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }
}