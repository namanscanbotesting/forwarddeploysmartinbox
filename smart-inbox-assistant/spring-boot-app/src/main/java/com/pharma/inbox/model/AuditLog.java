package com.pharma.inbox.model;

import java.sql.Clob;
import java.sql.Timestamp;

/**
 * Comprehensive audit log entry
 */
public class AuditLog extends BaseEntity {
    private String entityType;
    private Long entityId;
    private String eventType;
    private String actor;
    private Clob payloadRef;
    private Timestamp timestamp;

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Clob getPayloadRef() {
        return payloadRef;
    }

    public void setPayloadRef(Clob payloadRef) {
        this.payloadRef = payloadRef;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
