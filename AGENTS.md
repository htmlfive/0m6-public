# PowBot Community Scripts

A collection of community-contributed scripts for the PowBot SDK. This repository allows developers to share their scripts with the PowBot community.

- **CRITICAL: Never add `resolutionStrategy.cacheChangingModulesFor` to any `build.gradle` or `build.gradle.kts` file.** It disables Gradle's dependency cache and forces IntelliJ into an infinite indexing loop when PowBot SDK `3.+` changing modules trigger re-download on every sync.

## Getting Started

### Prerequisites

- **JDK 11+** - Required for building scripts
- **Gradle** - Build automation (wrapper included in each project)
- **Android device or emulator** with PowBot client installed
- **ADB** - For port forwarding during development

### Repository Structure

```
community-scripts/
├── README.md                    # This file
├── YourScriptName/              # Each script in its own folder
│   ├── src/main/kotlin/...      # or src/main/java/...
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── ...
└── AnotherScript/
    └── ...
```

## Creating Your Own Script

### Project Structure

#### Kotlin

```
MyScript/
├── src/main/kotlin/org/powbot/community/myscript/
│   └── MyScript.kt
├── src/main/resources/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── .gitignore
```

#### Java

```
MyScript/
├── src/main/java/org/powbot/community/myscript/
│   └── MyScript.java
├── src/main/resources/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── .gitignore
```

### build.gradle.kts Template

#### For Kotlin:

```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
}

group = "org.powbot.community.yourscript"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.powbot.org/releases")
}

dependencies {
    compileOnly("org.powbot:client-sdk:1.+")
    compileOnly("org.powbot:client-sdk-loader:1.+")
    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(11)
}

tasks.jar {
    archiveBaseName.set("YourScriptName")
}
```

#### For Java:

```kotlin
plugins {
    java
}

group = "org.powbot.community.yourscript"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.powbot.org/releases")
}

dependencies {
    compileOnly("org.powbot:client-sdk:1.+")
    compileOnly("org.powbot:client-sdk-loader:1.+")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
```

## Script Anatomy

### Basic Script Template (Kotlin)

```kotlin
package org.powbot.community.yourscript

import org.powbot.api.script.AbstractScript
import org.powbot.api.script.ScriptCategory
import org.powbot.api.script.ScriptManifest

@ScriptManifest(
    name = "Your Script Name",
    description = "What your script does",
    version = "1.0.0",
    category = ScriptCategory.Other,
    author = "Your Name"
)
class YourScript : AbstractScript() {

    override fun onStart() {
        // Called once when the script starts
        log.info("Script started!")
    }

    override fun poll() {
        // Main loop - called repeatedly
        // Keep this fast (< 600ms)
    }

    override fun onStop() {
        // Called once when the script stops
        log.info("Script stopped!")
    }
}
```

### Key Concepts

#### 1. State Machine Pattern

```kotlin
private enum class State { WORKING, BANKING, IDLE }

private fun getState(): State {
    return when {
        Inventory.isFull() -> State.BANKING
        Players.local().animation() == -1 -> State.WORKING
        else -> State.IDLE
    }
}

override fun poll() {
    when (getState()) {
        State.BANKING -> handleBanking()
        State.WORKING -> handleWorking()
        State.IDLE -> Condition.sleep(300)
    }
}
```

#### 2. Paint/UI Overlay

```kotlin
override fun onStart() {
    val paint = PaintBuilder.newBuilder()
        .x(40).y(40)
        .trackSkill(Skill.Mining)
        .addString("Status:") { currentStatus }
        .addString("Count:") { itemCount.toString() }
        .build()
    addPaint(paint)
}
```

#### 3. Entity Interaction

```kotlin
// Finding objects
val rock = Objects.stream()
    .name("Iron rocks")
    .action("Mine")
    .within(15)
    .nearest()
    .first()

// Interacting
if (rock.valid() && rock.interact("Mine")) {
    Condition.wait({ Players.local().animation() != -1 }, 2500, 100)
}
```

#### 4. Condition Waiting

```kotlin
// Wait for a condition with timeout and polling interval
Condition.wait(
    { /* condition */ Players.local().animation() != -1 },
    2500,  // timeout in ms
    100    // polling interval in ms
)

// Simple sleep with randomization
Condition.sleep(Random.nextInt(100, 300))
```

## Building and Running

### Build Your Script

```bash
cd YourScript/
./gradlew jar
```

The JAR file will be in `build/libs/`.

### Running on Device

1. **Set up port forwarding:**
   ```bash
   adb forward tcp:61666 tcp:61666
   ```

2. **Upload via scriptloader** or copy the JAR to your device

3. **Start the script** from the PowBot client

## Deploying Your Script

### Submission Process

1. **Create a branch** with the naming pattern `script/<script-name>` (e.g., `script/my-power-miner`)

2. **Open a Pull Request** using the PR template. Your PR should include:
   - **Script name**
   - **Description** of what the script does and its features
   - **Short description** for display on the website

3. **Review process** - The SW team will review your PR. Anyone can leave feedback. If approved, you'll receive a comment with a **Script ID** to add to your manifest.

4. **Update your manifest** with the provided Script ID:
   ```kotlin
   @ScriptManifest(
       name = "Your Script Name",
       description = "What your script does",
       version = "1.0.0",
       category = ScriptCategory.Other,
       author = "Your Name",
       scriptId = "your-assigned-script-id"  // Add this line
   )
   ```

5. **Merge** - Once you've updated the manifest, the PR will be marked as approved and you can merge it.

6. **Go live** - Your script will be available within **10 minutes** of merging.

### Updating Your Script

Follow the same process for updates:

1. Create a branch with your changes
2. Open a PR describing the changes
3. Wait for review and approval
4. Merge once approved
5. Changes go live within 10 minutes

## Resources

- [PowBot Website](https://powbot.org)
- [PowBot Discord](https://discord.gg/powbot)
- [API Documentation](https://docs.powbot.org)

## License

Community scripts are provided as-is for educational and personal use. Please respect the PowBot terms of service.
