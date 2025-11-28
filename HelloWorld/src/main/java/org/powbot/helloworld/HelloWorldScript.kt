package org.powbot.helloworld

import org.powbot.api.Notifications
import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptManifest

@ScriptManifest(scriptId = "3374639d-9dfd-443a-994a-9cb277eebf81", version = "0.0.1")
class HelloWorldScript : AbstractScript() {

    override fun onStart() {
        Notifications.showNotification("This is just a testing script")
    }

    override fun poll() {
        log.info("This doesn't actually do anything ;)")
        controller.stop()
    }
}

