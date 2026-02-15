package org.powbot.om6.api.feroxpool

import org.powbot.api.Condition
import org.powbot.api.rt4.Players

object FeroxPoolWaits {
    private const val WAIT_INTERVAL_MS = 150
    private const val ANIMATION_START_ATTEMPTS = 20
    private const val ANIMATION_END_ATTEMPTS = 40

    const val POST_DRINK_SLEEP_MS = 1200

    fun waitForDrinkAnimationStart(): Boolean =
        Condition.wait({ Players.local().animation() != -1 }, WAIT_INTERVAL_MS, ANIMATION_START_ATTEMPTS)

    fun waitForDrinkAnimationEnd(): Boolean =
        Condition.wait({ Players.local().animation() == -1 }, WAIT_INTERVAL_MS, ANIMATION_END_ATTEMPTS)

    fun waitForDrinkCycleAndSettle(): Boolean {
        val started = waitForDrinkAnimationStart()
        val ended = waitForDrinkAnimationEnd()
        Condition.sleep(POST_DRINK_SLEEP_MS)
        return started && ended
    }
}
