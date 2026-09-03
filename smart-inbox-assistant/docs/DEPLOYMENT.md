# Smart Inbox Assistant - Complete Deployment Guide

## Overview

This is a complete pharmacovigilance/quality intake system (Phases 0-4) that:
- Reads incoming emails + PDF attachments
- Classifies documents into: ICSR (Safety Report), PQC (Quality Complaint), MI (Info Request), or Not Relevant
- Extracts structured fields with source-linked provenance
- Provides Angular UI for human reviewer oversight
- Maintains full audit trail in Oracle database

**Stack:** Angular 17 → Spring Boot 3.2/Java 17 → Python FastAPI → Oracle Database

---

## System Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐     ┌─────────────┐
│   Angular UI    │────▶│  Spring Boot API │────▶│  Python AI Svc  │────▶│   Oracle    │
│   (Port 4200)   │     │   (Port 8080)    │     │   (Port 5000)   │     │  (1521)     │
└─────────────────┘     └──────────────────┘     └─────────────────┘     └─────────────┘
        │                       │                        │                      │
        └───────────────────────┴────────────────────────┴──────────────────────┘
                                    All communications via REST/JSON
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

### Environment Variables

Create a `.env` file or export these variables:

```bash
# Oracle Database
export ORACLE_HOST=localhost
export ORACLE_PORT=1521
export ORACLE_SERVICE=FREEPDB1
export ORACLE_USERNAME=inbox_admin
export ORACLE_PASSWORD=inbox_password

# Spring Boot App
export APP_ADMIN_USER=admin
export APP_ADMIN_PASSWORD=admin123

# AI Service
export AI_SERVICE_URL=http://localhost:5000
export AI_API_KEY=your-api-key-here

# Mail Configuration (optional - for IMAP polling)
export MAIL_IMAP_HOST=imap.gmail.com
export MAIL_IMAP_PORT=993
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
export MAIL_FOLDER=INBOX
export MAIL_POLL_INTERVAL=60000
```

---

## Phase-by-Phase Deployment Commands

### Step 1: Start Oracle Database (Docker)

```bash
# Pull and run Oracle Free Edition
docker run -d \
  --name oracle-free \
  -p 1521:1521 \
  -e ORACLE_PWD=YourStrongPassword123 \
  -e ORACLE_DATABASE=FREEPDB1 \
  gvenzl/oracle-free:23-slim

# Wait for database to be ready (2-3 minutes)
docker logs -f oracle-free

# Once ready, run schema initialization
docker cp oracle/schema.sql oracle-free:/tmp/schema.sql
docker exec -it oracle-free sqlplus inbox_admin/inbox_password@FREEPDB1 @/tmp/schema.sql
```

### Step 2: Deploy Python AI Service (Phase 1-2)

```bash
cd /workspace/smart-inbox-assistant/python-ai-service

# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Set environment variables
export AI_API_KEY=your-api-key-here
export AI_MODEL=gpt-4o

# Run the service
uvicorn main:app --host 0.0.0.0 --port 5000 --reload

# Verify health
curl http://localhost:5000/api/health
```

Expected response: `{"status":"healthy","timestamp":"..."}`

### Step 3: Deploy Spring Boot Backend (Phase 0-1)

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

Verify endpoints:
```bash
# Health check
curl -u admin:admin123 http://localhost:8080/api/health

# Get messages
curl -u admin:admin123 http://localhost:8080/api/messages
```

### Step 4: Deploy Angular UI (Phase 1-3)

```bash
cd /workspace/smart-inbox-assistant/angular-ui

# Install dependencies
npm install

# Serve development build
ng serve --open

# Or build production version
ng build --configuration production

# The app will be available at http://localhost:4200
```

---

## Quick Start - All Services

For rapid testing, run all services in separate terminals:

### Terminal 1: Oracle Database
```bash
docker run -d --name oracle-free -p 1521:1521 \
  -e ORACLE_PWD=inbox_password gvenzl/oracle-free:23-slim
```

### Terminal 2: Python AI Service
```bash
cd /workspace/smart-inbox-assistant/python-ai-service
source venv/bin/activate
uvicorn main:app --host 0.0.0.0 --port 5000
```

### Terminal 3: Spring Boot Backend
```bash
cd /workspace/smart-inbox-assistant/spring-boot-app
mvn spring-boot:run
```

### Terminal 4: Angular UI
```bash
cd /workspace/smart-inbox-assistant/angular-ui
ng serve
```

---

## Testing the Pipeline

### 1. Insert Test Data

```sql
-- Connect to Oracle
sqlplus inbox_admin/inbox_password@localhost:1521/FREEPDB1

-- Insert test message
INSERT INTO INCOMING_MESSAGE (sender, subject, body_text, processed_flag)
VALUES (
  'reporter@hospital.com',
  'Adverse Event Report - Patient ABC123',
  'Patient, age 52, male, experienced severe headache and nausea after taking medication XYZ. 
   Reported by physician Dr. Smith. Product batch number B12345. 
   Patient was hospitalized for observation.',
  0
);

COMMIT;

-- Verify
SELECT id, sender, subject FROM INCOMING_MESSAGE;
```

### 2. Trigger Processing via API

```bash
# Get message ID from above query (e.g., ID=1)
curl -X POST -u admin:admin123 \
  http://localhost:8080/api/processing/trigger/1
```

### 3. View Results in UI

Navigate to `http://localhost:4200` and:
1. See the message in the list
2. Click "View" to see details
3. Review classifications (ICSR/PQC/MI)
4. Check extracted fields with confidence tiers
5. Submit reviewer actions (accept/override)

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
| GET | `/api/health` | Health check |
| POST | `/api/process` | Full classification + extraction |
| POST | `/api/classify` | Classification only |
| POST | `/api/extract` | Extraction only |

---

## Phase Completion Checklist

### Phase 0 ✓ - Skeleton & Data Model
- [x] Oracle schema with all tables
- [x] Spring Boot project structure
- [x] Python AI service scaffold
- [x] Angular UI skeleton
- [x] End-to-end path for one document

### Phase 1 ✓ - Core Pipeline
- [x] Multi-label classification (ICSR/PQC/MI/Not Relevant)
- [x] Two-stage extract-verify pattern
- [x] Field extraction with provenance
- [x] Confidence tiering (HIGH/MEDIUM/LOW)
- [x] Angular review queue and detail view
- [x] Reviewer action logging

### Phase 2 ✓ - PDF Variety
- [x] PDF type detection (digital/scanned/article/non_english)
- [x] Language detection support
- [x] Table extraction patterns
- [x] Multi-column article handling heuristics

### Phase 3 ✓ - Batch & Polish
- [x] Per-document timing in MODEL_RUN
- [x] Complete audit trail
- [x] Health endpoints
- [x] Error handling

### Phase 4 ✓ - Literature Screening (Bonus)
- [x] Relevance decision logic
- [x] Multi-case splitting support
- [x] Article-specific field groups

---

## Troubleshooting

### Oracle Connection Issues
```bash
# Check if database is running
docker ps | grep oracle

# View database logs
docker logs oracle-free

# Test connection
sqlplus inbox_admin/inbox_password@localhost:1521/FREEPDB1
```

### Spring Boot Won't Start
```bash
# Check Oracle connectivity first
# Then verify environment variables
echo $ORACLE_HOST
echo $AI_SERVICE_URL

# Check port availability
netstat -tlnp | grep 8080
```

### Python Service Errors
```bash
# Verify virtual environment is active
which python

# Reinstall dependencies
pip install -r requirements.txt --force-reinstall

# Check port 5000 availability
netstat -tlnp | grep 5000
```

### Angular Build Fails
```bash
# Clear node modules
rm -rf node_modules package-lock.json
npm install

# Check Node.js version
node --version  # Should be 18+
```

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
3. Implement connection pooling for Oracle
4. Add caching layer (Redis) for frequent queries
5. Consider gRPC between Java and Python services

### Monitoring
1. Add Prometheus metrics (already enabled via Actuator)
2. Configure Grafana dashboards
3. Set up alerting for failed jobs
4. Implement distributed tracing (Jaeger/Zipkin)
5. Log aggregation (ELK stack)

### Data Handling Note
- This prototype uses synthetic test data only
- For production PHI handling:
  - Encrypt data at rest and in transit
  - Implement proper access controls
  - Ensure HIPAA/GDPR compliance
  - Consider on-premise LLM deployment

---

## File Structure Summary

```
smart-inbox-assistant/
├── oracle/
│   └── schema.sql              # Database DDL
├── spring-boot-app/
│   ├── pom.xml                 # Maven config
│   └── src/main/java/com/pharma/inbox/
│       ├── SmartInboxAssistantApplication.java
│       ├── config/             # Security, Async config
│       ├── controller/         # REST endpoints
│       ├── model/              # Entity classes
│       ├── repository/         # JDBC repositories
│       └── service/            # Business logic
├── python-ai-service/
│   ├── main.py                 # FastAPI app
│   └── requirements.txt        # Python deps
├── angular-ui/
│   ├── package.json
│   └── src/app/
│       ├── components/         # UI components
│       ├── models/             # TypeScript interfaces
│       └── services/           # HTTP clients
└── docs/
    └── DEPLOYMENT.md          # This file
```

---

## License & Attribution

This implementation follows the OODA-based architecture plan for the Smart Inbox Assistant pharmacovigilance system. All code is provided as-is for educational and prototyping purposes.

**Key Design Decisions Documented:**
- Two-stage extract-verify pattern for hallucination control
- Evidence-based confidence scoring (not self-reported)
- Full traceability: every fact linked to source
- Unknown over guessing principle
- Append-only audit log for reviewer actions
