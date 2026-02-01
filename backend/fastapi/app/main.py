from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routes.feedback import router as feedback_router
from app.config import settings
from app.db import client

app = FastAPI(title="Call Feedback API")

# CORS
origins = [o.strip() for o in settings.ALLOWED_ORIGINS.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins or ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(feedback_router)


@app.get("/health")
async def health():
    return {"ok": True}


@app.on_event("shutdown")
async def shutdown_event():
    try:
        client.close()
    except Exception:
        pass
