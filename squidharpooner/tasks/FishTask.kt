package org.powbot.om6.squidharpooner.tasks

import org.powbot.api.Condition
import org.powbot.om6.squidharpooner.SquidHarpooner
import org.powbot.om6.squidharpooner.Task

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
