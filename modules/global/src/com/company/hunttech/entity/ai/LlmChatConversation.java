package com.company.hunttech.entity.ai;

import com.company.hunttech.entity.ExtUser;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "HUNTTECH_LLM_CHAT_CONVERSATION")
@Entity(name = "hunttech_LlmChatConversation")
@NamePattern("%s|title")
public class LlmChatConversation extends StandardEntity {
    private static final long serialVersionUID = 1L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    @OnDeleteInverse(DeletePolicy.DENY)
    private ExtUser user;

    @Column(name = "TITLE", length = 255)
    private String title;

    @NotNull
    @Column(name = "STATUS", nullable = false, length = 32)
    private String status = "ACTIVE";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LAST_MESSAGE_AT")
    private Date lastMessageAt;

    public ExtUser getUser() { return user; }
    public void setUser(ExtUser user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Date lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
