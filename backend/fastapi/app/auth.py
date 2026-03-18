from fastapi import  Header,HTTPException
from app.config import settings

def verify_api_key(api_key: str = Header(...,alias="X-API-Key")):
    if api_key != settings.API_KEY:
        raise HTTPException(status_code=403, detail="Invalid API Key")
    
def verify_read_api_key(api_key: str = Header(...,alias="X-API-Key")):
    if api_key != settings.READ_API_KEY:
        raise HTTPException(status_code=403, detail="Invalid Read API Key")