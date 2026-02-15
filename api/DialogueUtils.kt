package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.rt4.Chat
import kotlin.random.Random

/**
 * Common dialogue and chat utility functions.
 */
object DialogueUtils {

    /**
     * Handles dialogue if present.
     * @param sleepMin Minimum sleep time in milliseconds after clicking continue (default: 500)
     * @param sleepMax Maximum sleep time in milliseconds after clicking continue (default: 800)
     * @return true if dialogue was present and handled
     */
    fun handleDialogue(sleepMin: Int = 500, sleepMax: Int = 800): Boolean {
        if (!Chat.canContinue()) return false
        Chat.clickContinue()
        Condition.sleep(Random.nextInt(sleepMin, sleepMax))
        return true
    }

    /**
     * Handles multiple dialogue prompts.
     * @param maxAttempts Maximum number of dialogue prompts to handle (default: 2)
     * @param sleepMin Minimum sleep time in milliseconds between prompts (default: 500)
     * @param sleepMax Maximum sleep time in milliseconds between prompts (default: 800)
     * @return The number of dialogue prompts that were handled
     */
    fun handleMultipleDialogues(maxAttempts: Int = 2, sleepMin: Int = 500, sleepMax: Int = 800): Int {
        var count = 0
        while (count < maxAttempts && handleDialogue(sleepMin, sleepMax)) {
            count++
        }
        return count
    }

    /**
     * Handles a specific dialogue option by text.
     * @param dialogueText The text to search for in dialogue options
     * @return true if the option was found and selected
     */
    fun handleDialogueOption(dialogueText: String): Boolean {
        if (Chat.chatting()) {
            val option = Chat.stream()
                .textContains(dialogueText)
                .firstOrNull()

            if (option != null) {
                return option.select()
            }
        }
        return false
    }

    /**
     * Waits until a condition is met with random timing.
     * @param condition The condition to wait for
     * @param minWait Minimum wait time in milliseconds (default: 50)
     * @param maxWait Maximum wait time in milliseconds (default: 150)
     * @param maxAttempts Maximum number of attempts (default: 20)
     * @return true if condition was met
     */
    fun waitUntil(
        condition: () -> Boolean,
        minWait: Int = 50,
        maxWait: Int = 150,
        maxAttempts: Int = 20
    ): Boolean {
        return Condition.wait(condition, Random.nextInt(minWait, maxWait), maxAttempts)
    }
}
