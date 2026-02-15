# PowBot Community-Style Script Repo

This repository is structured to match the `powbot/community-scripts` pattern:

- Each script project lives in its own top-level folder.
- Each folder contains its own Gradle build files and source tree.

## Projects

- `baggedplants/`
- `barbarianfishing/`
- `ectofunctus/`
- `herblore/`
- `herbrun/`
- `ironmandailies/`
- `libationprayer/`
- `lumbyfires/`
- `maplefletcher/`
- `mortmyre/`
- `pohcake/`
- `squidharpooner/`
- `wallsafe/`
- `winebuyer/`

Legacy consolidated project:
- `Om6Public/`

## Build

```powershell
cd <script-folder>
.\gradlew.bat jar
```

## Workspace Gradle (Root)

You can now run Gradle from repo root as a composite build:

```powershell
.\gradlew.bat tasks
.\gradlew.bat :pohcake:classes
.\gradlew.bat :winebuyer:jar
.\gradlew.bat classesAll
```
