package com.pharma.inbox.model;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Base entity with common audit fields
 */
public abstract class BaseEntity {
    private Long id;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void markCreated() {
        this.createdAt = Timestamp.from(Instant.now());
        this.updatedAt = Timestamp.from(Instant.now());
    }

    public void markUpdated() {
        this.updatedAt = Timestamp.from(Instant.now());
    }
}
