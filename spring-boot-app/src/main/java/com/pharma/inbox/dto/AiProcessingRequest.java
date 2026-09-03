package com.pharma.inbox.dto;

import lombok.Data;

/**
 * Request to AI service for document processing.
 */
@Data
public class AiProcessingRequest {
    private String documentType; // 'email', 'pdf'
    private String content;      // text content
    private String metadata;     // JSON metadata
}
