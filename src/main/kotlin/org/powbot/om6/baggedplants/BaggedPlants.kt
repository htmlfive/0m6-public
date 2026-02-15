package org.powbot.om6.baggedplants

import org.powbot.api.script.*
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.Condition
import org.powbot.om6.baggedplants.tasks.*
import kotlin.random.Random

@ScriptManifest(
    name = "0m6 Bagged Plants",
    description = "Removes and rebuilds plants",
    version = "1.0.0",
    author = "0m6",
    category = ScriptCategory.Farming
)
class BaggedPlants : AbstractScript() {

    // Stats tracking
    var plantsBuilt = 0
    private var startTime = 0L
    var currentTask: String = "Initializing"

    // Task list
    private val allTasks: List<Task> by lazy {
        logger.info("INIT: Task list initialized")
        listOf(
            RefillTask(this),
            RemovePlantTask(this),
            BuildPlantTask(this)
        )
    }

    override fun onStart() {
        logger.info("SCRIPT START: Bagged Plants initializing...")
        startTime = System.currentTimeMillis()

        val paint = PaintBuilder.newBuilder()
            .x(40).y(80)
            .addString("Current Task") { currentTask }
            .addString("Plants Built") { plantsBuilt.toString() }
            .trackSkill(Skill.Farming)
            .trackSkill(Skill.Construction)
            .build()
        addPaint(paint)
        
        logger.info("SCRIPT START: Bagged Plants ready")
    }
    
    override fun poll() {
        try {
            for (task in allTasks) {
                if (task.activate()) {
                    val taskName = task::class.simpleName ?: "Unknown"
                    currentTask = taskName
                    logger.info("EXECUTING: $taskName")
                    task.execute()
                    return
                }
            }
            
            logger.warn("POLL: No task activated. Waiting...")
            Condition.sleep(Random.nextInt(1000, 2000))
            
        } catch (e: Exception) {
            logger.error("CRASH in poll(): ${e.message}", e)
            Condition.sleep(1000)
        }
    }

    abstract class Task(val script: BaggedPlants) {
        abstract fun activate(): Boolean
        abstract fun execute()
    }
}


fun main() {
    val script = BaggedPlants()
    script.startScript("127.0.0.1", "0m6", false)
}
