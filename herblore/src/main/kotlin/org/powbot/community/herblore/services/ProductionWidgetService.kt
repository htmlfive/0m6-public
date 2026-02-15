package org.powbot.community.herblore.services

import org.powbot.api.rt4.Widgets

class ProductionWidgetService {

    fun clickMakeAction(): Boolean {
        val makeComponent = Widgets.component(270, 15)
        if (!makeComponent.valid() || !makeComponent.visible()) return false
        return makeComponent.click()
    }

    fun hasMakeComponent(): Boolean {
        val makeComponent = Widgets.component(270, 15)
        return makeComponent.valid() && makeComponent.visible()
    }
}

