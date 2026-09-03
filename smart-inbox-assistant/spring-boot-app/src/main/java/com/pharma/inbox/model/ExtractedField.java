package com.pharma.inbox.model;

import java.sql.Clob;
import java.sql.Timestamp;

/**
 * Extracted field with full provenance/traceability
 * Every extracted fact must be traceable to its source
 */
public class ExtractedField extends BaseEntity {
    public enum FieldGroup {
        Patient,
        Reporter,
        Product,
        Reaction,
        Severity,
        Narrative,
        PQC,
        MI
    }

    private Long classificationResultId;
    private String fieldGroup;
    private String fieldName;
    private Clob fieldValue;
    private String confidenceTier; // HIGH, MEDIUM, LOW
    private String sourceType; // email_body, pdf_page
    private String sourceRef; // page number, char offset, table cell reference
    private int verifiedFlag;
    private Long extractionRunId;

    public Long getClassificationResultId() {
        return classificationResultId;
    }

    public void setClassificationResultId(Long classificationResultId) {
        this.classificationResultId = classificationResultId;
    }

    public String getFieldGroup() {
        return fieldGroup;
    }

    public void setFieldGroup(String fieldGroup) {
        this.fieldGroup = fieldGroup;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Clob getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(Clob fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getConfidenceTier() {
        return confidenceTier;
    }

    public void setConfidenceTier(String confidenceTier) {
        this.confidenceTier = confidenceTier;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public int getVerifiedFlag() {
        return verifiedFlag;
    }

    public void setVerifiedFlag(int verifiedFlag) {
        this.verifiedFlag = verifiedFlag;
    }

    public Long getExtractionRunId() {
        return extractionRunId;
    }

    public void setExtractionRunId(Long extractionRunId) {
        this.extractionRunId = extractionRunId;
    }
}
