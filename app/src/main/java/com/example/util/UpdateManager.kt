package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object UpToDate : UpdateState
    data class UpdateAvailable(
        val versionName: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val fileName: String
    ) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class Downloaded(val fileUri: Uri, val filePath: String) : UpdateState
    data class Error(val message: String) : UpdateState
}

class UpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    suspend fun checkForUpdates(repoOwnerRepo: String = "plusemon/piprapay-connect") = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Checking
        try {
            val url = "https://api.github.com/repos/${repoOwnerRepo.trim()}/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "PipraPay-Connect-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    _updateState.value = UpdateState.Error("Repository or Release not found on GitHub")
                    return@withContext
                }
                if (!response.isSuccessful) {
                    _updateState.value = UpdateState.Error("GitHub API returned error: HTTP ${response.code}")
                    return@withContext
                }

                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    _updateState.value = UpdateState.Error("Empty response from GitHub API")
                    return@withContext
                }

                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").trim()
                val bodyText = json.optString("body", "No release notes available.")
                val assetsArray = json.optJSONArray("assets")

                if (tagName.isBlank()) {
                    _updateState.value = UpdateState.Error("No releases or tags found")
                    return@withContext
                }

                var downloadUrl = ""
                var apkFileName = "piprapay-connect-$tagName.apk"

                if (assetsArray != null && assetsArray.length() > 0) {
                    for (i in 0 until assetsArray.length()) {
                        val asset = assetsArray.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            apkFileName = name
                            break
                        }
                    }
                }

                // If no direct APK asset, try html_url
                if (downloadUrl.isBlank()) {
                    downloadUrl = json.optString("html_url", "")
                    if (downloadUrl.isBlank()) {
                        _updateState.value = UpdateState.Error("No downloadable APK found in the latest release")
                        return@withContext
                    }
                }

                val currentVersion = BuildConfig.VERSION_NAME
                if (isNewerVersion(currentVersion, tagName)) {
                    _updateState.value = UpdateState.UpdateAvailable(
                        versionName = tagName,
                        releaseNotes = bodyText,
                        downloadUrl = downloadUrl,
                        fileName = apkFileName
                    )
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            }
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Failed to check for updates: ${e.localizedMessage ?: "Connection error"}")
        }
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.removePrefix("v").trim()
        val cleanLatest = latest.removePrefix("v").trim()
        if (cleanCurrent == cleanLatest) return false

        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val currVal = currentParts.getOrElse(i) { 0 }
            val latVal = latestParts.getOrElse(i) { 0 }
            if (latVal > currVal) return true
            if (currVal > latVal) return false
        }
        return false
    }

    suspend fun downloadUpdate(downloadUrl: String, fileName: String) = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Downloading(0f)
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "PipraPay-Connect-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _updateState.value = UpdateState.Error("Failed to download APK: HTTP ${response.code}")
                    return@withContext
                }

                val responseBody = response.body
                if (responseBody == null) {
                    _updateState.value = UpdateState.Error("Empty body from update server")
                    return@withContext
                }

                val totalBytes = responseBody.contentLength()
                val updatesDir = File(context.cacheDir, "updates")
                if (!updatesDir.exists()) {
                    updatesDir.mkdirs()
                }

                val apkFile = File(updatesDir, fileName)
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(apkFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead: Long = 0

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (totalBytes > 0) {
                                val progress = totalBytesRead.toFloat() / totalBytes
                                _updateState.value = UpdateState.Downloading(progress)
                            }
                        }
                    }
                }

                val authority = "${context.packageName}.provider"
                val uri = FileProvider.getUriForFile(context, authority, apkFile)
                _updateState.value = UpdateState.Downloaded(uri, apkFile.absolutePath)
            }
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Download failed: ${e.localizedMessage ?: "Connection error"}")
        }
    }

    fun installUpdate(fileUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
