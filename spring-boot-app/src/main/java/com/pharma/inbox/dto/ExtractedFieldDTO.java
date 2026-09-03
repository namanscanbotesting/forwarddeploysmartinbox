package com.pharma.inbox.dto;

import lombok.Data;

/**
 * DTO for extracted fields with provenance.
 */
@Data
public class ExtractedFieldDTO {
    private Long id;
    private String fieldGroup;
    private String fieldName;
    private String fieldValue;
    private String confidenceTier;
    private String sourceType;
    private String sourceRef;
    private Boolean verifiedFlag;
}
