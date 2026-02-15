package org.powbot.om6.api

import org.powbot.api.AppManager.logger
import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.rt4.Component
import org.powbot.api.rt4.Components
import org.powbot.api.rt4.Widgets

/**
 * Comprehensive widget utility functions for interacting with OSRS widget components.
 * Handles clicking, validation, text extraction, and widget unpacking.
 */
object WidgetUtils {

    data class WidgetPair(
        val parentId: Int,
        val componentIndex: Int,
        val action: String = ""
    )

    // ========================================
    // WIDGET CLICKING
    // ========================================

    /**
     * Clicks a widget component by root and component indices.
     * @param root The root widget index
     * @param component The component index within the root widget
     * @param index Optional sub-component index
     * @param action Optional action to perform. If null, performs a simple click.
     * @return true if the widget was valid and clicked successfully
     */
    fun clickWidget(root: Int, component: Int, index: Int? = null, action: String? = null): Boolean {
        val widget = if (index != null) {
            Widgets.widget(root).component(component).component(index)
        } else {
            Widgets.widget(root).component(component)
        }
        if (!widget.valid()) return false
        val result = if (action != null) widget.interact(action) else widget.click()
        if (result) Condition.sleep(Random.nextInt(600, 900))
        return result
    }

    // ========================================
    // WIDGET VALIDATION
    // ========================================

    /**
     * Checks if a widget component is visible and valid.
     * @param root The root widget index
     * @param component The component index
     * @param index Optional sub-component index
     * @return true if the widget is valid and visible
     */
    fun isVisible(root: Int, component: Int, index: Int? = null): Boolean {
        val widget = if (index != null) {
            Widgets.widget(root).component(component).component(index)
        } else {
            Widgets.widget(root).component(component)
        }
        return widget.valid() && widget.visible()
    }

    // ========================================
    // TEXT EXTRACTION
    // ========================================

    /**
     * Gets the text content of a widget component.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return The text content, or empty string if not available
     */
    fun getText(groupId: Int, componentId: Int): String {
        return Widgets.component(groupId, componentId).text()
    }

    /**
     * Gets the numeric value from a widget component.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return The parsed integer, or 0 if not available
     */
    fun getNumber(groupId: Int, componentId: Int): Int {
        return Widgets.component(groupId, componentId).text().toIntOrNull() ?: 0
    }

    /**
     * Gets the numeric value from a widget component with a default value.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @param default Default value if parsing fails
     * @return The parsed integer, or default if not available
     */
    fun getNumber(groupId: Int, componentId: Int, default: Int): Int {
        return Widgets.component(groupId, componentId).text().toIntOrNull() ?: default
    }

    /**
     * Gets a child component's text from a parent widget component.
     * @param groupId The widget group ID
     * @param parentComponentId The parent component ID
     * @param childComponentId The child component ID
     * @return The child component text, or empty string if not available
     */
    fun getChildText(groupId: Int, parentComponentId: Int, childComponentId: Int): String {
        return Widgets.component(groupId, parentComponentId, childComponentId).text()
    }

    // ========================================
    // WIDGET DIMENSIONS
    // ========================================

    /**
     * Gets the width of a widget component.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return The width in pixels
     */
    fun getWidth(groupId: Int, componentId: Int): Int {
        return Widgets.component(groupId, componentId).width()
    }

    /**
     * Gets the height of a widget component.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return The height in pixels
     */
    fun getHeight(groupId: Int, componentId: Int): Int {
        return Widgets.component(groupId, componentId).height()
    }

    /**
     * Extracts a percentage from a bar-type widget.
     * Useful for progress bars or resource bars.
     * @param groupId The widget group ID
     * @param barComponentId The bar component ID
     * @param fillComponentId The fill component ID
     * @return The percentage (0-100), or null if bar is not visible
     */
    fun getBarPercentage(groupId: Int, barComponentId: Int, fillComponentId: Int): Int? {
        val barComp = Widgets.component(groupId, barComponentId)
        if (barComp.visible()) {
            val barLength = barComp.width()
            val activityBar = Widgets.component(groupId, barComponentId, fillComponentId).width().toDouble()
            return ((activityBar / barLength) * 100).toInt()
        }
        return null
    }

    // ========================================
    // WIDGET UNPACKING
    // ========================================

    /**
     * Converts a single, packed OSRS widget ID into its Parent ID, Component Index, and Action.
     *
     * @param packedId The single integer representing the packed widget (e.g., 61800449).
     * @param action The action string for the widget (default: empty string).
     * @return A WidgetPair containing the parent ID, component index, and action.
     */
    fun unpack(packedId: Int, action: String = ""): WidgetPair {
        val divisor = 65536
        val parentId = packedId / divisor
        val componentIndex = packedId % divisor
        return WidgetPair(parentId, componentIndex, action)
    }

    /**
     * Clicks a widget component.
     * @param groupId The widget group ID
     * @param componentId The component ID
     * @return true if the component was clicked
     */
    fun click(groupId: Int, componentId: Int): Boolean {
        return Widgets.component(groupId, componentId).click()
    }

    // ========================================
    // BANK/WIDGET ITEM INTERACTION
    // ========================================

    /**
     * Withdraws an item from a bank widget by name (partial match).
     * @param widgetId The widget ID containing the items
     * @param componentId The component ID containing the items
     * @param itemName The name of the item to withdraw (supports partial match)
     * @param action The withdraw action (e.g., "Withdraw-1", "Withdraw-All")
     * @return true if the item was found and interacted with
     */
    fun withdrawItem(widgetId: Int, componentId: Int, itemName: String, action: String = "Withdraw-1"): Boolean {
        val component = Components.stream(widgetId, componentId)
            .filtered { it.name().contains(itemName, ignoreCase = true) }
            .first()

        if (component.visible()) {
            return component.interact(action)
        }
        return false
    }

    /**
     * Finds a component by item name (partial match).
     * @param widgetId The widget ID containing the items
     * @param componentId The component ID containing the items
     * @param itemName The name of the item to find (supports partial match)
     * @return The component if found and visible, null otherwise
     */
    fun findItemByName(widgetId: Int, componentId: Int, itemName: String): Component? {
        val component = Components.stream(widgetId, componentId)
            .filtered { it.name().contains(itemName, ignoreCase = true) }
            .first()

        return if (component.visible()) component else null
    }

    /**
     * Finds a component whose name exactly matches the requested item name (case-insensitive).
     * Strips HTML color tags from component names before comparison.
     * Useful when items share prefixes (e.g., "Raw shark" vs "Shark").
     */
    fun findItemByExactName(widgetId: Int, componentId: Int, itemName: String): Component? {
        val component = Components.stream(widgetId, componentId)
            .filtered {
                val cleanName = it.name().replace(Regex("<[^>]*>"), "").trim()
                cleanName.equals(itemName, ignoreCase = true)
            }
            .firstOrNull()

        return component?.takeIf { it.visible() }
    }

    fun debugComponent(searchText: String) {
        val component = Components.stream().filtered {
            it.name().contains(searchText, ignoreCase = true) || it.text().contains(searchText, ignoreCase = true)
        }.firstOrNull()

        component?.let {
            logger.info("=== Component Debug Info ===")
            logger.info("Name: ${it.name()}")
            logger.info("Text: ${it.text()}")
            logger.info("Index: ${it.index()}")
            logger.info("Parent ID: ${it.parentId()}")
            logger.info("Screen Point: ${it.screenPoint()}")
            logger.info("Visible: ${it.visible()}")
            logger.info("Actions: ${it.actions().toList()}")
            logger.info("Item ID: ${it.itemId()}")
            logger.info("Item Stack Size: ${it.itemStackSize()}")
            logger.info("========================")
        } ?: logger.info("Component not found for: $searchText")
    }
}
