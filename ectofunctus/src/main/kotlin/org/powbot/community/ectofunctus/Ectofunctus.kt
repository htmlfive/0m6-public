package org.powbot.community.ectofunctus

import org.powbot.api.Condition
import org.powbot.api.Tile
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.community.ectofunctus.tasks.BankTask
import org.powbot.community.ectofunctus.tasks.IdleTask
import org.powbot.community.ectofunctus.tasks.ProcessBonesTask
import org.powbot.community.ectofunctus.tasks.ShouldBank
import org.powbot.community.ectofunctus.tasks.ShouldProcessBones
import org.powbot.community.ectofunctus.tasks.ShouldWorship
import org.powbot.community.ectofunctus.tasks.Task
import org.powbot.community.ectofunctus.tasks.WorshipTask

@ScriptManifest(
    name = "Ectofunctus",
    description = "Grinds dragon bones, worships the Ectofuntus, and restocks from Castle Wars automatically.",
    version = "1.0.0",
    author = "0m6",
    category = ScriptCategory.Prayer
)
class Ectofunctus : AbstractScript() {
    val castleWarsBankTile: Tile = EctofunctusConstants.CASTLE_WARS_BANK_TILE
    val grinderTile: Tile = EctofunctusConstants.GRINDER_TILE
    val ectofuntusTile: Tile = EctofunctusConstants.ECTOFUNTUS_TILE
    val batchSize: Int = EctofunctusConstants.DEFAULT_BATCH_SIZE

    private var startPrayerXp: Int = 0
    private var taskDescription: String = "Initializing"

    private lateinit var shouldBank: ShouldBank
    private lateinit var shouldProcessBones: ShouldProcessBones
    private lateinit var shouldWorship: ShouldWorship
    private lateinit var bankTask: BankTask
    private lateinit var processBonesTask: ProcessBonesTask
    private lateinit var worshipTask: WorshipTask
    private lateinit var idleTask: IdleTask

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val script = Ectofunctus()
            script.startScript("localhost", "0m6", false)
        }
    }

    override fun onStart() {
        startPrayerXp = Skills.experience(Skill.Prayer)
        shouldBank = ShouldBank()
        shouldProcessBones = ShouldProcessBones()
        shouldWorship = ShouldWorship()
        bankTask = BankTask(this)
        processBonesTask = ProcessBonesTask(this)
        worshipTask = WorshipTask(this)
        idleTask = IdleTask()

        addPaint(
            PaintBuilder.newBuilder()
                .trackSkill(Skill.Prayer)
                .addString("Status") { taskDescription }
                .build()
        )
    }

    override fun poll() {
        val selectedTask: Task = when {
            shouldBank.validate() -> bankTask
            shouldProcessBones.validate() -> processBonesTask
            shouldWorship.validate() -> worshipTask
            else -> idleTask
        }
        taskDescription = selectedTask.toString()
        Condition.sleep(selectedTask.execute())
    }
}
