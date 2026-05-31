@file:Suppress("unused")

package org.powbot.community.pestcontrol.helpers

import org.powbot.api.Locatable
import org.powbot.api.rt4.Npc
import org.powbot.api.rt4.Npcs
import org.powbot.community.pestcontrol.Constants

fun voidKnight(): Npc {
    return Npcs.stream().name(Constants.VOID_KNIGHT_NAME).first()
}

fun squire(): Npc {
    return Npcs.stream().name(Constants.SQUIRE_NAME).first()
}

fun nextMonster(locatable: Locatable): Npc {
    val brawler = Npcs.stream().within(Constants.BRAWLER_SEARCH_DISTANCE.toDouble())
        .name(Constants.BRAWLER_NAME).nearest().viewable()
        .filtered { it.healthPercent() > Constants.BRAWLER_HEALTH_THRESHOLD }.firstOrNull()

    if (brawler?.valid() == true) {
        return brawler
    }

    val monster = Npcs.stream().name(*Constants.MONSTER_NAMES.toTypedArray())
        .viewable()
        .filtered { it.healthPercent() == Constants.IDLE_ANIMATION || it.healthPercent() > Constants.MONSTER_HEALTH_THRESHOLD }
        .filtered { it.tile().distanceTo(locatable) < Constants.MONSTER_SEARCH_DISTANCE }
        .toList()
        .minByOrNull { it.tile().distanceTo(locatable) }

    if (monster?.valid() == true) {
        return monster
    }

    return Npc.Nil
}
