package com.pharma.inbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Smart Inbox Assistant - Pharmacovigilance/Quality Intake System
 * 
 * Reads incoming emails + PDF attachments, classifies them into:
 * - Safety Report (ICSR)
 * - Quality Complaint (PQC)
 * - Info Request (MI)
 * - Not Relevant
 * 
 * Extracts structured fields with source-linked provenance.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SmartInboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartInboxApplication.class, args);
    }
}
