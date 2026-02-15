package org.powbot.om6.herblore.tasks

import org.powbot.om6.herblore.HerbloreTrainer

abstract class HerbloreTask(
    protected val script: HerbloreTrainer,
    val name: String
) {
    abstract fun shouldExecute(): Boolean

    abstract fun execute()
}
