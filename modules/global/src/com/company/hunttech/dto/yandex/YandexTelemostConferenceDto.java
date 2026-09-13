package com.company.hunttech.dto.yandex;

import java.io.Serializable;

public class YandexTelemostConferenceDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String joinUrl;
    private String invitation;
    private String accessLevel;

    public YandexTelemostConferenceDto() {
    }

    public YandexTelemostConferenceDto(String id, String joinUrl, String invitation, String accessLevel) {
        this.id = id;
        this.joinUrl = joinUrl;
        this.invitation = invitation;
        this.accessLevel = accessLevel;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJoinUrl() {
        return joinUrl;
    }

    public void setJoinUrl(String joinUrl) {
        this.joinUrl = joinUrl;
    }

    public String getInvitation() {
        return invitation;
    }

    public void setInvitation(String invitation) {
        this.invitation = invitation;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }
}
