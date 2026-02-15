package org.powbot.community.herblore.tasks

import org.powbot.community.herblore.HerbloreTrainer

abstract class HerbloreTask(
    protected val script: HerbloreTrainer,
    val name: String
) {
    abstract fun shouldExecute(): Boolean

    abstract fun execute()
}

