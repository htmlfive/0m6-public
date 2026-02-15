package org.powbot.om6.squidharpooner

abstract class Task(protected val script: SquidHarpooner, val name: String) {
    abstract fun activate(): Boolean
    abstract fun execute()
}
