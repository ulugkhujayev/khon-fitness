package dev.mirzohidkhon.khonfitness.data

import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface NightsState {
    data object NotRead : NightsState
    /** Health Connect is not installed, needs an update, or the sleep permission is not granted. */
    data object NoAccess : NightsState
    data class Loaded(val nights: Map<LocalDate, Night>) : NightsState
    /** A read failed and no read had loaded nights before. */
    data object Failed : NightsState
}

const val NIGHT_READ_DAYS = 1_100
const val NIGHT_FRESH_MS = 5 * 60_000L

/**
 * The nights of the last [NIGHT_READ_DAYS] days, in memory only. One read at a time: a call while a read runs waits for
 * that read. A failed read keeps the nights loaded before; a read without access clears them.
 */
class NightStore(
    private val source: SleepSource,
    private val scope: CoroutineScope,
    private val clocks: Clocks = SystemClocks,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val current = MutableStateFlow<NightsState>(NightsState.NotRead)
    val state: StateFlow<NightsState> = current

    private val lock = Any()
    private var running: Deferred<Unit>? = null
    private var lastGoodElapsed: Long? = null

    /** Reads again unless the last good read is younger than [NIGHT_FRESH_MS] and [force] is false. */
    suspend fun refresh(force: Boolean = false) {
        val read = synchronized(lock) {
            running?.takeIf { it.isActive } ?: run {
                val last = lastGoodElapsed
                if (!force && last != null && clocks.elapsedMs() - last in 0 until NIGHT_FRESH_MS) return
                scope.async { read() }.also { running = it }
            }
        }
        read.await()
    }

    private suspend fun read() {
        val now = clocks.wallMs()
        val result = try {
            nights(source.spans(HrRange(minusMs(now, NIGHT_READ_DAYS * DAY_MS), now)), zone())
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoSleepAccess) {
            synchronized(lock) { lastGoodElapsed = null }
            current.value = NightsState.NoAccess
            return
        } catch (e: Exception) {
            if (current.value !is NightsState.Loaded) current.value = NightsState.Failed
            return
        }
        synchronized(lock) { lastGoodElapsed = clocks.elapsedMs() }
        current.value = NightsState.Loaded(result)
    }
}
