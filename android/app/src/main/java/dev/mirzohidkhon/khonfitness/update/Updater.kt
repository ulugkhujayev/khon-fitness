package dev.mirzohidkhon.khonfitness.update

import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Reads the latest GitHub release and installs its APK over the current build. Same signing key every release. */
object Updater {
    const val REPO = "ulugkhujayev/khon-fitness"
    /** Debug builds read a local feed so the install path can be tested on an emulator. */
    private val latestUrl: String get() = if (dev.mirzohidkhon.khonfitness.BuildConfig.DEBUG) "http://10.0.2.2:8766/latest.json" else "https://api.github.com/repos/$REPO/releases/latest"

    @Serializable data class Asset(val name: String, val browser_download_url: String, val size: Long = 0)
    @Serializable data class Release(val tag_name: String, val name: String? = null, val body: String? = null, val assets: List<Asset> = emptyList())

    sealed class Result {
        data class UpToDate(val current: String) : Result()
        data class Available(val release: Release, val apk: Asset) : Result()
        data class Failed(val message: String) : Result()
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(currentVersion: String): Result = withContext(Dispatchers.IO) {
        try {
            val conn = URL(latestUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 10000; conn.readTimeout = 15000
            if (conn.responseCode == 404) return@withContext Result.Failed("No release published yet")
            if (conn.responseCode != 200) return@withContext Result.Failed("GitHub answered ${conn.responseCode}")
            val release = json.decodeFromString<Release>(conn.inputStream.bufferedReader().readText())
            val latest = release.tag_name.removePrefix("v")
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return@withContext Result.Failed("Release ${release.tag_name} has no APK")
            if (compare(latest, currentVersion) > 0) Result.Available(release, apk) else Result.UpToDate(currentVersion)
        } catch (e: Exception) {
            Result.Failed(e.message ?: "Network error")
        }
    }

    /** Downloads the APK into the cache dir and hands it to the package installer. */
    suspend fun download(context: Context, asset: Asset, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, asset.name)
        val conn = URL(asset.browser_download_url).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 10000; conn.readTimeout = 30000
        val total = if (conn.contentLengthLong > 0) conn.contentLengthLong else asset.size
        conn.inputStream.use { input -> file.outputStream().use { out ->
            val buf = ByteArray(64 * 1024); var read: Int; var done = 0L
            while (input.read(buf).also { read = it } > 0) { out.write(buf, 0, read); done += read; if (total > 0) onProgress(done.toFloat() / total) }
        } }
        file
    }

    /** Installs through a package installer session. The system relaunches the app via [UpdateActivity] when done. */
    fun install(context: Context, file: File) {
        runCatching {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                if (Build.VERSION.SDK_INT >= 34) setRequestUpdateOwnership(true)
            }
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                session.openWrite("update.apk", 0, file.length()).use { out -> file.inputStream().use { it.copyTo(out) }; session.fsync(out) }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                val target = Intent(context, UpdateActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val opts = if (Build.VERSION.SDK_INT >= 34) android.app.ActivityOptions.makeBasic().setPendingIntentCreatorBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED).toBundle() else null
                val pi = PendingIntent.getActivity(context, 7, target, flags, opts)
                session.commit(pi.intentSender)
                android.util.Log.i("KhonUpdate", "session $id committed")
            }
        }.onFailure {
            android.util.Log.w("KhonUpdate", "session install failed", it)
            // Fallback: hand the APK to the system installer the old way.
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private fun compare(a: String, b: String): Int {
        val pa = a.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) { val d = (pa.getOrNull(i) ?: 0) - (pb.getOrNull(i) ?: 0); if (d != 0) return d }
        return 0
    }
}
