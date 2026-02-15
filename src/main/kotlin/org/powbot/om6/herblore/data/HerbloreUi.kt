package org.powbot.om6.herblore.data

object HerbloreUi {
    const val CONFIG_PAGE_OPTION = "Configuration Page"
    const val PAGE_GENERAL = ""
    const val PAGE_GENERAL_LEGACY = "General"
    const val PAGE_HERB_CLEANING = "Herb cleaning"
    const val PAGE_UNFINISHED = "Unfinished potions"
    const val PAGE_FINISHED = "Finished potions"
    const val PAGE_FINISHED_QUEUE = "Finished queue"
    const val PAGE_WHOLE_SHOW = "The whole show"

    const val OPTION_MODE = "Mode"
    const val OPTION_AUTO_CLEAN_AND_UNF = "Auto clean + unf all"
    const val OPTION_CLEANING_HERB = "Cleaning Herb"
    const val OPTION_CLEANING_MAKE_ALL = "Cleaning Make all capable"
    const val OPTION_UNFINISHED_HERB = "Unfinished Herb"
    const val OPTION_UNFINISHED_MAKE_ALL = "Unfinished Make all capable"
    const val OPTION_FINISHED_POTION = "Finished Potion"

    fun finishedQueueOptionName(potionName: String): String = "Queue $potionName"

    val PAGE_OPTION_GROUPS: Map<String, List<String>> = mapOf(
        PAGE_GENERAL to listOf(OPTION_AUTO_CLEAN_AND_UNF),
        PAGE_GENERAL_LEGACY to listOf(OPTION_AUTO_CLEAN_AND_UNF),
        PAGE_HERB_CLEANING to listOf(OPTION_CLEANING_HERB, OPTION_CLEANING_MAKE_ALL),
        PAGE_UNFINISHED to listOf(OPTION_UNFINISHED_HERB, OPTION_UNFINISHED_MAKE_ALL),
        PAGE_FINISHED to listOf(OPTION_FINISHED_POTION),
        PAGE_FINISHED_QUEUE to FinishedPotionRecipes.supported.map { finishedQueueOptionName(it.displayName) },
        PAGE_WHOLE_SHOW to emptyList()
    )

    val ALL_PAGE_OPTIONS: Set<String> = PAGE_OPTION_GROUPS.values.flatten().toSet()
}
