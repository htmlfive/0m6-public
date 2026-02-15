package org.powbot.om6.api

/**
 * Base task interface for task-based script systems.
 * Provides a common structure for all tasks across different scripts.
 */
interface ITask {
    /**
     * Checks if this task should be executed.
     * @return true if the task conditions are met
     */
    fun validate(): Boolean

    /**
     * Executes the task logic.
     */
    fun execute()
}

/**
 * Base abstract task with name support for logging.
 */
abstract class NamedTask(val name: String) : ITask {
    abstract override fun validate(): Boolean
    abstract override fun execute()
}

/**
 * Alternative task interface for scripts using "activate" naming.
 */
interface IActivatableTask {
    /**
     * Checks if this task should be activated.
     * @return true if the task should run
     */
    fun activate(): Boolean

    /**
     * Executes the task logic.
     */
    fun execute()
}
