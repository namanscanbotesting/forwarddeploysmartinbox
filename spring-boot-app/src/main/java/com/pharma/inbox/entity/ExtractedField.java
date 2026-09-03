package com.pharma.inbox.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Extracted field with source-linked provenance.
 * Every fact must be traceable to its source.
 */
@Entity
@Table(name = "EXTRACTED_FIELD")
@Data
@NoArgsConstructor
public class ExtractedField {

    public enum FieldGroup {
        Patient,
        Reporter,
        Product,
        Reaction,
        Severity,
        Narrative,
        PQC,  // Quality Complaint specific
        MI    // Medical Information specific
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_result_id")
    private ClassificationResult classificationResult;

    @Column(name = "field_group", length = 100)
    @Enumerated(EnumType.STRING)
    private FieldGroup fieldGroup;

    @Column(name = "field_name", length = 200)
    private String fieldName;

    @Column(columnDefinition = "CLOB")
    private String fieldValue;

    @Column(name = "confidence_tier", length = 20)
    private String confidenceTier; // HIGH, MEDIUM, LOW

    @Column(name = "source_type", length = 50)
    private String sourceType; // email_body, pdf_page, attachment

    @Column(name = "source_ref", length = 500)
    private String sourceRef; // page #, char offset, table cell reference

    @Column(name = "verified_flag")
    private Boolean verifiedFlag = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_run_id")
    private ModelRun extractionRun;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
