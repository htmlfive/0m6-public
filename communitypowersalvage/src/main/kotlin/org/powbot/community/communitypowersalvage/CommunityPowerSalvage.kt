package org.powbot.community.communitypowersalvage

import com.google.common.eventbus.Subscribe
import org.powbot.api.Condition
import org.powbot.api.Events
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.api.event.PaintCheckboxChangedEvent
import org.powbot.api.event.RenderEvent
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.community.api.ScriptLogging
import org.powbot.community.communitypowersalvage.app.WireframeRenderer
import org.powbot.community.communitypowersalvage.tasks.base.Task
import org.powbot.community.communitypowersalvage.tasks.execution.CameraSetupTask
import org.powbot.community.communitypowersalvage.tasks.execution.DeployHookTask
import org.powbot.community.communitypowersalvage.tasks.execution.DropSalvageTask
import org.powbot.community.communitypowersalvage.tasks.execution.WorldHopTask
import kotlin.random.Random
import org.powbot.api.script.ScriptConfiguration.List as ConfigList

@ScriptManifest(
    name = "0m6 Community Power-Salvage",
    description = "Salvages any shipwreck at the nearest hook and drops all salvage. Simple power-salvage loop.",
    version = "1.0.5",
    author = "0m6",
    scriptId = "8e0276aa-4861-4fd0-a2f0-5e5abeb2594f",
    category = ScriptCategory.Other
)
@ConfigList(
    [
        ScriptConfiguration(
            "Salvaging Hook",
            "Type of salvaging hook to deploy.",
            optionType = OptionType.STRING,
            defaultValue = "Adamant salvaging hook",
            allowedValues = ["Bronze salvaging hook", "Iron salvaging hook", "Steel salvaging hook", "Mithril salvaging hook", "Adamant salvaging hook", "Rune salvaging hook", "Dragon salvaging hook"]
        ),
        ScriptConfiguration(
            "Cargo Hold Tier",
            "Tier of cargo hold on your ship.",
            optionType = OptionType.STRING,
            defaultValue = "Mahogany cargo hold",
            allowedValues = ["Basic cargo hold", "Oak cargo hold", "Teak cargo hold", "Mahogany cargo hold", "Camphor cargo hold", "Ironwood cargo hold", "Rosewood cargo hold"]
        ),
        ScriptConfiguration(
            "Hop worlds if no shipwreck?",
            "Hop to a random world when no active shipwreck is found.",
            optionType = OptionType.BOOLEAN, defaultValue = "true"
        ),
        ScriptConfiguration(
            "Tap-to-drop",
            "Tap-to-drop: If true, enabled tap-to-drop before starting",
            optionType = OptionType.BOOLEAN, defaultValue = "true"
        ),
    ]
)
class CommunityPowerSalvage : AbstractScript() {
    val salvagingHookName: String get() = getOption<String>("Salvaging Hook")
    val cargoHoldTierDisplayName: String get() = getOption<String>("Cargo Hold Tier")
    val hopWorlds: Boolean get() = getOption<Boolean>("Hop worlds if no shipwreck?")
    val tapToDrop: Boolean get() = getOption<Boolean>("Tap-to-drop")
    @Volatile var drawWireFrames: Boolean = false
    val wireframeRenderer: WireframeRenderer by lazy { WireframeRenderer(this) }
    private val renderSubscriber = RenderSubscriber(this)
    @Volatile var status: String = "Waiting"
    @Volatile var justHopped: Boolean = false
    private var lastPollLog = 0L

    companion object {
        const val WIREFRAME_CHECKBOX_ID = "community_powersalvage_wireframe"
    }
    private val allTasks: List<Task> by lazy {
        listOf(
            CameraSetupTask(this),
            WorldHopTask(this),
            DropSalvageTask(this),
            DeployHookTask(this)
        )
    }

    override fun onStart() {
        ScriptLogging.info(logger, "Community Power-Salvage starting...")

        addPaint(
            PaintBuilder.newBuilder()
                .x(40).y(80)
                .addString("Status") { status }
                .trackSkill(Skill.Sailing)
                .addCheckbox("Show Wireframes", WIREFRAME_CHECKBOX_ID, false)
                .build()
        )

        Events.register(renderSubscriber)

        Inventory.enableShiftDropping()
        if (tapToDrop) { Game.setMouseToggleAction(Game.MouseToggleAction.DROP) }

        Condition.sleep(Random.nextInt(600, 1200))

        if (tapToDrop) { Game.setMouseActionToggled(true) } else { Game.setMouseActionToggled(false) }
    }

    override fun poll() {
        val pollNow = System.currentTimeMillis()
        if (pollNow - lastPollLog > 2000) {
            lastPollLog = pollNow
            ScriptLogging.info(logger, "POLL: status=${status}")
        }

        try {
            for (task in allTasks) {
                if (task.activate()) {
                    task.execute()
                    return
                }
            }
            Condition.sleep(300)
        } catch (e: Exception) {
            ScriptLogging.error(logger, "Error in poll: ${e.message}")
            Condition.sleep(1000)
        }
    }

    @Subscribe
    @Suppress("unused")
    fun onPaintCheckboxChanged(evt: PaintCheckboxChangedEvent) {
        if (evt.checkboxId == WIREFRAME_CHECKBOX_ID) {
            drawWireFrames = evt.checked
        }
    }

    fun stopScript(reason: String) {
        ScriptLogging.stopWithNotification(this, reason)
    }
}

private class RenderSubscriber(private val script: CommunityPowerSalvage) {
    @Subscribe
    @Suppress("unused")
    fun onRender(@Suppress("UNUSED_PARAMETER") evt: RenderEvent) {
        script.wireframeRenderer.onRender(System.currentTimeMillis())
    }
}

fun main() {
    val script = CommunityPowerSalvage()
    script.startScript("127.0.0.1", "0m6", false)
}
