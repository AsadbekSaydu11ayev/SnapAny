from typing import List, Optional
from pydantic import BaseModel, Field, HttpUrl

class ParseRequest(BaseModel):
    url: str = Field(..., description="Video yoki audio havolasi")

class FormatInfo(BaseModel):
    format_id: str = Field(..., description="Formatning unikal identifikatori")
    quality: str = Field(..., description="Video/Audio sifati (masalan, 1080p, 720p, Faqat Audio (MP3))")
    ext: str = Field(..., description="Fayl kengaytmasi (mp4 yoki mp3)")
    filesize: str = Field(..., description="Fayl hajmi yoki taxminiy hajm")
    download_endpoint: str = Field(..., description="Yuklab olish uchun API endpoint manzili")

class ParseResponse(BaseModel):
    title: str = Field(..., description="Video nomi")
    thumbnail: str = Field(..., description="Video poster rasm URL manzili")
    duration: str = Field(..., description="Video davomiyligi (masalan, 03:45)")
    platform: str = Field(..., description="Platforma nomi: youtube | tiktok | instagram | other")
    formats: List[FormatInfo] = Field(default_factory=list, description="Mavjud formatlar ro'yxati")
    author: Optional[str] = Field(None, description="Muallif yoki kanal nomi")

class ErrorResponse(BaseModel):
    error: str
    detail: Optional[str] = None

