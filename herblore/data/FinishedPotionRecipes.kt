package org.powbot.om6.herblore.data

data class FinishedPotionRecipe(
    val displayName: String,
    val level: Int,
    val unfinishedPotionName: String,
    val secondaryName: String,
    val supported: Boolean = true,
    val todoNote: String? = null
)

object FinishedPotionRecipes {
    val all: List<FinishedPotionRecipe> = listOf(
        FinishedPotionRecipe("Attack potion", 3, "Guam potion (unf)", "Eye of newt"),
        FinishedPotionRecipe("Antipoison", 5, "Marrentill potion (unf)", "Unicorn horn dust"),
        FinishedPotionRecipe("Strength potion", 12, "Tarromin potion (unf)", "Limpwurt root"),
        FinishedPotionRecipe("Serum 207", 15, "Tarromin potion (unf)", "Ashes"),
        FinishedPotionRecipe("Compost potion", 21, "Harralander potion (unf)", "Volcanic ash"),
        FinishedPotionRecipe("Restore potion", 22, "Harralander potion (unf)", "Red spiders' eggs"),
        FinishedPotionRecipe("Energy potion", 26, "Harralander potion (unf)", "Chocolate dust"),
        FinishedPotionRecipe("Defence potion", 30, "Ranarr potion (unf)", "White berries"),
        FinishedPotionRecipe("Agility potion", 34, "Toadflax potion (unf)", "Toad's legs"),
        FinishedPotionRecipe("Combat potion", 36, "Harralander potion (unf)", "Goat horn dust"),
        FinishedPotionRecipe("Prayer potion", 38, "Ranarr potion (unf)", "Snape grass"),
        FinishedPotionRecipe("Super attack", 45, "Irit potion (unf)", "Eye of newt"),
        FinishedPotionRecipe("Superantipoison", 48, "Irit potion (unf)", "Unicorn horn dust"),
        FinishedPotionRecipe("Fishing potion", 50, "Avantoe potion (unf)", "Snape grass"),
        FinishedPotionRecipe("Super energy", 52, "Avantoe potion (unf)", "Mort myre fungus"),
        FinishedPotionRecipe("Hunter potion", 53, "Avantoe potion (unf)", "Kebbit teeth dust"),
        FinishedPotionRecipe("Super strength", 55, "Kwuarm potion (unf)", "Limpwurt root"),
        FinishedPotionRecipe("Weapon poison", 60, "Kwuarm potion (unf)", "Dragon scale dust"),
        FinishedPotionRecipe("Super restore", 63, "Snapdragon potion (unf)", "Red spiders' eggs"),
        FinishedPotionRecipe("Super defence", 66, "Cadantine potion (unf)", "White berries"),
        FinishedPotionRecipe("Antifire potion", 69, "Lantadyme potion (unf)", "Dragon scale dust"),
        FinishedPotionRecipe("Ranging potion", 72, "Dwarf weed potion (unf)", "Wine of zamorak"),
        FinishedPotionRecipe("Magic potion", 76, "Lantadyme potion (unf)", "Potato cactus"),
        FinishedPotionRecipe("Zamorak brew", 78, "Torstol potion (unf)", "Jangerberries"),
        FinishedPotionRecipe("Saradomin brew", 81, "Toadflax potion (unf)", "Crushed nest"),
        FinishedPotionRecipe(
            "Goading potion",
            54,
            "Harralander potion (unf)",
            "Aldarium",
            supported = false,
            todoNote = "TODO: implement Varlamore/Aldarium recipe handling."
        ),
        FinishedPotionRecipe(
            "Haemostatic poultice",
            56,
            "Elkhorn potion (unf)",
            "Squid paste",
            supported = false,
            todoNote = "TODO: implement non-standard base potion line."
        ),
        FinishedPotionRecipe(
            "Haemostatic dressing",
            56,
            "Haemostatic poultice",
            "Cotton yarn",
            supported = false,
            todoNote = "TODO: implement chained non-(unf) base recipe."
        ),
        FinishedPotionRecipe(
            "Prayer regeneration potion",
            58,
            "Huasca potion (unf)",
            "Aldarium",
            supported = false,
            todoNote = "TODO: implement Huasca/Aldarium recipe handling."
        ),
        FinishedPotionRecipe(
            "Super fishing potion",
            62,
            "Pillar potion (unf)",
            "Haddock eye",
            supported = false,
            todoNote = "TODO: implement non-standard base potion line."
        ),
        FinishedPotionRecipe(
            "Extreme energy potion",
            66,
            "Super energy(4)",
            "Yellow fin (x4)",
            supported = false,
            todoNote = "TODO: implement x4 secondary quantity recipes."
        ),
        FinishedPotionRecipe(
            "Super hunter potion",
            67,
            "Pillar potion (unf)",
            "Crab paste",
            supported = false,
            todoNote = "TODO: implement non-standard base potion line."
        ),
        FinishedPotionRecipe(
            "Stamina potion",
            77,
            "Super energy(4)",
            "Amylase crystal (x4)",
            supported = false,
            todoNote = "TODO: implement x4 secondary quantity recipes."
        ),
        FinishedPotionRecipe(
            "Bastion potion",
            80,
            "Cadantine blood potion (unf)",
            "Wine of zamorak",
            supported = false,
            todoNote = "TODO: implement blood potion base line."
        ),
        FinishedPotionRecipe(
            "Battlemage potion",
            80,
            "Cadantine blood potion (unf)",
            "Potato cactus",
            supported = false,
            todoNote = "TODO: implement blood potion base line."
        ),
        FinishedPotionRecipe(
            "Surge potion",
            81,
            "Torstol potion (unf)",
            "Demonic tallow",
            supported = false,
            todoNote = "TODO: implement Demonic tallow recipe handling."
        ),
        FinishedPotionRecipe(
            "Extended antifire",
            84,
            "Antifire potion(4)",
            "Lava scale shard (x4)",
            supported = false,
            todoNote = "TODO: implement x4 upgrade potion recipes."
        ),
        FinishedPotionRecipe(
            "Ancient brew",
            85,
            "Dwarf weed potion (unf)",
            "Nihil dust",
            supported = false,
            todoNote = "TODO: implement Ancient brew line."
        ),
        FinishedPotionRecipe(
            "Extended stamina potion",
            85,
            "Stamina potion(4)",
            "Marlin scales (x4)",
            supported = false,
            todoNote = "TODO: implement x4 upgrade potion recipes."
        ),
        FinishedPotionRecipe(
            "Anti-venom",
            87,
            "Antidote++(4)",
            "Zulrah's scales (x20)",
            supported = false,
            todoNote = "TODO: implement x20 secondary quantity recipes."
        ),
        FinishedPotionRecipe(
            "Menaphite remedy",
            88,
            "Dwarf weed potion (unf)",
            "Lily of the sands",
            supported = false,
            todoNote = "TODO: implement Menaphite remedy handling."
        ),
        FinishedPotionRecipe(
            "Armadyl brew",
            89,
            "Umbral potion (unf)",
            "Rainbow crab paste",
            supported = false,
            todoNote = "TODO: implement Umbral potion base line."
        ),
        FinishedPotionRecipe(
            "Super combat potion",
            90,
            "Super attack/strength/defence(4)",
            "Torstol",
            supported = false,
            todoNote = "TODO: implement multi-base super combat recipe."
        ),
        FinishedPotionRecipe(
            "Forgotten brew",
            91,
            "Ancient brew(4)",
            "Ancient essence (x80)",
            supported = false,
            todoNote = "TODO: implement x80 secondary quantity recipes."
        ),
        FinishedPotionRecipe(
            "Super antifire potion",
            92,
            "Antifire potion(4)",
            "Crushed superior dragon bones",
            supported = false,
            todoNote = "TODO: implement upgrade potion recipes."
        ),
        FinishedPotionRecipe(
            "Anti-venom+",
            94,
            "Anti-venom(4)",
            "Torstol",
            supported = false,
            todoNote = "TODO: implement upgrade potion recipes."
        ),
        FinishedPotionRecipe(
            "Extended anti-venom+",
            94,
            "Anti-venom+(4)",
            "Araxyte venom sack (x4)",
            supported = false,
            todoNote = "TODO: implement x4 upgrade potion recipes."
        ),
        FinishedPotionRecipe(
            "Extended super antifire (from Super antifire potion(4))",
            98,
            "Super antifire potion(4)",
            "Lava scale shard (x4)",
            supported = false,
            todoNote = "TODO: implement x4 upgrade potion recipes."
        ),
        FinishedPotionRecipe(
            "Extended super antifire (from Extended antifire(4))",
            98,
            "Extended antifire(4)",
            "Crushed superior dragon bones",
            supported = false,
            todoNote = "TODO: implement alternate route upgrade recipes."
        )
    )

    private val byDisplay = all.associateBy { it.displayName }
    val supported: List<FinishedPotionRecipe> = all.filter { it.supported }

    fun fromDisplayName(displayName: String): FinishedPotionRecipe? = byDisplay[displayName]
}
