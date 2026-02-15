package org.powbot.om6.api

import org.powbot.api.AppManager.logger
import org.powbot.api.Condition
import org.powbot.api.rt4.Game
import kotlin.random.Random

/**
 * Common retry and action utility functions.
 */
object WaitUtils {
    fun randomDelay(min: Int, max: Int) {
        Condition.sleep(Random.nextInt(min, max + 1))
    }
    /**
     * Waits for a condition to be true with retries.
     * Waits for loading screen
     * @return true if condition was met within attempts
     */
    fun waitForLoad(): Boolean {
        logger.info("Checking client state for loading...")
        return if (Game.clientState() == 25) {
            logger.info("Client state is 25 (loading), waiting for state 30 (loaded)...")
            val result = Condition.wait({ Game.clientState() == 30 }, 50, 100)
            if (result) {
                logger.info("Successfully loaded (client state 30)")
            } else {
                logger.info("Timeout waiting for load")
            }
            result
        } else {
            logger.info("Client state is not 25, skipping wait (current state: ${Game.clientState()})")
            false
        }
    }

    /**
     * Waits for a condition to be true with retries.
     * @param condition The condition to wait for
     * @param timeout Timeout per attempt in milliseconds
     * @param maxAttempts Maximum number of attempts
     * @return true if condition was met within attempts
     */
    fun waitForCondition(
        condition: () -> Boolean,
        timeout: Int = 500,
        maxAttempts: Int = 10
    ): Boolean {
        return Condition.wait(condition, timeout, maxAttempts)
    }
}
