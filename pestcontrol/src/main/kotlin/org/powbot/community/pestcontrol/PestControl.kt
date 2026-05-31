package org.powbot.community.pestcontrol

import org.powbot.api.Condition
import org.powbot.api.EventFlows
import org.powbot.api.Notifications
import org.powbot.api.Random
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.script.paint.PaintFormatters
import org.powbot.api.script.paint.PaintFormatters.round
import org.powbot.mobile.script.ScriptManager
import org.powbot.community.pestcontrol.data.Activity
import org.powbot.community.pestcontrol.data.Boat
import org.powbot.community.pestcontrol.data.PestControlMap
import org.powbot.community.pestcontrol.data.PrayerType
import org.powbot.community.pestcontrol.helpers.Zeal
import org.powbot.community.pestcontrol.helpers.squire
import org.powbot.community.pestcontrol.helpers.voidKnightHealth
import org.powbot.community.pestcontrol.task.*

@ScriptManifest(
    name = "0m6 PestControl",
    description = "Plays the pest control minigame, start geared up",
    version = "1.0.1",
    scriptId = "5a74316f-e6c0-4c23-abad-4c9c89258a68",
    category = ScriptCategory.Minigame,
    markdownFileName = "pestcontrol.md"
)
@ScriptConfiguration.List(
    [
        ScriptConfiguration(
            name = "Boat Type",
            description = "Which level boat",
            defaultValue = "Hard",
            allowedValues = [
                "Easy", "Medium", "Hard"
            ]
        ),
        ScriptConfiguration(
            name = "Activity",
            description = "Which activity to take part in",
            defaultValue = "Attack Portal",
            allowedValues = [
                "Defend Knight", "Attack Portal", "Mix"
            ]
        ),
        ScriptConfiguration(
            name = "Overhead Prayer",
            description = "Overhead prayer to activate during games",
            defaultValue = "None",
            allowedValues = [
                "None", "Protect from Magic", "Protect from Melee", "Protect from Missiles", "Redemption"
            ]
        ),
        ScriptConfiguration(
            name = "Offensive Prayer",
            description = "Offensive prayer to activate during games",
            defaultValue = "None",
            allowedValues = [
                "None", "Eagle Eye", "Mystic Might", "Rigour"
            ]
        ),
        ScriptConfiguration(
            name = "Stop at Attack Level",
            description = "Stop script when reaching this Attack level (0 to disable)",
            defaultValue = "0",
            optionType = OptionType.INTEGER
        ),
        ScriptConfiguration(
            name = "Stop at Strength Level",
            description = "Stop script when reaching this Strength level (0 to disable)",
            defaultValue = "0",
            optionType = OptionType.INTEGER
        ),
        ScriptConfiguration(
            name = "Stop at Defence Level",
            description = "Stop script when reaching this Defence level (0 to disable)",
            defaultValue = "0",
            optionType = OptionType.INTEGER
        ),
        ScriptConfiguration(
            name = "Stop at Ranged Level",
            description = "Stop script when reaching this Ranged level (0 to disable)",
            defaultValue = "0",
            optionType = OptionType.INTEGER
        ),
        ScriptConfiguration(
            name = "Stop at Magic Level",
            description = "Stop script when reaching this Magic level (0 to disable)",
            defaultValue = "0",
            optionType = OptionType.INTEGER
        ),
        ScriptConfiguration(
            name = "Stop at Points",
            description = "Stop script when reaching this many points (0 to disable)",
            defaultValue = "0",
            optionType = OptionType.INTEGER
        ),
    ]
)
class PestControl : AbstractScript() {

    val tasks: MutableList<Task> = mutableListOf<Task>()
    var status: String = "None"

    var boat: Boat? = null

    var initialPoints: Int? = null
    var pointsGained: Int = 0

    var isMix = false
    var activity: Activity? = null

    var gamesPlayed = 0
    var gamesSinceChangedActivity = -1

    var playedGame = false

    var attackPortal: AttackPortal? = null

    var zealPercentage: Int? = null

    var overheadPrayer: PrayerType? = null
    var offensivePrayer: PrayerType? = null

    var stopAtAttackLevel: Int = 0
    var stopAtStrengthLevel: Int = 0
    var stopAtDefenceLevel: Int = 0
    var stopAtRangedLevel: Int = 0
    var stopAtMagicLevel: Int = 0
    var stopAtPoints: Int = 0

    override fun onStart() {
        val boatOpt = getOption<String?>("Boat Type")
        if (boatOpt != null) {
            boat = Boat.valueOf(boatOpt.replace(" ", ""))
            logger.info("Boat type selected: ${boat?.name}")
        }
        val activityOpt = getOption<String?>("Activity")
        if (activityOpt != null) {
            activity = Activity.valueOf(activityOpt.replace(" ", ""))
            logger.info("Activity selected: ${activity?.name}")
        }

        if (activity == null) {
            logger.error("No activity set - stopping script")
            Notifications.showNotification("No activity set")
            ScriptManager.stop()
        }

        if (boat == null) {
            logger.error("No boat set - stopping script")
            Notifications.showNotification("No boat set")
            ScriptManager.stop()
        }

        if (activity == Activity.Mix) {
            isMix = true
            logger.info("Mix mode enabled - will alternate between activities")
        }

        val overheadOpt = getOption<String?>("Overhead Prayer")
        if (overheadOpt != null && overheadOpt != "None") {
            overheadPrayer = PrayerType.fromDisplayName(overheadOpt)
            logger.info("Overhead prayer: ${overheadPrayer?.prayerName}")
        }

        val offensiveOpt = getOption<String?>("Offensive Prayer")
        if (offensiveOpt != null && offensiveOpt != "None") {
            offensivePrayer = PrayerType.fromDisplayName(offensiveOpt)
            logger.info("Offensive prayer: ${offensivePrayer?.prayerName}")
        }

        stopAtAttackLevel = getOption<Int>("Stop at Attack Level")
        stopAtStrengthLevel = getOption<Int>("Stop at Strength Level")
        stopAtDefenceLevel = getOption<Int>("Stop at Defence Level")
        stopAtRangedLevel = getOption<Int>("Stop at Ranged Level")
        stopAtMagicLevel = getOption<Int>("Stop at Magic Level")
        stopAtPoints = getOption<Int>("Stop at Points")

        logger.info("Stop conditions - Attack: $stopAtAttackLevel, Strength: $stopAtStrengthLevel, Defence: $stopAtDefenceLevel, Ranged: $stopAtRangedLevel, Magic: $stopAtMagicLevel, Points: $stopAtPoints")

        EventFlows.collectTicks {
            zealPercentage = Zeal.percentage()
        }

        val p = PaintBuilder.newBuilder()
            .addString("Status") { status }
            .addString("Activity") { activity?.name ?: "-" }
            .addString(
                "Points"
            ) {
                PaintFormatters.formatAmount((pointsGained).toLong())
            }
            .addString(
                "Games Played"
            ) {
                "${gamesPlayed}"
            }
            .addString(
                "Success Rate"
            ) {
                if (gamesPlayed == 0) {
                    "-"
                } else if (pointsGained == 0) {
                    "0%"
                } else {
                    "${(((pointsGained / boat!!.pointsPerGame).toDouble() / gamesPlayed.toDouble()) * 100).round(2)}%"
                }
            }
            .addString(
                "Fighting"
            ) {
                Players.local().interacting().name
            }
            .addString (
                "Zeal %"
            ) {
                if (zealPercentage != null) "${zealPercentage}%" else "-"
            }
            .trackSkill(Skill.Attack)
            .trackSkill(Skill.Defence)
            .trackSkill(Skill.Strength)
            .trackSkill(Skill.Hitpoints)
            .trackSkill(Skill.Magic)
            .trackSkill(Skill.Ranged)

        initTasks()

        if (attackPortal != null) {
            p.addString("Portal") { attackPortal?.portal?.name ?: "-" }
        }
        addPaint(p.build())
    }

    private fun initTasks() {
        tasks.clear()
        logger.info("Initializing tasks for activity: ${activity?.name}")

        tasks.add(Sleep())
        tasks.add(SetZoom())
        tasks.add(BoatWait(this))
        tasks.add(LeaveBoat(this))

        if (overheadPrayer != null || offensivePrayer != null) {
            tasks.add(ActivatePrayers(this))
        }

        tasks.add(CrossGangplank(boat!!, this))
        tasks.add(AttackInteracting())

        when (activity) {
            Activity.DefendKnight -> tasks.add(DefendKnight(activity!!))
            Activity.AttackPortal -> {
                tasks.add(AttackNearestNpc())
                attackPortal = AttackPortal(activity!!)
                tasks.add(attackPortal!!)
            }
            else -> {}
        }
    }

    override fun poll() {
        val squire = squire()
        if (squire.valid() && voidKnightHealth().visible()) {
            PestControlMap.update(squire.tile())
        }

        if (isMix && (gamesSinceChangedActivity == -1 || gamesSinceChangedActivity >= Random.nextInt(Constants.MIN_GAMES_BEFORE_ACTIVITY_CHANGE, Constants.MAX_GAMES_BEFORE_ACTIVITY_CHANGE))) {
            activity = if (Random.nextBoolean()) Activity.DefendKnight else Activity.AttackPortal
            logger.info("Mix mode: Switching activity to ${activity?.name}")
            initTasks()

            gamesSinceChangedActivity = 0
        }

        if (!Movement.running() && Movement.energyLevel() >= Random.nextInt(Constants.MIN_ENERGY_LEVEL, Constants.MAX_ENERGY_LEVEL)) {
            Movement.running(true)
        }

        val task = tasks.firstOrNull { it.valid() }
        if (task != null) {
            status = task.name()
            task.run()
            return
        }

        status = "None"
        Condition.sleep(100)
    }

}

fun main() {
    val script = PestControl()
    script.startScript("127.0.0.1", "0m6", false)
}
