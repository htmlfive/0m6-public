@file:Suppress("unused")

package org.powbot.community.pestcontrol.helpers

import org.powbot.api.rt4.Component
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Widgets


fun currentPoints(): Component {
    return Components.stream(407)
        .textContains("Pest Points").first()
}

fun portalHealth(idx: Int): Int {
    val w = Widgets.component(408, idx)
    if (w.valid() && w.text() != "") {
        return w.text().toInt()
    }

    return 0
}

fun activityLevelPercentage(): Int {
    val c = Components.stream(408, 12).filtered { it.textColor() == 40704 }.firstOrNull() ?: return 0

    return (c.width() / 141) * 100
}

fun portalHasShield(idx: Int): Boolean {
    return Widgets.component(408, idx).visible()
}


fun voidKnightHealth(): Component {
    return Widgets.component(408, 6)
}
