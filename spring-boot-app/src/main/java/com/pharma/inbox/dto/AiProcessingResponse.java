package com.pharma.inbox.dto;

import lombok.Data;
import java.util.List;

/**
 * Response from AI service with classification and extraction results.
 */
@Data
public class AiProcessingResponse {
    private List<ClassificationDTO> classifications;
    private List<ExtractedFieldDataDTO> extractedFields;
    private String summary;
    private Long processingTimeMs;
    
    @Data
    public static class ClassificationDTO {
        private String category;
        private String confidenceTier;
        private String reason;
    }
    
    @Data
    public static class ExtractedFieldDataDTO {
        private String fieldGroup;
        private String fieldName;
        private String fieldValue;
        private String confidenceTier;
        private String sourceType;
        private String sourceRef;
        private Boolean verifiedFlag;
    }
}
