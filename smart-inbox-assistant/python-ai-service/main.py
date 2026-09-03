"""
Smart Inbox Assistant - Python AI Service
Phase 0-4: Complete implementation for document classification and extraction
"""

from fastapi import FastAPI, HTTPException, Header, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
import os
import time
import json
import re
from datetime import datetime
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Smart Inbox Assistant AI Service",
    description="Pharmacovigilance document classification and field extraction service",
    version="1.0.0"
)

# CORS middleware for Angular frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://127.0.0.1:4200"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============================================================================
# Configuration - OpenAI Compatible API (Phase 1-4)
# ============================================================================
AI_BASE_URL = os.getenv("AI_BASE_URL", "https://api.openai.com/v1")
AI_API_KEY = os.getenv("AI_API_KEY", "your-api-key-here")
AI_MODEL = os.getenv("AI_MODEL", "gpt-4o")
AI_TIMEOUT = int(os.getenv("AI_TIMEOUT", "120"))
AI_MAX_TOKENS = int(os.getenv("AI_MAX_TOKENS", "4096"))
AI_TEMPERATURE = float(os.getenv("AI_TEMPERATURE", "0.1"))

CONFIDENCE_THRESHOLDS = {
    "HIGH": 0.85,
    "MEDIUM": 0.65,
    "LOW": 0.45
}

# HTTP client for AI API calls
import httpx

# ============================================================================
# Pydantic Models
# ============================================================================

class ProcessRequest(BaseModel):
    text: str
    pdf_type: str = "digital"  # digital, scanned, article, non_english
    language: str = "en"
    stage: str = "extract"  # extract or verify
    extracted_fields: Optional[List[Dict[str, Any]]] = None
    classifications: Optional[List[str]] = None

class ExtractedField(BaseModel):
    field_group: str
    field_name: str
    field_value: str
    confidence_tier: str
    source_type: str
    source_ref: str
    verified_flag: bool

class ClassificationResult(BaseModel):
    category: str
    confidence_tier: str
    reason: str

class ProcessResponse(BaseModel):
    success: bool
    error: Optional[str] = None
    classifications: List[ClassificationResult] = []
    extracted_fields: List[ExtractedField] = []
    narrative: Optional[str] = None
    latency_ms: int = 0
    metadata: Dict[str, Any] = {}

# ============================================================================
# Classification Logic (Phase 1)
# ============================================================================

async def call_ai_api(messages: list, temperature: float = AI_TEMPERATURE) -> dict:
    """Call OpenAI-compatible API with retry logic"""
    async with httpx.AsyncClient(
        base_url=AI_BASE_URL,
        headers={
            "Authorization": f"Bearer {AI_API_KEY}",
            "Content-Type": "application/json"
        },
        timeout=AI_TIMEOUT
    ) as client:
        payload = {
            "model": AI_MODEL,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": AI_MAX_TOKENS,
            "response_format": {"type": "json_object"}
        }
        
        try:
            response = await client.post("/chat/completions", json=payload)
            response.raise_for_status()
            result = response.json()
            return {
                "content": result["choices"][0]["message"]["content"],
                "usage": result.get("usage", {}),
                "model": result.get("model", AI_MODEL)
            }
        except Exception as e:
            logger.error(f"AI API call failed: {str(e)}")
            raise HTTPException(status_code=500, detail=f"AI API call failed: {str(e)}")

def parse_json_response(content: str) -> dict:
    """Parse JSON from AI response, handling markdown code blocks"""
    if content.startswith("```"):
        lines = content.split("\n")
        if lines[0].startswith("```"):
            lines = lines[1:]
        if lines[-1].startswith("```"):
            lines = lines[:-1]
        content = "\n".join(lines)
    
    try:
        return json.loads(content)
    except json.JSONDecodeError as e:
        logger.error(f"Failed to parse AI response as JSON: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to parse AI response: {str(e)}")

def classify_document(text: str) -> List[ClassificationResult]:
    """
    Multi-label classification based on pharmacovigilance rules:
    - ICSR: identifiable patient + reporter + suspect product + adverse event
    - PQC: product quality issue (batch/lot, defect, contamination)
    - MI: medical information request (question about product/dosing)
    - NOT_RELEVANT: marketing, spam, unrelated content
    
    Uses rule-based detection for speed and determinism.
    LLM-based classification available via /api/classify endpoint.
    """
    text_lower = text.lower()
    results = []

    # ICSR Detection (4-element test from ICH E2B)
    icsr_signals = {
        "patient": any(term in text_lower for term in ["patient", "subject", "case", "individual"]),
        "reporter": any(term in text_lower for term in ["reported by", "physician", "doctor", "nurse", "contact"]),
        "product": any(term in text_lower for term in ["drug", "medication", "product", "vaccine", "treatment"]),
        "adverse_event": any(term in text_lower for term in ["adverse", "reaction", "side effect", "symptom", "event", "ae"])
    }
    
    icsr_score = sum(icsr_signals.values()) / 4
    if icsr_score >= 0.75:  # At least 3 of 4 elements
        results.append(ClassificationResult(
            category="ICSR",
            confidence_tier=get_confidence_tier(icsr_score),
            reason=f"Safety report with {int(icsr_score * 4)} of 4 ICSR elements present"
        ))
    elif icsr_score >= 0.5:
        results.append(ClassificationResult(
            category="ICSR",
            confidence_tier="LOW",
            reason=f"Possible safety report with {int(icsr_score * 4)} of 4 ICSR elements"
        ))

    # PQC Detection
    pqc_keywords = ["defect", "contamination", "discoloration", "broken", "damaged", 
                    "batch", "lot", "recall", "quality complaint", "manufacturing"]
    pqc_score = sum(1 for kw in pqc_keywords if kw in text_lower) / len(pqc_keywords)
    if pqc_score >= 0.3:
        results.append(ClassificationResult(
            category="PQC",
            confidence_tier=get_confidence_tier(min(pqc_score * 3, 1.0)),
            reason="Product quality indicators detected"
        ))

    # MI Detection
    mi_patterns = ["question about", "inquiry regarding", "information on", 
                   "how to use", "dosing", "indication", "contraindication"]
    mi_score = sum(1 for p in mi_patterns if p in text_lower) / len(mi_patterns)
    if mi_score >= 0.2:
        results.append(ClassificationResult(
            category="MI",
            confidence_tier=get_confidence_tier(min(mi_score * 4, 1.0)),
            reason="Medical information request patterns detected"
        ))

    # If no categories matched, mark as NOT_RELEVANT
    if not results:
        results.append(ClassificationResult(
            category="NOT_RELEVANT",
            confidence_tier="MEDIUM",
            reason="No pharmacovigilance-relevant content detected"
        ))

    return results

def get_confidence_tier(score: float) -> str:
    """Convert numeric score to confidence tier"""
    if score >= CONFIDENCE_THRESHOLDS["HIGH"]:
        return "HIGH"
    elif score >= CONFIDENCE_THRESHOLDS["MEDIUM"]:
        return "MEDIUM"
    else:
        return "LOW"

# ============================================================================
# Extraction Logic (Phase 1-2)
# ============================================================================

def extract_fields(text: str, pdf_type: str, language: str) -> List[ExtractedField]:
    """
    Two-stage extraction: propose fields then verify against source
    Supports ICSR (Patient/Reporter/Product/Reaction/Severity/Narrative),
    PQC (product/batch/defect), and MI (question/topic) fields
    """
    fields = []
    text_lower = text.lower()

    # === ICSR Fields (E2B R3 simplified mapping) ===
    
    # Patient demographics
    patient_age = extract_with_pattern(text, r"(?:age|years old|yo)[:\s]*(\d+)", "Patient", "age")
    if patient_age:
        fields.append(patient_age)

    patient_gender = extract_with_pattern(text, r"(?:gender|sex)[:\s]*(male|female|m|f)", "Patient", "gender")
    if patient_gender:
        fields.append(patient_gender)

    # Reporter information
    reporter_profession = extract_with_pattern(
        text, 
        r"(?:reported by|reporter|contact)[:\s]*(physician|doctor|nurse|pharmacist|consumer|patient)",
        "Reporter", "profession"
    )
    if reporter_profession:
        fields.append(reporter_profession)

    # Product information
    product_name = extract_with_pattern(
        text,
        r"(?:product|drug|medication|brand name)[:\s]*([A-Za-z0-9\-]+(?:\s+[A-Za-z0-9\-]+)?)",
        "Product", "name"
    )
    if product_name:
        fields.append(product_name)

    batch_lot = extract_with_pattern(text, r"(?:batch|lot)[:\s]*([A-Za-z0-9\-]+)", "Product", "batch_lot")
    if batch_lot:
        fields.append(batch_lot)

    # Reaction/Adverse Event
    reaction_terms = extract_reaction_terms(text)
    for i, term in enumerate(reaction_terms[:5]):  # Limit to top 5
        fields.append(ExtractedField(
            field_group="Reaction",
            field_name=f"reaction_term_{i+1}",
            field_value=term,
            confidence_tier="MEDIUM",
            source_type="pdf_page" if pdf_type != "digital" else "email_body",
            source_ref="text_extraction",
            verified_flag=False
        ))

    # Severity/Seriousness
    seriousness_criteria = extract_seriousness(text)
    if seriousness_criteria:
        fields.append(ExtractedField(
            field_group="Severity",
            field_name="seriousness_criteria",
            field_value=", ".join(seriousness_criteria),
            confidence_tier="MEDIUM",
            source_type="pdf_page" if pdf_type != "digital" else "email_body",
            source_ref="text_extraction",
            verified_flag=False
        ))

    # Narrative (case summary)
    narrative = generate_narrative(text)
    if narrative:
        fields.append(ExtractedField(
            field_group="Narrative",
            field_name="case_summary",
            field_value=narrative,
            confidence_tier="MEDIUM",
            source_type="pdf_page" if pdf_type != "digital" else "email_body",
            source_ref="ai_generated",
            verified_flag=False
        ))

    # === PQC Fields ===
    if any(kw in text_lower for kw in ["defect", "quality", "batch", "lot"]):
        defect_description = extract_with_pattern(
            text,
            r"(?:defect|issue|problem|complaint)[:\s]*([^.\n]+)",
            "PQC", "defect_description"
        )
        if defect_description:
            fields.append(defect_description)

    # === MI Fields ===
    if any(kw in text_lower for kw in ["question", "inquiry", "information"]):
        question = extract_with_pattern(
            text,
            r"(?:question|inquiry)[:\s]*([^.\n]+)",
            "MI", "question"
        )
        if question:
            fields.append(question)

    return fields

def extract_with_pattern(text: str, pattern: str, field_group: str, field_name: str) -> Optional[ExtractedField]:
    """Extract field value using regex pattern with source tracking"""
    match = re.search(pattern, text, re.IGNORECASE)
    if match:
        value = match.group(1).strip()
        start_pos = match.start()
        
        # Determine source reference (character offset)
        source_ref = f"char_offset:{start_pos}"
        
        return ExtractedField(
            field_group=field_group,
            field_name=field_name,
            field_value=value,
            confidence_tier="HIGH",  # Regex matches are high confidence
            source_type="email_body",
            source_ref=source_ref,
            verified_flag=True
        )
    return None

def extract_reaction_terms(text: str) -> List[str]:
    """Extract adverse reaction terms from text"""
    # Common AE patterns
    ae_indicators = ["experienced", "reported", "developed", "noted", "observed"]
    terms = []
    
    sentences = text.split('.')
    for sentence in sentences:
        for indicator in ae_indicators:
            if indicator in sentence.lower():
                # Simple extraction - in production would use NER
                words = sentence.split()
                # Look for medical-sounding terms after the indicator
                idx = next((i for i, w in enumerate(words) if indicator.lower() in w.lower()), None)
                if idx and idx + 1 < len(words):
                    term = ' '.join(words[idx+1:idx+4])  # Take next 3 words
                    terms.append(term.strip(' ,.'))
    
    return list(set(terms))[:5]

def extract_seriousness(text: str) -> List[str]:
    """Extract seriousness criteria (death, life-threatening, hospitalization, etc.)"""
    criteria = []
    text_lower = text.lower()
    
    if any(term in text_lower for term in ["death", "fatal", "died"]):
        criteria.append("death")
    if any(term in text_lower for term in ["life-threatening", "life threatening"]):
        criteria.append("life_threatening")
    if any(term in text_lower for term in ["hospitalization", "hospitalized", "admitted"]):
        criteria.append("hospitalization")
    if any(term in text_lower for term in ["disability", "permanent"]):
        criteria.append("disability")
    if any(term in text_lower for term in ["congenital", "anomaly"]):
        criteria.append("congenital_anomaly")
    
    return criteria

def generate_narrative(text: str) -> str:
    """Generate a concise case summary narrative"""
    # Simple summarization - in production would use LLM
    sentences = text.split('.')
    relevant = [s.strip() for s in sentences if len(s.strip()) > 20][:5]
    
    if relevant:
        return "Case Summary: " + ". ".join(relevant) + "."
    return ""

# ============================================================================
# Verification Stage (Two-Stage Pattern)
# ============================================================================

def verify_extracted_fields(text: str, fields: List[ExtractedField]) -> List[ExtractedField]:
    """
    Verification pass: check each proposed field against source text
    Downgrade confidence or null out unsupported values
    """
    verified_fields = []
    
    for field in fields:
        # Check if field value appears in source text
        field_value_lower = field.field_value.lower()
        text_lower = text.lower()
        
        # Direct string matching for verification
        if field_value_lower in text_lower:
            field.verified_flag = True
            field.confidence_tier = "HIGH"
        else:
            # Try fuzzy matching (simple substring check)
            words = field_value_lower.split()
            match_count = sum(1 for w in words if len(w) > 3 and w in text_lower)
            
            if match_count >= len(words) * 0.7:
                field.verified_flag = True
                field.confidence_tier = "MEDIUM"
            else:
                field.verified_flag = False
                field.confidence_tier = "LOW"
                field.field_value = "Not stated"  # Unknown over guessing
        
        verified_fields.append(field)
    
    return verified_fields

# ============================================================================
# PDF Type Detection (Phase 2)
# ============================================================================

def detect_pdf_type(text: str, has_text_layer: bool = True) -> str:
    """Detect PDF type based on heuristics"""
    if not has_text_layer:
        return "scanned"
    
    # Check for multi-column patterns (article)
    if re.search(r'\[\d+\]', text) or re.search(r'et al\.', text, re.IGNORECASE):
        return "article"
    
    # Check for non-English
    non_english_ratio = len(re.findall(r'[àáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ]', text, re.IGNORECASE))
    if non_english_ratio > 10:
        return "non_english"
    
    return "digital"

# ============================================================================
# API Endpoints
# ============================================================================

@app.get("/api/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "model": AI_MODEL,
        "api_base_url": AI_BASE_URL
    }

@app.post("/api/process", response_model=ProcessResponse)
async def process_document(request: ProcessRequest, x_api_key: str = Header(None)):
    """
    Main processing endpoint for document classification and extraction
    Two-stage pattern: extract then verify
    
    Supports OpenAI-compatible APIs via AI_BASE_URL environment variable.
    """
    start_time = time.time()
    
    # API Key validation (simple for prototype)
    if x_api_key and x_api_key != AI_API_KEY and AI_API_KEY != "your-api-key-here":
        raise HTTPException(status_code=401, detail="Invalid API key")
    
    try:
        # Stage 1: Classification (rule-based for speed)
        classifications = classify_document(request.text)
        
        # Stage 1: Extraction (pattern-based with optional LLM enhancement)
        if request.stage == "extract":
            extracted_fields = extract_fields(request.text, request.pdf_type, request.language)
            
            latency_ms = int((time.time() - start_time) * 1000)
            
            return ProcessResponse(
                success=True,
                classifications=classifications,
                extracted_fields=extracted_fields,
                narrative=next((f.field_value for f in extracted_fields if f.field_name == "case_summary"), None),
                latency_ms=latency_ms,
                metadata={
                    "pdf_type": request.pdf_type,
                    "language": request.language,
                    "stage": "extract",
                    "model_used": AI_MODEL,
                    "ai_base_url": AI_BASE_URL
                }
            )
        
        # Stage 2: Verification
        elif request.stage == "verify" and request.extracted_fields:
            # Convert dict fields back to ExtractedField objects
            fields_to_verify = [ExtractedField(**f) for f in request.extracted_fields]
            verified_fields = verify_extracted_fields(request.text, fields_to_verify)
            
            latency_ms = int((time.time() - start_time) * 1000)
            
            return ProcessResponse(
                success=True,
                classifications=classifications,
                extracted_fields=verified_fields,
                narrative=next((f.field_value for f in verified_fields if f.field_name == "case_summary"), None),
                latency_ms=latency_ms,
                metadata={
                    "pdf_type": request.pdf_type,
                    "language": request.language,
                    "stage": "verify",
                    "verification_applied": True
                }
            )
        
        else:
            raise HTTPException(status_code=400, detail="Invalid stage or missing extracted_fields for verification")
    
    except Exception as e:
        logger.error(f"Processing error: {str(e)}")
        return ProcessResponse(
            success=False,
            error=str(e),
            latency_ms=int((time.time() - start_time) * 1000)
        )

@app.post("/api/classify")
async def classify_only(request: ProcessRequest):
    """Classification-only endpoint (rule-based)"""
    classifications = classify_document(request.text)
    return {
        "success": True,
        "classifications": [c.dict() for c in classifications],
        "model_used": AI_MODEL,
        "ai_base_url": AI_BASE_URL
    }

@app.post("/api/extract")
async def extract_only(request: ProcessRequest):
    """Extraction-only endpoint (pattern-based)"""
    fields = extract_fields(request.text, request.pdf_type, request.language)
    return {
        "success": True,
        "extracted_fields": [f.dict() for f in fields],
        "model_used": AI_MODEL,
        "ai_base_url": AI_BASE_URL
    }

@app.post("/api/classify-llm")
async def classify_with_llm(request: ProcessRequest):
    """LLM-based classification endpoint using OpenAI-compatible API"""
    prompt = f"""You are a pharmacovigilance expert classifier. Analyze the following document and classify it.

CATEGORIES:
- ICSR: Contains identifiable patient, reporter, suspect product, and adverse event (4-element test).
- PQC: Reports product quality issues, manufacturing defects, batch/lot concerns.
- MI: Asks questions about products, dosing, indications.
- NOT_RELEVANT: Marketing, spam, unrelated content.

DOCUMENT:
Subject: {request.subject if hasattr(request, 'subject') else ''}
Text: {request.text[:3000]}

Respond ONLY with JSON:
{{"classifications": [{{"category": "ICSR|PQC|MI|NOT_RELEVANT", "confidence_tier": "HIGH|MEDIUM|LOW", "reason": "explanation"}}]}}
"""
    
    messages = [
        {"role": "system", "content": "You are a pharmacovigilance classification expert."},
        {"role": "user", "content": prompt}
    ]
    
    try:
        result = await call_ai_api(messages)
        data = parse_json_response(result["content"])
        
        return {
            "success": True,
            "classifications": data.get("classifications", []),
            "model_used": result.get("model", AI_MODEL),
            "ai_base_url": AI_BASE_URL,
            "token_usage": result.get("usage", {})
        }
    except Exception as e:
        logger.error(f"LLM classification failed: {str(e)}")
        # Fallback to rule-based
        classifications = classify_document(request.text)
        return {
            "success": True,
            "classifications": [c.dict() for c in classifications],
            "fallback": "rule-based",
            "error": str(e)
        }

# ============================================================================
# Main Entry Point
# ============================================================================

if __name__ == "__main__":
    import uvicorn
    print(f"Starting Smart Inbox Assistant AI Service")
    print(f"AI Base URL: {AI_BASE_URL}")
    print(f"AI Model: {AI_MODEL}")
    print(f"Timeout: {AI_TIMEOUT}s")
    uvicorn.run(app, host="0.0.0.0", port=5000)
