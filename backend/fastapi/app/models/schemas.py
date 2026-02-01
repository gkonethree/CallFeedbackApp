from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime

class FeedbackCreate(BaseModel):
    rating: int = Field(..., ge=1, le=5)
    comment: Optional[str] = None
    call_id: Optional[str] = None
    timestamp: Optional[datetime] = None

class FeedbackRequest(BaseModel):
    """Request format from Android frontend"""
    voiceQuality: int = Field(..., ge=1, le=5, description="Voice quality rating 1-5")
    delays: int = Field(..., ge=1, le=5, description="Call delays rating 1-5")
    networkReliability: int = Field(..., ge=1, le=5, description="Network reliability rating 1-5")
    comment: Optional[str] = None
    call_id: Optional[str] = None

class FeedbackInDB(FeedbackCreate):
    id: str
    created_at: datetime

class FeedbackListResponse(BaseModel):
    items: List[FeedbackInDB]
    total: int
    page: int
    size: int
