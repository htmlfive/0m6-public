package org.powbot.om6.api

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.World
import org.powbot.api.rt4.Worlds

/**
 * Reusable blocking world hop service.
 *
 * Integration in other scripts:
 * 1) Create once in your script class:
 *    `private val hopService = BlockingWorldHopService(onInfo = { logger.info(it) }, onWarn = { logger.warn(it) })`
 * 2) Call when you want to hop:
 *    `val hopped = hopService.hopToPreferredWorldWithRetries("My hop reason")`
 * 3) Handle false return (all retries exhausted) in your script flow.
 */
class BlockingWorldHopService(
    private val onInfo: (String) -> Unit = {},
    private val onWarn: (String) -> Unit = {},
    private val worldType: World.Type = World.Type.MEMBERS,
    private val minPopulation: Int = 0,
    private val maxPopulation: Int = 1000,
    private val specialty: World.Specialty = World.Specialty.NONE,
    private val servers: Set<World.Server> = setOf(
        World.Server.NORTH_AMERICA,
        World.Server.UNITED_KINGDOM
    ),
    private val maxAttempts: Int = 10,
    private val retryDelayMinMs: Int = 2_000,
    private val retryDelayMaxMs: Int = 3_000,
    private val hopCheckDelayMs: Int = 500,
    private val hopCheckAttempts: Int = 20
) {
    /**
     * Blocking hop that always retries until success or max attempts.
     */
    fun hopToPreferredWorldWithRetries(reason: String): Boolean {
        var attempt = 0
        while (attempt < maxAttempts) {
            attempt++
            val worldBefore = Worlds.current().number
            val target = WorldHopUtils.findRandomWorld(
                worldType = worldType,
                minPopulation = minPopulation,
                maxPopulation = maxPopulation,
                specialty = specialty,
                servers = servers
            )

            if (target == null) {
                onWarn("$reason: no suitable world found (attempt $attempt/$maxAttempts).")
            } else if (target.number == worldBefore) {
                onInfo("$reason: selected current world ($worldBefore), retrying (attempt $attempt/$maxAttempts).")
            } else {
                if (!Worlds.open()) {
                    onWarn("$reason: failed to open world switcher (attempt $attempt/$maxAttempts).")
                }

                val clicked = target.hop()
                if (!clicked) {
                    onWarn("$reason: hop click failed for world ${target.number} (attempt $attempt/$maxAttempts).")
                } else {
                    val hopped = Condition.wait(
                        { Worlds.current().number != worldBefore && Worlds.current().number > 0 },
                        hopCheckDelayMs,
                        hopCheckAttempts
                    )
                    if (hopped) {
                        onInfo("$reason: hopped from $worldBefore to ${Worlds.current().number}.")
                        return true
                    }
                    onWarn("$reason: world did not change after hop click (attempt $attempt/$maxAttempts).")
                }
            }

            if (attempt < maxAttempts) {
                Condition.sleep(Random.nextInt(retryDelayMinMs, retryDelayMaxMs + 1))
            }
        }

        onWarn("$reason: failed to hop after $maxAttempts attempts.")
        return false
    }
}
