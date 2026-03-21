from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routes.feedback import router as feedback_router
from app.config import settings
from app.db import client
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware
from fastapi.responses import JSONResponse
from app.core.limiter import limiter

app = FastAPI(title="Call Feedback API")
app.state.limiter = limiter
app.add_middleware(SlowAPIMiddleware)

@app.exception_handler(RateLimitExceeded)
async def rate_limit_exceeded_handler(request, exc):
    return JSONResponse(
        status_code=429,
        content={"detail": "Rate limit exceeded. Please try again later."},
    )
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
