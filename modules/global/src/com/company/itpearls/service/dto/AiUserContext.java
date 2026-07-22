package com.company.itpearls.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiUserContext implements Serializable {
    private static final long serialVersionUID = 4018224866074032864L;

    private boolean active;
    private final Map<String, String> profileData = new LinkedHashMap<>();
    private final List<String> customInstructions = new ArrayList<>();

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Map<String, String> getProfileData() {
        return profileData;
    }

    public List<String> getCustomInstructions() {
        return customInstructions;
    }

    public boolean isEmpty() {
        return !active || (profileData.isEmpty() && customInstructions.isEmpty());
    }
}
