package org.powbot.om6.ectofunctus.tasks

import org.powbot.om6.mixology.structure.TreeTask

class IdleTask : TreeTask(true) {
    override fun execute(): Int = getShortSleepTime()

    override fun toString(): String = "Idling"
}
