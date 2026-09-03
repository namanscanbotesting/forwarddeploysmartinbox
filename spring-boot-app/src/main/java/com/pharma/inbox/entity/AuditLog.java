package com.pharma.inbox.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * System-wide audit log for traceability.
 */
@Entity
@Table(name = "AUDIT_LOG")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(length = 100)
    private String actor;

    @Column(name = "payload_ref", length = 500)
    private String payloadRef;

    private Instant timestamp;

    @PrePersist
    protected void onPrePersist() {
        timestamp = Instant.now();
    }
}
