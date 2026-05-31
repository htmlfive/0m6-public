package org.powbot.community.pestcontrol.data

import org.powbot.api.rt4.Prayer

enum class PrayerType(val prayerName: String, val prayer: Prayer.Effect) {
    PROTECT_MAGIC("Protect from Magic", Prayer.Effect.PROTECT_FROM_MAGIC),
    PROTECT_MELEE("Protect from Melee", Prayer.Effect.PROTECT_FROM_MELEE),
    PROTECT_RANGE("Protect from Missiles", Prayer.Effect.PROTECT_FROM_MISSILES),
    REDEMPTION("Redemption", Prayer.Effect.REDEMPTION),

    EAGLE_EYE("Eagle Eye", Prayer.Effect.EAGLE_EYE),
    MYSTIC_MIGHT("Mystic Might", Prayer.Effect.MYSTIC_MIGHT),
    RIGOUR("Rigour", Prayer.Effect.RIGOUR),

    NONE("None", Prayer.Effect.THICK_SKIN);

    companion object {
        fun fromDisplayName(name: String): PrayerType? {
            return values().firstOrNull { it.prayerName == name }
        }
    }
}
