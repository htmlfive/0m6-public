package org.powbot.community.libationprayer.domain.model

data class LibationStateSnapshot(
    val boneShardCount: Int,
    val hasUnblessedWine: Boolean,
    val hasBlessedSacrificeJugs: Boolean,
    val prayerPoints: Int,
    val prayerMax: Int,
    val needsInitialFullRecharge: Boolean,
    val awaitingBowlFill: Boolean,
    val bowlHasSacrificeAction: Boolean,
    val bowlHasFillAction: Boolean
)

