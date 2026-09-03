package com.pharma.inbox.controller;

import com.pharma.inbox.dto.ClassificationResultDTO;
import com.pharma.inbox.dto.ExtractedFieldDTO;
import com.pharma.inbox.entity.*;
import com.pharma.inbox.repository.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for reviewer UI.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // For Angular dev server
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ClassificationResultRepository classificationRepository;
    private final ExtractedFieldRepository extractedFieldRepository;
    private final ReviewerActionRepository reviewerActionRepository;
    private final IncomingMessageRepository messageRepository;

    public ReviewController(
            ClassificationResultRepository classificationRepository,
            ExtractedFieldRepository extractedFieldRepository,
            ReviewerActionRepository reviewerActionRepository,
            IncomingMessageRepository messageRepository) {
        this.classificationRepository = classificationRepository;
        this.extractedFieldRepository = extractedFieldRepository;
        this.reviewerActionRepository = reviewerActionRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Get all classification results for the review queue.
     */
    @GetMapping("/classifications")
    public List<ClassificationResultDTO> getAllClassifications() {
        log.info("Fetching all classifications");
        
        List<ClassificationResult> results = classificationRepository.findAllOrderedByDate();
        
        return results.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get classification details with extracted fields.
     */
    @GetMapping("/classifications/{id}")
    public ClassificationResultDTO getClassification(@PathVariable Long id) {
        log.info("Fetching classification ID: {}", id);
        
        ClassificationResult result = classificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Classification not found: " + id));
        
        return toDTO(result);
    }

    /**
     * Submit reviewer decision (accept/override).
     */
    @PostMapping("/classifications/{id}/review")
    public ClassificationResultDTO submitReview(
            @PathVariable Long id,
            @RequestBody ReviewRequest request) {
        
        log.info("Submitting review for classification ID: {}, action: {}", id, request.getActionType());
        
        ClassificationResult result = classificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Classification not found: " + id));
        
        // Create reviewer action
        ReviewerAction action = new ReviewerAction();
        action.setClassificationResult(result);
        action.setReviewerId(request.getReviewerId());
        action.setActionType(ReviewerAction.ActionType.valueOf(request.getActionType()));
        action.setPreviousValue(request.getPreviousValue());
        action.setNewValue(request.getNewValue());
        reviewerActionRepository.save(action);
        
        // Log audit event
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("REVIEWER_ACTION");
        auditLog.setEntityId(action.getId());
        auditLog.setEventType("REVIEW_SUBMITTED");
        auditLog.setActor(request.getReviewerId());
        auditLog.setPayloadRef("action=" + request.getActionType());
        
        log.info("Review saved with action ID: {}", action.getId());
        
        return toDTO(result);
    }

    /**
     * Create a test message (for demo without email ingestion).
     */
    @PostMapping("/test/messages")
    public IncomingMessage createTestMessage(@RequestBody TestMessageRequest request) {
        log.info("Creating test message");
        
        IncomingMessage message = new IncomingMessage();
        message.setSender(request.getSender());
        message.setSubject(request.getSubject());
        message.setBodyText(request.getBodyText());
        message.setRawSourceRef("test_" + System.currentTimeMillis());
        
        message = messageRepository.save(message);
        log.info("Created test message ID: {}", message.getId());
        
        return message;
    }

    private ClassificationResultDTO toDTO(ClassificationResult result) {
        ClassificationResultDTO dto = new ClassificationResultDTO();
        dto.setId(result.getId());
        dto.setMessageId(result.getIncomingMessage() != null ? result.getIncomingMessage().getId() : null);
        dto.setSender(result.getIncomingMessage() != null ? result.getIncomingMessage().getSender() : null);
        dto.setSubject(result.getIncomingMessage() != null ? result.getIncomingMessage().getSubject() : null);
        dto.setCategory(result.getCategory() != null ? result.getCategory().name() : null);
        dto.setConfidenceTier(result.getConfidenceTier() != null ? result.getConfidenceTier().name() : null);
        dto.setReason(result.getReason());
        dto.setCreatedAt(result.getCreatedAt());
        
        // Load extracted fields
        List<ExtractedField> fields = extractedFieldRepository.findByClassificationResultId(result.getId());
        dto.setExtractedFields(fields.stream().map(this::toFieldDTO).collect(Collectors.toList()));
        
        return dto;
    }

    private ExtractedFieldDTO toFieldDTO(ExtractedField field) {
        ExtractedFieldDTO dto = new ExtractedFieldDTO();
        dto.setId(field.getId());
        dto.setFieldGroup(field.getFieldGroup() != null ? field.getFieldGroup().name() : null);
        dto.setFieldName(field.getFieldName());
        dto.setFieldValue(field.getFieldValue());
        dto.setConfidenceTier(field.getConfidenceTier());
        dto.setSourceType(field.getSourceType());
        dto.setSourceRef(field.getSourceRef());
        dto.setVerifiedFlag(field.getVerifiedFlag());
        return dto;
    }

    // Request DTOs
    @Data
    static class ReviewRequest {
        private String reviewerId;
        private String actionType; // ACCEPT or OVERRIDE
        private String previousValue;
        private String newValue;
    }

    @Data
    static class TestMessageRequest {
        private String sender;
        private String subject;
        private String bodyText;
    }
}
