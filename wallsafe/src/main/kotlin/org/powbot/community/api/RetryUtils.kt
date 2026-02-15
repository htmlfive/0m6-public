package org.powbot.community.api

import org.powbot.api.Condition

/**
 * Common retry and action utility functions.
 */
object RetryUtils {

    /**
     * Retries an action multiple times with a delay between attempts.
     * @param maxRetries Maximum number of retries
     * @param delayMs Delay between retries in milliseconds
     * @param action The action to retry (should return true on success)
     * @return true if action succeeded within maxRetries, false otherwise
     */
    fun retryAction(maxRetries: Int, delayMs: Int, action: () -> Boolean): Boolean {
        for (i in 1..maxRetries) {
            if (action()) {
                return true
            }
            if (i < maxRetries) {
                Condition.sleep(delayMs)
            }
        }
        return false
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

