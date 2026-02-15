package org.powbot.om6.herblore.data

data class HerbloreRuntimeConfig(
    val mode: HerbloreActivityMode,
    val autoCleanAndUnfAll: Boolean,
    val finishedQueueEnabled: Boolean,
    val wholeShowEnabled: Boolean,
    val cleaningHerb: HerbDefinition?,
    val cleaningMakeAll: Boolean,
    val unfinishedHerb: HerbDefinition?,
    val unfinishedMakeAll: Boolean,
    val finishedPotion: FinishedPotionRecipe?,
    val finishedQueueSelections: Set<String>
)
