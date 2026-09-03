package com.pharma.inbox.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Reviewer actions (append-only audit trail).
 */
@Entity
@Table(name = "REVIEWER_ACTION")
@Data
@NoArgsConstructor
public class ReviewerAction {

    public enum ActionType {
        ACCEPT,
        OVERRIDE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_result_id")
    private ClassificationResult classificationResult;

    @Column(name = "reviewer_id", length = 100)
    private String reviewerId;

    @Column(name = "action_type", length = 50)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(columnDefinition = "CLOB")
    private String previousValue;

    @Column(columnDefinition = "CLOB")
    private String newValue;

    private Instant timestamp;

    @PrePersist
    protected void onPrePersist() {
        timestamp = Instant.now();
    }
}
