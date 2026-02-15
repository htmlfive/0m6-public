package org.powbot.community.herblore.services

import org.powbot.community.herblore.HerbloreTrainer
import org.powbot.community.herblore.data.FinishedPotionRecipes
import org.powbot.community.herblore.data.HerbDefinitions
import org.powbot.community.herblore.data.HerbloreActivityMode
import org.powbot.community.herblore.data.HerbloreRuntimeConfig
import org.powbot.community.herblore.data.HerbloreUi

class HerbloreConfigService(private val script: HerbloreTrainer) {

    fun read(): HerbloreRuntimeConfig {
        val page = optionString(HerbloreUi.CONFIG_PAGE_OPTION, HerbloreUi.PAGE_GENERAL)
        val mode = resolveMode()
        val cleaningHerb =
            HerbDefinitions.fromDisplayName(optionString(HerbloreUi.OPTION_CLEANING_HERB, "Ranarr"))
        val unfinishedHerb =
            HerbDefinitions.fromDisplayName(optionString(HerbloreUi.OPTION_UNFINISHED_HERB, "Ranarr"))
        val finishedPotion =
            FinishedPotionRecipes.fromDisplayName(optionString(HerbloreUi.OPTION_FINISHED_POTION, "Super energy"))
        val queueSelections = FinishedPotionRecipes.supported
            .filter { optionBoolean(HerbloreUi.finishedQueueOptionName(it.displayName), false) }
            .map { it.displayName }
            .toSet()

        return HerbloreRuntimeConfig(
            mode = mode,
            autoCleanAndUnfAll = optionBoolean(HerbloreUi.OPTION_AUTO_CLEAN_AND_UNF, false),
            finishedQueueEnabled = page == HerbloreUi.PAGE_FINISHED_QUEUE || page == HerbloreUi.PAGE_WHOLE_SHOW,
            wholeShowEnabled = page == HerbloreUi.PAGE_WHOLE_SHOW,
            cleaningHerb = cleaningHerb,
            cleaningMakeAll = optionBoolean(HerbloreUi.OPTION_CLEANING_MAKE_ALL, false),
            unfinishedHerb = unfinishedHerb,
            unfinishedMakeAll = optionBoolean(HerbloreUi.OPTION_UNFINISHED_MAKE_ALL, false),
            finishedPotion = finishedPotion,
            finishedQueueSelections = queueSelections
        )
    }

    private fun resolveMode(): HerbloreActivityMode {
        return when (optionString(HerbloreUi.CONFIG_PAGE_OPTION, HerbloreUi.PAGE_GENERAL)) {
            HerbloreUi.PAGE_HERB_CLEANING -> HerbloreActivityMode.HERB_CLEANING
            HerbloreUi.PAGE_UNFINISHED -> HerbloreActivityMode.UNFINISHED_POTIONS
            HerbloreUi.PAGE_FINISHED -> HerbloreActivityMode.FINISHED_POTIONS
            HerbloreUi.PAGE_FINISHED_QUEUE -> HerbloreActivityMode.FINISHED_POTIONS
            HerbloreUi.PAGE_WHOLE_SHOW -> HerbloreActivityMode.HERB_CLEANING
            HerbloreUi.PAGE_GENERAL_LEGACY -> HerbloreActivityMode.fromUi(optionString(HerbloreUi.OPTION_MODE, "Herb cleaning"))
            else -> HerbloreActivityMode.fromUi(optionString(HerbloreUi.OPTION_MODE, "Herb cleaning"))
        }
    }

    private fun optionString(name: String, fallback: String): String {
        return runCatching { script.getOption<String>(name) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: fallback
    }

    private fun optionBoolean(name: String, fallback: Boolean): Boolean {
        return runCatching { script.getOption<Boolean>(name) }.getOrDefault(fallback)
    }
}

