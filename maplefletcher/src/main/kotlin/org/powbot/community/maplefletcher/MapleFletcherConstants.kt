package org.powbot.community.maplefletcher

import org.powbot.api.Tile

object MapleFletcherConstants {
    const val KNIFE_NAME = "Knife"
    const val MAPLE_LOG_NAME = "Maple logs"
    const val MAPLE_LONGBOW_NAME = "Maple longbow (u)"
    const val MAPLE_TREE_NAME = "Maple tree"
    const val BANK_OBJECT = "Bank booth"
    val TREE_TILE: Tile = Tile(2728, 3500, 0)
    val BANK_TILE: Tile = Tile(2725, 3492, 0)
    const val TREE_RADIUS = 7
    const val BANK_RADIUS = 6
    const val TREE_MIN_Y_OFFSET_FROM_BANK = 2

    enum class FletchTo(val label: String, val productName: String, val optionMatch: String) {
        ARROW_SHAFT("Arrow shaft", "Arrow shafts", "arrow shaft"),
        MAPLE_SHORTBOW_U("Maple shortbow (u)", "Maple shortbow (u)", "maple shortbow"),
        MAPLE_LONGBOW_U("Maple longbow (u)", "Maple longbow (u)", "maple longbow"),
        MAPLE_SHIELD("Maple shield", "Maple shield", "maple shield");

        companion object {
            fun fromLabel(label: String): FletchTo {
                return entries.firstOrNull { it.label.equals(label.trim(), ignoreCase = true) } ?: MAPLE_LONGBOW_U
            }
        }
    }
}

