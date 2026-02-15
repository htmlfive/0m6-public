package org.powbot.community.libationprayer.domain.decision

import org.powbot.community.libationprayer.domain.model.LibationConfig
import org.powbot.community.libationprayer.domain.model.LibationStateSnapshot

class LibationDecisionService(
    private val config: LibationConfig
) {
    fun nextAction(state: LibationStateSnapshot): LibationAction {
        if (state.awaitingBowlFill && !state.hasUnblessedWine && !state.hasBlessedSacrificeJugs) {
            if (state.bowlHasFillAction) {
                return LibationAction.BANK
            }
            if (state.prayerPoints < 2) {
                return LibationAction.RECHARGE
            }
            return LibationAction.SACRIFICE
        }

        if (state.boneShardCount < config.minimumBoneShards) {
            return LibationAction.BANK
        }
        // Bank only when we have no valid unblessed wine and none of the two valid blessed jug items.
        if (!state.hasUnblessedWine && !state.hasBlessedSacrificeJugs) {
            return LibationAction.BANK
        }
        if (state.hasUnblessedWine) {
            return LibationAction.BLESS
        }
        if (state.hasBlessedSacrificeJugs && shouldRechargePrayer(state)) {
            return LibationAction.RECHARGE
        }
        if (state.hasBlessedSacrificeJugs) {
            return LibationAction.SACRIFICE
        }
        return LibationAction.IDLE
    }

    private fun shouldRechargePrayer(state: LibationStateSnapshot): Boolean {
        if (state.prayerPoints < 2) {
            return true
        }
        return state.needsInitialFullRecharge && state.prayerPoints < state.prayerMax
    }
}

