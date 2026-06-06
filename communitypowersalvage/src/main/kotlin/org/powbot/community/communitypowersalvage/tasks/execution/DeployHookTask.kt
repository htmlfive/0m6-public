package org.powbot.community.communitypowersalvage.tasks.execution

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.community.api.ScriptLogging
import org.powbot.community.communitypowersalvage.CommunityPowerSalvage
import org.powbot.community.communitypowersalvage.tasks.base.Task

class DeployHookTask(script: CommunityPowerSalvage) : Task(script) {
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

        ScriptLogging.info(script.logger, "DEPLOY: Active shipwreck at ${shipwreck.tile()}. Looking for a salvaging hook...")

        val allDeployHooks = Objects.stream().action("Deploy").inMyWorldView().list()
        ScriptLogging.info(script.logger, "DEPLOY: Found ${allDeployHooks.size} deploy hooks in world view")
        allDeployHooks.forEach { ScriptLogging.info(script.logger, "DEPLOY:   Hook candidate: name=${it.name()} tile=${it.tile()}") }

        val hooks = allDeployHooks.filter { it.name() == script.salvagingHookName }
        if (hooks.isEmpty()) {
            ScriptLogging.warn(script.logger, "DEPLOY: No hook matching '${script.salvagingHookName}' found among ${allDeployHooks.size} candidates")
            return
        }
        ScriptLogging.info(script.logger, "DEPLOY: ${hooks.size} hook(s) match '${script.salvagingHookName}'")

        val allCargoHolds = Objects.stream().action("Open").filtered { it.name().contains("cargo hold", ignoreCase = true) }.inMyWorldView().list()
        ScriptLogging.info(script.logger, "DEPLOY: Found ${allCargoHolds.size} cargo holds in world view")
        allCargoHolds.forEach { ScriptLogging.info(script.logger, "DEPLOY:   Cargo candidate: name=${it.name()} tile=${it.tile()}") }

        val cargoHold = Objects.stream().name(script.cargoHoldTierDisplayName).action("Open").inMyWorldView().nearest().first()
        val hook = if (cargoHold.valid() && hooks.size > 1) {
            ScriptLogging.info(script.logger, "DEPLOY: ${hooks.size} hooks match. Using furthest from cargo hold at ${cargoHold.tile()}.")
            hooks.maxByOrNull { it.tile().distanceTo(cargoHold.tile()) }!!
        } else {
            ScriptLogging.info(script.logger, "DEPLOY: ${hooks.size} hooks match, no cargo hold detected. Using nearest to shipwreck.")
            hooks.minByOrNull { it.tile().distanceTo(shipwreck.tile()) }!!
        }
        ScriptLogging.info(script.logger, "DEPLOY: Selected hook at ${hook.tile()} name=${hook.name()}")

        if (Players.local().animation() != -1) {
            ScriptLogging.info(script.logger, "DEPLOY: Player still animating, waiting...")
            return
        }

        hook.bounds(-82, 42, -124, -10, -82, 52)
        if (hook.interact("Deploy")) {
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
