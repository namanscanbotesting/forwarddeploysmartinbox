package com.pharma.inbox.model;

import java.sql.Timestamp;

/**
 * Classification result for a message (multi-label support)
 * Categories: ICSR (Safety Report), PQC (Quality Complaint), MI (Info Request), NOT_RELEVANT
 */
public class ClassificationResult extends BaseEntity {
    public enum Category {
        ICSR,      // Individual Case Safety Report
        PQC,       // Product Quality Complaint
        MI,        // Medical Information Request
        NOT_RELEVANT
    }

    public enum ConfidenceTier {
        HIGH,
        MEDIUM,
        LOW
    }

    private Long messageId;
    private String category;
    private String confidenceTier;
    private String reason;
    private Long modelRunId;
    private Timestamp createdAt;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getConfidenceTier() {
        return confidenceTier;
    }

    public void setConfidenceTier(String confidenceTier) {
        this.confidenceTier = confidenceTier;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getModelRunId() {
        return modelRunId;
    }

    public void setModelRunId(Long modelRunId) {
        this.modelRunId = modelRunId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
