package com.company.hunttech.service.dto;

import java.io.Serializable;

/**
 * Статус подключения к Hermes Agent на сервере.
 */
public class HermesConnectionStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean available;
    private String serverHost;
    private String containerName;
    private String profileName;
    private String details;
    private long checkDurationMs;

    public HermesConnectionStatus() {
    }

    public HermesConnectionStatus(boolean available, String serverHost, String containerName,
                                  String profileName, String details, long checkDurationMs) {
        this.available = available;
        this.serverHost = serverHost;
        this.containerName = containerName;
        this.profileName = profileName;
        this.details = details;
        this.checkDurationMs = checkDurationMs;
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getServerHost() { return serverHost; }
    public void setServerHost(String serverHost) { this.serverHost = serverHost; }

    public String getContainerName() { return containerName; }
    public void setContainerName(String containerName) { this.containerName = containerName; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public long getCheckDurationMs() { return checkDurationMs; }
    public void setCheckDurationMs(long checkDurationMs) { this.checkDurationMs = checkDurationMs; }
}
