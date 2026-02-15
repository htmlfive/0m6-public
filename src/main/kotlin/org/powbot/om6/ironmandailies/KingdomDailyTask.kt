package org.powbot.om6.ironmandailies

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Npcs
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Widgets
import org.powbot.om6.api.ScriptLogging
import org.powbot.om6.api.WorldHopUtils
import org.slf4j.Logger

class KingdomDailyTask(
    private val logger: Logger,
    private val hopBeforeContinue: Boolean,
    debugStartStep: String
) : DailyTask {

    private enum class Stage {
        PREPARE_SUPPLIES,
        WALK_TO_PATCH,
        RAKE_FLAX,
        RAKE_HERBS,
        DROP_WEEDS,
        WAIT_FOR_CONTINUE_OR_HOP,
        HOP_BEFORE_CONTINUE,
        WALK_TO_GHRIM,
        COLLECT_GHRIM_FIRST,
        SELECT_COLLECT_CHAT_OPTION,
        CLOSE_BEFORE_SECOND_GHRIM,
        COLLECT_GHRIM_SECOND,
        CLICK_CHAT_CONTINUE,
        CLICK_DEPOSIT_WIDGET,
        FINAL_CHAT_CONTINUE,
        SEND_DEPOSIT_INPUT,
        CONTINUE_BEFORE_CLOSE,
        SECOND_CONTINUE_BEFORE_CLOSE,
        CLICK_CLOSE_WIDGET,
        COMPLETE
    }

    private companion object {
        val PATCH_TILE = Tile(2527, 3851, 0)
        val GHRIM_TILE = Tile(2500, 3857, 1)
        const val WALK_DISTANCE = 4.0
        const val BANK_WALK_DISTANCE = 8.0
        const val SEARCH_RADIUS = 20.0
        const val REQUIRED_COINS = 75_000
    }

    override val name: String = "Kingdom"
    override val subStage: String
        get() = currentTask

    private var stage = resolveStartStage(debugStartStep)
    private var currentTask = "Starting"
    private var failureReason: String? = null
    private var postDepositContinueClicks = 0

    init {
        ScriptLogging.info(logger, "Kingdom task start stage: ${stage.name}")
    }

    override fun poll(): DailyTaskResult {
        val reason = failureReason
        if (reason != null) {
            return DailyTaskResult.Failed(reason)
        }

        when (stage) {
            Stage.PREPARE_SUPPLIES -> prepareSupplies()
            Stage.WALK_TO_PATCH -> walkToPatch()
            Stage.RAKE_FLAX -> rakeFlax()
            Stage.RAKE_HERBS -> rakeHerbs()
            Stage.DROP_WEEDS -> dropWeedsStage()
            Stage.WAIT_FOR_CONTINUE_OR_HOP -> waitForContinueOrHop()
            Stage.HOP_BEFORE_CONTINUE -> hopBeforeContinue()
            Stage.WALK_TO_GHRIM -> walkToGhrim()
            Stage.COLLECT_GHRIM_FIRST -> collectGhrimFirst()
            Stage.SELECT_COLLECT_CHAT_OPTION -> selectCollectChatOption()
            Stage.CLOSE_BEFORE_SECOND_GHRIM -> closeBeforeSecondGhrim()
            Stage.COLLECT_GHRIM_SECOND -> collectGhrimSecond()
            Stage.CLICK_CHAT_CONTINUE -> clickChatContinue()
            Stage.CLICK_DEPOSIT_WIDGET -> clickDepositWidget()
            Stage.FINAL_CHAT_CONTINUE -> finalChatContinue()
            Stage.SEND_DEPOSIT_INPUT -> sendDepositInput()
            Stage.CONTINUE_BEFORE_CLOSE -> continueBeforeClose()
            Stage.SECOND_CONTINUE_BEFORE_CLOSE -> secondContinueBeforeClose()
            Stage.CLICK_CLOSE_WIDGET -> clickCloseWidget()
            Stage.COMPLETE -> return DailyTaskResult.Completed
        }

        return DailyTaskResult.InProgress
    }

    private fun prepareSupplies() {
        currentTask = "Preparing supplies"
        if (hasRequiredInventory()) {
            stage = Stage.WALK_TO_PATCH
            return
        }

        if (!walkToClosestBank()) {
            ScriptLogging.warn(logger, "Failed to walk to nearest bank. Retrying.")
            return
        }

        if (!Bank.opened() && !Bank.open()) {
            ScriptLogging.warn(logger, "Could not open bank at nearest location. Retrying.")
            return
        }

        if (!Condition.wait({ Bank.opened() }, 120, 20)) {
            return
        }

        Bank.depositInventory()
        Condition.wait({ Inventory.stream().isEmpty() }, 120, 20)

        val withdrewCloak = withdrawItemContaining("Ardougne cloak", 1)
        val withdrewStaff = withdrawItemContaining("Dramen staff", 1)
        val withdrewCoins = Bank.withdraw("Coins", REQUIRED_COINS)
        val withdrewRake = Bank.withdraw("Rake", 1)

        Condition.wait({ hasRequiredInventory() }, 120, 30)
        Bank.close()
        Condition.wait({ !Bank.opened() }, 120, 10)

        if (!withdrewCloak || !withdrewStaff || !withdrewCoins || !withdrewRake || !hasRequiredInventory()) {
            failureReason = "Missing required Kingdom supplies in bank (cloak/staff/coins/rake)."
            return
        }

        stage = Stage.WALK_TO_PATCH
    }

    private fun walkToPatch() {
        currentTask = "Webwalking to flax/herb plots"
        if (Players.local().tile().distanceTo(PATCH_TILE) <= WALK_DISTANCE) {
            stage = Stage.RAKE_FLAX
            return
        }

        webWalkTo(PATCH_TILE, WALK_DISTANCE)
    }

    private fun rakeFlax() {
        currentTask = "Raking flax"
        val flax = Objects.stream()
            .name("Flax")
            .within(PATCH_TILE, SEARCH_RADIUS)
            .nearest()
            .firstOrNull()

        if (flax == null || !flax.valid()) {
            return
        }

        if (flax.interact("Rake")) {
            ScriptLogging.action(logger, "Rake -> Flax")
            waitForRakeAnimationCycle()
            stage = Stage.RAKE_HERBS
        }
    }

    private fun rakeHerbs() {
        currentTask = "Raking herbs"
        val herbs = getHerbsObject()
        if (herbs == null || !herbs.valid()) {
            return
        }

        if (!herbsHasRakeAction(herbs)) {
            stage = Stage.WAIT_FOR_CONTINUE_OR_HOP
            return
        }

        if (herbs.interact("Rake")) {
            ScriptLogging.action(logger, "Rake -> Herbs")
            waitForRakeAnimationCycle()
            stage = Stage.DROP_WEEDS
        }
    }

    private fun dropWeedsStage() {
        currentTask = "Dropping weeds"
        dropAllWeeds()
        stage = Stage.WAIT_FOR_CONTINUE_OR_HOP
    }

    private fun dropAllWeeds() {
        var weeds = Inventory.stream().name("Weeds").firstOrNull()
        while (weeds != null && weeds.valid()) {
            val before = Inventory.stream().name("Weeds").count(true).toInt()
            if (!weeds.interact("Drop")) {
                break
            }
            Condition.wait({ Inventory.stream().name("Weeds").count(true).toInt() < before }, 80, 20)
            weeds = Inventory.stream().name("Weeds").firstOrNull()
        }
    }

    private fun waitForContinueOrHop() {
        currentTask = "Waiting for continue prompt"
        if (!Chat.canContinue()) {
            stage = Stage.RAKE_FLAX
            return
        }

        if (hopBeforeContinue) {
            stage = Stage.HOP_BEFORE_CONTINUE
            return
        }

        if (Chat.clickContinue()) {
            Condition.wait({ !Chat.canContinue() }, 120, 10)
            stage = Stage.WALK_TO_GHRIM
        }
    }

    private fun hopBeforeContinue() {
        currentTask = "Hopping world before continue"
        val hopped = WorldHopUtils.hopToNextAvailableWorld()
        if (hopped) {
            stage = Stage.WALK_TO_GHRIM
            return
        }
        Condition.sleep(400)
    }

    private fun walkToGhrim() {
        currentTask = "Webwalking to Advisor Ghrim"
        if (Players.local().tile().distanceTo(GHRIM_TILE) <= WALK_DISTANCE) {
            stage = Stage.COLLECT_GHRIM_FIRST
            return
        }

        webWalkTo(GHRIM_TILE, WALK_DISTANCE)
    }

    private fun collectGhrimFirst() {
        currentTask = "Collect from Advisor Ghrim (1)"
        val ghrim = getAdvisorGhrim() ?: return
        if (ghrim.interact("Collect")) {
            Condition.wait({ Chat.chatting() || Chat.canContinue() }, 120, 20)
            stage = Stage.SELECT_COLLECT_CHAT_OPTION
        }
    }

    private fun selectCollectChatOption() {
        currentTask = "Selecting collect option"
        val collectOption = Chat.stream().textContains("Collect").firstOrNull()
        if (collectOption != null && collectOption.select()) {
            val collectConfirmShown = Condition.wait(
                {
                    val collectConfirm = Widgets.component(616, 5)
                    collectConfirm.valid() && collectConfirm.visible()
                },
                60,
                30
            )
            if (collectConfirmShown) {
                val collectConfirm = Widgets.component(616, 5)
                if (collectConfirm.valid() && collectConfirm.visible()) {
                    collectConfirm.click()
                }
            }
            Condition.wait({ !Chat.chatting() || Chat.canContinue() }, 120, 20)
            stage = Stage.CLOSE_BEFORE_SECOND_GHRIM
            return
        }

        if (Chat.canContinue() && Chat.clickContinue()) {
            Condition.wait({ !Chat.canContinue() }, 120, 10)
            stage = Stage.CLOSE_BEFORE_SECOND_GHRIM
        }
    }

    private fun closeBeforeSecondGhrim() {
        currentTask = "Closing before second collect"
        val closeComponent = Widgets.component(391, 161)
        if (closeComponent.valid() && closeComponent.visible()) {
            if (closeComponent.interact("Close") || closeComponent.click()) {
                stage = Stage.COLLECT_GHRIM_SECOND
            }
            return
        }

        stage = Stage.COLLECT_GHRIM_SECOND
    }

    private fun collectGhrimSecond() {
        currentTask = "Collect from Advisor Ghrim (2)"
        val ghrim = getAdvisorGhrim() ?: return
        if (ghrim.interact("Collect")) {
            Condition.wait({ Chat.canContinue() }, 120, 20)
            stage = Stage.CLICK_CHAT_CONTINUE
        }
    }

    private fun clickChatContinue() {
        currentTask = "Continuing Advisor Ghrim chat"
        if (Chat.canContinue() && Chat.clickContinue()) {
            Condition.wait({ !Chat.canContinue() }, 120, 10)
            stage = Stage.CLICK_DEPOSIT_WIDGET
        }
    }

    private fun clickDepositWidget() {
        currentTask = "Clicking deposit widget"
        val depositComponent = Components.stream().textContains("Deposit").firstOrNull()
        if (depositComponent != null && depositComponent.visible() && depositComponent.click()) {
            postDepositContinueClicks = 0
            stage = Stage.FINAL_CHAT_CONTINUE
        }
    }

    private fun finalChatContinue() {
        currentTask = "Final continue before amount input"
        if (Chat.pendingInput()) {
            stage = Stage.SEND_DEPOSIT_INPUT
            return
        }

        if (Chat.canContinue() && postDepositContinueClicks < 2 && Chat.clickContinue()) {
            postDepositContinueClicks++
            Condition.wait({ !Chat.canContinue() || Chat.pendingInput() }, 120, 10)
            if (Chat.pendingInput()) {
                stage = Stage.SEND_DEPOSIT_INPUT
            }
        }
    }

    private fun sendDepositInput() {
        currentTask = "Sending 75k input"
        if (!Chat.pendingInput()) {
            return
        }

        Condition.sleep(1200)
        if (Chat.sendInput(REQUIRED_COINS)) {
            stage = Stage.CONTINUE_BEFORE_CLOSE
        }
    }

    private fun continueBeforeClose() {
        currentTask = "Continue click before close"
        if (!Chat.canContinue()) {
            return
        }

        if (Chat.clickContinue()) {
            Condition.wait({ !Chat.canContinue() }, 120, 10)
            stage = Stage.SECOND_CONTINUE_BEFORE_CLOSE
        }
    }

    private fun secondContinueBeforeClose() {
        currentTask = "Second continue click before close"
        if (Chat.canContinue()) {
            if (Chat.clickContinue()) {
                Condition.wait({ !Chat.canContinue() }, 120, 10)
                stage = Stage.CLICK_CLOSE_WIDGET
            }
            return
        }

        stage = Stage.CLICK_CLOSE_WIDGET
    }

    private fun clickCloseWidget() {
        currentTask = "Clicking close widget 391,161"
        if (Chat.canContinue()) {
            if (Chat.clickContinue()) {
                Condition.wait({ !Chat.canContinue() }, 120, 10)
            }
        }

        val closeComponent = Widgets.component(391, 161)
        if (!closeComponent.valid() || !closeComponent.visible()) {
            stage = Stage.COMPLETE
            return
        }

        if (closeComponent.interact("Close") || closeComponent.click()) {
            Condition.wait(
                {
                    val widget = Widgets.component(391, 161)
                    !widget.valid() || !widget.visible()
                },
                120,
                10
            )
        }
    }

    private fun waitForRakeAnimationCycle() {
        Condition.wait({ Players.local().animation() != -1 }, 100, 30)
        Condition.wait({ Players.local().animation() == -1 }, 100, 120)
    }

    private fun walkToClosestBank(): Boolean {
        val nearestBankTile = Bank.nearest().tile()
        if (!nearestBankTile.valid()) {
            return false
        }

        if (Players.local().tile().distanceTo(nearestBankTile) <= BANK_WALK_DISTANCE) {
            return true
        }

        webWalkTo(nearestBankTile, BANK_WALK_DISTANCE)
        return Players.local().tile().distanceTo(nearestBankTile) <= BANK_WALK_DISTANCE
    }

    private fun webWalkTo(target: Tile, distance: Double): Boolean {
        if (Players.local().tile().distanceTo(target) <= distance) {
            return true
        }

        Movement.moveTo(target)

        return Condition.wait({ Players.local().tile().distanceTo(target) <= distance }, 200, 40)
    }

    private fun withdrawItemContaining(namePart: String, amount: Int): Boolean {
        val bankItem = Bank.stream().nameContains(namePart).firstOrNull() ?: return false
        if (!bankItem.valid()) {
            return false
        }
        return Bank.withdraw(bankItem.id(), amount)
    }

    private fun hasRequiredInventory(): Boolean {
        val hasRake = Inventory.stream().name("Rake").isNotEmpty()
        val hasCloak = Inventory.stream().nameContains("Ardougne cloak").isNotEmpty()
        val hasStaff = Inventory.stream().nameContains("Dramen staff").isNotEmpty()
        val coins = Inventory.stream().name("Coins").firstOrNull()?.stackSize() ?: 0
        return hasRake && hasCloak && hasStaff && coins >= REQUIRED_COINS
    }

    private fun getAdvisorGhrim() = Npcs.stream()
        .name("Advisor Ghrim")
        .nearest()
        .firstOrNull()

    private fun getHerbsObject() = Objects.stream()
        .name("Herbs")
        .within(PATCH_TILE, SEARCH_RADIUS)
        .nearest()
        .firstOrNull()

    private fun herbsHasRakeAction(herbsObject: org.powbot.api.rt4.GameObject?): Boolean {
        if (herbsObject == null || !herbsObject.valid()) {
            return false
        }
        return herbsObject.actions().any { it.equals("Rake", ignoreCase = true) }
    }

    private fun resolveStartStage(raw: String): Stage {
        val value = raw.trim()
        if (value.isBlank()) {
            return Stage.PREPARE_SUPPLIES
        }

        val numericPrefix = value.takeWhile { it.isDigit() }
        val asIndex = if (numericPrefix.isNotEmpty()) numericPrefix.toIntOrNull() else value.toIntOrNull()
        if (asIndex != null) {
            val idx = asIndex - 1
            if (idx in Stage.entries.indices) {
                return Stage.entries[idx]
            }
            ScriptLogging.warn(logger, "Invalid Kingdom Start Step index '$value'. Using PREPARE_SUPPLIES.")
            return Stage.PREPARE_SUPPLIES
        }

        val stageText = value.substringAfter("-", value).trim()
        val byName = Stage.entries.firstOrNull { it.name.equals(stageText, ignoreCase = true) }
        if (byName != null) {
            return byName
        }

        ScriptLogging.warn(logger, "Invalid Kingdom Start Step value '$value'. Using PREPARE_SUPPLIES.")
        return Stage.PREPARE_SUPPLIES
    }
}
