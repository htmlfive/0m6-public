package org.powbot.community.ectofunctus.tasks

import org.powbot.community.mixology.structure.TreeTask

class IdleTask : TreeTask(true) {
    override fun execute(): Int = getShortSleepTime()

    override fun toString(): String = "Idling"
}

