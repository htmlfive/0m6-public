package org.powbot.community.barbarianfishing

abstract class Task(protected val script: BarbarianFishing, val name: String) {
    abstract fun activate(): Boolean
    abstract fun execute()
}

