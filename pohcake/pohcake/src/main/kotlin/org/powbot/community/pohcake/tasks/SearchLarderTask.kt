package org.powbot.community.pohcake.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Objects
import org.powbot.community.pohcake.PohCakeMaker
import org.powbot.community.pohcake.Task

private const val LARDER_NAME = "Larder"

class SearchLarderTask(script: PohCakeMaker) : Task(script, "Search Larder") {

    override fun activate(): Boolean =
        script.nextMissingLarderOption() != null

    override fun execute() {
        val option = script.nextMissingLarderOption() ?: return
        script.logInventorySnapshot()

        if (Inventory.emptySlotCount() < 4) {
            script.logger.info("Low inventory space (<4). Dropping before searching larder.")
            if (script.dropJunkItems()) {
                return
            }
            Condition.sleep(150)
            return
        }

        if (Chat.chatting()) {
            script.selectChatOption(option)
            return
        }

        val larder = Objects.stream()
            .name(LARDER_NAME)
            .nearest()
            .first()

        if (!larder.valid()) {
            script.logger.warn("Larder not found.")
            Condition.sleep(200)
            return
        }

        if (!larder.inViewport()) {
            Camera.turnTo(larder)
        }

        script.logger.info("Searching larder for: $option")
        if (larder.click()) {
            Condition.wait({ Chat.chatting() }, 150, 20)
        } else {
            script.logger.warn("Failed to interact with larder.")
            Condition.sleep(200)
        }
    }
}

