package org.powbot.om6.herbrun.services

class RunCycleService(private val script: org.powbot.om6.herbrun.HerbRun) {

    fun selectNextPatch() {
        if (script.runCyclePatchQueueIsEmpty()) {
            if (!script.runCycleLoopRuns()) {
                val reason = "Finished all enabled patches. Stopping script."
                script.runCycleInfo(reason)
                script.runCycleStop(reason)
                return
            }
            if (script.runCycleStartRunNowRequested()) {
                script.runCycleClearStartRunNowRequested()
                script.runCycleSetNextRunAt(0L)
                script.runCycleSetPendingRunCompletionHop(false)
                restartRunCycle("Start run now requested; restarting herb run cycle #")
            } else {
                val now = System.currentTimeMillis()
                if (script.runCycleNextRunAt() == 0L) {
                    script.runCycleSetNextRunAt(now + script.runCycleCooldownMillis())
                    script.runCycleSetPendingRunCompletionHop(script.runCycleHopAfterEachRun())
                    script.runCycleInfo("Cooldown started for next run: ${script.runCycleCooldownMinutes()} minutes.")
                    script.runCycleSetCurrentTask("Cooldown")
                    return
                }
                if (now < script.runCycleNextRunAt()) {
                    script.runCycleSetCurrentTask("Cooldown")
                    return
                }
                script.runCycleSetNextRunAt(0L)
                restartRunCycle("Cooldown complete; restarting herb run cycle #")
            }
        }

        val nextPatch = script.runCyclePopNextPatch()
        script.runCycleSetCurrentPatch(nextPatch)
        script.runCycleSetPatchComplete(false)
        script.runCycleSetCurrentTask("Travel - ${nextPatch.displayName}")
        script.runCycleInfo("Next patch: ${nextPatch.displayName}")
    }

    fun restartRunCycle(reasonPrefix: String) {
        script.runCycleSetPreflightComplete(false)
        script.runCycleEnqueueEnabledPatches()
        script.runCycleSetPatchesCompletedThisRun(0)
        script.runCycleSetBigCompostBinPending(true)
        script.runCycleIncrementRunCount()
        script.runCycleInfo("$reasonPrefix${script.runCycleRunCount()}")
    }
}
