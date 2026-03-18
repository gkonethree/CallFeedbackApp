import os
from dotenv import load_dotenv
from pydantic_settings import BaseSettings

load_dotenv()

class Settings(BaseSettings):
    MONGODB_URI: str = os.getenv("MONGODB_URI", "mongodb://localhost:27017/callfeedback")
    MONGODB_DB: str = os.getenv("MONGODB_DB", "callfeedback")
    ALLOWED_ORIGINS: str = os.getenv("ALLOWED_ORIGINS", "*")
    HOST: str = os.getenv("FASTAPI_HOST", "0.0.0.0")
    PORT: int = int(os.getenv("FASTAPI_PORT", "8000"))
    API_KEY: str = os.getenv("API_KEY", "your_api_key")
    READ_API_KEY: str = os.getenv("READ_API_KEY", "your_read_api_key")

settings = Settings()
