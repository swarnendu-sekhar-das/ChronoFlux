package com.chronoflux.adminapi.dto;

public class SystemStatusDTO {
    private long activeEvents;
    private long pendingWebhooks;
    private long totalUsers;
    private String status;

    public SystemStatusDTO() {
    }

    public SystemStatusDTO(long activeEvents, long pendingWebhooks, long totalUsers, String status) {
        this.activeEvents = activeEvents;
        this.pendingWebhooks = pendingWebhooks;
        this.totalUsers = totalUsers;
        this.status = status;
    }

    public long getActiveEvents() {
        return activeEvents;
    }

    public void setActiveEvents(long activeEvents) {
        this.activeEvents = activeEvents;
    }

    public long getPendingWebhooks() {
        return pendingWebhooks;
    }

    public void setPendingWebhooks(long pendingWebhooks) {
        this.pendingWebhooks = pendingWebhooks;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
