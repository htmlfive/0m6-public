package org.powbot.community.herblore.data

data class HerbDefinition(
    val displayName: String,
    val grimyName: String,
    val cleanName: String,
    val cleanLevel: Int,
    val unfinishedLevel: Int
)

object HerbDefinitions {
    val all: List<HerbDefinition> = listOf(
        HerbDefinition("Guam", "Grimy guam leaf", "Guam leaf", 3, 1),
        HerbDefinition("Marrentill", "Grimy marrentill", "Marrentill", 5, 5),
        HerbDefinition("Tarromin", "Grimy tarromin", "Tarromin", 11, 12),
        HerbDefinition("Harralander", "Grimy harralander", "Harralander", 20, 22),
        HerbDefinition("Ranarr", "Grimy ranarr weed", "Ranarr weed", 25, 30),
        HerbDefinition("Toadflax", "Grimy toadflax", "Toadflax", 30, 34),
        HerbDefinition("Irit", "Grimy irit leaf", "Irit leaf", 40, 45),
        HerbDefinition("Avantoe", "Grimy avantoe", "Avantoe", 48, 50),
        HerbDefinition("Kwuarm", "Grimy kwuarm", "Kwuarm", 54, 55),
        HerbDefinition("Snapdragon", "Grimy snapdragon", "Snapdragon", 59, 63),
        HerbDefinition("Cadantine", "Grimy cadantine", "Cadantine", 65, 66),
        HerbDefinition("Lantadyme", "Grimy lantadyme", "Lantadyme", 67, 69),
        HerbDefinition("Dwarf weed", "Grimy dwarf weed", "Dwarf weed", 70, 72),
        HerbDefinition("Torstol", "Grimy torstol", "Torstol", 75, 78)
    )

    private val byDisplay = all.associateBy { it.displayName }

    fun fromDisplayName(displayName: String): HerbDefinition? = byDisplay[displayName]
}

