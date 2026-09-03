package com.pharma.inbox.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Classification result for a message (multi-label).
 * Categories: ICSR, PQC, MI, NOT_RELEVANT
 */
@Entity
@Table(name = "CLASSIFICATION_RESULT")
@Data
@NoArgsConstructor
public class ClassificationResult {

    public enum Category {
        ICSR,      // Safety Report (Individual Case Safety Report)
        PQC,       // Product Quality Complaint
        MI,        // Medical Information Request
        NOT_RELEVANT
    }

    public enum ConfidenceTier {
        HIGH,
        MEDIUM,
        LOW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private IncomingMessage incomingMessage;

    @Column(length = 100)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "confidence_tier", length = 20)
    @Enumerated(EnumType.STRING)
    private ConfidenceTier confidenceTier;

    @Column(length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_run_id")
    private ModelRun modelRun;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
