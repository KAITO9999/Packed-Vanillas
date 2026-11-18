# Packed-Vanillas — static audit and implementation log

## Target and provenance

- Upstream: `ItsBlackGear/VanillaBackport`, branch `1.20.1`
- Audited upstream commit: `2f62cbef963dfb0906706535474720fe07ed755d`
- Upstream mod version: `1.2.2`
- Target: Minecraft 1.20.1 / Forge 47.x
- Fork: `KAITO9999/Packed-Vanillas`
- Working branch: `packed/platform-internalization`
- Local checkout: `~/src/Packed-Vanillas`
- Upstream remote: `https://github.com/ItsBlackGear/VanillaBackport.git`
- Origin remote: `https://github.com/KAITO9999/Packed-Vanillas.git`

The Forge mod id and the `minecraft` resource/data namespace are intentionally retained as `vanillabackport` for world, registry, and existing resource compatibility. The display name/artifact name becomes `Packed-Vanillas` without gratuitously changing persisted identifiers.

## Static inventory

Before vendoring:

- 702 Java source files across the common and Forge source sets.
- 153 Mixin source files (common/client/compat groups).
- 2,620 resource files under the inspected common/Forge resource trees.
- 136 source files import Platform APIs, with 213 direct Platform imports.
- Platform API imports span `core`, `client`, and `common`; this is not a two-line Gradle dependency removal.

## Platform dependency findings

The upstream project had Platform dependencies in both `common/build.gradle` and `forge/build.gradle`, versioned through `platform_indev_version=1.4.2`. Forge `mods.toml` declared `platform` as a mandatory runtime mod. A clean GitHub Actions checkout confirmed that the upstream build was not reproducible: Gradle failed because `flatDir:Platform-common-1.20.1-1.4.2` resolved to the absent absolute path `/libs/Platform-common-1.20.1-1.4.2.jar`.

Platform was used for:

- bootstrap/lifecycle and environment detection;
- config registration/loading;
- registry builders and typed registry helpers;
- Forge event pipelines for common/client/HUD/rendering;
- custom block/item/entity/particle/sound registration helpers;
- synced entity data and networking;
- biome/worldgen modifier and placement helpers;
- loot modifier and data/resource reload integration;
- creative tab and rendering integration APIs.

A simple Gradle dependency deletion would leave unresolved imports and, more importantly, silently remove event pipelines used by the mod.

### Current internalization state

The working branch now vendors 202 Platform common + Forge Java source files under `com.blackgear.vanillabackport.internal.platform` and removes the separate Platform mod requirement.

Completed locally:

- Relocated Platform Java packages and all VanillaBackport imports.
- Excluded Platform's separate `@Mod` entrypoint and metadata.
- Retargeted all 8 Platform Forge event-subscriber classes to `vanillabackport`.
- Called the internal Platform bootstrap from the existing Forge entrypoint before VanillaBackport's own bootstrap.
- Added Platform common/Forge Mixin configs to Forge's Mixin registration.
- Merged Platform's access-widener declarations into the existing VanillaBackport access widener.
- Removed Platform artifacts from both Gradle projects and removed the absolute `/libs` repository.
- Removed the Fabric project from this Forge-only fork's Gradle settings.
- Preserved the original mod id, resource/data namespaces, and `platform:` internal resource identifiers.
- Included the vendored Platform MIT notice in `LICENSES/Platform-MIT.txt`.

The Platform API is now internal source, not a separate download or runtime mod. Its internal resource namespace remains `platform:` where used by the vendored networking/biome-modifier implementation; this is distinct from a required mod id.

### Static checks

A Termux static-check script currently passes 26/26 assertions:

- no old `com.blackgear.platform` package references;
- all 202 vendored Java files have package/path agreement;
- no separate Platform `@Mod` entrypoint;
- all 8 event subscribers use `vanillabackport`;
- all four configured Mixin files resolve their classes;
- no Platform dependency remains in `mods.toml` or Gradle coordinates;
- internal bootstrap, Mixin configs, access widener, and MIT notice are present;
- Forge-only target and original mod id are preserved.

This is static validation, not a compile or runtime pass. The first GitHub Actions compile reached common Java compilation and exposed one additional upstream gap: ShelfRenderer references ModelBoundingBoxManager, but that class is absent from the public Platform 1.20.1 source. A local implementation was added to derive bounds from baked quads using the FIXED item transform. The next CI run must verify this implementation and reveal any further missing API or dependency.

## Resource-pack compatibility: 1.20.1 vs modern packs

Minecraft 1.20.1 uses resource-pack format 15. Modern resource packs have moved far beyond that format. Relevant changes to account for:

- 1.21.4: item model definitions under `assets/<namespace>/items/`.
- 1.21.6: strict JSON parsing for resource-pack JSON.
- 1.21.9: `min_format` / `max_format` replace `supported_formats` for modern pack metadata; `pack_format` is optional for new-format packs.
- 1.21.11: resource-pack format 75; item textures are separated into an `items` atlas without mipmaps; atlas sprite-name collisions warn; block model/state rotations expand; texture `.mcmeta` gains `mipmap_strategy` and `alpha_cutoff_bias`.
- 26.3 (latest stable release as of 2026-11-18): resource-pack format 97.1; armor-trim palette texture paths changed, terrain shaders were refactored for multi-draw, and Order-Independent Transparency (OIT) shader resources were added/renamed.
- 26.4 Snapshot 9/10 (development snapshot, not the stable target): resource-pack format 104.0; OIT shader interfaces continue changing, including subpass-based rendering and additional shader resource changes.

Default compatibility target should be the latest stable release, 26.3 (format 97.1). 26.4 snapshots should be an explicitly experimental best-effort tier, because the renderer and OIT shader interfaces are still changing. Compatibility should be implemented at the resource-pack ingestion/adapter boundary, not by changing the global renderer blindly. Initial safe scope to investigate:

1. Read modern `pack.mcmeta` and normalize metadata for the 1.20.1 pack reader without modifying the user's source archive.
2. Convert the subset of modern `items/*.json` item-model definitions that can be represented by 1.20.1 item models; explicitly warn/skip unsupported node types instead of silently producing broken models.
3. Merge/remap the modern `items` atlas definitions into the legacy 1.20.1 blocks atlas where semantics permit.
4. Translate supported `.mcmeta` animation/texture fields and ignore only fields with no 1.20.1 equivalent.
5. Backport supported 26.3 palette texture path changes and compatible non-shader asset renames where their meaning is unchanged.
6. Treat modern shader packs, equipment definitions, expanded model rotations, and snapshot-only renderer resources as separate compatibility classes; do not claim full support unless each has a faithful conversion. Core shaders should be treated as unsupported/experimental rather than mechanically rewritten.

The mod's existing resource reload code primarily handles its own JSON assets (waypoint styles, foliage colors, ghast equipment). It does not currently provide a general modern resource-pack conversion layer. Official reference points: [1.21.4](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-4), [1.21.9](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-9), [1.21.11](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11), [26.3](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3), and [26.4 Snapshot 9](https://www.minecraft.net/en-us/article/minecraft-26-4-snapshot-9).

## Performance candidates (behavior-preserving first)

### High-confidence candidate

`ClientWaypointManager.forEachWaypoint()` currently creates a stream and sorts the entire waypoint collection by distance on every HUD render. The locator bar is rendered per frame, so this repeats sorting and stream work at display refresh rate even though waypoint state is updated on client ticks. Investigate a reusable sorted snapshot/cache invalidated on waypoint mutations and refreshed at most once per client tick. Preserve far-to-near draw order; compare behavior when two markers overlap or cross distance order within a tick.

### Low-value / do not optimize without evidence

- `DynamicBlockRenderingHandler` polls only two watched config values every 40 ticks; cost is negligible. Preserve the chunk rebuild when either value changes.
- Registry-wide item/block enumeration in `ItemLikeRendering` happens during setup/model registration, not per frame.
- `ItemWaypointProvider` scans inventory only when compass-waypoint display is enabled (default false); avoid adding a hot-path cache until profiling shows a need.
- Concurrent collections may be replaceable, but only after confirming all packet/event mutations occur on the client thread.

## Compatibility paths: preserve pending evidence

Existing compatibility gates are not arbitrary overhead and currently protect meaningful behavior:

- Better Combat: avoids duplicate spear attack/range/animation behavior.
- Caverns & Chasms: avoids duplicate weathering lightning-rod entries.
- Quark: recognizes its shiny animal texture variants.
- Backported Wolves: conversion/variant interoperability.
- Tiny Takeover: avoids replacing its baby animal models.
- Every Compat: conditional wood integration.
- JER and Mouse Tweaks mixins are `@Pseudo`; they provide optional integrations for biome listing and bundle mouse-wheel behavior.

Do not delete these solely to reduce source size. Re-evaluate only if the relevant upstream integration is obsolete for the actual target versions and the removal cannot alter gameplay, resource packs, or user interaction.

## Validation tool audit

Inspected `~/mc-test-kit/README.md`, `REQUEST.txt`, `PROVENANCE.txt`, `package.json`, and `bin/mc-test.mjs`.

- `java-jar`: ZIP integrity plus common Fabric/Quilt/Forge metadata detection.
- `java-project`: invokes Gradle `test` and `build`; it executes project Gradle scripts and is not a static source analyzer.
- It does not currently validate Mixin targets, runtime event wiring, resource-pack format conversions, or rendered output.
- Its installed Node test suite passed 6/6 tests on the audit run.
- Termux's build-plan gate blocked a local Forge compile because the project scan was incomplete and the estimated artifact size was unknown; it directed this project to GitHub Actions or a cloud build host.

### CI baseline findings

The initial workflow exposed two upstream repository issues:

1. The checked-in Gradle wrapper JAR is missing and the wrapper script is not executable. The CI workflow therefore installs Gradle 8.14.4 directly with `gradle/actions/setup-gradle`.
2. With Gradle available, the unmodified upstream project still fails at dependency resolution because the Platform common JAR is expected at `/libs/Platform-common-1.20.1-1.4.2.jar`, which is not part of a clean checkout.

The second failure is the reproducibility defect the internal-source approach is intended to remove. The first post-internalization CI run failed at the missing ModelBoundingBoxManager symbol; that missing API has since been filled locally. A second CI compile is pending.

## Remaining implementation order

1. Obtain a clean GitHub Actions compile of the internalized Platform branch; fix source/dependency errors before feature work.
2. Run the existing `mc-test java-project` path only through an approved build route and use `mc-test java-jar` on the produced artifact.
3. Implement and test the waypoint render-order cache.
4. Implement resource-pack metadata normalization and the supported modern item-model/atlas conversion subset with fixtures.
5. Only then evaluate larger Mixin/config compatibility removals or deeper optimizations.
