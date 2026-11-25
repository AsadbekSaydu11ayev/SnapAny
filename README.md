# SnapAny - Universal Video & Audio Downloader 🚀

**SnapAny** — bu YouTube, TikTok va Instagram platformalaridan video va audiolar yuklab olish uchun mo'ljallangan, zamonaviy, to'liq Dark Mode dizayndagi shaxsiy Web ilova.

Hech qanday ro'yxatdan o'tish, obuna, to'lovlar yoki reklamalarsiz toza va tezkor ishlaydi.

---

## 🌟 Asosiy Imkoniyatlari

- **Platformalar:** YouTube, TikTok, Instagram (va boshqa ko'plab platformalar).
- **Video Sifatlari:** 1080p Full HD, 720p HD, 480p SD, 360p (MP4 formatda).
- **Audio Ajratish:** Videodan faqat audioni alohida MP3 formatda yuklab olish.
- **Tezkor Tahlil:** Eng so'nggi `yt-dlp` dvigateli yordamida soniyalarda metama'lumotlarni olish.
- **Zamonaviy Interfeys:** SnapAny uslubidagi to'liq Dark Mode, Glassmorphism effektlari, clipboarddan bitta bosishda havola qo'yish (Paste) va moslashuvchan dizayn (Mobile & Desktop).
- **Xavfsiz Streaming:** Server orqali yuklab uzatish (Proxy stream) va avtomatik disk tozalash mexanizmi (vaqtinchalik fayllar foydalanuvchiga yuborilgach darhol o'chiriladi).

---

## 🛠 Texnologik Stack

- **Backend:** Python 3.10+ & FastAPI
- **Dvigatel:** `yt-dlp` (eng so'nggi versiya)
- **Audio/Video Qayta Ishlash:** `imageio-ffmpeg` (o'rnatilgan static FFmpeg binary)
- **Frontend:** HTML5, Tailwind CSS (CDN), Vanilla JavaScript

---

## 📁 Loyiha Tuzilishi

```text
SnapAny/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI asosiy ilovasi va endpointlar
│   ├── config.py            # Konfiguratsiyalar va yo'llar
│   ├── schemas.py           # Pydantic ma'lumotlar modellari
│   ├── utils.py             # Yordamchi funksiyalar (vaqt, hajm, xavfsizlik)
│   └── services/
│       ├── __init__.py
│       └── downloader.py    # yt-dlp tahlil va yuklash xizmati
├── static/
│   ├── index.html           # SnapAny foydalanuvchi interfeysi
│   ├── css/
│   │   └── style.css        # Maxsus Dark Mode va animatsiya stillari
│   └── js/
│       └── app.js           # Frontend interaktiv logikasi
├── requirements.txt         # Kerakli Python kutubxonalari
├── .gitignore
└── README.md
```

---

## 🚀 O'rnatish va Ishga Tushirish

### 1. Loyiha papkasiga kiring:
```bash
cd "SnapAny"
```

### 2. Python virtual muhitini yarating va faollashtiring:
```bash
# Virtual muhit yaratish
python3 -m venv venv

# Faollashtirish (macOS / Linux):
source venv/bin/activate

# Windows (PowerShell):
# .\venv\Scripts\Activate.ps1
```

### 3. Kerakli kutubxonalarni o'rnating:
```bash
pip install --upgrade pip
pip install -r requirements.txt
```

*(Eslatma: `imageio-ffmpeg` paketining o'zi FFmpeg binarisini o'z ichiga olgani sababli, tizimga qo'shimcha FFmpeg o'rnatish shart emas!)*

### 4. Ilovani ishga tushiring:
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 5. Brauzerda oching:
- **Veb-ilova:** [http://localhost:8000](http://localhost:8000)
- **Interaktiv API hujjatlari (Swagger UI):** [http://localhost:8000/docs](http://localhost:8000/docs)

---

## 🔌 API Endpointlar Tavsifi

### 1. `POST /api/parse`
Havolani tahlil qilib, mavjud formatlar va metama'lumotlarni qaytaradi.

**So'rov (Request JSON):**
```json
{
  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
}
```

**Javob (Response JSON):**
```json
{
  "title": "Rick Astley - Never Gonna Give You Up (Official Video) (4K Remaster)",
  "thumbnail": "https://i.ytimg.com/vi_webp/dQw4w9WgXcQ/maxresdefault.webp",
  "duration": "03:33",
  "platform": "youtube",
  "formats": [
    {
      "format_id": "yt_1080p",
      "quality": "1080p Full HD",
      "ext": "mp4",
      "filesize": "32.3 MB",
      "download_endpoint": "/api/download?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DdQw4w9WgXcQ&format_id=yt_1080p"
    },
    {
      "format_id": "yt_720p",
      "quality": "720p HD",
      "ext": "mp4",
      "filesize": "20.1 MB",
      "download_endpoint": "/api/download?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DdQw4w9WgXcQ&format_id=yt_720p"
    },
    {
      "format_id": "yt_mp3",
      "quality": "Faqat Audio (MP3)",
      "ext": "mp3",
      "filesize": "3.3 MB",
      "download_endpoint": "/api/download?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DdQw4w9WgXcQ&format_id=yt_mp3"
    }
  ],
  "author": "Rick Astley"
}
```

### 2. `GET /api/download`
Parametrlar: `url` va `format_id`.
Tanlangan formatdagi video yoki audioni server orqali tayyorlab, brauzerga `attachment` bilan yuklab olish uchun uzatadi.

---

## 🔒 Xavfsizlik va Tozalash Mexanizmi

- Serverda foydalanuvchilar yuklagan fayllar to'planib qolmaydi: FastAPI'ning `BackgroundTask` imkoniyati yordamida fayl to'liq uzatib bo'lingach, vaqtinchalik papkadan zudlik bilan o'chirib tashlanadi.
- Xavfli fayl nomlari (`/`, `\`, quotes va hk) avtomatik tarzda tozalangan xavfsiz nomga keltiriladi.

