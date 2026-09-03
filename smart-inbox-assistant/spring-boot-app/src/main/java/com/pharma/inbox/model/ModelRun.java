package com.pharma.inbox.model;

import java.sql.Clob;
import java.sql.Timestamp;

/**
 * Model run tracking - every AI decision traceable to input/output
 */
public class ModelRun extends BaseEntity {
    private String modelName;
    private String promptVersion;
    private Clob inputRef;
    private Clob outputRef;
    private Long latencyMs;
    private Timestamp createdAt;

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public Clob getInputRef() {
        return inputRef;
    }

    public void setInputRef(Clob inputRef) {
        this.inputRef = inputRef;
    }

    public Clob getOutputRef() {
        return outputRef;
    }

    public void setOutputRef(Clob outputRef) {
        this.outputRef = outputRef;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
