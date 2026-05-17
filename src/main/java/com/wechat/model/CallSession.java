package com.wechat.model;

import java.time.LocalDateTime;

public class CallSession {
    private Long id;
    private Long callerId;
    private Long calleeId;
    private String type;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public CallSession() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCallerId() {
        return callerId;
    }

    public void setCallerId(Long callerId) {
        this.callerId = callerId;
    }

    public Long getCalleeId() {
        return calleeId;
    }

    public void setCalleeId(Long calleeId) {
        this.calleeId = calleeId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    @Override
    public String toString() {
        return "CallSession{" +
                "id=" + id +
                ", callerId=" + callerId +
                ", calleeId=" + calleeId +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", startedAt=" + startedAt +
                ", endedAt=" + endedAt +
                '}';
    }
}