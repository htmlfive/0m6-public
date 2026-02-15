package org.powbot.om6.mortmyre

abstract class Task(protected val script: MortMyreFungusHarvester, val name: String) {
    abstract fun activate(): Boolean
    abstract fun execute()
}
