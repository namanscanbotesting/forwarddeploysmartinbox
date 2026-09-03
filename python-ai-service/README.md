# Smart Inbox Assistant - Python AI Service

FastAPI-based AI service for document classification and field extraction.

## Setup

```bash
pip install fastapi uvicorn pydantic python-multipart
pip install anthropic  # or openai, depending on your LLM choice
```

## Run

```bash
uvicorn app:app --host 0.0.0.0 --port 5000 --reload
```

## API Endpoints

- `POST /api/process` - Process a document for classification and extraction
- `GET /health` - Health check
