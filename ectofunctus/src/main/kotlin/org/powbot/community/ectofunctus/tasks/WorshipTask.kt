package org.powbot.community.ectofunctus.tasks

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.Tile
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.community.api.WaitUtils
import org.powbot.community.ectofunctus.Ectofunctus
import org.powbot.community.ectofunctus.EctofunctusConstants.BUCKET_OF_SLIME
import org.powbot.community.ectofunctus.EctofunctusConstants.DRAGON_BONEMEAL
import org.powbot.community.ectofunctus.EctofunctusConstants.ECTO_TOKEN
import org.powbot.community.ectofunctus.EctofunctusConstants.ECTOPHIAL
import org.powbot.community.ectofunctus.EctofunctusConstants.GHOST_DISCIPLE
import org.powbot.community.ectofunctus.EctofunctusConstants.WORSHIP_OBJECT
import java.util.logging.Logger

class WorshipTask(private val script: Ectofunctus) : Task {
    private val logger = Logger.getLogger(WorshipTask::class.java.name)

    override fun execute(): Int {
        val ectoTile = script.ectofuntusTile
        ensureNearEctofuntus(ectoTile)
        while (canWorship()) {
            if (!worshipOnce()) {
                Condition.sleep(200)
            }
        }
        if (Inventory.stream().name(DRAGON_BONEMEAL).isEmpty()) {
            collectEctoTokens()
        }
        return 250
    }

    private fun canWorship(): Boolean {
        return Inventory.stream().name(DRAGON_BONEMEAL).isNotEmpty() &&
            Inventory.stream().name(BUCKET_OF_SLIME).isNotEmpty()
    }

    private fun worshipOnce(): Boolean {
        val ectofuntus = Objects.stream()
            .name(WORSHIP_OBJECT)
            .nearest()
            .first()
        if (!ectofuntus.valid()) {
            logger.warning("Unable to locate Ectofuntus.")
            return false
        }
        val startBonemeal = Inventory.stream().name(DRAGON_BONEMEAL).count()
        val startSlime = Inventory.stream().name(BUCKET_OF_SLIME).count()
        if (!ectofuntus.click()) {
            return false
        }
        val consumed = Condition.wait({
            Inventory.stream().name(DRAGON_BONEMEAL).count() < startBonemeal ||
                Inventory.stream().name(BUCKET_OF_SLIME).count() < startSlime
        }, 60, 8)
        if (!consumed) {
            logger.warning("Worship interaction timed out.")
        }
        return consumed
    }

    private fun collectEctoTokens() {
        val startTokens = Inventory.stream().name(ECTO_TOKEN).count()
        var ghost = Npcs.stream().name(GHOST_DISCIPLE).nearest().first()
        if (!ghost.valid()) {
            logger.warning("Ghost disciple not found for token turn-in.")
            return
        }
        if (Players.local().tile().distanceTo(ghost.tile()) > 4) {
            Movement.walkTo(ghost.tile())
            Condition.wait({
                Players.local().tile().distanceTo(ghost.tile()) <= 4
            }, 400, 15)
        }
        if (!ghost.inViewport()) {
            Movement.step(ghost.tile())
            Condition.wait({ ghost.inViewport() }, 200, 10)
        }
        ghost = Npcs.stream().name(GHOST_DISCIPLE).nearest().first()
        if (!ghost.valid() || !ghost.interact("Talk-to")) {
            return
        }
        if (!Condition.wait({ Chat.canContinue() }, 200, 20)) {
            logger.warning("No dialogue appeared when talking to ghost disciple.")
            return
        }
        var clicks = 0
        while (clicks < 6) {
            if (Chat.canContinue()) {
                Chat.clickContinue()
                Condition.sleep(Random.nextInt(80, 151))
                clicks++
            } else if (!Condition.wait({ Chat.canContinue() }, 200, 5)) {
                break
            }
        }
        Condition.wait({
            Inventory.stream().name(ECTO_TOKEN).count() > startTokens
        }, 400, 15)
    }

    private fun ensureNearEctofuntus(tile: Tile) {
        if (isNear(tile)) return
        val farFromEcto = Players.local().tile().distanceTo(tile) > 20
        val teleported = if (farFromEcto) teleportToEctofuntus(tile) else false
        if (!teleported) stepTo(tile)
    }

    private fun teleportToEctofuntus(tile: Tile): Boolean {
        val ectophial = Inventory.stream().name(ECTOPHIAL).first()
        if (!ectophial.valid()) {
            logger.warning("No Ectophial available to teleport for worship.")
            return false
        }
        logger.info("Emptying Ectophial to reach worship area.")
        if (!ectophial.interact("Empty")) {
            return false
        }
        WaitUtils.waitForLoad()
        Condition.wait({ Players.local().animation() == -1 }, 200, 10)
        val teleported = Condition.wait({
            Players.local().tile().distanceTo(tile) <= 12
        }, 400, 25)
        if (!teleported) {
            logger.warning("Ectophial teleport failed to reach worship area.")
        }
        return teleported
    }

    private fun stepTo(tile: Tile) {
        if (isNear(tile)) return
        Movement.step(tile)
        Condition.wait({ isNear(tile) }, 400, 25)
    }

    private fun isNear(tile: Tile, distance: Int = 6): Boolean {
        return Players.local().tile().distanceTo(tile) <= distance
    }

    override fun toString(): String = "Worshipping the Ectofuntus"
}

