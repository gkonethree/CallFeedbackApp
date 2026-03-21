from fastapi import APIRouter, Depends, HTTPException
from app.models.schemas import UserFeedback, FeedbackInDB
from app.db import get_feedback_collection
from app.auth import verify_api_key, verify_read_api_key
from datetime import datetime, timezone
from main import limiter

router = APIRouter(prefix="/gk/feedback", tags=["feedback"])


def serialize(doc: dict) -> dict:
    return {
        "id": str(doc.get("_id")),
        "voiceQuality": doc.get("voiceQuality"),
        "audioIssues": doc.get("audioIssues"),
        "environment": doc.get("environment"),
        "comment": doc.get("comment"),
        "carrier": doc.get("carrier"),
        "networkGeneration": doc.get("networkGeneration"),
        "signalStrength": doc.get("signalStrength"),
        "latitude": doc.get("latitude"),
        "longitude": doc.get("longitude"),
        "callDuration": doc.get("callDuration"),
        "timestamp": doc.get("timestamp"),
        "created_at": doc.get("created_at"),
    }


@router.post("", response_model=FeedbackInDB, status_code=201)
@limiter.limit("10/minute")
async def create_feedback(payload: UserFeedback, api_key: str = Depends(verify_api_key)):
    col = get_feedback_collection()

    doc = {
        "voiceQuality": payload.voiceQuality,
        "audioIssues": [issue.value for issue in payload.audioIssues] if payload.audioIssues else None,
        "environment": payload.environment.value if payload.environment else None,
        "comment": payload.comment,
        "carrier": payload.carrier,
        "networkGeneration": payload.networkGeneration,
        "signalStrength": payload.signalStrength,
        "latitude": payload.latitude,
        "longitude": payload.longitude,
        "timestamp": payload.timestamp,
        "callDuration": payload.callDuration,
    }

    doc = {k: v for k, v in doc.items() if v is not None}

    doc["created_at"] = datetime.now(timezone.utc)

    res = await col.insert_one(doc)
    doc["_id"] = res.inserted_id

    return serialize(doc)

@router.get("", response_model=list[FeedbackInDB])
async def get_all_feedbacks(
    skip: int = 0,
    limit: int = 50,
    api_key: str = Depends(verify_read_api_key)
):
    col = get_feedback_collection()

    docs = await col.find()\
        .sort("created_at", -1)\
        .skip(skip)\
        .limit(limit)\
        .to_list(length=limit)

    return [serialize(doc) for doc in docs]
