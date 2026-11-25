package com.snapany.app.ui.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snapany.app.model.DownloadedItem
import com.snapany.app.model.FormatDownloadState
import com.snapany.app.model.FormatInfo
import com.snapany.app.model.ParseResponse
import com.snapany.app.network.SnapAnyApi
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SnapAnyViewModel(application: Application) : AndroidViewModel(application) {
    private val api = SnapAnyApi(application)
    private val historyFile = File(application.filesDir, "snapany_history.json")

    var urlInput by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var isServerOnline by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var parsedMedia by mutableStateOf<ParseResponse?>(null)

    var serverUrlInput by mutableStateOf(api.getServerUrl())
    var isCheckingHealth by mutableStateOf(false)

    val downloadStates = mutableStateMapOf<String, FormatDownloadState>()
    var downloadHistory by mutableStateOf<List<DownloadedItem>>(emptyList())

    init {
        loadHistory()
        checkHealth()
    }

    fun checkHealth(customUrl: String? = null) {
        viewModelScope.launch {
            isCheckingHealth = true
            isServerOnline = api.checkHealth(customUrl)
            isCheckingHealth = false
        }
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
        if (!clip.isNullOrEmpty()) {
            urlInput = clip
            errorMessage = null
        } else {
            Toast.makeText(getApplication(), "Buferda havola topilmadi", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearInput() {
        urlInput = ""
        errorMessage = null
        parsedMedia = null
        downloadStates.clear()
    }

    fun analyzeUrl(overrideUrl: String? = null) {
        val target = (overrideUrl ?: urlInput).trim()
        if (overrideUrl != null) {
            urlInput = target
        }

        if (target.isEmpty()) {
            errorMessage = "Iltimos, avval video havolasini kiriting."
            return
        }
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            errorMessage = "Havola http:// yoki https:// bilan boshlanishi kerak."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            parsedMedia = null
            downloadStates.clear()

            val result = api.parseUrl(target)
            result.onSuccess { response ->
                parsedMedia = response
                isServerOnline = true
            }.onFailure { err ->
                errorMessage = err.message ?: "Noma'lum xatolik yuz berdi"
            }

            isLoading = false
        }
    }

    fun startDownload(media: ParseResponse, format: FormatInfo) {
        if (downloadStates[format.formatId] is FormatDownloadState.Downloading) return

        viewModelScope.launch {
            downloadStates[format.formatId] = FormatDownloadState.Downloading(0.03f)
            val result = api.downloadFormat(media, format) { progress ->
                downloadStates[format.formatId] = FormatDownloadState.Downloading(progress)
            }

            result.onSuccess { file ->
                downloadStates[format.formatId] = FormatDownloadState.Completed(file.absolutePath)
                val item = DownloadedItem(
                    title = media.title,
                    thumbnailUrl = media.thumbnail,
                    platform = media.platform,
                    quality = format.quality,
                    ext = format.ext,
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length()
                )
                downloadHistory = listOf(item) + downloadHistory
                saveHistory()
                Toast.makeText(getApplication(), "Yuklandi: ${format.quality}", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                downloadStates[format.formatId] = FormatDownloadState.Failed(err.message ?: "Xatolik")
            }
        }
    }

    fun saveServerUrl(newUrl: String) {
        if (newUrl.isBlank()) return
        api.setServerUrl(newUrl)
        serverUrlInput = api.getServerUrl()
        checkHealth(serverUrlInput)
        Toast.makeText(getApplication(), "Server manzili saqlandi", Toast.LENGTH_SHORT).show()
    }

    fun deleteHistoryItem(item: DownloadedItem) {
        File(item.filePath).delete()
        downloadHistory = downloadHistory.filterNot { it.id == item.id }
        saveHistory()
    }

    fun clearAllDownloads() {
        val dir = File(getApplication<Application>().filesDir, "downloads")
        dir.deleteRecursively()
        downloadHistory = emptyList()
        saveHistory()
        Toast.makeText(getApplication(), "Barcha yuklab olishlar tozalandi", Toast.LENGTH_SHORT).show()
    }

    fun totalCacheSizeText(): String {
        val total = downloadHistory.sumOf { it.fileSizeBytes }
        if (total <= 0L) return "0 MB"
        val mb = total / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }

    fun shareFile(context: Context, item: DownloadedItem) {
        val file = File(item.filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = if (item.isAudio) "audio/mpeg" else "video/mp4"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Faylni ulashish"))
    }

    private fun loadHistory() {
        if (!historyFile.exists()) return
        try {
            val arr = JSONArray(historyFile.readText())
            val list = mutableListOf<DownloadedItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val path = obj.optString("filePath")
                if (File(path).exists()) {
                    list.add(
                        DownloadedItem(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            thumbnailUrl = obj.optString("thumbnailUrl"),
                            platform = obj.optString("platform"),
                            quality = obj.optString("quality"),
                            ext = obj.optString("ext"),
                            fileName = obj.optString("fileName"),
                            filePath = path,
                            fileSizeBytes = obj.optLong("fileSizeBytes"),
                            createdAt = obj.optLong("createdAt")
                        )
                    )
                }
            }
            downloadHistory = list
        } catch (_: Exception) {
        }
    }

    private fun saveHistory() {
        try {
            val arr = JSONArray()
            downloadHistory.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("thumbnailUrl", item.thumbnailUrl)
                        put("platform", item.platform)
                        put("quality", item.quality)
                        put("ext", item.ext)
                        put("fileName", item.fileName)
                        put("filePath", item.filePath)
                        put("fileSizeBytes", item.fileSizeBytes)
                        put("createdAt", item.createdAt)
                    }
                )
            }
            historyFile.writeText(arr.toString())
        } catch (_: Exception) {
        }
    }
}
