package org.powbot.community.herbrun.config

import org.powbot.api.Tile

/**
 * Represents the herb the user wants to plant/harvest on this run.
 */
enum class HerbType(
    val displayName: String,
    val seedName: String,
    val produceName: String
) {
    GUAM("Guam", "Guam seed", "Grimy guam leaf"),
    MARRENTILL("Marrentill", "Marrentill seed", "Grimy marrentill"),
    TARROMIN("Tarromin", "Tarromin seed", "Grimy tarromin"),
    HARRALANDER("Harralander", "Harralander seed", "Grimy harralander"),
    RANARR("Ranarr", "Ranarr seed", "Grimy ranarr weed"),
    TOADFLAX("Toadflax", "Toadflax seed", "Grimy toadflax"),
    IRIT("Irit", "Irit seed", "Grimy irit leaf"),
    AVANTOE("Avantoe", "Avantoe seed", "Grimy avantoe"),
    KWUARM("Kwuarm", "Kwuarm seed", "Grimy kwuarm"),
    SNAPDRAGON("Snapdragon", "Snapdragon seed", "Grimy snapdragon"),
    CADANTINE("Cadantine", "Cadantine seed", "Grimy cadantine"),
    LANTADYME("Lantadyme", "Lantadyme seed", "Grimy lantadyme"),
    DWARF_WEED("Dwarf weed", "Dwarf weed seed", "Grimy dwarf weed"),
    TORSTOL("Torstol", "Torstol seed", "Grimy torstol"),
    FELLSTALK("Fellstalk", "Fellstalk seed", "Grimy fellstalk");

    companion object {
        fun fromOption(value: String): HerbType {
            return values().firstOrNull { it.displayName.equals(value, ignoreCase = true) } ?: RANARR
        }
    }
}

/**
 * Compost tiers supported by the script.
 */
enum class CompostType(
    val displayName: String,
    val itemName: String,
    val keywords: List<String>
) {
    COMPOST("Compost", "Compost", listOf("compost")),
    SUPERCOMPOST("Supercompost", "Supercompost", listOf("supercompost")),
    ULTRACOMPOST("Ultracompost", "Ultracompost", listOf("ultracompost")),
    BOTTOMLESS("Bottomless bucket", "Bottomless bucket", listOf("bottomless", "bucket"));

    companion object {
        fun detectFromInventory(inventoryItems: List<InventoryItem>): String? {
            for (type in values()) {
                val match = inventoryItems.firstOrNull { item ->
                    val lower = item.name.lowercase()
                    type.keywords.any { keyword -> lower.contains(keyword) }
                }
                if (match != null) {
                    return match.name
                }
            }
            return null
        }
    }
}

/**
 * Simple teleport entry used for paint/debug.
 */
data class TeleportOption(
    val label: String,
    val details: String = ""
)

/**
 * Herb patch definitions with travel hints and diary bonuses.
 * Tile coordinates are intentionally near the herb patch center
 * to help with locating patch objects and guarding NPCs.
 */
enum class HerbPatch(
    val displayName: String,
    val optionName: String,
    val tile: Tile,
    val herbPatchObjectId: Int? = null,
    val gardener: String? = null,
    val teleports: List<TeleportOption> = emptyList(),
    val diaryBonus: String? = null,
    val diseaseFreeNote: String? = null
) {
    FALADOR(
        displayName = "Falador",
        optionName = "Run Falador Patch",
        tile = Tile(3056, 3310, 0),
        herbPatchObjectId = 8150,
        gardener = "Elstan",
        teleports = listOf(
            TeleportOption("Explorer's ring 2+", "Cabbage patch teleport"),
            TeleportOption("Ring of the elements -> Air Altar"),
            TeleportOption("Spirit tree -> Port Sarim"),
            TeleportOption("Draynor Manor teleport"),
            TeleportOption("Glory -> Draynor"),
            TeleportOption("Skills necklace -> Mining Guild"),
            TeleportOption("Ring of wealth -> Falador Park"),
            TeleportOption("Falador teleport")
        ),
        diaryBonus = "10% extra farming experience after Falador medium diary."
    ),
    PORT_PHASMATYS(
        displayName = "Port Phasmatys",
        optionName = "Run Port Phasmatys Patch",
        tile = Tile(3607, 3532, 0),
        herbPatchObjectId = 8153,
        gardener = "Lyra",
        teleports = listOf(
            TeleportOption("Ectophial"),
            TeleportOption("Fairy ring ALQ"),
            TeleportOption("Fenkenstrain's Castle teleport"),
            TeleportOption("Kharyrll teleport"),
            TeleportOption("Charter ship -> Port Phasmatys")
        )
    ),
    CATHERBY(
        displayName = "Catherby",
        optionName = "Run Catherby Patch",
        tile = Tile(2813, 3465, 0),
        herbPatchObjectId = 8151,
        gardener = "Dantaera",
        teleports = listOf(
            TeleportOption("Catherby teleport"),
            TeleportOption("Camelot teleport then run east"),
            TeleportOption("Charter ship -> Catherby")
        ),
        diaryBonus = "5/10/15% extra herbs after Kandarin diary tiers."
    ),
    ARDOUGNE(
        displayName = "Ardougne",
        optionName = "Run Ardougne Patch",
        tile = Tile(2672, 3375, 0),
        herbPatchObjectId = 8152,
        gardener = "Kragen",
        teleports = listOf(
            TeleportOption("Ardougne cloak 2+"),
            TeleportOption("Skills necklace -> Fishing Guild"),
            TeleportOption("Fishing Guild teleport"),
            TeleportOption("Combat bracelet -> Ranging Guild"),
            TeleportOption("Quest point cape teleport"),
            TeleportOption("Fairy ring BLR"),
            TeleportOption("Ardougne teleport")
        )
    ),
    HOSIDIUS(
        displayName = "Hosidius",
        optionName = "Run Hosidius Patch",
        tile = Tile(1738, 3552, 0),
        herbPatchObjectId = 27115,
        gardener = "Marisi",
        teleports = listOf(
            TeleportOption("Xeric's talisman -> Xeric's Glade"),
            TeleportOption("Portal/Nexus -> Hosidius"),
            TeleportOption("Kharedst's memoirs -> Lunch by the lancalliums"),
            TeleportOption("Tithe Farm teleport"),
            TeleportOption("Spirit tree -> Hosidius"),
            TeleportOption("Fairy ring AKR then run west"),
            TeleportOption("Skills necklace -> Woodcutting Guild")
        ),
        diaryBonus = "Disease free + extra yield after Kourend diary tiers.",
        diseaseFreeNote = "Disease-free after Easy Kourend diary."
    ),
    TROLL_STRONGHOLD(
        displayName = "Troll Stronghold",
        optionName = "Run Troll Stronghold Patch",
        tile = Tile(2861, 3695, 2),
        gardener = "My Arm",
        teleports = listOf(
            TeleportOption("Stony basalt teleport"),
            TeleportOption("Trollheim teleport then run west"),
            TeleportOption("Redirected house tab -> Trollheim portal")
        ),
        diseaseFreeNote = "Disease-free after My Arm's Big Adventure."
    ),
    HARMONY_ISLAND(
        displayName = "Harmony Island",
        optionName = "Run Harmony Island Patch",
        tile = Tile(3809, 2836, 0),
        teleports = listOf(
            TeleportOption("Harmony Island teleport"),
            TeleportOption("Mos le'harmless scroll -> Brother Tranquility"),
            TeleportOption("Ectophial -> Mos ferry -> Brother Tranquility"),
            TeleportOption("Trouble Brewing teleport -> run west/south")
        ),
        diseaseFreeNote = "Requires Elite Morytania diary for access and disease immunity."
    ),
    WEISS(
        displayName = "Weiss",
        optionName = "Run Weiss Patch",
        tile = Tile(2848, 3945, 0),
        gardener = "Boulder",
        teleports = listOf(
            TeleportOption("Icy basalt"),
            TeleportOption("Fairy ring DKS -> Larry's boat"),
            TeleportOption("Ring of shadows -> Ghorrock Dungeon")
        ),
        diseaseFreeNote = "Disease-free with Making Friends With My Arm."
    ),
    FARMING_GUILD(
        displayName = "Farming Guild",
        optionName = "Run Farming Guild Patch",
        tile = Tile(1240, 3730, 0),
        herbPatchObjectId = 33979,
        teleports = listOf(
            TeleportOption("Farming cape teleport"),
            TeleportOption("Skills necklace -> Farming Guild"),
            TeleportOption("Spirit tree planted in Farming Guild"),
            TeleportOption("Lovakengj minecart -> Farming Guild"),
            TeleportOption("Fairy ring CIR"),
            TeleportOption("Battlefront teleport then run west")
        ),
        diaryBonus = "5% extra herbs after Kourend & Kebos hard diary."
    ),
    CIVITAS(
        displayName = "Civitas illa Fortis",
        optionName = "Run Civitas Patch",
        tile = Tile(1583, 3095, 0),
        herbPatchObjectId = 50697,
        gardener = "Harminia",
        teleports = listOf(
            TeleportOption("Quetzal whistle / Hunter cape -> Hunter Guild"),
            TeleportOption("Civitas teleport -> Quetzal transport -> Hunter Guild"),
            TeleportOption("Fairy ring AJP -> run north-west")
        ),
        diseaseFreeNote = "Disease-free when Champion rank is reached at Fortis Colosseum."
    );

    companion object {
        val defaultOrder: List<HerbPatch> = listOf(
            FALADOR,
            PORT_PHASMATYS,
            CATHERBY,
            ARDOUGNE,
            HOSIDIUS,
            TROLL_STRONGHOLD,
            HARMONY_ISLAND,
            WEISS,
            CIVITAS,
            FARMING_GUILD
        )
    }
}

/**
 * Tracks compost and planting progress for a patch during the current run.
 */
data class HerbPatchStatus(
    var compostApplied: Boolean = false,
    var seedPlanted: Boolean = false,
    var lastHarvestTime: Long = 0L,
    var missingCount: Int = 0
) {
    fun reset() {
        compostApplied = false
        seedPlanted = false
        missingCount = 0
    }
}

/**
 * Represents an entry from the GUI inventory builder.
 */
data class InventoryItem(
    val id: Int,
    val name: String,
    val quantity: Int
)

/**
 * Consolidated configuration for the herb run.
 */
data class HerbRunConfig(
    val herbType: HerbType,
    val seedItemName: String?,
    val fallbackSeedItemNames: List<String>,
    val compostItemName: String?,
    val inventoryItems: List<InventoryItem>,
    val enabledPatches: List<HerbPatch>,
    val runBigCompostBin: Boolean,
    val loopRuns: Boolean,
    val noteHarvest: Boolean,
    val pickpocketBetweenRuns: Boolean,
    val enableLimpwurtFarming: Boolean,
    val startWithPickpocket: Boolean,
    val pickpocketWineWithdrawAmount: Int,
    val pickpocketHealHpDeficit: Int,
    val pickpocketMasterFarmerTile: Tile
) {
    val hasSeedName: Boolean get() = !seedItemName.isNullOrBlank()
    val hasCompostName: Boolean get() = !compostItemName.isNullOrBlank()
    val seedPriorityItemNames: List<String>
        get() = listOfNotNull(seedItemName) + fallbackSeedItemNames
}

