package org.powbot.community.herbrun.tasks

import org.powbot.community.herbrun.HerbRun

/**
 * Drops disposable items (bucket/weeds) before any other task work.
 */
class DropDisposableItemsTask(script: HerbRun) : HerbRunTask(script, "Drop Disposable Items") {
    override fun shouldExecute(): Boolean {
        return script.shouldDropDisposableItems()
    }

    override fun execute() {
        script.dropDisposableItems()
    }
}

