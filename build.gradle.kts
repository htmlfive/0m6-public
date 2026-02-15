tasks.register("classesAll") {
    group = "build"
    description = "Compiles classes for all included script builds."
    dependsOn(
        gradle.includedBuild("baggedplants").task(":classes"),
        gradle.includedBuild("barbarianfishing").task(":classes"),
        gradle.includedBuild("ectofunctus").task(":classes"),
        gradle.includedBuild("herblore").task(":classes"),
        gradle.includedBuild("herbrun").task(":classes"),
        gradle.includedBuild("ironmandailies").task(":classes"),
        gradle.includedBuild("libationprayer").task(":classes"),
        gradle.includedBuild("lumbyfires").task(":classes"),
        gradle.includedBuild("maplefletcher").task(":classes"),
        gradle.includedBuild("mortmyre").task(":classes"),
        gradle.includedBuild("pohcake").task(":classes"),
        gradle.includedBuild("squidharpooner").task(":classes"),
        gradle.includedBuild("wallsafe").task(":classes"),
        gradle.includedBuild("winebuyer").task(":classes")
    )
}
