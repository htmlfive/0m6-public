package org.powbot.om6.herblore.data

enum class HerbloreActivityMode(val uiValue: String) {
    HERB_CLEANING("Herb cleaning"),
    UNFINISHED_POTIONS("Unfinished potions"),
    FINISHED_POTIONS("Finished potions");

    companion object {
        fun fromUi(value: String): HerbloreActivityMode {
            return entries.firstOrNull { it.uiValue.equals(value, ignoreCase = true) } ?: HERB_CLEANING
        }
    }
}
