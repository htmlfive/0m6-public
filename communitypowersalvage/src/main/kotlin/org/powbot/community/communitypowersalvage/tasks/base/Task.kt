package org.powbot.community.communitypowersalvage.tasks.base

import org.powbot.community.communitypowersalvage.CommunityPowerSalvage

abstract class Task(protected val script: CommunityPowerSalvage) {
    protected fun stopScript(reason: String) {
        script.stopScript(reason)
    }

    abstract fun activate(): Boolean
    abstract fun execute()
}
