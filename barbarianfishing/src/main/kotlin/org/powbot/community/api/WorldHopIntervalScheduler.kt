package org.powbot.community.api

import kotlin.random.Random

/**
 * Schedules world hops using a random minute interval between a configured min/max range.
 */
class WorldHopIntervalScheduler(
    minMinutes: Int,
    maxMinutes: Int,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private var minMinutes: Int = 1
    private var maxMinutes: Int = 1
    private var nextHopAt: Long = 0L

    init {
        configure(minMinutes, maxMinutes)
    }

    fun configure(minMinutes: Int, maxMinutes: Int) {
        val normalizedMin = minMinutes.coerceAtLeast(1)
        val normalizedMax = maxMinutes.coerceAtLeast(1)
        if (normalizedMin <= normalizedMax) {
            this.minMinutes = normalizedMin
            this.maxMinutes = normalizedMax
            return
        }
        this.minMinutes = normalizedMax
        this.maxMinutes = normalizedMin
    }

    fun scheduleNext() {
        val delayMinutes = Random.nextInt(minMinutes, maxMinutes + 1)
        nextHopAt = clock() + delayMinutes * 60_000L
    }

    fun forceNow() {
        nextHopAt = 0L
    }

    fun shouldHopNow(): Boolean = clock() >= nextHopAt

    fun countdownText(): String {
        val remaining = (nextHopAt - clock()).coerceAtLeast(0L)
        val totalSeconds = remaining / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format("%d:%02d:%02d", hours, minutes, seconds)
    }
}

