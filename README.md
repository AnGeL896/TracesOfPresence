# TracesOfPresence

TracesOfPresence is an actively developed Minecraft Forge mod for Minecraft Java Edition 1.20.1. It adds brief encounters with apparitions shaped by traces of life, memory, and presence.

## About TracesOfPresence

The mod explores a simple idea: someone may have been here before you. Its phenomena appear in the world as fleeting echoes rather than ordinary hostile mobs.

## Atmosphere and design philosophy

TracesOfPresence aims for light fantasy, quiet mystery, cozy melancholy, and a sense of wonder. It is **not a horror mod**. The encounters are meant to feel like fragments of another life left in the world.

## The four phenomena

- **Blue** — In the Overworld at night, the Blue Echo can appear near a Life Echo crystal in a densely wooded area. It walks toward a nearby tree and fades with a brief visual effect.
- **Purple** — Interacting with a Life Echo crystal can trigger a short vision. A Purple Echo then appears at a safe spot ahead of the player; if no suitable spot is available, the appearance is retried. Approaching the apparition reveals a brief message before it disappears.
- **Green** — The Green Echo can replay a selection of a player's recent mining, block placement, and crafting actions after a sustained period of work and inactivity.
- **Red** — The Red Echo can replay a recorded death after the player returns near the place of death. The recording includes the player's recent movement and nearby events. A death-site mark may also appear on suitable natural ground.

## Implemented features

- Life Echo crystals generate with an irregular, mossy forest-floor site.
- Blue, Purple, Green, and Red apparition encounters are implemented.
- The Blue Echo's appearance chances and cooldown, and the Green Echo's enablement, chances, and cooldown, can be configured.
- Operators can preview the apparition types with `/traces test blue`, `/traces test purple`, `/traces test green`, and `/traces test red`.
- The item `traces:blue_shard` is registered and appears in the mod's creative tab. It currently uses a technical model with Minecraft's blue-ice texture as a placeholder; its finished visual design is not available yet. For testing, use `/give @s traces:blue_shard`.

## Currently in development

The planned Blue Echo story, **“A Quiet Place,”** is not implemented. The Blue Shard is registered, but it has no survival acquisition method and does not yet unlock a story encounter. The shard's finished visual design is also pending.

## Installation

1. Install Minecraft Java Edition 1.20.1 and a compatible Forge 1.20.1 installation.
2. Obtain a TracesOfPresence JAR from a project build.
3. Place the JAR in the Minecraft `mods` folder and launch the Forge profile.

No verified public release download is listed here. To build from the source repository, use JDK 17 and the included Gradle wrapper: `gradlew build` (Windows: `gradlew.bat build`).

## Project status

The mod is in active alpha development. Existing encounters are being refined while new story content is planned. Back up worlds before testing development builds.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for confirmed development milestones and the current in-progress work.
