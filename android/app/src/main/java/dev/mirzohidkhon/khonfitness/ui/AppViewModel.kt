package dev.mirzohidkhon.khonfitness.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo: KhonRepository = (app as KhonApp).repo
    private fun <T> stream(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> = flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val exercises = stream(repo.exercises, emptyList())
    val modalities = stream(repo.modalities, emptyList())
    val programs = stream(repo.programs, emptyList())
    val sessions = stream(repo.sessions, emptyList())
    val bodyweights = stream(repo.bodyweights, emptyList())
    val unfinished = stream(repo.unfinishedSession, null)
    val allSetLogs = stream(repo.allSetLogs, emptyList())
    val blocks = stream(repo.blocks, emptyList())
    val blockExercises = stream(repo.blockExercises, emptyList())
    val allIntervalLogs = stream(repo.allIntervalLogs, emptyList())
    val planData = stream(repo.planData, KhonRepository.PlanData(emptyList(), emptyList(), emptyList(), emptyList()))

    fun run(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    /** Latest release check, done once per app start. Today and Settings both read it. */
    val update = kotlinx.coroutines.flow.MutableStateFlow<dev.mirzohidkhon.khonfitness.update.Updater.Result?>(null)
    val updateProgress = kotlinx.coroutines.flow.MutableStateFlow<Float?>(null)
    /** Downloaded APK waiting for the unknown-sources permission. */
    val pendingInstall = kotlinx.coroutines.flow.MutableStateFlow<java.io.File?>(null)
    private var checked = false
    fun checkUpdate(force: Boolean = false) {
        if (checked && !force) return
        checked = true
        viewModelScope.launch { update.value = dev.mirzohidkhon.khonfitness.update.Updater.check(dev.mirzohidkhon.khonfitness.BuildConfig.VERSION_NAME) }
    }
    /** Download and install in one go. The system relaunches the app when the install is done. */
    fun runUpdate(context: android.content.Context, onNeedPermission: () -> Unit, onError: (String) -> Unit) {
        val u = update.value as? dev.mirzohidkhon.khonfitness.update.Updater.Result.Available ?: return
        updateProgress.value = 0f
        viewModelScope.launch {
            runCatching { dev.mirzohidkhon.khonfitness.update.Updater.download(context, u.apk) { updateProgress.value = it } }
                .onSuccess { file -> updateProgress.value = null; if (context.packageManager.canRequestPackageInstalls()) dev.mirzohidkhon.khonfitness.update.Updater.install(context, file) else { pendingInstall.value = file; onNeedPermission() } }
                .onFailure { updateProgress.value = null; onError(it.message ?: "Download failed") }
        }
    }
    /** Called after the user returns from the permission screen. */
    fun installPending(context: android.content.Context) {
        val f = pendingInstall.value ?: return
        if (context.packageManager.canRequestPackageInstalls()) { pendingInstall.value = null; dev.mirzohidkhon.khonfitness.update.Updater.install(context, f) }
    }

    fun startItem(item: PlanItem, onStarted: (Session) -> Unit) = run {
        val s = when (item.itemType) {
            ItemType.PROGRAM -> item.program?.let { repo.startProgramSession(it.id) }
            ItemType.CARDIO -> item.cardio?.let { repo.startCardioSession(it) }
            else -> null
        }
        if (s != null) onStarted(s)
    }

    fun today(): LocalDate = LocalDate.now()
}
