import asyncio
import os
import shutil
import tempfile
from pathlib import Path
from typing import Dict, Any, List, Tuple
from urllib.parse import quote

import yt_dlp

from app.config import FFMPEG_PATH, NODE_PATH, DEFAULT_USER_AGENT
from app.schemas import ParseResponse, FormatInfo
from app.utils import detect_platform, format_duration, format_bytes, sanitize_filename


def _get_cookie_file_path() -> str | None:
    """Agar YOUTUBE_COOKIES yoki YTDLP_COOKIES env berilgan bo'lsa, vaqtinchalik cookie faylini tayyorlaydi."""
    cookies_content = os.environ.get("YOUTUBE_COOKIES") or os.environ.get("YTDLP_COOKIES")
    if not cookies_content or not cookies_content.strip():
        return None

    cookie_path = os.path.join(tempfile.gettempdir(), "snapany_yt_cookies.txt")
    with open(cookie_path, "w", encoding="utf-8") as f:
        f.write(cookies_content.strip() + "\n")
    return cookie_path


def _get_base_ydl_opts() -> Dict[str, Any]:
    """Asosiy yt-dlp konfiguratsiyasini qaytaradi."""
    opts: Dict[str, Any] = {
        "quiet": True,
        "no_warnings": True,
        "nocheckcertificate": True,
        "ignoreerrors": False,
        "logtostderr": False,
        "user_agent": DEFAULT_USER_AGENT,
    }
    cookie_path = _get_cookie_file_path()
    if cookie_path:
        opts["cookiefile"] = cookie_path
    else:
        opts["extractor_args"] = {
            "youtube": {
                "player_client": ["android_vr", "tv", "ios", "web"],
            }
        }
    if FFMPEG_PATH:
        opts["ffmpeg_location"] = FFMPEG_PATH
    if NODE_PATH:
        opts["js_runtimes"] = {"node": {"path": NODE_PATH}}
    return opts


def _estimate_format_size(fmt: dict, duration: float | None) -> str:
    """Format hajmini aniqlaydi yoki hisoblaydi."""
    size = fmt.get("filesize") or fmt.get("filesize_approx")
    if size:
        return format_bytes(size)
    
    # Agar filesize bo'lmasa, tbr (total bitrate kbps) va duration orqali hisoblash
    tbr = fmt.get("tbr") or fmt.get("vbr", 0) + fmt.get("abr", 0)
    if tbr and duration and duration > 0:
        est_bytes = (tbr * 1024 / 8) * duration
        return f"~{format_bytes(est_bytes)}"
    
    return "Noma'lum"


def _estimate_merged_size(video_fmt: dict, audio_fmt: dict | None, duration: float | None) -> str:
    """Video va audio birlashgandagi taxminiy umumiy hajmni hisoblaydi."""
    v_size = video_fmt.get("filesize") or video_fmt.get("filesize_approx") or 0
    a_size = (audio_fmt.get("filesize") or audio_fmt.get("filesize_approx") or 0) if audio_fmt else 0
    
    if v_size and a_size:
        return format_bytes(v_size + a_size)
    elif v_size:
        # Audioga qo'shimcha taxminiy 3-5 MB qo'shish
        extra_audio = (128 * 1024 / 8) * (duration or 0) if duration else 0
        return f"~{format_bytes(v_size + extra_audio)}"
    
    return _estimate_format_size(video_fmt, duration)


async def parse_media_url(url: str) -> ParseResponse:
    """
    Havolani yt-dlp orqali tahlil qilib, sarlavha, thumbnail,
    davomiylik va yuklab olish formatlarini qaytaradi.
    """
    opts = _get_base_ydl_opts()
    
    def _extract():
        with yt_dlp.YoutubeDL(opts) as ydl:
            return ydl.extract_info(url, download=False)
            
    try:
        loop = asyncio.get_event_loop()
        info = await loop.run_in_executor(None, _extract)
    except Exception as e:
        error_msg = str(e)
        lower_err = error_msg.lower()
        if "not a bot" in lower_err or "confirm you" in lower_err:
            raise ValueError(
                "YouTube server IP manzilini bot tekshiruvidan o'tkazmadi. "
                "Render muhitida YOUTUBE_COOKIES o'zgaruvchisini sozlash tavsiya etiladi."
            )
        elif "Private video" in error_msg or "Sign in" in error_msg:
            raise ValueError("Ushbu video xususiy (private) yoki platformada tizimga kirishni talab qiladi.")
        elif "unavailable" in error_msg.lower() or "not found" in error_msg.lower():
            raise ValueError("Video mavjud emas yoki o'chirib tashlangan.")
        elif "Unsupported URL" in error_msg:
            raise ValueError("Ushbu havola formati qo'llab-quvvatlanmaydi.")
        elif "login" in error_msg.lower() or "checkpoint" in error_msg.lower():
            raise ValueError("Ushbu profil yoki video yopiq (xususiy) hisoblanadi.")
        
        # yt-dlp ning keraksiz texnik prefikslarini tozalash
        clean_err = error_msg.replace("ERROR: ", "").strip()
        if " [youtube] " in clean_err:
            clean_err = clean_err.split(" [youtube] ", 1)[-1]
        elif " [tiktok] " in clean_err:
            clean_err = clean_err.split(" [tiktok] ", 1)[-1]
        elif " [instagram] " in clean_err:
            clean_err = clean_err.split(" [instagram] ", 1)[-1]
            
        raise ValueError(f"Havolani tahlil qilishda xatolik yuz berdi: {clean_err}")

    if not info:
        raise ValueError("Video ma'lumotlarini olib bo'lmadi.")

    title = info.get("title") or "Nomsiz video"
    thumbnail = info.get("thumbnail") or ""
    duration_sec = info.get("duration")
    duration_str = format_duration(duration_sec)
    extractor = info.get("extractor", "")
    platform = detect_platform(url, extractor)
    author = info.get("uploader") or info.get("channel") or info.get("artist")

    formats_list: List[FormatInfo] = []
    raw_formats = info.get("formats", [])

    # Eng yaxshi audio formatni topish (hajmni hisoblash uchun)
    audio_formats = [f for f in raw_formats if f.get("acodec") != "none" and f.get("vcodec") == "none"]
    best_audio = audio_formats[-1] if audio_formats else None
    audio_size_str = _estimate_format_size(best_audio, duration_sec) if best_audio else "Noma'lum"

    encoded_url = quote(url, safe="")

    if platform == "youtube":
        # YouTube uchun sifatlar: 1080p, 720p, 480p, 360p
        target_resolutions = [
            ("1080p", 1080, "1080p Full HD"),
            ("720p", 720, "720p HD"),
            ("480p", 480, "480p SD"),
            ("360p", 360, "360p Standart"),
        ]
        
        # Videoda mavjud barcha video balandliklari
        available_heights = set(f.get("height") for f in raw_formats if f.get("height"))
        
        # Agar videoning maksimal balandligi 1080p dan yuqori bo'lsa (masalan 4K / 2K)
        max_height = max(available_heights) if available_heights else 720
        if max_height > 1080:
            target_resolutions.insert(0, (f"{max_height}p", max_height, f"{max_height}p Ultra HD"))

        for res_code, height, label in target_resolutions:
            if any(h and h >= height for h in available_heights):
                # Mos formatni topish
                matched_video = [f for f in raw_formats if f.get("height") == height and f.get("vcodec") != "none"]
                size_label = "Noma'lum"
                if matched_video:
                    size_label = _estimate_merged_size(matched_video[-1], best_audio, duration_sec)
                
                formats_list.append(
                    FormatInfo(
                        format_id=f"yt_{res_code}",
                        quality=label,
                        ext="mp4",
                        filesize=size_label,
                        download_endpoint=f"/api/download?url={encoded_url}&format_id=yt_{res_code}",
                    )
                )

        # Agar birorta ham res mos kelmasa, umumiy eng yaxshi sifat
        if not formats_list:
            formats_list.append(
                FormatInfo(
                    format_id="yt_best",
                    quality="Eng yuqori sifat (MP4)",
                    ext="mp4",
                    filesize="Avtomatik",
                    download_endpoint=f"/api/download?url={encoded_url}&format_id=yt_best",
                )
            )

        # Faqat Audio (MP3) qo'shish
        formats_list.append(
            FormatInfo(
                format_id="yt_mp3",
                quality="Faqat Audio (MP3)",
                ext="mp3",
                filesize=audio_size_str,
                download_endpoint=f"/api/download?url={encoded_url}&format_id=yt_mp3",
            )
        )

    elif platform in ["tiktok", "instagram"]:
        # TikTok va Instagram uchun odatda bitta HD MP4 va Audio
        # Hajmni aniqlash
        filesize_str = "Avtomatik"
        if raw_formats:
            best_fmt = raw_formats[-1]
            filesize_str = _estimate_format_size(best_fmt, duration_sec)
        elif info.get("filesize") or info.get("filesize_approx"):
            filesize_str = format_bytes(info.get("filesize") or info.get("filesize_approx"))

        formats_list.append(
            FormatInfo(
                format_id=f"{platform}_best",
                quality="Eng yuqori sifat (HD)",
                ext="mp4",
                filesize=filesize_str,
                download_endpoint=f"/api/download?url={encoded_url}&format_id={platform}_best",
            )
        )
        formats_list.append(
            FormatInfo(
                format_id=f"{platform}_mp3",
                quality="Faqat Audio (MP3)",
                ext="mp3",
                filesize=audio_size_str if audio_size_str != "Noma'lum" else "~5 MB",
                download_endpoint=f"/api/download?url={encoded_url}&format_id={platform}_mp3",
            )
        )

    else:
        # Boshqa platformalar (Twitter/X, Facebook va h.k.)
        formats_list.append(
            FormatInfo(
                format_id="best_video",
                quality="Eng yuqori sifat (MP4)",
                ext="mp4",
                filesize="Avtomatik",
                download_endpoint=f"/api/download?url={encoded_url}&format_id=best_video",
            )
        )
        formats_list.append(
            FormatInfo(
                format_id="best_audio",
                quality="Faqat Audio (MP3)",
                ext="mp3",
                filesize=audio_size_str,
                download_endpoint=f"/api/download?url={encoded_url}&format_id=best_audio",
            )
        )

    return ParseResponse(
        title=title,
        thumbnail=thumbnail,
        duration=duration_str,
        platform=platform,
        formats=formats_list,
        author=author,
    )


def _build_download_opts(format_id: str, out_template: str) -> Dict[str, Any]:
    """Format ID ga qarab yuklab olish parametrlarini shakllantiradi."""
    opts = _get_base_ydl_opts()
    opts["outtmpl"] = out_template

    is_audio = format_id.endswith("_mp3") or format_id == "mp3" or format_id == "best_audio"

    if is_audio:
        opts["format"] = "bestaudio/best"
        if FFMPEG_PATH:
            opts["postprocessors"] = [{
                "key": "FFmpegExtractAudio",
                "preferredcodec": "mp3",
                "preferredquality": "192",
            }]
    elif format_id.startswith("yt_"):
        # YouTube sifatlari
        height_str = format_id.replace("yt_", "").replace("p", "")
        if height_str.isdigit():
            h = int(height_str)
            # bestvideo + bestaudio agar ffmpeg bo'lsa, aks holda progressive best
            opts["format"] = f"bestvideo[height<={h}]+bestaudio/best[height<={h}]/best"
        else:
            opts["format"] = "bestvideo+bestaudio/best"
            
        if FFMPEG_PATH:
            opts["merge_output_format"] = "mp4"
            opts["postprocessors"] = [{
                "key": "FFmpegVideoRemuxer",
                "preferedformat": "mp4",
            }]
    elif format_id in ["tiktok_best", "instagram_best", "best_video"]:
        opts["format"] = "bestvideo+bestaudio/best"
        if FFMPEG_PATH:
            opts["merge_output_format"] = "mp4"
    else:
        # Boshqa maxsus format id berilgan bo'lsa
        opts["format"] = format_id

    return opts


async def download_media_file(url: str, format_id: str) -> Tuple[str, str, str, str]:
    """
    Videoni yoki audioni vaqtinchalik papkaga yuklaydi.
    Qaytaradi: (faylning_to'liq_yo'li, fayl_nomi, mime_type, vaqtinchalik_papka_yo'li)
    """
    temp_dir = tempfile.mkdtemp(prefix="snapany_")
    out_template = os.path.join(temp_dir, "%(title)s.%(ext)s")
    
    opts = _build_download_opts(format_id, out_template)

    def _download():
        with yt_dlp.YoutubeDL(opts) as ydl:
            # Info olish va yuklash
            info = ydl.extract_info(url, download=True)
            return info

    loop = asyncio.get_event_loop()
    try:
        info = await loop.run_in_executor(None, _download)
    except Exception as e:
        # Xatolik bo'lsa vaqtinchalik papkani tozalash
        shutil.rmtree(temp_dir, ignore_errors=True)
        raise ValueError(f"Faylni yuklab olishda xatolik yuz berdi: {str(e)}")

    # Yuklab olingan faylni topish
    files = [f for f in os.listdir(temp_dir) if not f.endswith(".part") and not f.endswith(".ytdl")]
    if not files:
        shutil.rmtree(temp_dir, ignore_errors=True)
        raise ValueError("Fayl yuklab olindi, ammo yakuniy fayl topilmadi.")

    filename = files[0]
    file_path = os.path.join(temp_dir, filename)

    is_audio = format_id.endswith("_mp3") or format_id == "mp3" or format_id == "best_audio" or filename.lower().endswith(".mp3")
    mime_type = "audio/mpeg" if is_audio else "video/mp4"

    # Xavfsiz va chiroyli fayl nomi
    raw_title = info.get("title", "download") if info else "download"
    clean_title = sanitize_filename(raw_title)
    ext = "mp3" if is_audio else (filename.split(".")[-1] if "." in filename else "mp4")
    safe_download_name = f"{clean_title}.{ext}"

    return file_path, safe_download_name, mime_type, temp_dir
