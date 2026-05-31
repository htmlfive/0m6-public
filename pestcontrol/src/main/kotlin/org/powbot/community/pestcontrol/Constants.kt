package org.powbot.community.pestcontrol

object Constants {
    const val PORTAL_ATTACK_DISTANCE = 4
    const val PORTAL_LOW_HEALTH_THRESHOLD = 20
    const val BRAWLER_PRIORITY_DISTANCE = 3
    const val BRAWLER_HEALTH_THRESHOLD = 20
    const val SPINNER_SEARCH_DISTANCE = 7
    const val SPINNER_MIN_COUNT = 2
    const val SPINNER_HEALTH_THRESHOLD = 20
    const val NEAREST_NPC_SEARCH_DISTANCE = 7
    const val MONSTER_SEARCH_DISTANCE = 12
    const val BRAWLER_SEARCH_DISTANCE = 6
    const val MONSTER_HEALTH_THRESHOLD = 10
    const val GANGPLANK_SEARCH_DISTANCE = 4.0
    const val GATE_SEARCH_DISTANCE = 2
    const val PORTAL_TILE_PROXIMITY = 2
    const val PORTAL_SEARCH_DISTANCE = 7
    const val KNIGHT_DISTANCE_THRESHOLD = 4
    const val VOID_KNIGHT_NEARBY_DISTANCE = 1

    const val LOW_ZEAL_THRESHOLD = 30

    const val MAX_ZOOM_LEVEL = 5

    const val MIN_ENERGY_LEVEL = 5
    const val MAX_ENERGY_LEVEL = 10

    const val CHAT_WAIT_ATTEMPTS = 3
    const val CHAT_WAIT_INTERVAL = 500
    const val CHAT_CLOSE_MIN_DELAY = 600
    const val CHAT_CLOSE_MAX_DELAY = 800

    const val MIN_GAMES_BEFORE_ACTIVITY_CHANGE = 3
    const val MAX_GAMES_BEFORE_ACTIVITY_CHANGE = 7

    const val OBJECT_SEARCH_DISTANCE = 20

    const val IDLE_ANIMATION = -1

    const val PRAYER_ACTIVATION_WAIT_MS = 600

    const val VOID_KNIGHT_NAME = "Void Knight"
    const val SQUIRE_NAME = "Squire"
    const val PORTAL_NAME = "Portal"
    const val BRAWLER_NAME = "Brawler"
    const val SPINNER_NAME = "Spinner"
    const val GANGPLANK_NAME = "Gangplank"
    const val GATE_NAME = "Gate"

    const val ATTACK_ACTION = "Attack"
    const val CROSS_ACTION = "Cross"
    const val OPEN_ACTION = "Open"
    const val REPAIR_ACTION = "Repair"

    val MONSTER_NAMES = listOf(
        "Spinner", "Torcher", "Ravager", "Defiler", "Brawler", "Splatter", "Shifter"
    )
}
