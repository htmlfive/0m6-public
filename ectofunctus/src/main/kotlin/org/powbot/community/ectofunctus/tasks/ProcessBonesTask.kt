package org.powbot.community.ectofunctus.tasks

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.community.api.WaitUtils
import org.powbot.community.ectofunctus.EctofunctusConstants.BIN_NAME
import org.powbot.community.ectofunctus.EctofunctusConstants.DRAGON_BONEMEAL
import org.powbot.community.ectofunctus.EctofunctusConstants.DRAGON_BONES
import org.powbot.community.ectofunctus.EctofunctusConstants.ECTOPHIAL
import org.powbot.community.ectofunctus.EctofunctusConstants.EMPTY_ACTION
import org.powbot.community.ectofunctus.EctofunctusConstants.GRINDER_NAME
import org.powbot.community.ectofunctus.EctofunctusConstants.LOADER_NAME
import org.powbot.community.ectofunctus.EctofunctusConstants.WIND_ACTION
import org.powbot.community.mixology.structure.ScriptRecord
import org.powbot.community.mixology.structure.TreeTask
import java.util.logging.Logger

class ProcessBonesTask(private val record: ScriptRecord) : TreeTask(true) {
    private val logger = Logger.getLogger(ProcessBonesTask::class.java.name)
    private val windTile = Tile(3659, 3524, 1)

    override fun execute(): Int {
        val grinderTile = record.getNotedPosition("grinder")
        if (grinderTile == null) {
            logger.severe("Grinder tile not configured, stopping script.")
            record.controller.stop()
            return super.execute()
        }
        if (!ensureAtGrinder(grinderTile)) {
            return super.execute()
        }
        while (Inventory.stream().name(DRAGON_BONES).isNotEmpty()) {
            val bonemealBefore = Inventory.stream().name(DRAGON_BONEMEAL).count()
            if (!loadBone()) break
            if (!windAndEmpty(bonemealBefore)) break
        }
        return super.execute()
    }

    private fun ensureAtGrinder(grinderTile: Tile): Boolean {
        if (isAtTile(grinderTile, 2)) return true
        val nearEctoTile = record.getNotedPosition("ectofuntus")
        val farFromGrinder = Players.local().tile().distanceTo(grinderTile) > 20
        val nearEctofuntus = nearEctoTile?.let { Players.local().tile().distanceTo(it) <= 8 } ?: false
        if (farFromGrinder && !nearEctofuntus) {
            tryTeleportToEctofuntusBase()
        }
        walkToTile(grinderTile)
        return isAtTile(grinderTile, 2)
    }

    private fun walkToTile(tile: Tile, tolerance: Int = 2): Boolean {
        if (isAtTile(tile, tolerance)) return true
        Movement.walkTo(tile)
        return Condition.wait({ isAtTile(tile, tolerance) }, 400, 25)
    }

    private fun tryTeleportToEctofuntusBase(): Boolean {
        val baseTile = record.getNotedPosition("ectofuntus") ?: return false
        val ectophial = Inventory.stream().name(ECTOPHIAL).first()
        if (!ectophial.valid()) return false
        if (!ectophial.interact("Empty")) {
            return false
        }
        WaitUtils.waitForLoad()
        return Condition.wait({
            Players.local().animation() == -1 &&
                Players.local().tile().distanceTo(baseTile) <= 12
        }, 400, 25)
    }

    private fun isAtTile(tile: Tile, tolerance: Int = 0): Boolean {
        return if (tolerance <= 0) {
            Players.local().tile() == tile
        } else {
            Players.local().tile().distanceTo(tile) <= tolerance
        }
    }

    private fun loadBone(): Boolean {
        val bone = Inventory.stream().name(DRAGON_BONES).first()
        if (!bone.valid()) return false
        val loader = Objects.stream().name(LOADER_NAME).nearest().first()
        if (!loader.valid()) {
            logger.warning("Unable to find loader.")
            return false
        }
        val startingBones = Inventory.stream().name(DRAGON_BONES).count()
        if (bone.useOn(loader)) {
            val loaded = Condition.wait({
                Inventory.stream().name(DRAGON_BONES).count() < startingBones
            }, 400, 10)
            if (!loaded) {
                logger.warning("Timed out loading bone into grinder.")
            }
            return loaded
        }
        return false
    }

    private fun windAndEmpty(previousBonemeal: Long): Boolean {
        val grinder = Objects.stream().name(GRINDER_NAME).nearest().first()
        if (!grinder.valid()) {
            logger.warning("Missing bone grinder.")
            return false
        }
        if (!grinder.click()) {
            return false
        }
        val positioned = Condition.wait({
            Players.local().tile() == windTile
        }, 400, 15)
        if (!positioned) {
            logger.warning("Player did not move to wind tile $windTile after starting the grinder.")
            return false
        }
        val started = Condition.wait({ Players.local().animation() != -1 }, 300, 10)
        if (!started) {
            return false
        }
        val finished = Condition.wait({ Players.local().animation() == -1 }, 400, 15)
        if (!finished) {
            return false
        }
        if (!collectBonemeal()) {
            return false
        }
        val bonemealIncreased = Condition.wait({
            Inventory.stream().name(DRAGON_BONEMEAL).count() > previousBonemeal
        }, 400, 10)
        if (!bonemealIncreased) {
            logger.warning("Bonemeal count did not increase after emptying bin.")
            return false
        }
        return true
    }

    private fun collectBonemeal(): Boolean {
        val bin = Objects.stream().name(BIN_NAME).nearest().first()
        if (!bin.valid()) return false
        val startBonemeal = Inventory.stream().name(DRAGON_BONEMEAL).count()
        if (bin.interact(EMPTY_ACTION)) {
            val collected = Condition.wait({
                Inventory.stream().name(DRAGON_BONEMEAL).count() > startBonemeal
            }, 400, 10)
            if (!collected) {
                logger.warning("Failed to empty grinder bin.")
            }
            return collected
        }
        return false
    }

    override fun toString(): String = "Processing dragon bones"
}

