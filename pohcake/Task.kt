package org.powbot.om6.pohcake

abstract class Task(protected val script: PohCakeMaker, val name: String) {
    abstract fun activate(): Boolean
    abstract fun execute()
}
