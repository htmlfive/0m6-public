package org.powbot.community.pestcontrol.task

import org.powbot.api.Condition
import org.powbot.api.rt4.Prayer
import org.powbot.community.pestcontrol.PestControl
import org.powbot.community.pestcontrol.helpers.voidKnightHealth
import kotlin.random.Random

class ActivatePrayers(val script: PestControl): Task {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass.simpleName)

    override fun name(): String {
        return "Activating Prayers"
    }

    override fun valid(): Boolean {
        if (!voidKnightHealth().visible()) {
            return false
        }

        val needsActivation =
            (script.overheadPrayer != null && !Prayer.prayerActive(script.overheadPrayer!!.prayer)) ||
            (script.offensivePrayer != null && !Prayer.prayerActive(script.offensivePrayer!!.prayer))

        return needsActivation
    }

    override fun run() {
        val activated = mutableListOf<String>()

        script.overheadPrayer?.let { prayer ->
            if (!Prayer.prayerActive(prayer.prayer)) {
                if (Prayer.prayer(prayer.prayer, true)) {
                    logger.info("Activated overhead prayer: ${prayer.prayerName}")
                    activated.add(prayer.prayerName)
                    Condition.sleep(Random.nextInt(300,500))
                    Condition.wait { Prayer.prayerActive(prayer.prayer) }
                }
            }
        }

        script.offensivePrayer?.let { prayer ->
            if (!Prayer.prayerActive(prayer.prayer)) {
                if (Prayer.prayer(prayer.prayer, true)) {
                    logger.info("Activated offensive prayer: ${prayer.prayerName}")
                    activated.add(prayer.prayerName)
                    Condition.sleep(Random.nextInt(300,500))
                    Condition.wait { Prayer.prayerActive(prayer.prayer) }
                }
            }
        }

        if (activated.isNotEmpty()) {
            logger.info("Prayers activated: ${activated.joinToString(", ")}")
        }
    }
}
