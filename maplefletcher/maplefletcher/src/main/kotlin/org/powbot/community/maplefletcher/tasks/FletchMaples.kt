package org.powbot.community.maplefletcher.tasks

import org.powbot.api.Condition
import org.powbot.api.rt4.Component
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Inventory
import org.powbot.community.maplefletcher.MapleFletcherConstants
import org.powbot.community.mixology.structure.ScriptRecord
import org.powbot.community.mixology.structure.TreeTask
import java.util.logging.Logger

class FletchMaples(private val record: ScriptRecord) : TreeTask(true) {
    private val logger = Logger.getLogger(FletchMaples::class.java.name)

    override fun execute(): Int {
        if (Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).isEmpty()) {
            logger.fine("No maple logs to fletch.")
            return super.execute()
        }
        if (!ensureFletchingInterface()) {
            logger.warning("Unable to open fletching interface.")
            return super.execute()
        }
        val option = findMapleOption()
        if (option == null) {
            logger.warning("Unable to locate Maple longbow (u) option.")
            return super.execute()
        }
        val bowsBefore = Inventory.stream().name(MapleFletcherConstants.MAPLE_LONGBOW_NAME).count(true).toInt()
        if (option.interact("Make All") || option.click()) {
            val completed = Condition.wait(
                { Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).isEmpty() },
                500,
                40
            )
            if (!completed) {
                logger.fine("Fletching cycle interrupted before logs were consumed.")
            }
            val bowsAfter = Inventory.stream().name(MapleFletcherConstants.MAPLE_LONGBOW_NAME).count(true).toInt()
            if (bowsAfter > bowsBefore) {
                val crafted = bowsAfter - bowsBefore
                val total = record.getNotedValue("bows_made")
                record.setNotedValue("bows_made", total + crafted)
            }
        }
        return super.execute()
    }

    private fun ensureFletchingInterface(): Boolean {
        if (findMapleOption() != null) return true

        val knife = Inventory.stream().name(MapleFletcherConstants.KNIFE_NAME).first()
        val log = Inventory.stream().name(MapleFletcherConstants.MAPLE_LOG_NAME).first()
        if (!knife.valid() || !log.valid()) return false
        if (!knife.useOn(log)) return false
        Condition.wait({ findMapleOption() != null }, 250, 12)
        return findMapleOption() != null
    }

    private fun findMapleOption(): Component? {
        val option = Components.stream()
            .filtered {
                it.visible() &&
                    (it.name().contains(MapleFletcherConstants.MAPLE_LONGBOW_NAME, ignoreCase = true) ||
                        it.text().contains(MapleFletcherConstants.MAPLE_LONGBOW_NAME, ignoreCase = true))
            }
            .first()
        return if (option.valid()) option else null
    }

    override fun toString(): String = "Fletching maple logs"
}

