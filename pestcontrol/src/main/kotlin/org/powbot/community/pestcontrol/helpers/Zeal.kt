package org.powbot.community.pestcontrol.helpers

import org.powbot.api.rt4.Widgets

object Zeal {

    fun percentage(): Int? {
        val barComp = Widgets.component(408, 11)
        if (barComp.visible()) {
            val barLength = barComp.width()
            val activityBar = Widgets.component(408, 12, 0).width().toDouble()
            return ((activityBar / barLength) * 100).toInt()
        }

        return null
    }

}
