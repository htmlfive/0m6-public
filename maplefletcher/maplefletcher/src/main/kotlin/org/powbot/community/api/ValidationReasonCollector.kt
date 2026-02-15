package org.powbot.community.api

data class ValidationReason(val verbose: String, val detail: String)

class ValidationReasonCollector {
    private val reasons = mutableListOf<ValidationReason>()

    fun add(condition: Boolean, verbose: String, detail: String) {
        if (!condition) {
            return
        }
        reasons.add(ValidationReason(verbose, detail))
    }

    fun verboseReasons(): List<String> = reasons.map { it.verbose }

    fun detailReasons(): List<String> = reasons.map { it.detail }
}

