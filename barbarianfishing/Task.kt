package org.powbot.om6.barbarianfishing

abstract class Task(protected val script: BarbarianFishing, val name: String) {
    abstract fun activate(): Boolean
    abstract fun execute()
}
