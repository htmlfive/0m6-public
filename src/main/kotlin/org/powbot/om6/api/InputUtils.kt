package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Input
import java.awt.Point
import kotlin.random.Random

/**
 * Common input and text entry utility functions.
 */
object InputUtils {

    /**
     * Types text and presses Enter.
     *
     * @param text The text to type.
     * @param preDelay Delay before typing in milliseconds (default: random 1800-2400)
     * @param postDelay Delay after typing in milliseconds (default: random 400-600)
     */
    fun typeAndEnter(
        text: String,
        preDelay: Int = Random.nextInt(1800, 2400),
        postDelay: Int = Random.nextInt(400, 600)
    ) {
        try {
            Input.sendln(text)
            Condition.sleep(preDelay)
            Input.sendln("")
            Condition.sleep(postDelay)
        } catch (e: Exception) {
            // Silent failure, let caller handle
        }
    }

    /**
     * Types text without pressing Enter.
     *
     * @param text The text to type.
     * @param delay Delay after typing in milliseconds (default: random 400-600)
     */
    fun type(text: String, delay: Int = Random.nextInt(400, 600)) {
        try {
            Input.send(text)
            Condition.sleep(delay)
        } catch (e: Exception) {
            // Silent failure, let caller handle
        }
    }

    /**
     * Generates a random delay within a specified range.
     *
     * @param min Minimum delay in milliseconds
     * @param max Maximum delay in milliseconds
     */
    fun randomDelay(min: Int, max: Int) {
        val delay = Random.nextInt(min, max)
        Condition.sleep(delay)
    }

    /**
     * Sleeps for a randomized duration within ±200ms of the base value.
     * @param baseMs Base duration in milliseconds
     */
    fun randomSleep(baseMs: Int) {
        val randomOffset = (-200..200).random()
        val sleepTime = (baseMs + randomOffset).coerceAtLeast(0)
        Condition.sleep(sleepTime)
    }

    /**
     * Clicks at coordinates with random offset.
     * @param x Base X coordinate
     * @param y Base Y coordinate
     * @param offsetRange Random offset range (default: 3)
     * @return true if click was attempted
     */
    fun clickTeleport(x: Int = 453, y: Int = 223, offsetRange: Int = 3): Boolean {
        val randomX = x + Random.nextInt(-offsetRange, offsetRange)
        val randomY = y + Random.nextInt(-offsetRange, offsetRange)

        return Input.tap(randomX,randomY)
    }
}
