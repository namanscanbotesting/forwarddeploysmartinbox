[INFO] Smart Inbox Assistant - Complete Implementation (Phases 0-4)
===================================================================

This document provides the complete implementation guide with all commands.

QUICK START - ALL SERVICES
===========================

Terminal 1: Oracle Database
---------------------------
docker run -d --name oracle-free -p 1521:1521 \
  -e ORACLE_PWD=inbox_password \
  -e ORACLE_DATABASE=FREEPDB1 \
  gvenzl/oracle-free:23-slim

# Wait 2-3 minutes for database to initialize
docker logs -f oracle-free

Terminal 2: Python AI Service
------------------------------
cd /workspace/smart-inbox-assistant/python-ai-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
export AI_BASE_URL=https://api.openai.com/v1
export AI_API_KEY=your-api-key-here
export AI_MODEL=gpt-4o
uvicorn main:app --host 0.0.0.0 --port 5000

Terminal 3: Spring Boot Backend
--------------------------------
cd /workspace/smart-inbox-assistant/spring-boot-app
mvn spring-boot:run

Terminal 4: Angular UI
-----------------------
cd /workspace/smart-inbox-assistant/angular-ui
npm install
ng serve --open

ACCESS POINTS
=============
- Angular UI: http://localhost:4200
- Spring Boot API: http://localhost:8080
- Python AI Service: http://localhost:5000
- Oracle DB: localhost:1521/FREEPDB1

DEFAULT CREDENTIALS
===================
- Username: admin
- Password: admin123

TEST THE PIPELINE
==================
1. Insert test message via SQL*Plus:
   docker exec -it oracle-free sqlplus inbox_admin/inbox_password@FREEPDB1
   
   INSERT INTO INCOMING_MESSAGE (sender, subject, body_text, processed_flag)
   VALUES ('test@hospital.com', 'Adverse Event Report', 
           'Patient age 52 experienced headache after taking drug XYZ.', 0);
   COMMIT;

2. Trigger processing:
   curl -u admin:admin123 -X POST http://localhost:8080/api/processing/trigger/1

3. View results in Angular UI at http://localhost:4200

For complete documentation, see docs/DEPLOYMENT.md
