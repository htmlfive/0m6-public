package org.powbot.om6.api

import kotlin.math.max

class TaskSafetyMonitor(
    private val capacity: Int,
    private val leakAmount: Int,
    private val leakIntervalMs: Long
) {
    private data class Bucket(var level: Int = 0)

    private val buckets = mutableMapOf<String, Bucket>()
    private var lastLeakTime = System.currentTimeMillis()

    fun tick() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastLeakTime
        if (elapsed < leakIntervalMs) {
            return
        }

        val leakSteps = (elapsed / leakIntervalMs).toInt()
        if (leakSteps <= 0) {
            return
        }

        val totalLeak = leakSteps * leakAmount
        buckets.values.forEach { bucket ->
            bucket.level = max(0, bucket.level - totalLeak)
        }
        lastLeakTime = now
    }

    fun snapshot(): Map<String, Int> = buckets.mapValues { it.value.level }

    fun capacityLimit(): Int = capacity

    fun increment(taskName: String, countHit: Boolean = true, weight: Int = 1): Int {
        val bucket = buckets.getOrPut(taskName) { Bucket() }
        if (countHit) {
            bucket.level += weight
        }
        return bucket.level
    }

    fun capacityExceeded(level: Int): Boolean = level >= capacity
}
