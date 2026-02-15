# AGENTS Instructions For Community Scripts

Use these rules when creating or updating scripts for `powbot/community-scripts`.

## Required Script Project Format

Each script must be a standalone top-level folder:

```
<script-name>/
  src/main/kotlin/org/powbot/community/<script-name>/...
  src/main/resources/
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  .gitignore
  gradlew
  gradlew.bat
  gradle/wrapper/*
```

## Required Namespace Rules

- Use `org.powbot.community.*` only.
- Do not use `org.powbot.om6.*`.
- Keep package path aligned with folder path under `src/main/kotlin`.

## Build File Requirements (`build.gradle.kts`)

Each script build must include:

- Plugins:
  - `java`
  - `kotlin("jvm")`
  - `application`
- Repositories:
  - `mavenCentral()`
  - `google()`
  - `maven("https://repo.powbot.org/releases")`
- PowBot dependencies:
  - `implementation("org.powbot:client-sdk:3+")`
  - `implementation("org.powbot:client-sdk-loader:3+")`
- JVM toolchain:
  - `kotlin { jvmToolchain(11) }`
- Application entry:
  - `application { mainClass.set("<script-main-class>") }`
- Local runner task:
  - `runLocal` (`JavaExec`) using `sourceSets["main"].runtimeClasspath`

## Main Entrypoint Requirement

Every script should have a runnable entrypoint for local loader startup:

- Preferred: top-level `fun main() { <ScriptClass>().startScript("localhost", "author", false) }`
- If using companion object main, keep it as `@JvmStatic fun main(args: Array<String>)`.

`runLocal` must target the real JVM main class:

- Top-level main file `<FileName>.kt` -> `<package>.<FileName>Kt`
- Companion main in class `<ClassName>` -> `<package>.<ClassName>`

## Compile And Run Commands

From repo root (composite build):

- Compile one script: `.\gradlew.bat :<script-name>:classes`
- Run one script locally: `.\gradlew.bat :<script-name>:runLocal`
- Compile all scripts: `.\gradlew.bat classesAll`

From script folder:

- Compile: `.\gradlew.bat classes`
- Run local loader startup: `.\gradlew.bat runLocal`

Notes:

- `classes` only compiles; it does not start scripts.
- `runLocal` runs `main()` which calls `startScript(...)`.

## PR Submission Rules (Community Repo)

- One branch per script: `script/<script-name>`
- One PR per script folder.
- PR body must include:
  - Script name
  - Description/features
  - Short website description
- After SW review, add assigned `scriptId` to `@ScriptManifest`.

## Fork And PR Workflow (Required)

Use this exact flow when publishing to `powbot/community-scripts`:

1. Ensure fork exists:
   - `gh repo fork powbot/community-scripts --clone=false --remote=false`
2. Clone your fork (or update existing clone):
   - `git clone https://github.com/<your-user>/community-scripts.git`
3. Add/update upstream remote:
   - `git remote add upstream https://github.com/powbot/community-scripts.git`
   - `git fetch upstream`
4. Start from upstream main:
   - `git checkout -B script/<script-name> upstream/main`
5. Copy only the target script folder into the fork repo:
   - `<script-name>/...`
6. Commit one script per branch:
   - `git add <script-name>`
   - `git commit -m "Update <script-name> community script"`
7. Push branch to fork:
   - `git push -u origin script/<script-name>`
8. Open PR to upstream:
   - base: `powbot/community-scripts:main`
   - head: `<your-user>:script/<script-name>`
9. Never bundle multiple script folders in one PR.

PR command example:

```powershell
gh pr create `
  --repo powbot/community-scripts `
  --base main `
  --head <your-user>:script/<script-name> `
  --title "script: <Script Display Name>" `
  --body @"
Script name: <Script Display Name>

Description:
<Detailed script behavior and features>

Short description:
<Website short description>
"@
```

## New Script Checklist

When adding a new community script, do these in order:

1. Create folder: `<script-name>/`
2. Add source under: `src/main/kotlin/org/powbot/community/<script-name>/`
3. Add `main()` entrypoint that calls `startScript(...)`
4. Add required Gradle files:
   - `build.gradle.kts`
   - `settings.gradle.kts`
   - `gradle.properties`
   - `.gitignore`
   - wrapper files (`gradlew*`, `gradle/wrapper/*`)
5. Ensure `build.gradle.kts` contains:
   - `application` plugin
   - `google()` repository
   - `application { mainClass.set(...) }`
   - `runLocal` `JavaExec` task
6. Add script to root composite build:
   - `settings.gradle.kts`: `includeBuild("<script-name>")`
   - `build.gradle.kts`: include `gradle.includedBuild("<script-name>").task(":classes")` in `classesAll`
7. Verify compile only:
   - `.\gradlew.bat :<script-name>:classes`
8. Open PR:
   - branch `script/<script-name>`
   - one script folder per PR

## Templates

### `settings.gradle.kts` (script folder)

```kotlin
pluginManagement {
    plugins {
        kotlin("jvm") version "1.9.22"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "ScriptName"
```

### `build.gradle.kts` (script folder)

```kotlin
plugins {
    id("java")
    kotlin("jvm")
    application
}

group = "org.powbot.community.<script-name>"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven("https://repo.powbot.org/releases")
}

dependencies {
    implementation("org.powbot:client-sdk:3+")
    implementation("org.powbot:client-sdk-loader:3+")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(11)
}

tasks.jar {
    archiveBaseName.set("ScriptName")
}

application {
    mainClass.set("org.powbot.community.<script-name>.<MainFileName>Kt")
}

tasks.register<JavaExec>("runLocal") {
    group = "application"
    description = "Runs script locally (invokes startScript)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.powbot.community.<script-name>.<MainFileName>Kt")
}
```

## Safety Defaults

- Default to compile checks only (`classes`), not `runLocal`.
- Do not execute `runLocal` unless explicitly requested by the user.
- `runLocal` may connect to PowBot and start runtime behavior; treat it as side-effectful.
