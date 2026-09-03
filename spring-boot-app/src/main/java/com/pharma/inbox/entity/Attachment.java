package com.pharma.inbox.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Represents an email attachment (typically PDF).
 */
@Entity
@Table(name = "ATTACHMENT")
@Data
@NoArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private IncomingMessage incomingMessage;

    @Column(length = 500)
    private String filename;

    @Column(name = "mime_type", length = 200)
    private String mimeType;

    @Column(name = "pdf_type", length = 100)
    private String pdfType; // 'digital', 'scanned', 'article', 'non_english'

    @Column(name = "storage_ref", length = 500)
    private String storageRef;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "language_detected", length = 50)
    private String languageDetected;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
