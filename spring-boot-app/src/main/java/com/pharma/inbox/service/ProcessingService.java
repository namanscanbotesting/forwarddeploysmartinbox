package com.pharma.inbox.service;

import com.pharma.inbox.dto.AiProcessingResponse;
import com.pharma.inbox.entity.*;
import com.pharma.inbox.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service to process incoming messages with AI classification and extraction.
 */
@Service
public class ProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingService.class);

    private final AiServiceClient aiServiceClient;
    private final IncomingMessageRepository messageRepository;
    private final ClassificationResultRepository classificationRepository;
    private final ExtractedFieldRepository extractedFieldRepository;
    private final ModelRunRepository modelRunRepository;
    private final AuditLogRepository auditLogRepository;

    public ProcessingService(
            AiServiceClient aiServiceClient,
            IncomingMessageRepository messageRepository,
            ClassificationResultRepository classificationRepository,
            ExtractedFieldRepository extractedFieldRepository,
            ModelRunRepository modelRunRepository,
            AuditLogRepository auditLogRepository) {
        this.aiServiceClient = aiServiceClient;
        this.messageRepository = messageRepository;
        this.classificationRepository = classificationRepository;
        this.extractedFieldRepository = extractedFieldRepository;
        this.modelRunRepository = modelRunRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Process a message asynchronously (via @Async).
     */
    @Async
    @Transactional
    public void processMessageAsync(Long messageId) {
        log.info("Starting async processing for message ID: {}", messageId);
        processMessage(messageId);
    }

    /**
     * Process a message synchronously (for testing/demo).
     */
    @Transactional
    public void processMessage(Long messageId) {
        log.info("Processing message ID: {}", messageId);

        // Log audit event
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("INCOMING_MESSAGE");
        auditLog.setEntityId(messageId);
        auditLog.setEventType("PROCESSING_STARTED");
        auditLog.setActor("SYSTEM");
        auditLog.setPayloadRef("message_id=" + messageId);
        auditLogRepository.save(auditLog);

        try {
            // Fetch message
            IncomingMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

            // Build content for AI processing
            StringBuilder content = new StringBuilder();
            content.append("Subject: ").append(message.getSubject()).append("\n");
            content.append("Sender: ").append(message.getSender()).append("\n");
            content.append("Body: ").append(message.getBodyText()).append("\n");

            // Add attachment content if present
            if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
                for (Attachment attachment : message.getAttachments()) {
                    content.append("\nAttachment: ").append(attachment.getFilename());
                    content.append(" (Type: ").append(attachment.getPdfType()).append(")\n");
                }
            }

            String metadata = "{\"messageId\": " + messageId + 
                             ", \"sender\": \"" + message.getSender() + "\"" +
                             ", \"receivedAt\": \"" + message.getReceivedAt() + "\"}";

            // Call AI service
            long startTime = System.currentTimeMillis();
            AiProcessingResponse aiResponse = aiServiceClient.processDocument(
                content.toString(), metadata);
            long processingTime = System.currentTimeMillis() - startTime;

            // Save model run
            ModelRun modelRun = new ModelRun();
            modelRun.setModelName("pharma-llm-v1");
            modelRun.setPromptVersion("1.0");
            modelRun.setInputRef("message_" + messageId);
            modelRun.setOutputRef("result_" + messageId);
            modelRun.setLatencyMs(processingTime);
            modelRun = modelRunRepository.save(modelRun);

            // Save classifications
            if (aiResponse.getClassifications() != null) {
                for (AiProcessingResponse.ClassificationDTO dto : aiResponse.getClassifications()) {
                    ClassificationResult result = new ClassificationResult();
                    result.setIncomingMessage(message);
                    result.setCategory(ClassificationResult.Category.valueOf(dto.getCategory()));
                    result.setConfidenceTier(ClassificationResult.ConfidenceTier.valueOf(dto.getConfidenceTier()));
                    result.setReason(dto.getReason());
                    result.setModelRun(modelRun);
                    result = classificationRepository.save(result);

                    // Log audit event
                    AuditLog classificationAudit = new AuditLog();
                    classificationAudit.setEntityType("CLASSIFICATION_RESULT");
                    classificationAudit.setEntityId(result.getId());
                    classificationAudit.setEventType("CLASSIFICATION_CREATED");
                    classificationAudit.setActor("AI_SERVICE");
                    classificationAudit.setPayloadRef("category=" + dto.getCategory());
                    auditLogRepository.save(classificationAudit);

                    log.info("Saved classification: {} with confidence {}", 
                        dto.getCategory(), dto.getConfidenceTier());
                }
            }

            // Log completion
            AuditLog completionAudit = new AuditLog();
            completionAudit.setEntityType("INCOMING_MESSAGE");
            completionAudit.setEntityId(messageId);
            completionAudit.setEventType("PROCESSING_COMPLETED");
            completionAudit.setActor("SYSTEM");
            completionAudit.setPayloadRef("processing_time_ms=" + processingTime);
            auditLogRepository.save(completionAudit);

            log.info("Completed processing message ID: {} in {}ms", messageId, processingTime);

        } catch (Exception e) {
            log.error("Error processing message ID: {}", messageId, e);
            
            // Log error audit event
            AuditLog errorAudit = new AuditLog();
            errorAudit.setEntityType("INCOMING_MESSAGE");
            errorAudit.setEntityId(messageId);
            errorAudit.setEventType("PROCESSING_ERROR");
            errorAudit.setActor("SYSTEM");
            errorAudit.setPayloadRef("error=" + e.getMessage());
            auditLogRepository.save(errorAudit);
            
            throw e;
        }
    }
}
