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
