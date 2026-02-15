package org.powbot.om6.libationprayer.client

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Objects
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.bank.Quantity
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.om6.api.ScriptLogging
import org.powbot.om6.libationprayer.domain.model.LibationConfig
import org.powbot.om6.libationprayer.domain.model.LibationStateSnapshot
import org.powbot.om6.libationprayer.util.LibationConstants

class LibationGameClient(
    private val script: AbstractScript,
    private val config: LibationConfig
) {
    private val blessedWineNames = arrayOf(
        "Jug of blessed wine",
        "Jug of blessed sunfire wine"
    )

    fun stateSnapshot(needsInitialFullRecharge: Boolean, awaitingBowlFill: Boolean): LibationStateSnapshot {
        return LibationStateSnapshot(
            boneShardCount = boneShardCount(),
            hasUnblessedWine = hasUnblessedWine(),
            hasBlessedSacrificeJugs = hasBlessedWine(),
            prayerPoints = Prayer.prayerPoints(),
            prayerMax = Skills.realLevel(Skill.Prayer),
            needsInitialFullRecharge = needsInitialFullRecharge,
            awaitingBowlFill = awaitingBowlFill,
            bowlHasSacrificeAction = libationBowlHasAction("Sacrifice"),
            bowlHasFillAction = libationBowlHasAction("Fill")
        )
    }

    fun isNear(tile: Tile, distance: Int): Boolean {
        return Players.local().tile().distanceTo(tile) <= distance
    }

    fun walkTo(tile: Tile, distance: Int) {
        if (isNear(tile, distance)) return
        Movement.walkTo(tile)
        Condition.wait({ isNear(tile, distance) }, 250, 28)
    }

    fun openBank(): Boolean {
        if (Bank.opened()) return true
        if (Bank.open()) {
            Condition.wait({ Bank.opened() }, 200, 20)
        }
        return Bank.opened()
    }

    fun closeBank() {
        Bank.close()
        Condition.wait({ !Bank.opened() }, 150, 12)
    }

    fun depositAllJugs() {
        val jugNames = Inventory.stream()
            .filtered { it.name().contains(LibationConstants.JUG_NAME_PART, ignoreCase = true) }
            .toList()
            .map { it.name() }
            .distinct()

        for (name in jugNames) {
            val count = Inventory.stream().name(name).count(true).toInt()
            if (count <= 0) continue
            Bank.deposit(name, count)
            Condition.wait({ Inventory.stream().name(name).isEmpty() }, 120, 12)
        }
    }

    fun withdrawAllWine(): Boolean {
        if (Bank.stream().name(config.wineType).isEmpty()) return false
        val withdrew = Bank.withdraw().item(config.wineType, Quantity.of(Bank.Amount.ALL.value)).submit()
        if (!withdrew) return false
        return Condition.wait({ hasUnblessedWine() }, 120, 20)
    }

    fun withdrawAllBoneShardsIfMissing() {
        if (boneShardCount() > 0) return
        val bankBoneShards = Bank.stream().nameContains(LibationConstants.BONE_SHARD_NAME_PART).firstOrNull()
        if (bankBoneShards == null || !bankBoneShards.valid()) return
        Bank.withdraw().item(bankBoneShards.name(), Quantity.of(Bank.Amount.ALL.value)).submit()
        Condition.wait({ boneShardCount() > 0 }, 120, 20)
    }

    fun blessAtExposedAltar(): Boolean {
        val altar = Objects.stream()
            .name(LibationConstants.EXPOSED_ALTAR)
            .action("Bless")
            .nearest()
            .firstOrNull()
        if (altar == null || !altar.valid()) return false
        if (!altar.interact("Bless")) return false
        return Condition.wait({ hasBlessedWine() || !hasUnblessedWine() }, 150, 24)
    }

    fun baskAtShrine(): Boolean {
        val shrine = Objects.stream()
            .name(LibationConstants.SHRINE_OF_RALOS)
            .action("Bask")
            .nearest()
            .firstOrNull()
        if (shrine == null || !shrine.valid()) return false
        val before = Prayer.prayerPoints()
        if (!shrine.interact("Bask")) return false
        return Condition.wait({ Prayer.prayerPoints() > before || Prayer.prayerPoints() >= Skills.realLevel(Skill.Prayer) }, 150, 20)
    }

    fun clickLibationBowl() {
        val bowl = Objects.stream()
            .name(LibationConstants.LIBATION_BOWL)
            .nearest()
            .firstOrNull()
        if (bowl == null || !bowl.valid()) return
        bowl.click()
    }

    fun libationBowlHasAction(action: String): Boolean {
        return Objects.stream()
            .name(LibationConstants.LIBATION_BOWL)
            .action(action)
            .nearest()
            .firstOrNull()
            ?.valid() == true
    }

    fun boneShardCount(): Int {
        return Inventory.stream()
            .nameContains(LibationConstants.BONE_SHARD_NAME_PART)
            .count(true)
            .toInt()
    }

    fun unblessedJugCount(): Int = Inventory.stream().name(config.wineType).count(true).toInt()

    fun blessedJugCount(): Int {
        return Inventory.stream()
            .name(*blessedWineNames)
            .count(true)
            .toInt()
    }

    fun hasUnblessedWine(): Boolean = Inventory.stream().name(config.wineType).isNotEmpty()

    fun hasBlessedWine(): Boolean {
        return Inventory.stream()
            .name(*blessedWineNames)
            .isNotEmpty()
    }

    fun stopScript(reason: String) {
        ScriptLogging.stopWithNotification(script, reason)
    }
}
