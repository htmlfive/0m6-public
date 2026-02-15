package org.powbot.community.libationprayer.exec

import org.powbot.community.libationprayer.client.LibationGameClient
import org.powbot.community.libationprayer.domain.model.LibationConfig
import org.powbot.community.libationprayer.domain.model.LibationRuntime
import org.powbot.community.libationprayer.util.LibationConstants

class BankingExecutor(
    private val client: LibationGameClient,
    private val config: LibationConfig
) {
    fun execute(runtime: LibationRuntime) {
        runtime.currentTask = "Banking Aubervale"
        client.walkTo(LibationConstants.BANK_TILE, 8)
        if (!client.openBank()) return

        client.depositAllJugs()

        if (!client.withdrawAllWine()) {
            client.stopScript("No more ${config.wineType} available in bank.")
            return
        }

        client.withdrawAllBoneShardsIfMissing()

        if (client.boneShardCount() < config.minimumBoneShards) {
            client.stopScript("Bone shards below minimum (${config.minimumBoneShards}).")
            return
        }

        if (!client.hasUnblessedWine()) {
            client.stopScript("No ${config.wineType} in inventory after banking.")
            return
        }

        runtime.needsInitialFullRecharge = false
        runtime.awaitingBowlFill = false
        client.closeBank()
    }
}

