from motor.motor_asyncio import AsyncIOMotorClient
from app.config import settings

client = AsyncIOMotorClient(settings.MONGODB_URI)

def get_db():
    return client[settings.MONGODB_DB]


def get_feedback_collection():
    return get_db()["feedback"]
