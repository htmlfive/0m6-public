package org.powbot.community.pestcontrol.task

interface Task {

    fun name(): String

    fun valid(): Boolean

    fun run()
}
