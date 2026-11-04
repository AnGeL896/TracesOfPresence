# Traces of Presence

Minecraft Forge 1.20.1, version 0.5.22.

## Echoes

- **Blue:** appears at night in forests and taigas near a Life Echo crystal.
- **Green:** can replay recent mining, block placement, and crafting actions around a coherent work site in any dimension, with travel time scaled to the distance between actions.
- **Red:** replays the last ten seconds before a player's death when they return nearby.

Encounter chances and cooldowns are in `config/traces-common.toml`. Set a chance to `1.0` to force frequent eligible checks while testing.

## Testing the green echo

With cheats enabled or as an operator, run:

```
/traces test green
```

This creates a visual-only sample of mining, building, and crafting actions. It never changes blocks in the world.

## Performance work in 0.5.15–0.5.22

- Reuse a nearby Life Echo crystal while it remains loaded and within range.
- Record nearby killer movement at 10 frames per second and prune stale tracks once per second.
- Reduce temporary allocations during nearby entity sampling.
- Advance death and killer replay cursors incrementally instead of rescanning frames.
- Cache replay equipment and block particle data; clear bounded client and server caches when their world or server ends.
