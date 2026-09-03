package com.pharma.inbox.model;

import java.sql.Clob;
import java.sql.Timestamp;

/**
 * Reviewer actions - append-only audit log
 */
public class ReviewerAction extends BaseEntity {
    private Long classificationResultId;
    private String reviewerId;
    private String actionType; // accept, override
    private Clob previousValue;
    private Clob newValue;
    private String fieldName;
    private Timestamp timestamp;

    public Long getClassificationResultId() {
        return classificationResultId;
    }

    public void setClassificationResultId(Long classificationResultId) {
        this.classificationResultId = classificationResultId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Clob getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(Clob previousValue) {
        this.previousValue = previousValue;
    }

    public Clob getNewValue() {
        return newValue;
    }

    public void setNewValue(Clob newValue) {
        this.newValue = newValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
