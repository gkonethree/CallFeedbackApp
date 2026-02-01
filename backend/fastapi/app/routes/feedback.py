from fastapi import APIRouter, HTTPException, Depends, Header
from app.models.schemas import FeedbackCreate, FeedbackInDB, FeedbackListResponse, FeedbackRequest
from app.db import get_feedback_collection
from app.config import settings
from typing import Optional, Union
from datetime import datetime
from bson import ObjectId
import uuid

router = APIRouter(prefix="/feedback", tags=["feedback"])


def serialize(doc: dict) -> dict:
    return {
        "id": str(doc.get("_id")),
        "rating": doc.get("rating"),
        "comment": doc.get("comment"),
        "call_id": doc.get("call_id"),
        "timestamp": doc.get("timestamp"),
        "created_at": doc.get("created_at"),
    }


@router.post("", response_model=FeedbackInDB, status_code=201)
async def create_feedback(payload: FeedbackRequest):

    col = get_feedback_collection()
   
    # Calculate average rating from voice quality, delays, and network reliability
    avg_rating = round((payload.voiceQuality + payload.delays + payload.networkReliability) / 3)
    avg_rating = max(1, min(5, avg_rating))  # Ensure rating is between 1-5
    
    doc = {
        "rating": avg_rating,
        "comment": payload.comment,
        "call_id": str(uuid.uuid4()),  # Auto-generate call_id if not provided
        "voiceQuality": payload.voiceQuality,
        "delays": payload.delays,
        "networkReliability": payload.networkReliability,
    }

    
    now = datetime.utcnow()
    if doc.get("timestamp") is None:
        doc["timestamp"] = now
    doc["created_at"] = now
    
    res = await col.insert_one(doc)
    created = await col.find_one({"_id": res.inserted_id})
    return serialize(created)


@router.get("", response_model=FeedbackListResponse)
async def list_feedback(
    page: int = 1,
    size: int = 20,
    min_rating: Optional[int] = None,
    max_rating: Optional[int] = None,
    call_id: Optional[str] = None,
):
    col = get_feedback_collection()
    skip = (page - 1) * size
    query = {}
    if min_rating is not None or max_rating is not None:
        rating_query = {}
        if min_rating is not None:
            rating_query["$gte"] = min_rating
        if max_rating is not None:
            rating_query["$lte"] = max_rating
        query["rating"] = rating_query
    if call_id:
        query["call_id"] = call_id

    total = await col.count_documents(query)
    cursor = col.find(query).sort("created_at", -1).skip(skip).limit(size)
    items = [serialize(d) async for d in cursor]
    return {"items": items, "total": total, "page": page, "size": size}


@router.get("/{id}", response_model=FeedbackInDB)
async def get_feedback(id: str):
    col = get_feedback_collection()
    try:
        oid = ObjectId(id)
    except Exception:
        raise HTTPException(status_code=404, detail="Not found")
    doc = await col.find_one({"_id": oid})
    if not doc:
        raise HTTPException(status_code=404, detail="Not found")
    return serialize(doc)


@router.delete("/{id}")
async def delete_feedback(id: str, x_api_key: Optional[str] = Header(None)):
    if x_api_key != settings.ADMIN_API_KEY:
        raise HTTPException(status_code=403, detail="Forbidden")
    col = get_feedback_collection()
    try:
        oid = ObjectId(id)
    except Exception:
        raise HTTPException(status_code=404, detail="Not found")
    res = await col.delete_one({"_id": oid})
    if res.deleted_count == 0:
        raise HTTPException(status_code=404, detail="Not found")
    return {"ok": True}
