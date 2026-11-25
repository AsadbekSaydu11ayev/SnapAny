package com.snapany.app.network

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.snapany.app.model.FormatInfo
import com.snapany.app.model.ParseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class SnapAnyApi(private val context: Context) {

    companion object {
        const val DEFAULT_SERVER_URL = "https://snapany.onrender.com"
        private const val PREFS_NAME = "snapany_prefs"
        private const val KEY_SERVER_URL = "server_url"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getServerUrl(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL)?.trim() ?: DEFAULT_SERVER_URL
        val normalized = if (saved.isEmpty()) DEFAULT_SERVER_URL else saved
        return normalized.trimEnd('/')
    }

    fun setServerUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        val finalUrl = if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            cleaned
        } else {
            "https://$cleaned"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER_URL, finalUrl)
            .apply()
    }

    suspend fun checkHealth(customBaseUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        val base = (customBaseUrl ?: getServerUrl()).trimEnd('/')
        val request = Request.Builder()
            .url("$base/api/health")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body?.string() ?: return@withContext false
                val json = JSONObject(body)
                json.optString("status") == "ok"
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun parseUrl(videoUrl: String): Result<ParseResponse> = withContext(Dispatchers.IO) {
        try {
            val base = getServerUrl()
            val payload = JSONObject().put("url", videoUrl).toString()
            val body = payload.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("$base/api/parse")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = try {
                        val json = JSONObject(raw)
                        json.optString("detail").ifEmpty { json.optString("error", "Server xatosi: ${response.code}") }
                    } catch (_: Exception) {
                        "Server xatosi (${response.code})"
                    }
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(raw)
                val formatsArray = json.optJSONArray("formats")
                val formats = mutableListOf<FormatInfo>()
                if (formatsArray != null) {
                    for (i in 0 until formatsArray.length()) {
                        val item = formatsArray.getJSONObject(i)
                        formats.add(
                            FormatInfo(
                                formatId = item.optString("format_id"),
                                quality = item.optString("quality"),
                                ext = item.optString("ext"),
                                filesize = item.optString("filesize", "Noma'lum"),
                                downloadEndpoint = item.optString("download_endpoint")
                            )
                        )
                    }
                }

                val parsed = ParseResponse(
                    title = json.optString("title", "Nomsiz video"),
                    thumbnail = json.optString("thumbnail", ""),
                    duration = json.optString("duration", "Noma'lum"),
                    platform = json.optString("platform", "other"),
                    formats = formats,
                    author = json.optString("author").takeIf { it.isNotEmpty() && it != "null" }
                )
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Tarmoq xatosi: ${e.localizedMessage ?: "ulanib bo'lmadi"}"))
        }
    }

    suspend fun downloadFormat(
        media: ParseResponse,
        format: FormatInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val base = getServerUrl()
            val fullUrl = "$base${format.downloadEndpoint}"
            val request = Request.Builder().url(fullUrl).get().build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val raw = response.body?.string().orEmpty()
                    val msg = try {
                        JSONObject(raw).optString("detail", "Yuklash xatosi (${response.code})")
                    } catch (_: Exception) {
                        "Yuklash xatosi (${response.code})"
                    }
                    return@withContext Result.failure(Exception(msg))
                }

                val body = response.body ?: return@withContext Result.failure(Exception("Bo'sh javob keldi"))
                val totalBytes = body.contentLength()
                val rawFileName = extractFileName(
                    response.header("Content-Disposition"),
                    fallbackTitle = media.title,
                    ext = format.ext
                )

                val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
                val uniqueName = "${System.currentTimeMillis()}_$rawFileName"
                val outFile = File(downloadsDir, uniqueName)

                body.byteStream().use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0) {
                                onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0.02f, 0.99f))
                            } else {
                                onProgress(0.55f)
                            }
                        }
                    }
                }

                // Telefonning umumiy Downloads papkasiga ham nusxalash
                exportToPublicDownloads(outFile, rawFileName, format.isAudio)

                onProgress(1f)
                Result.success(outFile)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Yuklab olishda xatolik"))
        }
    }

    private fun exportToPublicDownloads(sourceFile: File, displayName: String, isAudio: Boolean) {
        try {
            val mimeType = if (isAudio) "audio/mpeg" else "video/mp4"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SnapAny")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { it.copyTo(out) }
                    }
                }
            } else {
                val pubDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "SnapAny"
                ).apply { mkdirs() }
                sourceFile.copyTo(File(pubDir, displayName), overwrite = true)
            }
        } catch (_: Exception) {
            // Ilova ichidagi nusxa baribir saqlangan
        }
    }

    private fun extractFileName(disposition: String?, fallbackTitle: String, ext: String): String {
        if (!disposition.isNullOrBlank()) {
            val utf8Marker = "filename*=UTF-8''"
            val idx = disposition.indexOf(utf8Marker)
            if (idx >= 0) {
                val raw = disposition.substring(idx + utf8Marker.length).substringBefore(";").trim()
                val decoded = URLDecoder.decode(raw, "UTF-8")
                if (decoded.isNotEmpty()) return sanitizeFileName(decoded)
            }
            val quoteMarker = "filename=\""
            val qIdx = disposition.indexOf(quoteMarker)
            if (qIdx >= 0) {
                val raw = disposition.substring(qIdx + quoteMarker.length).substringBefore("\"")
                if (raw.isNotEmpty()) return sanitizeFileName(raw)
            }
        }
        return "${sanitizeFileName(fallbackTitle)}.$ext"
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80)
    }
}
