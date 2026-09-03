package com.pharma.inbox.model;

import java.sql.Clob;
import java.sql.Timestamp;

/**
 * Incoming email message from monitored mailbox
 */
public class IncomingMessage extends BaseEntity {
    private String sender;
    private String subject;
    private Timestamp receivedAt;
    private Clob bodyText;
    private String rawSourceRef;
    private int processedFlag;

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Timestamp getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Timestamp receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Clob getBodyText() {
        return bodyText;
    }

    public void setBodyText(Clob bodyText) {
        this.bodyText = bodyText;
    }

    public String getRawSourceRef() {
        return rawSourceRef;
    }

    public void setRawSourceRef(String rawSourceRef) {
        this.rawSourceRef = rawSourceRef;
    }

    public int getProcessedFlag() {
        return processedFlag;
    }

    public void setProcessedFlag(int processedFlag) {
        this.processedFlag = processedFlag;
    }
}
