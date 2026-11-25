import re
from urllib.parse import urlparse

def detect_platform(url: str, extractor: str = "") -> str:
    """Havola yoki extractor nomiga qarab platformani aniqlaydi."""
    extractor_lower = (extractor or "").lower()
    url_lower = (url or "").lower()
    
    if "youtube" in extractor_lower or "youtu.be" in url_lower or "youtube.com" in url_lower:
        return "youtube"
    elif "tiktok" in extractor_lower or "tiktok.com" in url_lower:
        return "tiktok"
    elif "instagram" in extractor_lower or "instagram.com" in url_lower:
        return "instagram"
    elif "facebook" in extractor_lower or "fb.watch" in url_lower or "facebook.com" in url_lower:
        return "facebook"
    elif "twitter" in extractor_lower or "x.com" in url_lower or "twitter.com" in url_lower:
        return "twitter"
    return "other"

def format_duration(seconds: int | float | None) -> str:
    """Sekundlarni MM:SS yoki HH:MM:SS formatiga o'tkazadi."""
    if seconds is None or seconds < 0:
        return "00:00"
    
    total_seconds = int(seconds)
    hours = total_seconds // 3600
    minutes = (total_seconds % 3600) // 60
    secs = total_seconds % 60
    
    if hours > 0:
        return f"{hours:02d}:{minutes:02d}:{secs:02d}"
    return f"{minutes:02d}:{secs:02d}"

def format_bytes(size_bytes: int | float | None) -> str:
    """Baytlarni MB yoki GB formatiga o'tkazadi."""
    if size_bytes is None or size_bytes <= 0:
        return "Noma'lum"
    
    kb = size_bytes / 1024
    mb = kb / 1024
    gb = mb / 1024
    
    if gb >= 1:
        return f"{gb:.2f} GB"
    elif mb >= 1:
        return f"{mb:.1f} MB"
    elif kb >= 1:
        return f"{kb:.1f} KB"
    return f"{int(size_bytes)} B"

def sanitize_filename(name: str) -> str:
    """Fayl nomidagi tizim uchun yaroqsiz belgilarni olib tashlaydi."""
    if not name:
        return "video"
    # Windows/Linux/Mac uchun nojo'ya belgilarni tozalash
    clean = re.sub(r'[\\/*?:"<>|]', "", name)
    clean = re.sub(r'\s+', " ", clean).strip()
    return clean[:120] if clean else "media"

