package org.powbot.community.maplefletcher.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.community.maplefletcher.MapleFletcher
import org.powbot.community.maplefletcher.MapleFletcherConstants
import java.util.logging.Logger

class ChopMaples(private val script: MapleFletcher) : Task {
    private val logger = Logger.getLogger(ChopMaples::class.java.name)

    override fun execute(): Int {
        if (Inventory.isFull()) return 250
        if (!ensureAtTrees()) return 300

        val tree = Objects.stream()
            .name(MapleFletcherConstants.MAPLE_TREE_NAME)
            .action("Chop down")
            .nearest()
            .first()

        if (!tree.valid()) {
            logger.fine("No maple tree found nearby.")
            return 300
        }

        if (!tree.inViewport()) {
            Camera.turnTo(tree)
        }

        val logsBefore = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).count(true).toInt()
        if (tree.interact("Chop down")) {
            Condition.wait(
                { Players.local().animation() != -1 },
                200,
                10
            )
            val success = Condition.wait(
                {
                    Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).count(true).toInt() > logsBefore ||
                        Players.local().animation() == -1 ||
                        Inventory.isFull()
                },
                300,
                20
            )
            if (success) {
                val logsAfter = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).count(true).toInt()
                if (logsAfter > logsBefore) {
                    val gained = logsAfter - logsBefore
                    script.logsCut += gained
                }
            }
        }

        return 250
    }

    private fun ensureAtTrees(): Boolean {
        val treeTile = script.treeTile
        if (treeTile.distance() <= MapleFletcherConstants.TREE_RADIUS) return true
        if (Movement.step(treeTile)) {
            Condition.wait(
                { treeTile.distance() <= MapleFletcherConstants.TREE_RADIUS },
                250,
                16
            )
        }
        return treeTile.distance() <= MapleFletcherConstants.TREE_RADIUS
    }

    override fun toString(): String = "Chopping maple trees"
}

