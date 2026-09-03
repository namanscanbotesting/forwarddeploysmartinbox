package com.pharma.inbox.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Tracks each AI model execution for traceability.
 */
@Entity
@Table(name = "MODEL_RUN")
@Data
@NoArgsConstructor
public class ModelRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", length = 200)
    private String modelName;

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    @Column(name = "input_ref", length = 500)
    private String inputRef;

    @Column(name = "output_ref", length = 500)
    private String outputRef;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
