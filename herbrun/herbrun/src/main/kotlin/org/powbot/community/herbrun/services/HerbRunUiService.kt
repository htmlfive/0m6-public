package org.powbot.community.herbrun.services

import org.powbot.api.event.PaintCheckboxChangedEvent

class HerbRunUiService(private val script: org.powbot.community.herbrun.HerbRun) {
    fun onPaintCheckboxChanged(evt: PaintCheckboxChangedEvent) {
        when (evt.checkboxId) {
            script.uiStartRunNowCheckboxId() -> {
                if (evt.checked) {
                    script.uiSetStartRunNowRequested(true)
                    script.uiInfo("Start run now checkbox set to: true")
                }
            }
            script.uiShowTrackedItemsCheckboxId() -> {
                toggleTrackedItems(evt.checked)
                val state = if (evt.checked) "shown" else "hidden"
                script.uiInfo("Tracked paint items are now $state.")
            }
            script.uiShowRunConfigCheckboxId() -> {
                toggleRunConfig(evt.checked)
                val state = if (evt.checked) "shown" else "hidden"
                script.uiInfo("Run config paint items are now $state.")
            }
        }
    }

    fun toggleTrackedItems(show: Boolean) {
        script.uiSetShowTrackedItems(show)
        if (!script.uiPaintBuilderInitialized()) {
            return
        }
        script.uiEnsureTrackedItemRows()
        if (script.uiTrackedItemPaintRows().isEmpty()) {
            return
        }
        val items = script.uiPaintItems()
        script.uiTrackedItemPaintRows().forEach { row -> items.remove(row) }
        if (show) {
            val insertIndex = script.uiTrackedItemInsertIndex().coerceIn(0, items.size)
            items.addAll(insertIndex, script.uiTrackedItemPaintRows())
        }
    }

    fun toggleRunConfig(show: Boolean) {
        script.uiSetShowRunConfig(show)
        if (!script.uiPaintBuilderInitialized() || script.uiRunConfigPaintRows().isEmpty()) {
            return
        }
        val items = script.uiPaintItems()
        script.uiRunConfigPaintRows().forEach { row -> items.remove(row) }
        if (show) {
            val insertIndex = script.uiRunConfigInsertIndex().coerceIn(0, items.size)
            items.addAll(insertIndex, script.uiRunConfigPaintRows())
        }
    }

    fun updateConfigPageVisibility(page: String) {
        script.uiSetCurrentConfigPage(page)
        val visibleSet = script.uiPageVisibleSet(page)
        script.uiAllPageOptions().forEach { optionName ->
            script.uiUpdateOptionVisibility(optionName, visibleSet.contains(optionName))
        }
    }
}

