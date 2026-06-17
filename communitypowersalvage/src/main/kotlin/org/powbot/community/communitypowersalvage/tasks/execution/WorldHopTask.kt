package org.powbot.community.communitypowersalvage.tasks.execution

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.World
import org.powbot.api.rt4.Worlds
import org.powbot.community.api.ScriptLogging
import org.powbot.community.communitypowersalvage.CommunityPowerSalvage
import org.powbot.community.communitypowersalvage.tasks.base.Task

class WorldHopTask(script: CommunityPowerSalvage) : Task(script) {
    companion object {
        val ACTIVE_SHIPWRECK_IDS = intArrayOf(
            60464, 60466, 60468, 60470, 60472, 60474, 60476, 60478,
        )
    }

    override fun activate(): Boolean {
        return script.hopWorlds && !Objects.stream(10).id(*ACTIVE_SHIPWRECK_IDS).first().valid()
    }

    override fun execute() {
        script.status = "Hopping worlds"
        ScriptLogging.info(script.logger, "WORLD: No shipwreck found and hop enabled. Starting world hop.")

        val currentWorld = Worlds.current()
        ScriptLogging.info(script.logger, "WORLD: Current world: ${currentWorld.id()}")

        val validWorlds = Worlds.stream()
            .filtered {
                it.type() == World.Type.MEMBERS &&
                        it.specialty() == World.Specialty.NONE &&
                        it.number != 318 &&
                        it.number != 569 &&
                        it.specialty() != World.Specialty.BOUNTY_HUNTER
            }
            .toList()
            .shuffled()

        if (validWorlds.isEmpty()) {
            ScriptLogging.warn(script.logger, "WORLD: No valid worlds found to hop to")
            Condition.wait({ false }, 600, 1)
            return
        }

        for (world in validWorlds.take(1)) {
            ScriptLogging.info(script.logger, "WORLD: Attempting to hop to world: ${world.id()}")

            if (world.hop()) {
                val cancelAppeared = Condition.wait({
                    Components.stream()
                        .filtered { it.text().contains("Cancel", ignoreCase = true) && it.visible() }
                        .isNotEmpty()
                }, 300, 5)

                if (cancelAppeared) {
                    ScriptLogging.warn(script.logger, "WORLD: Cancel dialog appeared - world hop blocked")

                    val cancelComponent = Components.stream()
                        .filtered { it.text().contains("Cancel", ignoreCase = true) && it.visible() }
                        .firstOrNull()

                    cancelComponent?.let {
                        ScriptLogging.info(script.logger, "WORLD: Clicking Cancel button")
                        it.click()
                        Condition.wait({ !it.visible() }, 100, 10)
                    }

                    ScriptLogging.warn(script.logger, "WORLD: Failed to hop to world: ${world.id()}, trying next...")
                    Condition.wait({ false }, 300, 1)
                    continue
                }

                if (Condition.wait({ Worlds.current() != currentWorld }, 1500, 10)) {
                    ScriptLogging.info(script.logger, "WORLD: Successfully hopped to world: ${Worlds.current().id()}")
                    script.justHopped = true
                    if (script.tapToDrop) {
                        if (Game.getMouseToggle() != Game.MouseToggleAction.DROP) {
                            Game.setMouseToggleAction(Game.MouseToggleAction.DROP)
                        }
                        Game.setMouseActionToggled(true)
                    }
                    val targetPitch = Random.nextInt(90, 101)
                    ScriptLogging.info(script.logger, "CAMERA: Pitch is ${Camera.pitch()}, setting to $targetPitch")
                    Camera.pitch(targetPitch)
                    return
                }
            }

            ScriptLogging.warn(script.logger, "WORLD: Failed to hop to world: ${world.id()}, trying next...")
            Condition.wait({ false }, 300, 1)
        }

        ScriptLogging.warn(script.logger, "WORLD: Failed to hop after 10 attempts")
    }
}
