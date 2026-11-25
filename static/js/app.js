// SnapAny Frontend Application Logic

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elementlari
    const parseForm = document.getElementById('parseForm');
    const urlInput = document.getElementById('urlInput');
    const clearBtn = document.getElementById('clearBtn');
    const pasteBtn = document.getElementById('pasteBtn');
    const submitBtn = document.getElementById('submitBtn');
    const btnText = document.getElementById('btnText');
    const btnIcon = document.getElementById('btnIcon');
    const btnSpinner = document.getElementById('btnSpinner');

    const errorBox = document.getElementById('errorBox');
    const errorMessage = document.getElementById('errorMessage');
    const closeErrorBtn = document.getElementById('closeErrorBtn');

    const loadingBox = document.getElementById('loadingBox');
    const resultBox = document.getElementById('resultBox');
    const resThumbnail = document.getElementById('resThumbnail');
    const resDuration = document.getElementById('resDuration');
    const resPlatformBadge = document.getElementById('resPlatformBadge');
    const resTitle = document.getElementById('resTitle');
    const resAuthor = document.getElementById('resAuthor');
    const formatsList = document.getElementById('formatsList');
    const resetBtn = document.getElementById('resetBtn');

    // Input tozalash tugmasini boshqarish
    urlInput.addEventListener('input', () => {
        if (urlInput.value.trim().length > 0) {
            clearBtn.classList.remove('hidden');
        } else {
            clearBtn.classList.add('hidden');
        }
    });

    clearBtn.addEventListener('click', () => {
        urlInput.value = '';
        clearBtn.classList.add('hidden');
        urlInput.focus();
    });

    // URL parametri orqali kelgan havolani avtomatik tahlil qilish (masalan Android Share orqali)
    const urlParams = new URLSearchParams(window.location.search);
    const sharedUrl = urlParams.get('url');
    if (sharedUrl && sharedUrl.trim().length > 0) {
        urlInput.value = sharedUrl.trim();
        clearBtn.classList.remove('hidden');
        setTimeout(() => handleParse(), 200);
    }

    // Clipboarddan Paste qilish
    pasteBtn.addEventListener('click', async () => {
        try {
            if (window.AndroidBridge && typeof window.AndroidBridge.getClipboardText === 'function') {
                const text = window.AndroidBridge.getClipboardText();
                if (text && text.trim().length > 0) {
                    urlInput.value = text.trim();
                    clearBtn.classList.remove('hidden');
                    urlInput.focus();
                    handleParse();
                } else {
                    showError("Xotirada havola topilmadi.");
                }
                return;
            }
            if (navigator.clipboard && navigator.clipboard.readText) {
                const text = await navigator.clipboard.readText();
                if (text && text.trim().length > 0) {
                    urlInput.value = text.trim();
                    clearBtn.classList.remove('hidden');
                    urlInput.focus();
                    // Avtomatik tahlilni boshlash
                    handleParse();
                } else {
                    showError("Xotirada havola topilmadi.");
                }
            } else {
                showError("Brauzeringiz clipboarddan o'qishni qo'llab-quvvatlamaydi. Iltimos, havolani qo'lda joylang.");
            }
        } catch (err) {
            console.warn("Clipboard access error:", err);
            showError("Xotiradan o'qishga ruxsat berilmadi yoki xatolik yuz berdi. Iltimos, havolani qo'lda joylang.");
        }
    });

    // Xatolik xabarini yopish
    closeErrorBtn.addEventListener('click', hideError);

    // Yangi havola tugmasi
    resetBtn.addEventListener('click', () => {
        resultBox.classList.add('hidden');
        urlInput.value = '';
        clearBtn.classList.add('hidden');
        urlInput.focus();
    });

    // Form submit hodisasi
    parseForm.addEventListener('submit', (e) => {
        e.preventDefault();
        handleParse();
    });

    // Asosiy tahlil qilish funksiyasi
    async function handleParse() {
        const url = urlInput.value.trim();
        if (!url) {
            showError("Iltimos, video havolasini kiriting.");
            urlInput.focus();
            return;
        }

        if (!url.startsWith('http://') && !url.startsWith('https://')) {
            showError("Yaroqsiz havola. Havola http:// yoki https:// bilan boshlanishi kerak.");
            return;
        }

        hideError();
        resultBox.classList.add('hidden');
        setLoading(true);

        try {
            const response = await fetch('/api/parse', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ url }),
            });

            const data = await response.json();

            if (!response.ok) {
                const msg = data.detail || data.error || "Videoni tahlil qilishda xatolik yuz berdi.";
                throw new Error(msg);
            }

            renderResult(data);
        } catch (error) {
            showError(error.message || "Server bilan bog'lanishda xatolik.");
        } finally {
            setLoading(false);
        }
    }

    // Natijani DOM-ga chiqarish
    function renderResult(data) {
        // Thumbnail
        resThumbnail.src = data.thumbnail || 'https://via.placeholder.com/640x360?text=No+Thumbnail';
        resDuration.textContent = data.duration || '00:00';

        // Platforma nishoni
        const platform = (data.platform || 'video').toLowerCase();
        resPlatformBadge.textContent = platform;
        
        if (platform === 'youtube') {
            resPlatformBadge.className = "px-2.5 py-1 rounded-lg text-xs font-semibold uppercase bg-red-500/20 text-red-300 border border-red-500/30";
        } else if (platform === 'tiktok') {
            resPlatformBadge.className = "px-2.5 py-1 rounded-lg text-xs font-semibold uppercase bg-cyan-500/20 text-cyan-300 border border-cyan-500/30";
        } else if (platform === 'instagram') {
            resPlatformBadge.className = "px-2.5 py-1 rounded-lg text-xs font-semibold uppercase bg-pink-500/20 text-pink-300 border border-pink-500/30";
        } else {
            resPlatformBadge.className = "px-2.5 py-1 rounded-lg text-xs font-semibold uppercase bg-indigo-500/20 text-indigo-300 border border-indigo-500/30";
        }

        // Sarlavha va muallif
        resTitle.textContent = data.title || "Nomsiz video";
        if (data.author) {
            resAuthor.innerHTML = `
                <svg class="w-3.5 h-3.5 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
                </svg>
                <span>${escapeHtml(data.author)}</span>
            `;
            resAuthor.classList.remove('hidden');
        } else {
            resAuthor.classList.add('hidden');
        }

        // Formatlar ro'yxatini shakllantirish
        formatsList.innerHTML = '';

        if (!data.formats || data.formats.length === 0) {
            formatsList.innerHTML = `
                <div class="p-4 rounded-xl bg-slate-800/50 text-slate-400 text-xs text-center border border-slate-700/50">
                    Yuklab olish uchun mos formatlar topilmadi.
                </div>
            `;
        } else {
            data.formats.forEach((fmt) => {
                const card = createFormatCard(fmt);
                formatsList.appendChild(card);
            });
        }

        resultBox.classList.remove('hidden');
        // Sahifani natijaga qarab pastroqqa silliq surish
        resultBox.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    // Format kartochkasini yaratish
    function createFormatCard(fmt) {
        const isAudio = fmt.ext === 'mp3' || fmt.quality.toLowerCase().includes('audio');
        const card = document.createElement('div');
        card.className = "glass-card glass-card-hover rounded-xl p-3 sm:p-3.5 flex items-center justify-between gap-3 border border-slate-800/80";

        const iconSvg = isAudio
            ? `<svg class="w-4 h-4 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                 <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3"></path>
               </svg>`
            : `<svg class="w-4 h-4 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                 <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
               </svg>`;

        const badgeClass = isAudio
            ? "bg-purple-500/10 text-purple-400 border border-purple-500/20"
            : "bg-indigo-500/10 text-indigo-400 border border-indigo-500/20";

        card.innerHTML = `
            <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-lg bg-slate-800/80 flex items-center justify-center flex-shrink-0 border border-white/5">
                    ${iconSvg}
                </div>
                <div>
                    <div class="flex items-center gap-2">
                        <span class="text-sm font-semibold text-white tracking-wide">${escapeHtml(fmt.quality)}</span>
                        <span class="text-[10px] font-bold px-1.5 py-0.5 rounded uppercase ${badgeClass}">
                            ${fmt.ext}
                        </span>
                    </div>
                    <div class="text-[11px] text-slate-400 mt-0.5">
                        Hajmi: <span class="text-slate-300 font-medium">${escapeHtml(fmt.filesize)}</span>
                    </div>
                </div>
            </div>

            <div>
                <button 
                    type="button" 
                    class="download-btn px-4 py-2 rounded-xl bg-gradient-to-r ${isAudio ? 'from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500' : 'from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500'} text-white text-xs font-semibold shadow-md flex items-center gap-1.5 transition-all transform active:scale-95"
                    data-endpoint="${fmt.download_endpoint}"
                >
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                    </svg>
                    <span class="btn-lbl">Yuklab olish</span>
                </button>
            </div>
        `;

        // Yuklab olish tugmasi hodisasi
        const dlBtn = card.querySelector('.download-btn');
        const dlLbl = card.querySelector('.btn-lbl');
        dlBtn.addEventListener('click', () => {
            triggerDownload(fmt.download_endpoint, dlBtn, dlLbl);
        });

        return card;
    }

    // Yuklab olishni boshlash
    function triggerDownload(endpoint, btn, lbl) {
        if (!endpoint) return;

        // Tugmani "Tayyorlanmoqda..." holatiga o'tkazish
        const originalContent = btn.innerHTML;
        btn.disabled = true;
        btn.classList.add('opacity-80', 'cursor-wait');
        btn.innerHTML = `
            <svg class="animate-spin w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
            </svg>
            <span>Tayyorlanmoqda...</span>
        `;

        // Agar Android ilova ichida bo'lsa, native DownloadManager orqali yuklash
        if (window.AndroidBridge && typeof window.AndroidBridge.downloadFile === 'function') {
            const fullUrl = new URL(endpoint, window.location.origin).href;
            window.AndroidBridge.downloadFile(fullUrl);
            setTimeout(() => {
                btn.disabled = false;
                btn.classList.remove('opacity-80', 'cursor-wait');
                btn.innerHTML = originalContent;
            }, 2500);
            return;
        }

        // Brauzer orqali yuklab olish
        // Iframe orqali uzatish sahifaning qotib qolishini oldini oladi
        const iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        iframe.src = endpoint;
        document.body.appendChild(iframe);

        // 5 soniyadan so'ng iframe ni tozalash va tugmani qaytarish
        setTimeout(() => {
            if (iframe && iframe.parentNode) {
                iframe.parentNode.removeChild(iframe);
            }
            btn.disabled = false;
            btn.classList.remove('opacity-80', 'cursor-wait');
            btn.innerHTML = originalContent;
        }, 5000);
    }

    // Yuklanish holatini o'zgartirish
    function setLoading(isLoading) {
        if (isLoading) {
            submitBtn.disabled = true;
            btnText.textContent = "Kutilmoqda...";
            btnIcon.classList.add('hidden');
            btnSpinner.classList.remove('hidden');
            loadingBox.classList.remove('hidden');
        } else {
            submitBtn.disabled = false;
            btnText.textContent = "Tekshirish";
            btnIcon.classList.remove('hidden');
            btnSpinner.classList.add('hidden');
            loadingBox.classList.add('hidden');
        }
    }

    // Xatolik ko'rsatish
    function showError(msg) {
        errorMessage.textContent = msg;
        errorBox.classList.remove('hidden');
        errorBox.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    // Xatolikni yashirish
    function hideError() {
        errorBox.classList.add('hidden');
    }

    // XSS himoyasi uchun yordamchi funksiya
    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});

