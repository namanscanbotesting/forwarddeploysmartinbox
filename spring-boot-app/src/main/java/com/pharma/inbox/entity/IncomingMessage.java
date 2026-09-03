package com.pharma.inbox.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an incoming email message.
 */
@Entity
@Table(name = "INCOMING_MESSAGE")
@Data
@NoArgsConstructor
public class IncomingMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String sender;

    @Column(length = 1000)
    private String subject;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(columnDefinition = "CLOB")
    private String bodyText;

    @Column(name = "raw_source_ref", length = 500)
    private String rawSourceRef;

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(mappedBy = "incomingMessage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "incomingMessage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ClassificationResult> classificationResults = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
