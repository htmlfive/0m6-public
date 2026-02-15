package org.powbot.om6.herbrun.services

import org.powbot.api.rt4.GameObject
import org.powbot.om6.herbrun.HerbRun

internal class PatchStateResolver {
    internal fun resolve(patchObject: GameObject): HerbRun.PatchState {
        if (patchObject == GameObject.Nil || !patchObject.valid()) {
            return HerbRun.PatchState.UNKNOWN
        }
        val name = patchObject.name().lowercase()
        val actions = patchObject.actions().map { it.lowercase() }

        val hasInspect = actions.any { it.contains("inspect") }

        return when {
            "dead" in name || actions.any { it.contains("clear") } -> HerbRun.PatchState.DEAD
            actions.any { it.contains("pick") || it.contains("harvest") } -> HerbRun.PatchState.READY_TO_HARVEST
            actions.any { it.contains("rake") } -> HerbRun.PatchState.NEEDS_WEEDING
            actions.any { it.contains("plant") } -> HerbRun.PatchState.EMPTY_SOIL
            hasInspect && name.contains("herb patch") -> HerbRun.PatchState.EMPTY_SOIL
            hasInspect && name.contains("herbs") -> HerbRun.PatchState.GROWING
            else -> HerbRun.PatchState.GROWING
        }
    }
}
