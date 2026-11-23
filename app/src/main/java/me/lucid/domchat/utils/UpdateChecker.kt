package me.lucid.domchat.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.lucid.common.platform.PlatformHttpClient
import me.lucid.common.platform.PlatformHttpRequest
import me.lucid.domchat.BuildConfig
import me.lucid.domchat.R

private const val GITHUB_API_URL = "https://api.github.com/repos/DomcordFr/Domchat/releases/latest"

class UpdateChecker(private val client: PlatformHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _isForcedCheck = MutableStateFlow(false)
    val isForcedCheck = _isForcedCheck.asStateFlow()

    // Counter incremented every time a fresh check should be triggered
    // Start at 1 so the first subscriber immediately gets a check
    private val _checkTrigger = MutableStateFlow(1)
    val checkTrigger = _checkTrigger.asStateFlow()

    fun forceUpdateCheck() {
        _isForcedCheck.value = true
        _checkTrigger.value++ // re-trigger the update flow
    }

    fun clearForcedCheck() {
        _isForcedCheck.value = false
    }

    fun checkUpdate(): Flow<UiState<UpdateInfo>> = flow {
        emit(UiState.Loading)

        emit(
            UiState.Success(
                data = try {
                    val response = client.execute(
                        PlatformHttpRequest(
                            method = "GET",
                            url = GITHUB_API_URL,
                            headers = mapOf(
                                "Accept" to "application/vnd.github+json",
                                "User-Agent" to "Domchat ${BuildConfig.VERSION_NAME} #${BuildConfig.VERSION_CODE}"
                            )
                        )
                    )
                    if (response.statusCode in 200..299) {
                        val release = json.decodeFromString<GitHubRelease>(response.body.decodeToString())
                        
                        // Convert GitHub release to UpdateInfo
                        val arch = getDeviceArchitecture()
                        val downloads = release.assets
                            .filter { it.name.endsWith(".apk") }
                            .map { asset ->
                                val shaRegex = Regex("""([a-fA-F0-9]{64})\s+[*]?\s*(${Regex.escape(asset.name)})""", RegexOption.IGNORE_CASE)
                                val shaMatch = shaRegex.find(release.body) ?: Regex("""(${Regex.escape(asset.name)})[:\s]+([a-fA-F0-9]{64})""", RegexOption.IGNORE_CASE).find(release.body)
                                val sha256 = shaMatch?.let { m ->
                                    if (m.groupValues[1].length == 64) m.groupValues[1] else m.groupValues[2]
                                }
                                UpdateDownload(
                                    name = asset.name,
                                    url = asset.browser_download_url,
                                    size = formatFileSize(asset.size),
                                    sha256 = sha256,
                                )
                            }
                        
                        // Sort downloads to prioritize architecture match
                        val sortedDownloads = downloads.sortedByDescending { download ->
                            when {
                                download.name.contains(arch, ignoreCase = true) -> 2
                                download.name.contains("universal", ignoreCase = true) -> 1
                                else -> 0
                            }
                        }
                        
                        UpdateInfo(
                            version = release.tag_name.removePrefix("v"),
                            publishedAt = release.published_at,
                            changelog = release.body,
                            downloads = sortedDownloads
                        )
                    } else {
                        throw Exception("Failed to fetch update info: ${response.statusCode}")
                    }
                } catch (e: Exception) {
                    throw e
                }
            )
        )
    }.catch {
        emit(UiState.Error(it))
    }.flowOn(Dispatchers.IO)
    
    private fun getDeviceArchitecture(): String {
        val abis = Build.SUPPORTED_ABIS
        return when {
            abis.any { it.contains("arm64") } -> "arm64-v8a"
            abis.any { it.contains("armeabi") } -> "armeabi-v7a"
            abis.any { it.contains("x86_64") } -> "x86_64"
            abis.any { it.contains("x86") } -> "x86"
            else -> "universal"
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024 -> String.format("%.1f KB", bytes / 1_024.0)
            else -> "$bytes B"
        }
    }

    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadProgressMap = java.util.concurrent.ConcurrentHashMap<Long, MutableStateFlow<DownloadProgress>>()
    private val downloadCounter = java.util.concurrent.atomic.AtomicLong(1L)

    fun downloadUpdate(context: Context, download: UpdateDownload): Long {
        val id = downloadCounter.getAndIncrement()
        val flow = MutableStateFlow(DownloadProgress(status = DownloadManager.STATUS_PENDING))
        downloadProgressMap[id] = flow

        downloadScope.launch {
            try {
                flow.value = flow.value.copy(status = DownloadManager.STATUS_RUNNING)
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val partFile = File(updatesDir, "update.apk.part")
                val finalFile = File(updatesDir, "update.apk")
                if (partFile.exists()) partFile.delete()
                if (finalFile.exists()) finalFile.delete()

                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(download.url)
                    .header("User-Agent", "Domchat-Android/${BuildConfig.VERSION_NAME}")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw java.io.IOException("Unexpected HTTP response ${response.code}")
                    }
                    val body = response.body ?: throw java.io.IOException("Empty response body")
                    val contentLength = body.contentLength()
                    var downloaded = 0L

                    body.byteStream().use { input ->
                        partFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var lastEmitTime = 0L
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                val now = System.currentTimeMillis()
                                if (now - lastEmitTime > 150L || downloaded == contentLength) {
                                    lastEmitTime = now
                                    flow.value = DownloadProgress(
                                        bytesDownloaded = downloaded,
                                        totalBytes = if (contentLength > 0) contentLength else downloaded,
                                        status = DownloadManager.STATUS_RUNNING,
                                        localUri = null
                                    )
                                }
                            }
                        }
                    }
                }

                if (partFile.renameTo(finalFile)) {
                    flow.value = DownloadProgress(
                        bytesDownloaded = finalFile.length(),
                        totalBytes = finalFile.length(),
                        status = DownloadManager.STATUS_SUCCESSFUL,
                        localUri = android.net.Uri.fromFile(finalFile).toString()
                    )
                } else {
                    throw java.io.IOException("Failed to rename temporary download file")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("UpdateChecker", "Download failed", e)
                File(context.cacheDir, "updates/update.apk.part").delete()
                flow.value = DownloadProgress(
                    status = DownloadManager.STATUS_FAILED,
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.update_download_failed), Toast.LENGTH_SHORT).show()
                    context.openUrl(download.url)
                }
            }
        }
        return id
    }

    fun observeDownload(context: Context, downloadId: Long): Flow<DownloadProgress> {
        return downloadProgressMap[downloadId] ?: flowOf(DownloadProgress(status = DownloadManager.STATUS_FAILED))
    }

    fun ignoreUpdate(context: Context, version: String) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("ignored_version", version)
            .putLong("ignored_time", System.currentTimeMillis())
            .apply()
    }

    fun canInstallDownloadedUpdate(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    suspend fun installDownloadedUpdateAsync(
        context: Context,
        uriString: String,
        expectedSha256: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!canInstallDownloadedUpdate(context)) {
            withContext(Dispatchers.Main) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${context.packageName}".toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching {
                    context.startActivity(intent)
                }.onFailure {
                    Log.e("UpdateChecker", "Failed to launch unknown sources settings", it)
                }
            }
            return@withContext false
        }

        try {
            val sourceUri = uriString.toUri()
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val localApk = File(updatesDir, "update.apk")

            if (sourceUri.scheme == "file") {
                val file = sourceUri.toFile()
                if (file.absolutePath != localApk.absolutePath && file.exists()) {
                    if (localApk.exists()) localApk.delete()
                    file.copyTo(localApk, overwrite = true)
                }
            } else if (sourceUri.scheme == "content") {
                if (localApk.exists()) localApk.delete()
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    localApk.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (!localApk.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Downloaded file not found", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }

            if (!expectedSha256.isNullOrBlank()) {
                val digest = MessageDigest.getInstance("SHA-256")
                localApk.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        digest.update(buffer, 0, read)
                    }
                }
                val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actualSha256.equals(expectedSha256.trim(), ignoreCase = true)) {
                    localApk.delete()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Update verification failed: SHA-256 mismatch", Toast.LENGTH_LONG).show()
                    }
                    return@withContext false
                }
            }

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                localApk
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            withContext(Dispatchers.Main) {
                context.startActivity(intent)
            }
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("UpdateChecker", "Failed to stage update APK", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to launch package installer", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    fun installDownloadedUpdate(context: Context, uriString: String, expectedSha256: String? = null): Boolean {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            installDownloadedUpdateAsync(context, uriString, expectedSha256)
        }
    }
    
    fun isUpdateIgnored(context: Context, version: String, forceCheck: Boolean): Boolean {
        if (forceCheck) return false
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val ignoredVersion = prefs.getString("ignored_version", null)
        val ignoredTime = prefs.getLong("ignored_time", 0L)
        val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
        
        return ignoredVersion == version && (System.currentTimeMillis() - ignoredTime) < oneWeekMillis
    }
}

data class DownloadProgress(
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val status: Int = -1,
    val localUri: String? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes.toFloat() else 0f
}

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val published_at: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long
)

@Serializable
data class UpdateDownload(
    val name: String,
    val url: String,
    val size: String,
    val sha256: String? = null,
)

@Serializable
data class UpdateInfo(
    val version: String,
    val publishedAt: String,
    val changelog: String,
    val downloads: List<UpdateDownload>
)

/**
 * 版本号值类，封装版本号字符串并提供比较功能
 */
@JvmInline
value class Version(val value: String) : Comparable<Version> {

    /**
     * 将版本号分解为数字数组
     */
    private fun parseVersion(): List<Int> {
        return value.split(".")
            .map { it.toIntOrNull() ?: 0 }
    }

    /**
     * 实现 Comparable 接口的比较方法
     */
    override fun compareTo(other: Version): Int {
        val thisParts = this.parseVersion()
        val otherParts = other.parseVersion()

        val maxLength = maxOf(thisParts.size, otherParts.size)

        for (i in 0 until maxLength) {
            val thisPart = if (i < thisParts.size) thisParts[i] else 0
            val otherPart = if (i < otherParts.size) otherParts[i] else 0

            when {
                thisPart > otherPart -> return 1
                thisPart < otherPart -> return -1
            }
        }

        return 0
    }

    companion object {
        /**
         * 比较两个版本号字符串
         */
        fun compare(version1: String, version2: String): Int {
            return Version(version1).compareTo(Version(version2))
        }
    }
}

// 扩展操作符函数，使比较更直观
operator fun String.compareTo(other: Version): Int = Version(this).compareTo(other)
operator fun Version.compareTo(other: String): Int = this.compareTo(Version(other))
