package org.powbot.community.squidharpooner.tasks

import org.powbot.api.Condition
import org.powbot.community.squidharpooner.SquidHarpooner
import org.powbot.community.squidharpooner.Task

class FishTask(script: SquidHarpooner, name: String) : Task(script, name) {

    override fun activate(): Boolean {
        return script.shouldHarpoon()
    }

    override fun execute() {
        if (!script.harpoonNow()) {
            Condition.sleep(120)
            return
        }

        Condition.sleep(200)
    }
}

