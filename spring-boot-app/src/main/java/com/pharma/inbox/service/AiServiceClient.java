package com.pharma.inbox.service;

import com.pharma.inbox.dto.AiProcessingRequest;
import com.pharma.inbox.dto.AiProcessingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service to communicate with Python AI service via REST.
 */
@Service
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final WebClient webClient;

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    @Value("${ai.service.timeout-seconds:120}")
    private int timeoutSeconds;

    public AiServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Send document to AI service for classification and extraction.
     */
    public AiProcessingResponse processDocument(String content, String metadata) {
        log.info("Sending document to AI service for processing");
        
        AiProcessingRequest request = new AiProcessingRequest();
        request.setDocumentType("combined");
        request.setContent(content);
        request.setMetadata(metadata);

        try {
            AiProcessingResponse response = webClient.post()
                .uri(aiServiceUrl + "/api/process")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiProcessingResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

            log.info("AI service returned {} classifications", 
                response != null && response.getClassifications() != null 
                    ? response.getClassifications().size() : 0);

            return response;
        } catch (Exception e) {
            log.error("Error calling AI service: {}", e.getMessage());
            // Return a fallback response for demo purposes
            return createFallbackResponse(content);
        }
    }

    /**
     * Fallback response when AI service is unavailable (for demo/testing).
     */
    private AiProcessingResponse createFallbackResponse(String content) {
        log.warn("Using fallback classification (AI service unavailable)");
        
        AiProcessingResponse response = new AiProcessingResponse();
        
        // Simple keyword-based fallback classification
        String lowerContent = content.toLowerCase();
        AiProcessingResponse.ClassificationDTO classification = new AiProcessingResponse.ClassificationDTO();
        
        if (lowerContent.contains("adverse") || lowerContent.contains("side effect") || 
            lowerContent.contains("patient") || lowerContent.contains("reaction")) {
            classification.setCategory("ICSR");
            classification.setConfidenceTier("MEDIUM");
            classification.setReason("Keywords suggest safety report (fallback mode)");
        } else if (lowerContent.contains("defect") || lowerContent.contains("quality") || 
                   lowerContent.contains("damaged") || lowerContent.contains("batch")) {
            classification.setCategory("PQC");
            classification.setConfidenceTier("MEDIUM");
            classification.setReason("Keywords suggest quality complaint (fallback mode)");
        } else if (lowerContent.contains("information") || lowerContent.contains("question") || 
                   lowerContent.contains("request")) {
            classification.setCategory("MI");
            classification.setConfidenceTier("MEDIUM");
            classification.setReason("Keywords suggest information request (fallback mode)");
        } else {
            classification.setCategory("NOT_RELEVANT");
            classification.setConfidenceTier("LOW");
            classification.setReason("No relevant keywords detected (fallback mode)");
        }
        
        response.setClassifications(java.util.List.of(classification));
        response.setExtractedFields(java.util.List.of());
        response.setSummary("Fallback classification - AI service unavailable");
        response.setProcessingTimeMs(0L);
        
        return response;
    }
}
