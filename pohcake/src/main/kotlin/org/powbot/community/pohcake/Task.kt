package org.powbot.community.pohcake

abstract class Task(protected val script: PohCakeMaker, val name: String) {
    abstract fun activate(): Boolean
    abstract fun execute()
}

