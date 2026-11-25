import os
import shutil
from pathlib import Path

# Bazaviy yo'llar
BASE_DIR = Path(__file__).resolve().parent.parent
STATIC_DIR = BASE_DIR / "static"
DOWNLOAD_DIR = BASE_DIR / "downloads"
DOWNLOAD_DIR.mkdir(parents=True, exist_ok=True)

# FFmpeg yo'lini aniqlash
def get_ffmpeg_path() -> str | None:
    # 1. Tizimdagi ffmpeg
    system_ffmpeg = shutil.which("ffmpeg")
    if system_ffmpeg:
        return system_ffmpeg
    
    # 2. imageio-ffmpeg orqali o'rnatilgan binary
    try:
        import imageio_ffmpeg
        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        return None

# Node.js JS runtime yo'li (YouTube signature deciphering uchun)
def get_node_path() -> str | None:
    return shutil.which("node")

FFMPEG_PATH = get_ffmpeg_path()
NODE_PATH = get_node_path()

# User-Agent sozlamalari
DEFAULT_USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)

