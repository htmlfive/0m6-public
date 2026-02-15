package org.powbot.community.mortmyre

abstract class Task(protected val script: MortMyreFungusHarvester, val name: String) {
    abstract fun activate(): Boolean
    abstract fun execute()
}

