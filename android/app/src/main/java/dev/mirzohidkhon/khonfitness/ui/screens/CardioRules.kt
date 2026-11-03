package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session

/** What a new cardio entry needs before Save: a time or a distance (swim laps count as distance). Null when it has one. */
fun cardioMissing(s: Session): String? =
    if ((s.timeSec ?: 0) > 0 || (s.distanceM ?: 0) > 0 || (s.laps ?: 0) > 0) null else "Enter time or distance"

/**
 * An open cardio session whose interval timer has ended (Stop, or the last round): the timer stored its time and no
 * timer runs for it now ([runningTimer] is the session id the timer service runs, or null). It holds after an app
 * restart too, as the stored time says so. Today offers Save and Discard for it, not Resume.
 */
fun timerStopped(s: Session, runningTimer: String?): Boolean =
    s.itemType == ItemType.CARDIO && !s.finished && s.timeSec != null && runningTimer != s.id
