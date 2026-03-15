from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime
from enum import Enum

class AudioIssueEnum(str, Enum):
    """Audio issues reported by user"""
    CALL_DROPPED = "CALL_DROPPED"
    COULD_NOT_HEAR_OTHER = "COULD_NOT_HEAR_OTHER"
    OTHER_COULD_NOT_HEAR_ME = "OTHER_COULD_NOT_HEAR_ME"
    BACKGROUND_NOISE = "BACKGROUND_NOISE"
    ECHO = "ECHO"

class EnvironmentEnum(str, Enum):
    """Environment where call was made"""
    INDOOR = "INDOOR"
    OUTDOOR = "OUTDOOR"
    IN_VEHICLE = "IN_VEHICLE"
    NOISY_AREA = "NOISY_AREA"

class UserFeedback(BaseModel):
    voiceQuality: Optional[int] = Field(None, ge=1, le=5, description="Voice quality rating 1-5")
    audioIssues: Optional[List[AudioIssueEnum]] = Field(None, description="List of audio issues")
    environment: Optional[EnvironmentEnum] = Field(None, description="Environment type")
    comment: Optional[str] = Field(None, description="User comments")
    carrier: Optional[str] = Field(None, description="Mobile carrier")
    networkGeneration: Optional[str] = Field(None, description="Network type: WiFi, 2G, 3G, 4G, 5G")
    signalStrength: Optional[int] = Field(None, description="Signal strength in dBm")
    latitude: Optional[float] = Field(None, description="Device latitude")
    longitude: Optional[float] = Field(None, description="Device longitude")
    timestamp: Optional[int] = Field(None, description="Unix timestamp in milliseconds")

class FeedbackInDB(UserFeedback):
    """Feedback stored in database"""
    id: str
    created_at: datetime

class FeedbackListResponse(BaseModel):
    """List of feedbacks response"""
    items: List[FeedbackInDB]
    total: int
    page: int
    size: int



