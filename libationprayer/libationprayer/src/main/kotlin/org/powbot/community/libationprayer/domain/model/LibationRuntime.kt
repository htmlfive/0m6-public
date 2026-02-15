package org.powbot.community.libationprayer.domain.model

data class LibationRuntime(
    var currentTask: String = "Initializing",
    var needsInitialFullRecharge: Boolean = false,
    var awaitingBowlFill: Boolean = false
)

