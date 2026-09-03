package com.pharma.inbox.model;

import java.sql.Clob;
import java.sql.Timestamp;

/**
 * Processing job for async queue
 */
public class ProcessingJob extends BaseEntity {
    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    private Long messageId;
    private String status;
    private String errorMessage;
    private Timestamp createdAt;
    private Timestamp startedAt;
    private Timestamp completedAt;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }
}
