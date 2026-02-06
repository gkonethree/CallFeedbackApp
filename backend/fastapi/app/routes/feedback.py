from fastapi import APIRouter, HTTPException, Depends, Header
from app.models.schemas import UserFeedback, FeedbackInDB, FeedbackListResponse
from app.db import get_feedback_collection
from app.config import settings
from typing import Optional
from datetime import datetime
from bson import ObjectId
import uuid

router = APIRouter(prefix="/feedback", tags=["feedback"])


def serialize(doc: dict) -> dict:
    return {
        "id": str(doc.get("_id")),
        "voiceQuality": doc.get("voiceQuality"),
        "audioIssues": doc.get("audioIssues"),
        "environment": doc.get("environment"),
        "comment": doc.get("comment"),
        "networkGeneration": doc.get("networkGeneration"),
        "signalStrength": doc.get("signalStrength"),
        "latitude": doc.get("latitude"),
        "longitude": doc.get("longitude"),
        "timestamp": doc.get("timestamp"),
        "created_at": doc.get("created_at"),
    }


@router.post("", response_model=FeedbackInDB, status_code=201)
async def create_feedback(payload: UserFeedback):
    col = get_feedback_collection()

    doc = {
        "voiceQuality": payload.voiceQuality,
        "audioIssues": [issue.value for issue in payload.audioIssues] if payload.audioIssues else None,
        "environment": payload.environment.value if payload.environment else None,
        "comment": payload.comment,
        "networkGeneration": payload.networkGeneration,
        "signalStrength": payload.signalStrength,
        "latitude": payload.latitude,
        "longitude": payload.longitude,
        "timestamp": payload.timestamp,
    }

    now = datetime.utcnow()
    doc["created_at"] = now
    
    res = await col.insert_one(doc)
    created = await col.find_one({"_id": res.inserted_id})
    return serialize(created)


# @router.get("", response_model=FeedbackListResponse)
# async def list_feedback(
#     page: int = 1,
#     size: int = 20,
#     min_quality: Optional[int] = None,
#     max_quality: Optional[int] = None,
#     network: Optional[str] = None,
# ):
#     col = get_feedback_collection()
#     skip = (page - 1) * size
#     query = {}

#     # Filter by voice quality
#     if min_quality is not None or max_quality is not None:
#         quality_query = {}
#         if min_quality is not None:
#             quality_query["$gte"] = min_quality
#         if max_quality is not None:
#             quality_query["$lte"] = max_quality
#         query["voiceQuality"] = quality_query

#     # Filter by network type
#     if network:
#         query["networkGeneration"] = network

#     total = await col.count_documents(query)
#     cursor = col.find(query).sort("created_at", -1).skip(skip).limit(size)
#     items = [serialize(d) async for d in cursor]
#     return {"items": items, "total": total, "page": page, "size": size}


# @router.get("/{id}", response_model=FeedbackInDB)
# async def get_feedback(id: str):
#     col = get_feedback_collection()
#     try:
#         oid = ObjectId(id)
#     except Exception:
#         raise HTTPException(status_code=404, detail="Not found")
#     doc = await col.find_one({"_id": oid})
#     if not doc:
#         raise HTTPException(status_code=404, detail="Not found")
#     return serialize(doc)


# @router.delete("/{id}")
# async def delete_feedback(id: str, x_api_key: Optional[str] = Header(None)):
#     if x_api_key != settings.ADMIN_API_KEY:
#         raise HTTPException(status_code=403, detail="Forbidden")
#     col = get_feedback_collection()
#     try:
#         oid = ObjectId(id)
#     except Exception:
#         raise HTTPException(status_code=404, detail="Not found")
#     res = await col.delete_one({"_id": oid})
#     if res.deleted_count == 0:
#         raise HTTPException(status_code=404, detail="Not found")
#     return {"ok": True}
