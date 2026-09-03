package com.pharma.inbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AiServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceClient.class);

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    @Value("${ai.service.timeout:300}")
    private int timeoutSeconds;

    @Value("${ai.service.api-key:your-api-key-here}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiServiceClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send document to AI service for classification and extraction
     * Two-stage process: extract then verify
     */
    public AiProcessingResult processDocument(String documentText, String pdfType, String language) {
        Instant startTime = Instant.now();

        try {
            // Stage 1: Classification + Extraction
            Map<String, Object> extractionRequest = new HashMap<>();
            extractionRequest.put("text", documentText);
            extractionRequest.put("pdf_type", pdfType != null ? pdfType : "digital");
            extractionRequest.put("language", language != null ? language : "en");
            extractionRequest.put("stage", "extract");

            AiProcessingResult extractionResult = callAiService(extractionRequest);

            if (extractionResult == null || !extractionResult.isSuccess()) {
                logger.error("AI extraction failed: {}", extractionResult != null ? extractionResult.getError() : "No response");
                return AiProcessingResult.failure("Extraction stage failed");
            }

            // Stage 2: Verification
            Map<String, Object> verificationRequest = new HashMap<>();
            verificationRequest.put("text", documentText);
            verificationRequest.put("extracted_fields", extractionResult.getExtractedFields());
            verificationRequest.put("classifications", extractionResult.getClassifications());
            verificationRequest.put("stage", "verify");

            AiProcessingResult verificationResult = callAiService(verificationRequest);

            if (verificationResult == null || !verificationResult.isSuccess()) {
                logger.warn("AI verification failed, using extraction results only: {}", 
                    verificationResult != null ? verificationResult.getError() : "No response");
                verificationResult = extractionResult;
            }

            // Calculate latency
            long latencyMs = Duration.between(startTime, Instant.now()).toMillis();
            verificationResult.setLatencyMs(latencyMs);

            logger.info("AI processing completed in {}ms", latencyMs);
            return verificationResult;

        } catch (Exception e) {
            logger.error("AI service call failed", e);
            return AiProcessingResult.failure("AI service error: " + e.getMessage());
        }
    }

    private AiProcessingResult callAiService(Map<String, Object> request) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(aiServiceUrl + "/api/process")
                .build().toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return objectMapper.readValue(response.getBody(), AiProcessingResult.class);
            } else {
                logger.error("AI service returned error status: {}", response.getStatusCode());
                return AiProcessingResult.failure("HTTP " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Failed to call AI service", e);
            return AiProcessingResult.failure("Connection error: " + e.getMessage());
        }
    }

    /**
     * Health check for AI service
     */
    public boolean isHealthy() {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(aiServiceUrl + "/api/health")
                .build().toUri();
            
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("AI service health check failed", e);
            return false;
        }
    }

    // Inner class for result handling
    public static class AiProcessingResult {
        private boolean success;
        private String error;
        private String[] classifications;
        private Map<String, Object>[] extractedFields;
        private String narrative;
        private Long latencyMs;
        private Map<String, Object> metadata;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String[] getClassifications() {
            return classifications;
        }

        public void setClassifications(String[] classifications) {
            this.classifications = classifications;
        }

        public Map<String, Object>[] getExtractedFields() {
            return extractedFields;
        }

        public void setExtractedFields(Map<String, Object>[] extractedFields) {
            this.extractedFields = extractedFields;
        }

        public String getNarrative() {
            return narrative;
        }

        public void setNarrative(String narrative) {
            this.narrative = narrative;
        }

        public Long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Long latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public static AiProcessingResult failure(String error) {
            AiProcessingResult result = new AiProcessingResult();
            result.setSuccess(false);
            result.setError(error);
            return result;
        }
    }
}
