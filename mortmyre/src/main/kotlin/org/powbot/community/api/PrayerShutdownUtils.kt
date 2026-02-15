package org.powbot.community.api

object PrayerShutdownUtils {
    fun disableProtectionAndOffensive(
        protectionCandidates: Iterable<String>,
        reason: String,
        logPrefix: String,
        onInfo: (String) -> Unit,
        onWarn: (String) -> Unit = {},
        disableOffensive: (String) -> Unit
    ) {
        for (prayerName in protectionCandidates.distinct()) {
            if (!PrayerUtils.isPrayerActive(prayerName)) {
                continue
            }
            onInfo("$logPrefix Disabling $prayerName $reason.")
            if (PrayerUtils.deactivatePrayer(prayerName)) {
                break
            } else {
                onWarn("$logPrefix Failed to deactivate $prayerName $reason.")
            }
        }
        disableOffensive(logPrefix)
    }
}

