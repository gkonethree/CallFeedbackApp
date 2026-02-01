# FastAPI Call Feedback backend

This is a minimal FastAPI service to store call feedback in MongoDB.

Run locally (virtualenv):

1. Create and activate a virtualenv:

```bash
python3 -m venv .venv
source .venv/bin/activate
```

2. Install dependencies:

```bash
pip install -r requirements.txt
```

3. Copy `.env.example` to `.env` and edit values if needed:

```bash
cp .env.example .env
```

4. Run the app with `uvicorn`:

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Run with Docker (recommended for production/local parity):

From the `backend/` directory run:

```bash
docker compose up --build
```

The API will be available at `http://localhost:8000`.

Endpoints:

- GET /health
- POST /feedback
- GET /feedback
- GET /feedback/{id}
- DELETE /feedback/{id} (requires header `X-API-Key`)

Example POST:

```bash
curl -X POST http://localhost:8000/feedback -H "Content-Type: application/json" -d '{"rating":5, "comment":"Great call!"}'
```
