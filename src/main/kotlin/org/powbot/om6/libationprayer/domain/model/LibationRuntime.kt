package org.powbot.om6.libationprayer.domain.model

data class LibationRuntime(
    var currentTask: String = "Initializing",
    var needsInitialFullRecharge: Boolean = false,
    var awaitingBowlFill: Boolean = false
)
