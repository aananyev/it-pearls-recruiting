package com.company.hunttech.service.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Сообщение в чате Hermes Agent.
 */
public class HermesChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String role; // "user" or "assistant"
    private String content;
    private Date createTs;
    private Integer sequenceNo;
    private String hermesSessionId;

    public HermesChatMessage() {
        this.id = UUID.randomUUID();
        this.createTs = new Date();
    }

    public HermesChatMessage(String role, String content) {
        this();
        this.role = role;
        this.content = content;
    }

    public HermesChatMessage(String role, String content, String hermesSessionId) {
        this(role, content);
        this.hermesSessionId = hermesSessionId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getCreateTs() { return createTs; }
    public void setCreateTs(Date createTs) { this.createTs = createTs; }

    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }

    public String getHermesSessionId() { return hermesSessionId; }
    public void setHermesSessionId(String hermesSessionId) { this.hermesSessionId = hermesSessionId; }
}
