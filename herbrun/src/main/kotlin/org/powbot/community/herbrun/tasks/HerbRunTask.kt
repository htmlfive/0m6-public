package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

/**
 * Shared base task used by the herb run script.
 */
abstract class HerbRunTask(
    protected val script: HerbRun,
    val taskName: String
) {
    /**
     * Should return true when the task wants control on this poll.
     */
    abstract fun shouldExecute(): Boolean

    /**
     * Performs the task logic. Exceptions should be handled by the caller.
     */
    abstract fun execute()
}

