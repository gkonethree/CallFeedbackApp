import os
from fastapi import  Header,HTTPException


API_KEY = os.getenv("API_KEY")

def verify_api_key(api_key: str = Header(...,alias="X-API-Key")):
    if api_key != API_KEY:
        raise HTTPException(status_code=403, detail="Invalid API Key")