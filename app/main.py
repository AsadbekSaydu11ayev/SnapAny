import os
import shutil
from pathlib import Path
from urllib.parse import quote

from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from starlette.background import BackgroundTask

from app.config import STATIC_DIR
from app.schemas import ParseRequest, ParseResponse, ErrorResponse
from app.services.downloader import parse_media_url, download_media_file

app = FastAPI(
    title="SnapAny Clone - Universal Video Downloader",
    description="YouTube, TikTok va Instagram platformalaridan video va audio yuklab olish xizmati",
    version="1.0.0",
)

# CORS sozlamalari
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def cleanup_temp_dir(temp_dir: str):
    """Fayl uzatilgandan so'ng vaqtinchalik papkani tozalash."""
    try:
        shutil.rmtree(temp_dir, ignore_errors=True)
    except Exception:
        pass


@app.post(
    "/api/parse",
    response_model=ParseResponse,
    responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}},
    summary="Video havolasini tahlil qilish",
)
async def api_parse(req: ParseRequest):
    """
    Kiritilgan video havolasini (YouTube, TikTok, Instagram) tahlil qiladi
    va yuklab olish uchun mavjud formatlar ro'yxatini qaytaradi.
    """
    url = req.url.strip() if req.url else ""
    if not url:
        raise HTTPException(status_code=400, detail="Havola kiritilmagan.")
    
    if not (url.startswith("http://") or url.startswith("https://")):
        raise HTTPException(status_code=400, detail="Yaroqsiz havola formati (http:// yoki https:// bilan boshlanishi kerak).")

    try:
        result = await parse_media_url(url)
        return result
    except ValueError as ve:
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Ichki xatolik: {str(e)}")


@app.get(
    "/api/download",
    summary="Video yoki audio faylni yuklab olish",
)
async def api_download(
    url: str = Query(..., description="Video URL manzili"),
    format_id: str = Query("best", description="Tanlangan format ID"),
):
    """
    Tanlangan formatdagi videoni yoki audioni server orqali tayyorlab,
    to'g'ri sarlavha va Content-Disposition (attachment) bilan brauzerga uzatadi.
    """
    url = url.strip()
    if not url:
        raise HTTPException(status_code=400, detail="Havola ko'rsatilmadi.")

    try:
        file_path, safe_filename, mime_type, temp_dir = await download_media_file(url, format_id)
        
        # Fayl uzatib bo'lingach, vaqtinchalik papkani o'chiradigan fon vazifasi
        cleanup_task = BackgroundTask(cleanup_temp_dir, temp_dir)
        
        # Fayl nomini to'g'ri kodlash (UTF-8 RFC 5987 formati)
        encoded_filename = quote(safe_filename)
        content_disposition = f'attachment; filename="{safe_filename}"; filename*=UTF-8\'\'{encoded_filename}'
        
        return FileResponse(
            path=file_path,
            media_type=mime_type,
            filename=safe_filename,
            headers={
                "Content-Disposition": content_disposition,
                "Access-Control-Expose-Headers": "Content-Disposition",
            },
            background=cleanup_task,
        )
    except ValueError as ve:
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Yuklab olishda xatolik yuz berdi: {str(e)}")


@app.get("/api/health", summary="Tizim holatini tekshirish")
async def health_check():
    return {"status": "ok", "app": "SnapAny", "version": "1.0.0"}


# Static fayllarni ulash
if STATIC_DIR.exists():
    app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")

    @app.get("/", include_in_schema=False)
    async def serve_index():
        return FileResponse(STATIC_DIR / "index.html")
