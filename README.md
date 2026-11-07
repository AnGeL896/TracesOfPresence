# Traces of Presence

Minecraft Forge 1.20.1, version 0.6.35-alpha.1 (alpha build).

## Echoes

- **Blue:** may appear at night near a Life Echo crystal when several trees are nearby, and tries to hide behind a tree.
- **Purple:** first interaction with a Life Echo crystal triggers a brief blindness vision before the apparition appears. If a safe spawn spot is unavailable, the player gets a fleeting doubt and the apparition retries after five seconds.
- **Green:** can replay recent mining, block placement, and crafting actions around a coherent work site in any dimension, with travel time scaled to the distance between actions.
- **Red:** replays the last ten seconds before a player's death when they return nearby.

Encounter chances and cooldowns are in `config/traces-common.toml`. Set a chance to `1.0` to force frequent eligible checks while testing.

## Building

Use JDK 17 and the Gradle wrapper:

```
.\gradlew.bat build
```

On macOS or Linux, run `sh gradlew build`. The wrapper downloads the project's pinned Gradle version on its first run.

## Replay path update in 0.5.25-alpha.1

- Death echoes and killers follow the recorded positions using linear interpolation. Synthetic approach motion and spline path bending are removed to avoid cutting through blocks.

## Crystal texture update in 0.5.25-alpha.1

- Replaces the crystal texture with a hand-pixelled 32×32 teal mineral surface inspired by the supplied icy reference; the existing crystal geometry is unchanged.

## Apparition texture restoration in 0.5.26-alpha.1

- Restores the blue, green, and red apparition textures used by the entity renderer.

## Testing apparitions

With cheats enabled or as an operator, test an apparition with:

```
/traces test blue
/traces test purple
/traces test green
/traces test red
```

The red test replays a synthetic player path and zombie attack. These tests are visual-only and do not change blocks or cause damage. The blue test still needs a suitable nearby tree as cover. Only one apparition test can be active within 48 blocks at a time.

## Apparition test commands in 0.5.27-alpha.1

- Adds `/traces test blue`, `purple`, and `red` alongside the existing green test.

## Death scar texture in 0.5.28-alpha.1

- Adds a transparent, pixel-art earth mark over the podzol death-scar block.

## Purple Echo spawn range in 0.5.29-alpha.1

- Search only for a safe floor near the player's current elevation and within one block of the point ahead, rather than using the surface heightmap.
- If no suitable spot is available, show the existing doubt message and retry after five seconds.

## Purple Echo encounter in 0.6.29-alpha.1

- The echo appears 10–12 blocks ahead of the player. If no safe spot is available at that distance, it retries after five seconds.
- Approaching within 1.5 blocks triggers “I was here before you” once.
- The echo dissolves when its summoning player moves more than 30 blocks away; that crystal cannot summon it again after a successful spawn.

## Purple Echo lifetime fix in 0.6.30-alpha.1

- Keep the model visible until the summoning player approaches or moves beyond 30 blocks; do not apply the generic apparition fade timer.
- Stop emitting ambient particles while the purple echo dissolves.

## Purple Echo approach ending in 0.6.31-alpha.1

- After the summoning player approaches within 1.5 blocks and sees “I was here before you,” the echo begins dissolving.

## Colored apparition messages in 0.6.32-alpha.1

- Color chat lines and test feedback to match their apparition: blue, green, purple, or red.

## Blue Echo habitat and hiding time in 0.6.33-alpha.1

- Allow the blue echo anywhere in the Overworld with at least four nearby tree trunks, instead of limiting it to forest and taiga biome tags.
- Extend its time to reach tree cover from 11 to 30 seconds.

## Green Echo rarity in 0.6.34-alpha.1

- Require at least 10 recent actions, including 8 block placements or breaks across 6 distinct positions, over at least one minute of work.
- Wait 15 seconds after the last action and reduce the eligible-check chance from 8% to 2%.

## Blue Shard item model in 0.6.35-alpha.1

- Adds the Blue Shard item to the Traces of Presence creative tab as the first item for the blue echo story.
- Gives it a custom, floating ice-fragment model using Minecraft's blue ice texture as a temporary material until the author-provided texture is added.
- The shard does not yet have a survival acquisition method; it can be inspected in Creative or obtained with `/give @s traces:blue_shard` for testing.

## Performance work in 0.5.15–0.5.23

- Reuse a nearby Life Echo crystal while it remains loaded and within range.
- Record nearby killer movement at full 20 frames per second and prune stale tracks once per second.
- Reduce temporary allocations during nearby entity sampling.
- Advance death and killer replay cursors incrementally instead of rescanning frames.
- Cache replay equipment and block particle data; clear bounded client and server caches when their world or server ends.
- Bound renderer caches and reset world-bound visual entities when switching worlds.

