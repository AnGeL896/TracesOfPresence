# Changelog

This file records changes supported by the available source snapshot and GitHub commit history. Dates are commit dates. A version is listed only when a commit or the project configuration explicitly identifies it; a configured version is not, by itself, proof of a published release.

## 0.6.35-alpha.1 — Blue Shard

The public README and `build.gradle` identify `0.6.35-alpha.1` as the current project version. A release date or downloadable release asset could not be independently confirmed.

### Added

- Added the Blue Shard item to the mod's creative tab as the first item for the Blue Echo story. Its floating ice-fragment model uses Minecraft's blue-ice texture as a temporary material; there is no survival acquisition method yet.

### In Progress

- The Blue Shard has no survival acquisition method or finished visual design.
- The planned Blue Echo story, “A Quiet Place,” has not been implemented.

## Versioned milestones documented in the public README

The public README identifies the versions below and describes their changes. The available commit search did not establish a matching commit date for every version, so dates are omitted here.

### 0.6.34-alpha.1 — Changed

- Made Green Echo encounters rarer: require sustained work, wait after the last action, and lower the eligible-check chance.

### 0.6.33-alpha.1 — Changed

- Allowed Blue Echo encounters across the Overworld where enough nearby tree trunks are present and extended the time it can take to reach cover.

### 0.6.32-alpha.1 — Changed

- Colored apparition chat messages and test feedback to match each apparition.

### 0.6.31-alpha.1 — Changed

- Made the Purple Echo begin dissolving after its approach message is shown.

### 0.6.30-alpha.1 — Fixed

- Kept the Purple Echo visible until its approach or departure condition and stopped ambient particles during its dissolution.

### 0.6.29-alpha.1 — Added

- Added the Purple Echo encounter 10–12 blocks ahead of the player, with an approach message, a distance-based ending, and retry behavior when no safe spawn spot is available.

### 0.5.29-alpha.1 — Changed

- Restricted Purple Echo spawn searches to a safe floor close to the player's elevation and the point ahead; retained retry behavior when no spot is available.

### 0.5.28-alpha.1 — Added

- Added a transparent pixel-art texture for the death-site mark.

### 0.5.27-alpha.1 — Added

- Added Blue, Purple, and Red operator test commands alongside the Green test.

### 0.5.26-alpha.1 — Fixed

- Restored the Blue, Green, and Red apparition textures used by the entity renderer.

## 2026-11-07 — Documentation

### Changed

- Updated the public README to describe the mod and its implemented features. (Commit `6af0e6e`.)

## 2026-11-05 — Purple vision and replay updates

### Added

- Added the Purple Echo vision triggered by interacting with a Life Echo crystal, including the apparition sequence and retry handling. (Commits `2be76a0`, `332960b`, `6467a1d`.)

### Changed

- Changed Red Echo playback to follow recorded positions. The release commit identifies this update as `0.5.24.1`. (Commits `6a4fbd4`, `86a36bb`.)
- Updated the Life Echo crystal texture in version `0.5.25-alpha.1`. (Commit `44a50c0`.)

### Fixed

- Fixed Purple Echo ground placement and a Green Echo recorder player-type issue. (Commits `ee30e0d`, `7a62fbf`.)
- Fixed a swamp biome check for Forge 1.20.1 and restored the complete Presence Director source. (Commits `d586713`, `ef3cee9`.)

## 2026-11-04 — Apparitions and memory replays

### Added

- Added the apparition entity and developed the Blue Echo's tree-cover encounter and fade effects. (Commits `629a6a8`, `6d1608d`, `94dea83`, `7735ce5`.)
- Added player-history recording and the Red Echo's death replay. (Commits `6fb9f8d`, `51b8081`.)
- Added Green Echo replays of recent mining, block placement, and crafting. The release commit identifies this work as `0.5.13`. (Commits `b91f9d4`, `ab6cc24`.)
- Added operator commands for testing apparition types and a chance-based death-site mark. (Commits `c7fd431`, `046ebe3`.)

### Changed

- Made Blue and Green encounter settings configurable and refined Green Echo memory selection and pacing. The pacing update identifies version `0.5.14`. (Commits `09be76e`, `70b8e2f`, `fb8d1ea`, `5d06455`.)
- Improved the death replay presentation, including the killer reveal, movement, and equipment. The history contains a `0.5.0` version bump; it does not establish that all of these changes were released under that version. (Commits `fbe574d`, `d19b18f`, `4b4432d`.)
- Added bounded recording and rendering optimizations across versions `0.5.15`–`0.5.23`, including crystal caching, periodic history cleanup, incremental replay cursors, and cached rendering data. (Commits `045d63a`, `8941191`, `84b779d`, `942a081`, `644b80c`, `f546539`, `374b32f`, `bb06531`, `6517805`.)

## History notes

- The local Git history available during this update ends on 2026-11-03. The dated milestones above were cross-checked against the public GitHub commit history.
- The Blue Shard and versions `0.5.26`–`0.6.35` are documented in the public README; the available commit search did not establish a matching date for every milestone.
- No verified release date or downloadable release asset was found for `0.6.35-alpha.1`.
- The “A Quiet Place” story is a plan, not an implemented feature.
