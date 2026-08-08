package com.adriaan.claudeusage.data.update

import com.adriaan.claudeusage.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Details of a release newer than the installed build. */
data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String?,
    val releasePageUrl: String
)

/**
 * Checks the project's GitHub Releases for a version newer than the running build and, if found,
 * returns the download URL so the app can prompt the user to update.
 *
 * Uses the public `releases/latest` endpoint (no auth) — subject to GitHub's unauthenticated rate
 * limit, which is ample for an occasional per-launch check.
 */
class UpdateChecker(
    private val currentVersionName: String = BuildConfig.VERSION_NAME,
    private val owner: String = BuildConfig.GITHUB_OWNER,
    private val repo: String = BuildConfig.GITHUB_REPO
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun check(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/releases/latest")
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("User-Agent", "ClaudeUsage-Android")
                .get()
                .build()

            val (successful, body) = client.newCall(request).execute().use { response ->
                response.isSuccessful to response.body?.string()
            }
            if (!successful || body.isNullOrBlank()) {
                return@withContext Result.success(null)
            }

            val json = JSONObject(body)
            val tag = json.optString("tag_name").ifEmpty { json.optString("name") }
            val latestVersion = normalizeVersion(tag)
            if (latestVersion.isEmpty()) return@withContext Result.success(null)

            if (!isNewer(latestVersion, normalizeVersion(currentVersionName))) {
                return@withContext Result.success(null)
            }

            val apkUrl = findApkAsset(json)
            val info = UpdateInfo(
                versionName = latestVersion,
                releaseNotes = json.optString("body").ifBlank { "A new version is available." },
                apkDownloadUrl = apkUrl,
                releasePageUrl = json.optString("html_url")
                    .ifEmpty { "https://github.com/$owner/$repo/releases/latest" }
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findApkAsset(release: JSONObject): String? {
        val assets = release.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                return asset.optString("browser_download_url").ifEmpty { null }
            }
        }
        return null
    }

    /** Strip a leading "v" and any pre-release/build suffix, keeping the dotted numeric core. */
    private fun normalizeVersion(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim().removePrefix("v").removePrefix("V").takeWhile { it.isDigit() || it == '.' }
    }

    /** True when [candidate] is a strictly higher dotted-numeric version than [current]. */
    private fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split(".").mapNotNull { it.toIntOrNull() }
        val b = current.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(a.size, b.size)
        for (i in 0 until len) {
            val ai = a.getOrElse(i) { 0 }
            val bi = b.getOrElse(i) { 0 }
            if (ai != bi) return ai > bi
        }
        return false
    }
}
