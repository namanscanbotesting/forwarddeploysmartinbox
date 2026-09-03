package com.pharma.inbox.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

/**
 * DTO for classification results with extracted fields.
 */
@Data
public class ClassificationResultDTO {
    private Long id;
    private Long messageId;
    private String sender;
    private String subject;
    private String category;
    private String confidenceTier;
    private String reason;
    private Instant createdAt;
    private List<ExtractedFieldDTO> extractedFields;
}
