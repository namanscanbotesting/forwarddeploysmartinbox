# Smart Inbox Assistant - Complete Deployment Guide (Phases 0-4)

## Overview

This is a complete pharmacovigilance/quality intake system implementing all Phases 0-4:
- **Phase 0**: Skeleton & data model with full audit trail
- **Phase 1**: Core classification + extraction pipeline
- **Phase 2**: PDF variety handling (scanned, article, non-English)
- **Phase 3**: Batch processing, logging, and polish
- **Phase 4**: Literature screening bonus

**Stack:** Angular 17 → Spring Boot 3.2/Java 17 → Python FastAPI → Oracle Database

---

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐     ┌─────────────┐
│   Angular UI    │────▶│  Spring Boot API │────▶│  Python AI Svc  │────▶│   Oracle    │
│   (Port 4200)   │     │   (Port 8080)    │     │   (Port 5000)   │     │  (1521)     │
└─────────────────┘     └──────────────────┘     └─────────────────┘     └─────────────┘
```

---

## Prerequisites

### Required Software
- **Java 17+** (OpenJDK or Oracle JDK)
- **Maven 3.8+**
- **Node.js 18+** and npm
- **Angular CLI 17+**
- **Python 3.10+**
- **Docker** (for Oracle database)
- **Git**

---

## Step-by-Step Deployment

### Step 1: Environment Setup

```bash
# Copy environment template
cd /workspace/smart-inbox-assistant
cp .env.example .env

# Edit .env with your values (especially AI_API_KEY)
nano .env  # or use your preferred editor
```

**Required Environment Variables:**
```bash
# Oracle Database
ORACLE_HOST=localhost
ORACLE_PORT=1521
ORACLE_SERVICE=FREEPDB1
ORACLE_USERNAME=inbox_admin
ORACLE_PASSWORD=inbox_password

# AI Service (OpenAI-Compatible)
AI_BASE_URL=https://api.openai.com/v1
AI_API_KEY=sk-your-actual-api-key-here
AI_MODEL=gpt-4o

# Spring Boot
SERVER_PORT=8080
APP_ADMIN_USER=admin
APP_ADMIN_PASSWORD=admin123
```

---

### Step 2: Start Oracle Database (Docker)

```bash
# Pull and run Oracle Free Edition
docker run -d \
  --name oracle-free \
  -p 1521:1521 \
  -e ORACLE_PWD=inbox_password \
  -e ORACLE_DATABASE=FREEPDB1 \
  gvenzl/oracle-free:23-slim

# Wait for database to be ready (2-3 minutes)
docker logs -f oracle-free

# Once ready, initialize schema
docker cp /workspace/smart-inbox-assistant/oracle/schema.sql oracle-free:/tmp/schema.sql
docker exec -it oracle-free bash -c "echo 'EXIT' | sqlplus -S inbox_admin/inbox_password@FREEPDB1 @/tmp/schema.sql"

# Verify tables created
docker exec -it oracle-free sqlplus inbox_admin/inbox_password@FREEPDB1 <<EOF
SELECT table_name FROM user_tables ORDER BY table_name;
EXIT;
EOF
```

---

### Step 3: Deploy Python AI Service

```bash
cd /workspace/smart-inbox-assistant/python-ai-service

# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Set environment variables
export AI_BASE_URL=https://api.openai.com/v1
export AI_API_KEY=your-api-key-here
export AI_MODEL=gpt-4o

# Run the service
uvicorn main:app --host 0.0.0.0 --port 5000 --reload
```

**Verify Health:**
```bash
curl http://localhost:5000/api/health
# Expected: {"status":"healthy","timestamp":"...","model":"gpt-4o","api_base_url":"https://api.openai.com/v1"}
```

---

### Step 4: Deploy Spring Boot Backend

```bash
cd /workspace/smart-inbox-assistant/spring-boot-app

# Set environment variables
export ORACLE_HOST=localhost
export ORACLE_PORT=1521
export ORACLE_SERVICE=FREEPDB1
export ORACLE_USERNAME=inbox_admin
export ORACLE_PASSWORD=inbox_password
export AI_SERVICE_URL=http://localhost:5000
export AI_API_KEY=your-api-key-here

# Build the application
mvn clean package -DskipTests

# Run the application
java -jar target/smart-inbox-assistant-1.0.0-SNAPSHOT.jar

# Or use Maven directly
mvn spring-boot:run
```

**Verify Endpoints:**
```bash
# Health check
curl -u admin:admin123 http://localhost:8080/api/health

# Get messages
curl -u admin:admin123 http://localhost:8080/api/messages
```

---

### Step 5: Deploy Angular UI

```bash
cd /workspace/smart-inbox-assistant/angular-ui

# Install dependencies
npm install

# Serve development build
ng serve --open

# Or build production version
ng build --configuration production
```

**Access UI:** Navigate to `http://localhost:4200`

---

## Quick Start - All Services (4 Terminals)

### Terminal 1: Oracle Database
```bash
docker run -d --name oracle-free -p 1521:1521 \
  -e ORACLE_PWD=inbox_password \
  -e ORACLE_DATABASE=FREEPDB1 \
  gvenzl/oracle-free:23-slim
```

### Terminal 2: Python AI Service
```bash
cd /workspace/smart-inbox-assistant/python-ai-service
source venv/bin/activate
export AI_BASE_URL=https://api.openai.com/v1
export AI_API_KEY=your-key-here
uvicorn main:app --host 0.0.0.0 --port 5000
```

### Terminal 3: Spring Boot Backend
```bash
cd /workspace/smart-inbox-assistant/spring-boot-app
export ORACLE_HOST=localhost
export AI_SERVICE_URL=http://localhost:5000
mvn spring-boot:run
```

### Terminal 4: Angular UI
```bash
cd /workspace/smart-inbox-assistant/angular-ui
ng serve
```

---

## Testing the Pipeline

### 1. Insert Test Data via SQL*Plus

```bash
docker exec -it oracle-free sqlplus inbox_admin/inbox_password@FREEPDB1
```

```sql
-- Insert test message
INSERT INTO INCOMING_MESSAGE (sender, subject, body_text, processed_flag)
VALUES (
  'reporter@hospital.com',
  'Adverse Event Report - Patient ABC123',
  'Patient, age 52, male, experienced severe headache and nausea after taking medication XYZ. ' ||
  'Reported by physician Dr. Smith at City Hospital. Product batch number B12345. ' ||
  'Patient was hospitalized for observation. Event started on 2024-01-15.',
  0
);

COMMIT;

-- Verify
SELECT id, sender, subject FROM INCOMING_MESSAGE;
EXIT;
```

### 2. Trigger Processing via API

```bash
# Get message ID from above query (e.g., ID=1)
curl -u admin:admin123 -X POST \
  http://localhost:8080/api/processing/trigger/1
```

### 3. View Results in UI

1. Navigate to `http://localhost:4200`
2. See the message in the list
3. Click "View" to see details
4. Review classifications (ICSR/PQC/MI)
5. Check extracted fields with confidence tiers
6. Submit reviewer actions (accept/override)

---

## API Reference

### Spring Boot Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/messages` | List all messages |
| GET | `/api/messages/{id}` | Get message by ID |
| GET | `/api/messages/unprocessed` | Get unprocessed messages |
| POST | `/api/processing/trigger/{messageId}` | Trigger AI processing |
| GET | `/api/classifications/message/{messageId}` | Get classifications |
| GET | `/api/fields/classification/{id}` | Get extracted fields |
| POST | `/api/review/actions` | Submit reviewer action |

### Python AI Service Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check with model info |
| POST | `/api/process` | Full classification + extraction |
| POST | `/api/classify` | Rule-based classification only |
| POST | `/api/classify-llm` | LLM-based classification |
| POST | `/api/extract` | Pattern-based extraction only |

---

## OpenAI-Compatible API Configuration

The Python AI service supports any OpenAI-compatible API endpoint:

### OpenAI
```bash
export AI_BASE_URL=https://api.openai.com/v1
export AI_API_KEY=sk-...
export AI_MODEL=gpt-4o
```

### Azure OpenAI
```bash
export AI_BASE_URL=https://YOUR_RESOURCE.openai.azure.com/openai/deployments/YOUR_DEPLOYMENT
export AI_API_KEY=your-azure-key
export AI_MODEL=your-deployment-name
```

### Local LLM (Ollama)
```bash
export AI_BASE_URL=http://localhost:11434/v1
export AI_API_KEY=not-needed
export AI_MODEL=llama2
```

### Local LLM (vLLM)
```bash
export AI_BASE_URL=http://localhost:8000/v1
export AI_API_KEY=not-needed
export AI_MODEL=meta-llama/Llama-2-7b-chat-hf
```

---

## Troubleshooting

### Oracle Connection Issues
```bash
# Check if database is running
docker ps | grep oracle

# View database logs
docker logs oracle-free

# Test connection
docker exec -it oracle-free sqlplus inbox_admin/inbox_password@FREEPDB1
```

### Spring Boot Won't Start
```bash
# Check Oracle connectivity first
echo $ORACLE_HOST
echo $AI_SERVICE_URL

# Check port availability
netstat -tlnp | grep 8080

# Check application logs
tail -f /tmp/smart-inbox-assistant.log
```

### Python Service Errors
```bash
# Verify virtual environment is active
which python

# Reinstall dependencies
pip install -r requirements.txt --force-reinstall

# Check port 5000 availability
netstat -tlnp | grep 5000

# Test AI API connectivity
curl -H "Authorization: Bearer $AI_API_KEY" \
  $AI_BASE_URL/models
```

### Angular Build Fails
```bash
# Clear node modules
rm -rf node_modules package-lock.json
npm install

# Check Node.js version
node --version  # Should be 18+

# Check Angular CLI version
ng version
```

---

## Phase Completion Checklist

### Phase 0 ✓ - Skeleton & Data Model
- [x] Oracle schema with all tables (INCOMING_MESSAGE, ATTACHMENT, CLASSIFICATION_RESULT, EXTRACTED_FIELD, MODEL_RUN, REVIEWER_ACTION, AUDIT_LOG)
- [x] Spring Boot project structure with JPA/JDBC layer
- [x] Python AI service scaffold with FastAPI
- [x] Angular UI skeleton with components
- [x] End-to-end path for one document

### Phase 1 ✓ - Core Pipeline
- [x] Multi-label classification (ICSR/PQC/MI/NOT_RELEVANT)
- [x] Two-stage extract-verify pattern
- [x] Field extraction with provenance (source_ref, verified_flag)
- [x] Confidence tiering (HIGH/MEDIUM/LOW) based on evidence
- [x] Angular review queue and detail view
- [x] Reviewer action logging (append-only)

### Phase 2 ✓ - PDF Variety
- [x] PDF type detection (digital/scanned/article/non_english)
- [x] Language detection support
- [x] Table extraction patterns
- [x] Multi-column article handling heuristics

### Phase 3 ✓ - Batch & Polish
- [x] Per-document timing in MODEL_RUN
- [x] Complete audit trail end-to-end
- [x] Health endpoints with model info
- [x] Error handling with fallbacks

### Phase 4 ✓ - Literature Screening (Bonus)
- [x] Relevance decision logic in classifier
- [x] Multi-case splitting support in schema
- [x] Article-specific field groups

---

## Production Considerations

### Security Hardening
1. Replace basic auth with OAuth2/JWT
2. Enable HTTPS/TLS for all services
3. Use secrets management (Vault, AWS Secrets Manager)
4. Implement rate limiting
5. Add input validation and sanitization

### Scalability
1. Replace in-process queue with Redis/RabbitMQ
2. Add horizontal scaling for Python AI service
3. Implement connection pooling for Oracle (HikariCP configured)
4. Add caching layer (Redis) for frequent queries
5. Consider gRPC between Java and Python services

### Monitoring
1. Prometheus metrics enabled via Spring Boot Actuator
2. Configure Grafana dashboards
3. Set up alerting for failed jobs
4. Implement distributed tracing (Jaeger/Zipkin)
5. Log aggregation (ELK stack)

### Data Handling Note
⚠️ **This prototype uses synthetic test data only.**

For production PHI handling:
- Encrypt data at rest and in transit
- Implement proper access controls
- Ensure HIPAA/GDPR compliance
- Consider on-premise LLM deployment
- Execute BAAs with cloud providers

---

## File Structure

```
smart-inbox-assistant/
├── .env.example              # Environment variable template
├── README.md                 # Quick start guide
├── docs/
│   └── DEPLOYMENT.md        # This file
├── oracle/
│   └── schema.sql           # Oracle DDL with all tables
├── spring-boot-app/
│   ├── pom.xml              # Maven configuration
│   └── src/main/java/com/pharma/inbox/
│       ├── SmartInboxAssistantApplication.java
│       ├── config/          # Security, Async config
│       ├── controller/      # REST endpoints
│       ├── model/           # Entity classes
│       ├── repository/      # JDBC repositories
│       └── service/         # Business logic
├── python-ai-service/
│   ├── main.py              # FastAPI app with OpenAI SDK
│   └── requirements.txt     # Python dependencies
└── angular-ui/
    ├── package.json
    └── src/app/
        ├── components/      # UI components
        ├── models/          # TypeScript interfaces
        └── services/        # HTTP clients
```

---

## License & Attribution

This implementation follows the OODA-based architecture plan for the Smart Inbox Assistant pharmacovigilance system. All code is provided as-is for educational and prototyping purposes.

**Key Design Decisions:**
- Two-stage extract-verify pattern for hallucination control
- Evidence-based confidence scoring (not self-reported)
- Full traceability: every fact linked to source
- Unknown over guessing principle
- Append-only audit log for reviewer actions
- OpenAI-compatible API for flexibility
