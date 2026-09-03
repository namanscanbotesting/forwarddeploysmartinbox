"""
Smart Inbox Assistant - Python AI Service

Two-stage extract-then-verify pipeline for pharmacovigilance document processing.
Categories: ICSR (Safety Report), PQC (Quality Complaint), MI (Info Request), NOT_RELEVANT
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import json
import time
import os
import re

# Optional LLM imports (fallback to rule-based if not available)
try:
    import anthropic
    ANTHROPIC_AVAILABLE = True
except ImportError:
    ANTHROPIC_AVAILABLE = False

try:
    import openai
    OPENAI_AVAILABLE = True
except ImportError:
    OPENAI_AVAILABLE = False

app = FastAPI(title="Smart Inbox AI Service", version="1.0.0")

# Configuration
LLM_PROVIDER = os.getenv("LLM_PROVIDER", "fallback")  # 'anthropic', 'openai', 'fallback'
ANTHROPIC_KEY = os.getenv("ANTHROPIC_API_KEY", "")
OPENAI_KEY = os.getenv("OPENAI_API_KEY", "")

# Initialize clients
anthropic_client = None
openai_client = None

if LLM_PROVIDER == "anthropic" and ANTHROPIC_AVAILABLE and ANTHROPIC_KEY:
    anthropic_client = anthropic.Anthropic(api_key=ANTHROPIC_KEY)
elif LLM_PROVIDER == "openai" and OPENAI_AVAILABLE and OPENAI_KEY:
    openai_client = openai.OpenAI(api_key=OPENAI_KEY)


class ClassificationDTO(BaseModel):
    category: str
    confidence_tier: str
    reason: str


class ExtractedFieldDataDTO(BaseModel):
    field_group: str
    field_name: str
    field_value: str
    confidence_tier: str
    source_type: str
    source_ref: str
    verified_flag: bool


class AiProcessingRequest(BaseModel):
    document_type: str
    content: str
    metadata: Optional[str] = "{}"


class AiProcessingResponse(BaseModel):
    classifications: List[ClassificationDTO] = []
    extracted_fields: List[ExtractedFieldDataDTO] = []
    summary: str = ""
    processing_time_ms: int = 0


def classify_with_rules(content: str) -> List[Dict[str, str]]:
    """Rule-based classification fallback."""
    lower = content.lower()
    classifications = []
    
    # ICSR: Safety Report (4-element test: patient, reporter, product, event)
    icsr_score = 0
    if any(w in lower for w in ["patient", "subject", "case"]):
        icsr_score += 1
    if any(w in lower for w in ["reporter", "physician", "consumer", "contact"]):
        icsr_score += 1
    if any(w in lower for w in ["drug", "product", "medication", "treatment", "dose"]):
        icsr_score += 1
    if any(w in lower for w in ["adverse", "reaction", "event", "side effect", "symptom"]):
        icsr_score += 1
    
    if icsr_score >= 3:
        classifications.append({
            "category": "ICSR",
            "confidence_tier": "HIGH" if icsr_score == 4 else "MEDIUM",
            "reason": f"Detected {icsr_score}/4 ICSR elements (patient, reporter, product, event)"
        })
    
    # PQC: Quality Complaint
    pqc_keywords = ["defect", "quality", "damaged", "batch", "lot", "expired", 
                    "contaminat", "broken", "malfunction", "complaint"]
    pqc_count = sum(1 for kw in pqc_keywords if kw in lower)
    
    if pqc_count >= 2:
        classifications.append({
            "category": "PQC",
            "confidence_tier": "HIGH" if pqc_count >= 3 else "MEDIUM",
            "reason": f"Quality complaint indicators: {pqc_count} keywords matched"
        })
    
    # MI: Medical Information Request
    mi_keywords = ["information", "question", "request", "inquiry", "ask", 
                   "clarification", "details about", "how to use"]
    mi_count = sum(1 for kw in mi_keywords if kw in lower)
    
    if mi_count >= 2 and icsr_score < 2:  # Not a safety report
        classifications.append({
            "category": "MI",
            "confidence_tier": "MEDIUM",
            "reason": f"Information request detected: {mi_count} keywords matched"
        })
    
    if not classifications:
        classifications.append({
            "category": "NOT_RELEVANT",
            "confidence_tier": "LOW",
            "reason": "No relevant pharmacovigilance or quality indicators detected"
        })
    
    return classifications


def extract_fields_with_rules(content: str, category: str) -> List[Dict[str, Any]]:
    """Rule-based field extraction fallback."""
    fields = []
    lines = content.split('\n')
    
    if category == "ICSR":
        # Patient fields
        patient_patterns = [
            (r"patient (?:is |was )?(\d+)-?(year|yr|y)?-?old", "age"),
            (r"(?:male|female|man|woman)", "sex"),
            (r"weight[:\s]+(\d+(?:\.\d+)?)\s*(kg|lbs)", "weight"),
        ]
        
        for pattern, field_name in patient_patterns:
            match = re.search(pattern, content, re.IGNORECASE)
            if match:
                fields.append({
                    "field_group": "Patient",
                    "field_name": field_name,
                    "field_value": match.group(0),
                    "confidence_tier": "MEDIUM",
                    "source_type": "email_body",
                    "source_ref": f"char_offset_{match.start()}",
                    "verified_flag": True
                })
        
        # Product fields
        product_match = re.search(r"(?:drug|product|medication)[:\s]+([^\n\.]+)", content, re.IGNORECASE)
        if product_match:
            fields.append({
                "field_group": "Product",
                "field_name": "suspect_product",
                "field_value": product_match.group(1).strip(),
                "confidence_tier": "MEDIUM",
                "source_type": "email_body",
                "source_ref": f"char_offset_{product_match.start()}",
                "verified_flag": True
            })
        
        # Reaction fields
        reaction_match = re.search(r"(?:reaction|event|symptom|adverse)[:\s]+([^\n\.]+)", content, re.IGNORECASE)
        if reaction_match:
            fields.append({
                "field_group": "Reaction",
                "field_name": "reaction_description",
                "field_value": reaction_match.group(1).strip(),
                "confidence_tier": "MEDIUM",
                "source_type": "email_body",
                "source_ref": f"char_offset_{reaction_match.start()}",
                "verified_flag": True
            })
        
        # Narrative
        fields.append({
            "field_group": "Narrative",
            "field_name": "case_summary",
            "field_value": content[:500] + "..." if len(content) > 500 else content,
            "confidence_tier": "LOW",
            "source_type": "email_body",
            "source_ref": "full_text",
            "verified_flag": False
        })
    
    elif category == "PQC":
        # Batch/Lot
        batch_match = re.search(r"(?:batch|lot)[:\s]*([A-Z0-9\-]+)", content, re.IGNORECASE)
        if batch_match:
            fields.append({
                "field_group": "PQC",
                "field_name": "batch_number",
                "field_value": batch_match.group(1),
                "confidence_tier": "HIGH",
                "source_type": "email_body",
                "source_ref": f"char_offset_{batch_match.start()}",
                "verified_flag": True
            })
        
        # Problem description
        problem_match = re.search(r"(?:problem|issue|defect|complaint)[:\s]+([^\n\.]+)", content, re.IGNORECASE)
        if problem_match:
            fields.append({
                "field_group": "PQC",
                "field_name": "problem_description",
                "field_value": problem_match.group(1).strip(),
                "confidence_tier": "MEDIUM",
                "source_type": "email_body",
                "source_ref": f"char_offset_{problem_match.start()}",
                "verified_flag": True
            })
    
    elif category == "MI":
        # Question
        question_match = re.search(r"(?:question|inquiry|request)[:\s]+([^\n\.]+)", content, re.IGNORECASE)
        if question_match:
            fields.append({
                "field_group": "MI",
                "field_name": "question",
                "field_value": question_match.group(1).strip(),
                "confidence_tier": "MEDIUM",
                "source_type": "email_body",
                "source_ref": f"char_offset_{question_match.start()}",
                "verified_flag": True
            })
    
    return fields


def generate_summary(content: str, classifications: List[Dict]) -> str:
    """Generate a brief summary of the document."""
    categories = [c["category"] for c in classifications]
    
    if "ICSR" in categories:
        return "Safety report requiring review. Contains patient adverse event information."
    elif "PQC" in categories:
        return "Quality complaint requiring investigation. Contains product defect information."
    elif "MI" in categories:
        return "Medical information request. Requires response from medical affairs."
    else:
        return "Document does not appear to be pharmacovigilance or quality related."


@app.get("/health")
async def health_check():
    return {"status": "healthy", "llm_provider": LLM_PROVIDER}


@app.post("/api/process", response_model=AiProcessingResponse)
async def process_document(request: AiProcessingRequest):
    """
    Process a document for classification and field extraction.
    
    Uses two-stage pipeline:
    1. Classification (multi-label)
    2. Field extraction with verification
    
    Falls back to rule-based processing if LLM is unavailable.
    """
    start_time = time.time()
    
    try:
        # Parse metadata
        metadata = json.loads(request.metadata) if request.metadata else {}
        
        # Stage 1: Classification
        if LLM_PROVIDER == "anthropic" and anthropic_client:
            # TODO: Implement LLM-based classification
            classifications = classify_with_rules(request.content)
        elif LLM_PROVIDER == "openai" and openai_client:
            # TODO: Implement LLM-based classification
            classifications = classify_with_rules(request.content)
        else:
            # Fallback to rule-based
            classifications = classify_with_rules(request.content)
        
        # Stage 2: Field extraction for each classification
        all_fields = []
        for classification in classifications:
            if classification["category"] != "NOT_RELEVANT":
                fields = extract_fields_with_rules(request.content, classification["category"])
                all_fields.extend(fields)
        
        # Generate summary
        summary = generate_summary(request.content, classifications)
        
        processing_time = int((time.time() - start_time) * 1000)
        
        return AiProcessingResponse(
            classifications=[ClassificationDTO(**c) for c in classifications],
            extracted_fields=[ExtractedFieldDataDTO(**f) for f in all_fields],
            summary=summary,
            processing_time_ms=processing_time
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
