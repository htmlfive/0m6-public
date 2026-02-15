package org.powbot.community.pohcake

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptManifest
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.mobile.script.ScriptManager
import org.powbot.community.pohcake.tasks.CombineIngredientsTask
import org.powbot.community.pohcake.tasks.CookCakeTask
import org.powbot.community.pohcake.tasks.DropItemsTask
import org.powbot.community.pohcake.tasks.SearchLarderTask

private const val ITEM_EGG = "Egg"
private const val ITEM_CAKE_TIN = "Cake tin"
private const val ITEM_UNCOOKED_CAKE = "Uncooked cake"
private const val ITEM_CAKE = "Cake"
private const val ITEM_BURNT_CAKE = "Burnt cake"
private const val ITEM_POT = "Pot"
private const val ITEM_BUCKET = "Bucket"
private const val ITEM_MILK = "Bucket of milk"
private const val ITEM_FLOUR = "Pot of flour"

private const val OPTION_MILK = "Milk"
private const val OPTION_EGG = "Egg"
private const val OPTION_FLOUR = "Flour"

@ScriptManifest(
    name = "0m6 POH Cake Maker",
    description = "Makes cakes in POH using the larder and fancy range.",
    version = "1.0.0",
    author = "0m6",
    category = ScriptCategory.Cooking
)
class PohCakeMaker : AbstractScript() {

    private var currentTask = "Starting"

    private val tasks = listOf(
        SearchLarderTask(this),
        CombineIngredientsTask(this),
        CookCakeTask(this),
        DropItemsTask(this)
    )

    override fun onStart() {
        logger.info("POH Cake Maker starting.")
        if (!hasCakeTinOrUncookedCake()) {
            logger.error("Missing Cake tin or Uncooked cake. Stopping.")
            ScriptManager.stop()
            return
        }
        ensureShiftDrop()

        val paint = PaintBuilder.newBuilder()
            .x(40)
            .y(80)
            .addString("Task:") { currentTask }
            .addString("Egg:") { hasEgg().toString() }
            .addString("Milk:") { hasMilk().toString() }
            .addString("Flour:") { hasFlour().toString() }
            .addString("Uncooked:") { hasUncookedCake().toString() }
            .trackSkill(Skill.Cooking)
            .build()
        addPaint(paint)
    }

    override fun poll() {
        if (!Game.loggedIn()) {
            Condition.sleep(250)
            return
        }

        if (!hasCakeTinOrUncookedCake()) {
            logger.error("Cake tin or Uncooked cake missing during run. Stopping.")
            ScriptManager.stop()
            return
        }

        val task = tasks.firstOrNull { it.activate() }
        if (task != null) {
            if (currentTask != task.name) {
                currentTask = task.name
            }
            logger.info("Task: ${task.name}")
            task.execute()
            return
        }
        Condition.sleep(Random.nextInt(120, 180))
    }

    internal fun hasAllIngredients(): Boolean =
        hasEgg() && hasMilk() && hasFlour()

    internal fun hasEgg(): Boolean =
        Inventory.stream().name(ITEM_EGG).isNotEmpty()

    internal fun hasMilk(): Boolean =
        Inventory.stream().nameContains("milk").isNotEmpty()

    internal fun hasFlour(): Boolean =
        Inventory.stream().nameContains("flour").isNotEmpty()

    internal fun hasCakeTin(): Boolean =
        Inventory.stream().name(ITEM_CAKE_TIN).isNotEmpty()

    internal fun hasCakeTinOrUncookedCake(): Boolean =
        hasCakeTin() || hasUncookedCake()

    internal fun hasUncookedCake(): Boolean =
        Inventory.stream().name(ITEM_UNCOOKED_CAKE).isNotEmpty()

    internal fun hasDropItems(): Boolean =
        Inventory.stream().name(ITEM_POT, ITEM_CAKE, ITEM_BUCKET, ITEM_BURNT_CAKE).isNotEmpty()

    internal fun dropJunkItems(): Boolean {
        val items = Inventory.stream()
            .name(ITEM_POT, ITEM_CAKE, ITEM_BUCKET, ITEM_BURNT_CAKE)
            .toList()
        if (items.isEmpty()) {
            return false
        }
        ensureShiftDrop()
        logger.info("Dropping ${items.size} junk items.")
        Inventory.drop(items)
        return Condition.wait(
            { Inventory.stream().name(ITEM_POT, ITEM_CAKE, ITEM_BUCKET, ITEM_BURNT_CAKE).isEmpty() },
            150,
            20
        )
    }

    internal fun ensureShiftDrop(): Boolean =
        if (Inventory.shiftDroppingEnabled()) {
            true
        } else {
            logger.info("Enabling tap-to-drop (shift drop).")
            Inventory.enableShiftDropping()
        }

    internal fun nextMissingLarderOption(): String? =
        when {
            !hasMilk() -> OPTION_MILK
            !hasEgg() -> OPTION_EGG
            !hasFlour() -> OPTION_FLOUR
            else -> null
        }

    internal fun getCountForOption(option: String): Int =
        when (option) {
            OPTION_MILK -> Inventory.stream().nameContains("milk").count().toInt()
            OPTION_EGG -> Inventory.stream().name(ITEM_EGG).count().toInt()
            OPTION_FLOUR -> Inventory.stream().nameContains("flour").count().toInt()
            else -> 0
        }

    internal fun selectChatOption(option: String): Boolean {
        if (!Chat.chatting()) {
            return false
        }
        logger.info("Selecting chat option: $option")
        val before = getCountForOption(option)
        val optionMatch = Chat.get({ it.text().equals(option, true) }, true)
            .firstOrNull()
        val selected = if (optionMatch != null && optionMatch.valid()) {
            optionMatch.select()
        } else {
            val firstOption = Chat.get(null, true).firstOrNull()
            if (firstOption != null && firstOption.valid()) {
                logger.warn("Option '$option' not found; selecting first option.")
                firstOption.select()
            } else {
                false
            }
        }
        if (!selected) {
            logger.warn("Failed to select chat option: $option")
            return false
        }
        val gained = Condition.wait(
            { getCountForOption(option) > before || !Chat.chatting() },
            150,
            20
        )
        logger.info("Chat option result for $option: $gained")
        return gained
    }

    internal fun logInventorySnapshot() {
        logger.info(
            "Inventory: egg=${hasEgg()}, milk=${hasMilk()}, flour=${hasFlour()}, uncooked=${hasUncookedCake()}"
        )
    }
}

fun main() {
    val script = PohCakeMaker()
    script.startScript("127.0.0.1", "0m6", false)
}

