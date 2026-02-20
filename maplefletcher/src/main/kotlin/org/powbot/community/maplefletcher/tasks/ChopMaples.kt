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
    private var lastChopAttemptAt: Long = 0L

    override fun execute(): Int {
        if (Inventory.isFull()) return 250
        if (!ensureAtTrees()) return 300
        if (Players.local().animation() != -1) return 500

        val tree = Objects.stream(script.treeTile, MapleFletcherConstants.TREE_RADIUS)
            .name(MapleFletcherConstants.MAPLE_TREE_NAME)
            .action("Chop down")
            .filtered { it.tile().y() >= script.bankTile.y() + MapleFletcherConstants.TREE_MIN_Y_OFFSET_FROM_BANK }
            .nearest()
            .first()

        if (!tree.valid()) {
            logger.fine("No maple tree found in the Seers north cluster.")
            return 450
        }

        val now = System.currentTimeMillis()
        if (now - lastChopAttemptAt < 1400L) {
            return 300
        }

        if (!tree.inViewport()) {
            Camera.turnTo(tree)
        }

        val logsBefore = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).count(true).toInt()
        if (tree.interact("Chop down")) {
            lastChopAttemptAt = now
            val started = Condition.wait({ Players.local().animation() != -1 }, 100, 25)
            val success = if (started) {
                Condition.wait(
                    {
                        Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).count(true).toInt() > logsBefore ||
                            Inventory.isFull() ||
                            !tree.valid()
                    },
                    250,
                    30
                )
            } else {
                false
            }
            if (success) {
                return 350
            }
        } else {
            lastChopAttemptAt = now
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

