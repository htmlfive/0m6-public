package org.powbot.community.communitypowersalvage.tasks.execution

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.community.api.ScriptLogging
import org.powbot.community.communitypowersalvage.CommunityPowerSalvage
import org.powbot.community.communitypowersalvage.config.Constants
import org.powbot.community.communitypowersalvage.config.sailingAngle
import org.powbot.community.communitypowersalvage.tasks.base.Task

class DeployHookTask(script: CommunityPowerSalvage) : Task(script) {
    private var lastAnimationLog = 0L
    private var lastScanLog = 0L

    companion object {
        val ACTIVE_SHIPWRECK_IDS = intArrayOf(
            60464, 60466, 60468, 60470, 60472, 60474, 60476, 60478,
        )
    }

    override fun activate(): Boolean {
        return !Inventory.isFull() && Objects.stream(10).id(*ACTIVE_SHIPWRECK_IDS).first().valid()
    }

    override fun execute() {
        script.status = "Deploying"

        val shipwreck = Objects.stream(10).id(*ACTIVE_SHIPWRECK_IDS).first()
        if (!shipwreck.valid()) {
            ScriptLogging.info(script.logger, "DEPLOY: No active shipwreck found.")
            return
        }

        val allDeployHooks = Objects.stream().action("Deploy").inMyWorldView().list()

        val hooks = allDeployHooks.filter { it.name() == script.salvagingHookName }
        if (hooks.isEmpty()) {
            ScriptLogging.warn(script.logger, "DEPLOY: No hook matching '${script.salvagingHookName}' found among ${allDeployHooks.size} candidates")
            return
        }

        val allCargoHolds = Objects.stream().action("Open").filtered { it.name().contains("cargo hold", ignoreCase = true) }.inMyWorldView().list()

        val cargoHold = Objects.stream().name(script.cargoHoldTierDisplayName).action("Open").inMyWorldView().nearest().first()
        val hook = if (cargoHold.valid() && hooks.size > 1) {
            hooks.maxByOrNull { it.tile().distanceTo(cargoHold.tile()) }!!
        } else {
            hooks.minByOrNull { it.tile().distanceTo(shipwreck.tile()) }!!
        }

        val now = System.currentTimeMillis()
        if (now - lastScanLog > 2000) {
            lastScanLog = now
            ScriptLogging.info(script.logger, "DEPLOY: Active shipwreck at ${shipwreck.tile()}. Found hook at ${hook.tile()} (${hook.name()}), ${hooks.size} candidate(s)")
        }

        if (Players.local().animation() != -1) {
            val now = System.currentTimeMillis()
            if (now - lastAnimationLog > 2000) {
                lastAnimationLog = now
                ScriptLogging.info(script.logger, "DEPLOY: Player still animating, waiting...")
            }
            return
        }

        Camera.turnTo(hook)

        val angle = sailingAngle()
        val b = Constants.Bounds.forAngle(angle)
        hook.bounds(b.hX1, b.hX2, b.hY1, b.hY2, b.hZ1, b.hZ2)
        Condition.wait({ false }, Random.nextInt(300, 600), 1)

        val clicked = if (script.justHopped) {
            script.justHopped = false
            ScriptLogging.info(script.logger, "DEPLOY: First interaction after hop, using click()")
            hook.click()
        } else {
            hook.interact("Deploy")
        }
        if (clicked) {
            ScriptLogging.info(script.logger, "DEPLOY: Hook clicked. Waiting for animation...")
            val animated = Condition.wait({ Players.local().animation() != -1 }, Random.nextInt(150, 300), 8)
            if (animated) {
                ScriptLogging.info(script.logger, "DEPLOY: Hook animation started.")
                Condition.wait({ Players.local().animation() == -1 }, 150, 10)
                ScriptLogging.info(script.logger, "DEPLOY: Deploy cycle finished.")
            } else {
                ScriptLogging.info(script.logger, "DEPLOY: No animation after ~1800ms. Will retry on next poll.")
            }
        } else {
            ScriptLogging.warn(script.logger, "DEPLOY: Failed to interact with hook. Retrying...")
        }
    }
}
