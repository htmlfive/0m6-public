package org.powbot.om6.herbrun.services

import org.powbot.api.Tile
import org.powbot.om6.herbrun.config.CompostType
import org.powbot.om6.herbrun.config.HerbPatch
import org.powbot.om6.herbrun.config.HerbRunConfig
import org.powbot.om6.herbrun.config.HerbType

class HerbRunConfigFactory(private val logWarn: (String) -> Unit) {

    data class Keys(
        val herbType: String,
        val fallbackSeed1: String,
        val fallbackSeed2: String,
        val fallbackSeed3: String,
        val fallbackSeed4: String,
        val fallbackSeed5: String,
        val compostType: String,
        val runBigCompostBin: String,
        val loopRuns: String,
        val noteHerbs: String,
        val pickpocketBetweenRuns: String,
        val enableLimpwurtFarming: String,
        val startWithPickpocket: String,
        val pickpocketWineWithdraw: String,
        val pickpocketHealDeficit: String,
        val masterFarmerTile: String
    )

    fun build(
        keys: Keys,
        getString: (String) -> String?,
        getBoolean: (String) -> Boolean?,
        getInt: (String) -> Int?,
        defaultMasterFarmerTile: Tile,
        defaultPickpocketWineWithdrawCount: Int,
        defaultPickpocketHealDeficit: Int
    ): HerbRunConfig {
        val herbChoice = (getString(keys.herbType) ?: "Ranarr").trim()
        val herbType = HerbType.fromOption(herbChoice)
        val fallbackSeedTypes = listOf(
            parseFallbackSeedType(getString(keys.fallbackSeed1)),
            parseFallbackSeedType(getString(keys.fallbackSeed2)),
            parseFallbackSeedType(getString(keys.fallbackSeed3)),
            parseFallbackSeedType(getString(keys.fallbackSeed4)),
            parseFallbackSeedType(getString(keys.fallbackSeed5))
        )
            .filterNotNull()
            .filter { it != herbType }
            .distinct()

        val compostChoice = (getString(keys.compostType) ?: "Ultracompost").trim()
        val compostType = CompostType.values()
            .firstOrNull { it.displayName.equals(compostChoice, ignoreCase = true) }

        val seedName = herbType.seedName
        val fallbackSeedNames = fallbackSeedTypes.map { it.seedName }
        val compostName = compostType?.itemName
        val enabledPatches = HerbPatch.defaultOrder.filter { getBoolean(it.optionName) ?: true }

        val runBigCompostBin = getBoolean(keys.runBigCompostBin) ?: false
        val loopRuns = getBoolean(keys.loopRuns) ?: false
        val noteHerbs = getBoolean(keys.noteHerbs) ?: true
        val pickpocketBetweenRuns = getBoolean(keys.pickpocketBetweenRuns) ?: false
        val enableLimpwurtFarming = getBoolean(keys.enableLimpwurtFarming) ?: false
        val startWithPickpocket = getBoolean(keys.startWithPickpocket) ?: false
        val pickpocketWineWithdrawAmount = (getInt(keys.pickpocketWineWithdraw) ?: defaultPickpocketWineWithdrawCount).coerceAtLeast(1)
        val pickpocketHealHpDeficit = (getInt(keys.pickpocketHealDeficit) ?: defaultPickpocketHealDeficit).coerceAtLeast(0)
        val pickpocketMasterFarmerTile = parseTileOption(
            getString(keys.masterFarmerTile),
            defaultMasterFarmerTile,
            keys.masterFarmerTile
        )

        return HerbRunConfig(
            herbType = herbType,
            seedItemName = seedName,
            fallbackSeedItemNames = fallbackSeedNames,
            compostItemName = compostName,
            inventoryItems = emptyList(),
            enabledPatches = enabledPatches,
            runBigCompostBin = runBigCompostBin,
            loopRuns = loopRuns,
            noteHarvest = noteHerbs,
            pickpocketBetweenRuns = pickpocketBetweenRuns,
            enableLimpwurtFarming = enableLimpwurtFarming,
            startWithPickpocket = startWithPickpocket,
            pickpocketWineWithdrawAmount = pickpocketWineWithdrawAmount,
            pickpocketHealHpDeficit = pickpocketHealHpDeficit,
            pickpocketMasterFarmerTile = pickpocketMasterFarmerTile
        )
    }

    private fun parseFallbackSeedType(raw: String?): HerbType? {
        val choice = raw?.trim().orEmpty()
        if (choice.isEmpty() || choice.equals("None", ignoreCase = true)) {
            return null
        }
        return HerbType.fromOption(choice)
    }

    private fun parseTileOption(raw: String?, fallback: Tile, optionName: String): Tile {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return fallback
        }
        val parts = trimmed.split(",")
        if (parts.size != 3) {
            logWarn("Invalid $optionName value \"$trimmed\", using fallback.")
            return fallback
        }
        val x = parts[0].trim().toIntOrNull()
        val y = parts[1].trim().toIntOrNull()
        val plane = parts[2].trim().toIntOrNull()
        if (x == null || y == null || plane == null) {
            logWarn("Invalid $optionName value \"$trimmed\", using fallback.")
            return fallback
        }
        return Tile(x, y, plane)
    }
}
